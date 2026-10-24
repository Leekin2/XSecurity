"""SystemUI-only parsed UID preservation with boot settings, fresh and cached parses."""
from pathlib import Path
import subprocess
import tempfile
from test_dark_mode_hooks import SOURCES as BASE

ROOT=Path(__file__).resolve().parents[1]
SOURCES={name:BASE[name] for name in ('android/content/SharedPreferences.java','io/github/libxposed/api/XposedInterface.java','io/github/leekin2/xsecurity/Hooks.java')}
SOURCES.update({
 'com/android/server/pm/PackageSetting.java':'''package com.android.server.pm;
public class PackageSetting {public boolean system=true;public boolean isSystem(){return system;}}
''',
 'com/android/server/pm/SharedUserSetting.java':'''package com.android.server.pm;
public class SharedUserSetting {public final String name;public SharedUserSetting(String n){name=n;}}
''',
 'com/android/server/pm/Settings.java':'''package com.android.server.pm;
public class Settings {
 public PackageSetting current=new PackageSetting();public SharedUserSetting shared=new SharedUserSetting("android.uid.system");
 public boolean fail;
 PackageSetting getPackageLPr(String name){return name.equals("com.android.systemui")?current:null;}
 SharedUserSetting getSharedUserSettingLPr(String name){return shared;}
 public boolean readLPw(boolean success){if(fail)throw new IllegalStateException("settings host");return success;}
}
''',
 'com/android/internal/pm/parsing/pkg/PackageImpl.java':'''package com.android.internal.pm.parsing.pkg;
public class PackageImpl {
 public String name="com.android.systemui",path="/data/app/vmdl123.tmp",sharedUserId="android.uid.systemui";
 public boolean leaving;
 public String getPackageName(){return name;}public String getPath(){return path;}
 public String getSharedUserId(){return sharedUserId;}public boolean isLeavingSharedUser(){return leaving;}
 public PackageImpl setSharedUserId(String uid){sharedUserId=uid;return this;}
}
''',
 'com/android/internal/pm/parsing/PackageParser2.java':'''package com.android.internal.pm.parsing;
public class PackageParser2 {
 public com.android.internal.pm.parsing.pkg.PackageImpl next=new com.android.internal.pm.parsing.pkg.PackageImpl();
 public boolean fail;
 public com.android.internal.pm.parsing.pkg.PackageImpl parsePackage(java.io.File file,int flags,boolean cache){
  if(fail)throw new IllegalStateException("parser host");return next;}
}
''',
 'io/github/leekin2/xsecurity/UidModuleMain.java':'''package io.github.leekin2.xsecurity;
import java.lang.reflect.*;import java.util.*;import java.io.*;import android.content.*;
import com.android.server.pm.*;import com.android.internal.pm.parsing.*;import com.android.internal.pm.parsing.pkg.*;
public class UidModuleMain {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Throwable {
  Map<String,Boolean> flags=new HashMap<>();Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean d){return flags.getOrDefault(k,d);}});
  check(!Features.defaultValue(Features.CP_SYSTEM_UI_UID)&&!Features.legacyDefault(Features.CP_SYSTEM_UI_UID),"new behavior off for new and migrating installs");
  ClassLoader cl=UidModuleMain.class.getClassLoader();Hooks off=new Hooks();new SystemUiUidHooks(off,cl).install();
  check(off.installed.isEmpty(),"default off installs nothing");flags.put(Features.CP_SYSTEM_UI_UID,true);
  Hooks h=new Hooks();new SystemUiUidHooks(h,cl).install();check(h.installed.size()==3,"only boot read, setter and parser hooks registered");
  Method read=Settings.class.getMethod("readLPw",boolean.class),set=PackageImpl.class.getMethod("setSharedUserId",String.class);
  Method parse=PackageParser2.class.getMethod("parsePackage",File.class,int.class,boolean.class);
  PackageImpl uninitialized=new PackageImpl();h.invoke(set,uninitialized,"android.uid.systemui");
  check(uninitialized.sharedUserId.equals("android.uid.systemui"),"no invented UID before settings load");
  Settings settings=new Settings();h.invoke(read,settings,true);PackageImpl fresh=new PackageImpl();
  check(h.invoke(set,fresh,"android.uid.systemui")==fresh&&fresh.sharedUserId.equals("android.uid.system"),"actual setter and its return value retain installed identity");
  PackageParser2 parser=new PackageParser2();PackageImpl cached=parser.next;
  check(h.invoke(parse,parser,new File("ignored"),0,true)==cached&&cached.sharedUserId.equals("android.uid.system"),"cached parsed object normalized in place");
  parser.next=new PackageImpl();parser.next.sharedUserId="vendor.uid.future_systemui";
  h.invoke(parse,parser,new File("ignored"),0,false);
  check(parser.next.sharedUserId.equals("android.uid.system"),"future declaration uses installed identity rather than a fixed incoming APK/version");
  for(String name:List.of("example.app","android","com.android.settings")){
   PackageImpl other=new PackageImpl();other.name=name;h.invoke(set,other,"vendor.uid.other");
   check(other.sharedUserId.equals("vendor.uid.other"),"unrelated package unchanged: "+name);
  }
  for(String path:List.of("/system/priv-app/SystemUI","/system_ext/priv-app/SystemUI","/data/local/tmp/base.apk","/data/application/base.apk")){
   PackageImpl stock=new PackageImpl();stock.path=path;h.invoke(set,stock,"android.uid.systemui");
   check(stock.sharedUserId.equals("android.uid.systemui"),"non-update path untouched: "+path);
  }
  PackageImpl departing=new PackageImpl();departing.leaving=true;h.invoke(set,departing,"android.uid.systemui");
  check(departing.sharedUserId.equals("android.uid.systemui"),"explicit shared-user departure retains stock handling");
  PackageImpl noUid=new PackageImpl();h.invoke(set,noUid,(Object)null);check(noUid.sharedUserId==null,"no joining a shared UID from a null declaration");
  h.invoke(set,noUid,"");check(noUid.sharedUserId.isEmpty(),"blank declaration unchanged");
  settings.shared=new SharedUserSetting("vendor.uid.existing");h.invoke(read,settings,true);fresh=new PackageImpl();h.invoke(set,fresh,"android.uid.systemui");
  check(fresh.sharedUserId.equals("vendor.uid.existing"),"existing identity comes from this phone; android.uid.system is not hardcoded");
  settings.current.system=false;h.invoke(read,settings,true);fresh=new PackageImpl();h.invoke(set,fresh,"android.uid.systemui");
  check(fresh.sharedUserId.equals("android.uid.systemui"),"non-system package cannot borrow system identity");
  settings.current=null;h.invoke(read,settings,true);fresh=new PackageImpl();h.invoke(set,fresh,"android.uid.systemui");
  check(fresh.sharedUserId.equals("android.uid.systemui"),"fresh installs do not receive a shared UID");
  settings.current=new PackageSetting();settings.shared=null;h.invoke(read,settings,true);fresh=new PackageImpl();h.invoke(set,fresh,"android.uid.systemui");
  check(fresh.sharedUserId.equals("android.uid.systemui"),"missing old shared-user identity unchanged");
  settings.shared=new SharedUserSetting("android.uid.system");h.invoke(read,settings,true);h.invoke(read,settings,false);fresh=new PackageImpl();h.invoke(set,fresh,"android.uid.systemui");
  check(fresh.sharedUserId.equals("android.uid.systemui"),"failed settings load clears previous identity");
  h.invoke(read,settings,true);flags.put(Features.CP_SYSTEM_UI_UID,false);fresh=new PackageImpl();h.invoke(set,fresh,"android.uid.systemui");
  check(fresh.sharedUserId.equals("android.uid.systemui"),"runtime off restores setter behavior");
  parser.next=new PackageImpl();h.invoke(parse,parser,new File("ignored"),0,true);
  check(parser.next.sharedUserId.equals("android.uid.systemui"),"runtime off restores cached parse behavior");
  flags.put(Features.CP_SYSTEM_UI_UID,true);parser.fail=true;
  try{h.invoke(parse,parser,new File("ignored"),0,false);throw new AssertionError("host failure swallowed");}
  catch(IllegalStateException expected){check(expected.getMessage().equals("parser host"),"host parser exception retained");}
  settings.fail=true;
  try{h.invoke(read,settings,true);throw new AssertionError("settings failure swallowed");}
  catch(IllegalStateException expected){check(expected.getMessage().equals("settings host"),"host settings failure retained");}
  Hooks missing=new Hooks();try{new SystemUiUidHooks(missing,new ClassLoader(cl){public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.endsWith("PackageParser2"))throw new ClassNotFoundException(n);return super.loadClass(n);}}).install();throw new AssertionError("missing target accepted");}
  catch(ClassNotFoundException expected){check(missing.installed.isEmpty(),"required targets resolved before registration");}
  Hooks rebooted=new Hooks();new SystemUiUidHooks(rebooted,cl).install();Settings boot=new Settings();
  PackageParser2 fromOriginalApk=new PackageParser2();
  check(fromOriginalApk.next.sharedUserId.equals("android.uid.systemui"),"incoming APK declaration itself remains unchanged");
  rebooted.invoke(read,boot,true);rebooted.invoke(parse,fromOriginalApk,new File("ignored"),0,true);
  check(fromOriginalApk.next.sharedUserId.equals("android.uid.system"),"fresh module process restores installed identity after simulated reboot");
  check(boot.shared.name.equals("android.uid.system")&&boot.current.system,"installed UID record and system-app state untouched");
  check(h.errors.isEmpty(),"no normal metadata mutation errors");
  System.out.println("PASS: "+checks+" module UID preservation checks");
 }
}
''',
})

