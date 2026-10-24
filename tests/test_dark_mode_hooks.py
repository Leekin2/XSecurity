"""Exercise production DarkModeHooks against the locally inspected HyperOS list contract."""
from pathlib import Path
import subprocess
import tempfile

from test_extension_hooks import FIXTURES

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {
    "android/content/pm/ApplicationInfo.java": """package android.content.pm;
public class ApplicationInfo {
 public static final int FLAG_SYSTEM=1; public int flags,uid; public String packageName;
 public ApplicationInfo(String p,int u,int f){packageName=p;uid=u;flags=f;}
}
""",
    "android/content/pm/PackageInfo.java": """package android.content.pm;
public class PackageInfo {public ApplicationInfo applicationInfo;
 public PackageInfo(ApplicationInfo a){applicationInfo=a;}}
""",
    "com/android/server/DarkModeAppSettingsInfo.java": """package com.android.server;
public class DarkModeAppSettingsInfo {public int adaptStat;}
""",
    "com/miui/darkmode/DarkModeAppDetailInfo.java": """package com.miui.darkmode;
public class DarkModeAppDetailInfo {
 public String name; public boolean enabled; public int user,adapt;
 public String getPkgName(){return name;}
}
""",
    "com/miui/darkmode/DarkModeAppData.java": """package com.miui.darkmode;
import java.util.*;
public class DarkModeAppData {
 public List<DarkModeAppDetailInfo> rows=new ArrayList<>();
 public List<DarkModeAppDetailInfo> getDarkModeAppDetailInfoList(){return rows;}
 public void setDarkModeAppDetailInfoList(List<DarkModeAppDetailInfo> r){rows=r;}
}
""",
    "com/android/server/ForceDarkAppListManager.java": """package com.android.server;
import java.util.*; import android.content.pm.*; import com.miui.darkmode.*;
public class ForceDarkAppListManager {
 public DarkModeAppData mDarkModeAppData=new DarkModeAppData();
 public Map<String,DarkModeAppSettingsInfo> mForceDarkAppSettings=new HashMap<>();
 public List<PackageInfo> installed=new ArrayList<>(); public Set<String> enabled=new HashSet<>();
 public boolean hostFailure,rowFailure;
 public DarkModeAppData getDarkModeAppList(long timestamp,int user){
  if(hostFailure)throw new IllegalStateException("host");
  return timestamp==42?null:mDarkModeAppData;
 }
 private List<PackageInfo> getInstalledPkgList(){return installed;}
 private DarkModeAppDetailInfo getAppInfo(DarkModeAppSettingsInfo settings,ApplicationInfo info,int user){
  if(rowFailure)throw new IllegalStateException("row");
  DarkModeAppDetailInfo row=new DarkModeAppDetailInfo();row.name=info.packageName;
  row.enabled=enabled.contains(row.name);row.user=user;row.adapt=settings.adaptStat;return row;
 }
}
""",
    "io/github/leekin2/xsecurity/DarkModeMain.java": """package io.github.leekin2.xsecurity;
import java.util.*; import java.lang.reflect.*;
import android.content.*; import android.content.pm.*;
import com.android.server.*; import com.miui.darkmode.*;
public class DarkModeMain {
 static int checks; static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static ApplicationInfo app(String p,int uid,int flags){return new ApplicationInfo(p,uid,flags);}
 public static void main(String[] args)throws Throwable {
  Map<String,Boolean> prefs=new HashMap<>();
  Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean fallback){return prefs.getOrDefault(k,fallback);}});
  ClassLoader cl=DarkModeMain.class.getClassLoader();Hooks off=new Hooks();
  new DarkModeHooks(off,cl).install();check(off.installed.isEmpty(),"default off installs nothing");
  prefs.put(Features.DARK_MODE_ALL_APPS,true);Hooks h=new Hooks();new DarkModeHooks(h,cl).install();
  check(h.installed.size()==1,"exact list hook installed");
  Method method=ForceDarkAppListManager.class.getMethod("getDarkModeAppList",long.class,int.class);
  ForceDarkAppListManager m=new ForceDarkAppListManager();
  DarkModeAppDetailInfo weibo=new DarkModeAppDetailInfo();weibo.name="weibo";m.mDarkModeAppData.rows.add(weibo);
  List<DarkModeAppDetailInfo> original=m.mDarkModeAppData.rows;
  m.installed.addAll(Arrays.asList(new PackageInfo(app("weibo",10001,0)),
   new PackageInfo(app("hidden",10002,0)),new PackageInfo(app("new.app",10003,0)),
   new PackageInfo(app("hidden",10002,0)),new PackageInfo(app("system",10004,1)),
   new PackageInfo(app("shared",1000,0)),new PackageInfo(app("profile.shared",101000,0)),
   new PackageInfo(app("profile.app",110005,0)),new PackageInfo(null),null));
  DarkModeAppSettingsInfo entry=new DarkModeAppSettingsInfo();entry.adaptStat=9;
  m.mForceDarkAppSettings.put("hidden",entry);m.enabled.add("hidden");
  DarkModeAppData result=(DarkModeAppData)h.invoke(method,m,0L,10);
  check(result==m.mDarkModeAppData,"returns framework data object");
  check(result.rows.size()==4,"adds hidden, unconfigured, profile apps; no duplicates or system identities");
  check(original.size()==1&&result.rows.get(0)==weibo,"preserves existing rows and original list");
  DarkModeAppDetailInfo hidden=result.rows.get(1);
  check(hidden.enabled&&hidden.user==10&&hidden.adapt==9,"saved state, caller user, adaptation preserved");
  check(m.mForceDarkAppSettings.size()==1&&m.mForceDarkAppSettings.get("hidden")==entry,"cloud config untouched");
  result=(DarkModeAppData)h.invoke(method,m,42L,10);
  check(result.rows.size()==4,"cached null path returns complete list without duplicates");
  prefs.put(Features.DARK_MODE_ALL_APPS,false);
  check(h.invoke(method,m,42L,10)==null,"runtime off preserves original null return");
  prefs.put(Features.DARK_MODE_ALL_APPS,true);
  ForceDarkAppListManager failure=new ForceDarkAppListManager();failure.rowFailure=true;
  failure.installed.add(new PackageInfo(app("failure",10001,0)));
  List<?> failureRows=failure.mDarkModeAppData.rows;
  check(h.invoke(method,failure,0L,0)==failure.mDarkModeAppData,"row failure keeps original return");
  check(failure.mDarkModeAppData.rows==failureRows,"row failure commits no partial list");
  h.invoke(method,failure,0L,0);check(h.errors.size()==1,"logs failure only once");
  failure.hostFailure=true;
  try{h.invoke(method,failure,0L,0);throw new AssertionError("host exception swallowed");}
  catch(IllegalStateException expected){check(expected.getMessage().equals("host"),"host failure preserved");}
  Hooks missing=new Hooks();new DarkModeHooks(missing,new ClassLoader(cl){
   public Class<?> loadClass(String n)throws ClassNotFoundException{
    if(n.equals("com.android.server.DarkModeAppSettingsInfo"))throw new ClassNotFoundException(n);
    return super.loadClass(n);}}).install();
  check(missing.installed.isEmpty()&&missing.errors.size()==1,"unsupported ROM installs nothing and reports error");
  System.out.println("PASS: "+checks+" dark-mode regression checks");
 }
}
""",
}
SOURCES["io/github/libxposed/api/XposedInterface.java"] = FIXTURES["io/github/libxposed/api/XposedInterface.java"]
SOURCES["android/content/SharedPreferences.java"] = FIXTURES["android/content/SharedPreferences.java"].replace(
    "boolean getBoolean", "default java.util.Set<String> getStringSet(String key, java.util.Set<String> fallback){return fallback;}\n    boolean getBoolean")
SOURCES["io/github/leekin2/xsecurity/Hooks.java"] = FIXTURES["io/github/leekin2/xsecurity/Hooks.java"].replace(
    "void deoptimize(Executable m){}", "void deoptimize(Executable m){} void deoptimizeReported(Executable m){}")


def main():
    with tempfile.TemporaryDirectory(prefix="xsecurity-dark-tests-") as directory:
        temp = Path(directory)
        files = []
        for name, source in SOURCES.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")
            files.append(str(path))
        production = ROOT / "app/src/main/java/io/github/leekin2/xsecurity"
        files.extend(str(production / name) for name in ("Features.java", "DarkModeHooks.java"))
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(temp / "classes"), *files], check=True)
        subprocess.run(["java", "-cp", str(temp / "classes"), "io.github.leekin2.xsecurity.DarkModeMain"], check=True)


if __name__ == "__main__":
    main()
