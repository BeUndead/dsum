package com.com.dsum.util;

import java.util.prefs.Preferences;

// Thin wrapper over the JDK preference store, so that chosen settings survive a restart.
// The JDK picks the backing store per platform, so there is no file handling of our own to do:
//   Windows - the registry, under HKCU\Software\JavaSoft\Prefs (node com/com/dsum/util)
//   macOS   - ~/Library/Preferences/com.apple.java.util.prefs.plist, written through CFPreferences
//   Linux   - ~/.java/.userPrefs/com/com/dsum/util/prefs.xml
// On macOS the values are owned by the cfprefsd daemon rather than by us, so the plist on disk
// can lag behind a write; "defaults read com.apple.java.util.prefs" is the reliable way to
// inspect what was actually stored.
//
// Every read takes the fallback to use when nothing is stored yet, or when what is stored can no
// longer be understood.  Callers still have to range check what comes back: a value written by an
// older version of the app is not necessarily one this version considers valid.
public final class UserPreferences {

    public static final String GAME = "game";
    public static final String ROUTE = "route";
    public static final String LEAD_LEVEL = "leadLevel";
    public static final String THRESHOLD = "threshold";

    private static final Preferences PREFERENCES = openStore();

    private UserPreferences() {
    }

    public static int getInt(final String key, final int fallback) {
        if (PREFERENCES == null) {
            return fallback;
        }
        return PREFERENCES.getInt(key, fallback);
    }

    public static void putInt(final String key, final int value) {
        if (PREFERENCES == null) {
            return;
        }
        PREFERENCES.putInt(key, value);
    }

    public static double getDouble(final String key, final double fallback) {
        if (PREFERENCES == null) {
            return fallback;
        }
        return PREFERENCES.getDouble(key, fallback);
    }

    public static void putDouble(final String key, final double value) {
        if (PREFERENCES == null) {
            return;
        }
        PREFERENCES.putDouble(key, value);
    }

    // Enums are stored by name() rather than by ordinal, so that reordering the constants does not
    // silently turn a stored value into a different one.
    public static <E extends Enum<E>> E getEnum(final String key, final E fallback) {
        if (PREFERENCES == null) {
            return fallback;
        }
        final String stored = PREFERENCES.get(key, null);
        if (stored == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), stored);
        } catch (final IllegalArgumentException unknownConstantEx) {
            // The constant was renamed or removed since it was written; fall back rather than fail.
            return fallback;
        }
    }

    public static void putEnum(final String key, final Enum<?> value) {
        if (PREFERENCES == null) {
            return;
        }
        PREFERENCES.put(key, value.name());
    }

    private static Preferences openStore() {
        try {
            return Preferences.userNodeForPackage(UserPreferences.class);
        } catch (final RuntimeException storeEx) {
            // No preference store available on this machine; settings simply will not persist.
            return null;
        }
    }
}
