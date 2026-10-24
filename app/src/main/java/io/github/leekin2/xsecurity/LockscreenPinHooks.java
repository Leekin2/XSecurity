package io.github.leekin2.xsecurity;

import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HyperOS 4 phone PIN layout. Adapted from HyperCeiler ScramblePIN (AGPL-3.0,
 * Copyright 2023-2026 HyperCeiler Contributions); see NOTICE. Moves complete digit views,
 * preserving their labels, input callbacks, IDs and values. Verification is never hooked.
 */
final class LockscreenPinHooks {
    private final Hooks hooks;
    private final ClassLoader loader;
    private final AtomicBoolean reported = new AtomicBoolean();

    LockscreenPinHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        if (!Features.enabled(Features.LOCKSCREEN_PIN_SCRAMBLE)) return;
        try {
            Class<?> pin = loader.loadClass("com.android.keyguard.KeyguardPINView");
            Class<?> key = loader.loadClass("com.android.keyguard.NumPadKey");
            if (!View.class.isAssignableFrom(pin) || !View.class.isAssignableFrom(key)) {
                throw new IllegalStateException("Unknown SystemUI PIN types");
            }
            Field views = field(pin, "mViews", View[][].class);
            Field numbers = field(pin, "mNumViews", View[][].class);
            Field digit = field(key, "mDigit", int.class);
            Method inflate = pin.getDeclaredMethod("onFinishInflate");
            Method appear = pin.getDeclaredMethod("startAppearAnimation");
            if (inflate.getReturnType() != void.class || appear.getReturnType() != void.class
                    || Modifier.isStatic(inflate.getModifiers()) || Modifier.isStatic(appear.getModifiers())) {
                throw new IllegalStateException("Unknown PIN lifecycle methods");
            }
            hooks.install(inflate, chain -> {
                Object result = chain.proceed();
                shuffleSafely(chain.getThisObject(), pin, key, views, numbers, digit);
                return result;
            });
            hooks.install(appear, chain -> {
                // New order before the entry animation, never during digit entry.
                shuffleSafely(chain.getThisObject(), pin, key, views, numbers, digit);
                return chain.proceed();
            });
            hooks.deoptimizeReported(inflate);
            hooks.deoptimizeReported(appear);
            hooks.info("lockscreen PIN: phone layout hooks ready; credential verification unchanged");
        } catch (Throwable failure) {
            hooks.error("lockscreen PIN unavailable; original keypad retained", failure);
        }
    }

    private void shuffleSafely(Object self, Class<?> pin, Class<?> key, Field views, Field numbers, Field digit) {
        if (!Features.enabled(Features.LOCKSCREEN_PIN_SCRAMBLE) || self == null || self.getClass() != pin) return;
        try {
            boolean changed = shuffle((View) self, key, digit, (View[][]) views.get(self), (View[][]) numbers.get(self));
            if (reported.compareAndSet(false, true)) {
                hooks.info(changed ? "lockscreen PIN: digit views shuffled" : "lockscreen PIN: unsupported layout; original keypad retained");
            }
        } catch (Throwable failure) { hooks.error("lockscreen PIN layout; original order restored where possible", failure); }
    }

    private static Field field(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type || Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("Unknown PIN field: " + name);
        }
        field.setAccessible(true);
        return field;
    }

    static boolean shuffle(View root, Class<?> key, Field digit, View[][] views, View[][] numbers)
            throws IllegalAccessException {
        if (views == null || numbers == null || views.length != 6 || numbers.length != 4
                || views[4] == null || views[4].length != 3 || numbers[3] == null || numbers[3].length != 3
                || views[4][1] != numbers[3][1]) return false;
        ViewGroup[] rows = new ViewGroup[4];
        IdentityHashMap<ViewGroup, Boolean> groups = new IdentityHashMap<>();
        for (int row = 0; row < 4; row++) {
            int id = root.getResources().getIdentifier("row" + (row + 1), "id", "com.android.systemui");
            View candidate = id == 0 ? null : root.findViewById(id);
            if (!(candidate instanceof ViewGroup group) || group.getChildCount() != 3 || groups.put(group, true) != null) return false;
            if (row < 3 && (views[row + 1] == null || views[row + 1].length != 3 || numbers[row] != views[row + 1])) return false;
            rows[row] = group;
        }
        List<View> original = new ArrayList<>();
        ViewGroup.LayoutParams[] positions = new ViewGroup.LayoutParams[10];
        IdentityHashMap<View, Boolean> identities = new IdentityHashMap<>();
        boolean[] digits = new boolean[10];
        for (int index = 0; index < 10; index++) {
            int row = index < 9 ? index / 3 : 3, col = index < 9 ? index % 3 : 1;
            View view = rows[row].getChildAt(col);
            if (view == null || view.getClass() != key || view != views[row + 1][col]
                    || view.getParent() != rows[row] || identities.put(view, true) != null) return false;
            int value = digit.getInt(view);
            if (value < 0 || value > 9 || digits[value] || view.getLayoutParams() == null) return false;
            digits[value] = true;
            positions[index] = view.getLayoutParams();
            original.add(view);
        }
        if (key.isInstance(rows[3].getChildAt(0)) || key.isInstance(rows[3].getChildAt(2))) return false;
        List<View> order = new ArrayList<>(original);
        Collections.shuffle(order);
        boolean identical = true;
        for (int index = 0; index < 10; index++) identical &= order.get(index) == original.get(index);
        if (identical) Collections.rotate(order, 1);
        try { move(rows, original, order, positions); }
        catch (RuntimeException failure) {
            try { move(rows, original, original, positions); }
            catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
        for (int index = 0; index < 9; index++) views[index / 3 + 1][index % 3] = order.get(index);
        views[4][1] = order.get(9);
        numbers[3][1] = order.get(9);
        return true;
    }

    private static void move(ViewGroup[] rows, List<View> original, List<View> order, ViewGroup.LayoutParams[] positions) {
        for (View view : original) if (view.getParent() instanceof ViewGroup parent) parent.removeView(view);
        for (int index = 0; index < 10; index++) {
            rows[index < 9 ? index / 3 : 3].addView(order.get(index), index < 9 ? index % 3 : 1, positions[index]);
        }
    }
}
