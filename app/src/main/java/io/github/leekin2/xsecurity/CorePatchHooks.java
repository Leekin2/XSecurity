package io.github.leekin2.xsecurity;

import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Optional PackageManager installation policies. Target names were cross-checked
 * against AOSP and Core Patch N-1.0; this is an independent libxposed-102 adapter.
 * Never changes MessageDigest/TLS checks outside package-install verification.
 */
final class CorePatchHooks {
    static final String[] FEATURES = Features.CORE_PATCH;
    private final Hooks hooks;
    private final ClassLoader loader;
    private static final ThreadLocal<Boolean> ORIGINAL_SIGNING = new ThreadLocal<>();
    private interface Step { void run() throws Throwable; }
    interface OriginalStep { Object run() throws Throwable; }

    CorePatchHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (Features.enabled(Features.CP_VERIFY) || Features.enabled(Features.CP_PREVIOUS)) new ApkIntegrityHooks(hooks, loader).install();
        if (Features.enabled(Features.CP_DOWNGRADE)) step("downgrade", () ->
                constant("com.android.server.pm.PackageManagerServiceUtils", "checkDowngrade", void.class, null));
        if (Features.enabled(Features.CP_VERIFY)) {
            step("package signature verification", () -> constant("com.android.server.pm.PackageManagerServiceUtils", "verifySignatures", boolean.class, false));
            step("minimum signature scheme", () -> constant("com.android.server.pm.ScanPackageUtils", "assertMinSignatureSchemeIsValid", void.class, null));
            step("target SDK signature scheme", () -> constant("android.util.apk.ApkSignatureVerifier", "getMinimumSignatureSchemeVersionForTargetSdk", int.class, 0));
            step("JAR verification", this::jarVerification);
            step("APK verity verification", this::verityVerification);
            step("package-install digest equality", this::digestEquality);
        }
        if (Features.enabled(Features.CP_ARSC)) step("resources.arsc", () -> constant("android.content.res.AssetManager", "containsAllocatedTable", boolean.class, false));
        if (Features.enabled(Features.CP_DIGEST)) {
            step("signer capabilities", this::signingCapabilities);
            step("upgrade keysets", this::upgradeKeysets);
        }
        if (Features.enabled(Features.CP_EXACT)) step("exact signer matching", () -> {
            Class<?> details = loader.loadClass("android.content.pm.SigningDetails");
            Method exact = details.getDeclaredMethod("signaturesMatchExactly", details);
            hooks.install(exact, chain -> inInstallFlow() ? true : chain.proceed());
        });
        if (Features.enabled(Features.CP_PREVIOUS)) step("previous-package permissions", this::previousPackage);
        if (Features.enabled(Features.CP_HIDDEN)) step("system hidden APIs", this::hiddenApis);
        if (Features.enabled(Features.CP_SHARED) && Features.enabled(Features.CP_DIGEST)) step("shared UID", this::sharedUid);
        else if (Features.enabled(Features.CP_SHARED)) hooks.info("Core Patch shared UID requires signer-capability option; stock behaviour kept");
        if (Features.enabled(Features.CP_SHARED) && Features.enabled(Features.CP_DIGEST)) step("shared UID member state", () -> new SharedUidStateHooks(hooks, loader).install());
        if (Features.enabled(Features.CP_AGENT)) step("verification agent", this::verificationAgent);
        if (Features.enabled(Features.CP_BLOCK)) step("Nothing OS blocklist", () -> {
            constant("com.nothing.server.ex.NtConfigListServiceImpl", "isInstallingAppForbidden", boolean.class, false);
            constant("com.nothing.server.ex.NtConfigListServiceImpl", "isStartingAppForbidden", boolean.class, false);
        });
    }

    private void step(String name, Step step) {
        try { step.run(); hooks.info("Core Patch " + name + " ready"); }
        catch (Throwable t) { hooks.error("Core Patch " + name + " unavailable; stock behaviour kept for missing targets", t); }
    }

    private void constant(String owner, String name, Class<?> result, Object value) throws ReflectiveOperationException {
        methods(owner, name, result, chain -> value);
    }

    private void methods(String owner, String name, Class<?> result, XposedInterface.Hooker interceptor) throws ReflectiveOperationException {
        int count = 0;
        for (Method method : loader.loadClass(owner).getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getReturnType() == result) {
                hooks.install(method, interceptor);
                count++;
            }
        }
        if (count == 0) throw new NoSuchMethodException(owner + "." + name + ":" + result.getName());
    }

    private void signingCapabilities() throws ReflectiveOperationException {
        Class<?> details = loader.loadClass("android.content.pm.SigningDetails");
        int count = 0;
        for (String name : new String[]{"checkCapability", "checkCapabilityRecover"}) {
            try {
                Method method = details.getDeclaredMethod(name, details, int.class);
                if (method.getReturnType() != boolean.class) continue;
                hooks.install(method, chain -> {
                    int flags = (Integer) chain.getArg(1);
                    // Preserve permission and authentication capabilities, including combined flags.
                    if ((flags & (4 | 16)) != 0 || !inInstallFlow()) return chain.proceed();
                    return true;
                });
                count++;
            } catch (NoSuchMethodException missing) { hooks.info("Core Patch optional signer target missing: " + name); }
        }
        if (count == 0) throw new NoSuchMethodException("SigningDetails capability targets");
    }

    private void upgradeKeysets() throws ReflectiveOperationException {
        XposedInterface.Hooker allowDuringInstall = chain -> inInstallFlow() ? true : chain.proceed();
        methods("com.android.server.pm.KeySetManagerService", "shouldCheckUpgradeKeySetLocked", boolean.class, allowDuringInstall);
        methods("com.android.server.pm.KeySetManagerService", "checkUpgradeKeySetLocked", boolean.class, allowDuringInstall);
    }

    private void digestEquality() throws ReflectiveOperationException {
        Method method = java.security.MessageDigest.class.getDeclaredMethod("isEqual", byte[].class, byte[].class);
        hooks.install(method, chain -> inInstallFlow() ? true : chain.proceed());
    }

    private void jarVerification() throws ReflectiveOperationException {
        constant("android.util.jar.StrictJarVerifier", "verifyMessageDigest", boolean.class, true);
        constant("android.util.jar.StrictJarVerifier", "verify", boolean.class, true);
    }

    private void verityVerification() throws ReflectiveOperationException {
        methods("android.util.apk.ApkSigningBlockUtils", "parseVerityDigestAndVerifySourceLength", byte[].class, chain -> {
            Object value = chain.getArg(0);
            if (!(value instanceof byte[] bytes) || bytes.length < 32) return chain.proceed();
            return Arrays.copyOf(bytes, 32);
        });
        constant("android.util.apk.ApkSigningBlockUtils", "verifyIntegrityForVerityBasedAlgorithm", void.class, null);
    }

    private void previousPackage() throws ReflectiveOperationException {
        methods("com.android.server.pm.InstallPackageHelper", "doesSignatureMatchForPermissions", boolean.class, chain -> {
            Object original = chain.proceed();
            if (!Boolean.FALSE.equals(original) || !Features.enabled(Features.CP_DIGEST) || chain.getArgs().size() < 2) return original;
            Object name = chain.getArg(0), pkg = chain.getArg(1);
            if (!(name instanceof String) || pkg == null) return original;
            return name.equals(pkg.getClass().getMethod("getPackageName").invoke(pkg)) ? true : original;
        });
    }

    @android.annotation.SuppressLint("SoonBlockedPrivateApi") // Runs inside LSPosed's system_server, outside app hidden-API enforcement.
    private void hiddenApis() throws ReflectiveOperationException {
        Class<?> info = loader.loadClass("android.content.pm.ApplicationInfo");
        Field flags = info.getField("flags");
        Method method = info.getDeclaredMethod("isPackageWhitelistedForHiddenApis");
        if (method.getReturnType() != boolean.class) throw new NoSuchMethodException("ApplicationInfo hidden API check result");
        hooks.install(method, chain -> (flags.getInt(chain.getThisObject()) & (1 | 128)) != 0 ? true : chain.proceed());
    }

    private void sharedUid() throws ReflectiveOperationException {
        if (!Features.enabled(Features.CP_DIGEST)) {
            hooks.info("Core Patch shared UID requires signer-capability option; stock behaviour kept");
            return;
        }
        Class<?> details = loader.loadClass("android.content.pm.SigningDetails");
        Method ancestor = details.getDeclaredMethod("hasCommonAncestor", details);
        hooks.install(ancestor, chain -> inInstallFlow() ? true : chain.proceed());
        constant("com.android.server.pm.PackageManagerServiceUtils", "canJoinSharedUserId", boolean.class, true);
        Class<?> reconcile = loader.loadClass("com.android.server.pm.ReconcilePackageUtils");
        try {
            Field allow = reconcile.getDeclaredField("ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS");
            allow.setAccessible(true);
            allow.setBoolean(null, true);
        } catch (NoSuchFieldException absent) { hooks.info("Core Patch shared UID preload flag not present on this ROM"); }
        for (Method method : reconcile.getDeclaredMethods()) {
            if (method.getName().equals("reconcilePackages")) hooks.deoptimize(method);
        }
    }

    private void verificationAgent() throws ReflectiveOperationException {
        Class<?> session = loader.loadClass("com.android.server.pm.VerifyingSession");
        Field flags = session.getDeclaredField("mInstallFlags");
        flags.setAccessible(true);
        methods(session.getName(), "handleStartVerify", void.class, chain -> {
            Object self = chain.getThisObject();
            flags.setInt(self, flags.getInt(self) | 0x00080000);
            return chain.proceed();
        });
        constant(session.getName(), "isAdbVerificationEnabled", boolean.class, false);
    }

    static boolean inInstallFlow() {
        if (Boolean.TRUE.equals(ORIGINAL_SIGNING.get())) return false;
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String name = frame.getClassName();
            if (name.equals("com.android.server.pm.InstallPackageHelper")
                    || name.equals("com.android.server.pm.PackageManagerServiceUtils")
                    || name.equals("com.android.server.pm.ReconcilePackageUtils")
                    || name.equals("com.android.server.pm.ScanPackageUtils")
                    || name.startsWith("android.util.apk.") || name.equals("android.util.jar.StrictJarVerifier")) return true;
        }
        return false;
    }

    static Object originalSigning(OriginalStep step) throws Throwable {
        Boolean previous = ORIGINAL_SIGNING.get();
        ORIGINAL_SIGNING.set(true);
        try { return step.run(); }
        finally {
            if (previous == null) ORIGINAL_SIGNING.remove();
            else ORIGINAL_SIGNING.set(previous);
        }
    }
}
