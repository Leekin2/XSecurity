package io.github.leekin2.xsecurity;

import android.content.pm.ApplicationInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Dark mode "Individual apps" list: the China ROM shows almost no apps there. This builds the
 * list as the global ROM does, so every user app can be toggled for forced dark mode.
 *
 * Ported from HyperCeiler (AGPL-3.0) rule systemframework/display/AllDarkMode (itself from
 * SetoHook by SetoSkins): temporarily reports a global build while the list is built and lets
 * non-system user apps show in Settings. Rewritten against this module's hook API. See NOTICE.
 */
final class DarkModeHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    DarkModeHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.DARK_MODE_ALL_APPS)) return;
        try { hookList(); hooks.info("dark-mode app list ready"); }
        catch (Throwable t) { hooks.error("dark-mode app list unavailable; stock list kept", t); }
    }

    private void hookList() throws ReflectiveOperationException {
        Class<?> manager = loader.loadClass("com.android.server.ForceDarkAppListManager");
        Class<?> miuiBuild = loader.loadClass("miui.os.Build");
        Field international = miuiBuild.getDeclaredField("IS_INTERNATIONAL_BUILD");
        international.setAccessible(true);
        boolean stock = international.getBoolean(null);

        // While the list is assembled, report a global build so the full set is produced.
        for (Method method : manager.getDeclaredMethods()) {
            if (!method.getName().equals("getDarkModeAppList")) continue;
            hooks.install(method, chain -> {
                international.setBoolean(null, true);
                try { return chain.proceed(); }
                finally { international.setBoolean(null, stock); }
            });
        }

        // Show every ordinary user app in the Settings list (skip system apps and shared-uid system).
        for (Method method : manager.getDeclaredMethods()) {
            if (!method.getName().equals("shouldShowInSettings")) continue;
            hooks.install(method, chain -> {
                Object arg = chain.getArg(0);
                if (!(arg instanceof ApplicationInfo)) return chain.proceed();
                ApplicationInfo info = (ApplicationInfo) arg;
                return (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && info.uid >= 10000;
            });
        }
    }
}
