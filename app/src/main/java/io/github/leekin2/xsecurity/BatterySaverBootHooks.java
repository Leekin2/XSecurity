package io.github.leekin2.xsecurity;

import android.content.Context;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/** Honor the OEM auto-exit preference when BootReceiver requests its extra saver-close check. */
final class BatterySaverBootHooks {
    private final Hooks hooks;

    BatterySaverBootHooks(Hooks hooks) { this.hooks = hooks; }

    void install(DexTargets targets) throws ReflectiveOperationException {
        List<Method> boot = targets.usingStrings("battery_saver_boot_close_v1", "void",
                "com.miui.powercenter.action.TRY_CLOSE_SAVE_MODE").stream()
                .filter(m -> m.getDeclaringClass().getName().equals("com.miui.securitycenter.receiver.BootReceiver")
                        && !Modifier.isStatic(m.getModifiers()) && m.getReturnType() == void.class
                        && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Context.class).toList();
        List<Method> preference = targets.usingStrings("battery_saver_auto_exit_v1", "boolean",
                "key_auto_exit_power_save_mode_enabled").stream()
                .filter(m -> Modifier.isStatic(m.getModifiers()) && m.getParameterCount() == 0
                        && m.getReturnType() == boolean.class).toList();
        if (boot.size() != 1 || preference.size() != 1)
            throw new NoSuchMethodException("Unique saver boot-close and auto-exit preference required");
        Method autoExit = preference.get(0);
        autoExit.setAccessible(true);
        hooks.install(boot.get(0), chain -> {
            if (Features.enabled(Features.POWER_SAVE_BOOT)) {
                try {
                    if (Boolean.FALSE.equals(autoExit.invoke(null))) return null;
                } catch (ReflectiveOperationException unavailable) {
                    hooks.error("Battery Saver auto-exit preference unavailable; boot check retained", unavailable);
                }
            }
            return chain.proceed();
        });
        hooks.deoptimizeReported(boot.get(0));
        hooks.info("Battery Saver boot-close respects OEM auto-exit preference");
    }
}
