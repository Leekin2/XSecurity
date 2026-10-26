package io.github.leekin2.xsecurity;

import android.content.Context;
import android.os.PersistableBundle;
import java.lang.reflect.Method;

/**
 * Phone app (com.android.phone). Keeps Xiaomi's own "carrier check disabled" switches for VoLTE
 * and VoWiFi on — the ones its secret codes *#*#86583#*#* and *#*#869434#*#* toggle. With them on,
 * Xiaomi's CarrierConfigLoaderInjector shows "Use VoLTE" and the Wi-Fi calling network preference
 * on the SIM card settings page. Stock resets both at boot whenever an early carrier config lacks
 * config_support_secret_code_for_volte_and_wfc, so they never survive a restart; here they do.
 * Nothing else is changed: the carrier values come from Xiaomi's own code path.
 */
final class PhoneHooks {
    private static final String INJECTOR = "com.android.phone.CarrierConfigLoaderInjector";
    private static final String IMS_UTILS = "com.android.phone.MiuiImsPhoneUtils";

    private final Hooks hooks;
    private final ClassLoader loader;

    PhoneHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.SIM_MENUS)) return;
        try {
            Class<?> injector = Class.forName(INJECTOR, false, loader);
            Method volte = injector.getDeclaredMethod("isVolteForceEnabled", Context.class);
            Method vowifi = injector.getDeclaredMethod("isVoWifiForceEnabled", Context.class);
            Method support = Class.forName(IMS_UTILS, false, loader)
                    .getDeclaredMethod("supportSecretCodeForVolteAndWfc", PersistableBundle.class);
            if (volte.getReturnType() != boolean.class || vowifi.getReturnType() != boolean.class
                    || support.getReturnType() != boolean.class) {
                throw new IllegalStateException("Unknown phone app methods");
            }
            hooks.constant(volte, true);
            hooks.constant(vowifi, true);
            // Without this the boot-time config pass sees "unsupported" and skips the overrides.
            hooks.constant(support, true);
            // Small static callers can be JIT-inlined; keep the config pass interpreted.
            for (Method method : injector.getDeclaredMethods()) {
                if (method.getName().equals("updateVoLTECarrierConfig")) hooks.deoptimize(method);
            }
            hooks.info("phone: VoLTE / VoWiFi SIM menus ready");
        } catch (Throwable failure) {
            hooks.error("phone: SIM menus unavailable; stock settings kept", failure);
        }
    }
}
