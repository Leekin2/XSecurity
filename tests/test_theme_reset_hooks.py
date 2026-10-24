"""Verify that theme protection affects only the known periodic receiver event."""
from pathlib import Path
import subprocess
import tempfile

from test_dark_mode_hooks import SOURCES as DARK_SOURCES

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {key: DARK_SOURCES[key] for key in (
    "android/content/SharedPreferences.java",
    "io/github/libxposed/api/XposedInterface.java",
    "io/github/leekin2/xsecurity/Hooks.java",
)}
SOURCES.update({
    "android/content/Context.java": "package android.content; public class Context {}",
    "android/content/Intent.java": """package android.content;
public class Intent {private String action; public Intent(String a){action=a;}
 public String getAction(){return action;}}
""",
    "miui/drm/ThemeReceiver.java": """package miui.drm;
import android.content.*;
public class ThemeReceiver {
 public int periodic,other; public boolean fail;
 public void onReceive(Context context,Intent intent){
  if(fail)throw new IllegalStateException("host");
  if(intent!=null&&"miui.intent.action.CHECK_TIME_UP".equals(intent.getAction()))periodic++;
  else other++;
 }
}
""",
    "io/github/leekin2/xsecurity/ThemeResetMain.java": """package io.github.leekin2.xsecurity;
import java.util.*; import java.lang.reflect.*; import android.content.*; import miui.drm.*;
public class ThemeResetMain {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static void main(String[] args)throws Throwable {
  Map<String,Boolean> values=new HashMap<>();
  Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean fallback){return values.getOrDefault(k,fallback);}});
  ClassLoader cl=ThemeResetMain.class.getClassLoader();Hooks off=new Hooks();
  new FixThemeResetHooks(off,cl).install();check(off.installed.isEmpty(),"default off installs no hook");
  values.put(Features.THEME_FIX_RESET,true);Hooks hooks=new Hooks();
  new FixThemeResetHooks(hooks,cl).install();check(hooks.installed.size()==1,"only periodic receiver hooked");
  Method receive=ThemeReceiver.class.getMethod("onReceive",Context.class,Intent.class);
  ThemeReceiver receiver=new ThemeReceiver();Context context=new Context();
  Intent periodic=new Intent("miui.intent.action.CHECK_TIME_UP");
  check(hooks.invoke(receive,receiver,context,periodic)==null&&receiver.periodic==0,"automatic reset check suppressed");
  hooks.invoke(receive,receiver,context,new Intent("manual.theme.change"));
  check(receiver.other==1,"unrelated action retained");
  hooks.invoke(receive,receiver,context,new Intent(null));hooks.invoke(receive,receiver,context,null);
  check(receiver.other==3,"null intents and actions retain host semantics");
  values.put(Features.THEME_FIX_RESET,false);hooks.invoke(receive,receiver,context,periodic);
  check(receiver.periodic==1,"runtime disabling restores the periodic event");
  receiver.fail=true;
  try{hooks.invoke(receive,receiver,context,periodic);throw new AssertionError("host failure swallowed");}
  catch(IllegalStateException expected){check(expected.getMessage().equals("host"),"host failure retained");}
  values.put(Features.THEME_FIX_RESET,true);Hooks missing=new Hooks();
  new FixThemeResetHooks(missing,new ClassLoader(cl){
   public Class<?> loadClass(String name)throws ClassNotFoundException{
    if(name.equals("miui.drm.ThemeReceiver"))throw new ClassNotFoundException(name);
    return super.loadClass(name);}}).install();
  check(missing.installed.isEmpty()&&missing.errors.size()==1,"unsupported ROM reported without partial hook");
  System.out.println("PASS: "+checks+" theme reset regression checks");
 }
}
""",
})


def main():
    with tempfile.TemporaryDirectory(prefix="xsecurity-theme-tests-") as directory:
        temp = Path(directory)
        files = []
        for name, source in SOURCES.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")
            files.append(str(path))
        production = ROOT / "app/src/main/java/io/github/leekin2/xsecurity"
        files.extend(str(production / name) for name in ("Features.java", "FixThemeResetHooks.java"))
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(temp / "classes"), *files], check=True)
        subprocess.run(["java", "-cp", str(temp / "classes"), "io.github.leekin2.xsecurity.ThemeResetMain"], check=True)


if __name__ == "__main__":
    main()
