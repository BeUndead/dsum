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
public final class UserPreferences {

    private static final String LEAD_LEVEL_KEY = "leadLevel";

    private static final Preferences PREFERENCES = openStore();

    private UserPreferences() {
    }

    public static int getLeadLevel(final int fallback) {
        if (PREFERENCES == null) {
            return fallback;
        }
        return PREFERENCES.getInt(LEAD_LEVEL_KEY, fallback);
    }

    public static void setLeadLevel(final int leadLevel) {
        if (PREFERENCES == null) {
            return;
        }
        PREFERENCES.putInt(LEAD_LEVEL_KEY, leadLevel);
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
