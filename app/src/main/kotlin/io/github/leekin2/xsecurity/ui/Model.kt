package io.github.leekin2.xsecurity.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReportOff
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CarCrash
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DataUsage
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.HdrOn
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.HourglassDisabled
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UpdateDisabled
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.leekin2.xsecurity.Features as F

/** One switch of the home page. */
class Item(val key: String, val icon: ImageVector)

/** A folding card of the home page, with its badge color. */
class Section(val id: String, val icon: ImageVector, val color: Color, val items: List<Item>)

/** Home page order: the updater first, then the most used groups. */
val SECTIONS = listOf(
    Section("updater", Icons.Rounded.SystemUpdate, Color(0xFF3482FF), listOf(
        Item(F.UPDATER_NO_AUTO_DIALOG, Icons.Rounded.UpdateDisabled),
        Item(F.UPDATER_OTA_VALIDATE, Icons.Rounded.SystemUpdate))),
    Section("charge", Icons.Rounded.BatteryChargingFull, Color(0xFF2EB872), listOf(
        Item(F.CHARGE_PROTECT, Icons.Rounded.BatteryChargingFull),
        Item(F.SMART_CHARGE, Icons.Rounded.Bolt),
        Item(F.SUPER_WIRELESS, Icons.Rounded.WifiTethering))),
    Section("games", Icons.Rounded.SportsEsports, Color(0xFFE5484D), listOf(
        Item(F.GAME_FPS, Icons.Rounded.Speed),
        Item(F.GAME_KEEP_ENABLED, Icons.Rounded.SportsEsports),
        Item(F.GAME_MODES, Icons.Rounded.Bolt),
        Item(F.GT_SUPER_RES, Icons.Rounded.SlowMotionVideo),
        Item(F.GT_HDR, Icons.Rounded.HdrOn),
        Item(F.GT_DYNAMIC_PERF, Icons.Rounded.Tune),
        Item(F.GT_TOUCH, Icons.Rounded.TouchApp),
        Item(F.GT_MI_TIME, Icons.Rounded.Timer),
        Item(F.GT_EXTRAS, Icons.Rounded.GpsFixed),
        Item(F.GAME_TOAST_OFF, Icons.Rounded.NotificationsOff),
        Item(F.GAME_MACRO, Icons.Rounded.Keyboard),
        Item(F.GAME_NETWORK, Icons.Rounded.Wifi),
        Item(F.JOYOSE_GPU, Icons.Rounded.DeveloperBoard),
        Item(F.JOYOSE_CLOUD_OFF, Icons.Rounded.CloudOff))),
    Section("network", Icons.Rounded.CellTower, Color(0xFF8E4EC6), listOf(
        Item(F.IMS_ICONS, Icons.Rounded.SimCard),
        Item(F.SIM_MENUS, Icons.Rounded.SimCard),
        Item(F.NET_APP_CONTROLS, Icons.Rounded.Wifi),
        Item(F.NET_QUOTA_KEEP, Icons.Rounded.SignalCellularAlt),
        Item(F.NET_HOTSPOT_KEEP, Icons.Rounded.Wifi),
        Item(F.DATA_USAGE, Icons.Rounded.DataUsage))),
    Section("battery", Icons.Rounded.BatteryStd, Color(0xFFF2A11B), listOf(
        Item(F.BATTERY_RANKING, Icons.Rounded.Insights),
        Item(F.BATTERY_SCREEN_TIME, Icons.Rounded.Schedule),
        Item(F.BATTERY_HEALTH, Icons.Rounded.BatteryStd),
        Item(F.BATTERY_LIFE, Icons.Rounded.HealthAndSafety),
        Item(F.POWER_EXTRAS, Icons.Rounded.PowerSettingsNew),
        Item(F.POWER_SAVE_REFRESH, Icons.Rounded.Speed),
        Item(F.POWER_SAVE_BRIGHTNESS, Icons.Rounded.Tune),
        Item(F.POWER_SAVE_BOOT, Icons.Rounded.PowerSettingsNew),
        Item(F.LOW_BATTERY_QUIET, Icons.Rounded.NotificationsOff))),
    Section("media", Icons.Rounded.Movie, Color(0xFFE0558E), listOf(
        Item(F.VIDEO_TOOLBOX, Icons.Rounded.Movie),
        Item(F.CALL_TOOLBOX, Icons.Rounded.VideoCall))),
    Section("system", Icons.Rounded.Info, Color(0xFF6E7787), listOf(
        Item(F.FLAG_SECURE, Icons.Rounded.Screenshot),
        Item(F.APP_INFO, Icons.Rounded.Info),
        Item(F.APP_INFO_BUTTONS, Icons.Rounded.TouchApp),
        Item(F.APP_ICON_LAUNCH, Icons.Rounded.TouchApp),
        Item(F.TOOLBOX, Icons.Rounded.Crop),
        Item(F.DOCK, Icons.Rounded.ViewSidebar),
        Item(F.PERM_NO_WAIT, Icons.Rounded.HourglassDisabled),
        Item(F.APPLOCK_SCRAMBLE, Icons.Rounded.Keyboard),
        Item(F.LOCKSCREEN_PIN_SCRAMBLE, Icons.Rounded.Keyboard),
        Item(F.CAR_SICKNESS, Icons.Rounded.CarCrash),
        Item(F.SECOND_SPACE, Icons.Rounded.Group),
        Item(F.WARNINGS, Icons.Rounded.CarCrash),
        Item(F.FBO, Icons.Rounded.Storage),
        Item(F.SIM_SUCCESS_DIALOG_OFF, Icons.Rounded.SimCard),
        Item(F.ROOT_CHECK_OFF, Icons.Rounded.Security),
        Item(F.USB_INSTALL_CHECK_OFF, Icons.Rounded.Usb),
        Item(F.DARK_MODE_ALL_APPS, Icons.Rounded.DarkMode),
        Item(F.CLIPBOARD_WHITELIST, Icons.Rounded.ContentPaste),
        Item(F.APPLIST_UPLOAD_OFF, Icons.Rounded.CloudOff))),
    Section("messages", Icons.Rounded.Sms, Color(0xFF2E9C86), listOf(
        Item(F.MMS_ADS, Icons.Rounded.Block),
        Item(F.MMS_FRAUD, Icons.Rounded.Warning),
        Item(F.MMS_OVERSEAS, Icons.Rounded.Public),
        Item(F.MMS_OLD_DEVICE, Icons.Rounded.PhoneAndroid),
        Item(F.MMS_COPY_CODE, Icons.Rounded.ContentCopy))),
    Section("launcher", Icons.Rounded.Home, Color(0xFF658BDA), listOf(
        Item(F.HOME_PRESTART, Icons.Rounded.Speed),
        Item(F.HOME_REPORT, Icons.Rounded.ReportOff))),
    Section("store", Icons.Rounded.Storefront, Color(0xFF1FA2A6), listOf(
        Item(F.STORE_FORCE_CHOOSER, Icons.Rounded.Storefront),
        Item(F.STORE_GETAPPS_MINI, Icons.Rounded.PictureInPictureAlt))),
    Section("theme", Icons.Rounded.Palette, Color(0xFFD6409F), listOf(
        Item(F.THEME_THIRD_PARTY, Icons.Rounded.Palette),
        Item(F.THEME_FIX_RESET, Icons.Rounded.Lock))),
    Section("corepatch", Icons.Rounded.DeveloperBoard, Color(0xFF997744),
        F.CORE_PATCH.map { Item(it, Icons.Rounded.DeveloperBoard) }),
)

