package io.github.leekin2.xsecurity;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Global-ROM VoLTE / VoNR status bar icons on a China HyperOS build. The SystemUI code is the
 * same in both regions (compared with the global nezha OS4.0.0.10 SystemUI); only two
 * IS_INTERNATIONAL_BUILD checks differ for this icon:
 *  - MiuiOperatorCustomizedPolicy.getMiuiOperatorConfig picks the boxed Vo/LTE and Vo/NR
 *    drawables from the carrier's getStatusBarVolteType on global, the "HD" drawable on China;
 *  - MiuiMobileIconBinder starts the VoLTE visibility collector only on global.
 * Both are reproduced here exactly as the global code runs them. Visibility still comes from
 * Xiaomi's own flows (IMS registration broadcast, VoNR state, Wi-Fi, airplane, satellite); the
 * region flag, carrier settings and IMS itself are never changed.
 */
final class ImsIconHooks {
    private static final String PKG = "com.android.systemui";
    private static final String POLICY = "com.android.systemui.MiuiOperatorCustomizedPolicy";
    private static final String BINDER = "com.android.systemui.statusbar.pipeline.mobile.ui.binder.MiuiMobileIconBinder$bind$1$1$10";
    private static final String VIEW_MODEL = "com.android.systemui.statusbar.pipeline.mobile.ui.viewmodel.MiuiMobileIconViewModel";

    private final Hooks hooks;
    private final ClassLoader loader;

    ImsIconHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.IMS_ICONS)) return;
        try {
            if (Class.forName("miui.os.Build", false, loader).getField("IS_INTERNATIONAL_BUILD").getBoolean(null)) {
                hooks.info("IMS icons: global build, nothing to change");
                return;
            }
        } catch (Throwable failure) {
            hooks.error("IMS icons: region unknown; original icons kept", failure);
            return;
        }
        try { hookDrawables(); }
        catch (Throwable failure) { hooks.error("IMS icons: drawables unavailable; original icons kept", failure); }
        try { hookVisibility(); }
        catch (Throwable failure) { hooks.error("IMS icons: visibility unavailable; original icons kept", failure); }
    }

    /** Global branch of getMiuiOperatorConfig: volteResId / vonrResId by the carrier's drawable type. */
    private void hookDrawables() throws Exception {
        Class<?> policy = Class.forName(POLICY, false, loader);
        Method config = policy.getDeclaredMethod("getMiuiOperatorConfig", int.class);
        Method type = policy.getDeclaredMethod("getStatusBarVolteType", int.class);
        type.setAccessible(true);
        Field context = field(policy, "mContext");
        hooks.install(config, chain -> {
            Object result = chain.proceed();
            if (result == null) return null;
            int slot = (Integer) chain.getArg(0);
            int kind = (Integer) type.invoke(chain.getThisObject(), slot);
            Context ctx = (Context) context.get(chain.getThisObject());
            int volte = drawable(ctx, volteName(kind));
            int vonr = drawable(ctx, kind == 1 || kind == 2 ? "stat_sys_signal_vonr_no_frame" : "stat_sys_signal_vonr");
            if (volte != 0 && vonr != 0) {
                field(result.getClass(), "volteResId").setInt(result, volte);
                field(result.getClass(), "vonrResId").setInt(result, vonr);
            }
            return result;
        });
        hooks.info("IMS icons: global Vo/LTE and Vo/NR drawables ready");
    }

    private static String volteName(int kind) {
        switch (kind) {
            case 1: return "stat_sys_signal_volte_4g";
            case 2: return "stat_sys_signal_volte_no_frame";
            case 3: return "stat_sys_signal_volte_hd_voice";
            case 4: return "stat_sys_signal_hd_big";
            case 5: return "stat_sys_signal_volte_vo4g";
            default: return "stat_sys_signal_volte";
        }
    }

    private static int drawable(Context context, String name) {
        // R8 inlined SystemUI's R class, so resolve by name.
        return context.getResources().getIdentifier(name, "drawable", PKG);
    }

    /**
     * The binder coroutine runs, on global only:
     *   collectLatest(combine(flowCombine(vm.volteVisibleGlobal, dripEnd, $1), vm.satelliteVisible, vm.isVisible, $2), $3)
     * Run the same chain with Xiaomi's own $1/$2/$3 lambdas when the region check would skip it.
     */
    private void hookVisibility() throws Exception {
        Class<?> binder = Class.forName(BINDER, false, loader);
        Class<?> flow = type("kotlinx.coroutines.flow.Flow");
        Class<?> continuation = type("kotlin.coroutines.Continuation");
        Class<?> vm = type(VIEW_MODEL);
        Field model = field(binder, "$miuiVM");
        Field drip = field(binder, "$dripEndFlow");
        Field group = field(binder, "$mobileGroup");
        Field volte = field(binder, "$volte");
        Field label = field(binder, "label");
        if (volte.getType() != ImageView.class) throw new IllegalStateException("Unknown VoLTE view type");
        Class<?> wifiRule = type(BINDER + "$1");
        Class<?> satelliteRule = type(BINDER + "$2");
        Constructor<?> collector = type(BINDER + "$3").getDeclaredConstructor(group.getType(), ImageView.class, continuation);
        collector.setAccessible(true);
        // R8 dropped the lambdas' forwarding constructors; call SuspendLambda(arity, null) on them.
        Constructor<?> lambda = type("kotlin.coroutines.jvm.internal.SuspendLambda").getDeclaredConstructor(int.class, continuation);
        Class<?> flowKt = type("kotlinx.coroutines.flow.FlowKt");
        Method flowCombine = flowKt.getMethod("flowCombine", flow, flow, type("kotlin.jvm.functions.Function3"));
        Method combine = flowKt.getMethod("combine", flow, flow, flow, type("kotlin.jvm.functions.Function4"));
        Method collectLatest = flowKt.getMethod("collectLatest", flow, type("kotlin.jvm.functions.Function2"), continuation);
        Method visibleGlobal = vm.getMethod("getVolteVisibleGlobal");
        Method satellite = vm.getMethod("getSatelliteVisible");
        Method visible = vm.getMethod("isVisible");
        group.getType().getMethod("setChildVisible", View.class, boolean.class);
        Method throwOnFailure = type("kotlin.ResultKt").getMethod("throwOnFailure", Object.class);
        Object suspended = type("kotlin.coroutines.intrinsics.CoroutineSingletons").getField("COROUTINE_SUSPENDED").get(null);
        Object unit = type("kotlin.Unit").getField("INSTANCE").get(null);
        Method invoke = binder.getDeclaredMethod("invokeSuspend", Object.class);
        hooks.install(invoke, chain -> {
            Object self = chain.getThisObject();
            int state = label.getInt(self);
            if (state == 1) {
                call(throwOnFailure, null, chain.getArg(0));
                return unit;
            }
            if (state != 0) return chain.proceed();
            call(throwOnFailure, null, chain.getArg(0));
            Object vmValue = model.get(self);
            Object wifi = call(flowCombine, null, call(visibleGlobal, vmValue), drip.get(self),
                    hooks.newInstanceSpecial(lambda, wifiRule, 3, null));
            Object shown = call(combine, null, wifi, call(satellite, vmValue), call(visible, vmValue),
                    hooks.newInstanceSpecial(lambda, satelliteRule, 4, null));
            Object action = collector.newInstance(group.get(self), volte.get(self), null);
            label.setInt(self, 1);
            Object result = call(collectLatest, null, shown, action, self);
            return result == suspended ? suspended : unit;
        });
        hooks.info("IMS icons: global VoLTE visibility collector ready");
    }

    private Class<?> type(String name) throws ClassNotFoundException { return Class.forName(name, false, loader); }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static Object call(Method method, Object target, Object... args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }
}
