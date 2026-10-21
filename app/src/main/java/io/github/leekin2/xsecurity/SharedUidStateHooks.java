package io.github.leekin2.xsecurity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Keeps non-system shared-UID signing state consistent with its actual members. */
final class SharedUidStateHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    SharedUidStateHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() throws ReflectiveOperationException {
        Class<?> shared = loader.loadClass("com.android.server.pm.SharedUserSetting");
        Field flags = field(shared, "uidFlags");
        Field packages = field(shared, "mPackages", "packages");
        int count = 0;
        for (Method method : shared.getDeclaredMethods()) {
            if (!(method.getName().equals("addPackage") || method.getName().equals("removePackage")) || method.getParameterCount() != 1) continue;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                if (!Boolean.FALSE.equals(result) && (flags.getInt(chain.getThisObject()) & 1) == 0) {
                    try {
                        CorePatchHooks.originalSigning(() -> {
                            repair(chain.getThisObject(), packages);
                            return null;
                        });
                    } catch (Throwable unsupported) {
                        hooks.error("Core Patch shared UID state update unavailable; original member operation retained", unsupported);
                    }
                }
                return result;
            });
            count++;
        }
        if (count == 0) throw new NoSuchMethodException("SharedUserSetting.addPackage/removePackage");
    }

    private void repair(Object shared, Field packages) throws ReflectiveOperationException {
        Object storage = packages.get(shared);
        if (!(storage instanceof Iterable<?>)) storage = field(storage.getClass(), "mStorage").get(storage);
        if (!(storage instanceof Iterable<?> members)) return;
        Object current = signing(shared);
        List<Object> signers = new ArrayList<>();
        for (Object member : members) {
            Object candidate = signing(member);
            if (candidate == null) continue;
            if (current != null && (compatible(candidate, current) || compatible(current, candidate))) return;
            signers.add(candidate);
        }
        if (signers.isEmpty()) return;
        Object merged = signers.get(0);
        for (int i = 1; i < signers.size(); i++) {
            try {
                Method merge = merged.getClass().getMethod("mergeLineageWith", merged.getClass(), int.class);
                Object combined = merge.invoke(merged, signers.get(i), 2);
                if (combined != null) merged = combined;
            } catch (java.lang.reflect.InvocationTargetException incompatible) {
                // Separate test signers do not necessarily have a common lineage.
            }
        }
        Object signatures = field(shared.getClass(), "signatures", "mSignatures").get(shared);
        field(signatures.getClass(), "mSigningDetails").set(signatures, merged);
    }

    private static Object signing(Object setting) throws ReflectiveOperationException {
        Object signatures = field(setting.getClass(), "signatures", "mSignatures").get(setting);
        return signatures == null ? null : field(signatures.getClass(), "mSigningDetails").get(signatures);
    }

    private static boolean compatible(Object first, Object second) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(first.getClass().getMethod("checkCapability", first.getClass(), int.class).invoke(first, second, 0));
    }

    private static Field field(Class<?> type, String... names) throws NoSuchFieldException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (String name : names) {
                try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
                catch (NoSuchFieldException ignored) { }
            }
        }
        throw new NoSuchFieldException(type.getName() + ": " + String.join("/", names));
    }
}
