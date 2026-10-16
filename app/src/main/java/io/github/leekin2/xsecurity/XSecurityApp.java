package io.github.leekin2.xsecurity;

import android.app.Application;
import android.util.Log;
import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;
import java.util.function.Consumer;

/**
 * Holds the LSPosed service for the whole process. XposedServiceHelper hands a service that came
 * before the listener only once and then forgets it, so the listener is registered here, once,
 * and every screen (also a recreated one) reads the kept service.
 */
public final class XSecurityApp extends Application {
    private static volatile XposedService service;
    private static volatile boolean answered;
    private static Consumer<XposedService> watcher;

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(new XposedServiceHelper.OnServiceListener() {
            @Override public void onServiceBind(XposedService value) {
                Log.i(Hooks.TAG, "LSPosed service bound: API " + safeApi(value));
                update(value);
            }
            @Override public void onServiceDied(XposedService value) {
                Log.w(Hooks.TAG, "LSPosed service died");
                if (service == value) update(null);
            }
        });
    }

    private static int safeApi(XposedService value) {
        try { return value.getApiVersion(); } catch (Throwable unavailable) { return -1; }
    }

    private static synchronized void update(XposedService value) {
        service = value;
        answered = true;
        Consumer<XposedService> target = watcher;
        if (target != null) target.accept(value);
    }

    /** The current service (or null) now, and every later change, until {@link #unwatch}. */
    static synchronized void watch(Consumer<XposedService> value) {
        watcher = value;
        if (answered || service != null) value.accept(service);
    }

    static synchronized void unwatch(Consumer<XposedService> value) {
        if (watcher == value) watcher = null;
    }

    static XposedService current() { return service; }
}
