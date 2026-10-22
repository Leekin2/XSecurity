package io.github.leekin2.xsecurity;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** APK verification adapters, independently implemented for the modern ParseInput API. */
final class ApkIntegrityHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final ThreadLocal<Boolean> recovering = new ThreadLocal<>();

    ApkIntegrityHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        try {
            Class<?> verifier = loader.loadClass("android.util.apk.ApkSignatureVerifier");
            int count = 0;
            for (Method method : verifier.getDeclaredMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (Features.enabled(Features.CP_VERIFY) && (method.getName().equals("verifyV2Signature") || method.getName().equals("verifyV3Signature"))
                        && p.length >= 3 && p[p.length - 1] == boolean.class) {
                    hooks.install(method, chain -> {
                        Object[] args = chain.getArgs().toArray();
                        args[args.length - 1] = false;
                        return chain.proceed(args);
                    });
                    count++;
                }
                if (method.getName().equals("verifyV1Signature") && p.length == 3
                        && p[0].getName().equals("android.content.pm.parsing.result.ParseInput") && p[1] == String.class) {
                    installV1Recovery(method);
                    count++;
                }
            }
            if (count == 0) throw new NoSuchMethodException("Modern APK verification targets");
            hooks.info("Core Patch APK verifier adapters ready: " + count);
        } catch (Throwable t) { hooks.error("Core Patch APK verifier adapters unavailable; original parse results kept", t); }
    }

    private void installV1Recovery(Method method) throws ReflectiveOperationException {
        Class<?> parseResult = loader.loadClass("android.content.pm.parsing.result.ParseResult");
        Method isError = parseResult.getMethod("isError");
        Method getCode = parseResult.getMethod("getErrorCode");
        Class<?> parseInput = loader.loadClass("android.content.pm.parsing.result.ParseInput");
        Method success = parseInput.getMethod("success", Object.class);
        Class<?> signature = loader.loadClass("android.content.pm.Signature");
        Constructor<?> signatureFromBytes = signature.getConstructor(byte[].class);
        Class<?> details = loader.loadClass("android.content.pm.SigningDetails");
        Constructor<?> detailsConstructor = details.getDeclaredConstructor(Array.newInstance(signature, 0).getClass(), int.class);
        detailsConstructor.setAccessible(true);
        Class<?> withDigests = loader.loadClass("android.util.apk.ApkSignatureVerifier$SigningDetailsWithDigests");
        Constructor<?> withDigestsConstructor = withDigests.getDeclaredConstructor(details, java.util.Map.class);
        withDigestsConstructor.setAccessible(true);
        hooks.install(method, chain -> {
            Object original = chain.proceed();
            if (Boolean.TRUE.equals(recovering.get()) || !Features.enabled(Features.CP_VERIFY) || original == null
                    || !Boolean.TRUE.equals(isError.invoke(original)) || ((Number) getCode.invoke(original)).intValue() != -103) return original;
            recovering.set(true);
            try {
                String path = (String) chain.getArg(1);
                Object signatures = Features.enabled(Features.CP_PREVIOUS) ? installedSignatures(path) : null;
                if (signatures == null && Features.enabled(Features.CP_DIGEST)) {
                    List<Certificate> certs = jarCertificates(path);
                    if (!certs.isEmpty()) {
                        signatures = Array.newInstance(signature, 1);
                        Array.set(signatures, 0, signatureFromBytes.newInstance((Object) certs.get(0).getEncoded()));
                    }
                }
                // Never invent a platform identity or conceal an unrelated parser failure.
                if (signatures == null || Array.getLength(signatures) == 0) return original;
                Object signing = detailsConstructor.newInstance(signatures, 1);
                Object result = withDigestsConstructor.newInstance(signing, null);
                Object input = chain.getArg(0);
                // reset belongs to the concrete parser, not the ParseInput interface.
                // Resolve the actual implementation for ROMs with different parser types.
                Method reset = input.getClass().getMethod("reset");
                reset.setAccessible(true);
                reset.invoke(input);
                return success.invoke(input, result);
            } catch (ReflectiveOperationException | java.io.IOException | java.security.cert.CertificateException unsupported) {
                hooks.error("Core Patch V1 signer recovery unavailable; parse rejection retained", unsupported);
                return original;
            } finally { recovering.remove(); }
        });
    }

    private Object installedSignatures(String path) throws ReflectiveOperationException {
        Class<?> thread = loader.loadClass("android.app.ActivityThread");
        Object current = thread.getDeclaredMethod("currentActivityThread").invoke(null);
        if (current == null) return null;
        Object context = thread.getDeclaredMethod("getSystemContext").invoke(current);
        if (context == null) return null;
        Object manager = context.getClass().getMethod("getPackageManager").invoke(context);
        Class<?> packageManager = loader.loadClass("android.content.pm.PackageManager");
        Object archive = packageManager.getMethod("getPackageArchiveInfo", String.class, int.class).invoke(manager, path, 0);
        if (archive == null) return null;
        String name = (String) archive.getClass().getField("packageName").get(archive);
        Object installed = packageManager.getMethod("getPackageInfo", String.class, int.class).invoke(manager, name, 0x08000000);
        Object signing = installed.getClass().getField("signingInfo").get(installed);
        return signing == null ? null : signing.getClass().getMethod("getApkContentsSigners").invoke(signing);
    }

    private static List<Certificate> jarCertificates(String path) throws java.io.IOException, java.security.cert.CertificateException {
        try (JarFile jar = new JarFile(path, false)) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName().toUpperCase(java.util.Locale.ROOT);
                if (!name.startsWith("META-INF/") || !(name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) continue;
                try (InputStream input = jar.getInputStream(entry)) {
                    Collection<? extends Certificate> certs = CertificateFactory.getInstance("X.509").generateCertificates(input);
                    return List.copyOf(certs);
                }
            }
            return List.of();
        }
    }
}