/**
 * How the screen looks. [hyper] picks the HyperOS (Miuix) style, otherwise Material You.
 * [theme]: 0 follows the system, 1 light, 2 dark. [monet] and [blur] belong to the HyperOS style.
 */
data class Look(val hyper: Boolean = true, val theme: Int = 0, val monet: Boolean = false, val blur: Boolean = false)

data class UiState(
    /** null while LSPosed has not answered yet. */
    val connected: Boolean? = null,
    val values: Map<String, Boolean> = emptyMap(),
    val language: AppLanguage = AppLanguage.TR,
    val look: Look = Look(),
    val settingsOpen: Boolean = false,
    val restarting: Boolean = false,
    /** Whether the home screen launcher icon is currently hidden. */
    val iconHidden: Boolean = false,
    /** How many apps are on the background clipboard list. */
    val clipboardApps: Int = 0,
    /** Native-only HyperOS launchers do not run the Java launcher hook entry. */
    val nativeLauncher: Boolean = false,
)

fun featureAvailable(state: UiState, key: String): Boolean =
    !state.nativeLauncher || key !in setOf(F.HOME_PRESTART, F.HOME_REPORT)

fun featureCanChange(state: UiState, key: String): Boolean =
    state.connected != false && (featureAvailable(state, key) || state.values[key] == true)

/** Everything the screens can ask for. */
class Actions(
    val onChange: (String, Boolean) -> Unit,
    val onRestart: () -> Unit,
    val onLanguage: (AppLanguage) -> Unit,
    val onLook: (Look) -> Unit,
    val onSettings: (Boolean) -> Unit,
    val onLsposed: () -> Unit,
    /** Hides or shows the home screen launcher icon. */
    val onHideIcon: (Boolean) -> Unit,
    /** Opens the picker for apps allowed to read the clipboard in the background. */
    val onPickClipboardApps: () -> Unit,
)
