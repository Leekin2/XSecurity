package io.github.leekin2.xsecurity

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.leekin2.xsecurity.ui.Actions
import io.github.leekin2.xsecurity.ui.AppLanguage
import io.github.leekin2.xsecurity.ui.Look
import io.github.leekin2.xsecurity.ui.MaterialScreen
import io.github.leekin2.xsecurity.ui.MiuixScreen
import io.github.leekin2.xsecurity.ui.UiState
import io.github.leekin2.xsecurity.ui.XTheme
import io.github.leekin2.xsecurity.ui.textsFor
import io.github.libxposed.service.XposedService
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private var preferences: SharedPreferences? = null
    private var state by mutableStateOf(UiState())
    private val worker = Executors.newSingleThreadExecutor()
    private val texts get() = textsFor(state.language)

    /** The switches shown before LSPosed connects, and the screen's own choices. */
    private val local get() = getSharedPreferences("switches", MODE_PRIVATE)
    private val ui get() = getSharedPreferences("ui", MODE_PRIVATE)

    private val serviceWatcher = java.util.function.Consumer<XposedService?> { service ->
        runOnUiThread {
            if (service == null) {
                preferences = null
                if (state.connected != null) state = state.copy(connected = false)
            } else bind(service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // First run follows the system language; afterwards the user's choice wins.
        val language = ui.getString("language", null)?.let(AppLanguage::of) ?: AppLanguage.forSystem(Locale.getDefault().language)
        state = state.copy(
            values = Features.ALL.associateWith { local.getBoolean(it, Features.defaultValue(it)) },
            language = language,
            look = Look(ui.getBoolean("hyper", true), ui.getInt("theme", 0), ui.getBoolean("monet", false), ui.getBoolean("blur", false)),
            iconHidden = iconHidden()
        )
        val actions = Actions(::change, ::restartApps, ::changeLanguage, ::changeLook,
            { state = state.copy(settingsOpen = it) }, ::openLsposed, ::setIconHidden)
        setContent {
            XTheme(state.look) {
                if (state.look.hyper) MiuixScreen(state, actions) else MaterialScreen(state, actions)
            }
        }
        XSecurityApp.watch(serviceWatcher)
        // LSPosed hands its service right after the process starts; give a slow boot a few seconds.
        window.decorView.postDelayed({ if (state.connected == null && XSecurityApp.current() == null) state = state.copy(connected = false) }, 6000)
    }

    override fun onDestroy() {
        XSecurityApp.unwatch(serviceWatcher)
        worker.shutdown()
        super.onDestroy()
    }

    private fun bind(service: XposedService) {
        val remote = runCatching { service.getRemotePreferences(Features.GROUP) }
            .onFailure { android.util.Log.e(Hooks.TAG, "LSPosed settings unavailable", it) }.getOrNull()
        preferences = remote
        // The service itself proves the module is enabled; settings may still be unavailable.
        state = state.copy(connected = true)
        if (remote != null) {
            // Retired features are removed from both UI and hooks, including stored choices.
            val retired = arrayOf("powerkeeper_no_kill", "autostart_keep_grants", "system_reduce_cleaning",
                "developer_signature_test", "developer_downgrade_test", "developer_test_packages", "developer_certificate_sha256",
                "ims_icon_support", "ims_restart_pending")
            remote.edit().apply { retired.forEach { remove(it) } }.apply()
            local.edit().apply { retired.forEach { remove(it) } }.apply()
            val values = Features.ALL.associateWith { remote.getBoolean(it, Features.defaultValue(it)) }
            local.edit().apply { values.forEach { (k, v) -> putBoolean(k, v) } }.apply()
            state = state.copy(values = values)
        }
        requestMissingScope(service)
    }

    /** Keep the fixed module targets selected; remove obsolete or unrelated scope entries. */
    private fun requestMissingScope(service: XposedService) {
        runCatching {
            val scope = service.scope.toSet()
            val supported = setOf("system", "com.miui.securitycenter", "com.miui.powerkeeper",
                "com.android.updater", "com.xiaomi.joyose", "com.android.thememanager", "com.miui.gallery")
            val obsolete = scope - supported
            if (obsolete.isNotEmpty()) service.removeScope(obsolete.toList())
            val missing = supported.filter { it !in scope }
                .filter { it == "system" || runCatching { packageManager.getPackageInfo(it, 0) }.isSuccess }
            if (missing.isEmpty()) return
            val version = packageManager.getPackageInfo(packageName, 0).longVersionCode
            val asked = getSharedPreferences("scope", MODE_PRIVATE)
            val requestKey = "asked:" + missing.sorted().joinToString("|")
            if (asked.getLong(requestKey, 0) == version) return
            service.requestScope(missing, object : XposedService.OnScopeEventListener {})
            asked.edit().putLong(requestKey, version).apply()
        }.onFailure { android.util.Log.e(Hooks.TAG, "Scope request failed", it) }
    }

    private fun change(key: String, value: Boolean) {
        val remote = preferences
        if (remote == null) {
            Toast.makeText(this, texts.notConnected, Toast.LENGTH_SHORT).show()
            return
        }
        remote.edit().putBoolean(key, value).apply()
        local.edit().putBoolean(key, value).apply()
        state = state.copy(values = state.values + (key to value))
        if (key in Features.CORE_PATCH || key in arrayOf(Features.FLAG_SECURE, Features.POWER_SAVE_REFRESH, Features.POWER_SAVE_BRIGHTNESS)) {
            Toast.makeText(this, io.github.leekin2.xsecurity.ui.frameworkRestartHint(state.language), Toast.LENGTH_LONG).show()
        }
        if (value) XSecurityApp.current()?.let(::requestMissingScope)
    }

    private fun changeLanguage(language: AppLanguage) {
        ui.edit().putString("language", language.code).apply()
        state = state.copy(language = language)
    }

    private fun changeLook(look: Look) {
        ui.edit().putBoolean("hyper", look.hyper).putInt("theme", look.theme)
            .putBoolean("monet", look.monet).putBoolean("blur", look.blur).apply()
        state = state.copy(look = look)
    }

    /** Opens XSecurity's page in LSPosed (or LSPosed's main screen where links are ignored). */
    private fun openLsposed() {
        val user = android.os.Process.myUid() / 100000
        val module = Intent(Intent.ACTION_MAIN).setComponent(ComponentName("org.lsposed.manager", "org.lsposed.manager.ui.activity.MainActivity"))
            .setData(Uri.parse("module://$packageName:$user"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivity(module) }
            .recoverCatching { startActivity(packageManager.getLaunchIntentForPackage("org.lsposed.manager")!!) }
            .onFailure { Toast.makeText(this, texts.notConnected, Toast.LENGTH_SHORT).show() }
    }

    // The alias class is under the code namespace, which differs from the application id.
    private val launcherAlias get() = ComponentName(this, "${javaClass.`package`!!.name}.Launcher")

    private fun iconHidden() = packageManager.getComponentEnabledSetting(launcherAlias) ==
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    /** Hides or shows the home screen icon; the app still opens from LSPosed when hidden. */
    private fun setIconHidden(hidden: Boolean) {
        runCatching {
            packageManager.setComponentEnabledSetting(launcherAlias,
                if (hidden) PackageManager.COMPONENT_ENABLED_STATE_DISABLED else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
            state = state.copy(iconHidden = hidden)
        }.onFailure { Toast.makeText(this, texts.notConnected, Toast.LENGTH_SHORT).show() }
    }

    /** Changes apply when the scope apps start again; this needs root. */
    private fun restartApps() {
        val t = texts
        state = state.copy(restarting = true)
        worker.execute {
            // Root is proven by "id -u" printing 0; a process that is already gone is not a failure.
            // Processes are found by exact name: "pkill -f" would also match (and kill) this shell.
            val names = "com.miui.securitycenter com.miui.securitycenter.remote com.miui.securitycenter.power " +
                "com.miui.securitycenter:ui com.miui.securitycenter.bootaware com.miui.powerkeeper com.xiaomi.joyose com.android.updater com.android.thememanager com.miui.gallery"
            val ok = runCatching {
                val process = ProcessBuilder("su", "-c",
                    "id -u; am force-stop com.miui.securitycenter; am force-stop com.android.updater; " +
                        "for p in \$(pidof $names); do kill \$p; done; true")
                    .redirectErrorStream(true).start()
                val output = String(process.inputStream.readBytes())
                process.waitFor()
                output.lineSequence().firstOrNull()?.trim() == "0"
            }.getOrDefault(false)
            runOnUiThread {
                state = state.copy(restarting = false)
                Toast.makeText(this, if (ok) t.restarted else t.restartFailed, Toast.LENGTH_LONG).show()
            }
        }
    }
}
