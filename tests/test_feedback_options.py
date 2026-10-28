"""Run the new messaging/launcher hooks against host contracts, without real SMS or app launches."""
from pathlib import Path
import subprocess
import tempfile
from test_dark_mode_hooks import SOURCES as BASE

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {n: BASE[n] for n in ('android/content/SharedPreferences.java', 'io/github/libxposed/api/XposedInterface.java', 'io/github/leekin2/xsecurity/Hooks.java')}
SOURCES.update({
'android/content/Context.java': '''package android.content;
public class Context {public static final String CLIPBOARD_SERVICE="clipboard";public String pkg="com.android.mms";
 public ClipboardManager clipboard=new ClipboardManager();public String getPackageName(){return pkg;}public Object getSystemService(String s){return clipboard;}}
''',
'android/content/Intent.java': '''package android.content;
public class Intent {public String text, function="function_copy";public boolean verified=true;
 public Intent(String t){text=t;}public boolean getBooleanExtra(String k,boolean d){return k.equals("is_verification_code")?verified:d;}
 public String getStringExtra(String k){return k.equals("extra_text")?text:k.equals("function_type")?function:null;}}
''',
'android/net/Uri.java': 'package android.net;public class Uri {}',
'com/android/mms/storage/DevicesUtil.java': '''package com.android.mms.storage;
public class DevicesUtil {public static boolean isOldDevice(){return true;}}
''',
'com/android/mms/transaction/VerificationCodeCopyReceiver.java': '''package com.android.mms.transaction;
import android.content.*;public class VerificationCodeCopyReceiver {
 public static Intent factory(int id,Context c,android.net.Uri u,String code,String sender){return new Intent(code);}}
''',
'android/content/ClipData.java': '''package android.content;
public class ClipData {public String text;public Description d=new Description();public Description getDescription(){return d;}
 public static ClipData newPlainText(CharSequence l,CharSequence t){ClipData c=new ClipData();c.text=t.toString();return c;}
 public static class Description {public android.os.PersistableBundle extras;public void setExtras(android.os.PersistableBundle b){extras=b;}}}
''',
'android/content/ClipboardManager.java': '''package android.content;
public class ClipboardManager {public int writes;public ClipData clip;public boolean deny;
 public void setPrimaryClip(ClipData c){if(deny)throw new IllegalStateException("denied");writes++;clip=c;}}
''',
'android/os/PersistableBundle.java': '''package android.os;
public class PersistableBundle {public java.util.Map<String,Boolean> values=new java.util.HashMap<>();public void putBoolean(String k,boolean v){values.put(k,v);}}
''',
'android/os/Bundle.java': 'package android.os;public class Bundle {}',
'android/app/PendingIntent.java': '''package android.app;import android.content.*;
public class PendingIntent {public static boolean fail;
 public static PendingIntent getActivity(Context c,int n,Intent i,int f){if(fail)throw new IllegalStateException("host");return new PendingIntent();}
 public static PendingIntent getActivity(Context c,int n,Intent i,int f,android.os.Bundle b){return getActivity(c,n,i,f);}
 public static PendingIntent getBroadcast(Context c,int n,Intent i,int f){return getActivity(c,n,i,f);}
 public static PendingIntent getService(Context c,int n,Intent i,int f){return getActivity(c,n,i,f);}}
''',
'org/luckypray/dexkit/query/matchers/MethodMatcher.java': '''package org.luckypray.dexkit.query.matchers;
public class MethodMatcher {public static MethodMatcher create(){return new MethodMatcher();}public MethodMatcher usingStrings(String...s){return this;}
public MethodMatcher returnType(String s){return this;}public MethodMatcher paramCount(int n){return this;}}
''',
'io/github/leekin2/xsecurity/DexTargets.java': '''package io.github.leekin2.xsecurity;import java.util.*;import java.lang.reflect.*;
final class DexTargets implements AutoCloseable {public static final Map<String,List<Method>> results=new HashMap<>();
 DexTargets(ClassLoader l,String a,String d){}interface Query{org.luckypray.dexkit.query.matchers.MethodMatcher build();}
 List<Method> usingStrings(String k,String t,String...s){return results.getOrDefault(k,List.of());}
 List<Method> named(String k,String n){return results.getOrDefault(k,List.of());}
 List<Method> matching(String k,Query q){q.build();return results.getOrDefault(k,List.of());}public void close(){}}
''',
'com/miui/smsextra/sdk/SmartContact.java': '''package com.miui.smsextra.sdk;
public class SmartContact {public String mRiskType="";public boolean isRiskyNumber(){return true;}public boolean isDefraudNumber(){return true;}}
''',
'com/miui/smsextra/internal/sdk/xiaomi/YellowPagePhone.java': '''package com.miui.smsextra.internal.sdk.xiaomi;
public class YellowPagePhone {public int mNumberType=12;public boolean isRiskyNumber(){return mNumberType==11;}
 public boolean isDefraudNumber(){return mNumberType==12;}}
''',
'com/miui/smsextra/ui/BottomMenu.java': '''package com.miui.smsextra.ui;import android.content.Context;
public class BottomMenu {public static boolean allowMenuMode(Context c){return true;}public boolean hidden;
public void setHideButton(boolean b){hidden=b;}public void setHideButton(String s){throw new AssertionError("wrong overload");}}
''',
'com/android/mms/ContactFactory.java': '''package com.android.mms;import com.miui.smsextra.sdk.SmartContact;
public class ContactFactory {public static String next;public static void fill(SmartContact c){c.mRiskType=next;}public static boolean old(){return true;}}
''',
'com/miui/launcher/utils/SystemProperties.java': '''package com.miui.launcher.utils;
public class SystemProperties {public static boolean getBoolean(String k,boolean d){return d;}}
''',
'com/miui/home/launcher/ShortcutInfo.java': '''package com.miui.home.launcher;
public class ShortcutInfo {public String getInstallerPackageName(){return "original.store";}}
''',
'com/miui/home/launcher/uninstall/BaseUninstallDialog.java': '''package com.miui.home.launcher.uninstall;import java.util.*;import android.content.Context;
public class BaseUninstallDialog {public static Runnable during;public boolean fail;
public void init(Context c,List l){if(during!=null)during.run();if(fail)throw new IllegalStateException("dialog");}
public void init(Context c,List l,String s){init(c,l);}public void init(String unrelated){throw new AssertionError();}}
''',
'io/github/leekin2/xsecurity/FeedbackMain.java': '''package io.github.leekin2.xsecurity;
import java.util.*;import java.lang.reflect.*;import android.content.*;import android.app.*;
import com.miui.smsextra.sdk.*;import com.miui.smsextra.ui.*;import com.android.mms.*;
import com.miui.home.launcher.*;import com.miui.home.launcher.uninstall.*;
public class FeedbackMain {
static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
public static void main(String[] args)throws Throwable {
Map<String,Boolean> flags=new HashMap<>();SharedPreferences prefs=new SharedPreferences(){public boolean getBoolean(String k,boolean d){return flags.getOrDefault(k,d);}};Features.attach(prefs);
ClassLoader cl=FeedbackMain.class.getClassLoader();
check(!Features.read(prefs,Features.APP_INFO_BUTTONS),"fresh install leaves header buttons off");
flags.put(Features.APP_INFO,true);check(Features.read(prefs,Features.APP_INFO_BUTTONS),"existing master-on keeps header before migration");
flags.put(Features.APP_INFO_BUTTONS,false);check(!Features.read(prefs,Features.APP_INFO_BUTTONS)&&Features.enabled(Features.APP_INFO),"explicit button off preserves master actions");
check(!Features.enabled(Features.APP_ICON_LAUNCH),"icon launch independent and off by default");
for(String key:new String[]{Features.MMS_ADS,Features.MMS_FRAUD,Features.MMS_OVERSEAS,Features.MMS_OLD_DEVICE,Features.MMS_COPY_CODE,Features.HOME_PRESTART,Features.HOME_REPORT,Features.APP_ICON_LAUNCH})check(!Features.legacyDefault(key),"no new behavior on legacy upgrade");
Hooks off=new Hooks();new MessageHooks(off,cl).install("fake",null);new HomeHooks(off,cl).install();check(off.installed.isEmpty(),"all new hooks off by default");
flags.put(Features.MMS_OVERSEAS,true);check(MessageHooks.suppressRisk("11")&&!MessageHooks.suppressRisk("12")&&!MessageHooks.suppressRisk("99"),"overseas does not suppress fraud or unknown risk");
flags.put(Features.MMS_OVERSEAS,false);flags.put(Features.MMS_FRAUD,true);check(MessageHooks.suppressRisk(new String("12"))&&!MessageHooks.suppressRisk("11")&&!MessageHooks.suppressRisk(null),"fraud compares content and preserves overseas");
Method fill=ContactFactory.class.getMethod("fill",SmartContact.class);DexTargets.results.put("mms_smart_contact_v1",List.of(fill));
flags.put(Features.MMS_ADS,true);flags.put(Features.MMS_COPY_CODE,true);flags.put(Features.MMS_OLD_DEVICE,true);
DexTargets.results.put("mms_old_device_bool_v2",List.of(ContactFactory.class.getMethod("old")));
DexTargets.results.put("mms_hide_promo_v1",List.of(BottomMenu.class.getMethod("setHideButton",boolean.class),BottomMenu.class.getMethod("setHideButton",String.class)));
Hooks sms=new Hooks();new MessageHooks(sms,cl).install("fake",null);
SmartContact c=new SmartContact();ContactFactory.next="11";sms.invoke(fill,null,c);check(c.mRiskType.equals("11"),"other risk label retained after factory");
ContactFactory.next="12";sms.invoke(fill,null,c);check(c.mRiskType.equals(""),"chosen fraud label removed after factory");
Method risky=SmartContact.class.getMethod("isRiskyNumber");c.mRiskType="99";check((Boolean)sms.invoke(risky,c),"generic model risk is not globally bypassed");
c.mRiskType="12";check((Boolean)sms.invoke(risky,c),"fraud switch does not replace overseas getter");
Method fraud=SmartContact.class.getMethod("isDefraudNumber");check(!(Boolean)sms.invoke(fraud,c),"selected fraud getter hidden");
var yellow=new com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone();
check(!(Boolean)sms.invoke(yellow.getClass().getMethod("isDefraudNumber"),yellow),"integer risk model supported");
yellow.mNumberType=11;check((Boolean)sms.invoke(yellow.getClass().getMethod("isRiskyNumber"),yellow),"integer overseas label retained independently");
flags.put(Features.MMS_OVERSEAS,true);check(!(Boolean)sms.invoke(risky,c),"overseas getter follows its own switch");flags.put(Features.MMS_OVERSEAS,false);
BottomMenu menu=new BottomMenu();sms.invoke(BottomMenu.class.getMethod("setHideButton",boolean.class),menu,false);check(menu.hidden,"promotional setter receives true");
check(!sms.installed.containsKey(BottomMenu.class.getMethod("setHideButton",String.class)),"wrong setter overload skipped");
Method device=com.android.mms.storage.DevicesUtil.class.getMethod("isOldDevice");
check(!(Boolean)sms.invoke(device,null),"actual named device gate adapted");
flags.put(Features.MMS_OLD_DEVICE,false);check((Boolean)sms.invoke(device,null),"disabled device gate restores stock result");flags.put(Features.MMS_OLD_DEVICE,true);
Method intent=PendingIntent.class.getMethod("getActivity",Context.class,int.class,Intent.class,int.class);
Context context=new Context();sms.invoke(intent,null,context,0,new Intent("123456"),0);check(context.clipboard.writes==1&&context.clipboard.clip.text.equals("123456"),"copies only numeric notification code");
check(Boolean.TRUE.equals(context.clipboard.clip.d.extras.values.get("android.content.extra.IS_SENSITIVE")),"clipboard marked sensitive");
sms.invoke(intent,null,context,0,new Intent("123456"),0);check(context.clipboard.writes==1,"duplicate notification code does not rewrite clipboard");
for(String text:new String[]{"full message code 123456","http://test/1234","123","123456789","AB1234",""})sms.invoke(intent,null,context,0,new Intent(text),0);
check(context.clipboard.writes==1,"message bodies URLs and noncodes never copied");
context.pkg="other.app";sms.invoke(intent,null,context,0,new Intent("445566"),0);check(context.clipboard.writes==1,"non-Messages caller ignored");context.pkg="com.android.mms";
context.clipboard.deny=true;Object answer=sms.invoke(intent,null,context,0,new Intent("445566"),0);check(answer instanceof PendingIntent&&context.clipboard.writes==1,"clipboard denial preserves notification result");context.clipboard.deny=false;
PendingIntent.fail=true;try{sms.invoke(intent,null,context,0,new Intent("445566"),0);throw new AssertionError();}catch(IllegalStateException e){check(e.getMessage().equals("host"),"host PendingIntent failure retained");}PendingIntent.fail=false;
Method intent5=PendingIntent.class.getMethod("getActivity",Context.class,int.class,Intent.class,int.class,android.os.Bundle.class);
sms.invoke(intent5,null,context,0,new Intent(" 7788 "),0,null);check(context.clipboard.writes==2&&context.clipboard.clip.text.equals("7788"),"five-argument intent trims four-digit code");
sms.invoke(intent5,null,context,0,new Intent("11223344"),0,null);check(context.clipboard.writes==3&&context.clipboard.clip.text.equals("11223344"),"eight-digit code supported");
flags.put(Features.MMS_COPY_CODE,false);sms.invoke(intent,null,context,0,new Intent("445566"),0);check(context.clipboard.writes==3,"disabled copy leaves clipboard unchanged");
flags.put(Features.MMS_COPY_CODE,true);
Intent unverified=new Intent("445566");unverified.verified=false;sms.invoke(intent,null,context,0,unverified,0);
Intent todo=new Intent("445566");todo.function="ADD_TODO";sms.invoke(intent,null,context,0,todo,0);
check(context.clipboard.writes==3,"unmarked numeric text and todo actions are never copied");
Method factory=com.android.mms.transaction.VerificationCodeCopyReceiver.class.getMethod("factory",int.class,Context.class,android.net.Uri.class,String.class,String.class);
Object built=sms.invoke(factory,null,7,context,null,"556677","sender");
check(built instanceof Intent&&context.clipboard.writes==4&&context.clipboard.clip.text.equals("556677"),"actual Messages 18 factory copies extracted code and preserves intent");
Method broadcast=PendingIntent.class.getMethod("getBroadcast",Context.class,int.class,Intent.class,int.class);
sms.invoke(broadcast,null,context,0,built,0);check(context.clipboard.writes==4,"factory and broadcast deduplicate same notification");
sms.invoke(broadcast,null,context,0,new Intent("889900"),0);check(context.clipboard.writes==5,"broadcast-only path supported");
check(Boolean.TRUE.equals(context.clipboard.clip.d.extras.values.get("mms_is_ververification_code")),"OEM clipboard marker preserved");
flags.put(Features.MMS_ADS,false);menu.hidden=false;sms.invoke(BottomMenu.class.getMethod("setHideButton",boolean.class),menu,false);check(!menu.hidden,"ads off restores original setter args");
flags.put(Features.MMS_FRAUD,false);c.mRiskType="12";check((Boolean)sms.invoke(risky,c),"risk toggles off restore original result");
flags.put(Features.HOME_PRESTART,true);flags.put(Features.HOME_REPORT,true);Hooks home=new Hooks();new HomeHooks(home,cl).install();
Method prop=com.miui.launcher.utils.SystemProperties.class.getMethod("getBoolean",String.class,boolean.class);
check(!(Boolean)home.invoke(prop,null,"persist.sys.prestart.proc",true),"launcher preload reads disabled");
check((Boolean)home.invoke(prop,null,"unrelated.property",true),"unrelated property unchanged");
check((Boolean)home.invoke(prop,null,null,true),"null keys keep stock behavior");
Method getter=ShortcutInfo.class.getMethod("getInstallerPackageName");ShortcutInfo shortcut=new ShortcutInfo();
check(home.invoke(getter,shortcut).equals("original.store"),"installer outside uninstall unchanged");
BaseUninstallDialog.during=()->{try{check(home.invoke(getter,shortcut).equals("com.xiaomi.market"),"installer override only during dialog");}catch(Throwable t){throw new RuntimeException(t);}};
Method init=BaseUninstallDialog.class.getMethod("init",Context.class,List.class);BaseUninstallDialog dialog=new BaseUninstallDialog();home.invoke(init,dialog,context,List.of());
check(home.invoke(getter,shortcut).equals("original.store"),"dialog exit restores installer behavior");
dialog.fail=true;try{home.invoke(init,dialog,context,List.of());throw new AssertionError();}catch(IllegalStateException e){check(e.getMessage().equals("dialog"),"dialog exception retained");}
check(home.invoke(getter,shortcut).equals("original.store"),"exception clears dialog scope");
flags.put(Features.HOME_REPORT,false);BaseUninstallDialog.during=()->{try{check(home.invoke(getter,shortcut).equals("original.store"),"report switch off preserves installer inside dialog");}catch(Throwable t){throw new RuntimeException(t);}};dialog.fail=false;home.invoke(init,dialog,context,List.of());
flags.put(Features.HOME_PRESTART,false);check((Boolean)home.invoke(prop,null,"persist.sys.prestart.proc",true),"preload off preserves original read");
flags.put(Features.HOME_REPORT,true);flags.put(Features.HOME_PRESTART,true);Hooks missing=new Hooks();new HomeHooks(missing,new ClassLoader(cl){public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.startsWith("com.miui."))throw new ClassNotFoundException(n);return super.loadClass(n);}}).install();check(missing.installed.isEmpty()&&missing.errors.size()==2,"unsupported launcher installs no hooks");
flags.put(Features.MMS_FRAUD,false);flags.put(Features.MMS_OVERSEAS,false);flags.put(Features.MMS_OLD_DEVICE,true);flags.put(Features.MMS_ADS,false);
flags.put(Features.MMS_COPY_CODE,false);
DexTargets.results.put("mms_old_device_bool_v2",List.of(ContactFactory.class.getMethod("old"),ContactFactory.class.getMethod("old")));
ClassLoader noNamedGate=new ClassLoader(cl){public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.equals("com.android.mms.storage.DevicesUtil"))throw new ClassNotFoundException(n);return super.loadClass(n);}};
Hooks ambiguous=new Hooks();new MessageHooks(ambiguous,noNamedGate).install("fake",null);check(!ambiguous.installed.containsKey(ContactFactory.class.getMethod("old")),"ambiguous device string match skipped");
check(new HashSet<>(Arrays.asList(Features.ALL)).size()==Features.ALL.length,"no duplicate feature keys");
System.out.println("PASS: "+checks+" feedback feature checks");
}}
'''
})

def main():
    with tempfile.TemporaryDirectory(prefix='xsecurity-feedback-') as directory:
        temp = Path(directory)
        files = []
        for name, source in SOURCES.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding='utf-8')
            files.append(str(path))
        files += [str(ROOT/'app/src/main/java/io/github/leekin2/xsecurity'/name) for name in ('Features.java','MessageHooks.java','HomeHooks.java')]
        subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes'),*files],check=True)
        subprocess.run(['java','-cp',str(temp/'classes'),'io.github.leekin2.xsecurity.FeedbackMain'],check=True)
    text=(ROOT/'app/src/main/kotlin/io/github/leekin2/xsecurity/ui/FeedbackTexts.kt').read_text(encoding='utf-8')
    assert text.count(' to Line(')==55
    scopes=(ROOT/'app/src/main/resources/META-INF/xposed/scope.list').read_text().splitlines()
    assert len(set(scopes))==len(scopes)==12
    print('PASS: 55 translated feature descriptions and 12 unique scopes')

if __name__ == '__main__': main()
