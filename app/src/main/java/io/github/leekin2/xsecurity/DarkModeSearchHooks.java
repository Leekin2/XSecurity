package io.github.leekin2.xsecurity;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/** Adds a search action to the existing HyperOS dark-mode app category in Settings. */
final class DarkModeSearchHooks {
    private static final String KEY = "xsecurity_dark_mode_search";
    private final Hooks hooks;
    private final ClassLoader loader;
    private final Map<Object, State> states = Collections.synchronizedMap(new WeakHashMap<>());
    private Field category;
    private Field userId;
    private Method loadIcon;
    private Constructor<?> preference;
    private Class<?> listener;
    private Method getContext, find, add, count, child, title, key, visible, setVisible;
    private Method setKey, setTitle, setSummary, setOrder, setPersistent, setIcon, setClick;

    DarkModeSearchHooks(Hooks hooks, ClassLoader loader) {
        this.hooks = hooks;
        this.loader = loader;
    }

    void install() {
        if (!Features.enabled(Features.DARK_MODE_ALL_APPS)) return;
        try {
            Class<?> fragment = loader.loadClass("com.android.settings.display.DarkModeFragment");
            Class<?> pref = loader.loadClass("androidx.preference.Preference");
            Class<?> group = loader.loadClass("androidx.preference.PreferenceGroup");
            listener = loader.loadClass("androidx.preference.Preference$OnPreferenceClickListener");
            category = fragment.getDeclaredField("mDarkModeAppsCategory");
            category.setAccessible(true);
            getContext = fragment.getMethod("getContext");
            preference = pref.getConstructor(Context.class);
            find = group.getMethod("findPreference", CharSequence.class);
            add = group.getMethod("addPreference", pref);
            count = group.getMethod("getPreferenceCount");
            child = group.getMethod("getPreference", int.class);
            title = pref.getMethod("getTitle");
            key = pref.getMethod("getKey");
            visible = pref.getMethod("isVisible");
            setVisible = pref.getMethod("setVisible", boolean.class);
            setKey = pref.getMethod("setKey", String.class);
            setTitle = pref.getMethod("setTitle", CharSequence.class);
            setSummary = pref.getMethod("setSummary", CharSequence.class);
            setOrder = pref.getMethod("setOrder", int.class);
            setPersistent = pref.getMethod("setPersistent", boolean.class);
            setIcon = pref.getMethod("setIcon", int.class);
            setClick = pref.getMethod("setOnPreferenceClickListener", listener);
            // Resolve all required targets before registering anything.
            Method created = fragment.getDeclaredMethod("onViewCreated", View.class, Bundle.class);
            Method shown = fragment.getDeclaredMethod("showAppList", java.util.List.class);
            Method destroyed = fragment.getDeclaredMethod("onDestroy");
            // Optional: the ROM's scroll loader indexes the unfiltered list. Prime icons for
            // the first search results so a distant match does not keep its placeholder icon.
            try {
                Method iconLoader = fragment.getDeclaredMethod("loadIcon", String.class, int.class);
                Field user = fragment.getDeclaredField("userId");
                iconLoader.setAccessible(true);
                user.setAccessible(true);
                loadIcon = iconLoader;
                userId = user;
            } catch (ReflectiveOperationException unavailable) {
                // Other ROMs still keep their normal icon loader.
            }
            hooks.install(created, chain -> {
                Object result = chain.proceed();
                refreshSafely(chain.getThisObject());
                return result;
            });
            hooks.install(shown, chain -> {
                Object result = chain.proceed();
                refreshSafely(chain.getThisObject());
                return result;
            });
            hooks.install(destroyed, chain -> {
                try { return chain.proceed(); }
                finally {
                    State state = states.remove(chain.getThisObject());
                    if (state != null && state.dialog != null) {
                        try { state.dialog.dismiss(); }
                        catch (Throwable failure) { hooks.error("dark-mode search cleanup", failure); }
                    }
                }
            });
            hooks.deoptimizeReported(shown);
            hooks.info("dark-mode app search ready in Settings");
        } catch (Throwable failure) {
            hooks.error("dark-mode search unavailable; stock Settings list kept", failure);
        }
    }

