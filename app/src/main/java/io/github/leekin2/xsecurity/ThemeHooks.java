package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;
import java.util.List;

import org.luckypray.dexkit.query.matchers.MethodMatcher;

/**
 * Hooks in the Theme Manager (com.android.thememanager). Lets themes that did not come from
 * Xiaomi's own store be applied, by answering the theme-rights check with "allowed" — the same
 * change HyperCeiler's "Allow to use third-party themes" makes.
 */
final class ThemeHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final String apkPath;
    private final String dataDir;

    ThemeHooks(Hooks hooks, ClassLoader loader, String apkPath, String dataDir) {
        this.hooks = hooks;
        this.loader = loader;
        this.apkPath = apkPath;
        this.dataDir = dataDir;
    }

    void install() {
        if (!Features.enabled(Features.THEME_THIRD_PARTY)) return;
        try (DexTargets targets = new DexTargets(loader, apkPath, dataDir)) {
            allowThirdParty(targets);
            hooks.info("theme: third-party themes ready");
        } catch (Throwable failure) {
            hooks.error("third-party themes", failure);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void allowThirdParty(DexTargets targets) throws Throwable {
        // The rights check logs "check rights isLegal: " and reads /system; make it return allowed.
        List<Method> methods = targets.matching("theme_rights", () -> MethodMatcher.create()
                .usingStrings(List.of("theme", "ThemeManagerTag", "/system", "check rights isLegal: ")));
        if (methods.isEmpty()) throw new NoSuchMethodException("theme rights check not found");
        Class<?> drmResult = Class.forName("miui.drm.DrmManager$DrmResult", false, loader);
        Object success = Enum.valueOf((Class<Enum>) drmResult, "DRM_SUCCESS");
        for (Method method : methods) hooks.constant(method, success);
    }
}
