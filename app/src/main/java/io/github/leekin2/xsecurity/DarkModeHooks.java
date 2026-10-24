package io.github.leekin2.xsecurity;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dark mode "Individual apps" list: the China ROM shows almost no apps there. This builds the
 * list with ordinary user apps, including entries hidden by the ROM's local/cloud configuration.
 *
 * Ported from HyperCeiler (AGPL-3.0) rule systemframework/display/AllDarkMode (itself from
 * SetoHook by SetoSkins). Unlike the original approach, no global build flag is changed:
 * complete the ROM's returned list using its own row builder and per-user saved state. See NOTICE.
 */
final class DarkModeHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final AtomicBoolean failureReported = new AtomicBoolean();

    DarkModeHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.DARK_MODE_ALL_APPS)) return;
        try { hookList(); hooks.info("dark-mode app list completion ready"); }
        catch (Throwable t) { hooks.error("dark-mode app list unavailable; stock list kept", t); }
    }

    private void hookList() throws ReflectiveOperationException {
        Class<?> manager = loader.loadClass("com.android.server.ForceDarkAppListManager");
        Class<?> settings = loader.loadClass("com.android.server.DarkModeAppSettingsInfo");
        Class<?> data = loader.loadClass("com.miui.darkmode.DarkModeAppData");
        Class<?> detail = loader.loadClass("com.miui.darkmode.DarkModeAppDetailInfo");
        Method list = method(manager, "getDarkModeAppList", long.class, int.class);
        Method installed = method(manager, "getInstalledPkgList");
        Method appInfo = method(manager, "getAppInfo", settings, ApplicationInfo.class, int.class);
        Method read = method(data, "getDarkModeAppDetailInfoList");
        Method write = method(data, "setDarkModeAppDetailInfoList", List.class);
        Method packageName = method(detail, "getPkgName");
        Field cached = field(manager, "mDarkModeAppData");
        Field configuration = field(manager, "mForceDarkAppSettings");
        Constructor<?> defaults = settings.getDeclaredConstructor();
        defaults.setAccessible(true);

        hooks.install(list, chain -> {
            Object original = chain.proceed();
            if (!Features.enabled(Features.DARK_MODE_ALL_APPS)) return original;
            try {
                Object self = chain.getThisObject();
                // A matching Settings disk-cache timestamp makes the stock method return null.
                Object result = original != null ? original : cached.get(self);
                if (result == null) return original;
                Object rows = read.invoke(result);
                List<Object> complete = new ArrayList<>();
                if (rows instanceof List<?>) complete.addAll((List<?>) rows);
                Set<String> seen = new HashSet<>();
                for (Object row : complete) {
                    if (row != null) seen.add((String) packageName.invoke(row));
                }
                Object packages = installed.invoke(self);
                if (!(packages instanceof List<?>)) return original;
                Object config = configuration.get(self);
                Map<?, ?> entries = config instanceof Map<?, ?> ? (Map<?, ?>) config : null;
                int userId = (Integer) chain.getArg(1);
                for (Object item : (List<?>) packages) {
                    if (!(item instanceof PackageInfo)) continue;
                    ApplicationInfo info = ((PackageInfo) item).applicationInfo;
                    if (!eligible(info) || seen.contains(info.packageName)) continue;
                    Object entry = entries == null ? null : entries.get(info.packageName);
                    if (entry == null) entry = defaults.newInstance();
                    Object row = appInfo.invoke(self, entry, info, userId);
                    if (row != null) {
                        complete.add(row);
                        seen.add(info.packageName);
                    }
                }
                // Commit only after all added rows were built successfully.
                write.invoke(result, complete);
                return result;
            } catch (Throwable failure) {
                if (failureReported.compareAndSet(false, true)) {
                    hooks.error("dark-mode list completion failed; stock result kept", failure);
                }
                return original;
            }
        });
        hooks.deoptimizeReported(list);
    }

    private static boolean eligible(ApplicationInfo info) {
        return info != null && info.packageName != null
                && (info.flags & ApplicationInfo.FLAG_SYSTEM) == 0
                && info.uid >= 10000 && info.uid % 100000 >= 10000;
    }

    private static Method method(Class<?> owner, String name, Class<?>... args)
            throws NoSuchMethodException {
        Method result = owner.getDeclaredMethod(name, args);
        result.setAccessible(true);
        return result;
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field result = owner.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
}