    private void refreshSafely(Object fragment) {
        try {
            if (!Features.enabled(Features.DARK_MODE_ALL_APPS)) return;
            Context context = (Context) getContext.invoke(fragment);
            Object group = category.get(fragment);
            if (context == null || group == null) return;
            State state = states.get(fragment);
            if (state == null) {
                state = new State(fragment);
                states.put(fragment, state);
            }
            Object row = find.invoke(group, KEY);
            if (row == null) {
                row = preference.newInstance(context);
                setKey.invoke(row, KEY);
                setOrder.invoke(row, Integer.MIN_VALUE);
                setTitle.invoke(row, words(context)[0]);
                setIcon.invoke(row, android.R.drawable.ic_menu_search);
                setPersistent.invoke(row, false);
                State target = state;
                Object callback = Proxy.newProxyInstance(loader, new Class<?>[]{listener}, (proxy, method, args) -> {
                    return switch (method.getName()) {
                        case "onPreferenceClick" -> {
                            showSearch(target);
                            yield true;
                        }
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> args != null && args.length == 1 && proxy == args[0];
                        case "toString" -> "XSecurityDarkModeSearch";
                        default -> null;
                    };
                });
                setClick.invoke(row, callback);
                add.invoke(group, row);
            }
            filter(group, row, state, context);
        } catch (Throwable failure) {
            hooks.error("dark-mode search refresh", failure);
        }
    }

    private void filter(Object group, Object search, State state, Context context) throws ReflectiveOperationException {
        int total = 0, matches = 0;
        int size = (Integer) count.invoke(group);
        for (int index = 0; index < size; index++) {
            Object row = child.invoke(group, index);
            String packageName = (String) key.invoke(row);
            if (KEY.equals(packageName)) continue;
            boolean original = state.visibility.computeIfAbsent(row, unused -> {
                try { return Boolean.TRUE.equals(visible.invoke(row)); }
                catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
            });
            if (original) total++;
            boolean match = original && AppSearch.matches((CharSequence) title.invoke(row), packageName, state.query);
            if (match) matches++;
            if (Boolean.TRUE.equals(visible.invoke(row)) != match) setVisible.invoke(row, match);
            if (match && matches <= 12 && !state.query.isEmpty() && loadIcon != null) {
                Object fragment = state.fragment.get();
                if (fragment != null) loadIcon.invoke(fragment, packageName, userId.getInt(fragment));
            }
        }
        setSummary.invoke(search, state.query.isEmpty() ? words(context)[1] : state.query + " · " + matches + "/" + total);
    }

    private void showSearch(State state) {
        try {
            Object fragment = state.fragment.get();
            if (fragment == null) return;
            Context context = (Context) getContext.invoke(fragment);
            if (context == null || state.dialog != null) return;
            String[] text = words(context);
            EditText input = new EditText(context);
            input.setSingleLine(true);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(256)});
            input.setHint(text[1]);
            input.setText(state.query);
            input.setSelection(input.length());
            int padding = Math.round(24 * context.getResources().getDisplayMetrics().density);
            int vertical = Math.round(12 * context.getResources().getDisplayMetrics().density);
            input.setPadding(padding, vertical, padding, vertical);
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle(text[0]).setView(input)
                    .setPositiveButton(text[2], (which, button) -> update(state, input.getText().toString().trim()))
                    .setNeutralButton(text[3], (which, button) -> update(state, ""))
                    .setNegativeButton(android.R.string.cancel, null).create();
            state.dialog = dialog;
            dialog.setOnDismissListener(ignored -> state.dialog = null);
            input.setOnEditorActionListener((view, action, event) -> {
                if (action != EditorInfo.IME_ACTION_SEARCH) return false;
                update(state, input.getText().toString().trim());
                dialog.dismiss();
                return true;
            });
            dialog.show();
            input.requestFocus();
        } catch (Throwable failure) {
            state.dialog = null;
            hooks.error("dark-mode search dialog", failure);
        }
    }

    private void update(State state, String query) {
        state.query = query;
        Object fragment = state.fragment.get();
        if (fragment != null) refreshSafely(fragment);
    }

    private static String[] words(Context context) {
        Locale locale = context.getResources().getConfiguration().getLocales().get(0);
        return switch (locale.getLanguage()) {
            case "tr" -> new String[]{"Uygulama ara", "Uygulama adı veya paket adı", "Ara", "Temizle"};
            case "ru" -> new String[]{"Поиск приложений", "Название или имя пакета", "Найти", "Очистить"};
            case "es" -> new String[]{"Buscar aplicaciones", "Nombre de la app o del paquete", "Buscar", "Borrar"};
            case "zh" -> new String[]{"搜索应用", "应用名称或包名", "搜索", "清除"};
            default -> new String[]{"Search apps", "App name or package name", "Search", "Clear"};
        };
    }

    private static final class State {
        final WeakReference<Object> fragment;
        final Map<Object, Boolean> visibility = new WeakHashMap<>();
        String query = "";
        AlertDialog dialog;
        State(Object fragment) { this.fragment = new WeakReference<>(fragment); }
    }
}
