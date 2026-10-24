package io.github.leekin2.xsecurity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.provider.Settings;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

/** Device-list compatibility; retain the ROM's required sensors and its actual enabled state. */
final class CarSicknessHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    CarSicknessHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install(DexTargets targets) throws ReflectiveOperationException {
        if (!Features.enabled(Features.CAR_SICKNESS)) return;
        // A capability hook cannot supply an implementation removed from a port ROM.
        loader.loadClass("com.miui.carsickness.ui.CarSicknessReliefSettingsActivity");
        loader.loadClass("com.miui.carsickness.service.CarSicknessService");
        loader.loadClass("com.miui.carsick.services.AntiCarsickManager");
        Set<Method> support = new LinkedHashSet<>();
        Set<Method> state = new LinkedHashSet<>();
        for (Method method : targets.usingStrings("carsick_legacy_support_v3", "boolean", "is_support_carsick")) {
            if (contextCheck(method)) support.add(method);
        }
        for (Class<?> owner : targets.classesUsing("carsick_utils_v3", "CarSicknessUtils", "pref_car_sickness_support_devices")) {
            // On the inspected HyperOS APK the support method checks a device/cloud list, then
            // linear-acceleration and gyroscope sensors. It does not use is_support_carsick.
            for (Method method : targets.usingStrings("carsick_sensor_support_v3", "boolean", "sensor")) {
                if (method.getDeclaringClass() == owner && contextCheck(method)) support.add(method);
            }
            for (Method method : targets.usingStrings("carsick_enabled_state_v3", "boolean", "settings_car_sickness_mode")) {
                if (method.getDeclaringClass() == owner && contextCheck(method)) state.add(method);
            }
        }
        if (support.isEmpty()) throw new NoSuchMethodException("No verified car-sickness support check");
        for (Method method : support) {
            hooks.install(method, chain -> {
                Object original = chain.proceed();
                if (Boolean.TRUE.equals(original) || !Features.enabled(Features.CAR_SICKNESS)) return original;
                return chain.getArg(0) instanceof Context context && hasRequiredSensors(context);
            });
            hooks.deoptimizeReported(method);
        }
        for (Method method : state) {
            hooks.install(method, chain -> {
                Object original = chain.proceed();
                if (!Features.enabled(Features.CAR_SICKNESS) || !(chain.getArg(0) instanceof Context context)
                        || !hasRequiredSensors(context)) return original;
                // MediaTek builds gate this read behind another device flag. Keep the actual
                // user's stored switch instead of returning true for the enabled-state getter.
                return Settings.System.getInt(context.getContentResolver(), "settings_car_sickness_mode", 0) == 1;
            });
            hooks.deoptimizeReported(method);
        }
        hooks.info("car sickness: verified support checks=" + support.size() + "; required sensors retained");
    }

    static boolean hasRequiredSensors(Context context) {
        Object service = context.getSystemService(Context.SENSOR_SERVICE);
        return service instanceof SensorManager sensors
                && sensors.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
                && sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null;
    }

    private static boolean contextCheck(Method method) {
        return method.getReturnType() == boolean.class && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == Context.class;
    }
}
