package io.github.leekin2.xsecurity;

import android.content.Context;
import android.os.IBinder;
import android.os.SystemClock;
import io.github.libxposed.api.XposedModule;
import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Hooks in the Security app (com.miui.securitycenter). Each feature mirrors one patch of the
 * modded Security app (SecurityMod 13.6.3), found by comparing it with the stock 13.6.6.
 */
final class SecurityHooks {
    private static final String POWER = "com.miui.powercenter.";
    private final Hooks hooks;
    private final XposedModule module;
    private final ClassLoader loader;

    SecurityHooks(Hooks hooks, XposedModule module, ClassLoader loader) {
        this.hooks = hooks;
        this.module = module;
        this.loader = loader;
    }

    // Processes of the Security app. Each hook runs only where the code it changes runs; the other
    // processes (.cache, :cache, .uniauth) get nothing, so they stay out of the log too.
    private static final int MAIN = 1, REMOTE = 2, UI = 4, POWER_PROC = 8, BOOT = 16;

    static int processFlag(String process) {
        if (process == null || process.equals("com.miui.securitycenter")) return MAIN;
        if (process.equals("com.miui.securitycenter.remote")) return REMOTE;
        if (process.equals("com.miui.securitycenter:ui")) return UI;
        if (process.equals("com.miui.securitycenter.power")) return POWER_PROC;
        if (process.equals("com.miui.securitycenter.bootaware")) return BOOT;
        return 0;
    }

