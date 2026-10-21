package io.github.leekin2.xsecurity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Removes only Battery Saver's display limits. Thermal/display-hardware votes remain intact. */
final class DisplayPolicyHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    DisplayPolicyHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (Features.enabled(Features.POWER_SAVE_REFRESH)) {
            try { keepRefresh(); hooks.info("framework battery-saver refresh ready"); }
            catch (Throwable t) { hooks.error("framework battery-saver refresh unavailable; stock behaviour kept", t); }
        }
        if (Features.enabled(Features.POWER_SAVE_BRIGHTNESS)) {
            try { keepBrightness(); hooks.info("framework battery-saver brightness ready"); }
            catch (Throwable t) { hooks.error("framework battery-saver brightness unavailable; stock behaviour kept", t); }
        }
    }

    private void keepRefresh() throws ReflectiveOperationException {
        Class<?> vote = loader.loadClass("com.android.server.display.mode.Vote");
        Class<?> storage = loader.loadClass("com.android.server.display.mode.VotesStorage");
        Set<Integer> lowPowerPriorities = new HashSet<>();
        for (String name : new String[]{"PRIORITY_LOW_POWER_MODE_RENDER_RATE", "PRIORITY_LOW_POWER_MODE_MODES", "PRIORITY_LOW_POWER_MODE"}) {
            try { Field f = vote.getDeclaredField(name); f.setAccessible(true); lowPowerPriorities.add(f.getInt(null)); }
            catch (NoSuchFieldException ignored) { }
        }
        if (lowPowerPriorities.isEmpty()) throw new NoSuchFieldException("Battery Saver display vote priorities");
        Method update = storage.getDeclaredMethod("updateVote", int.class, int.class, vote);
        if (update.getReturnType() != void.class) throw new NoSuchMethodException("Unexpected VotesStorage.updateVote result");
        hooks.install(update, chain -> {
            if (!lowPowerPriorities.contains((Integer) chain.getArg(1))) return chain.proceed();
            Object[] args = chain.getArgs().toArray();
            args[2] = null; // The normal storage path removes the previous vote and notifies listeners.
            return chain.proceed(args);
        });
    }

    @android.annotation.SuppressLint("SoonBlockedPrivateApi") // Runs inside LSPosed's system_server, outside app hidden-API enforcement.
    private void keepBrightness() throws ReflectiveOperationException {
        Class<?> policy = loader.loadClass("com.android.server.power.batterysaver.BatterySaverPolicy");
        Class<?> serviceType = loader.loadClass("android.os.PowerManager$ServiceType");
        Field typeField = serviceType.getDeclaredField("SCREEN_BRIGHTNESS");
        typeField.setAccessible(true);
        int brightnessType = typeField.getInt(null);
        Method get = policy.getDeclaredMethod("getBatterySaverPolicy", int.class);
        Class<?> state = loader.loadClass("android.os.PowerSaveState");
        if (get.getReturnType() != state) throw new NoSuchMethodException("Unexpected BatterySaverPolicy result");
        Field global = state.getField("globalBatterySaverEnabled");
        Class<?> builder = loader.loadClass("android.os.PowerSaveState$Builder");
        var constructor = builder.getConstructor();
        Method enabled = builder.getMethod("setBatterySaverEnabled", boolean.class);
        Method globallyEnabled = builder.getMethod("setGlobalBatterySaverEnabled", boolean.class);
        Method factor = builder.getMethod("setBrightnessFactor", float.class);
        Method build = builder.getMethod("build");
        hooks.install(get, chain -> {
            Object original = chain.proceed();
            if ((Integer) chain.getArg(0) != brightnessType || original == null) return original;
            Object replacement = constructor.newInstance();
            enabled.invoke(replacement, false);
            globallyEnabled.invoke(replacement, global.getBoolean(original));
            factor.invoke(replacement, 1f);
            return build.invoke(replacement);
        });
    }
}
