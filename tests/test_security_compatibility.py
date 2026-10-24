"""Exercise the new Toolbox and motion-support hooks without changing device state."""
from pathlib import Path
import subprocess
import tempfile
from test_dark_mode_hooks import SOURCES as BASE

ROOT=Path(__file__).resolve().parents[1]
SOURCES={name:BASE[name] for name in ('android/content/SharedPreferences.java','io/github/libxposed/api/XposedInterface.java','io/github/leekin2/xsecurity/Hooks.java')}
SOURCES.update({
 'android/R.java':'package android; public class R {public static class drawable {public static final int ic_menu_manage=999;}}',
 'android/content/ContentResolver.java':'package android.content; public class ContentResolver {public int motion;}',
 'android/content/Context.java':'''package android.content;
public class Context {
 public static final String SENSOR_SERVICE="sensor";public android.hardware.SensorManager sensors=new android.hardware.SensorManager();
 public ContentResolver resolver=new ContentResolver();public android.content.pm.PackageManager pm=new android.content.pm.PackageManager();
 public Object getSystemService(String key){return sensors;}public ContentResolver getContentResolver(){return resolver;}
 public android.content.res.Resources getResources(){return new android.content.res.Resources();}
 public String getPackageName(){return "com.miui.securitycenter";}public String getString(int id){return "localized title";}
 public android.content.pm.PackageManager getPackageManager(){return pm;}
}
''',
 'android/content/res/Resources.java':'''package android.content.res;
public class Resources {public int getIdentifier(String name,String type,String pkg){return 0;}}
''',
 'android/content/pm/PackageManager.java':'''package android.content.pm;
public class PackageManager {
 public java.util.Set<String> installed=new java.util.HashSet<>(),denied=new java.util.HashSet<>();
 public static class NameNotFoundException extends Exception {}
 public Object getPackageInfo(String name,int flags)throws NameNotFoundException{if(!installed.contains(name))throw new NameNotFoundException();return new Object();}
}
''',
 'android/content/Intent.java':'''package android.content;
public class Intent {
 public String value;public static Intent parseUri(String value,int flags){Intent i=new Intent();i.value=value;return i;}
 public Object resolveActivity(android.content.pm.PackageManager pm){return pm.denied.contains(value)?null:new Object();}
}
''',
 'android/hardware/Sensor.java':'package android.hardware; public class Sensor {public static final int TYPE_LINEAR_ACCELERATION=10,TYPE_GYROSCOPE=4;}',
 'android/hardware/SensorManager.java':'''package android.hardware;
public class SensorManager {public boolean linear=true,gyro=true;public Sensor getDefaultSensor(int type){return (type==10?linear:gyro)?new Sensor():null;}}
''',
 'android/provider/Settings.java':'''package android.provider;
public class Settings {public static class System {public static int getInt(android.content.ContentResolver resolver,String key,int fallback){return resolver.motion;}}}
''',
 'android/app/ActivityThread.java':'''package android.app;
public class ActivityThread {public static android.content.Context app=new android.content.Context();public static android.content.Context currentApplication(){return app;}}
''',
 'com/miui/common/card/GridFunctionData.java':'''package com.miui.common.card;
public class GridFunctionData {
 public String title,action,stat;public int icon;
 public GridFunctionData(String title,String summary,int icon,String action){this.title=title;this.icon=icon;this.action=action;}
 public String getAction(){return action;}public void setStatKey(String key){stat=key;}
}
''',
 'com/miui/common/card/models/TitleCardModel.java':'''package com.miui.common.card.models;
public class TitleCardModel {
 public java.util.List<com.miui.common.card.GridFunctionData> gridFunctionDataList;
 public String title;public TitleCardModel(String title,java.util.List<com.miui.common.card.GridFunctionData> entries){this.title=title;gridFunctionDataList=entries;}
 public String getTitle(){return title;}
}
''',
 'com/miui/carsickness/ui/CarSicknessReliefSettingsActivity.java':'package com.miui.carsickness.ui; public class CarSicknessReliefSettingsActivity {}',
 'com/miui/carsickness/service/CarSicknessService.java':'package com.miui.carsickness.service; public class CarSicknessService {}',
 'com/miui/carsick/services/AntiCarsickManager.java':'package com.miui.carsick.services; public class AntiCarsickManager {}',
 'io/github/leekin2/xsecurity/DexTargets.java':'''package io.github.leekin2.xsecurity;
import java.util.*;import java.lang.reflect.*;
public class DexTargets {
 public Map<String,List<Method>> methods=new HashMap<>();public List<Class<?>> owners=new ArrayList<>();
 public List<Method> usingStrings(String key,String type,String...strings){return methods.getOrDefault(key,List.of());}
 public List<Class<?>> classesUsing(String key,String...strings){return owners;}
}
''',
 'io/github/leekin2/xsecurity/SecurityCompatMain.java':'''package io.github.leekin2.xsecurity;
import java.lang.reflect.*;import java.util.*;import android.content.*;import android.app.ActivityThread;import com.miui.common.card.GridFunctionData;
public class SecurityCompatMain {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 public static class CarOwner {public static boolean support(Context c){return false;}public static boolean enabled(Context c){return false;}public static boolean unrelated(Context c){return false;}}
 public static class OtherOwner {public static boolean support(Context c){return false;}}
 public static ArrayList<GridFunctionData> chinaStock=new ArrayList<>();
 public static List<GridFunctionData> globalStock=new ArrayList<>();
 public static ArrayList<GridFunctionData> china(){return chinaStock;}
 public static List<GridFunctionData> global(){return Collections.unmodifiableList(globalStock);}
 public static List<String> wrong(){return List.of("not a tool");}
 public static List<Object> cachedStock=new ArrayList<>();
 public static List<Object> cached(Context context){return cachedStock;}
 static GridFunctionData grid(String action){return new GridFunctionData("stock",null,7,action);}
 static String action(String name){return "#Intent;action="+name+";end";}
 static long occurrences(List<?> list,String action){return list.stream().filter(item->((GridFunctionData)item).action.equals(action)).count();}
 public static void main(String[] args)throws Throwable {
  Locale.setDefault(Locale.US);
  Map<String,Boolean> flags=new HashMap<>();Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean d){return flags.getOrDefault(k,d);}});
  ClassLoader cl=SecurityCompatMain.class.getClassLoader();Hooks off=new Hooks();
  new CarSicknessHooks(off,cl).install(new DexTargets());new ToolboxHooks(off,cl).install(new DexTargets());
  check(off.installed.isEmpty(),"both optional features off install nothing");
  flags.put(Features.CAR_SICKNESS,true);DexTargets car=new DexTargets();car.owners.add(CarOwner.class);
  Method support=CarOwner.class.getMethod("support",Context.class),state=CarOwner.class.getMethod("enabled",Context.class);
  Method other=OtherOwner.class.getMethod("support",Context.class),unrelated=CarOwner.class.getMethod("unrelated",Context.class);
  car.methods.put("carsick_sensor_support_v3",List.of(support,other));car.methods.put("carsick_enabled_state_v3",List.of(state));
  Hooks ch=new Hooks();new CarSicknessHooks(ch,cl).install(car);Context context=new Context();
  check(ch.installed.size()==2&&!ch.installed.containsKey(other)&&!ch.installed.containsKey(unrelated),"only verified owner support and enabled getters hooked");
  check(Boolean.TRUE.equals(ch.invoke(support,null,context)),"device list relaxed with both sensors");
  context.sensors.gyro=false;check(Boolean.FALSE.equals(ch.invoke(support,null,context)),"missing gyro retains unsupported state");
  context.sensors.gyro=true;context.sensors.linear=false;check(Boolean.FALSE.equals(ch.invoke(support,null,context)),"missing linear acceleration retains unsupported state");
  context.sensors.linear=true;context.resolver.motion=0;check(Boolean.FALSE.equals(ch.invoke(state,null,context)),"never enables the user's motion setting");
  context.resolver.motion=1;check(Boolean.TRUE.equals(ch.invoke(state,null,context)),"MediaTek state read uses actual enabled preference");
  flags.put(Features.CAR_SICKNESS,false);check(Boolean.FALSE.equals(ch.invoke(state,null,context))&&Boolean.FALSE.equals(ch.invoke(support,null,context)),"runtime off restores original results");
  flags.put(Features.CAR_SICKNESS,true);Hooks absent=new Hooks();
  try{new CarSicknessHooks(absent,new ClassLoader(cl){public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.endsWith("AntiCarsickManager"))throw new ClassNotFoundException(n);return super.loadClass(n);}}).install(car);throw new AssertionError("missing implementation accepted");}
  catch(ClassNotFoundException expected){check(absent.installed.isEmpty(),"missing implementation installs no spoofed support");}
  try{new CarSicknessHooks(new Hooks(),cl).install(new DexTargets());throw new AssertionError("missing target accepted");}
  catch(NoSuchMethodException expected){check(true,"missing support target reported");}

  flags.put(Features.CAR_SICKNESS,false);flags.put(Features.TOOLBOX,true);
  Method china=SecurityCompatMain.class.getMethod("china"),global=SecurityCompatMain.class.getMethod("global");
  DexTargets toolbox=new DexTargets();toolbox.methods.put("toolbox_tools_v3_array",List.of(china));toolbox.methods.put("toolbox_tools_v3_list",List.of(global));
  String aid=action("com.miui.securitycenter.action.FIRST_AID_KIT"),hb=action("miui.intent.action.HB_MAIN_ACTIVITY");
  chinaStock.add(grid(aid));chinaStock.add(grid(hb));globalStock.add(grid(aid));
  Hooks th=new Hooks();new ToolboxHooks(th,cl).install(toolbox);check(th.installed.size()==2,"both China ArrayList and global List factories installed");
  List<?> cn=(List<?>)th.invoke(china,null);List<?> world=(List<?>)th.invoke(global,null);
  check(cn.size()==11&&world.size()==11,"both toolbox layouts get supported shortcuts");
  check(chinaStock.size()==2&&globalStock.size()==1&&cn!=chinaStock,"shared stock lists remain untouched");
  check(occurrences(cn,hb)==0&&chinaStock.get(1).action.equals(hb),"existing HB cleanup affects only returned copy");
  GridFunctionData added=(GridFunctionData)world.get(1);
  check(added.title.equals("Data usage")&&added.icon==999,"missing host resources get readable title and valid system icon");
  String network=action("miui.intent.action.NETWORKASSISTANT_ENTRANCE");globalStock.add(grid(network));
  world=(List<?>)th.invoke(global,null);check(occurrences(world,network)==1,"existing shortcut deduplicated");
  ActivityThread.app.pm.denied.add(action("miui.intent.action.QUICK_REPLY_SETTINGS"));
  world=(List<?>)th.invoke(global,null);check(occurrences(world,action("miui.intent.action.QUICK_REPLY_SETTINGS"))==0,"unavailable activities never advertised as working shortcuts");
  flags.put(Features.TOOLBOX,false);check(th.invoke(china,null)==chinaStock,"runtime off returns exact original list");
  flags.put(Features.CAR_SICKNESS,true);Hooks motionOnly=new Hooks();new ToolboxHooks(motionOnly,cl).install(toolbox);
  cn=(List<?>)motionOnly.invoke(china,null);
  check(cn.size()==3&&occurrences(cn,hb)==1&&occurrences(cn,action("miui.intent.action.CARSICKNESS_RELIEF_SETTINGS"))==1,"motion option adds its own entry without enabling full toolbox or removing HB");
  Hooks noMotion=new Hooks();new ToolboxHooks(noMotion,new ClassLoader(cl){public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.endsWith("AntiCarsickManager"))throw new ClassNotFoundException(n);return super.loadClass(n);}}).install(new DexTargets());
  check(noMotion.installed.isEmpty(),"motion-only shortcut omitted when implementation is missing");
  Method wrong=SecurityCompatMain.class.getMethod("wrong");DexTargets invalid=new DexTargets();invalid.methods.put("toolbox_tools_v3_list",List.of(wrong));
  Hooks wrongHook=new Hooks();new ToolboxHooks(wrongHook,cl).install(invalid);
  check(((List<?>)wrongHook.invoke(wrong,null)).equals(List.of("not a tool")),"unknown model list kept original");
  flags.put(Features.TOOLBOX,true);flags.put(Features.CAR_SICKNESS,true);
  Method cached=SecurityCompatMain.class.getMethod("cached",Context.class);
  DexTargets cacheTargets=new DexTargets();cacheTargets.methods.put("toolbox_cached_cards_v1_list",List.of(cached));
  List<GridFunctionData> originalTools=List.of(grid(aid));
  List<GridFunctionData> originalFamily=List.of(grid(action("miui.intent.action.WARNINGCENTER_MAIN")));
  com.miui.common.card.models.TitleCardModel tools=new com.miui.common.card.models.TitleCardModel("Tools",originalTools);
  com.miui.common.card.models.TitleCardModel family=new com.miui.common.card.models.TitleCardModel("Friends and family",originalFamily);
  com.miui.common.card.models.TitleCardModel basic=new com.miui.common.card.models.TitleCardModel("Basic features",List.of(grid(action("miui.intent.action.APP_MANAGER"))));
  cachedStock.addAll(List.of(tools,family,basic));Hooks cacheHooks=new Hooks();new ToolboxHooks(cacheHooks,cl).install(cacheTargets);
  check(cacheHooks.installed.size()==1,"cache getter alone supported without local factory use");
  Object cacheResult=cacheHooks.invoke(cached,null,new Context());
  check(cacheResult==cachedStock&&cachedStock.size()==3,"cached categories and outer list retained");
  check(tools.gridFunctionDataList.size()==12&&originalTools.size()==1,"cached Tools completed without altering original grid list");
  check(family.gridFunctionDataList==originalFamily&&family.gridFunctionDataList.size()==1,"Friends and family remains untouched");
  check(basic.gridFunctionDataList.size()==3,"Basic features cached group completed");
  List<GridFunctionData> once=tools.gridFunctionDataList;cacheHooks.invoke(cached,null,new Context());
  check(tools.gridFunctionDataList==once&&tools.gridFunctionDataList.size()==12,"cache reload does not duplicate entries or replace an unchanged list");
  flags.put(Features.TOOLBOX,false);flags.put(Features.CAR_SICKNESS,false);cacheHooks.invoke(cached,null,new Context());
  check(tools.gridFunctionDataList==once,"disabled options do not mutate cache cards");
  check(cacheHooks.errors.isEmpty(),"cached-card callbacks have no errors");
  check(ch.errors.isEmpty()&&th.errors.isEmpty()&&motionOnly.errors.isEmpty(),"no callback errors in compatibility scenarios");
  System.out.println("PASS: "+checks+" Security compatibility checks");
 }
}
''',
})

def main():
    with tempfile.TemporaryDirectory(prefix='xsecurity-security-compat-') as folder:
        temp=Path(folder)
        files=[]
        for name,source in SOURCES.items():
            path=temp/name
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(source,encoding='utf-8')
            files.append(str(path))
        production=ROOT/'app/src/main/java/io/github/leekin2/xsecurity'
        files += [str(production/name) for name in ('Features.java','CarSicknessHooks.java','ToolboxHooks.java')]
        subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes'),*files],check=True)
        subprocess.run(['java','-cp',str(temp/'classes'),'io.github.leekin2.xsecurity.SecurityCompatMain'],check=True)

if __name__=='__main__':main()
