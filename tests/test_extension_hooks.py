"""Host regression tests for hook isolation, rollback and missing-target handling.

Runs the production hook classes against small framework fixtures, not an Android
device. Device/ROM compatibility still needs a separate LSPosed runtime test.
"""
from pathlib import Path
import re
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
PRODUCTION = ROOT / "app/src/main/java/io/github/leekin2/xsecurity"
FIXTURES = {
    "android/annotation/SuppressLint.java": "package android.annotation; public @interface SuppressLint { String[] value(); }",
    "io/github/libxposed/api/XposedInterface.java": """package io.github.libxposed.api;
import java.util.List;
public interface XposedInterface {
    interface Hooker { Object intercept(Chain chain)throws Throwable; }
    interface Chain { Object getThisObject(); Object getArg(int i); List<Object> getArgs();
        Object proceed()throws Throwable; Object proceed(Object[] args)throws Throwable; }
}
""",
    "android/content/Context.java": "package android.content; public class Context {}",
    "android/os/PowerManager.java": """package android.os;
public class PowerManager { public interface ServiceType { int SCREEN_BRIGHTNESS = 7; } }
""",
    "android/os/PowerSaveState.java": """package android.os;
public class PowerSaveState {
    public boolean batterySaverEnabled, globalBatterySaverEnabled;
    public float brightnessFactor;
    public static class Builder {
        private final PowerSaveState s = new PowerSaveState();
        public Builder setBatterySaverEnabled(boolean b) { s.batterySaverEnabled=b; return this; }
        public Builder setGlobalBatterySaverEnabled(boolean b) { s.globalBatterySaverEnabled=b; return this; }
        public Builder setBrightnessFactor(float f) { s.brightnessFactor=f; return this; }
        public PowerSaveState build() { return s; }
    }
}
""",
    "com/android/server/power/batterysaver/BatterySaverPolicy.java": """package com.android.server.power.batterysaver;
import android.os.PowerSaveState;
public class BatterySaverPolicy {
    public final PowerSaveState original = new PowerSaveState.Builder().setBatterySaverEnabled(true)
        .setGlobalBatterySaverEnabled(true).setBrightnessFactor(.5f).build();
    public PowerSaveState getBatterySaverPolicy(int type) { return original; }
}
""",
    "com/android/server/display/mode/Vote.java": """package com.android.server.display.mode;
public class Vote {
    public static final int PRIORITY_LOW_POWER_MODE_RENDER_RATE=9, PRIORITY_LOW_POWER_MODE_MODES=10;
    public static final int PRIORITY_SKIN_TEMPERATURE=13, PRIORITY_USER_SETTING_PEAK_REFRESH_RATE=7;
}
""",
    "com/android/server/display/mode/VotesStorage.java": """package com.android.server.display.mode;
import java.util.*;
public class VotesStorage {
    public final Map<String,Vote> votes = new HashMap<>();
    public int notifications;
    public void updateVote(int display, int priority, Vote vote) {
        String key=display+":"+priority;
        if(vote==null) votes.remove(key); else votes.put(key,vote);
        notifications++;
    }
}
""",
    "io/github/leekin2/xsecurity/Hooks.java": """package io.github.leekin2.xsecurity;
import java.lang.reflect.*; import java.util.*;
import io.github.libxposed.api.XposedInterface.Hooker;
final class Hooks {

    static final class Chain implements io.github.libxposed.api.XposedInterface.Chain {
        final Method method; final Object self; final Object[] args;
        Chain(Method m,Object s,Object[] a){method=m;self=s;args=a;}
        public Object getThisObject(){return self;} public Object getArg(int i){return args[i];}
        public List<Object> getArgs(){return Arrays.asList(args);}
        public Object proceed() throws Throwable{return proceed(args);}
        public Object proceed(Object[] replacement) throws Throwable {
            try{return method.invoke(self,replacement);}catch(InvocationTargetException e){throw e.getCause();}
        }
    }
    final Map<Method,Hooker> installed=new HashMap<>(); final List<String> errors=new ArrayList<>();
    void install(Method m,Hooker h){m.setAccessible(true);installed.put(m,h);}
    void constant(Method m,Object v){install(m,c->v);}
    void deoptimize(Executable m){}
    void info(String m){} void error(String m,Throwable t){errors.add(m);}
    Object invoke(Method m,Object self,Object...args) throws Throwable {
        Chain c=new Chain(m,self,args); Hooker h=installed.get(m); return h==null?c.proceed():h.intercept(c);
    }
}
""",
    "android/content/SharedPreferences.java": """package android.content;
public interface SharedPreferences {
    boolean getBoolean(String key, boolean fallback);
    default String getString(String key, String fallback){return fallback;}
}
""",
    "io/github/leekin2/xsecurity/DexTargets.java": """package io.github.leekin2.xsecurity;
import java.lang.reflect.*; import java.util.*;
final class DexTargets {
    final Map<String,List<Method>> results=new HashMap<>();
    List<Method> usingStrings(String key,String type,String...strings){return results.getOrDefault(key,List.of());}
}
""",
    "com/miui/networkassistant/model/AppInfo.java": """package com.miui.networkassistant.model;
public class AppInfo { public int uid; public CharSequence packageName; public boolean isSystemApp;
    public AppInfo(int u,String p){uid=u;packageName=p;isSystemApp=true;}
    public AppInfo(AppInfo a){uid=a.uid;packageName=a.packageName;isSystemApp=a.isSystemApp;}
}
""",
    "com/miui/networkassistant/traffic/statistic/PreSetGroup.java": """package com.miui.networkassistant.traffic.statistic;
public class PreSetGroup {
    public static boolean isPreFirewallWhiteListPackage(String p){return p.equals("protected.firewall");}
    public static boolean isPrePolicyPackage(String p){return p.equals("protected.background");}
}
""",
    "com/miui/networkassistant/service/wrapper/AppMonitorWrapper.java": """package com.miui.networkassistant.service.wrapper;
import java.util.*; import com.miui.networkassistant.model.AppInfo;
public class AppMonitorWrapper {
    public ArrayList<AppInfo> system=new ArrayList<>(); public ArrayList<AppInfo> getSystemAppList(){return system;}
}
""",
    "com/miui/networkassistant/ui/fragment/FirewallListFragment.java": """package com.miui.networkassistant.ui.fragment;
import com.miui.networkassistant.service.wrapper.AppMonitorWrapper;
public class FirewallListFragment { public AppMonitorWrapper mAppMonitorWrapper=new AppMonitorWrapper(); }
""",
    "com/miui/networkassistant/ui/fragment/WlanFirewallFragment.java": """package com.miui.networkassistant.ui.fragment;
import java.util.*; import com.miui.networkassistant.model.AppInfo;
public class WlanFirewallFragment extends FirewallListFragment {
    public ArrayList<AppInfo> existing=new ArrayList<>();
    public ArrayList<AppInfo> getAppList(){return existing;}
}
""",
    "com/miui/networkassistant/ui/fragment/ShowAppDetailFragment.java": """package com.miui.networkassistant.ui.fragment;
import com.miui.networkassistant.model.AppInfo;
public class ShowAppDetailFragment {
    public AppInfo mAppInfo; public boolean mIsManagedProfileApp,throwDuringInit,seenSystem;
    public void initFirewallData(){seenSystem=mAppInfo.isSystemApp;if(throwDuringInit)throw new IllegalStateException("fixture");}
}
""",
    "android/content/pm/SigningDetails.java": """package android.content.pm;
public class SigningDetails {
    public boolean checkCapability(SigningDetails d,int flags){return flags==0 && this==d;}
    public boolean checkCapabilityRecover(SigningDetails d,int flags){return false;}
    public boolean signaturesMatchExactly(SigningDetails d){return false;}
    public boolean hasCommonAncestor(SigningDetails d){return false;}
    public static class TestSignature { private final byte[] bytes;
        public TestSignature(byte[] b){bytes=b;} public byte[] toByteArray(){return bytes;}}
    private final TestSignature[] signatures;
    public SigningDetails(byte[] b){signatures=new TestSignature[]{new TestSignature(b)};}
    public TestSignature[] getSignatures(){return signatures;}
}
""",
    "android/content/pm/ApplicationInfo.java": """package android.content.pm;
public class ApplicationInfo { public int flags; public boolean isPackageWhitelistedForHiddenApis(){return false;} }
""",
    "com/android/server/pm/InstallPackageHelper.java": """package com.android.server.pm;
import com.android.server.pm.pkg.AndroidPackage;
public class InstallPackageHelper {
    public interface Work { Object run()throws Throwable; }
    public static Object installationFlow(Work work)throws Throwable{return work.run();}
    public boolean doesSignatureMatchForPermissions(String name,AndroidPackage pkg){return false;}
}
""",
    "com/android/server/pm/KeySetManagerService.java": """package com.android.server.pm;
public class KeySetManagerService {
    public boolean shouldCheckUpgradeKeySetLocked(Object old,int flags){return false;}
    public boolean checkUpgradeKeySetLocked(Object old,Object pkg){return false;}
}
""",
    "android/content/pm/PackageInfoLite.java": """package android.content.pm;
public class PackageInfoLite { public String packageName; public PackageInfoLite(String n){packageName=n;} }
""",
    "com/android/server/pm/pkg/AndroidPackage.java": """package com.android.server.pm.pkg;
import android.content.pm.SigningDetails;
public class AndroidPackage {
    public String name="test.corepatch.app",path="/data/app/test",sharedUserId=null;
    public boolean debuggable=true; public SigningDetails signing;
    public String getPackageName(){return name;} public String getPath(){return path;}
    public String getSharedUserId(){return sharedUserId;} public boolean isDebuggable(){return debuggable;}
    public SigningDetails getSigningDetails(){return signing;}
}
""",
    "com/android/server/pm/PackageSetting.java": """package com.android.server.pm;
import com.android.server.pm.pkg.AndroidPackage;
public class PackageSetting {
    public PackageSignatures signatures=new PackageSignatures();
    public AndroidPackage pkg; public boolean system=false,privileged=false;
    public int appId=12345,sharedUserAppId=-1;
    public PackageSetting(AndroidPackage p){pkg=p;}
    public String getPackageName(){return pkg.name;} public boolean isSystem(){return system;}
    public boolean isPrivileged(){return privileged;} public int getAppId(){return appId;}
    public int getSharedUserAppId(){return sharedUserAppId;} public AndroidPackage getPkg(){return pkg;}
}
""",
    "com/android/server/pm/PackageSignatures.java": """package com.android.server.pm;
import android.content.pm.SigningDetails;
public class PackageSignatures { public SigningDetails mSigningDetails; }
""",
    "com/android/server/pm/SharedUserSetting.java": """package com.android.server.pm;
import java.util.*;
public class SharedUserSetting {
    public int uidFlags; public Set<PackageSetting> mPackages=new LinkedHashSet<>();
    public PackageSignatures signatures=new PackageSignatures();
    public boolean addPackage(PackageSetting p){return mPackages.add(p);}
    public boolean removePackage(PackageSetting p){return mPackages.remove(p);}
}
""",
    "com/android/server/pm/PackageManagerException.java": """package com.android.server.pm;
public class PackageManagerException extends Exception {
    public final int error; public PackageManagerException(int code){super("fixture "+code);error=code;}
}
""",
    "com/android/server/pm/PackageManagerServiceUtils.java": """package com.android.server.pm;
import android.content.pm.*; import com.android.server.pm.pkg.AndroidPackage;
public class PackageManagerServiceUtils {
    public static int verifyError=-7,downgradeError=-25,verifications;
    public static boolean verifySignatures(PackageSetting existing,PackageSetting disabled,SigningDetails incoming)throws PackageManagerException{
        verifications++; if(verifyError!=0)throw new PackageManagerException(verifyError);return true;}
    public static void checkDowngrade(AndroidPackage before,PackageInfoLite after)throws PackageManagerException{
        if(downgradeError!=0)throw new PackageManagerException(downgradeError);}
    public static boolean canJoinSharedUserId(){return false;}
}
""",
    "com/android/server/pm/ReconcilePackageUtils.java": """package com.android.server.pm;
public class ReconcilePackageUtils {
 private static final boolean ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS=Boolean.getBoolean("fixture.allow_non_preloads");
 public static boolean allow(){return ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS;}
 public static void reconcilePackages(){}
}
""",
    "io/github/leekin2/xsecurity/RegressionMain.java": r"""package io.github.leekin2.xsecurity;
import java.util.*; import java.lang.reflect.*;
import android.content.Context; import android.os.PowerSaveState;
import com.android.server.display.mode.*;
import com.android.server.power.batterysaver.BatterySaverPolicy;
import com.miui.networkassistant.model.AppInfo;
import com.miui.networkassistant.ui.fragment.*;
import android.content.pm.*;
import com.android.server.pm.*;
import com.android.server.pm.pkg.AndroidPackage;
public class RegressionMain {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static void rejection(Hooks h,Method m,int code,Object...args)throws Throwable{
        try{h.invoke(m,null,args);throw new AssertionError("expected install rejection "+code);}
        catch(PackageManagerException e){check(e.error==code,"preserve the original install rejection");}
    }
    public static class Game {
        boolean stored; public boolean enabled(){return stored;}
        public void setEnabled(boolean value){stored=value;}
        public void activeSession(boolean value){stored=value;}
        public void enter(Context context){} public void ambiguous(Context context){}
    }
    static void corePatchChecks(ClassLoader cl, Map<String,Boolean> switches)throws Throwable {
        for(String key:Features.CORE_PATCH)check(!Features.enabled(key),"Core Patch default off: "+key);
        Hooks off=new Hooks();new CorePatchHooks(off,cl).install();check(off.installed.isEmpty(),"no Core Patch hooks while disabled");
        switches.put(Features.CP_DOWNGRADE,true);switches.put(Features.CP_DIGEST,true);
        switches.put(Features.CP_EXACT,true);switches.put(Features.CP_HIDDEN,true);
        Hooks core=new Hooks();new CorePatchHooks(core,cl).install();
        check(core.errors.isEmpty(),"all selected Core Patch target groups resolve");
        Method downgrade=PackageManagerServiceUtils.class.getMethod("checkDowngrade",AndroidPackage.class,PackageInfoLite.class);
        check(core.invoke(downgrade,null,new AndroidPackage(),new PackageInfoLite("test.corepatch.app"))==null,"version check bypass installed without developer mode");
        SigningDetails signer=new SigningDetails(new byte[]{1});
        Method capability=SigningDetails.class.getMethod("checkCapability",SigningDetails.class,int.class);
        check(Boolean.FALSE.equals(core.invoke(capability,signer,signer,1)),"signer capability unchanged outside installation");
        check(Boolean.TRUE.equals(InstallPackageHelper.installationFlow(()->core.invoke(capability,signer,signer,1))),"installer data capability enabled");
        check(Boolean.TRUE.equals(InstallPackageHelper.installationFlow(()->core.invoke(capability,signer,signer,2))),"installer shared-UID capability enabled");
        for(int flags:new int[]{4,5,16,17,20}){
            check(Boolean.FALSE.equals(InstallPackageHelper.installationFlow(()->core.invoke(capability,signer,signer,flags))),"permission/auth flags preserved, including combinations "+flags);
        }
        Method exact=SigningDetails.class.getMethod("signaturesMatchExactly",SigningDetails.class);
        check(Boolean.FALSE.equals(core.invoke(exact,signer,signer)),"exact matching unchanged outside installation");
        check(Boolean.TRUE.equals(InstallPackageHelper.installationFlow(()->core.invoke(exact,signer,signer))),"exact matching bypass in installer");
        ApplicationInfo system=new ApplicationInfo();Method hidden=ApplicationInfo.class.getMethod("isPackageWhitelistedForHiddenApis");
        check(Boolean.FALSE.equals(core.invoke(hidden,system)),"normal apps retain hidden-API restrictions");
        system.flags=1;check(Boolean.TRUE.equals(core.invoke(hidden,system)),"system hidden API choice");
        system.flags=128;check(Boolean.TRUE.equals(core.invoke(hidden,system)),"updated-system hidden API choice");
        check(!CorePatchHooks.inInstallFlow(),"ordinary code and TLS do not inherit package-install context");
        switches.put(Features.CP_SHARED,true);
        Hooks sharedHooks=new Hooks();new CorePatchHooks(sharedHooks,cl).install();
        check(sharedHooks.errors.isEmpty(),"immutable preload policy does not abort shared-user hooks");
        check(!ReconcilePackageUtils.allow(),"final preload policy remains unchanged");
        Method ancestor=SigningDetails.class.getMethod("hasCommonAncestor",SigningDetails.class);
        check(Boolean.FALSE.equals(sharedHooks.invoke(ancestor,signer,signer)),"ancestor check preserved outside installation");
        check(Boolean.TRUE.equals(InstallPackageHelper.installationFlow(()->sharedHooks.invoke(ancestor,signer,signer))),"installer shared-user ancestry hook retained");
        switches.put(Features.CP_SHARED,false);
        switches.put(Features.CP_VERIFY,true);switches.put(Features.CP_PREVIOUS,true);
        Hooks integrity=new Hooks();new CorePatchHooks(integrity,cl).install();
        Method equal=java.security.MessageDigest.class.getMethod("isEqual",byte[].class,byte[].class);
        check(integrity.installed.containsKey(equal),"digest adapter target installed");
        byte[] first={1},second={2};
        check(Boolean.FALSE.equals(integrity.invoke(equal,null,first,second)),"TLS/general-purpose unequal digests stay unequal");
        check(Boolean.TRUE.equals(InstallPackageHelper.installationFlow(()->integrity.invoke(equal,null,first,second))),"digest option affects package-install verification only");
        Method verify=PackageManagerServiceUtils.class.getMethod("verifySignatures",PackageSetting.class,PackageSetting.class,SigningDetails.class);
        check(Boolean.FALSE.equals(integrity.invoke(verify,null,new PackageSetting(new AndroidPackage()),null,signer)),"PackageManager verification bypass returns its documented no-migration result");
        Method previous=InstallPackageHelper.class.getMethod("doesSignatureMatchForPermissions",String.class,AndroidPackage.class);
        InstallPackageHelper helper=new InstallPackageHelper();AndroidPackage pkg=new AndroidPackage();
        check(Boolean.TRUE.equals(integrity.invoke(previous,helper,pkg.name,pkg)),"previous-package compatibility requires same package name");
        check(Boolean.FALSE.equals(integrity.invoke(previous,helper,"other.package",pkg)),"previous-package compatibility does not accept another package name");
        InstallPackageHelper.installationFlow(()->{
            check(!((Boolean)CorePatchHooks.originalSigning(()->CorePatchHooks.inInstallFlow())),"shared-state maintenance uses original signer checks");
            check(CorePatchHooks.inInstallFlow(),"signer-check context restored after shared-state maintenance");
            return null;
        });
        Hooks membership=new Hooks();new SharedUidStateHooks(membership,cl).install();
        SigningDetails secondSigner=new SigningDetails(new byte[]{2});
        PackageSetting firstPackage=new PackageSetting(new AndroidPackage()),secondPackage=new PackageSetting(new AndroidPackage());
        firstPackage.signatures.mSigningDetails=signer;secondPackage.signatures.mSigningDetails=secondSigner;
        SharedUserSetting shared=new SharedUserSetting();shared.signatures.mSigningDetails=signer;shared.mPackages.add(firstPackage);
        Method add=SharedUserSetting.class.getMethod("addPackage",PackageSetting.class),remove=SharedUserSetting.class.getMethod("removePackage",PackageSetting.class);
        membership.invoke(add,shared,secondPackage);
        check(shared.signatures.mSigningDetails==signer,"shared UID keeps a signing identity still used by an existing member");
        membership.invoke(remove,shared,firstPackage);
        check(shared.signatures.mSigningDetails==secondSigner,"shared UID removal repairs identity to an actual remaining member");
        SharedUserSetting platform=new SharedUserSetting();platform.uidFlags=1;platform.signatures.mSigningDetails=signer;
        membership.invoke(add,platform,secondPackage);
        check(platform.signatures.mSigningDetails==signer,"system shared-UID signing identity is never rewritten");
    }
    public static void main(String[] args) throws Throwable {
        ClassLoader cl=RegressionMain.class.getClassLoader();
        check(!Features.ready(),"no remote preferences attached at startup");
        for(String key:List.of(Features.POWER_SAVE_REFRESH,Features.POWER_SAVE_BRIGHTNESS,Features.GAME_KEEP_ENABLED,
            Features.NET_APP_CONTROLS)){
            check(!Features.enabled(key),"new optional feature must default off: "+key);
        }
        Hooks off=new Hooks(); new DisplayPolicyHooks(off,cl).install();
        check(off.installed.isEmpty(),"disabled display options must install no hooks");
        Map<String,Boolean> switches=new HashMap<>();
        switches.put(Features.POWER_SAVE_REFRESH,true);switches.put(Features.POWER_SAVE_BRIGHTNESS,true);
        Features.attach((key,fallback)->switches.getOrDefault(key,fallback));
        Hooks h=new Hooks();new DisplayPolicyHooks(h,cl).install();
        check(h.errors.isEmpty()&&h.installed.size()==2,"both display targets must resolve");
        Method update=VotesStorage.class.getMethod("updateVote",int.class,int.class,Vote.class);
        VotesStorage storage=new VotesStorage();Vote vote=new Vote();
        storage.updateVote(0,9,vote); h.invoke(update,storage,0,9,vote);
        check(!storage.votes.containsKey("0:9")&&storage.notifications==2,"remove stale saver vote through normal notification path");
        h.invoke(update,storage,0,10,vote);check(!storage.votes.containsKey("0:10"),"mode-list saver vote removed");
        h.invoke(update,storage,0,13,vote);check(storage.votes.get("0:13")==vote,"thermal vote preserved");
        h.invoke(update,storage,0,7,vote);check(storage.votes.get("0:7")==vote,"user-selected rate vote preserved");
        Method get=BatterySaverPolicy.class.getMethod("getBatterySaverPolicy",int.class);
        BatterySaverPolicy policy=new BatterySaverPolicy();
        check(h.invoke(get,policy,3)==policy.original,"network policy identity and behaviour preserved");
        PowerSaveState brightness=(PowerSaveState)h.invoke(get,policy,7);
        check(!brightness.batterySaverEnabled&&brightness.brightnessFactor==1f,"only dimming is neutralized");
        check(brightness.globalBatterySaverEnabled&&policy.original.brightnessFactor==.5f,"global state and original object preserved");
        Hooks missing=new Hooks();ClassLoader noTargets=new ClassLoader(cl){
            @Override public Class<?> loadClass(String name)throws ClassNotFoundException{
                if(name.startsWith("com.android.server"))throw new ClassNotFoundException(name);return super.loadClass(name);}
        };
        new DisplayPolicyHooks(missing,noTargets).install();
        check(missing.installed.isEmpty()&&missing.errors.size()==2,"missing ROM classes keep stock behaviour and report each target");
        DexTargets dex=new DexTargets();Method getter=Game.class.getMethod("enabled"),setter=Game.class.getMethod("setEnabled",boolean.class);
        dex.results.put("game_keep_enabled_get_v1",List.of(getter));dex.results.put("game_keep_enabled_set_v1",List.of(setter));
        Hooks gameHooks=new Hooks();new SecurityBehaviorHooks(gameHooks).keepGameEnabled(dex);Game game=new Game();
        gameHooks.invoke(setter,game,false);check(game.stored&&(Boolean)gameHooks.invoke(getter,game),"master getter and persistent setter agree");
        gameHooks.invoke(Game.class.getMethod("activeSession",boolean.class),game,false);
        check(!game.stored,"ending a real game session still runs");
        DexTargets ambiguous=new DexTargets();ambiguous.results.put("power_save_brightness_entry_v1",List.of(
            Game.class.getMethod("enter",Context.class),Game.class.getMethod("ambiguous",Context.class)));
        Hooks noGuess=new Hooks();try{new SecurityBehaviorHooks(noGuess).keepBrightness(ambiguous);throw new AssertionError("ambiguous target accepted");}
        catch(NoSuchMethodException expected){check(noGuess.installed.isEmpty(),"ambiguous scans must install no guessed hook");}
        Hooks network=new Hooks();new NetworkControlHooks(network,cl).install();
        Method init=ShowAppDetailFragment.class.getMethod("initFirewallData");
        ShowAppDetailFragment details=new ShowAppDetailFragment();AppInfo original=new AppInfo(12345,"ordinary.system.app");details.mAppInfo=original;
        network.invoke(init,details);check(!details.seenSystem&&details.mAppInfo==original&&original.isSystemApp,"local copy enables UI without mutating shared app identity");
        details.throwDuringInit=true;try{network.invoke(init,details);}catch(IllegalStateException expected){}
        check(details.mAppInfo==original&&original.isSystemApp,"host exceptions must restore original app info");details.throwDuringInit=false;
        for(AppInfo protectedApp:List.of(new AppInfo(1000,"android"),new AppInfo(112345,"profile.app"),
            new AppInfo(12345,"protected.firewall"),new AppInfo(12345,"protected.background"))){
            details.mAppInfo=protectedApp;network.invoke(init,details);check(details.seenSystem,"protected/profile identity preserved: "+protectedApp.packageName);}
        details.mAppInfo=original;details.mIsManagedProfileApp=true;network.invoke(init,details);
        check(details.seenSystem,"managed-profile policy preserved");
        WlanFirewallFragment wlan=new WlanFirewallFragment();wlan.existing.add(original);
        wlan.mAppMonitorWrapper.system.addAll(List.of(original,new AppInfo(12346,"extra.app"),new AppInfo(1000,"android")));
        Object list=network.invoke(WlanFirewallFragment.class.getMethod("getAppList"),wlan);
        check(((List<?>)list).size()==2&&wlan.existing.size()==1,"WLAN list deduplicates and leaves shared lists untouched");

        corePatchChecks(cl, switches);
        System.out.println("PASS: "+checks+" hook isolation/regression checks");
    }
}
""",
}


