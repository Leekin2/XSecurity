package io.github.leekin2.xsecurity;

import android.content.pm.ApplicationInfo;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedModuleInterface;

public final class XSecurityModule extends XposedModule {
    private static final String SECURITY = "com.miui.securitycenter";
    private static final String POWERKEEPER = "com.miui.powerkeeper";
    private static final String UPDATER = "com.android.updater";
    private static final String JOYOSE = "com.xiaomi.joyose";
    private final Hooks hooks = new Hooks(this);
    private String process;

    @Override
    public void onModuleLoaded(XposedModuleInterface.ModuleLoadedParam param) {
        process = param.getProcessName();
        try { Features.attach(getRemotePreferences(Features.GROUP)); }
        catch (Throwable failure) { hooks.error("Settings unavailable; defaults used", failure); }
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
        }
    }
}
