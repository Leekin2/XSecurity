package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;
import java.util.List;

/** Hooks in Joyose (com.xiaomi.joyose), Xiaomi's game tuning service, as in HyperCeiler's Joyose rules. */
final class JoyoseHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    JoyoseHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    void install(String apkPath, String dataDir) {
        boolean gpu = Features.enabled(Features.JOYOSE_GPU);
        boolean cloud = Features.enabled(Features.JOYOSE_CLOUD_OFF);
        if (!gpu && !cloud) return;
        try (DexTargets targets = new DexTargets(loader, apkPath, dataDir)) {
            if (gpu) run("GPU tuner", () -> {
                List<Method> methods = targets.usingStrings("gpu_tuner", "boolean", "GPUTUNER_SWITCH");
                if (methods.isEmpty()) throw new NoSuchMethodException("GPU tuner switch not found");
                for (Method method : methods) hooks.constant(method, true);
            });
            if (cloud) run("Joyose cloud sync off", () -> {
                List<Method> methods = targets.usingStrings("joyose_sync", "void", "job exist, sync local...");
                if (methods.isEmpty()) throw new NoSuchMethodException("Joyose cloud sync not found");
                for (Method method : methods) hooks.install(method, chain -> null);
            });
        } catch (Throwable failure) {
            hooks.error("Joyose scan unavailable", failure);
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
}
