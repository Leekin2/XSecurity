package io.github.leekin2.xsecurity;

import android.content.SharedPreferences;
import java.util.Set;

/** Switch keys shared by the settings screen and the hooks, with their defaults. */
public final class Features {
    public static final String GROUP = "xsecurity";

    // Battery
    public static final String BATTERY_RANKING = "battery_ranking";
    public static final String BATTERY_SCREEN_TIME = "battery_screen_time";
    public static final String BATTERY_HEALTH = "battery_health";
    public static final String CHARGE_PROTECT = "charge_protect";
    // Games
    public static final String GAME_FPS = "game_fps";
    public static final String GAME_KEEP_ENABLED = "game_keep_enabled";
    public static final String GT_HDR = "gt_hdr";
    public static final String GT_SUPER_RES = "gt_super_res";
    public static final String GT_DYNAMIC_PERF = "gt_dynamic_perf";
    public static final String GT_TOUCH = "gt_touch";
    public static final String GT_MI_TIME = "gt_mi_time";
    public static final String GT_EXTRAS = "gt_extras";
    public static final String GAME_MODES = "game_modes";
    public static final String GAME_TOAST_OFF = "game_toast_off";
    public static final String GAME_MACRO = "game_macro_all_apps";
    public static final String GAME_NETWORK = "game_network_tools";
    public static final String JOYOSE_GPU = "joyose_gpu_tuner";
    public static final String JOYOSE_CLOUD_OFF = "joyose_cloud_off";
    // Battery (more)
    public static final String BATTERY_LIFE = "battery_life";
    public static final String POWER_EXTRAS = "power_extras";
    public static final String POWER_SAVE_REFRESH = "power_save_keep_refresh";
    public static final String POWER_SAVE_BRIGHTNESS = "power_save_keep_brightness";
    public static final String POWER_SAVE_BOOT = "power_save_keep_on_boot";
    // Charging
    public static final String SMART_CHARGE = "smart_charge_unlock";
    public static final String SUPER_WIRELESS = "super_wireless_charge";
    // Network
    public static final String NET_QUOTA_KEEP = "net_quota_keep_data";
    public static final String NET_HOTSPOT_KEEP = "net_hotspot_keep";
    public static final String NET_APP_CONTROLS = "network_app_controls";
    public static final String IMS_ICONS = "ims_global_icons";
    public static final String SIM_MENUS = "sim_volte_wfc_menus";
    // Video and calls
    public static final String VIDEO_TOOLBOX = "video_toolbox";
    public static final String CALL_TOOLBOX = "call_toolbox";
    // Privacy and system
    public static final String FLAG_SECURE = "flag_secure_off";
    public static final String PERM_NO_WAIT = "perm_no_wait";
    public static final String SECOND_SPACE = "second_space";
    public static final String WARNINGS = "warnings";
    public static final String FBO = "fbo";
    public static final String TOOLBOX = "toolbox_layout";
    public static final String DATA_USAGE = "data_usage_control_center";
    public static final String APP_INFO = "app_info_actions";
    public static final String APP_INFO_BUTTONS = "app_info_buttons";
    public static final String APP_ICON_LAUNCH = "app_info_icon_launch";
    public static final String MMS_ADS = "mms_disable_ad";
    public static final String MMS_FRAUD = "mms_disable_fraud_risk_tip";
    public static final String MMS_OVERSEAS = "mms_disable_overseas_risk_tip";
    public static final String MMS_OLD_DEVICE = "mms_im_old_device";
    public static final String MMS_COPY_CODE = "mms_verification_code_auto_copy";
    public static final String HOME_PRESTART = "home_other_disable_prestart";
    public static final String HOME_REPORT = "home_title_hide_report";
    public static final String DOCK = "dock_50_apps";
    public static final String CAR_SICKNESS = "car_sickness_relief";
    public static final String APPLOCK_SCRAMBLE = "applock_pin_scramble";
    public static final String LOCKSCREEN_PIN_SCRAMBLE = "lockscreen_pin_scramble";
    public static final String LOW_BATTERY_QUIET = "low_battery_quiet";
    public static final String SIM_SUCCESS_DIALOG_OFF = "sim_success_dialog_off";
    public static final String ROOT_CHECK_OFF = "root_check_off";
    public static final String USB_INSTALL_CHECK_OFF = "usb_install_check_off";
    // Clipboard (system_server) and Security's app-list upload
    public static final String CLIPBOARD_WHITELIST = "clipboard_whitelist";
    public static final String CLIPBOARD_APPS = "clipboard_whitelist_apps";
    public static final String APPLIST_UPLOAD_OFF = "applist_upload_off";
    // Display
    public static final String DARK_MODE_ALL_APPS = "dark_mode_all_apps";
    // App store redirects
    public static final String STORE_FORCE_CHOOSER = "store_force_chooser";
    public static final String STORE_GETAPPS_MINI = "store_getapps_detailmini";
    // Themes
    public static final String THEME_THIRD_PARTY = "theme_third_party";
    public static final String THEME_FIX_RESET = "theme_fix_reset";
    // Optional package-manager behaviour (Core Patch feature names).
    public static final String CP_DOWNGRADE = "corepatch_downgrade";
    public static final String CP_VERIFY = "corepatch_verification";
    public static final String CP_ARSC = "corepatch_resource_arsc";
    public static final String CP_DIGEST = "corepatch_digest";
    public static final String CP_EXACT = "corepatch_exact_signature";
    public static final String CP_PREVIOUS = "corepatch_previous_signatures";
    public static final String CP_HIDDEN = "corepatch_hidden_api";
    public static final String CP_SHARED = "corepatch_shared_uid";
    public static final String CP_AGENT = "corepatch_verification_agent";
    public static final String CP_BLOCK = "corepatch_vendor_block";
    public static final String CP_PERSISTENT = "corepatch_persistent_updates";
    public static final String CP_SYSTEM_UI_UID = "corepatch_systemui_keep_uid";
    public static final String[] CORE_PATCH = {CP_DOWNGRADE, CP_VERIFY, CP_ARSC, CP_DIGEST, CP_EXACT,
            CP_PREVIOUS, CP_HIDDEN, CP_SHARED, CP_AGENT, CP_BLOCK, CP_PERSISTENT, CP_SYSTEM_UI_UID};
    // System updater
    public static final String UPDATER_OTA_VALIDATE = "updater_ota_validate_off";
    public static final String UPDATER_NO_AUTO_DIALOG = "updater_no_auto_dialog";

