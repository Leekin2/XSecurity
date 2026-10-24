package io.github.leekin2.xsecurity;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

/** Material Rounded vectors, the icon family used by the current LSPosed manager. See NOTICE. */
final class ActionIcons {
    static Drawable load(Context host, String action, int color) {
        int id = switch (action) {
            case "open" -> R.drawable.xsecurity_action_open;
            case "path" -> R.drawable.xsecurity_action_path;
            case "default" -> R.drawable.xsecurity_action_default;
            default -> throw new IllegalArgumentException("Unknown app action: " + action);
        };
        try {
            // Resource IDs belong to XSecurity, not the hooked Security app's Resources.
            Resources own = host.getPackageManager().getResourcesForApplication(BuildConfig.APPLICATION_ID);
            Drawable icon = own.getDrawable(id, null).mutate();
            icon.setTint(color);
            return icon;
        } catch (PackageManager.NameNotFoundException failure) {
            throw new IllegalStateException("XSecurity icon resources unavailable", failure);
        }
    }

    private ActionIcons() { }
}
