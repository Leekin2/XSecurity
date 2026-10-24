package io.github.leekin2.xsecurity;

import android.content.Context;
import android.content.Intent;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stops the periodic ThemeReceiver check from automatically removing the applied theme.
 * The target and action are verified against the local HyperOS framework. Manual theme changes
 * and the system's crash-recovery reset are left intact. No filesystem permissions are changed.
 */
final class FixThemeResetHooks {
    private static final String CHECK_ACTION = "miui.intent.action.CHECK_TIME_UP";
    private final Hooks hooks;
    private final ClassLoader loader;
    private final AtomicBoolean reported = new AtomicBoolean();

    FixThemeResetHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    void install() {
        if (!Features.enabled(Features.THEME_FIX_RESET)) return;
        try {
            Class<?> receiver = loader.loadClass("miui.drm.ThemeReceiver");
            Method receive = receiver.getDeclaredMethod("onReceive", Context.class, Intent.class);
            if (receive.getReturnType() != void.class) {
                throw new NoSuchMethodException("Unexpected ThemeReceiver.onReceive return type");
            }
            hooks.install(receive, chain -> {
                Object value = chain.getArg(1);
                if (Features.enabled(Features.THEME_FIX_RESET) && value instanceof Intent
                        && CHECK_ACTION.equals(((Intent) value).getAction())) {
                    if (reported.compareAndSet(false, true)) {
                        hooks.info("theme reset: periodic check intercepted; manual theme changes retained");
                    }
                    return null;
                }
                return chain.proceed();
            });
            hooks.deoptimizeReported(receive);
            hooks.info("theme reset: periodic receiver hook ready");
        } catch (Throwable failure) {
            hooks.error("theme reset target unavailable; stock behaviour kept", failure);
        }
    }
}