    public static final String[] ALL = {
            BATTERY_RANKING, BATTERY_SCREEN_TIME, BATTERY_HEALTH, CHARGE_PROTECT, BATTERY_LIFE, POWER_EXTRAS,
            SMART_CHARGE, SUPER_WIRELESS, POWER_SAVE_REFRESH, POWER_SAVE_BRIGHTNESS, POWER_SAVE_BOOT,
            GAME_FPS, GAME_KEEP_ENABLED, GT_HDR, GT_SUPER_RES, GT_DYNAMIC_PERF, GT_TOUCH, GT_MI_TIME, GT_EXTRAS, GAME_MODES,
            GAME_TOAST_OFF, GAME_MACRO, GAME_NETWORK, JOYOSE_GPU, JOYOSE_CLOUD_OFF,
            NET_QUOTA_KEEP, NET_HOTSPOT_KEEP, NET_APP_CONTROLS, IMS_ICONS, SIM_MENUS,
            VIDEO_TOOLBOX, CALL_TOOLBOX,
            FLAG_SECURE, PERM_NO_WAIT, SECOND_SPACE, WARNINGS, FBO, TOOLBOX, DATA_USAGE, APP_INFO, DOCK, CAR_SICKNESS, APPLOCK_SCRAMBLE, LOW_BATTERY_QUIET,
            SIM_SUCCESS_DIALOG_OFF, ROOT_CHECK_OFF, USB_INSTALL_CHECK_OFF, LOCKSCREEN_PIN_SCRAMBLE,
            APP_INFO_BUTTONS, APP_ICON_LAUNCH, MMS_ADS, MMS_FRAUD, MMS_OVERSEAS, MMS_OLD_DEVICE, MMS_COPY_CODE, HOME_PRESTART, HOME_REPORT,
            DARK_MODE_ALL_APPS, STORE_FORCE_CHOOSER, STORE_GETAPPS_MINI,
            CLIPBOARD_WHITELIST, APPLIST_UPLOAD_OFF,
            THEME_THIRD_PARTY, THEME_FIX_RESET,
            CP_DOWNGRADE, CP_VERIFY, CP_ARSC, CP_DIGEST, CP_EXACT, CP_PREVIOUS, CP_HIDDEN, CP_SHARED, CP_AGENT, CP_BLOCK, CP_PERSISTENT, CP_SYSTEM_UI_UID,
            UPDATER_OTA_VALIDATE, UPDATER_NO_AUTO_DIALOG
    };

