package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;

/**
 * System gallery (com.miui.gallery): the China ROM hides the Google Photos backup server that the
 * global ROM offers. This makes the gallery behave as a global build so "Google" appears under the
 * backup server options.
 *
 * Ported from HyperCeiler (AGPL-3.0) rule gallery/ChangeBackupServer (its Google path), rewritten
 * against this module's hook API. Class/method names are resolved per firmware; a missing target is
 * logged and stock behaviour is kept. See NOTICE.
 */
final class GalleryHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    GalleryHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.GALLERY_GOOGLE_BACKUP)) return;
        boolean done = forceTrue("com.miui.gallery.transfer.GoogleSyncHelper", "isCloudServiceOffLine")
            || forceTrue("com.miui.gallery.util.BuildUtil", "isGlobal");
        if (done) hooks.info("gallery Google backup ready");
        else hooks.error("gallery Google backup unavailable; stock behaviour kept",
            new NoSuchMethodException("GoogleSyncHelper.isCloudServiceOffLine / BuildUtil.isGlobal"));
    }

    /** Forces a no-argument boolean method to return true; returns false if the target is absent. */
    private boolean forceTrue(String className, String methodName) {
        try {
            Class<?> clazz = loader.loadClass(className);
            Method method = clazz.getDeclaredMethod(methodName);
            hooks.constant(method, true);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
