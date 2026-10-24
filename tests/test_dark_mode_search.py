"""Run the production Settings hook against the inspected fragment/preference API contract."""
from pathlib import Path
import subprocess
import tempfile

from test_dark_mode_hooks import SOURCES as BASE

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {key: BASE[key] for key in (
    "android/content/SharedPreferences.java", "io/github/libxposed/api/XposedInterface.java",
    "io/github/leekin2/xsecurity/Hooks.java",
)}
SOURCES.update({
    "android/R.java": "package android; public class R {public static class drawable {public static int ic_menu_search=1;} public static class string {public static int cancel=2;}}",
    "android/os/Bundle.java": "package android.os; public class Bundle {}",
    "android/os/LocaleList.java": "package android.os; public class LocaleList {public java.util.Locale get(int i){return java.util.Locale.getDefault();}}",
    "android/content/Context.java": "package android.content; public class Context {public android.content.res.Resources getResources(){return new android.content.res.Resources();}}",
    "android/content/res/Resources.java": "package android.content.res; public class Resources {public Configuration getConfiguration(){return new Configuration();} public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}",
    "android/content/res/Configuration.java": "package android.content.res; public class Configuration {public android.os.LocaleList getLocales(){return new android.os.LocaleList();}}",
    "android/util/DisplayMetrics.java": "package android.util; public class DisplayMetrics {public float density=1;}",
    "android/view/View.java": "package android.view; public class View {}",
    "android/view/inputmethod/EditorInfo.java": "package android.view.inputmethod; public class EditorInfo {public static final int IME_ACTION_SEARCH=3;}",
    "android/text/InputFilter.java": "package android.text; public interface InputFilter {class LengthFilter implements InputFilter {public LengthFilter(int n){}}}",
    "android/text/InputType.java": "package android.text; public class InputType {public static final int TYPE_CLASS_TEXT=1;}",
    "android/content/DialogInterface.java": """package android.content;
public interface DialogInterface {
 interface OnClickListener {void onClick(DialogInterface d,int which);}
 interface OnDismissListener {void onDismiss(DialogInterface d);}
}
""",
    "android/widget/EditText.java": """package android.widget;
import android.content.*; import android.text.*;
public class EditText extends android.view.View {
 public static EditText latest; public String text="";
 public interface OnEditorActionListener {boolean onEditorAction(EditText v,int action,Object event);}
 public OnEditorActionListener editor;
 public EditText(Context c){latest=this;} public void setSingleLine(boolean b){}
 public void setInputType(int i){} public void setImeOptions(int i){} public void setFilters(InputFilter[] f){}
 public void setHint(String s){} public void setText(String s){text=s;} public int length(){return text.length();}
 public String getText(){return text;} public void setSelection(int i){} public void setPadding(int a,int b,int c,int d){}
 public void setOnEditorActionListener(OnEditorActionListener listener){editor=listener;}
 public boolean requestFocus(){return true;}
}
""",
    "android/app/AlertDialog.java": """package android.app;
import android.content.*; import android.view.*;
public class AlertDialog implements DialogInterface {
 public static AlertDialog latest; public OnClickListener positive,neutral; public OnDismissListener dismissed;
 public String title; public boolean open;
 public void show(){latest=this;open=true;} public void dismiss(){open=false;if(dismissed!=null)dismissed.onDismiss(this);}
 public void setOnDismissListener(OnDismissListener d){dismissed=d;}
 public static class Builder {
  AlertDialog dialog=new AlertDialog(); public Builder(Context c){}
  public Builder setTitle(String t){dialog.title=t;return this;} public Builder setView(View v){return this;}
  public Builder setPositiveButton(String t,OnClickListener l){dialog.positive=l;return this;}
  public Builder setNeutralButton(String t,OnClickListener l){dialog.neutral=l;return this;}
  public Builder setNegativeButton(int t,OnClickListener l){return this;}
  public AlertDialog create(){return dialog;}
 }
}
""",
    "androidx/preference/Preference.java": """package androidx.preference;
import android.content.*;
public class Preference {
 public interface OnPreferenceClickListener {boolean onPreferenceClick(Preference p);}
 public String key; public CharSequence title,summary; public boolean visible=true,checked,persistent=true;
 public int order; public OnPreferenceClickListener click;
 public Preference(Context c){} public String getKey(){return key;} public CharSequence getTitle(){return title;}
 public boolean isVisible(){return visible;} public void setVisible(boolean v){visible=v;}
 public void setKey(String s){key=s;} public void setTitle(CharSequence s){title=s;}
 public void setSummary(CharSequence s){summary=s;} public void setOrder(int o){order=o;}
 public void setPersistent(boolean p){persistent=p;} public void setIcon(int i){}
 public void setOnPreferenceClickListener(OnPreferenceClickListener l){click=l;}
}
""",
    "androidx/preference/PreferenceGroup.java": """package androidx.preference;
import java.util.*; import android.content.*;
public class PreferenceGroup extends Preference {
 public List<Preference> rows=new ArrayList<>(); public PreferenceGroup(Context c){super(c);}
 public Preference findPreference(CharSequence k){return rows.stream().filter(p->k.equals(p.key)).findFirst().orElse(null);}
 public boolean addPreference(Preference p){rows.add(p);rows.sort(Comparator.comparingInt(r->r.order));return true;}
 public int getPreferenceCount(){return rows.size();} public Preference getPreference(int i){return rows.get(i);}
}
""",
    "com/android/settings/display/DarkModeFragment.java": """package com.android.settings.display;
import android.content.*; import android.os.*; import android.view.*; import androidx.preference.*; import java.util.*;
public class DarkModeFragment {
 public Context context=new Context(); public PreferenceGroup mDarkModeAppsCategory=new PreferenceGroup(context);
 public boolean destroyed,fail; public int userId=10,icons;
 private void loadIcon(String pkg,int user){if(user==10)icons++;}
 public Context getContext(){return context;}
 public void onViewCreated(View v,Bundle b){} public void showAppList(List<?> list){}
 public void onDestroy(){destroyed=true;if(fail)throw new IllegalStateException("host");}
}
""",
    "io/github/leekin2/xsecurity/SearchMain.java": """package io.github.leekin2.xsecurity;
import java.util.*; import java.lang.reflect.*; import android.content.*; import android.os.*; import android.view.*;
import android.app.AlertDialog;import android.widget.EditText;import androidx.preference.*;import com.android.settings.display.*;
public class SearchMain {
 static int checks; static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static Preference row(Context c,String name,String pkg,boolean checked){Preference p=new Preference(c);p.title=name;p.key=pkg;p.checked=checked;return p;}
 static void search(Preference action,String text){action.click.onPreferenceClick(action);EditText.latest.setText(text);
  AlertDialog dialog=AlertDialog.latest;dialog.positive.onClick(dialog,1);dialog.dismiss();}
 public static void main(String[] args)throws Throwable {
  check(AppSearch.matches("İstanbul Çağrı","com.example.app","istanbul cagri"),"Turkish and accent folding");
  check(AppSearch.matches("IŞIK","com.sample","ışık"),"dotless i query");
  check(AppSearch.matches("Telegram Beta","org.telegram.beta","TELEGRAM beta"),"case and word matching");
  check(AppSearch.matches("Example","com.example.beta","com.example"),"package search");
  check(AppSearch.matches(null,null," "),"empty input matches");
  check(!AppSearch.matches("Example","com.example",".*"),"query treated literally");
  Map<String,Boolean> flags=new HashMap<>();
  Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean d){return flags.getOrDefault(k,d);}});
  ClassLoader cl=SearchMain.class.getClassLoader();Hooks off=new Hooks();new DarkModeSearchHooks(off,cl).install();
  check(off.installed.isEmpty(),"off installs nothing");flags.put(Features.DARK_MODE_ALL_APPS,true);
  Hooks h=new Hooks();new DarkModeSearchHooks(h,cl).install();check(h.installed.size()==3&&h.errors.isEmpty(),"all lifecycle targets installed");
  DarkModeFragment fragment=new DarkModeFragment();PreferenceGroup group=fragment.mDarkModeAppsCategory;
  Preference telegram=row(fragment.context,"Telegram Beta","org.telegram.beta",true);
  Preference gmail=row(fragment.context,"Gmail","com.google.android.gm",false);
  Preference hidden=row(fragment.context,"Hidden","com.hidden",true);hidden.visible=false;
  group.addPreference(telegram);group.addPreference(gmail);group.addPreference(hidden);
  Method created=DarkModeFragment.class.getMethod("onViewCreated",View.class,Bundle.class);
  Method shown=DarkModeFragment.class.getMethod("showAppList",List.class);
  Method destroyed=DarkModeFragment.class.getMethod("onDestroy");
  h.invoke(created,fragment,new View(),new Bundle());
  Preference action=group.findPreference("xsecurity_dark_mode_search");
  check(action!=null&&group.rows.get(0)==action&&!action.persistent,"nonpersistent action first in app category");
  search(action,"telegram");check(telegram.visible&&!gmail.visible&&!hidden.visible,"only matching app rows shown");
  check(fragment.icons==1,"first matching result icon is loaded using the fragment user");
  check(telegram.checked&&!gmail.checked&&hidden.checked,"per-app checked state retained");
  check(action.summary.toString().equals("telegram · 1/2"),"matching count excludes OEM hidden rows");
  search(action,"no match");check(!telegram.visible&&!gmail.visible&&action.visible,"zero results retains search action");
  action.click.onPreferenceClick(action);AlertDialog dialog=AlertDialog.latest;
  dialog.neutral.onClick(dialog,0);dialog.dismiss();check(telegram.visible&&gmail.visible&&!hidden.visible,"clear restores original visibility");
  search(action,"com.google");check(gmail.visible&&!telegram.visible,"package query filters UI");
  Preference added=row(fragment.context,"Google New","com.google.new",true);group.addPreference(added);
  h.invoke(shown,fragment,List.of());check(added.visible&&!telegram.visible&&group.rows.size()==5,"async reload reapplies query without duplicate action");
  action.click.onPreferenceClick(action);EditText.latest.setText("Telegram");
  check(EditText.latest.editor.onEditorAction(EditText.latest,3,null)&&telegram.visible&&!gmail.visible,"keyboard search submits query");
  action.click.onPreferenceClick(action);dialog=AlertDialog.latest;
  h.invoke(destroyed,fragment);check(fragment.destroyed&&!dialog.open,"destroy dismisses dialog");
  for(String language:List.of("tr","en","ru","es","zh")){
   Locale.setDefault(Locale.forLanguageTag(language));DarkModeFragment local=new DarkModeFragment();
   h.invoke(created,local,new View(),new Bundle());Preference item=local.mDarkModeAppsCategory.findPreference("xsecurity_dark_mode_search");
   check(item!=null&&item.title!=null&&!item.title.toString().isBlank(),"localized action: "+language);
  }
  Hooks missing=new Hooks();new DarkModeSearchHooks(missing,new ClassLoader(cl){
   public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.equals("com.android.settings.display.DarkModeFragment"))throw new ClassNotFoundException(n);return super.loadClass(n);}}).install();
  check(missing.installed.isEmpty()&&missing.errors.size()==1,"missing ROM target keeps stock behaviour");
  check(h.errors.isEmpty(),"no refresh or dialog failures");
  System.out.println("PASS: "+checks+" dark-mode search checks");
 }
}
""",
})


def main():
    with tempfile.TemporaryDirectory(prefix="xsecurity-search-tests-") as directory:
        temp = Path(directory)
        files = []
        for name, source in SOURCES.items():
            path = temp / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(source, encoding="utf-8")
            files.append(str(path))
        production = ROOT / "app/src/main/java/io/github/leekin2/xsecurity"
        files.extend(str(production / name) for name in ("Features.java", "AppSearch.java", "DarkModeSearchHooks.java"))
        subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(temp / "classes"), *files], check=True)
        subprocess.run(["java", "-cp", str(temp / "classes"), "io.github.leekin2.xsecurity.SearchMain"], check=True)

    scope = (ROOT / "app/src/main/resources/META-INF/xposed/scope.list").read_text().splitlines()
    assert scope.count("com.android.settings") == 1
    manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
    activity = (ROOT / "app/src/main/kotlin/io/github/leekin2/xsecurity/MainActivity.kt").read_text(encoding="utf-8")
    assert '<package android:name="com.android.settings"' in manifest
    assert '"com.android.settings"' in activity
    print("PASS: Settings scope and package visibility registered")


if __name__ == "__main__":
    main()
