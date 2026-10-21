package io.github.leekin2.xsecurity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Hooks in Battery and performance (com.miui.powerkeeper), which sets the screen refresh rate
 * per app. On the China ROM a game that is not on Xiaomi's high-FPS list (sent by Joyose) is put
 * at 60 Hz, even when you chose 120/144 Hz in Settings.
 */
final class PowerKeeperHooks {
    private static final String DISPLAY = "com.miui.powerkeeper.statemachine.DisplayFrameSetting";
    private final Hooks hooks;
    private final ClassLoader loader;

    PowerKeeperHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    void install() {
        if (Features.enabled(Features.GAME_FPS)) {
            try {
                gameRefreshRate();
                hooks.info("game refresh rate ready");
            } catch (Throwable failure) {
                hooks.error("game refresh rate unavailable; stock behaviour kept", failure);
            }
        }
    }

    /** A game gets at least the refresh rate you chose in Settings. */
    private void gameRefreshRate() throws Throwable {
        Class<?> display = Class.forName(DISPLAY, false, loader);
        Method setScreenEffect = display.getDeclaredMethod("setScreenEffect", String.class, int.class, int.class);
        Field gameCookie = display.getDeclaredField("COOKIE_GAME");
        Field userFps = display.getDeclaredField("mUserFps");
        gameCookie.setAccessible(true);
        userFps.setAccessible(true);
        hooks.install(setScreenEffect, chain -> {
            Object self = chain.getThisObject();
            int fps = (int) chain.getArg(1);
            int cookie = (int) chain.getArg(2);
            int chosen = userFps.getInt(self);
            if (cookie != gameCookie.getInt(self) || fps >= chosen) return chain.proceed();
            Object[] args = chain.getArgs().toArray();
            args[1] = chosen;
            return chain.proceed(args);
        });
    }
}
