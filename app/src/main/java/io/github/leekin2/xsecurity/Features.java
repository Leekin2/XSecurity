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
    // Charging
    public static final String SMART_CHARGE = "smart_charge_unlock";
    public static final String SUPER_WIRELESS = "super_wireless_charge";
    // Network
    public static final String NET_QUOTA_KEEP = "net_quota_keep_data";
    public static final String NET_HOTSPOT_KEEP = "net_hotspot_keep";
    // Video and calls
    public static final String VIDEO_TOOLBOX = "video_toolbox";
    public static final String CALL_TOOLBOX = "call_toolbox";
    // Privacy and system
    public static final String PERM_NO_WAIT = "perm_no_wait";
    public static final String SECOND_SPACE = "second_space";
    public static final String WARNINGS = "warnings";
    public static final String FBO = "fbo";
    public static final String TOOLBOX = "toolbox_layout";
    public static final String DATA_USAGE = "data_usage_control_center";
    public static final String APP_INFO = "app_info_actions";
    public static final String DOCK = "dock_50_apps";
    public static final String CAR_SICKNESS = "car_sickness_relief";
    public static final String APPLOCK_SCRAMBLE = "applock_pin_scramble";
    public static final String LOW_BATTERY_QUIET = "low_battery_quiet";
    // Performance and background
    public static final String PK_NO_KILL = "powerkeeper_no_kill";
    // System updater
    public static final String UPDATER_OTA_VALIDATE = "updater_ota_validate_off";
    public static final String UPDATER_NO_AUTO_DIALOG = "updater_no_auto_dialog";

    public static final String[] ALL = {
            BATTERY_RANKING, BATTERY_SCREEN_TIME, BATTERY_HEALTH, CHARGE_PROTECT, BATTERY_LIFE, POWER_EXTRAS,
            SMART_CHARGE, SUPER_WIRELESS,
            GAME_FPS, GT_HDR, GT_SUPER_RES, GT_DYNAMIC_PERF, GT_TOUCH, GT_MI_TIME, GT_EXTRAS, GAME_MODES,
            GAME_TOAST_OFF, GAME_MACRO, GAME_NETWORK, JOYOSE_GPU, JOYOSE_CLOUD_OFF,
            NET_QUOTA_KEEP, NET_HOTSPOT_KEEP,
            VIDEO_TOOLBOX, CALL_TOOLBOX,
            PERM_NO_WAIT, SECOND_SPACE, WARNINGS, FBO, TOOLBOX, DATA_USAGE, APP_INFO, DOCK, CAR_SICKNESS, APPLOCK_SCRAMBLE, LOW_BATTERY_QUIET,
            PK_NO_KILL,
            UPDATER_OTA_VALIDATE, UPDATER_NO_AUTO_DIALOG
    };

    /**
     * Off until chosen: OTA validation (wrong packages can be flashed), keeping background apps
     * (battery use) and Joyose's cloud updates (Xiaomi's game configuration stops updating).
     */
    private static final Set<String> OFF_BY_DEFAULT = Set.of(UPDATER_OTA_VALIDATE, JOYOSE_CLOUD_OFF, PK_NO_KILL, APPLOCK_SCRAMBLE, LOW_BATTERY_QUIET);

    /** Everything the modded Security app did is on by default, as in that mod. */
    public static boolean defaultValue(String key) {
        return !OFF_BY_DEFAULT.contains(key);
    }

    private static volatile SharedPreferences preferences;

    static void attach(SharedPreferences value) { preferences = value; }

    static boolean enabled(String key) {
        SharedPreferences p = preferences;
        return p == null ? defaultValue(key) : p.getBoolean(key, defaultValue(key));
    }

    private Features() { }
}
