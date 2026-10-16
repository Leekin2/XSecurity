package io.github.leekin2.xsecurity;

import android.util.Log;
import io.github.libxposed.api.XposedInterface;
import io.github.libxposed.api.XposedModule;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

/** Installs hooks in protective mode and logs to both logcat and the LSPosed log. */
final class Hooks {
    static final String TAG = "XSecurity";
    private final XposedModule module;

    Hooks(XposedModule module) { this.module = module; }

    void install(Executable method, XposedInterface.Hooker hooker) {
        method.setAccessible(true);
        module.hook(method).setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE).intercept(hooker);
    }

    /** The method no longer runs and always answers {@code value} (a "supported" check turned on). */
    void constant(Method method, Object value) {
        install(method, chain -> value);
    }

    void info(String message) {
        Log.i(TAG, message);
        module.log(Log.INFO, TAG, message);
    }

    void error(String message, Throwable error) {
        Log.e(TAG, message, error);
        module.log(Log.ERROR, TAG, message, error);
    }
}
