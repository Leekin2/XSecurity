package io.github.leekin2.xsecurity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Retains the installed system SystemUI shared-user identity in parsed update metadata.
 * Uses the boot-loaded PackageManager settings and the package's own mutable setter;
 * does not suppress UID exceptions, rewrite APK files, or change installed UID ownership.
 * The module must remain active when a differently-declared update is reparsed at boot.
 */
final class SystemUiUidHooks {
    private static final String SYSTEM_UI = "com.android.systemui";
    private final Hooks hooks;
    private final ClassLoader loader;
    private final AtomicInteger reports = new AtomicInteger();
    private volatile String installedSharedUid;
    private volatile boolean installed;

    SystemUiUidHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() throws ReflectiveOperationException {
        if (!Features.enabled(Features.CP_SYSTEM_UI_UID)) return;
        Class<?> settings = loader.loadClass("com.android.server.pm.Settings");
        Class<?> setting = loader.loadClass("com.android.server.pm.PackageSetting");
        Class<?> shared = loader.loadClass("com.android.server.pm.SharedUserSetting");
        Class<?> pkg = packageClass();
        Class<?> parser = parserClass();
        Method find = method(settings, "getPackageLPr", String.class);
        Method sharedUser = method(settings, "getSharedUserSettingLPr", String.class);
        Method system = method(setting, "isSystem");
        Field uidName = shared.getDeclaredField("name");
        if (uidName.getType() != String.class || Modifier.isStatic(uidName.getModifiers())) {
            throw new NoSuchFieldException("SharedUserSetting.name:String");
        }
        uidName.setAccessible(true);
        Method getName = method(pkg, "getPackageName");
        Method getPath = method(pkg, "getPath");
        Method getUid = method(pkg, "getSharedUserId");
        Method leaving = method(pkg, "isLeavingSharedUser");
        Method setUid = method(pkg, "setSharedUserId", String.class);
        if (getName.getReturnType() != String.class || getPath.getReturnType() != String.class
                || getUid.getReturnType() != String.class || leaving.getReturnType() != boolean.class
                || Modifier.isStatic(setUid.getModifiers())) throw new NoSuchMethodException("Unexpected parsed-package API");
        List<Method> reads = new ArrayList<>(), parses = new ArrayList<>();
        for (Method target : settings.getDeclaredMethods()) {
            if (target.getName().equals("readLPw") && !Modifier.isStatic(target.getModifiers())
                    && target.getReturnType() == boolean.class) reads.add(target);
        }
        for (Method target : parser.getDeclaredMethods()) {
            if (target.getName().equals("parsePackage") && !Modifier.isAbstract(target.getModifiers())
                    && target.getReturnType() != void.class && !target.getReturnType().isPrimitive()) parses.add(target);
        }
        if (reads.isEmpty() || parses.isEmpty()) throw new NoSuchMethodException("Settings.readLPw / PackageParser2.parsePackage");

        for (Method read : reads) {
            hooks.install(read, chain -> {
                installedSharedUid = null;
                Object result = chain.proceed();
                if (!installed || !Boolean.TRUE.equals(result) || !Features.enabled(Features.CP_SYSTEM_UI_UID)) return result;
                try {
                    Object old = find.invoke(chain.getThisObject(), SYSTEM_UI);
                    if (old != null && Boolean.TRUE.equals(system.invoke(old))) {
                        Object user = sharedUser.invoke(chain.getThisObject(), SYSTEM_UI);
                        Object name = user == null ? null : uidName.get(user);
                        if (name instanceof String text && !text.isBlank()) installedSharedUid = text;
                    }
                    hooks.info("SystemUI UID preservation: boot identity available=" + (installedSharedUid != null));
                } catch (Throwable failure) {
                    hooks.error("SystemUI installed identity unavailable; original metadata retained", failure);
                }
                return result;
            });
            hooks.deoptimizeReported(read);
        }
        hooks.install(setUid, chain -> {
            String incoming = chain.getArg(0) instanceof String text ? text : null;
            String keep = replacement(chain.getThisObject(), incoming, getName, getPath, leaving);
            if (keep == null) return chain.proceed();
            Object[] args = chain.getArgs().toArray();
            args[0] = keep;
            Object result = chain.proceed(args);
            if (reports.getAndIncrement() < 16) hooks.info("SystemUI parse setter: installed shared UID retained");
            return result;
        });
        hooks.deoptimizeReported(setUid);
        for (Method parse : parses) {
            hooks.install(parse, chain -> {
                Object result = chain.proceed();
                if (!pkg.isInstance(result)) return result;
                try {
                    String incoming = (String) getUid.invoke(result);
                    String keep = replacement(result, incoming, getName, getPath, leaving);
                    if (keep != null) {
                        // Covers cached Parcel results, which bypass the manifest setter.
                        setUid.invoke(result, keep);
                        if (!keep.equals(getUid.invoke(result))) throw new IllegalStateException("UID setter did not retain identity");
                        if (reports.getAndIncrement() < 16) hooks.info("SystemUI parsed update: installed shared UID retained");
                    }
                } catch (Throwable failure) {
                    hooks.error("SystemUI update normalization unavailable; installation checks retained", failure);
                }
                return result;
            });
            hooks.deoptimizeReported(parse);
        }
        installed = true;
        hooks.info("SystemUI UID preservation: parser and boot-settings hooks ready");
    }

    private String replacement(Object pkg, String incoming, Method getName, Method getPath, Method leaving) {
        String canonical = installedSharedUid;
        if (!installed || canonical == null || incoming == null || incoming.isBlank()
                || canonical.equals(incoming) || !Features.enabled(Features.CP_SYSTEM_UI_UID)) return null;
        try {
            if (!SYSTEM_UI.equals(getName.invoke(pkg)) || Boolean.TRUE.equals(leaving.invoke(pkg))) return null;
            String path = (String) getPath.invoke(pkg);
            return path != null && path.startsWith("/data/app/") ? canonical : null;
        } catch (ReflectiveOperationException failure) {
            return null;
        }
    }

    private Class<?> packageClass() throws ClassNotFoundException {
        for (String name : new String[]{"com.android.internal.pm.parsing.pkg.PackageImpl", "com.android.server.pm.parsing.pkg.PackageImpl"}) {
            try { return loader.loadClass(name); } catch (ClassNotFoundException absent) { }
        }
        throw new ClassNotFoundException("Parsed PackageImpl");
    }

    private Class<?> parserClass() throws ClassNotFoundException {
        for (String name : new String[]{"com.android.internal.pm.parsing.PackageParser2", "com.android.server.pm.parsing.PackageParser2"}) {
            try { return loader.loadClass(name); } catch (ClassNotFoundException absent) { }
        }
        throw new ClassNotFoundException("PackageParser2");
    }

    private static Method method(Class<?> owner, String name, Class<?>... args) throws NoSuchMethodException {
        Method target;
        try { target = owner.getMethod(name, args); }
        catch (NoSuchMethodException nonPublic) { target = owner.getDeclaredMethod(name, args); }
        target.setAccessible(true);
        return target;
    }
}
