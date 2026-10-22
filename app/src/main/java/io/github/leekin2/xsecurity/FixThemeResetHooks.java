package io.github.leekin2.xsecurity;

import android.system.Os;

import java.io.File;

/**
 * Keeps a user-applied theme from reverting to default. HyperOS periodically rewrites
 * {@code /data/system/theme} to restore the stock theme; removing the directory's read bit for
 * group/other stops that revert, so a third-party theme stays applied. This is the same change the
 * "Fixed theme reset" Magisk module makes ({@code chmod 731 /data/system/theme}), run here from
 * system_server at boot so no separate root module is needed.
 *
 * Runs only when the user turns the switch on; a failure (for example an SELinux denial) is logged
 * and the stock behaviour is kept.
 */
final class FixThemeResetHooks {
    private static final String THEME_DIR = "/data/system/theme";
    private static final int MODE = 0731; // owner rwx, group wx, other x: no read for group/other.

    private final Hooks hooks;

    FixThemeResetHooks(Hooks hooks) { this.hooks = hooks; }

    void install() {
        // The directory may not exist yet this early in boot, so apply now and retry a few times
        // while the theme service comes up. A daemon thread keeps system_server startup unblocked.
        Thread worker = new Thread(() -> {
            boolean applied = false;
            for (long delay : new long[]{0, 5000, 20000, 60000}) {
                if (delay > 0) {
                    try { Thread.sleep(delay); } catch (InterruptedException stop) { return; }
                }
                if (apply()) applied = true;
            }
            if (applied) hooks.info("fix theme reset: " + THEME_DIR + " locked (0731)");
            else hooks.error("fix theme reset unavailable; stock behaviour kept",
                    new java.io.IOException("could not chmod " + THEME_DIR));
        }, "xsecurity-theme-lock");
        worker.setDaemon(true);
        worker.start();
    }

    private boolean apply() {
        try {
            if (!new File(THEME_DIR).exists()) return false;
            Os.chmod(THEME_DIR, MODE);
            return true;
        } catch (Throwable denied) {
            return false;
        }
    }
}
