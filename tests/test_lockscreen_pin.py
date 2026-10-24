"""Production SystemUI hook: key identity, animation matrices, controls and rollback."""
from pathlib import Path
import subprocess
import tempfile
from test_dark_mode_hooks import SOURCES as BASE

ROOT = Path(__file__).resolve().parents[1]
SOURCES = {name: BASE[name] for name in (
    'android/content/SharedPreferences.java', 'io/github/libxposed/api/XposedInterface.java',
    'io/github/leekin2/xsecurity/Hooks.java')}
SOURCES.update({
 'android/content/res/Resources.java': '''package android.content.res;
public class Resources {public int getIdentifier(String name,String type,String pkg){return name.startsWith("row")?Integer.parseInt(name.substring(3)):0;}}
''',
 'android/view/View.java': '''package android.view;
public class View {
 public int id; public ViewGroup parent; public ViewGroup.LayoutParams params=new ViewGroup.LayoutParams();
 public Object getParent(){return parent;} public ViewGroup.LayoutParams getLayoutParams(){return params;}
 public android.content.res.Resources getResources(){return new android.content.res.Resources();}
 public View findViewById(int target){return id==target?this:null;}
}
''',
 'android/view/ViewGroup.java': '''package android.view;
import java.util.*;
public class ViewGroup extends View {
 public static class LayoutParams {public int slot;}
 public List<View> children=new ArrayList<>();public boolean failNextAdd;
 public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}
 public void addView(View v){addView(v,children.size(),v.params);}
 public void addView(View v,int i,LayoutParams p){
  if(failNextAdd){failNextAdd=false;throw new IllegalStateException("fixture insertion");}
  if(v.parent!=null)throw new IllegalStateException("already parented");children.add(i,v);v.parent=this;v.params=p;}
 public void removeView(View v){children.removeIf(item->item==v);v.parent=null;}
 @Override public View findViewById(int target){if(id==target)return this;for(View v:children){View found=v.findViewById(target);if(found!=null)return found;}return null;}
}
''',
 'com/android/keyguard/NumPadKey.java': '''package com.android.keyguard;
public class NumPadKey extends android.view.View {
 public final int mDigit;public final String label;
 public NumPadKey(int d){mDigit=d;label=""+d;}public int click(){return mDigit;}
}
''',
 'com/android/keyguard/KeyguardPINView.java': '''package com.android.keyguard;
import android.view.*;
public class KeyguardPINView extends ViewGroup {
 public View[][] mViews,mNumViews;public ViewGroup[] rows=new ViewGroup[4];
 public int inflates,appears,verifications;
 public KeyguardPINView(){
  View[][] digits=new View[3][3];for(int r=0;r<4;r++){rows[r]=new ViewGroup();rows[r].id=r+1;addView(rows[r]);
   for(int c=0;c<3;c++){View key=r<3?new NumPadKey(r*3+c+1):(c==1?new NumPadKey(0):new View());
    key.params.slot=r*3+c;rows[r].addView(key);if(r<3)digits[r][c]=key;}}
  View zero=rows[3].getChildAt(1);
  mViews=new View[][]{new View[]{new View(),null,null},digits[0],digits[1],digits[2],new View[]{null,zero,rows[3].getChildAt(2)},new View[]{new View(),null,new View(),new View()}};
  mNumViews=new View[][]{digits[0],digits[1],digits[2],new View[]{null,zero,rows[3].getChildAt(2)}};
 }
 public void onFinishInflate(){inflates++;}public void startAppearAnimation(){appears++;}
 public boolean verify(String entered){verifications++;return false;}
}
''',
 'io/github/leekin2/xsecurity/LockscreenPinMain.java': '''package io.github.leekin2.xsecurity;
import java.lang.reflect.*;import java.util.*;import android.content.*;import android.view.*;import com.android.keyguard.*;
public class LockscreenPinMain {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static List<View> order(KeyguardPINView p){List<View> out=new ArrayList<>();for(int i=0;i<10;i++)out.add(p.rows[i<9?i/3:3].getChildAt(i<9?i%3:1));return out;}
 static boolean same(List<View> a,List<View> b){for(int i=0;i<a.size();i++)if(a.get(i)!=b.get(i))return false;return true;}
 static void matrices(KeyguardPINView p){for(int i=0;i<9;i++)check(p.mViews[i/3+1][i%3]==p.rows[i/3].getChildAt(i%3)&&p.mNumViews[i/3]==p.mViews[i/3+1],"animation row aliases");
  check(p.mViews[4][1]==p.rows[3].getChildAt(1)&&p.mNumViews[3][1]==p.mViews[4][1],"zero animation alias");}
 static class Tiny extends KeyguardPINView {}
 public static void main(String[] args)throws Throwable {
  Map<String,Boolean> prefs=new HashMap<>();Features.attach(new SharedPreferences(){public boolean getBoolean(String k,boolean d){return prefs.getOrDefault(k,d);}});
  check(!Features.defaultValue(Features.LOCKSCREEN_PIN_SCRAMBLE)&&!Features.legacyDefault(Features.LOCKSCREEN_PIN_SCRAMBLE),"new and migrating installs default off");
  ClassLoader loader=LockscreenPinMain.class.getClassLoader();Hooks off=new Hooks();new LockscreenPinHooks(off,loader).install();
  check(off.installed.isEmpty(),"off registers no SystemUI hook");prefs.put(Features.LOCKSCREEN_PIN_SCRAMBLE,true);
  Hooks h=new Hooks();new LockscreenPinHooks(h,loader).install();check(h.installed.size()==2&&h.errors.isEmpty(),"inflate and appearance only");
  Method inflate=KeyguardPINView.class.getMethod("onFinishInflate"),appear=KeyguardPINView.class.getMethod("startAppearAnimation");
  Field digit=NumPadKey.class.getDeclaredField("mDigit");digit.setAccessible(true);
  KeyguardPINView pin=new KeyguardPINView();List<View> original=order(pin);View left=pin.rows[3].getChildAt(0),right=pin.rows[3].getChildAt(2),emergency=pin.mViews[5][0];
  h.invoke(inflate,pin);check(pin.inflates==1&&!same(original,order(pin)),"stock inflate then different digit order");
  check(pin.rows[3].getChildAt(0)==left&&pin.rows[3].getChildAt(2)==right&&pin.mViews[5][0]==emergency,"controls and emergency view stay put");
  Set<Integer> seen=new HashSet<>();for(View view:order(pin)){NumPadKey key=(NumPadKey)view;seen.add(key.click());check(key.label.equals(""+key.mDigit),"displayed label matches original input callback");}
  check(seen.size()==10&&seen.contains(0)&&seen.contains(9),"all digits retained exactly once");matrices(pin);
  for(int i=0;i<10;i++)check(order(pin).get(i).params.slot==(i<9?i:10),"layout parameters follow destination slot");
  List<View> first=order(pin);h.invoke(appear,pin);check(pin.appears==1&&!same(first,order(pin)),"reshuffle before each appear animation");matrices(pin);
  check(!h.installed.containsKey(KeyguardPINView.class.getMethod("verify",String.class))&&!pin.verify("1234")&&pin.verifications==1,"verification retains original implementation");
  prefs.put(Features.LOCKSCREEN_PIN_SCRAMBLE,false);List<View> second=order(pin);h.invoke(appear,pin);
  check(same(second,order(pin)),"runtime off stops shuffling");prefs.put(Features.LOCKSCREEN_PIN_SCRAMBLE,true);
  Tiny tiny=new Tiny();List<View> tinyOriginal=order(tiny);h.invoke(inflate,tiny);check(same(tinyOriginal,order(tiny)),"unknown subclass left original");
  KeyguardPINView malformed=new KeyguardPINView();List<View> old=order(malformed);malformed.mNumViews[0]=malformed.mViews[1].clone();
  check(!LockscreenPinHooks.shuffle(malformed,NumPadKey.class,digit,malformed.mViews,malformed.mNumViews)&&same(old,order(malformed)),"unknown matrix aliases cause no mutation");
  KeyguardPINView duplicate=new KeyguardPINView();View oldKey=duplicate.rows[0].getChildAt(0);duplicate.rows[0].removeView(oldKey);
  NumPadKey replacement=new NumPadKey(2);duplicate.rows[0].addView(replacement,0,oldKey.params);duplicate.mViews[1][0]=replacement;old=order(duplicate);
  check(!LockscreenPinHooks.shuffle(duplicate,NumPadKey.class,digit,duplicate.mViews,duplicate.mNumViews)&&same(old,order(duplicate)),"duplicate digit fails before mutation");
  KeyguardPINView rollback=new KeyguardPINView();old=order(rollback);rollback.rows[1].failNextAdd=true;
  try{LockscreenPinHooks.shuffle(rollback,NumPadKey.class,digit,rollback.mViews,rollback.mNumViews);throw new AssertionError("fixture did not throw");}
  catch(IllegalStateException expected){check(same(old,order(rollback)),"insertion failure rolls back all ten keys");matrices(rollback);}
  Hooks missing=new Hooks();new LockscreenPinHooks(missing,new ClassLoader(loader){public Class<?> loadClass(String n)throws ClassNotFoundException{if(n.equals("com.android.keyguard.NumPadKey"))throw new ClassNotFoundException(n);return super.loadClass(n);}}).install();
  check(missing.installed.isEmpty()&&missing.errors.size()==1,"unavailable targets preserve stock keypad");
  check(h.errors.isEmpty(),"normal lifecycle has no callback failures");System.out.println("PASS: "+checks+" lock-screen PIN checks");
 }
}
''',
})

def main():
    with tempfile.TemporaryDirectory(prefix='xsecurity-lockscreen-tests-') as folder:
        temp=Path(folder)
        files=[]
        for name,source in SOURCES.items():
            path=temp/name
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_text(source,encoding='utf-8')
            files.append(str(path))
        production=ROOT/'app/src/main/java/io/github/leekin2/xsecurity'
        files += [str(production/name) for name in ('Features.java','LockscreenPinHooks.java')]
        subprocess.run(['javac','-encoding','UTF-8','-d',str(temp/'classes'),*files],check=True)
        subprocess.run(['java','-cp',str(temp/'classes'),'io.github.leekin2.xsecurity.LockscreenPinMain'],check=True)
    text=(ROOT/'app/src/main/kotlin/io/github/leekin2/xsecurity/ui/Texts.kt').read_text(encoding='utf-8')
    assert text.count('F.LOCKSCREEN_PIN_SCRAMBLE to Line(')==5
    assert 'com.android.systemui' in (ROOT/'app/src/main/resources/META-INF/xposed/scope.list').read_text().splitlines()
    print('PASS: five languages and SystemUI scope')

if __name__=='__main__': main()
