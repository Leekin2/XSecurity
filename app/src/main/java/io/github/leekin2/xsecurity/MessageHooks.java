package io.github.leekin2.xsecurity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/** HyperCeiler MMS rules adapted to libxposed; AGPL-3.0, see NOTICE. */
final class MessageHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private String lastCode;

    MessageHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install(String apk, String data) {
        if (Features.enabled(Features.MMS_COPY_CODE)) step("verification-code clipboard", this::copyCode);
        if (Features.enabled(Features.MMS_FRAUD) || Features.enabled(Features.MMS_OVERSEAS)) riskGetters();
        if (!Features.enabled(Features.MMS_ADS) && !Features.enabled(Features.MMS_FRAUD)
                && !Features.enabled(Features.MMS_OVERSEAS) && !Features.enabled(Features.MMS_OLD_DEVICE)) return;
        try (DexTargets targets = new DexTargets(loader, apk, data)) {
            if (Features.enabled(Features.MMS_ADS)) step("message ads", () -> ads(targets));
            if (Features.enabled(Features.MMS_FRAUD) || Features.enabled(Features.MMS_OVERSEAS))
                step("message risk labels", () -> riskContacts(targets));
            if (Features.enabled(Features.MMS_OLD_DEVICE)) step("message device check", () -> {
                Method method;
                try {
                    method = loader.loadClass("com.android.mms.storage.DevicesUtil").getDeclaredMethod("isOldDevice");
                    if (method.getReturnType() != boolean.class || !Modifier.isStatic(method.getModifiers()))
                        throw new NoSuchMethodException("Unexpected Messages device gate");
                } catch (ClassNotFoundException | NoSuchMethodException missing) {
                    var methods = targets.usingStrings("mms_old_device_bool_v2", "boolean", "isOld", "deviceInfo");
                    if (methods.size() != 1 || methods.get(0).getParameterCount() != 0
                            || !Modifier.isStatic(methods.get(0).getModifiers()))
                        throw new NoSuchMethodException("Unique typed Messages device check unavailable");
                    method = methods.get(0);
                }
                hooks.install(method, chain -> Features.enabled(Features.MMS_OLD_DEVICE) ? false : chain.proceed());
                hooks.deoptimizeReported(method);
                hooks.info("Messages device-generation gate ready");
            });
        } catch (Throwable failure) { hooks.error("Messages scan unavailable; unsupported targets keep stock behaviour", failure); }
    }

    private void ads(DexTargets targets) throws Throwable {
        int count = 0;
        try {
            Method menu = loader.loadClass("com.miui.smsextra.ui.BottomMenu").getDeclaredMethod("allowMenuMode", Context.class);
            if (menu.getReturnType() == boolean.class) {
                hooks.install(menu, chain -> Features.enabled(Features.MMS_ADS) ? false : chain.proceed());
                count++;
            }
        } catch (ReflectiveOperationException missing) { /* The SDK varies by build. */ }
        for (Method method : targets.named("mms_hide_promo_v1", "setHideButton")) {
            String owner = method.getDeclaringClass().getName();
            if (!owner.startsWith("com.miui.smsextra.") && !owner.startsWith("com.android.mms.")) continue;
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != boolean.class
                    || method.getReturnType() != void.class || Modifier.isAbstract(method.getModifiers())) continue;
            hooks.install(method, chain -> Features.enabled(Features.MMS_ADS) ? chain.proceed(new Object[]{true}) : chain.proceed());
            count++;
        }
        if (count == 0) throw new NoSuchMethodException("Messages promotional controls unavailable");
        hooks.info("Messages promotional controls ready: " + count);
    }

    private void riskGetters() {
        for (String name : new String[]{"com.miui.smsextra.sdk.SmartContact", "com.miui.smsextra.internal.sdk.xiaomi.YellowPagePhone"}) {
            step("message risk model " + name, () -> {
                Class<?> type = loader.loadClass(name);
                for (String getter : new String[]{"isRiskyNumber", "isDefraudNumber"}) {
                    Method method;
                    try { method = type.getDeclaredMethod(getter); } catch (NoSuchMethodException absent) { continue; }
                    if (method.getReturnType() != boolean.class || Modifier.isStatic(method.getModifiers())) continue;
                    hooks.install(method, chain -> {
                        // On the inspected SDK, isRiskyNumber means overseas type 11;
                        // fraud has its own getter (type 12), including YellowPagePhone's integer model.
                        String key = getter.equals("isDefraudNumber") ? Features.MMS_FRAUD : Features.MMS_OVERSEAS;
                        if (Features.enabled(key)) return false;
                        return chain.proceed();
                    });
                    hooks.deoptimizeReported(method);
                }
            });
        }
    }

    private void riskContacts(DexTargets targets) throws Throwable {
        Set<Method> methods = new HashSet<>(targets.matching("mms_smart_contact_v1", () ->
                org.luckypray.dexkit.query.matchers.MethodMatcher.create().usingStrings("fillSmartContactByB2c: old smart contact is not null")));
        methods.addAll(targets.matching("mms_yellow_contact_v1", () ->
                org.luckypray.dexkit.query.matchers.MethodMatcher.create().usingStrings("fillYellowPageContact: ", "ContactFetcher")
                        .returnType("void").paramCount(2)));
        int count = 0;
        for (Method method : methods) {
            if (method.getParameterCount() < 1) continue;
            Field risk = riskField(method.getParameterTypes()[0]);
            if (risk == null) continue;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                Object contact = chain.getArg(0);
                if (contact != null && suppressRisk(risk.get(contact))) risk.set(contact, "");
                return result;
            });
            count++;
        }
        if (count == 0) throw new NoSuchMethodException("Typed Messages contact risk labels unavailable");
        hooks.info("Messages selected risk labels ready: " + count);
    }

    static boolean suppressRisk(Object type) {
        return ("11".equals(type) && Features.enabled(Features.MMS_OVERSEAS))
                || ("12".equals(type) && Features.enabled(Features.MMS_FRAUD));
    }

    private static Field riskField(Class<?> type) {
        for (Class<?> owner = type; owner != null; owner = owner.getSuperclass()) {
            try {
                Field field = owner.getDeclaredField("mRiskType");
                if (field.getType() != String.class || Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) return null;
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException absent) { /* May be declared on the base model. */ }
        }
        return null;
    }

    private void copyCode() throws Throwable {
        int count = 0;
        // Messages 18 creates a broadcast, not an activity PendingIntent. Its own factory
        // marks the extracted OTP explicitly, before building the notification action.
        try {
            Class<?> receiver = loader.loadClass("com.android.mms.transaction.VerificationCodeCopyReceiver");
            for (Method factory : receiver.getDeclaredMethods()) {
                Class<?>[] args = factory.getParameterTypes();
                if (!Modifier.isStatic(factory.getModifiers()) || factory.getReturnType() != Intent.class
                        || args.length != 5 || args[0] != int.class || args[1] != Context.class
                        || args[2] != android.net.Uri.class || args[3] != String.class || args[4] != String.class) continue;
                hooks.install(factory, chain -> {
                    Object result = chain.proceed();
                    if (result instanceof Intent intent) copyVerificationIntent((Context) chain.getArg(1), intent);
                    return result;
                });
                hooks.deoptimizeReported(factory);
                count++;
            }
        } catch (ClassNotFoundException missing) { /* Earlier versions use only the PendingIntent path. */ }
        for (Method method : android.app.PendingIntent.class.getDeclaredMethods()) {
            Class<?>[] args = method.getParameterTypes();
            String name = method.getName();
            if (!(name.equals("getActivity") || name.equals("getBroadcast") || name.equals("getService"))
                    || !Modifier.isStatic(method.getModifiers()) || (args.length != 4 && args.length != 5)
                    || args[0] != Context.class || args[1] != int.class || args[2] != Intent.class || args[3] != int.class
                    || (args.length == 5 && (!name.equals("getActivity") || args[4] != android.os.Bundle.class))) continue;
            hooks.install(method, chain -> {
                Object result = chain.proceed();
                if (result != null) copyVerificationIntent((Context) chain.getArg(0), (Intent) chain.getArg(2));
                return result;
            });
            hooks.deoptimizeReported(method);
            count++;
        }
        if (count == 0) throw new NoSuchMethodException("Messages verification-code notification targets unavailable");
        hooks.info("Messages verified-code notification factory and PendingIntent paths ready: " + count);
    }

    private void copyVerificationIntent(Context context, Intent intent) {
        if (!Features.enabled(Features.MMS_COPY_CODE) || context == null || intent == null
                || !"com.android.mms".equals(context.getPackageName())) return;
        try {
            if (!intent.getBooleanExtra("is_verification_code", false)
                    || !"function_copy".equals(intent.getStringExtra("function_type"))) return;
            String code = verificationCode(intent.getStringExtra("extra_text"));
            if (code == null) return;
            synchronized (this) {
                if (code.equals(lastCode)) return;
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard == null) return;
                ClipData clip = ClipData.newPlainText(null, code);
                PersistableBundle extras = new PersistableBundle();
                extras.putBoolean("android.content.extra.IS_SENSITIVE", true);
                extras.putBoolean("mms_is_ververification_code", true);
                clip.getDescription().setExtras(extras);
                clipboard.setPrimaryClip(clip);
                lastCode = code;
            }
        } catch (Throwable failure) { hooks.error("Messages verified-code copy unavailable", failure); }
        // Never log or persist a code, sender, message body or notification text.
    }

    static String verificationCode(String text) {
        if (text == null) return null;
        String code = text.trim();
        return code.matches("[0-9]{4,8}") ? code : null;
    }

    private interface Step { void run() throws Throwable; }
    private void step(String name, Step step) {
        try { step.run(); } catch (Throwable failure) { hooks.error(name + " unavailable; stock behaviour retained", failure); }
    }
}
