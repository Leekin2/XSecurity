package io.github.leekin2.xsecurity;

import android.annotation.SuppressLint;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.view.SurfaceControl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedInterface;

/**
 * Lets the owner screenshot and screen-record any app, for every app at once.
 *
 * Runs in system_server (see XSecurityModule.onSystemServerStarting). It turns
 * off every "secure window" gate the framework and HyperOS use to block
 * capture, so FLAG_SECURE no longer stops a screenshot anywhere.
 *
 * Hook set ported from LSPosed/DisableFlagSecure (GPL-3.0) and trimmed to the
 * targets that matter on this ROM family (HyperOS, Android 14..current):
 *   - com.android.server.wm.WindowState#isSecureLocked
 *   - com.android.server.wm.WindowManagerServiceImpl#notAllowCaptureDisplay (HyperOS)
 *   - ScreenCapture native capture args (secure content allowed)
 *   - ScreenshotHardwareBuffer#containsSecureLayers
 *   - DisplayControl / VirtualDisplayAdapter (secure virtual displays / cast)
 * Every hook is isolated in its own try/catch so a target missing on one ROM
 * version never breaks the others.
 */
final class FlagSecureHooks {
    private final Hooks hooks;
    private final ClassLoader cl;
    private int installedGroups;

    FlagSecureHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.cl = loader;
    }

    private interface Step { void run() throws Throwable; }

    void install() {
        try { deoptimize(); } catch (Throwable t) { hooks.error("flag secure: deoptimize skipped", t); }
        step("WindowState.isSecureLocked", this::hookWindowState);
        if (Build.VERSION.SDK_INT >= 35) step("screen recording detection", this::hookRecordingDetection);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            step("screenshot detection", this::hookScreenshotDetection);
            step("HyperOS notAllowCaptureDisplay", this::hookHyperOS);
        }
        step("ScreenCapture", this::hookScreenCapture);
        step("ScreenshotHardwareBuffer", this::hookScreenshotHardwareBuffer);
        step("DisplayControl", this::hookDisplayControl);
        step("VirtualDisplayAdapter", this::hookVirtualDisplayAdapter);
        hooks.info("flag secure capture targets installed: " + installedGroups + " groups; device test required");
    }

    private void step(String name, Step s) {
        try { s.run(); installedGroups++; }
        catch (Throwable t) {
            hooks.error("flag secure: " + name + " unavailable", t);
        }
    }

    // com.android.server.wm.WindowState#isSecureLocked -> false (except the window's own surface setup).
    private void hookWindowState() throws Throwable {
        Class<?> ws = cl.loadClass("com.android.server.wm.WindowState");
        final ClassLoader serverCl = ws.getClassLoader();
        Method m = ws.getDeclaredMethod("isSecureLocked");
        hooks.install(m, chain -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                boolean keep = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                        .walk(frames -> frames.anyMatch(f ->
                                f.getDeclaringClass() != null &&
                                f.getDeclaringClass().getClassLoader() == serverCl &&
                                (f.getMethodName().equals("setInitialSurfaceControlProperties") ||
                                        f.getMethodName().equals("createSurfaceLocked"))));
                if (keep) return chain.proceed();
            }
            return false;
        });
    }

    // Xiaomi HyperOS gate.
    private void hookRecordingDetection() throws Throwable {
        Class<?> owner = cl.loadClass("com.android.server.wm.WindowManagerService");
        Class<?> callback = cl.loadClass("android.window.IScreenRecordingCallback");
        Method target = owner.getDeclaredMethod("registerScreenRecordingCallback", callback);
        if (target.getReturnType() != boolean.class) throw new NoSuchMethodException("Recording callback result");
        hooks.install(target, chain -> false);
    }

    private void hookScreenshotDetection() throws Throwable {
        Class<?> owner = cl.loadClass("com.android.server.wm.ActivityTaskManagerService");
        Method target = owner.getDeclaredMethod("registerScreenCaptureObserver", cl.loadClass("android.os.IBinder"),
                cl.loadClass("android.app.IScreenCaptureObserver"));
        if (target.getReturnType() != void.class) throw new NoSuchMethodException("Capture observer result");
        hooks.install(target, chain -> null);
    }

    private void hookHyperOS() throws Throwable {
        Class<?> impl = cl.loadClass("com.android.server.wm.WindowManagerServiceImpl");
        hookAllByName(impl, "notAllowCaptureDisplay", chain -> false);
    }

    // Allow secure layers in the native capture args.
    @SuppressLint("BlockedPrivateApi") // Runs inside system_server under LSPosed, not the ordinary app process.
    private void hookScreenCapture() throws Throwable {
        Class<?> scClazz;
        Class<?> argsClazz;
        try {
            scClazz = cl.loadClass("android.window.ScreenCaptureInternal");
            argsClazz = cl.loadClass("android.window.ScreenCaptureInternal$CaptureArgs");
        } catch (ClassNotFoundException e1) {
            try {
                scClazz = cl.loadClass("android.window.ScreenCapture");
                argsClazz = cl.loadClass("android.window.ScreenCapture$CaptureArgs");
            } catch (ClassNotFoundException e2) {
                scClazz = SurfaceControl.class;
                argsClazz = cl.loadClass("android.view.SurfaceControl$CaptureArgs");
            }
        }
        final Field field;
        final boolean intPolicy;
        Field tmp;
        boolean asInt;
        try { tmp = argsClazz.getDeclaredField("mSecureContentPolicy"); asInt = true; }
        catch (NoSuchFieldException e) { tmp = argsClazz.getDeclaredField("mCaptureSecureLayers"); asInt = false; }
        tmp.setAccessible(true);
        field = tmp; intPolicy = asInt;
        XposedInterface.Hooker h = chain -> {
            Object captureArgs = chain.getArg(0);
            try { if (intPolicy) field.set(captureArgs, 1); else field.set(captureArgs, true); }
            catch (Throwable ignored) { }
            return chain.proceed();
        };
        hookAllByName(scClazz, "nativeCaptureDisplay", h);
        hookAllByName(scClazz, "nativeCaptureLayers", h);
    }

    @SuppressLint("BlockedPrivateApi") // Framework hook installed by LSPosed inside system_server.
    private void hookScreenshotHardwareBuffer() throws Throwable {
        Class<?> c;
        try { c = cl.loadClass("android.window.ScreenCaptureInternal$ScreenshotHardwareBuffer"); }
        catch (ClassNotFoundException internalMissing) {
            try { c = cl.loadClass("android.window.ScreenCapture$ScreenshotHardwareBuffer"); }
            catch (ClassNotFoundException captureMissing) {
                c = cl.loadClass("android.view.SurfaceControl$ScreenshotHardwareBuffer");
            }
        }
        Method m = c.getDeclaredMethod("containsSecureLayers");
        hooks.install(m, chain -> false);
    }

    private void hookDisplayControl() throws Throwable {
        Class<?> dc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ? cl.loadClass("com.android.server.display.DisplayControl")
                : SurfaceControl.class;
        String name = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM
                ? "createVirtualDisplay" : "createDisplay";
        Method m = dc.getDeclaredMethod(name, String.class, boolean.class);
        hooks.install(m, chain -> {
            Object[] args = chain.getArgs().toArray();
            args[1] = true;
            return chain.proceed(args);
        });
    }

    private void hookVirtualDisplayAdapter() throws Throwable {
        Class<?> vda = cl.loadClass("com.android.server.display.VirtualDisplayAdapter");
        hookAllByName(vda, "createVirtualDisplayLocked", chain -> {
            int caller = (int) chain.getArg(2);
            if (caller >= 10000 && chain.getArg(1) == null) return chain.proceed(); // not OS, not MediaProjection
            for (int i = 3; i < chain.getArgs().size(); i++) {
                Object arg = chain.getArg(i);
                if (arg instanceof Integer) {
                    int flags = (Integer) arg | DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE;
                    Object[] args = chain.getArgs().toArray();
                    args[i] = flags;
                    return chain.proceed(args);
                }
            }
            return chain.proceed();
        });
    }

    private void deoptimize() throws Throwable {
        deopt("com.android.server.wm.WindowStateAnimator", "createSurfaceLocked");
        deopt("com.android.server.wm.WindowManagerService", "relayoutWindow");
        for (int i = 0; i < 20; i++) {
            deoptQuiet("com.android.server.wm.RootWindowContainer$$ExternalSyntheticLambda" + i, "accept");
            deoptQuiet("com.android.server.wm.DisplayContent$" + i, "test");
        }
    }

    private void deopt(String className, String methodName) throws Throwable {
        Class<?> c = cl.loadClass(className);
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(methodName)) hooks.deoptimize(m);
        }
    }

    private void deoptQuiet(String className, String methodName) {
        try { deopt(className, methodName); } catch (Throwable ignored) { }
    }

    private void hookAllByName(Class<?> clazz, String name, XposedInterface.Hooker hooker) throws NoSuchMethodException {
        int count = 0;
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name)) { hooks.install(m, hooker); count++; }
        }
        if (count == 0) throw new NoSuchMethodException(clazz.getName() + "." + name);
    }
}