    /**
     * Off until chosen: everything the modded Security app did not do (extras taken from HyperCeiler
     * that change how the phone behaves), plus optional package-manager and framework policies.
     */
    private static final Set<String> OFF_BY_DEFAULT = Set.of(
            UPDATER_OTA_VALIDATE, UPDATER_NO_AUTO_DIALOG, JOYOSE_CLOUD_OFF, JOYOSE_GPU,
            APPLOCK_SCRAMBLE, LOCKSCREEN_PIN_SCRAMBLE, LOW_BATTERY_QUIET, GAME_TOAST_OFF, GAME_MACRO, GAME_NETWORK,
            APP_INFO_BUTTONS, APP_ICON_LAUNCH, MMS_ADS, MMS_FRAUD, MMS_OVERSEAS, MMS_OLD_DEVICE, MMS_COPY_CODE, HOME_PRESTART, HOME_REPORT,
            NET_QUOTA_KEEP, NET_HOTSPOT_KEEP, SMART_CHARGE, SUPER_WIRELESS, CAR_SICKNESS,
            SIM_SUCCESS_DIALOG_OFF, ROOT_CHECK_OFF, USB_INSTALL_CHECK_OFF, DARK_MODE_ALL_APPS,
            STORE_FORCE_CHOOSER, STORE_GETAPPS_MINI, THEME_THIRD_PARTY, THEME_FIX_RESET, FLAG_SECURE,
            CLIPBOARD_WHITELIST, APPLIST_UPLOAD_OFF,
            POWER_SAVE_REFRESH, POWER_SAVE_BRIGHTNESS, POWER_SAVE_BOOT, GAME_KEEP_ENABLED, NET_APP_CONTROLS, IMS_ICONS, SIM_MENUS,
            CP_DOWNGRADE, CP_VERIFY, CP_ARSC, CP_DIGEST, CP_EXACT, CP_PREVIOUS, CP_HIDDEN, CP_SHARED, CP_AGENT, CP_BLOCK, CP_PERSISTENT, CP_SYSTEM_UI_UID);

    /**
     * A brand-new install starts with every switch off; the user turns on what they want.
     * Existing installs keep their choices: {@link #legacyDefault} is frozen into their stored
     * settings once (see MainActivity), so this change never turns anything off for them.
     */
    public static boolean defaultValue(String key) {
        return false;
    }

    /**
     * The pre-1.4 default (the modded Security app's features on, HyperCeiler extras off). Written
     * once into an existing install's settings so an update never resets what they already had.
     */
    public static boolean legacyDefault(String key) {
        return !OFF_BY_DEFAULT.contains(key);
    }

    private static volatile SharedPreferences preferences;

    static void attach(SharedPreferences value) { preferences = value; }

    static boolean enabled(String key) {
        return read(preferences, key);
    }

    /** Old installs keep their header row until the new independent switch is explicitly saved. */
    public static boolean read(SharedPreferences p, String key) {
        if (p == null) return defaultValue(key);
        boolean fallback = APP_INFO_BUTTONS.equals(key) && p.getBoolean(APP_INFO, false);
        return p.getBoolean(key, fallback);
    }

    /** A stored list of package names (empty when unset or settings are unavailable). */
    static Set<String> stringSet(String key) {
        SharedPreferences p = preferences;
        Set<String> value = p == null ? null : p.getStringSet(key, null);
        return value == null ? Set.of() : value;
    }

    /** True once the remote settings are attached; false when they could not be read (e.g. early system_server). */
    static boolean ready() {
        return preferences != null;
    }

    private Features() { }
}
