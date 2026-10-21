package io.github.leekin2.xsecurity;

import android.content.Intent;
import android.net.Uri;

import java.lang.reflect.Method;

/**
 * App store redirects. The China ROM pushes every market:// link straight to GetApps. This
 * intercepts those redirects in system_server so the user can choose a store (e.g. Google Play),
 * and can open GetApps detail pages as a mini/floating window.
 *
 * Ported from HyperCeiler (AGPL-3.0) rule systemframework/others/BypassForceMiAppStore, rewritten
 * against this module's hook API. The rewrite is narrowed to market://details?id= intents so other
 * ACTION_VIEW intents are untouched. See NOTICE.
 */
final class AppStoreHooks {
    private final Hooks hooks;
    private final ClassLoader loader;

    AppStoreHooks(Hooks hooks, ClassLoader loader) { this.hooks = hooks; this.loader = loader; }

    void install() {
        boolean chooser = Features.enabled(Features.STORE_FORCE_CHOOSER);
        boolean mini = Features.enabled(Features.STORE_GETAPPS_MINI);
        if (!chooser && !mini) return;
        try { hookStartActivity(); hooks.info("app-store redirect ready (chooser=" + chooser + ", mini=" + mini + ")"); }
        catch (Throwable t) { hooks.error("app-store redirect unavailable; stock behaviour kept", t); }
    }

    private void hookStartActivity() throws ReflectiveOperationException {
        Class<?> atms = loader.loadClass("com.android.server.wm.ActivityTaskManagerService");
        int hooked = 0;
        for (Method method : atms.getDeclaredMethods()) {
            if (!method.getName().equals("startActivity")) continue;
            hooks.install(method, chain -> {
                Object[] args = chain.getArgs().toArray();
                int index = intentIndex(args);
                if (index >= 0) args[index] = rewrite((Intent) args[index]);
                return chain.proceed(args);
            });
            hooked++;
        }
        if (hooked == 0) throw new NoSuchMethodException("ActivityTaskManagerService.startActivity");
    }

    /**
     * Rewrites a market://details redirect; returns the intent to start (a chooser when enabled),
     * or the original intent unchanged for everything else.
     */
    private Intent rewrite(Intent intent) {
        if (intent == null || !Intent.ACTION_VIEW.equals(intent.getAction())) return intent;
        Uri data = intent.getData();
        if (data == null) return intent;

        Uri rewritten = data;
        String type = intent.getType();
        // GetApps uses the mimarket scheme; normalise it so the match below works.
        if ("mimarket".equals(rewritten.getScheme())) {
            rewritten = rewritten.buildUpon().scheme("market").build();
        }
        boolean mini = Features.enabled(Features.STORE_GETAPPS_MINI);
        String path = rewritten.getPath();
        if (mini && isMarketDetailsHost(rewritten) && (path == null || path.isEmpty() || "/".equals(path))) {
            // detailmini opens the GetApps detail page as a small floating window.
            rewritten = rewritten.buildUpon().path("/detailmini").build();
        }
        if (!rewritten.equals(data)) {
            if (type != null) intent.setDataAndType(rewritten, type); else intent.setData(rewritten);
        }

        Uri finalData = intent.getData();
        if (!isMarketDetailsIntent(finalData) || finalData.getQueryParameter("id") == null) return intent;

        // Drop the forced GetApps package so another store can handle the link.
        if (intent.getPackage() != null) intent.setPackage(null);
        intent.removeFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (Features.enabled(Features.STORE_FORCE_CHOOSER)) {
            return Intent.createChooser(intent, null);
        }
        return intent;
    }

    private static int intentIndex(Object[] args) {
        if (args == null) return -1;
        for (int i = 0; i < args.length; i++) if (args[i] instanceof Intent) return i;
        return -1;
    }

    private static boolean isMarketDetailsHost(Uri uri) {
        return uri != null && "market".equals(uri.getScheme()) && "details".equals(uri.getHost());
    }

    private static boolean isMarketDetailsIntent(Uri uri) {
        if (!isMarketDetailsHost(uri)) return false;
        String path = uri.getPath();
        return path == null || path.isEmpty() || "/".equals(path) || "/detailmini".equals(path);
    }
}
