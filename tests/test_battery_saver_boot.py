"""Exercise the OEM boot-close correction without rebooting or changing power state."""
from pathlib import Path
import subprocess
import tempfile
from test_feedback_options import SOURCES as BASE

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {k: BASE[k] for k in ('android/content/SharedPreferences.java',
    'io/github/libxposed/api/XposedInterface.java', 'io/github/leekin2/xsecurity/Hooks.java',
    'io/github/leekin2/xsecurity/DexTargets.java', 'org/luckypray/dexkit/query/matchers/MethodMatcher.java')}
SOURCES.update({
    'android/content/Context.java': 'package android.content;public class Context {}',
    'com/miui/securitycenter/receiver/BootReceiver.java': '''package com.miui.securitycenter.receiver;
public class BootReceiver {public int checks;public void close(android.content.Context c){checks++;}
 public void unrelated(android.content.Context c){checks++;}public void wrong(String c){checks++;}}''',
    'com/miui/powercenter/SaverPreferences.java': '''package com.miui.powercenter;
public class SaverPreferences {public static boolean autoExit=true, fail;
 public static boolean autoExit(){if(fail)throw new IllegalStateException("unavailable");return autoExit;}}''',
    'io/github/leekin2/xsecurity/BatteryBootMain.java': '''package io.github.leekin2.xsecurity;
import java.util.*;import java.lang.reflect.*;import android.content.*;
import com.miui.securitycenter.receiver.BootReceiver;import com.miui.powercenter.SaverPreferences;
public class BatteryBootMain {static int checks;static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
public static void main(String[] a)throws Throwable {
 Map<String,Boolean> flags=new HashMap<>();Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean d){return flags.getOrDefault(k,d);}});
 check(!Features.enabled(Features.POWER_SAVE_BOOT)&&!Features.legacyDefault(Features.POWER_SAVE_BOOT),"new and upgraded installs do not silently change saver behavior");
 Method boot=BootReceiver.class.getMethod("close",Context.class), unrelated=BootReceiver.class.getMethod("unrelated",Context.class);
 Method pref=SaverPreferences.class.getMethod("autoExit");
 DexTargets.results.put("battery_saver_boot_close_v1",List.of(boot));DexTargets.results.put("battery_saver_auto_exit_v1",List.of(pref));
 Hooks hooks=new Hooks();new BatterySaverBootHooks(hooks).install(new DexTargets(null,null,null));
 check(hooks.installed.size()==1&&hooks.installed.containsKey(boot),"only exact BootReceiver helper is hooked");
 BootReceiver receiver=new BootReceiver();Context c=new Context();
 SaverPreferences.autoExit=false;hooks.invoke(boot,receiver,c);check(receiver.checks==1,"feature off retains original boot check");
 flags.put(Features.POWER_SAVE_BOOT,true);hooks.invoke(boot,receiver,c);check(receiver.checks==1,"disabled OEM auto-exit skips boot close");
 SaverPreferences.autoExit=true;hooks.invoke(boot,receiver,c);check(receiver.checks==2,"enabled OEM auto-exit retains original check");
 SaverPreferences.fail=true;hooks.invoke(boot,receiver,c);check(receiver.checks==3&&hooks.errors.size()==1,"unreadable preference retains stock behavior");SaverPreferences.fail=false;
 SaverPreferences.autoExit=false;hooks.invoke(unrelated,receiver,c);check(receiver.checks==4,"unrelated manual or scheduled paths remain untouched");
 DexTargets.results.put("battery_saver_boot_close_v1",List.of(boot,unrelated));Hooks ambiguous=new Hooks();
 try{new BatterySaverBootHooks(ambiguous).install(new DexTargets(null,null,null));throw new AssertionError();}catch(NoSuchMethodException expected){check(ambiguous.installed.isEmpty(),"ambiguous boot helper never guessed");}
 DexTargets.results.put("battery_saver_boot_close_v1",List.of(BootReceiver.class.getMethod("wrong",String.class)));Hooks wrong=new Hooks();
 try{new BatterySaverBootHooks(wrong).install(new DexTargets(null,null,null));throw new AssertionError();}catch(NoSuchMethodException expected){check(wrong.installed.isEmpty(),"wrong method signature rejected");}
 DexTargets.results.put("battery_saver_boot_close_v1",List.of(boot));DexTargets.results.put("battery_saver_auto_exit_v1",List.of());
 try{new BatterySaverBootHooks(wrong).install(new DexTargets(null,null,null));throw new AssertionError();}catch(NoSuchMethodException expected){check(wrong.installed.isEmpty(),"missing auto-exit preference keeps stock behavior");}
 System.out.println("PASS: "+checks+" Battery Saver boot regression checks");}}
''',
})

def main():
    with tempfile.TemporaryDirectory(prefix='xsecurity-saver-') as directory:
        temp = Path(directory)
        files = []
        for name, source in SOURCES.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding='utf-8')
            files.append(str(path))
        files += [str(ROOT/'app/src/main/java/io/github/leekin2/xsecurity'/name)
                  for name in ('Features.java', 'BatterySaverBootHooks.java')]
        subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes'),*files],check=True)
        subprocess.run(['java','-cp',str(temp/'classes'),'io.github.leekin2.xsecurity.BatteryBootMain'],check=True)

if __name__ == '__main__': main()