def main():
    fixtures = dict(FIXTURES)
    fixtures['android/content/SharedPreferences.java'] = fixtures['android/content/SharedPreferences.java'].replace(
        'boolean getBoolean(String key, boolean fallback);',
        'boolean getBoolean(String key, boolean fallback); default java.util.Set<String> getStringSet(String k, java.util.Set<String> d){return d;}')
    fixtures['io/github/leekin2/xsecurity/Hooks.java'] = fixtures['io/github/leekin2/xsecurity/Hooks.java'].replace(
        'void deoptimize(Executable m){}', 'void deoptimize(Executable m){} boolean deoptimizeReported(Executable m){return true;}')
    with tempfile.TemporaryDirectory(prefix="xsecurity-hook-tests-") as directory:
        temp = Path(directory)
        sources = []
        for name, source in fixtures.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")
            sources.append(str(path))
        sources.extend(str(PRODUCTION / name) for name in (
            "Features.java", "DisplayPolicyHooks.java", "SecurityBehaviorHooks.java", "NetworkControlHooks.java",
            "CorePatchHooks.java", "ApkIntegrityHooks.java", "SharedUidStateHooks.java", "PersistentUpdateHooks.java", "SystemUiUidHooks.java"))
        classes = temp / "classes"
        classes.mkdir()
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(classes), *sources], check=True)
        subprocess.run(["java", "-cp", str(classes), "io.github.leekin2.xsecurity.RegressionMain"], check=True)

    # A toggle without its text/feature registration is invisible or crashes one UI language.
    texts = (ROOT / "app/src/main/kotlin/io/github/leekin2/xsecurity/ui/ExtensionTexts.kt").read_text(encoding="utf-8")
    entries = re.findall(r"F\.([A-Z_]+) to Line\(", texts)
    assert entries and all(entries.count(key) == 5 for key in set(entries))
    model = (ROOT / "app/src/main/kotlin/io/github/leekin2/xsecurity/ui/Model.kt").read_text(encoding="utf-8")
    for key in set(entries):
        assert f"Item(F.{key}," in model, key
    config = (ROOT / "app/src/main/resources/META-INF/xposed/module.prop").read_text()
    scope = (ROOT / "app/src/main/resources/META-INF/xposed/scope.list").read_text().splitlines()
    assert "staticScope=true" in config and "system" in scope and "android" not in scope
    core_texts = (ROOT / "app/src/main/kotlin/io/github/leekin2/xsecurity/ui/CorePatchTexts.kt").read_text(encoding="utf-8")
    assert core_texts.count('Line(') == 12 * 5
    assert 'F.CORE_PATCH.map' in model
    for filename in ("DeveloperInstallHooks.java", "DeveloperInstallPolicy.java", "BackgroundCleanerHooks.java", "AutostartGrantHooks.java"):
        assert not (PRODUCTION / filename).exists(), filename
    assert 'Section("background"' not in model and 'Section("developer"' not in model
    print("PASS: twelve install options, five languages, retired code deleted, fixed system scope")


if __name__ == "__main__":
    main()
