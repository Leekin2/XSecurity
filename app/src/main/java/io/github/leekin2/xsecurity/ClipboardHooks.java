package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Background clipboard access for apps the user picked. Android only lets the focused app (or the
 * default keyboard) read the clipboard; this lets the packages on the user's own list read it too.
 * Every other app keeps the stock rule. The list is read on each check, so edits apply at once.
 *
 * Ported from HyperCeiler (AGPL-3.0) rule systemframework/others/ClipboardWhitelist, rewritten
 * against this module's hook API. See NOTICE.
 */
final class ClipboardHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    ClipboardHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.CLIPBOARD_WHITELIST)) return;
        try { hookAccess(); hooks.info("clipboard whitelist ready"); }
        catch (Throwable t) { hooks.error("clipboard whitelist unavailable; stock rule kept", t); }
    }

    private void hookAccess() throws ReflectiveOperationException {
        Class<?> service = loader.loadClass("com.android.server.clipboard.ClipboardService");
        int hooked = 0;
        for (Method method : service.getDeclaredMethods()) {
            if (!method.getName().equals("clipboardAccessAllowed")) continue;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                if (Boolean.TRUE.equals(result)) return result;
                // Arguments: (op, callingPackage, uid, userId, ...); the package is the first String.
                for (Object arg : chain.getArgs()) {
                    if (!(arg instanceof String)) continue;
                    Set<String> allowed = Features.stringSet(Features.CLIPBOARD_APPS);
                    return allowed.contains(arg) ? Boolean.TRUE : result;
                }
                return result;
            });
            hooked++;
        }
        if (hooked == 0) throw new NoSuchMethodException("ClipboardService.clipboardAccessAllowed");
    }
}