    void install(String apkPath, String dataDir, String process) {
        int here = processFlag(process);
        if (here == 0) return;
        // Fixed class names first: they need no scan.
        if (in(here, MAIN | REMOTE | POWER_PROC) && Features.enabled(Features.BATTERY_RANKING)) run("battery ranking", this::batteryRanking);
        if (in(here, MAIN | REMOTE | POWER_PROC) && Features.enabled(Features.BATTERY_SCREEN_TIME)) run("screen time", this::screenTime);
        if (in(here, MAIN) && Features.enabled(Features.BATTERY_HEALTH)) run("battery health", this::batteryHealth);
        if (in(here, MAIN) && Features.enabled(Features.BATTERY_HEALTH)) run("battery health percent", this::batteryHealthPercent);
        if (in(here, MAIN) && Features.enabled(Features.PERM_NO_WAIT)) run("permission wait", this::permissionWait);
        if (in(here, MAIN | REMOTE) && Features.enabled(Features.WARNINGS)) run("car accident warning", this::carAccident);
        if (in(here, MAIN) && Features.enabled(Features.WARNINGS)) run("earthquake alert", this::earthquakeAlert);
        if (in(here, MAIN) && Features.enabled(Features.DOCK)) run("sidebar 50 apps", this::dockLimit);
        if (in(here, MAIN) && Features.enabled(Features.APP_INFO)) run("app info actions", this::appInfoActions);
        if (in(here, MAIN) && Features.enabled(Features.APP_INFO)) run("more app details", () -> new SecurityExtraHooks(hooks, loader).moreAppDetails());
        if (in(here, MAIN) && Features.enabled(Features.APPLOCK_SCRAMBLE)) run("app lock scramble", () -> new SecurityExtraHooks(hooks, loader).appLockScramble());
        if (in(here, MAIN | REMOTE | POWER_PROC) && Features.enabled(Features.LOW_BATTERY_QUIET)) run("quiet low battery", () -> new SecurityExtraHooks(hooks, loader).quietLowBattery());
        if (in(here, REMOTE) && Features.enabled(Features.NET_QUOTA_KEEP)) run("keep mobile data", () -> new SecurityExtraHooks(hooks, loader).keepMobileData());
        if (in(here, REMOTE) && Features.enabled(Features.NET_HOTSPOT_KEEP)) run("keep hotspot", () -> new SecurityExtraHooks(hooks, loader).keepHotspot());
        if (in(here, MAIN) && Features.enabled(Features.NET_APP_CONTROLS)) run("per-app network controls", () -> new NetworkControlHooks(hooks, loader).install());
        if (in(here, BOOT) && Features.enabled(Features.SIM_SUCCESS_DIALOG_OFF)) run("SIM verification dialog", this::simSuccessDialog);
        if (in(here, MAIN | REMOTE | POWER_PROC)) {
            try (DexTargets targets = new DexTargets(loader, apkPath, dataDir)) {
                SecurityBehaviorHooks behavior = new SecurityBehaviorHooks(hooks);
                if (Features.enabled(Features.POWER_SAVE_REFRESH)) run("power saver keeps refresh", () -> behavior.keepRefresh(targets));
                if (Features.enabled(Features.POWER_SAVE_BRIGHTNESS)) run("power saver keeps brightness", () -> behavior.keepBrightness(targets));
            } catch (Throwable failure) {
                hooks.error("Power saver scan unavailable; stock behaviour kept", failure);
            }
        }
        if (!in(here, MAIN | REMOTE | UI)) return;
        try (DexTargets targets = new DexTargets(loader, apkPath, dataDir)) {
            if (Features.enabled(Features.GAME_KEEP_ENABLED)) run("Game Turbo stays enabled", () -> new SecurityBehaviorHooks(hooks).keepGameEnabled(targets));
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.CHARGE_PROTECT)) run("charge protection", () -> chargeProtect(targets));
            if (in(here, MAIN | UI) && Features.enabled(Features.DOCK)) run("sidebar 50 apps (new)", () -> dockLimitNew(targets));
            if (Features.enabled(Features.GT_HDR)) run("game HDR", () -> alwaysTrue(targets, "gt_hdr", "support_displayfeature_gamemode_HDR"));
            if (Features.enabled(Features.GT_SUPER_RES)) run("super resolution", () -> superResolution(targets));
            if (Features.enabled(Features.GT_DYNAMIC_PERF)) run("dynamic performance", () -> alwaysTrue(targets, "gt_dynperf", "persist.sys.smartop.support_dynamic_performance"));
            if (Features.enabled(Features.GT_TOUCH)) run("game touch", () -> alwaysTrue(targets, "gt_touch", "support_touchfeature_gamemode"));
            if (Features.enabled(Features.GT_MI_TIME)) run("game Mi time", () -> alwaysTrue(targets, "gt_mitime", "support_game_mi_time"));
            if (in(here, MAIN) && Features.enabled(Features.APP_INFO)) run("open by default", () -> openByDefault(targets));
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.ROOT_CHECK_OFF)) run("root check off", () -> alwaysFalse(targets, "root_check", "key_check_item_root"));
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.USB_INSTALL_CHECK_OFF)) run("USB install check off", () -> alwaysFalse(targets, "usb_install_check", "permcenter_install_intercept_enabled"));
            if (in(here, MAIN) && Features.enabled(Features.SECOND_SPACE)) run("second space", () -> secondSpace(targets));
            if (in(here, REMOTE) && Features.enabled(Features.DATA_USAGE)) run("data usage in control center", () -> dataUsage(targets));
            if (in(here, MAIN) && Features.enabled(Features.TOOLBOX)) run("toolbox layout", () -> toolboxLayout(targets));
            for (Check check : CHECKS) {
                if (in(here, check.processes()) && Features.enabled(check.feature())) run(check.name(), () -> answer(targets, check));
            }
            SecurityExtraHooks extra = new SecurityExtraHooks(hooks, loader);
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.SMART_CHARGE)) run("smart charge", () -> extra.smartCharge(targets));
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.SUPER_WIRELESS)) run("super wireless charge", () -> extra.superWireless(targets));
            if (Features.enabled(Features.GAME_MODES)) run("Wild Boost support", () -> extra.wildBoost(targets));
            if (Features.enabled(Features.GAME_TOAST_OFF)) run("game toasts off", () -> extra.gameToasts(targets));
            if (Features.enabled(Features.GAME_MACRO)) run("macro for all games", () -> extra.macroAllGames(targets));
            if (Features.enabled(Features.GT_EXTRAS)) run("crosshair service", () -> extra.crosshairService(targets));
            if (Features.enabled(Features.VIDEO_TOOLBOX)) run("video toolbox temperature", () -> extra.videoTemperature(targets));
            if (Features.enabled(Features.VIDEO_TOOLBOX)) run("video Dolby", () -> extra.videoDolby(targets));
            if (Features.enabled(Features.VIDEO_TOOLBOX)) run("new video box", () -> extra.newVideoBox(targets));
            if (Features.enabled(Features.CALL_TOOLBOX)) run("call beauty", () -> extra.callBeauty(targets));
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.BATTERY_LIFE)) run("low temperature endurance", () -> extra.lowTempEndurance(targets));
            if (in(here, MAIN | REMOTE) && Features.enabled(Features.CAR_SICKNESS)) run("car sickness relief", () -> extra.carSickness(targets));
        } catch (Throwable failure) {
            hooks.error("Method scan unavailable; scanned features stay off", failure);
        }
    }

    private static boolean in(int here, int processes) { return (here & processes) != 0; }

    private interface Step { void run() throws Throwable; }

    /** A device check found by the text it uses, and the answer the modded Security app gave. */
    private record Check(String feature, String name, boolean value, String... strings) {
        /** Game Turbo and video tools are asked in the services (.remote) and the sidebar (:ui); the rest in the app and its services. */
        int processes() {
            return switch (feature) {
                case Features.VIDEO_TOOLBOX, Features.CALL_TOOLBOX, Features.GAME_MODES, Features.GT_EXTRAS, Features.GAME_NETWORK -> MAIN | REMOTE | UI;
                default -> MAIN | REMOTE;
            };
        }
    }

    private static final Check[] CHECKS = {
            // Video toolbox, theatre mode (Dolby, 3D LUT) and video effects.
            new Check(Features.VIDEO_TOOLBOX, "video toolbox", true, "is_support_video_tool_box"),
            new Check(Features.VIDEO_TOOLBOX, "theatre 3D", true, "isSupportCinemaVisionWith3d: "),
            new Check(Features.VIDEO_TOOLBOX, "theatre", true, "Immersive="),
            new Check(Features.VIDEO_TOOLBOX, "frame insertion", true, "debug.config.media.video.frc.support"),
            new Check(Features.VIDEO_TOOLBOX, "frame insertion hw", true, "ro.vendor.media.video.frc.support"),
            new Check(Features.VIDEO_TOOLBOX, "MEMC", true, "debug.config.media.video.mimemc.support"),
            new Check(Features.VIDEO_TOOLBOX, "AI picture", true, "debug.config.media.video.ais.support"),
            new Check(Features.VIDEO_TOOLBOX, "AI enhance", true, "debug.config.media.video.aie.support"),
            new Check(Features.VIDEO_TOOLBOX, "video processing", true, "ro.vendor.media.video.vpp.support"),
            new Check(Features.VIDEO_TOOLBOX, "call AI picture", true, "isSupportConversationAIS return false cause of pkgName is null"),
            // Call toolbox: hand gestures and effects in video calls.
            new Check(Features.CALL_TOOLBOX, "call gestures", true, "persist.vendor.camera.gesture.emoji.support"),
            // Wild Boost and the Balanced / Performance switch.
            new Check(Features.GAME_MODES, "Wild Boost", true, "isSupport wild model "),
            new Check(Features.GAME_MODES, "game mode switch", true, "isSupportGameModeChange = "),
            new Check(Features.GAME_MODES, "performance mode", true, "isSupportPerformanceMode = "),
            // Game Turbo tools.
            new Check(Features.GT_EXTRAS, "crosshair", true, "support_game_gunsight"),
            new Check(Features.GT_EXTRAS, "paper eye care", true, "support_paper_eyecare"),
            new Check(Features.GT_EXTRAS, "video DFPS", true, "support_video_dfps"),
            new Check(Features.GT_EXTRAS, "game HDR (MTK)", true, "ro.vendor.pq.disp_game_HDR_support"),
            new Check(Features.GT_EXTRAS, "Mirihi", true, "hasMirihiSupport"),
            new Check(Features.GT_EXTRAS, "dual SIM DSDA", true, "miui_dsda"),
            new Check(Features.GT_EXTRAS, "enhanced DSDA", true, "isEnhancedDsdaSupported"),
            new Check(Features.GT_EXTRAS, "second SIM call reject", true, "isViceImsAutoRejectSupport"),
            new Check(Features.GT_EXTRAS, "GWSD", true, "isGwsdSupport"),
            new Check(Features.GT_EXTRAS, "screen-off hold", false, "remove_screen_off_hold_on"),
            new Check(Features.GT_EXTRAS, "game wallpaper", false, "remove_vgpaper"),
            // Game Turbo network tools: Wi-Fi low latency, RPS network mode, network optimisation.
            new Check(Features.GAME_NETWORK, "Wi-Fi low latency", true, "support_wifi_low_latency_mode"),
            new Check(Features.GAME_NETWORK, "network RPS mode", true, "support_network_rps_mode"),
            new Check(Features.GAME_NETWORK, "network optimisation", true, "isSupportNetworkOpt : "),
            // Battery life and power tools.
            new Check(Features.BATTERY_LIFE, "battery life", true, "isBatteryLifeFunctionSupported"),
            new Check(Features.POWER_EXTRAS, "hang up with screen off", true, "support hangup while screen off: "),
            new Check(Features.POWER_EXTRAS, "port moisture", true, "isSupport portDamp = "),
            new Check(Features.POWER_EXTRAS, "smart FPS", true, "support_smart_fps"),
            // Storage defragmentation (FBO).
            new Check(Features.FBO, "FBO rules", true, "Fbo cloud rules = "),
            new Check(Features.FBO, "FBO state", true, "FBO_STATE_OPEN"),
    };

    private void answer(DexTargets targets, Check check) throws Throwable {
        List<Method> methods = targets.usingStrings("check_" + check.name(), "boolean", check.strings());
        if (methods.isEmpty()) throw new NoSuchMethodException("No check uses " + String.join(", ", check.strings()));
        for (Method method : methods) hooks.constant(method, check.value());
    }

    /**
     * Control Center / notification shade show "used this month" only when the data plan answer says
     * type 2 (used, no plan). Without a carrier brand set up the app answers -1 and they stay empty;
     * the modded app answered 2 there.
     */
    private void dataUsage(DexTargets targets) throws Throwable {
        List<Method> methods = targets.usingStrings("data_usage", "java.lang.String[]", "query data usage ");
        if (methods.isEmpty()) throw new NoSuchMethodException("Traffic details method not found");
        for (Method method : methods) {
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                if (result instanceof String[] details && details.length == 3
                        && "-1".equals(details[2]) && details[0] != null && !details[0].isEmpty()) {
                    details[2] = "2";
                }
                return result;
            });
        }
    }

    /**
     * App info page as in the modded app: the package name under the version, a row of Open / Path /
     * Open by default buttons inside the app card, and the stock Disable item (hidden for most apps)
     * in the bottom bar.
     */
    private void appInfoActions() throws Throwable {
        Class<?> fragment = Class.forName("com.miui.appmanager.fragment.ApplicationsDetailsFragment", false, loader);
        Class<?> title = Class.forName("com.miui.appmanager.widget.AppDetailTitlePreference", false, loader);
        Method bind = null;
        for (Method method : title.getDeclaredMethods()) {
            if (method.getName().equals("onBindViewHolder") && method.getParameterCount() == 1) bind = method;
        }
        if (bind == null) throw new NoSuchMethodException("AppDetailTitlePreference.onBindViewHolder");
        java.util.List<Field> textFields = new java.util.ArrayList<>();
        for (Field field : title.getDeclaredFields()) {
            if (field.getType() == android.widget.TextView.class) {
                field.setAccessible(true);
                textFields.add(field);
            }
        }
        hooks.install(bind, chain -> {
            Object result = chain.proceed();
            try {
                for (Field field : textFields) {
                    android.widget.TextView text = (android.widget.TextView) field.get(chain.getThisObject());
                    if (text == null) continue;
                    String name = idName(text);
                    if ("app_manager_details_appversion".equals(name)) {
                        decorateHeader(text);
                        break;
                    }
                }
            } catch (Throwable failure) {
                hooks.error("app info header", failure);
            }
            return result;
        });
        // Bottom bar: the stock Disable item (id 6) and the top Share item are hidden for many apps
        // (Disable for most user apps, Share for system apps and split APKs). Both stay visible here
        // and both are handled by XSecurity, so they work for every app.
        Field[] menuFields = menuItemFields(fragment);
        Method activityOf = fragment.getMethod("getActivity");
        hooks.install(fragment.getMethod("onCreateOptionsMenu", android.view.Menu.class, android.view.MenuInflater.class), chain -> {
            Object result = chain.proceed();
            try {
                Object self = chain.getThisObject();
                android.view.Menu menu = (android.view.Menu) chain.getArg(0);
                android.app.Activity host = (android.app.Activity) activityOf.invoke(self);
                if (host == null) return result;
                pinVisible(self, menuFields, menu.findItem(DISABLE_ID));
                pinVisible(self, menuFields, menu.findItem(shareId(host)));
                String pkg = packageOf(host);
                android.view.MenuItem disable = menu.findItem(DISABLE_ID);
                if (disable != null && pkg != null) disable.setTitle(disableTitle(host, isDisabled(host, pkg)));
            } catch (Throwable failure) {
                hooks.error("app info menu", failure);
            }
            return result;
        });
        Method selected = null;
        for (Method method : fragment.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 1 && types[0] == android.view.MenuItem.class && method.getReturnType() == void.class) {
                if (selected == null || method.getName().equals("optionsItemSelected")) selected = method;
            }
        }
        if (selected == null) throw new NoSuchMethodException("ApplicationsDetailsFragment.optionsItemSelected");
        hooks.install(selected, chain -> {
            try {
                android.view.MenuItem item = (android.view.MenuItem) chain.getArg(0);
                android.app.Activity host = (android.app.Activity) activityOf.invoke(chain.getThisObject());
                String pkg = packageOf(host);
                if (item != null && host != null && pkg != null) {
                    boolean tr = "tr".equals(Locale.getDefault().getLanguage());
                    if (item.getItemId() == DISABLE_ID) {
                        toggleEnabled(host, pkg, item, tr);
                        return null;
                    }
                    if (item.getItemId() == shareId(host)) {
                        shareApk(host, pkg, tr);
                        return null;
                    }
                }
            } catch (Throwable failure) {
                hooks.error("app info menu action", failure);
            }
            return chain.proceed();
        });
    }

    private static final int DISABLE_ID = 6;
    /** Apps whose loss leaves the phone unusable or unable to undo it: never disabled from here. */
    private static final java.util.Set<String> CORE_APPS = java.util.Set.of(
            "android", "com.android.systemui", "com.android.settings", "com.android.phone",
            "com.miui.securitycenter", "com.miui.home", "com.android.providers.settings",
            "com.lbe.security.miui", "com.miui.packageinstaller", "com.android.packageinstaller",
            "com.google.android.packageinstaller", "com.android.permissioncontroller",
            "com.google.android.permissioncontroller", "io.github.leekin2.xsecurity");

    private static Field[] menuItemFields(Class<?> fragment) {
        java.util.List<Field> fields = new java.util.ArrayList<>();
        for (Field field : fragment.getDeclaredFields()) {
            if (field.getType() == android.view.MenuItem.class && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fields.add(field);
            }
        }
        return fields.toArray(new Field[0]);
    }

    /**
     * Shows the item and swaps the fragment's own reference to it for a view that ignores "hide" and
     * "disable", so the stock code (which checks again later) cannot take it away.
     */
    private static void pinVisible(Object fragment, Field[] fields, android.view.MenuItem item) throws IllegalAccessException {
        if (item == null) return;
        item.setVisible(true);
        item.setEnabled(true);
        for (Field field : fields) {
            if (field.get(fragment) != item) continue;
            Object pinned = java.lang.reflect.Proxy.newProxyInstance(android.view.MenuItem.class.getClassLoader(),
                    new Class<?>[]{android.view.MenuItem.class}, (proxy, method, args) -> {
                        String name = method.getName();
                        if ((name.equals("setVisible") || name.equals("setEnabled")) && args != null && args.length == 1) {
                            method.invoke(item, true);
                            return proxy;
                        }
                        if (name.equals("equals") && args != null && args.length == 1) return args[0] == proxy || args[0] == item;
                        if (name.equals("hashCode") && args == null) return item.hashCode();
                        Object answer = method.invoke(item, args);
                        return answer == item ? proxy : answer;
                    });
            field.set(fragment, pinned);
        }
    }

    private static int shareId(Context context) {
        return context.getResources().getIdentifier("app_share", "id", context.getPackageName());
    }

    private static boolean isDisabled(Context context, String pkg) {
        try {
            int state = context.getPackageManager().getApplicationEnabledSetting(pkg);
            return state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    || state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                    || state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED;
        } catch (Throwable unknown) {
            return false;
        }
    }

    /** The Security app's own "Disable" / "Enable" words. */
    private static CharSequence disableTitle(Context context, boolean disabled) {
        boolean tr = "tr".equals(Locale.getDefault().getLanguage());
        return disabled ? stockString(context, "app_manager_enable_text", tr ? "Etkinleştir" : "Enable")
                : stockString(context, "app_manager_disable_text", tr ? "Devre dışı" : "Disable");
    }

    private static String stockString(Context context, String name, String fallback) {
        int id = context.getResources().getIdentifier(name, "string", context.getPackageName());
        return id != 0 ? context.getString(id) : fallback;
    }

    /** Disable / enable as the stock page does (the Security app may change any app's state). */
    private static void toggleEnabled(android.app.Activity host, String pkg, android.view.MenuItem item, boolean tr) {
        if (CORE_APPS.contains(pkg)) {
            android.widget.Toast.makeText(host, tr ? "Bu sistem uygulaması devre dışı bırakılamaz" : "This system app cannot be disabled",
                    android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        boolean disabled = isDisabled(host, pkg);
        Runnable apply = () -> {
            try {
                host.getPackageManager().setApplicationEnabledSetting(pkg, disabled
                        ? android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        : android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER, 0);
                boolean now = isDisabled(host, pkg);
                if (now == disabled) throw new IllegalStateException(tr ? "durum değişmedi" : "state unchanged");
                item.setTitle(disableTitle(host, now));
                android.widget.Toast.makeText(host, now
                        ? stockString(host, "app_manager_disabled", tr ? "Devre dışı bırakıldı" : "Disabled")
                        : stockString(host, "app_manager_enabled", tr ? "Etkinleştirildi" : "Enabled"), android.widget.Toast.LENGTH_SHORT).show();
            } catch (Throwable denied) {
                android.widget.Toast.makeText(host, (tr ? "Yapılamadı: " : "Failed: ") + denied.getMessage(), android.widget.Toast.LENGTH_LONG).show();
            }
        };
        if (disabled) {
            apply.run();
            return;
        }
        String title = stockString(host, "app_manager_disable_dlg_title", tr ? "Uygulama devre dışı bırakılsın mı?" : "Disable this app?");
        String text = stockString(host, "app_disable_dlg_text",
                tr ? "Devre dışı bırakılan uygulama, yeniden etkinleştirilene kadar çalışmaz." : "A disabled app stops working until you enable it again.");
        String positive = stockString(host, "app_disable_dlg_positive", tr ? "Devre dışı bırak" : "Disable");
        dialog(host, title, text, positive, apply);
    }

    /**
     * Shares the APK of any app. The Security app's file provider only reaches /data/app, so the APK
     * is copied under a readable name to its shared cache folder ("security_share") first.
     */
    private static void shareApk(android.app.Activity host, String pkg, boolean tr) {
        Context app = host.getApplicationContext();
        android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
        android.widget.Toast.makeText(host, tr ? "APK hazırlanıyor…" : "Preparing APK…", android.widget.Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                android.content.pm.PackageManager pm = app.getPackageManager();
                android.content.pm.PackageInfo info = pm.getPackageInfo(pkg, 0);
                String label = String.valueOf(info.applicationInfo.loadLabel(pm)).replaceAll("[^\\p{L}\\p{N}._ -]", "_").trim();
                if (label.isEmpty()) label = pkg;
                java.io.File folder = new java.io.File(app.getExternalCacheDir(), "security_share");
                if (!folder.isDirectory() && !folder.mkdirs()) throw new java.io.IOException("no share folder");
                java.io.File[] old = folder.listFiles();
                if (old != null) for (java.io.File file : old) if (file.getName().endsWith(".apk")) file.delete();
                java.io.File copy = new java.io.File(folder, label + "_" + info.versionName + ".apk");
                java.nio.file.Files.copy(new java.io.File(info.applicationInfo.sourceDir).toPath(), copy.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                android.net.Uri uri = new android.net.Uri.Builder().scheme("content")
                        .authority("com.miui.securitycenter.zman.fileProvider")
                        .encodedPath("/cache/" + android.net.Uri.encode(copy.getName())).build();
                main.post(() -> {
                    try {
                        android.content.Intent send = new android.content.Intent(android.content.Intent.ACTION_SEND);
                        send.setType("application/vnd.android.package-archive");
                        send.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                        send.setClipData(android.content.ClipData.newRawUri(copy.getName(), uri));
                        send.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        android.content.Intent chooser = android.content.Intent.createChooser(send, null);
                        chooser.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        host.startActivity(chooser);
                    } catch (Throwable failure) {
                        android.widget.Toast.makeText(host, String.valueOf(failure.getMessage()), android.widget.Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Throwable failure) {
                main.post(() -> android.widget.Toast.makeText(host, (tr ? "Paylaşılamadı: " : "Cannot share: ") + failure.getMessage(),
                        android.widget.Toast.LENGTH_LONG).show());
            }
        }, "XSecurity-share").start();
    }

    /** A dialog in the Security app's own HyperOS style (dark in dark mode); plain Android dialog as fallback. */
    private static void dialog(android.app.Activity host, CharSequence title, CharSequence message,
                               CharSequence positive, Runnable onPositive) {
        android.content.DialogInterface.OnClickListener ok = (d, w) -> onPositive.run();
        CharSequence cancel = host.getText(android.R.string.cancel);
        try {
            Class<?> builder = Class.forName("miuix.appcompat.app.AlertDialog$Builder", true, host.getClassLoader());
            Object b = builder.getConstructor(Context.class).newInstance(host);
            builder.getMethod("setTitle", CharSequence.class).invoke(b, title);
            builder.getMethod("setMessage", CharSequence.class).invoke(b, message);
            builder.getMethod("setPositiveButton", CharSequence.class, android.content.DialogInterface.OnClickListener.class).invoke(b, positive, ok);
            builder.getMethod("setNegativeButton", CharSequence.class, android.content.DialogInterface.OnClickListener.class).invoke(b, cancel, null);
            builder.getMethod("show").invoke(b);
            return;
        } catch (Throwable unavailable) {
            // Not a HyperOS build of the app: plain dialog below.
        }
        new android.app.AlertDialog.Builder(host, android.R.style.Theme_DeviceDefault_DayNight)
                .setTitle(title).setMessage(message)
                .setPositiveButton(positive, ok)
                .setNegativeButton(cancel, null)
                .show();
    }

    private static String idName(android.view.View view) {
        try {
            return view.getId() == android.view.View.NO_ID ? null : view.getResources().getResourceEntryName(view.getId());
        } catch (Throwable unknown) {
            return null;
        }
    }

    private static android.app.Activity activityOf(Context context) {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof android.app.Activity) return (android.app.Activity) context;
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    /** Header layout: [icon | label, version] → [icon | label, version, package] over a button row. */
    private static void decorateHeader(android.widget.TextView version) {
        android.view.ViewGroup column = (android.view.ViewGroup) version.getParent();
        if (column == null || !(column.getParent() instanceof android.widget.LinearLayout)) return;
        android.widget.LinearLayout root = (android.widget.LinearLayout) column.getParent();
        // Already split into [icon | texts] over the buttons: the card root is one level up.
        if ("xsecurity_top".equals(root.getTag()) && root.getParent() instanceof android.widget.LinearLayout) {
            root = (android.widget.LinearLayout) root.getParent();
        }
        android.app.Activity host = activityOf(root.getContext());
        String pkg = packageOf(host);
        if (host == null || pkg == null) return;
        Object done = root.getTag(0x58534545);
        if (pkg.equals(done)) return;
        android.view.View oldPackage = column.findViewWithTag("xsecurity_pkg");
        if (oldPackage != null) column.removeView(oldPackage);
        android.view.View oldRow = root.findViewWithTag("xsecurity_actions");
        if (oldRow != null) root.removeView(oldRow);

        android.widget.TextView packageLine = new android.widget.TextView(host);
        packageLine.setTag("xsecurity_pkg");
        packageLine.setText(pkg);
        packageLine.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, version.getTextSize());
        packageLine.setTextColor(version.getCurrentTextColor());
        packageLine.setSingleLine(true);
        packageLine.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        packageLine.setTextIsSelectable(true);
        column.addView(packageLine, column.indexOfChild(version) + 1, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));

        if (root.getOrientation() == android.widget.LinearLayout.HORIZONTAL) {
            // Move icon and texts into their own line so the buttons can sit under them.
            android.widget.LinearLayout top = new android.widget.LinearLayout(host);
            top.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            top.setTag("xsecurity_top");
            while (root.getChildCount() > 0) {
                android.view.View child = root.getChildAt(0);
                android.view.ViewGroup.LayoutParams params = child.getLayoutParams();
                root.removeViewAt(0);
                top.addView(child, params);
            }
            root.setOrientation(android.widget.LinearLayout.VERTICAL);
            root.addView(top, new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        android.widget.LinearLayout row = actionRow(host, pkg);
        row.setTag("xsecurity_actions");
        root.addView(row, new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
        root.setTag(0x58534545, pkg);
    }

    private static String packageOf(android.app.Activity host) {
        return host == null || host.getIntent() == null ? null : host.getIntent().getStringExtra("package_name");
    }

    private static android.widget.LinearLayout actionRow(android.app.Activity host, String pkg) {
        boolean tr = "tr".equals(Locale.getDefault().getLanguage());
        float density = host.getResources().getDisplayMetrics().density;
        boolean night = (host.getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int ink = night ? 0xFFE6E6E6 : 0xFF1A1A1A;
        int tile = night ? 0xFF333333 : 0xFFF0F0F0;
        android.widget.LinearLayout row = new android.widget.LinearLayout(host);
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setPadding(0, Math.round(4 * density), 0, Math.round(16 * density));
        String[][] items = {
                {"open", tr ? "Aç" : "Open"},
                {"path", tr ? "Yol" : "Path"},
                {"default", tr ? "Varsayılan aç" : "Open by default"},
        };
        for (int i = 0; i < items.length; i++) {
            android.widget.LinearLayout button = new android.widget.LinearLayout(host);
            button.setOrientation(android.widget.LinearLayout.VERTICAL);
            button.setGravity(android.view.Gravity.CENTER);
            int pad = Math.round(12 * density);
            button.setPadding(Math.round(4 * density), pad, Math.round(4 * density), pad);
            android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
            background.setCornerRadius(16 * density);
            background.setColor(tile);
            android.content.res.ColorStateList ripple = android.content.res.ColorStateList.valueOf(night ? 0x33FFFFFF : 0x22000000);
            button.setBackground(new android.graphics.drawable.RippleDrawable(ripple, background, null));
            android.widget.ImageView icon = new android.widget.ImageView(host);
            icon.setImageDrawable(new ActionIcon(items[i][0], ink, density));
            int size = Math.round(28 * density);
            button.addView(icon, new android.widget.LinearLayout.LayoutParams(size, size));
            android.widget.TextView label = new android.widget.TextView(host);
            label.setText(items[i][1]);
            label.setTextColor(ink);
            label.setGravity(android.view.Gravity.CENTER);
            label.setMaxLines(1);
            label.setAutoSizeTextTypeUniformWithConfiguration(9, 13, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
            android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT, Math.round(20 * density));
            labelParams.topMargin = Math.round(6 * density);
            button.addView(label, labelParams);
            final String action = items[i][0];
            button.setOnClickListener(view -> appAction(host, pkg, action, tr));
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(i == 0 ? 0 : Math.round(6 * density), 0, i == items.length - 1 ? 0 : Math.round(6 * density), 0);
            row.addView(button, params);
        }
        return row;
    }

    /** Line icons like the modded app's: enter arrow, folder with arrow, chain link. */
    private static final class ActionIcon extends android.graphics.drawable.Drawable {
        private final String kind;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        ActionIcon(String kind, int color, float density) {
            this.kind = kind;
            paint.setStyle(android.graphics.Paint.Style.STROKE);
            paint.setStrokeWidth(2.2f * density);
            paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
            paint.setStrokeJoin(android.graphics.Paint.Join.ROUND);
            paint.setColor(color);
        }

        @Override public void draw(android.graphics.Canvas canvas) {
            android.graphics.Rect b = getBounds();
            float s = Math.min(b.width(), b.height()) / 24f;
            canvas.save();
            canvas.translate(b.left + (b.width() - 24 * s) / 2f, b.top + (b.height() - 24 * s) / 2f);
            canvas.scale(s, s);
            paint.setStrokeWidth(paint.getStrokeWidth() / s);
            android.graphics.Path p = new android.graphics.Path();
            switch (kind) {
                case "open":
                    // bracket on the right, arrow entering from the left
                    p.moveTo(13, 4); p.lineTo(18, 4); p.quadTo(20, 4, 20, 6); p.lineTo(20, 18); p.quadTo(20, 20, 18, 20); p.lineTo(13, 20);
                    p.moveTo(4, 12); p.lineTo(15, 12);
                    p.moveTo(11, 8); p.lineTo(15, 12); p.lineTo(11, 16);
                    break;
                case "path":
                    // folder with an arrow inside
                    p.moveTo(3, 7); p.quadTo(3, 5, 5, 5); p.lineTo(9, 5); p.lineTo(11, 7); p.lineTo(19, 7); p.quadTo(21, 7, 21, 9);
                    p.lineTo(21, 17); p.quadTo(21, 19, 19, 19); p.lineTo(5, 19); p.quadTo(3, 19, 3, 17); p.close();
                    p.moveTo(8, 13); p.lineTo(15, 13);
                    p.moveTo(12.5f, 10.5f); p.lineTo(15, 13); p.lineTo(12.5f, 15.5f);
                    break;
                default:
                    // link, as in the modded app: two open half-links facing each other and a bar
                    p.moveTo(10, 7); p.lineTo(7, 7);
                    p.arcTo(new android.graphics.RectF(2, 7, 12, 17), 270, -180, false);
                    p.lineTo(10, 17);
                    p.moveTo(14, 7); p.lineTo(17, 7);
                    p.arcTo(new android.graphics.RectF(12, 7, 22, 17), 270, 180, false);
                    p.lineTo(14, 17);
                    p.moveTo(8, 12); p.lineTo(16, 12);
                    break;
            }
            canvas.drawPath(p, paint);
            paint.setStrokeWidth(paint.getStrokeWidth() * s);
            canvas.restore();
        }

        @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) { paint.setColorFilter(filter); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    private static void appAction(android.app.Activity host, String pkg, String action, boolean tr) {
        try {
            android.content.pm.PackageManager pm = host.getPackageManager();
            switch (action) {
                case "open": {
                    launchApp(host, pkg, tr);
                    break;
                }
                case "path": {
                    String path = pm.getApplicationInfo(pkg, 0).sourceDir;
                    dialog(host, tr ? "Uygulama yolu" : "App path", path, tr ? "Kopyala" : "Copy", () -> {
                        android.content.ClipboardManager clip = (android.content.ClipboardManager) host.getSystemService(Context.CLIPBOARD_SERVICE);
                        clip.setPrimaryClip(android.content.ClipData.newPlainText("path", path));
                        android.widget.Toast.makeText(host, tr ? "Kopyalandı" : "Copied", android.widget.Toast.LENGTH_SHORT).show();
                    });
                    break;
                }
                case "default": {
                    host.startActivity(new android.content.Intent("android.settings.APP_OPEN_BY_DEFAULT_SETTINGS",
                            android.net.Uri.parse("package:" + pkg)));
                    break;
                }
                default:
                    break;
            }
        } catch (Throwable failure) {
            android.widget.Toast.makeText(host, String.valueOf(failure.getMessage()), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    static void launchApp(android.app.Activity host, String pkg, boolean tr) {
        android.content.Intent launch = host.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            android.widget.Toast.makeText(host, tr ? "Açılabilir ekranı yok" : "Nothing to open", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        launch.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        host.startActivity(launch);
    }

    private static final int DOCK_LIMIT = 50;
    private static volatile java.lang.ref.WeakReference<java.util.List<?>> dockSelected = new java.lang.ref.WeakReference<>(null);

    /**
     * Sidebar (dock) editor: the modded app raised the 10-app limit to 50. The limit is a constant
     * inside the editor's methods, so the hooks make each check see room while fewer than 50 apps
     * are chosen, and the "chosen" grid grows past its 10 fixed slots.
     */
    private void dockLimit() throws Throwable {
        Class<?> activity = Class.forName("com.miui.dock.edit.DockAppEditActivity", false, loader);
        Class<?> all = Class.forName("com.miui.dock.edit.a", false, loader);
        Class<?> chosen = Class.forName("com.miui.dock.edit.b", false, loader);
        // The chosen list is the one the "chosen" adapter is built on.
        hooks.install(chosen.getConstructor(java.util.List.class), chain -> {
            Object result = chain.proceed();
            dockSelected = new java.lang.ref.WeakReference<>((java.util.List<?>) chain.getArg(0));
            return result;
        });
        Method listOf = null;
        for (Method method : chosen.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType() == java.util.List.class) listOf = method;
        }
        final Method chosenList = listOf;
        // 10 fixed slots in the chosen grid: grow with the list.
        hooks.install(chosen.getMethod("getItemCount"), chain -> {
            Object result = chain.proceed();
            if (chosenList != null && result instanceof Integer) {
                java.util.List<?> list = (java.util.List<?>) chosenList.invoke(chain.getThisObject());
                if (list != null) {
                    int wanted = Math.min(DOCK_LIMIT, list.size() + 1);
                    return Math.max((Integer) result, wanted);
                }
            }
            return result;
        });
        // "Can add more" switch of the all-apps grid.
        int switches = 0;
        for (Method method : all.getDeclaredMethods()) {
            if (method.getReturnType() == void.class && method.getParameterCount() == 1
                    && method.getParameterTypes()[0] == boolean.class && Modifier.isPublic(method.getModifiers())) {
                hooks.install(method, chain -> {
                    java.util.List<?> list = dockSelected.get();
                    if (list == null) return chain.proceed();
                    return chain.proceed(new Object[]{list.size() < DOCK_LIMIT});
                });
                switches++;
            }
        }
        // Adding an app returns early at 10: hide the apps past the 9th while it runs.
        Field chosenAdapter = null;
        for (Field field : activity.getDeclaredFields()) {
            if (field.getType() == chosen) { field.setAccessible(true); chosenAdapter = field; }
        }
        final Field adapterField = chosenAdapter;
        Method notifyAll = chosen.getMethod("notifyDataSetChanged");
        int adders = 0;
        for (Method method : activity.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 3 && types[0] == all && types[1] == int.class && method.getReturnType() == void.class) {
                hooks.install(method, chain -> {
                    java.util.List<Object> list = (java.util.List<Object>) dockSelected.get();
                    if (list == null || list.size() < 10 || list.size() >= DOCK_LIMIT) return chain.proceed();
                    java.util.List<Object> rest = new java.util.ArrayList<>(list.subList(9, list.size()));
                    list.subList(9, list.size()).clear();
                    Object result;
                    try {
                        result = chain.proceed();
                    } finally {
                        list.addAll(9, rest);
                    }
                    if (adapterField != null) {
                        Object adapter = adapterField.get(chain.getThisObject());
                        if (adapter != null) notifyAll.invoke(adapter);
                    }
                    return result;
                });
                adders++;
            }
        }
        // "Choose up to 10 apps" text.
        hooks.install(activity.getDeclaredMethod("onCreate", android.os.Bundle.class), chain -> {
            Object result = chain.proceed();
            try {
                android.app.Activity self = (android.app.Activity) chain.getThisObject();
                android.content.res.Resources res = self.getResources();
                int text = res.getIdentifier("tv_desc", "id", self.getPackageName());
                int plural = res.getIdentifier("gd_app_edit_desc_new", "plurals", self.getPackageName());
                android.view.View view = text == 0 ? null : self.findViewById(text);
                if (view instanceof android.widget.TextView && plural != 0) {
                    ((android.widget.TextView) view).setText(res.getQuantityString(plural, DOCK_LIMIT, DOCK_LIMIT));
                }
            } catch (Throwable failure) {
                hooks.error("dock text", failure);
            }
            return result;
        });
        if (switches == 0 || adders == 0) throw new NoSuchMethodException("Dock editor checks not found");
    }

    /**
     * New sidebar (edited in place, HyperOS 2+): pinning stops at 16 apps; the modded app made it 50.
     * Every limit check compares the pinned list's size with 16, so that size is mapped to one that
     * gives the same answers as comparing the real count with 50.
     */
    private void dockLimitNew(DexTargets targets) throws Throwable {
        Class<?> model = Class.forName("com.miui.dock.allapps.g0", false, loader);
        // The pinnable/unpinnable markers are set by g0's (List) -> List helpers, which each ask a
        // private no-argument method for the pinned apps; the helpers are reached through static bridges.
        java.util.Set<Method> counts = new java.util.HashSet<>();
        int bridges = 0;
        for (Method method : model.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (!Modifier.isStatic(method.getModifiers()) || types.length != 2 || types[0] != model
                    || types[1] != java.util.List.class || method.getReturnType() != java.util.List.class) continue;
            bridges++;
            for (Method helper : targets.invokedBy("dock_helper_" + method.getName(), method)) {
                if (helper.getDeclaringClass() != model || helper.getParameterCount() != 1) continue;
                for (Method called : targets.invokedBy("dock_count_" + helper.getName(), helper)) {
                    if (called.getDeclaringClass() == model && called.getParameterCount() == 0
                            && called.getReturnType() == java.util.List.class) counts.add(called);
                }
            }
        }
        if (counts.size() != 1) throw new NoSuchMethodException("Pinned-apps count: " + counts + " (bridges " + bridges + ")");
        Method pinnedCount = counts.iterator().next();
        // The sidebar strip trims the pinned list to 16 (newbox.r0 helpers get 16 as their last argument).
        Class<?> strip = Class.forName("com.miui.gamebooster.windowmanager.newbox.r0", false, loader);
        int trims = 0;
        for (Method method : strip.getDeclaredMethods()) {
            Class<?>[] types = method.getParameterTypes();
            if (!Modifier.isStatic(method.getModifiers()) || types.length < 2 || types[0] != java.util.List.class
                    || types[types.length - 1] != int.class) continue;
            hooks.install(method, chain -> {
                Object[] args = chain.getArgs().toArray();
                if (Integer.valueOf(16).equals(args[args.length - 1])) {
                    args[args.length - 1] = DOCK_LIMIT;
                    return chain.proceed(args);
                }
                return chain.proceed();
            });
            trims++;
        }
        if (trims == 0) throw new NoSuchMethodException("Sidebar strip trim not found");
        hooks.install(pinnedCount, chain -> {
            Object result = chain.proceed();
            if (!(result instanceof java.util.List)) return result;
            java.util.List<?> pinned = (java.util.List<?>) result;
            int real = pinned.size();
            // Every check compares this size with 16; answer as if comparing the real count with 50.
            int shown = real >= DOCK_LIMIT ? real - (DOCK_LIMIT - 16) : real == DOCK_LIMIT - 1 ? 15 : Math.min(real, 14);
            return new java.util.ArrayList<>(pinned.subList(0, Math.max(0, Math.min(shown, real))));
        });
    }

    /** Earthquake alert: the modded app closes it once it leaves the screen (as the other alerts do). */
    private void earthquakeAlert() throws Throwable {
        Class<?> helper = Class.forName("com.miui.warningcenter.AlertWindowHelper", false, loader);
        Field host = null;
        for (Field field : helper.getDeclaredFields()) {
            if (android.app.Activity.class.isAssignableFrom(field.getType())) { field.setAccessible(true); host = field; }
        }
        if (host == null) throw new NoSuchFieldException("AlertWindowHelper host");
        final Field hostField = host;
        hooks.install(helper.getMethod("delegate", boolean.class), chain -> {
            Object activity = hostField.get(chain.getThisObject());
            if (activity != null && activity.getClass().getName().endsWith("EarthquakeWarningAlertActivity")) {
                return chain.proceed(new Object[]{true});
            }
            return chain.proceed();
        });
    }

    /** One extra Toolbox entry: string and icon resource names, intent, package it needs (or null). */
    private record Tool(String title, String icon, String intent, String needsPackage) { }

    private static final String ACT = "#Intent;action=";
    private static final Tool[] TOOLS_MORE = {
            new Tool("activity_title_networkassistants", "ic_net_safe_n", ACT + "miui.intent.action.NETWORKASSISTANT_ENTRANCE;end", null),
            new Tool("app_manager_net_control_title", "ic_net_control_n", ACT + "miui.intent.action.NETWORKASSISTANT_FIREWALL;end", null),
            new Tool("quick_reply_title", "warning_wechat", ACT + "miui.intent.action.QUICK_REPLY_SETTINGS;end", null),
            new Tool("sim_lock_setting_title", "sp_icon_sim_protect", ACT + "miui.intent.action.SIM_LOCK_CHOOSE;end", null),
            new Tool("sp_power_off_password_title", "sp_icon_power_off_password", ACT + "miui.intent.action.SHUT_DOWN_PASSWORD_ACTIVITY;end", null),
            new Tool("videobox_settings_title", "ic_video_box_n", ACT + "com.miui.gamebooster.action.VIDEOBOX_SETTINGS_ALL;end", null),
            new Tool("privacy_risk_detection_title", "ic_privacy_protect_n",
                    "#Intent;component=com.miui.securitycenter/com.miui.permcenter.detection.PrivacyRiskDetectionActivity;end", null),
            new Tool("privacy_thumbnail_blur_title", "icon_clean_lockscreen",
                    "#Intent;component=com.miui.securitycenter/com.miui.permcenter.privacyblur.PrivacyThumbnailBlurSettings;end", null),
            new Tool("beauty_fc_assistant", "ic_special_perm_camera",
                    "#Intent;component=com.miui.securitycenter/com.miui.gamebooster.beauty.BeautySettingsActivity;end", null),
            new Tool("app_predict_setting_title", "app_predict_dialog_icon", ACT + "miui.intent.action.APP_PREDICT_GUIDE_DIALOG_ALWAYS;end", null),
    };
    private static final Tool[] CLEAN_MORE = {
            new Tool("card_main_facebookclean_title", "ic_facebook_clean_n", ACT + "miui.intent.action.GARBAGE_DEEPCLEAN_FACEBOOK;end", "com.facebook.katana"),
            new Tool("card_main_whatsappclean_title", "ic_whatsapp_clean_n", ACT + "miui.intent.action.GARBAGE_DEEPCLEAN_WHATSAPP;end", "com.whatsapp"),
            new Tool("card_main_apkclean_title", "ic_apk_clean_n", ACT + "miui.intent.action.GARBAGE_APK_MANAGE;end", null),
            new Tool("card_main_appdataclean_title", "ic_appdata_clean_n", ACT + "miui.intent.action.GARBAGE_APP_DATA_MANAGE;end", null),
            new Tool("card_main_video_manage_title", "ic_video_clean_n", ACT + "miui.intent.action.GARBAGE_VIDEO_MANAGE;end", null),
            new Tool("card_main_galleryclean_title", "ic_gallery_clean_n", ACT + "miui.intent.action.GARBAGE_IMAGE_MANAGE;end", null),
    };
    private static final Tool[] APPS_MORE = {
            new Tool("title_of_auto_launch_manage", "ic_auto_start_n", ACT + "miui.intent.action.OP_AUTO_START;end", null),
            new Tool("activity_title_permissions_manager", "ic_launcher_license_manage",
                    "#Intent;component=com.miui.securitycenter/com.miui.permcenter.permissions.AppPermissionsTabActivity;end", null),
    };
    private static final Tool[] BATTERY_MORE = {
            new Tool("power_center_auto_shutdown", "ic_shutdown_ontime_n", ACT + "miui.powercenter.intent.action.BOOT_SHUTDOWN_ONTIME;end", null),
            new Tool("auto_task_main_title", "ic_auto_task_n",
                    "#Intent;component=com.miui.securitycenter/com.miui.powercenter.autotask.AutoTaskManageActivity;end", null),
    };

    /**
     * The Toolbox lists ("Tools", "Cleaner", "Apps", "Battery") are built on the phone; the modded app
     * rewrote them to the global edition's full set. Nothing is fetched, so it also works where the
     * Security cloud host is blocked.
     */
    private void toolboxLayout(DexTargets targets) throws Throwable {
        Class<?> data = Class.forName("com.miui.common.card.GridFunctionData", false, loader);
        Constructor<?> create = data.getConstructor(String.class, String.class, int.class, String.class);
        Method action = data.getMethod("getAction");
        Method statKey = data.getMethod("setStatKey", String.class);
        int hooked = 0;
        hooked += extendList(targets, "toolbox_tools", create, action, statKey, TOOLS_MORE, 1, "HB_MAIN_ACTIVITY",
                "miui.intent.action.HB_MAIN_ACTIVITY", "com.miui.securitycenter.action.FIRST_AID_KIT", "miui.intent.action.WARNINGCENTER_MAIN");
        hooked += extendList(targets, "toolbox_clean", create, action, statKey, CLEAN_MORE, -1, null,
                "miui.intent.action.GARBAGE_DEEPCLEAN_QQ", "miui.intent.action.GARBAGE_DEEPCLEAN_WECHAT", "miui.intent.action.OPTIMIZE_MANAGE");
        hooked += extendList(targets, "toolbox_apps", create, action, statKey, APPS_MORE, 1, null,
                "miui.intent.action.MANAGE_PRIVACY_APPS", "miui.intent.action.XSPACE_SETTING", "com.miui.securitycenter.action.TRANSITION");
        hooked += extendList(targets, "toolbox_battery", create, action, statKey, BATTERY_MORE, 1, null,
                "com.miui.powercenter.POWER_SAVE", "com.miui.securitycenter.action.POWER_SETTINGS");
        if (hooked == 0) throw new NoSuchMethodException("Toolbox lists not found");
    }

    /** Inserts {@code more} at {@code at} (-1 = before the last entry); {@code drop} removes a stock entry by intent text. */
    private int extendList(DexTargets targets, String name, Constructor<?> create, Method action, Method statKey,
                           Tool[] more, int at, String drop, String... intents) throws Throwable {
        String[] strings = new String[intents.length];
        for (int i = 0; i < intents.length; i++) strings[i] = ACT + intents[i] + ";end";
        int count = 0;
        for (Method method : targets.usingStrings(name, "java.util.ArrayList", strings)) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                Context app = currentApplication();
                if (!(result instanceof java.util.ArrayList) || app == null) return result;
                @SuppressWarnings("unchecked") java.util.ArrayList<Object> list = (java.util.ArrayList<Object>) result;
                if (drop != null) {
                    list.removeIf(item -> {
                        try {
                            String intent = (String) action.invoke(item);
                            return intent != null && intent.contains(drop);
                        } catch (ReflectiveOperationException unavailable) {
                            return false;
                        }
                    });
                }
                java.util.ArrayList<Object> added = new java.util.ArrayList<>();
                for (Tool tool : more) {
                    if (tool.needsPackage() != null && !installed(app, tool.needsPackage())) continue;
                    int title = app.getResources().getIdentifier(tool.title(), "string", app.getPackageName());
                    int icon = app.getResources().getIdentifier(tool.icon(), "drawable", app.getPackageName());
                    if (title == 0 || icon == 0) continue;
                    boolean present = false;
                    for (Object item : list) if (tool.intent().equals(action.invoke(item))) { present = true; break; }
                    if (present) continue;
                    Object entry = create.newInstance(app.getString(title), null, icon, tool.intent());
                    statKey.invoke(entry, "xsecurity_" + tool.icon());
                    added.add(entry);
                }
                int index = at < 0 ? Math.max(0, list.size() - 1) : Math.min(at, list.size());
                list.addAll(index, added);
                return list;
            });
            count++;
        }
        return count;
    }

    private static boolean installed(Context context, String pkg) {
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (android.content.pm.PackageManager.NameNotFoundException absent) {
            return false;
        }
    }

    private static Context currentApplication() {
        try {
            return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        } catch (Throwable unavailable) {
            return null;
        }
    }

    /** Car accident detection and emergency contacts, also on phones the ROM leaves out. */
    private void carAccident() throws Throwable {
        Class<?> utils = Class.forName("com.miui.warningcenter.caraccident.CarAccidentUtils", false, loader);
        hooks.constant(utils.getDeclaredMethod("isSupportCarAccidentDetect", Context.class), true);
        hooks.constant(utils.getDeclaredMethod("isSupportEmergencyContact", Context.class), true);
    }

    /** One feature failing (a renamed class in a new build) never stops the others. */
    private void run(String name, Step step) {
        try {
            step.run();
            hooks.info(name + " ready");
        } catch (Throwable failure) {
            hooks.error(name + " unavailable; stock behaviour kept", failure);
        }
    }

    // --- Battery ------------------------------------------------------------------------------

    /**
     * Since Android 13 the Security app hides the screen from the battery ranking and merges
     * system items; one flag (SDK >= 33) drives this. The mod set it to false; so does this.
     */
    private void batteryRanking() throws Throwable {
        Class<?> helper = Class.forName(POWER + "legacypowerrank.f", true, loader);
        Field flag = null;
        for (Field field : helper.getDeclaredFields()) {
            int m = field.getModifiers();
            if (!Modifier.isStatic(m) || !Modifier.isFinal(m)) continue;
            field.setAccessible(true);
            if (field.getType() == boolean.class) {
                if (flag != null) throw new IllegalStateException("Two ranking flags");
                flag = field;
            } else if (field.getType() == HashSet.class) {
                // The drain types hidden on Android 13+ (2 = phone calls).
                ((HashSet<?>) field.get(null)).remove(2);
            }
        }
        if (flag == null) throw new NoSuchFieldException("Ranking flag missing in " + helper.getName());
        flag.setBoolean(null, false);
        // Compiled code may have kept the old value; run the helper interpreted.
        for (Method method : helper.getDeclaredMethods()) {
            if (!Modifier.isAbstract(method.getModifiers())) module.deoptimize(method);
        }
        // On some ROMs the legacy rank reports each app's power in a far-too-large unit, so the list
        // shows nonsense ("100015947 mAh") while the percentages (a ratio) stay correct. No phone
        // battery delivers tens of thousands of mAh to one app, so an impossible value is scaled down
        // by thousands until it is plausible; every sipper scales by the same factor, so the ratios
        // and percentages are unchanged. Devices that already report sane values never trigger this.
        try {
            Class<?> sipper = Class.forName("miui.securitycenter.powercenter.BatterySipper", false, loader);
            Method getValue = sipper.getDeclaredMethod("getValue");
            module.deoptimize(getValue);
            hooks.install(getValue, chain -> {
                double value = (double) chain.proceed();
                while (Math.abs(value) > 50000d) value /= 1000d;
                return value;
            });
        } catch (Throwable unavailable) {
            hooks.info("battery ranking: power value guard unavailable; stock values kept");
        }
    }

    /** The screen entry shows 0 min of use; fill in the real screen-on time since the last charge. */
    private void screenTime() throws Throwable {
        Class<?> data = Class.forName(POWER + "legacypowerrank.BatteryData", false, loader);
        Class<?> sipper = Class.forName("miui.securitycenter.powercenter.BatterySipper", false, loader);
        Field drainType = data.getField("drainType");
        Field usageTime = data.getField("usageTime");
        hooks.install(data.getDeclaredMethod("add", sipper), chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            if (drainType.getInt(self) == 5 && usageTime.getLong(self) == 0) {
                long millis = screenOnMillis();
                if (millis > 0) usageTime.setLong(self, millis);
            }
            return result;
        });
    }

    private static long screenOnMillis() {
        try {
            IBinder binder = (IBinder) Class.forName("android.os.ServiceManager")
                    .getMethod("getService", String.class).invoke(null, "batterystats");
            Object stats = Class.forName("com.android.internal.app.IBatteryStats$Stub")
                    .getMethod("asInterface", IBinder.class).invoke(null, binder);
            // Xiaomi's battery stats service; 0 = since the last full charge. Microseconds.
            long micros = (long) stats.getClass().getMethod("getScreenOnTime", long.class, int.class)
                    .invoke(stats, SystemClock.elapsedRealtime() * 1000, 0);
            return micros / 1000;
        } catch (Throwable unavailable) {
            return 0;
        }
    }

    /** Battery health rows under "Battery protection": capacity now, design capacity, cycles. */
    private void batteryHealth() throws Throwable {
        Class<?> fragment = Class.forName(POWER + "nightcharge.ChargeProtectFragment", false, loader);
        Class<?> preference = Class.forName("androidx.preference.Preference", false, loader);
        Class<?> group = Class.forName("androidx.preference.PreferenceGroup", false, loader);
        Class<?> category = Class.forName("miuix.preference.PreferenceCategory", false, loader);
        Class<?> text = Class.forName("miuix.preference.TextPreference", false, loader);
        Constructor<?> newCategory = category.getConstructor(Context.class, android.util.AttributeSet.class);
        Constructor<?> newText = text.getConstructor(Context.class);
        Method setTitle = preference.getMethod("setTitle", CharSequence.class);
        Method setKey = preference.getMethod("setKey", String.class);
        Method setSelectable = preference.getMethod("setSelectable", boolean.class);
        Method setPersistent = preference.getMethod("setPersistent", boolean.class);
        Method setText = text.getMethod("setText", String.class);
        Method add = group.getMethod("addPreference", preference);
        Method screen = fragment.getMethod("getPreferenceScreen");
        Method context = fragment.getMethod("getContext");
        Method findPreference = fragment.getMethod("findPreference", CharSequence.class);
        // Temperature row: "35°C Normal" as in the modded app (stock shows only "Normal").
        Method getKey = preference.getMethod("getKey");
        hooks.install(setText, chain -> {
            Object value = chain.getArg(0);
            if (value instanceof String && !((String) value).contains("°")
                    && "reference_current_temp".equals(getKey.invoke(chain.getThisObject()))) {
                long tenths = readLong("temp");
                if (tenths > 0 && tenths < 1000) {
                    return chain.proceed(new Object[]{Math.round(tenths / 10f) + "°C " + value});
                }
            }
            return chain.proceed();
        });
        hooks.install(fragment.getMethod("onCreatePreferences", android.os.Bundle.class, String.class), chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            Context ctx = (Context) context.invoke(self);
            Object root = screen.invoke(self);
            if (ctx == null || root == null) return result;
            long full = readLong("charge_full");
            long design = readLong("charge_full_design");
            long cycles = readLong("cycle_count");
            if (full <= 0 && design <= 0 && cycles < 0) return result;
            // Same place and wording as the modded app: inside Xiaomi's own "Battery info" card, after
            // its rows (health %, temperature, charges today): Cycles, Actual / design capacity.
            Object section = findPreference.invoke(self, "preference_key_category_battery_info");
            if (section == null) {
                section = newCategory.newInstance(ctx, null);
                setKey.invoke(section, "xsecurity_battery_health");
                add.invoke(root, section);
            }
            if (cycles >= 0 && findPreference.invoke(self, "reference_cycle_count") == null) {
                int cyclesTitle = ctx.getResources().getIdentifier("pc_cycle_count_protect_title", "string", ctx.getPackageName());
                row(ctx, newText, setTitle, setKey, setSelectable, setPersistent, setText, add, section,
                        cyclesTitle != 0 ? ctx.getString(cyclesTitle) : "Cycles", String.valueOf(cycles));
            }
            if (full > 0) row(ctx, newText, setTitle, setKey, setSelectable, setPersistent, setText, add, section,
                    "Actual battery capacity", full / 1000 + "mAh");
            if (design > 0) row(ctx, newText, setTitle, setKey, setSelectable, setPersistent, setText, add, section,
                    "Battery design capacity", design / 1000 + "mAh");
            return result;
        });
    }

    /**
     * "Battery health %91": the stock page writes it from a late message and sometimes never does.
     * Once the page is up, fill it from Xiaomi's own health value if it is still empty. The value is
     * asked from the app's own power provider (every build since 13.5 answers "getBatteryHealthValue"),
     * so no renamed method has to be found; this is the value HyperCeiler reads too.
     */
    private void batteryHealthPercent() throws Throwable {
        Class<?> fragment = Class.forName(POWER + "nightcharge.ChargeProtectFragment", false, loader);
        Class<?> text = Class.forName("miuix.preference.TextPreference", false, loader);
        Method setText = text.getMethod("setText", String.class);
        Method getText = text.getMethod("getText");
        Method findPreference = fragment.getMethod("findPreference", CharSequence.class);
        Method context = fragment.getMethod("getContext");
        Method added = fragment.getMethod("isAdded");
        hooks.install(fragment.getMethod("onResume"), chain -> {
            Object result = chain.proceed();
            Object self = chain.getThisObject();
            Context ctx = (Context) context.invoke(self);
            if (ctx == null) return result;
            Context app = ctx.getApplicationContext();
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            new Thread(() -> {
                int value = batteryHealthValue(app);
                if (value <= 0 || value > 100) return;
                Runnable fill = () -> {
                    try {
                        if (!(Boolean) added.invoke(self)) return;
                        Object row = findPreference.invoke(self, "reference_battery_health");
                        if (row == null) return;
                        Object shown = getText.invoke(row);
                        if (shown != null && shown.toString().contains("%")) return;
                        setText.invoke(row, java.text.NumberFormat.getPercentInstance().format(value / 100f));
                    } catch (Throwable failure) {
                        hooks.error("battery health percent", failure);
                    }
                };
                main.post(fill);
                main.postDelayed(fill, 1500);
            }, "XSecurity-health").start();
            return result;
        });
    }

    /** Xiaomi's battery health % (the "UI SOH"), or -1. */
    private static int batteryHealthValue(Context context) {
        try {
            android.os.Bundle answer = context.getContentResolver().call(
                    android.net.Uri.parse("content://com.miui.powercenter.provider"), "getBatteryHealthValue", null, null);
            return answer == null ? -1 : answer.getInt("key_get_battery_health_value", -1);
        } catch (Throwable unavailable) {
            return -1;
        }
    }

    private static void row(Context ctx, Constructor<?> newText, Method setTitle, Method setKey, Method setSelectable,
                            Method setPersistent, Method setText, Method add, Object section, String title, String value)
            throws ReflectiveOperationException {
        Object row = newText.newInstance(ctx);
        setKey.invoke(row, "xsecurity_" + title.hashCode());
        setTitle.invoke(row, title);
        setText.invoke(row, value);
        setSelectable.invoke(row, false);
        setPersistent.invoke(row, false);
        add.invoke(section, row);
    }

    /** Reads a battery value (µAh or count) from sysfs; -1 when the kernel does not offer it. */
    private static long readLong(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader("/sys/class/power_supply/battery/" + name))) {
            String line = reader.readLine();
            return line == null ? -1 : Long.parseLong(line.trim());
        } catch (Throwable unavailable) {
            return -1;
        }
    }

    /** Night charge and smart charge protection, also where the ROM does not list them. */
    private void chargeProtect(DexTargets targets) throws Throwable {
        List<Method> anchor = targets.usingStrings("charge_anchor", "boolean", "isSupportNightChargeProtection isSupport:");
        List<Method> property = targets.usingStrings("charge_prop", "boolean", "persist.vendor.night.charge");
        List<Method> ui = targets.usingStrings("charge_ui", "boolean", "supportNightProtectForUi: ");
        if (anchor.isEmpty()) throw new NoSuchMethodException("Night charge check not found");
        Class<?> owner = anchor.get(0).getDeclaringClass();
        for (Method method : anchor) hooks.constant(method, true);
        // Only the property check next to it; the charging manager reads the same property for other work.
        for (Method method : property) if (method.getDeclaringClass() == owner) hooks.constant(method, true);
        for (Method method : ui) hooks.constant(method, true);
    }

    // --- Game Turbo ---------------------------------------------------------------------------

    /**
     * App info: the "Clear defaults" row becomes "Open by default" (as in HyperCeiler / MIUIQOL): it
     * shows whether the app opens its supported links and leads to the system page for it.
     */
    private void openByDefault(DexTargets targets) throws Throwable {
        Class<?> fragment = Class.forName("com.miui.appmanager.fragment.ApplicationsDetailsFragment", false, loader);
        Class<?> preference = Class.forName("androidx.preference.Preference", false, loader);
        Method findPreference = fragment.getMethod("findPreference", CharSequence.class);
        Method setTitle = preference.getMethod("setTitle", CharSequence.class);
        Method setSummary = preference.getMethod("setSummary", CharSequence.class);
        Method getKey = preference.getMethod("getKey");
        Method activityOf = fragment.getMethod("getActivity");
        int loaded = 0;
        for (Method method : targets.usingStrings("app_default_loaded", "void", "enter_way")) {
            if (method.getDeclaringClass() != fragment) continue;
            loaded++;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                try {
                    android.app.Activity host = (android.app.Activity) activityOf.invoke(chain.getThisObject());
                    String pkg = packageOf(host);
                    Object row = findPreference.invoke(chain.getThisObject(), "app_default_pref");
                    if (host == null || pkg == null || row == null) return result;
                    boolean tr = "tr".equals(Locale.getDefault().getLanguage());
                    setTitle.invoke(row, tr ? "Varsayılan olarak aç" : "Open by default");
                    setSummary.invoke(row, linkState(host, pkg, tr));
                } catch (Throwable failure) {
                    hooks.error("open by default row", failure);
                }
                return result;
            });
        }
        if (loaded == 0) throw new NoSuchMethodException("ApplicationsDetailsFragment load finished");
        hooks.install(fragment.getMethod("onPreferenceClick", preference), chain -> {
            try {
                if ("app_default_pref".equals(getKey.invoke(chain.getArg(0)))) {
                    android.app.Activity host = (android.app.Activity) activityOf.invoke(chain.getThisObject());
                    String pkg = packageOf(host);
                    if (host != null && pkg != null) {
                        host.startActivity(new android.content.Intent("android.settings.APP_OPEN_BY_DEFAULT_SETTINGS",
                                android.net.Uri.parse("package:" + pkg)));
                        return true;
                    }
                }
            } catch (Throwable failure) {
                hooks.error("open by default click", failure);
            }
            return chain.proceed();
        });
    }

    private static String linkState(Context context, String pkg, boolean tr) {
        boolean allowed = false;
        try {
            android.content.pm.verify.domain.DomainVerificationManager manager =
                    context.getSystemService(android.content.pm.verify.domain.DomainVerificationManager.class);
            android.content.pm.verify.domain.DomainVerificationUserState state = manager == null ? null : manager.getDomainVerificationUserState(pkg);
            allowed = state != null && state.isLinkHandlingAllowed();
        } catch (Throwable ignored) {
        }
        if (tr) return allowed ? "Desteklenen bağlantıları açar" : "Bağlantıları açmaz";
        return allowed ? "Opens supported links" : "Doesn't open links";
    }

    private void alwaysTrue(DexTargets targets, String name, String feature) throws Throwable {
        List<Method> methods = targets.usingStrings(name, "boolean", feature);
        if (methods.isEmpty()) throw new NoSuchMethodException("No check uses " + feature);
        for (Method method : methods) hooks.constant(method, true);
    }

    /** Getters of the app's own on/off settings (no arguments): read as off. */
    private void alwaysFalse(DexTargets targets, String name, String key) throws Throwable {
        int hooked = 0;
        for (Method method : targets.usingStrings(name, "boolean", key)) {
            if (method.getParameterCount() != 0) continue;
            hooks.constant(method, false);
            hooked++;
        }
        if (hooked == 0) throw new NoSuchMethodException("No getter reads " + key);
    }

    /** The "SIM security verification completed" dialog shown after boot closes at once. */
    private void simSuccessDialog() throws Throwable {
        for (String name : new String[]{"com.miui.simlock.activity.SuccessDialogActivity",
                "com.miui.simlock.activity.SuccessDialogNormalActivity"}) {
            Class<?> activity;
            try {
                activity = Class.forName(name, false, loader);
            } catch (ClassNotFoundException missing) {
                continue;
            }
            Method create;
            try {
                create = activity.getDeclaredMethod("onCreate", android.os.Bundle.class);
            } catch (NoSuchMethodException inherited) {
                continue;
            }
            hooks.install(create, chain -> {
                Object result = chain.proceed();
                ((android.app.Activity) chain.getThisObject()).finish();
                return result;
            });
        }
    }

    /** Super resolution / frame insertion (YSRE) and the GPU tuner's support answer. */
    private void superResolution(DexTargets targets) throws Throwable {
        alwaysTrue(targets, "gt_ysre", "persist.sys.smartop.support_ysre");
        for (Method method : targets.named("gt_frame_insert", "isSupportSuperResolutionWithFrameInsert")) {
            if (method.getReturnType() == boolean.class) hooks.constant(method, true);
        }
    }

    // --- Privacy and system -------------------------------------------------------------------

    /** The "allow permission?" warning makes you wait 10 s (5 s for others) before you can allow. */
    private void permissionWait() throws Throwable {
        int count = 0;
        for (String name : new String[]{"InterceptBaseFragment", "InterceptPermissionFragment"}) {
            Class<?> type = Class.forName("com.miui.permcenter.privacymanager." + name, false, loader);
            for (Method method : type.getDeclaredMethods()) {
                int m = method.getModifiers();
                if (Modifier.isProtected(m) && !Modifier.isStatic(m) && !Modifier.isAbstract(m)
                        && method.getReturnType() == int.class && method.getParameterCount() == 0) {
                    hooks.constant(method, 0);
                    count++;
                }
            }
        }
        if (count == 0) throw new NoSuchMethodException("Countdown not found");
    }

    /** Second space and dual apps (XSpace), also on devices the ROM leaves them out. */
    private void secondSpace(DexTargets targets) throws Throwable {
        int count = 0;
        for (Method method : targets.usingStrings("space_security", "boolean", "isSupportSecuritySpace", "reflect error isSupportSecuritySpace")) {
            hooks.constant(method, true);
            count++;
        }
        for (Method method : targets.usingStrings("space_x", "boolean", "isSupportXSpace")) {
            hooks.constant(method, true);
            count++;
        }
        if (count == 0) throw new NoSuchMethodException("Space checks not found");
    }
}
