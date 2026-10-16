package io.github.leekin2.xsecurity;

import android.content.Context;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import org.luckypray.dexkit.query.matchers.MethodMatcher;

/**
 * Charging unlocks, Game Turbo and network hooks in the Security app. Charging and Game Turbo ones
 * follow HyperCeiler's Security rules; the network ones were found in the Security app's own code
 * (13.5.8 and 13.6.5), where the network assistant turns mobile data or the hotspot off by itself.
 */
final class SecurityExtraHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    SecurityExtraHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    // --- Charging unlocks ---------------------------------------------------------------------

    /** Smart charging (and its options) on phones whose kernel flag the app reads says no. */
    void smartCharge(DexTargets targets) throws Throwable {
        List<Method> methods = targets.usingStrings("smart_chg_support", "boolean", "persist.vendor.smartchg");
        if (methods.isEmpty()) throw new NoSuchMethodException("Smart charge checks not found");
        for (Method method : methods) hooks.constant(method, true);
    }

    /** Super wireless (reverse) charging speed option and its "connected" tip. */
    void superWireless(DexTargets targets) throws Throwable {
        List<Method> support = targets.usingStrings("super_wls", "boolean", "persist.vendor.tx.speed.control");
        if (support.isEmpty()) throw new NoSuchMethodException("Super wireless charge check not found");
        for (Method method : support) hooks.constant(method, true);
        for (Method tip : targets.matching("super_wls_tip", () -> MethodMatcher.create().usingStrings(List.of("key_is_connected_super_wls_tx")))) {
            if (tip.getReturnType() == boolean.class) {
                hooks.constant(tip, true);
            } else if (tip.getReturnType() == void.class && tip.getParameterCount() == 1 && tip.getParameterTypes()[0] == boolean.class) {
                hooks.install(tip, chain -> chain.proceed(new Object[]{true}));
            }
        }
    }

    // --- Game Turbo ---------------------------------------------------------------------------

    /** Wild Boost on devices whose "support_wild_boost" answer is no. */
    void wildBoost(DexTargets targets) throws Throwable {
        List<Method> methods = targets.usingStrings("wild_boost", "boolean", "support_wild_boost");
        if (methods.isEmpty()) throw new NoSuchMethodException("Wild Boost check not found");
        for (Method method : methods) hooks.constant(method, true);
    }

    /** No Wild mode / game toasts over the game. */
    void gameToasts(DexTargets targets) throws Throwable {
        int count = 0;
        for (String text : new String[]{"showWildModeToastView: ", "cancel game toast , isCanceled : "}) {
            for (Method method : targets.matching("game_toast_" + text.length(), () -> MethodMatcher.create().usingStrings(List.of(text)))) {
                if (method.getReturnType() != void.class) continue;
                hooks.install(method, chain -> null);
                count++;
            }
        }
        if (count == 0) throw new NoSuchMethodException("Game toasts not found");
    }

    /** Macros (Game Turbo's recorded taps) for every game, also those on Xiaomi's block list. */
    void macroAllGames(DexTargets targets) throws Throwable {
        List<Method> lists = new ArrayList<>();
        for (Method method : targets.matching("macro_list", () -> MethodMatcher.create().usingStrings(List.of("pref_gb_unsupport_macro_apps")))) {
            if (method.getParameterCount() == 0 && java.util.List.class.isAssignableFrom(method.getReturnType())) lists.add(method);
        }
        if (lists.isEmpty()) throw new NoSuchMethodException("Macro block list not found");
        for (Method method : lists) hooks.install(method, chain -> new ArrayList<String>());
        // The "is this game blocked" check that reads the list.
        for (Method list : lists) {
            String descriptor = new org.luckypray.dexkit.wrap.DexMethod(list).serialize();
            for (Method check : targets.matching("macro_check", () -> MethodMatcher.create().returnType("boolean")
                    .addInvoke(MethodMatcher.create().descriptor(descriptor)))) {
                hooks.constant(check, false);
            }
        }
        // The macro status provider's "may this game use macros" answers.
        for (Class<?> status : targets.classesUsing("macro_status", "content://com.xiaomi.macro.MacroStatusProvider/game_macro_change")) {
            for (Method method : status.getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class && !Modifier.isAbstract(method.getModifiers())
                        && (method.getParameterCount() == 2 || method.getParameterCount() == 3)) {
                    hooks.constant(method, true);
                }
            }
        }
    }

    /** The crosshair (gunsight) service for every game. */
    void crosshairService(DexTargets targets) throws Throwable {
        int count = 0;
        for (Class<?> owner : targets.classesUsing("gunsight", "gb_game_collimator_status")) {
            for (Method method : owner.getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class && method.getParameterCount() == 1
                        && method.getParameterTypes()[0] == String.class && !Modifier.isAbstract(method.getModifiers())) {
                    hooks.constant(method, true);
                    count++;
                }
            }
        }
        if (count == 0) throw new NoSuchMethodException("Crosshair support check not found");
    }

    /** Video toolbox effects stay on when the phone warms up (its board temperature reads 0). */
    void videoTemperature(DexTargets targets) throws Throwable {
        List<Method> methods = targets.usingStrings("video_temp", "float", "read board temp fail, node=");
        if (methods.isEmpty()) throw new NoSuchMethodException("Video toolbox temperature not found");
        for (Method method : methods) hooks.constant(method, 0.0f);
    }

    /** Dolby in the video toolbox without the game permission check. */
    void videoDolby(DexTargets targets) throws Throwable {
        int count = 0;
        for (Class<?> owner : targets.classesUsing("dolby_owner", "checkMiGamePermission error")) {
            for (Method method : targets.matching("dolby_" + owner.getName(), () -> MethodMatcher.create()
                    .declaredClass(owner.getName()).usingStrings(List.of("dolby")))) {
                if (method.getReturnType() != void.class) continue;
                hooks.install(method, chain -> null);
                count++;
            }
        }
        if (count == 0) throw new NoSuchMethodException("Dolby check not found");
    }

    // --- Network ------------------------------------------------------------------------------

    /**
     * When the monthly data plan runs out, the network assistant turns mobile data off and sets its
     * policy to "off". Both are skipped; the over-limit notification still comes.
     */
    void keepMobileData() throws Throwable {
        Class<?> telephony = Class.forName("com.miui.networkassistant.utils.TelephonyUtil", false, loader);
        Method set = telephony.getDeclaredMethod("setMobileDataState", android.content.Context.class, boolean.class);
        hooks.install(set, chain -> Boolean.FALSE.equals(chain.getArg(1)) ? null : chain.proceed());
        Method putInt = android.provider.Settings.Secure.class.getMethod("putInt", android.content.ContentResolver.class, String.class, int.class);
        hooks.install(putInt, chain -> {
            if ("mobile_policy".equals(chain.getArg(1)) && Integer.valueOf(0).equals(chain.getArg(2))) {
                for (StackTraceElement frame : new Throwable().getStackTrace()) {
                    if (frame.getClassName().startsWith("com.miui.networkassistant.service.tm.TrafficSimManager")) return true;
                }
            }
            return chain.proceed();
        });
    }

    /** The hotspot stays on past its daily limit; a warning notification is shown instead. */
    void keepHotspot() throws Throwable {
        Class<?> tether = Class.forName("com.miui.networkassistant.service.tm.TetherStatsManager", false, loader);
        Method over = tether.getDeclaredMethod("onTetherStatsOverLimit");
        java.lang.reflect.Field contextField = null;
        for (java.lang.reflect.Field field : tether.getDeclaredFields()) {
            if (field.getType() == android.content.Context.class) { field.setAccessible(true); contextField = field; }
        }
        final java.lang.reflect.Field context = contextField;
        hooks.install(over, chain -> {
            try {
                Class<?> notify = Class.forName("com.miui.networkassistant.utils.NotificationUtil", false, loader);
                Method warn = notify.getDeclaredMethod("sendTetherOverLimitWaringNotify", android.content.Context.class);
                warn.setAccessible(true);
                if (context != null) warn.invoke(null, context.get(chain.getThisObject()));
            } catch (Throwable unavailable) {
                hooks.error("hotspot warning", unavailable);
            }
            return null;
        });
    }

    // --- More from HyperCeiler's Security rules ------------------------------------------------

    /** Low-temperature extreme endurance (battery lasts longer in the cold) on every device. */
    void lowTempEndurance(DexTargets targets) throws Throwable {
        List<Method> methods = targets.usingStrings("low_temp", "boolean", "low_temperature_extreme_endurance_support");
        if (methods.isEmpty()) throw new NoSuchMethodException("Low temperature endurance check not found");
        for (Method method : methods) hooks.constant(method, true);
    }

    /** Car sickness relief and its reminder timing. */
    void carSickness(DexTargets targets) throws Throwable {
        int count = 0;
        for (Method method : targets.usingStrings("carsick", "boolean", "is_support_carsick")) {
            hooks.constant(method, true);
            count++;
        }
        try {
            Class<?> fragment = Class.forName(
                    "com.miui.carsickness.ui.CarSicknessReliefSettingsActivity$MotionSicknessReliefSettingsFragment", false, loader);
            Method create = fragment.getDeclaredMethod("onCreatePreferences", android.os.Bundle.class, String.class);
            for (Method called : targets.invokedBy("carsick_timing", create)) {
                Class<?>[] types = called.getParameterTypes();
                if (called.getReturnType() == boolean.class && types.length == 1 && types[0] == Context.class) {
                    hooks.constant(called, true);
                    count++;
                }
            }
        } catch (ClassNotFoundException | NoSuchMethodException absent) {
            // No settings page for reminder timing in this build.
        }
        if (count == 0) throw new NoSuchMethodException("Car sickness relief check not found");
    }

    /** Beauty in video calls: face tracking, privacy camera and auto light. */
    void callBeauty(DexTargets targets) throws Throwable {
        int count = 0;
        for (Method method : targets.usingStrings("beauty_pc", "boolean", "persist.vendor.camera.facetracker.support")) {
            hooks.constant(method, true);
            count++;
        }
        for (Method method : targets.usingStrings("beauty_light", "boolean", "taoyao")) {
            if (method.getParameterCount() == 0) { hooks.constant(method, true); count++; }
        }
        for (Method privacy : targets.matching("beauty_privacy", () -> MethodMatcher.create().usingStrings(List.of("persist.sys.privacy_camera")))) {
            Class<?>[] types = privacy.getParameterTypes();
            if (types.length >= 1 && types[0] == boolean.class) {
                hooks.install(privacy, chain -> {
                    Object[] args = chain.getArgs().toArray();
                    args[0] = true;
                    return chain.proceed(args);
                });
                count++;
            }
            for (Method method : privacy.getDeclaringClass().getDeclaredMethods()) {
                if (method.getReturnType() == boolean.class && method.getParameterCount() == 0
                        && !Modifier.isAbstract(method.getModifiers()) && !method.equals(privacy)) {
                    for (Method called : targets.invokedBy("beauty_privacy_" + method.getName(), method)) {
                        if (called.equals(privacy)) { hooks.constant(method, true); count++; break; }
                    }
                }
            }
        }
        if (count == 0) throw new NoSuchMethodException("Beauty checks not found");
    }

    /** The new video box (theatre mode page) on every device. */
    void newVideoBox(DexTargets targets) throws Throwable {
        List<Method> methods = targets.matching("vb_mode", () -> MethodMatcher.create()
                .usingStrings(List.of("TheatreModeUtils")).usingNumbers(List.of(32)));
        int count = 0;
        for (Method method : methods) {
            if (method.getReturnType() == boolean.class) { hooks.constant(method, true); count++; }
        }
        if (count == 0) throw new NoSuchMethodException("New video box check not found");
    }

    /** The app lock's number pad shows its digits in a new order every time. */
    void appLockScramble() throws Throwable {
        Class<?> pad = Class.forName("com.miui.applicationlock.widget.MiuiNumericInputView", false, loader);
        java.lang.reflect.Constructor<?>[] constructors = pad.getDeclaredConstructors();
        if (constructors.length == 0) throw new NoSuchMethodException("Numeric pad constructor");
        for (java.lang.reflect.Constructor<?> constructor : constructors) {
            hooks.install(constructor, chain -> {
                Object result = chain.proceed();
                try {
                    shuffle((android.widget.LinearLayout) chain.getThisObject());
                } catch (Throwable failure) {
                    hooks.error("app lock pad", failure);
                }
                return result;
            });
        }
    }

    /** Digits 1-9 and 0 change places; the bottom-left and bottom-right keys stay. */
    private static void shuffle(android.widget.LinearLayout keys) {
        if (keys.getChildCount() < 4) return;
        List<android.view.View> digits = new ArrayList<>();
        android.view.View bottomLeft = null;
        android.view.View bottomRight = null;
        for (int row = 0; row < 4; row++) {
            android.view.ViewGroup cols = (android.view.ViewGroup) keys.getChildAt(row);
            if (cols.getChildCount() < 3) return;
            for (int col = 0; col < 3; col++) {
                android.view.View key = cols.getChildAt(col);
                if (row == 3 && col == 0) bottomLeft = key;
                else if (row == 3 && col == 2) bottomRight = key;
                else digits.add(key);
            }
        }
        for (int row = 0; row < 4; row++) ((android.view.ViewGroup) keys.getChildAt(row)).removeAllViews();
        java.util.Collections.shuffle(digits);
        int next = 0;
        for (int row = 0; row < 4; row++) {
            android.view.ViewGroup cols = (android.view.ViewGroup) keys.getChildAt(row);
            for (int col = 0; col < 3; col++) {
                if (row == 3 && col == 0) cols.addView(bottomLeft);
                else if (row == 3 && col == 2) cols.addView(bottomRight);
                else cols.addView(digits.get(next++));
            }
        }
    }

    /** No low battery pop-up and sound from the Security app. */
    void quietLowBattery() throws Throwable {
        Class<?> system = android.provider.Settings.System.class;
        Method getInt = system.getMethod("getInt", android.content.ContentResolver.class, String.class, int.class);
        Method getIntPlain = system.getMethod("getInt", android.content.ContentResolver.class, String.class);
        Method getString = system.getMethod("getString", android.content.ContentResolver.class, String.class);
        hooks.install(getInt, chain -> "low_battery_dialog_disabled".equals(chain.getArg(1)) ? Integer.valueOf(1) : chain.proceed());
        hooks.install(getIntPlain, chain -> "low_battery_dialog_disabled".equals(chain.getArg(1)) ? Integer.valueOf(1) : chain.proceed());
        hooks.install(getString, chain -> "low_battery_sound".equals(chain.getArg(1)) ? null : chain.proceed());
    }

    /**
     * App info's "App info" page gets more rows, as in HyperCeiler: version code, UID, data folder,
     * APK file, minimum and target SDK. Tapping a row copies its value.
     */
    void moreAppDetails() throws Throwable {
        Class<?> fragment = Class.forName("com.miui.appmanager.fragment.AMAppInformationFragment", false, loader);
        Class<?> category = Class.forName("androidx.preference.PreferenceCategory", false, loader);
        Method addRow = null;
        java.lang.reflect.Field infoField = null;
        java.lang.reflect.Field categoryField = null;
        for (Method method : fragment.getDeclaredMethods()) {
            Class<?>[] t = method.getParameterTypes();
            if (method.getReturnType() == void.class && t.length == 3 && t[0] == String.class && t[1] == String.class && t[2] == String.class) addRow = method;
        }
        for (java.lang.reflect.Field field : fragment.getDeclaredFields()) {
            if (field.getType() == android.content.pm.PackageInfo.class) { field.setAccessible(true); infoField = field; }
            if (category.isAssignableFrom(field.getType())) { field.setAccessible(true); categoryField = field; }
        }
        if (addRow == null || infoField == null || categoryField == null) throw new NoSuchMethodException("App information rows");
        Class<?> text = Class.forName("miuix.preference.TextPreference", false, loader);
        Class<?> preference = Class.forName("androidx.preference.Preference", false, loader);
        Class<?> groupType = Class.forName("androidx.preference.PreferenceGroup", false, loader);
        // The click listener's interface is renamed in some builds: take it from the setter.
        Method setOnClickFound = null;
        for (Method method : preference.getMethods()) {
            if (method.getName().equals("setOnPreferenceClickListener") && method.getParameterCount() == 1) setOnClickFound = method;
        }
        final Method setOnClick = setOnClickFound;
        final Class<?> clickListener = setOnClick == null ? null : setOnClick.getParameterTypes()[0];
        java.lang.reflect.Constructor<?> newText = text.getConstructor(Context.class);
        Method setKey = preference.getMethod("setKey", String.class);
        Method setTitle = preference.getMethod("setTitle", CharSequence.class);
        Method setText = text.getMethod("setText", String.class);
        Method add = groupType.getMethod("addPreference", preference);
        Method find = groupType.getMethod("findPreference", CharSequence.class);
        Method context = fragment.getMethod("getContext");
        final java.lang.reflect.Field info = infoField;
        final java.lang.reflect.Field group = categoryField;
        hooks.install(addRow, chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            // The stock rows come in one go; ours follow once they are all in.
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                try {
                    Object section = group.get(self);
                    android.content.pm.PackageInfo pkg = (android.content.pm.PackageInfo) info.get(self);
                    Context ctx = (Context) context.invoke(self);
                    if (section == null || pkg == null || ctx == null || find.invoke(section, "xsecurity_uid") != null) return;
                    boolean tr = "tr".equals(java.util.Locale.getDefault().getLanguage());
                    android.content.pm.ApplicationInfo app = pkg.applicationInfo;
                    String[][] rows = {
                            {"xsecurity_version_code", tr ? "Sürüm kodu" : "Version code", String.valueOf(pkg.getLongVersionCode())},
                            {"xsecurity_uid", "UID", String.valueOf(app.uid)},
                            {"xsecurity_data", tr ? "Veri klasörü" : "Data folder", app.dataDir},
                            {"xsecurity_apk", tr ? "APK dosyası" : "APK file", app.sourceDir},
                            {"xsecurity_min_sdk", tr ? "En düşük SDK" : "Minimum SDK", String.valueOf(app.minSdkVersion)},
                            {"xsecurity_target_sdk", tr ? "Hedef SDK" : "Target SDK", String.valueOf(app.targetSdkVersion)},
                    };
                    for (String[] row : rows) {
                        Object item = newText.newInstance(ctx);
                        setKey.invoke(item, row[0]);
                        setTitle.invoke(item, row[1]);
                        setText.invoke(item, row[2]);
                        if (clickListener == null || !clickListener.isInterface()) { add.invoke(section, item); continue; }
                        Object listener = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[]{clickListener}, (proxy, method, args) -> {
                            switch (method.getName()) {
                                case "hashCode":
                                    return System.identityHashCode(proxy);
                                case "equals":
                                    return proxy == args[0];
                                case "toString":
                                    return "xsecurity_copy";
                                default:
                                    android.content.ClipboardManager clip = (android.content.ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                                    clip.setPrimaryClip(android.content.ClipData.newPlainText(row[1], row[2]));
                                    android.widget.Toast.makeText(ctx, tr ? "Kopyalandı" : "Copied", android.widget.Toast.LENGTH_SHORT).show();
                                    return true;
                            }
                        });
                        setOnClick.invoke(item, listener);
                        add.invoke(section, item);
                    }
                } catch (Throwable failure) {
                    hooks.error("more app details", failure);
                }
            });
            return result;
        });
    }
}
