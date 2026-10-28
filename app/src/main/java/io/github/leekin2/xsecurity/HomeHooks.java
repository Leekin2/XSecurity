package io.github.leekin2.xsecurity;

import android.content.Context;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;

/** HyperCeiler DisablePrestart / HideReportText rules, scoped to the launcher; AGPL-3.0. */
final class HomeHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final ThreadLocal<Integer> uninstallDepth = ThreadLocal.withInitial(() -> 0);
    private static final Set<String> PRESTART = Set.of("persist.sys.usap_pool_enabled", "persist.sys.dynamic_usap_enabled",
            "persist.sys.prestart.proc", "persist.sys.prestart.feedback.enable", "persist.sys.launch_response_optimization.enable");

    HomeHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (Features.enabled(Features.HOME_PRESTART)) {
            try { prestart(); } catch (Throwable failure) { hooks.error("Launcher preload target unavailable; stock behaviour retained", failure); }
        }
        if (Features.enabled(Features.HOME_REPORT)) {
            try { report(); } catch (Throwable failure) { hooks.error("Launcher uninstall-report target unavailable; stock behaviour retained", failure); }
        }
    }

    private void prestart() throws Throwable {
        Method method = loader.loadClass("com.miui.launcher.utils.SystemProperties").getDeclaredMethod("getBoolean", String.class, boolean.class);
        if (method.getReturnType() != boolean.class || !Modifier.isStatic(method.getModifiers())) throw new NoSuchMethodException("Launcher property getter");
        hooks.install(method, chain -> Features.enabled(Features.HOME_PRESTART) && chain.getArg(0) instanceof String key
                && PRESTART.contains(key) ? false : chain.proceed());
        hooks.deoptimizeReported(method);
        hooks.info("Launcher preload property reads ready; system properties unchanged");
    }

    private void report() throws Throwable {
        Class<?> dialog = loader.loadClass("com.miui.home.launcher.uninstall.BaseUninstallDialog");
        Method installer = loader.loadClass("com.miui.home.launcher.ShortcutInfo").getDeclaredMethod("getInstallerPackageName");
        if (installer.getReturnType() != String.class || Modifier.isStatic(installer.getModifiers())) throw new NoSuchMethodException("Shortcut installer getter");
        java.util.ArrayList<Method> initializers = new java.util.ArrayList<>();
        for (Method method : dialog.getDeclaredMethods()) {
            Class<?>[] args = method.getParameterTypes();
            if (method.getName().equals("init") && method.getReturnType() == void.class && !Modifier.isStatic(method.getModifiers())
                    && (args.length == 2 || args.length == 3) && args[0] == Context.class && args[1] == List.class
                    && (args.length == 2 || args[2] == String.class)) initializers.add(method);
        }
        if (initializers.isEmpty()) throw new NoSuchMethodException("Launcher uninstall dialog initialization");
        hooks.install(installer, chain -> Features.enabled(Features.HOME_REPORT) && uninstallDepth.get() > 0 ? "com.xiaomi.market" : chain.proceed());
        hooks.deoptimizeReported(installer);
        for (Method method : initializers) {
            hooks.install(method, chain -> {
                if (!Features.enabled(Features.HOME_REPORT)) return chain.proceed();
                int previous = uninstallDepth.get();
                uninstallDepth.set(previous + 1);
                try { return chain.proceed(); }
                finally { if (previous == 0) uninstallDepth.remove(); else uninstallDepth.set(previous); }
            });
            hooks.deoptimizeReported(method);
        }
        hooks.info("Launcher uninstall report text ready; installer identity outside dialog unchanged");
    }
}
