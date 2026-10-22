package io.github.leekin2.xsecurity;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows the persistent update check only while this thread prepares an APK.
 * Target names cross-checked against HyperCeiler's DisablePersistent (AGPL-3.0;
 * Copyright 2023-2026 HyperCeiler Contributions); see NOTICE.
 */
final class PersistentUpdateHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final ThreadLocal<Integer> preparing = new ThreadLocal<>();
    private final ThreadLocal<Attempt> currentAttempt = new ThreadLocal<>();
    private final AtomicInteger sequence = new AtomicInteger();
    private volatile boolean installed;

    /** Counts only calls on the preparing thread; no APK contents or paths are recorded. */
    private static final class Attempt {
        final int id;
        int checks;
        int overrides;
        Attempt(int id) { this.id = id; }
        boolean reportable() { return id > 0 && id <= 32; }
    }

    PersistentUpdateHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    void install() throws ReflectiveOperationException {
        Class<?> helper = loader.loadClass("com.android.server.pm.InstallPackageHelper");
        List<Method> preparation = new ArrayList<>();
        for (Method method : helper.getDeclaredMethods()) {
            // Android 16+ renamed preparePackageLI to preparePackage. Resolve the
            // actual methods, since vendor ROMs can backport framework changes.
            if ((method.getName().equals("preparePackage") || method.getName().equals("preparePackageLI"))
                    && !Modifier.isAbstract(method.getModifiers())) preparation.add(method);
        }
        if (preparation.isEmpty()) {
            throw new NoSuchMethodException(helper.getName() + ".preparePackage / preparePackageLI");
        }

        // Resolve actual methods instead of choosing by SDK: vendor ROMs can move them.
        Set<Method> checks = new LinkedHashSet<>();
        for (String owner : new String[]{
                "com.android.server.pm.PackageSetting",
                "com.android.internal.pm.parsing.pkg.PackageImpl",
                "com.android.server.pm.parsing.pkg.PackageImpl"}) {
            try {
                Method check = persistentCheck(loader.loadClass(owner));
                if (check.getReturnType() != boolean.class
                        || Modifier.isAbstract(check.getModifiers())
                        || Modifier.isStatic(check.getModifiers())) continue;
                checks.add(check);
            } catch (ClassNotFoundException | NoSuchMethodException absent) {
                hooks.info("Persistent update optional target absent: " + owner + ".isPersistent");
            }
        }
        if (checks.isEmpty()) throw new NoSuchMethodException("Persistent update isPersistent targets");

        // Inactive until a preparation hook runs; partial setup cannot disable persistence globally.
        for (Method check : checks) {
            hooks.install(check, chain -> {
                Object original = chain.proceed();
                Attempt attempt = currentAttempt.get();
                if (attempt != null) attempt.checks++;
                Integer depth = preparing.get();
                if (installed && depth != null && depth > 0 && Boolean.TRUE.equals(original)
                        && Features.enabled(Features.CP_PERSISTENT)) {
                    if (attempt != null) attempt.overrides++;
                    return false;
                }
                return original;
            });
            hooks.info("Persistent update check hooked: " + check.toGenericString());
        }
        for (Method method : preparation) {
            hooks.install(method, chain -> {
                if (!installed) return chain.proceed();
                boolean enabled = Features.enabled(Features.CP_PERSISTENT);
                Attempt attempt = new Attempt(sequence.incrementAndGet());
                Attempt previousAttempt = currentAttempt.get();
                currentAttempt.set(attempt);
                Integer previous = preparing.get();
                if (enabled) preparing.set(previous == null ? 1 : previous + 1);
                report(attempt, "entered " + method.getName() + "; enabled=" + enabled);
                try {
                    return chain.proceed();
                } catch (Throwable failure) {
                    if (attempt.reportable()) {
                        try { hooks.error("Persistent attempt #" + attempt.id + " preparation failed", failure); }
                        catch (Throwable loggingFailure) { /* Preserve the original exception. */ }
                    }
                    throw failure;
                } finally {
                    if (enabled) {
                        if (previous == null) preparing.remove();
                        else preparing.set(previous);
                    }
                    if (previousAttempt == null) currentAttempt.remove();
                    else currentAttempt.set(previousAttempt);
                    report(attempt, "finished; getterCalls=" + attempt.checks + "; overrides=" + attempt.overrides);
                }
            });
            // The original getter may otherwise be inlined into this caller.
            hooks.deoptimizeReported(method);
            hooks.info("Persistent update preparation hooked: " + method.toGenericString());
        }
        // A compiled caller can also inline the preparation entry point itself.
        // Resolve only installation callers; do not alter persistence outside preparation.
        for (Method caller : helper.getDeclaredMethods()) {
            if ((caller.getName().equals("installPackagesTraced") || caller.getName().equals("installPackagesLI"))
                    && !Modifier.isAbstract(caller.getModifiers())) hooks.deoptimizeReported(caller);
        }
        // Keep even previously installed wrappers inactive if any later target fails.
        installed = true;
    }

    private void report(Attempt attempt, String message) {
        if (!attempt.reportable()) return;
        try { hooks.info("Persistent attempt #" + attempt.id + " " + message); }
        catch (Throwable loggingFailure) { /* Diagnostics must not change installation behaviour. */ }
    }

    private static Method persistentCheck(Class<?> owner) throws NoSuchMethodException {
        try {
            // Includes inherited public implementations.
            return owner.getMethod("isPersistent");
        } catch (NoSuchMethodException notPublic) {
            for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
                try {
                    return type.getDeclaredMethod("isPersistent");
                } catch (NoSuchMethodException absent) {
                    // Continue to an inherited non-public implementation.
                }
            }
            throw notPublic;
        }
    }
}
