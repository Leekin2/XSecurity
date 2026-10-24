package io.github.leekin2.xsecurity;

import java.text.Normalizer;
import java.util.Locale;

/** Literal search shared by the dark-mode app list. Does not change any preference value. */
final class AppSearch {
    static boolean matches(CharSequence label, String packageName, String query) {
        String needle = normalize(query).trim();
        if (needle.isEmpty()) return true;
        String haystack = normalize(label) + " " + normalize(packageName);
        for (String word : needle.split("\\s+")) {
            if (!haystack.contains(word)) return false;
        }
        return true;
    }

    private static String normalize(CharSequence value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('\u0131', 'i');
    }

    private AppSearch() { }
}
