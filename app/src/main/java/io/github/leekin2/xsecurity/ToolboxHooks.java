package io.github.leekin2.xsecurity;

import android.content.Context;
import android.content.Intent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Locale;

/** Completes verified stock Toolbox lists; only shortcuts implemented on the ROM are added. */
final class ToolboxHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final java.util.Set<String> reported = java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    ToolboxHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }
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
    private static final Tool MOTION = new Tool("car_sickness_title", "ic_motion_sickness",
            ACT + "miui.intent.action.CARSICKNESS_RELIEF_SETTINGS;end", null);

    /**
     * The Toolbox lists ("Tools", "Cleaner", "Apps", "Battery") are built on the phone; the modded app
     * rewrote them to the global edition's full set. Nothing is fetched, so it also works where the
     * Security cloud host is blocked.
     */
    void install(DexTargets targets) throws Throwable {
        boolean full = Features.enabled(Features.TOOLBOX);
        boolean motion = Features.enabled(Features.CAR_SICKNESS);
        if (motion) {
            try {
                loader.loadClass("com.miui.carsickness.service.CarSicknessService");
                loader.loadClass("com.miui.carsick.services.AntiCarsickManager");
            } catch (ClassNotFoundException absent) {
                motion = false;
                hooks.info("toolbox: motion shortcut unavailable; ROM implementation missing");
            }
        }
        if (!full && !motion) return;
        Class<?> data = Class.forName("com.miui.common.card.GridFunctionData", false, loader);
        Constructor<?> create = data.getConstructor(String.class, String.class, int.class, String.class);
        Method action = data.getMethod("getAction");
        Method statKey = data.getMethod("setStatKey", String.class);
        int hooked = 0;
        java.util.ArrayList<Tool> tools = new java.util.ArrayList<>();
        if (full) tools.addAll(java.util.Arrays.asList(TOOLS_MORE));
        if (motion) tools.add(MOTION);
        // Both the global and China factories have these two anchors; the global one has no HB entry.
        hooked += extendList(targets, "toolbox_tools", create, action, statKey, tools.toArray(Tool[]::new), 1,
                full ? "HB_MAIN_ACTIVITY" : null,
                "com.miui.securitycenter.action.FIRST_AID_KIT", "miui.intent.action.WARNINGCENTER_MAIN");
        if (full) {
        hooked += extendList(targets, "toolbox_clean", create, action, statKey, CLEAN_MORE, -1, null,
                "miui.intent.action.GARBAGE_DEEPCLEAN_QQ", "miui.intent.action.GARBAGE_DEEPCLEAN_WECHAT", "miui.intent.action.OPTIMIZE_MANAGE");
        hooked += extendList(targets, "toolbox_apps", create, action, statKey, APPS_MORE, 1, null,
                "miui.intent.action.MANAGE_PRIVACY_APPS", "miui.intent.action.XSPACE_SETTING", "com.miui.securitycenter.action.TRANSITION");
        hooked += extendList(targets, "toolbox_battery", create, action, statKey, BATTERY_MORE, 1, null,
                "com.miui.powercenter.POWER_SAVE", "com.miui.securitycenter.action.POWER_SETTINGS");
        }
        int cached = hookCachedCards(targets, create, action, statKey, tools.toArray(Tool[]::new));
        if (hooked + cached == 0) throw new NoSuchMethodException("Toolbox lists not found");
        hooks.info("toolbox: factory hooks=" + hooked + "; cached-card hooks=" + cached);
    }

    /** Inserts {@code more} at {@code at} (-1 = before the last entry); {@code drop} removes a stock entry by intent text. */
    private int extendList(DexTargets targets, String name, Constructor<?> create, Method action, Method statKey,
                           Tool[] more, int at, String drop, String... intents) throws Throwable {
        String[] strings = new String[intents.length];
        for (int i = 0; i < intents.length; i++) strings[i] = ACT + intents[i] + ";end";
        int count = 0;
        java.util.Set<Method> methods = new java.util.LinkedHashSet<>(targets.usingStrings(name + "_v3_array", "java.util.ArrayList", strings));
        methods.addAll(targets.usingStrings(name + "_v3_list", "java.util.List", strings));
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) continue;
            hooks.install(method, chain -> complete(chain.proceed(), currentApplication(), name,
                    create, action, statKey, more, at, drop));
            hooks.deoptimizeReported(method);
            count++;
        }
        return count;
    }

    private Object complete(Object result, Context app, String name, Constructor<?> create, Method action,
                            Method statKey, Tool[] more, int at, String drop) throws Throwable {
        if (!Features.enabled(Features.TOOLBOX) && !Features.enabled(Features.CAR_SICKNESS)) return result;
        if (!(result instanceof java.util.List<?>) || app == null) return result;
        for (Object item : (List<?>) result) if (!create.getDeclaringClass().isInstance(item)) return result;
        java.util.ArrayList<Object> list = new java.util.ArrayList<>((List<?>) result);
        if (drop != null && Features.enabled(Features.TOOLBOX)) {
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
        int skipped = 0;
        for (Tool tool : more) {
            if (tool == MOTION ? !Features.enabled(Features.CAR_SICKNESS) : !Features.enabled(Features.TOOLBOX)) continue;
            if (tool.needsPackage() != null && !installed(app, tool.needsPackage())) continue;
            Intent destination = Intent.parseUri(tool.intent(), 0);
            if (destination.resolveActivity(app.getPackageManager()) == null) { skipped++; continue; }
            int title = app.getResources().getIdentifier(tool.title(), "string", app.getPackageName());
            int icon = app.getResources().getIdentifier(tool.icon(), "drawable", app.getPackageName());
            if (icon == 0) icon = android.R.drawable.ic_menu_manage;
            boolean present = false;
            for (Object item : list) if (tool.intent().equals(action.invoke(item))) { present = true; break; }
            if (present) continue;
            Object entry = create.newInstance(title == 0 ? fallbackTitle(tool.title()) : app.getString(title), null, icon, tool.intent());
            statKey.invoke(entry, "xsecurity_" + tool.icon());
            added.add(entry);
        }
        int index = at < 0 ? Math.max(0, list.size() - 1) : Math.min(at, list.size());
        list.addAll(index, added);
        if (reported.add(name)) hooks.info(name + ": added=" + added.size() + "; unavailable activities=" + skipped);
        return list.equals(result) ? result : list;
    }

    private record CardChange(Object card, Object oldList, Object newList) { }

    private int hookCachedCards(DexTargets targets, Constructor<?> create, Method action,
                                Method statKey, Tool[] tools) throws ReflectiveOperationException {
        Class<?> model;
        try { model = loader.loadClass("com.miui.common.card.models.TitleCardModel"); }
        catch (ClassNotFoundException older) { return 0; }
        Field entries = model.getField("gridFunctionDataList");
        Method getTitle = model.getMethod("getTitle");
        if (!List.class.isAssignableFrom(entries.getType()) || Modifier.isFinal(entries.getModifiers())) return 0;
        java.util.Set<Method> methods = new java.util.LinkedHashSet<>(
                targets.usingStrings("toolbox_cached_cards_v1_list", "java.util.List", "phonemanage_data_cache"));
        methods.addAll(targets.usingStrings("toolbox_cached_cards_v1_array", "java.util.ArrayList", "phonemanage_data_cache"));
        int count = 0;
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != Context.class) continue;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                if (!(result instanceof List<?> cards) || !(chain.getArg(0) instanceof Context context)) return result;
                boolean full = Features.enabled(Features.TOOLBOX);
                if (!full && !Features.enabled(Features.CAR_SICKNESS)) return result;
                java.util.ArrayList<CardChange> changes = new java.util.ArrayList<>();
                for (Object card : cards) {
                    if (!model.isInstance(card)) continue;
                    int kind = cachedCategory(context, (String) getTitle.invoke(card));
                    if (kind < 0 || (!full && kind != 0)) continue;
                    Object before = entries.get(card);
                    Tool[] extra = switch (kind) {
                        case 0 -> tools;
                        case 1 -> CLEAN_MORE;
                        case 2 -> APPS_MORE;
                        case 3 -> BATTERY_MORE;
                        default -> throw new IllegalStateException("Unknown Toolbox category");
                    };
                    Object after = complete(before == null ? java.util.List.of() : before, context,
                            "toolbox_cached_group_" + kind, create, action, statKey, extra,
                            kind == 1 ? -1 : 1, kind == 0 && full ? "HB_MAIN_ACTIVITY" : null);
                    if (after != before) changes.add(new CardChange(card, before, after));
                }
                try {
                    for (CardChange change : changes) entries.set(change.card(), change.newList());
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    for (CardChange change : changes) {
                        try { entries.set(change.card(), change.oldList()); }
                        catch (ReflectiveOperationException | RuntimeException rollback) { failure.addSuppressed(rollback); }
                    }
                    throw failure;
                }
                if (reported.add("cached_cards")) hooks.info("toolbox cached cards: updated categories=" + changes.size());
                return result;
            });
            hooks.deoptimizeReported(method);
            count++;
        }
        return count;
    }

    private static int cachedCategory(Context context, String title) {
        if (title == null) return -1;
        String[] names = {"phone_manage_system_tool_title", "phone_manage_clean_title", "title_of_app_manage", "phone_manage_electric_title"};
        for (int index = 0; index < names.length; index++) {
            int resource = context.getResources().getIdentifier(names[index], "string", context.getPackageName());
            if (resource != 0 && title.equals(context.getString(resource))) return index;
        }
        // Server labels can differ from the local resource. Leave family/recent/custom groups intact.
        return switch (title.trim().toLowerCase(Locale.ROOT)) {
            case "tools", "araçlar", "工具", "инструменты", "herramientas" -> 0;
            case "cleaner", "temizleyici", "temizleme", "清理", "очистка", "limpiador" -> 1;
            case "apps", "basic features", "uygulamalar", "temel özellikler", "应用", "приложения", "aplicaciones" -> 2;
            case "battery", "pil", "电池", "батарея", "batería" -> 3;
            default -> -1;
        };
    }

    private static String fallbackTitle(String key) {
        boolean tr = "tr".equals(Locale.getDefault().getLanguage());
        return switch (key) {
            case "activity_title_networkassistants" -> tr ? "Veri kullanımı" : "Data usage";
            case "app_manager_net_control_title" -> tr ? "Ağ erişimi" : "Network access";
            case "quick_reply_title" -> tr ? "Hızlı yanıt" : "Quick reply";
            case "sim_lock_setting_title" -> tr ? "SIM koruması" : "SIM protection";
            case "sp_power_off_password_title" -> tr ? "Kapatma parolası" : "Power-off password";
            case "videobox_settings_title" -> tr ? "Video araç kutusu" : "Video toolbox";
            case "privacy_risk_detection_title" -> tr ? "Gizlilik taraması" : "Privacy scan";
            case "privacy_thumbnail_blur_title" -> tr ? "Son uygulamaları bulanıklaştır" : "Blur app previews";
            case "beauty_fc_assistant" -> tr ? "Görüntülü arama efektleri" : "Video call effects";
            case "app_predict_setting_title" -> tr ? "Uygulama ön yükleme" : "App preloading";
            case "card_main_facebookclean_title" -> tr ? "Facebook temizliği" : "Facebook cleanup";
            case "card_main_whatsappclean_title" -> tr ? "WhatsApp temizliği" : "WhatsApp cleanup";
            case "card_main_apkclean_title" -> tr ? "APK dosyaları" : "APK files";
            case "card_main_appdataclean_title" -> tr ? "Uygulama verilerini temizle" : "App data cleanup";
            case "card_main_video_manage_title" -> tr ? "Video temizliği" : "Video cleanup";
            case "card_main_galleryclean_title" -> tr ? "Fotoğraf temizliği" : "Photo cleanup";
            case "title_of_auto_launch_manage" -> tr ? "Otomatik başlatma" : "Autostart";
            case "activity_title_permissions_manager" -> tr ? "İzinler" : "Permissions";
            case "power_center_auto_shutdown" -> tr ? "Planlı açma/kapatma" : "Scheduled power on/off";
            case "auto_task_main_title" -> tr ? "Otomatik görevler" : "Automated tasks";
            case "car_sickness_title" -> tr ? "Araç tutması rahatlatma" : "Motion sickness relief";
            default -> throw new IllegalArgumentException("Unknown Toolbox title");
        };
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

}
