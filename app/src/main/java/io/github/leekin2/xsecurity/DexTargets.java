package io.github.leekin2.xsecurity;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.luckypray.dexkit.DexKitBridge;
import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;
import org.luckypray.dexkit.wrap.DexMethod;

/**
 * The Security app renames its classes in every build, so its checks are found by the text they
 * use. A scan of the 60 MB APK takes a moment; the results are kept per APK build in the app's
 * data folder, so only the first start after an update scans.
 */
final class DexTargets implements AutoCloseable {
    private static final int CACHE_SCHEMA = 1;
    private static volatile boolean nativeLoaded;
    private final String apkPath;
    private final ClassLoader loader;
    private final File cache;
    private final Properties stored = new Properties();
    private DexKitBridge bridge;
    private boolean dirty;

    DexTargets(ClassLoader loader, String apkPath, String dataDir) throws IOException {
        if (apkPath == null || apkPath.isEmpty()) throw new IOException("Security app APK unavailable");
        this.apkPath = apkPath;
        this.loader = loader;
        cache = dataDir == null ? null : new File(dataDir, "xsecurity_targets.cache");
        File apk = new File(apkPath);
        String key = CACHE_SCHEMA + ":" + apkPath + ":" + apk.lastModified() + ":" + apk.length();
        if (cache != null && cache.isFile() && cache.length() < 262144) {
            try (var input = Files.newInputStream(cache.toPath())) { stored.load(input); }
            catch (IOException | IllegalArgumentException invalid) { stored.clear(); }
        }
        if (!key.equals(stored.getProperty("apk"))) stored.clear();
        stored.setProperty("apk", key);
    }

    /** Methods with this return type whose code uses all of {@code strings}. */
    List<Method> usingStrings(String name, String returnType, String... strings) throws ReflectiveOperationException {
        return find(name, () -> MethodMatcher.create().returnType(returnType).usingStrings(List.of(strings)));
    }

    /** Every non-abstract method with this name, in any class (interface proxies and stubs). */
    List<Method> named(String name, String methodName) throws ReflectiveOperationException {
        return find(name, () -> MethodMatcher.create().name(methodName));
    }

    /** Methods that {@code caller}'s code calls directly. */
    List<Method> invokedBy(String name, Method caller) throws ReflectiveOperationException {
        String saved = stored.getProperty(name);
        if (saved != null) {
            try {
                List<Method> methods = new ArrayList<>();
                if (!saved.isEmpty()) for (String d : saved.split("\n")) methods.add(new DexMethod(d).getMethodInstance(loader));
                return methods;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError stale) {
                stored.remove(name);
                dirty = true;
            }
        }
        if (bridge == null) {
            loadNative();
            bridge = DexKitBridge.create(apkPath);
        }
        List<Method> methods = new ArrayList<>();
        List<String> descriptors = new ArrayList<>();
        MethodData data = bridge.getMethodData(new DexMethod(caller).serialize());
        if (data != null) {
            for (MethodData called : data.getInvokes()) {
                if (!called.isMethod()) continue;
                try {
                    Method method = called.getMethodInstance(loader);
                    methods.add(method);
                    descriptors.add(new DexMethod(method).serialize());
                } catch (ReflectiveOperationException | RuntimeException | LinkageError outside) {
                    // A framework or library method: not one of the app's own.
                }
            }
        }
        stored.setProperty(name, String.join("\n", descriptors));
        dirty = true;
        return methods;
    }

    interface Query { MethodMatcher build(); }

    /** Methods matching any DexKit query (for matchers the helpers above do not cover). */
    List<Method> matching(String name, Query query) throws ReflectiveOperationException {
        return find(name, query);
    }

    /** Classes whose code uses all of {@code strings}. */
    List<Class<?>> classesUsing(String name, String... strings) throws ReflectiveOperationException {
        String saved = stored.getProperty("class:" + name);
        List<Class<?>> classes = new ArrayList<>();
        if (saved != null) {
            try {
                if (!saved.isEmpty()) for (String type : saved.split("\n")) classes.add(Class.forName(type, false, loader));
                return classes;
            } catch (ClassNotFoundException | LinkageError stale) {
                classes.clear();
                stored.remove("class:" + name);
            }
        }
        if (bridge == null) {
            loadNative();
            bridge = DexKitBridge.create(apkPath);
        }
        List<String> names = new ArrayList<>();
        for (ClassData data : bridge.findClass(FindClass.create().matcher(ClassMatcher.create().usingStrings(List.of(strings))))) {
            classes.add(data.getInstance(loader));
            names.add(data.getName());
        }
        stored.setProperty("class:" + name, String.join("\n", names));
        dirty = true;
        return classes;
    }

    private List<Method> find(String name, Query query) throws ReflectiveOperationException {
        String saved = stored.getProperty(name);
        if (saved != null) {
            if (saved.isEmpty()) return List.of();
            try {
                List<Method> methods = new ArrayList<>();
                for (String descriptor : saved.split("\n")) methods.add(new DexMethod(descriptor).getMethodInstance(loader));
                return methods;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError stale) {
                stored.remove(name);
                dirty = true;
            }
        }
        if (bridge == null) {
            loadNative();
            bridge = DexKitBridge.create(apkPath);
        }
        List<Method> methods = new ArrayList<>();
        List<String> descriptors = new ArrayList<>();
        for (MethodData data : bridge.findMethod(FindMethod.create().matcher(query.build()))) {
            if (!data.isMethod() || java.lang.reflect.Modifier.isAbstract(data.getModifiers())) continue;
            Method method = data.getMethodInstance(loader);
            methods.add(method);
            descriptors.add(new DexMethod(method).serialize());
        }
        stored.setProperty(name, String.join("\n", descriptors));
        dirty = true;
        return methods;
    }

    private static void loadNative() {
        if (nativeLoaded) return;
        synchronized (DexTargets.class) {
            if (!nativeLoaded) { System.loadLibrary("dexkit"); nativeLoaded = true; }
        }
    }

    @Override public void close() {
        try {
            if (bridge != null) bridge.close();
        } finally {
            if (dirty && cache != null) {
                File temporary = null;
                try {
                    temporary = File.createTempFile("xsecurity_targets", ".tmp", cache.getParentFile());
                    try (var output = Files.newOutputStream(temporary.toPath())) { stored.store(output, "XSecurity targets"); }
                    Files.move(temporary.toPath(), cache.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException | RuntimeException unavailable) {
                    // An unwritable cache only means the next start scans again.
                } finally {
                    if (temporary != null) temporary.delete();
                }
            }
        }
    }
}
