package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;
import java.util.List;

/**
 * The Guard Provider (com.miui.guardprovider) uploads the installed app list to Xiaomi's server
 * from time to time. This turns that upload into a no-op.
 *
 * Ported from HyperCeiler (AGPL-3.0) rule guardprovider/DisableUploadAppListNew, rewritten against
 * this module's hook API. See NOTICE.
 */
final class GuardHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    GuardHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install(String apkPath, String dataDir) {
        if (!Features.enabled(Features.APPLIST_UPLOAD_OFF)) return;
        try (DexTargets targets = new DexTargets(loader, apkPath, dataDir)) {
            List<Method> methods = targets.matching("guard_applist_upload", () ->
                org.luckypray.dexkit.query.matchers.MethodMatcher.create()
                    .usingStrings(List.of("AntiDefraudAppManager", "https://flash.sec.miui.com/detect/app")));
            if (methods.isEmpty()) throw new NoSuchMethodException("App list upload not found");
            for (Method method : methods) {
                // Void methods return null; others keep their default so callers do not crash.
                Class<?> type = method.getReturnType();
                Object value = type == boolean.class ? (Object) Boolean.FALSE : null;
                hooks.install(method, chain -> value);
            }
            hooks.info("app list upload off ready");
        } catch (Throwable failure) {
            hooks.error("app list upload hook unavailable; stock behaviour kept", failure);
        }
    }
}