def main():
    with tempfile.TemporaryDirectory(prefix='xsecurity-uid-module-') as folder:
        temp=Path(folder)
        files=[]
        for name,source in SOURCES.items():
            path=temp/name
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(source,encoding='utf-8')
            files.append(str(path))
        production=ROOT/'app/src/main/java/io/github/leekin2/xsecurity'
        files += [str(production/name) for name in ('Features.java','SystemUiUidHooks.java')]
        subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes'),*files],check=True)
        subprocess.run(['java','-cp',str(temp/'classes'),'io.github.leekin2.xsecurity.UidModuleMain'],check=True)
    text=(ROOT/'app/src/main/kotlin/io/github/leekin2/xsecurity/ui/CorePatchTexts.kt').read_text(encoding='utf-8')
    assert text.count('Line(')==60
    feature=(ROOT/'app/src/main/java/io/github/leekin2/xsecurity/Features.java').read_text()
    assert 'CP_PERSISTENT, CP_SYSTEM_UI_UID};' in feature
    model=(ROOT/'app/src/main/kotlin/io/github/leekin2/xsecurity/ui/Model.kt').read_text()
    assert 'F.CORE_PATCH.map' in model
    print('PASS: all 12 installation options have five-language text and UI registration')

if __name__=='__main__':main()
