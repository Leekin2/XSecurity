package io.github.leekin2.xsecurity;

import android.content.pm.ApplicationInfo;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class XSecurityModule extends XposedModule {
    private static final String SECURITY = "com.miui.securitycenter";
    private static final String POWERKEEPER = "com.miui.powerkeeper";
    private static final String UPDATER = "com.android.updater";
    private static final String JOYOSE = "com.xiaomi.joyose";
    private static final String THEMES = "com.android.thememanager";
    private static final String GUARD = "com.miui.guardprovider";
    private static final String SETTINGS = "com.android.settings";
    private static final String SYSTEM_UI = "com.android.systemui";
    private static final String PHONE = "com.android.phone";
    private final Hooks hooks = new Hooks(this);
    private String process;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        process = param.getProcessName();
        try { Features.attach(getRemotePreferences(Features.GROUP)); }
        catch (Throwable failure) { hooks.error("Settings unavailable; defaults used", failure); }
        hooks.info("module loaded in " + process + "; version=" + BuildConfig.VERSION_NAME + "; settings=" + Features.ready());
    }

    @Override
    public void onPackageReady(XposedModuleInterface.PackageReadyParam param) {
        String name = param.getPackageName();
        if (!param.isFirstPackage()) return;
        ApplicationInfo info = param.getApplicationInfo();
        if (SECURITY.equals(name)) {
            new SecurityHooks(hooks, this, param.getClassLoader()).install(info.sourceDir, info.dataDir, process);
        } else if (POWERKEEPER.equals(name) && (process == null || process.equals(POWERKEEPER))) {
            // Only the main process sets the refresh rate.
            new PowerKeeperHooks(hooks, param.getClassLoader()).install();
        } else if (UPDATER.equals(name) && (process == null || process.equals(UPDATER))) {
            new UpdaterHooks(hooks, param.getClassLoader()).install(info.sourceDir, info.dataDir);
        } else if (JOYOSE.equals(name) && (process == null || process.equals(JOYOSE))) {
            new JoyoseHooks(hooks, param.getClassLoader()).install(info.sourceDir, info.dataDir);
        } else if (THEMES.equals(name) && (process == null || process.equals(THEMES))) {
            new ThemeHooks(hooks, param.getClassLoader(), info.sourceDir, info.dataDir).install();
        } else if (GUARD.equals(name) && (process == null || process.equals(GUARD))) {
            new GuardHooks(hooks, param.getClassLoader()).install(info.sourceDir, info.dataDir);
        } else if (SETTINGS.equals(name) && (process == null || process.equals(SETTINGS))) {
            new DarkModeSearchHooks(hooks, param.getClassLoader()).install();
        } else if (SYSTEM_UI.equals(name) && (process == null || process.equals(SYSTEM_UI))) {
            new LockscreenPinHooks(hooks, param.getClassLoader()).install();
            new ImsIconHooks(hooks, param.getClassLoader()).install();
        } else if (PHONE.equals(name) && (process == null || process.equals(PHONE))) {
            new PhoneHooks(hooks, param.getClassLoader()).install();
        }
    }

    @Override
    public void onSystemServerStarting(XposedModuleInterface.SystemServerStartingParam param) {
        // Scope alone is not consent to enable a feature. Unavailable preferences keep
        // optional framework hooks off; the log distinguishes this from a missing target.
        if (!Features.ready()) {
            hooks.info("system_server: settings unavailable; optional hooks kept off");
            return;
        }
        hooks.info("system_server: settings ready");
        new CorePatchHooks(hooks, param.getClassLoader()).install();
        new DisplayPolicyHooks(hooks, param.getClassLoader()).install();
        new DarkModeHooks(hooks, param.getClassLoader()).install();
        new AppStoreHooks(hooks, param.getClassLoader()).install();
        new ClipboardHooks(hooks, param.getClassLoader()).install();
        if (Features.enabled(Features.THEME_FIX_RESET)) {
            new FixThemeResetHooks(hooks, param.getClassLoader()).install();
        }
        if (Features.enabled(Features.FLAG_SECURE)) {
            new FlagSecureHooks(hooks, param.getClassLoader()).install();
        }
    }
}
