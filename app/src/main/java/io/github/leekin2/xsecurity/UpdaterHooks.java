package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Hooks in the system updater (com.android.updater), as HyperCeiler's "Updater" page does.
 */
final class UpdaterHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    UpdaterHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    void install(String apkPath, String dataDir) {
        if (Features.enabled(Features.UPDATER_OTA_VALIDATE)) run("OTA validation off", this::otaValidation);
        if (!Features.enabled(Features.UPDATER_NO_AUTO_DIALOG)) return;
        try (DexTargets targets = new DexTargets(loader, apkPath, dataDir)) {
            run("auto update dialog off", () -> autoUpdateDialog(targets));
        } catch (Throwable failure) {
            hooks.error("Updater scan unavailable", failure);
        }
    }

    private interface Step { void run() throws Throwable; }

    private void run(String name, Step step) {
        try {
            step.run();
            hooks.info(name + " ready");
        } catch (Throwable failure) {
            hooks.error(name + " unavailable; stock behaviour kept", failure);
        }
    }

    /**
     * A local ROM package is first sent to Xiaomi's server for approval when the device lists the
     * "support_ota_validate" feature; without it the updater installs a full package directly (the
     * package's own signature is still checked by the system). Only for VAB devices.
     */
    private void otaValidation() throws Throwable {
        Class<?> parser = Class.forName("miui.util.FeatureParser", false, loader);
        Method hasFeature = parser.getDeclaredMethod("hasFeature", String.class, int.class);
        hooks.install(hasFeature, chain -> {
            if ("support_ota_validate".equals(chain.getArg(0))) return false;
            return chain.proceed();
        });
    }

    /** "Turn on automatic updates?" asks again every three days; the check that shows it does nothing. */
    private void autoUpdateDialog(DexTargets targets) throws Throwable {
        List<Method> methods = targets.usingStrings("updater_auto_dialog", "void", "isShowAutoSetDialog: interval ");
        if (methods.isEmpty()) throw new NoSuchMethodException("Auto update dialog check not found");
        for (Method method : methods) hooks.install(method, chain -> null);
    }
}
