package io.github.leekin2.xsecurity;

import android.content.Context;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Small behaviour changes anchored to strings verified in Security 13.6.5. */
final class SecurityBehaviorHooks {
    private final Hooks hooks;

    SecurityBehaviorHooks(Hooks hooks) { this.hooks = hooks; }

    void keepRefresh(DexTargets targets) throws ReflectiveOperationException {
        skipPowerModeEntry(targets.usingStrings("power_save_refresh_entry_v1", "void",
                "LowFpsPowerMode", "PowerMode enterPowerMode currentRefreshRate "));
    }

    void keepBrightness(DexTargets targets) throws ReflectiveOperationException {
        skipPowerModeEntry(targets.usingStrings("power_save_brightness_entry_v1", "void",
                "BrightnessPowerMode", "currentPercentage:"));
    }

    private void skipPowerModeEntry(List<Method> candidates) throws NoSuchMethodException {
        List<Method> matching = new ArrayList<>();
        for (Method m : candidates) {
            Class<?>[] p = m.getParameterTypes();
            if (m.getReturnType() == void.class && p.length >= 1 && p[0] == Context.class) matching.add(m);
        }
        // A changed/ambiguous APK should keep stock behaviour, not hook a guessed method.
        if (matching.size() != 1) throw new NoSuchMethodException("Expected one power-mode entry, found " + matching.size());
        hooks.install(matching.get(0), chain -> null);
        // Leave-mode methods still run, restoring values set before the module was enabled.
    }

    void keepGameEnabled(DexTargets targets) throws ReflectiveOperationException {
        List<Method> getters = targets.usingStrings("game_keep_enabled_get_v1", "boolean", "pref_open_game_booster")
                .stream().filter(m -> m.getParameterCount() == 0).toList();
        List<Method> setters = targets.usingStrings("game_keep_enabled_set_v1", "void", "pref_open_game_booster")
                .stream().filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class).toList();
        if (getters.size() != 1 || setters.size() != 1) {
            throw new NoSuchMethodException("Game Turbo master getter/setter not unique: " + getters.size() + "/" + setters.size());
        }
        hooks.constant(getters.get(0), true);
        hooks.install(setters.get(0), chain -> chain.proceed(new Object[]{true}));
        // Never force gb_boosting: that is the *active game session*, not the master setting.
    }
}
