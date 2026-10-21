package io.github.leekin2.xsecurity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Extends Xiaomi's own firewall UI; its binder and enterprise checks still enforce the rules. */
final class NetworkControlHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    NetworkControlHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() throws ReflectiveOperationException {
        Class<?> info = loader.loadClass("com.miui.networkassistant.model.AppInfo");
        Field uid = info.getField("uid");
        Field packageName = info.getField("packageName");
        Field system = info.getField("isSystemApp");
        Constructor<?> copy = info.getConstructor(info);
        Class<?> preset = loader.loadClass("com.miui.networkassistant.traffic.statistic.PreSetGroup");
        Method protectedFirewall = preset.getMethod("isPreFirewallWhiteListPackage", String.class);
        Method protectedBackground = preset.getMethod("isPrePolicyPackage", String.class);

        Class<?> details = loader.loadClass("com.miui.networkassistant.ui.fragment.ShowAppDetailFragment");
        Field app = field(details, "mAppInfo");
        Field managed = field(details, "mIsManagedProfileApp");
        Method init = details.getDeclaredMethod("initFirewallData");
        if (init.getReturnType() != void.class) throw new NoSuchMethodException("Unexpected firewall init result");
        Class<?> wlan = loader.loadClass("com.miui.networkassistant.ui.fragment.WlanFirewallFragment");
        Method list = wlan.getDeclaredMethod("getAppList");
        Class<?> base = loader.loadClass("com.miui.networkassistant.ui.fragment.FirewallListFragment");
        Field monitor = field(base, "mAppMonitorWrapper");
        Class<?> wrapper = loader.loadClass("com.miui.networkassistant.service.wrapper.AppMonitorWrapper");
        Method systemList = wrapper.getMethod("getSystemAppList");
        hooks.install(init, chain -> {
            Object self = chain.getThisObject();
            Object original = app.get(self);
            if (managed.getBoolean(self) || !eligible(original, uid, packageName, protectedFirewall, protectedBackground)) {
                return chain.proceed();
            }
            Object localCopy = copy.newInstance(original);
            system.setBoolean(localCopy, false);
            app.set(self, localCopy);
            try { return chain.proceed(); }
            finally { app.set(self, original); }
        });

        // The WLAN page normally shows only non-system apps. Add eligible app UIDs,
        // without modifying AppMonitorWrapper's shared lists or identities.
        hooks.install(list, chain -> {
            Object result = chain.proceed();
            if (!(result instanceof List<?> existing)) return result;
            Object service = monitor.get(chain.getThisObject());
            if (service == null) return result;
            Object extra = systemList.invoke(service);
            if (!(extra instanceof List<?> apps)) return result;
            ArrayList<Object> combined = new ArrayList<>(existing);
            Set<String> seen = new HashSet<>();
            for (Object item : existing) seen.add(packageName.get(item) + ":" + uid.getInt(item));
            for (Object item : apps) {
                if (eligible(item, uid, packageName, protectedFirewall, protectedBackground)
                        && seen.add(packageName.get(item) + ":" + uid.getInt(item))) combined.add(item);
            }
            return combined;
        });
    }

    private static boolean eligible(Object app, Field uid, Field name, Method firewall, Method background)
            throws ReflectiveOperationException {
        if (app == null) return false;
        int id = uid.getInt(app);
        // Never expose platform/shared service UIDs or managed-profile apps as ordinary apps.
        if (id < 10000 || id >= 100000) return false;
        Object value = name.get(app);
        if (value == null || value.toString().isEmpty()) return false;
        String pkg = value.toString();
        return !Boolean.TRUE.equals(firewall.invoke(null, pkg)) && !Boolean.TRUE.equals(background.invoke(null, pkg));
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
