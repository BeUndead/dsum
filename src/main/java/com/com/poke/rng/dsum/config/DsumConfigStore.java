package com.com.poke.rng.dsum.config;

import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Loads and saves {@value #FILE_NAME} under the user config directory, creating the file with defaults when missing.
 * User presets use keys {@code preset.count} and {@code preset.&lt;n&gt;.*}; those keys are preserved when saving main
 * settings (Cmd+S / Ctrl+S).
 */
public final class DsumConfigStore {

    public static final String FILE_NAME = "config.properties";

    /** Number of entries; use {@code preset.1.label}, {@code preset.1.game}, … {@code preset.1.target.slots}, etc. */
    public static final String KEY_PRESET_COUNT = "preset.count";

    private static final String KEY_GAME = "game";
    private static final String KEY_ROUTE = "route";
    private static final String KEY_TARGET_SLOTS = "target.slots";
    private static final String KEY_MODIFIER = "modifier";
    private static final String KEY_PIKA_LEAD = "pika.lead";
    private static final String KEY_LEAD_LEVEL = "lead.level";
    private static final String KEY_UNCERTAINTY_OUTER_RB = "uncertainty.outer.rb.cycle";

    private static final String STORE_COMMENT =
            "DSum — Cmd+S saves main keys; preset.count + preset.1.label, preset.1.game, …; Add preset… in Details.";

    private DsumConfigStore() {
    }

    public static Path configFilePath() {
        final String home = System.getProperty("user.home", ".");
        return Path.of(home, ".config", "dsum", FILE_NAME);
    }

    /**
     * If the config file is absent, creates parent dirs, writes {@link DsumAppSettings#defaults()}, and returns
     * defaults. Otherwise loads and merges with defaults for unknown or invalid keys.
     */
    public static DsumLoadedConfig loadOrCreate() throws IOException {
        final Path path = configFilePath();
        if (!Files.isRegularFile(path)) {
            Files.createDirectories(path.getParent());
            final DsumAppSettings d = DsumAppSettings.defaults();
            write(path, d);
            return new DsumLoadedConfig(d, List.of());
        }
        final Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return new DsumLoadedConfig(fromProperties(p), parseUserPresets(p));
    }

    /**
     * Writes the main settings keys. Any existing {@code preset.*} (and other) keys are read from the file
     * first and left unchanged so user-defined presets survive Cmd+S.
     */
    public static void write(final Path path, final DsumAppSettings s) throws IOException {
        final Properties p = new Properties();
        if (Files.isRegularFile(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        p.setProperty(KEY_GAME, s.game().name());
        p.setProperty(KEY_ROUTE, s.route().name());
        p.setProperty(KEY_TARGET_SLOTS, formatTargetSlots(s.targetSlots()));
        p.setProperty(KEY_MODIFIER, Integer.toString(s.modifierUi()));
        p.setProperty(KEY_PIKA_LEAD, Boolean.toString(s.pikaLead()));
        p.setProperty(KEY_LEAD_LEVEL, Integer.toString(s.leadLevel()));
        p.setProperty(KEY_UNCERTAINTY_OUTER_RB, Boolean.toString(s.showOuterRbCycleUncertaintyBand()));
        Files.createDirectories(path.getParent());
        storeToPath(path, p);
    }

    /**
     * Appends one user preset: increments {@link #KEY_PRESET_COUNT} and writes {@code preset.&lt;n&gt;.*}. Loads an
     * existing file when present so other keys are preserved.
     */
    public static void appendUserPreset(final Path path, final EncounterSetupPreset preset) throws IOException {
        final Properties p = new Properties();
        if (Files.isRegularFile(path)) {
            try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        }
        final int current = parseInt(p.getProperty(KEY_PRESET_COUNT), 0, 0, 50);
        final int next = current + 1;
        if (next > 50) {
            throw new IOException("At most 50 file presets are supported (preset.count).");
        }
        p.setProperty(KEY_PRESET_COUNT, Integer.toString(next));
        putPresetEntry(p, "preset." + next + ".", preset);
        Files.createDirectories(path.getParent());
        storeToPath(path, p);
    }

    private static void putPresetEntry(final Properties p, final String pre, final EncounterSetupPreset preset) {
        p.setProperty(pre + "label", preset.menuLabel());
        p.setProperty(pre + KEY_GAME, preset.game().name());
        p.setProperty(pre + KEY_ROUTE, preset.route().name());
        p.setProperty(pre + KEY_LEAD_LEVEL, Integer.toString(preset.leadLevel()));
        p.setProperty(pre + KEY_PIKA_LEAD, Boolean.toString(preset.pikaLead()));
        p.setProperty(pre + KEY_MODIFIER, Integer.toString(preset.modifierUi()));
        p.setProperty(pre + KEY_TARGET_SLOTS, formatTargetSlots(preset.targetSlots()));
    }

    private static void storeToPath(final Path path, final Properties p) throws IOException {
        try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            p.store(w, STORE_COMMENT);
        }
    }

    public static DsumAppSettings fromModelSnapshot(
            final Game game,
            final Route route,
            final List<EncounterSlot> targetSlots,
            final int modifierUi,
            final boolean pikaLead,
            final int leadLevel,
            final boolean showOuterRbCycleUncertaintyBand) {
        final List<EncounterSlot> slots =
                targetSlots == null ? List.of() : List.copyOf(targetSlots);
        return new DsumAppSettings(
                game,
                route == null ? DsumAppSettings.defaults().route() : route,
                slots,
                modifierUi,
                pikaLead,
                leadLevel,
                showOuterRbCycleUncertaintyBand);
    }

    private static DsumAppSettings fromProperties(final Properties p) {
        final DsumAppSettings d = DsumAppSettings.defaults();
        final Game game = parseEnum(p.getProperty(KEY_GAME), Game.class, d.game());
        Route route = parseEnum(p.getProperty(KEY_ROUTE), Route.class, d.route());
        if (game == Game.YELLOW && route == Route.SURFING) {
            route = d.route();
        }
        final String targetRaw = p.getProperty(KEY_TARGET_SLOTS);
        final List<EncounterSlot> targets = parseTargetSlotsProperty(targetRaw, d.targetSlots());
        final int modifier = parseInt(p.getProperty(KEY_MODIFIER), d.modifierUi(), -150, 100);
        final boolean pika = parseBool(p.getProperty(KEY_PIKA_LEAD), d.pikaLead());
        final int lead = parseInt(p.getProperty(KEY_LEAD_LEVEL), d.leadLevel(), 1, 100);
        final boolean outerRb =
                parseBool(p.getProperty(KEY_UNCERTAINTY_OUTER_RB), d.showOuterRbCycleUncertaintyBand());
        return new DsumAppSettings(game, route, targets, modifier, pika, lead, outerRb);
    }

    private static List<EncounterSetupPreset> parseUserPresets(final Properties p) {
        final int n = parseInt(p.getProperty(KEY_PRESET_COUNT), 0, 0, 50);
        if (n <= 0) {
            return List.of();
        }
        final List<EncounterSetupPreset> out = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            final String pre = "preset." + i + ".";
            final String label = p.getProperty(pre + "label");
            if (label == null || label.isBlank()) {
                continue;
            }
            final Game game = parseEnumOrNull(p.getProperty(pre + KEY_GAME), Game.class);
            if (game == null) {
                continue;
            }
            Route route = parseEnumOrNull(p.getProperty(pre + KEY_ROUTE), Route.class);
            if (route == null) {
                continue;
            }
            if (game == Game.YELLOW && route == Route.SURFING) {
                route = DsumAppSettings.defaults().route();
            }
            final int lead = parseInt(p.getProperty(pre + KEY_LEAD_LEVEL), 70, 1, 100);
            final boolean pika = parseBool(p.getProperty(pre + KEY_PIKA_LEAD), false);
            final int modifier = parseInt(p.getProperty(pre + KEY_MODIFIER), 0, -150, 100);
            final List<EncounterSlot> targets = parsePresetTargets(p.getProperty(pre + KEY_TARGET_SLOTS));
            out.add(new EncounterSetupPreset(
                    label.trim(), game, route, lead, pika, modifier, targets));
        }
        return List.copyOf(out);
    }

    private static List<EncounterSlot> parsePresetTargets(final String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of(EncounterSlot._9);
        }
        final List<EncounterSlot> parsed = parseTargetSlotTokens(raw);
        return parsed.isEmpty() ? List.of(EncounterSlot._9) : parsed;
    }

    private static <E extends Enum<E>> E parseEnumOrNull(final String raw, final Class<E> type) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim());
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    private static String formatTargetSlots(final List<EncounterSlot> slots) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(slots.get(i).ordinal() + 1);
        }
        return sb.toString();
    }

    /**
     * {@code null} → fallback; blank → empty selection; non-blank → parsed slots, or fallback if no valid tokens.
     */
    private static List<EncounterSlot> parseTargetSlotsProperty(final String raw, final List<EncounterSlot> fallback) {
        if (raw == null) {
            return List.copyOf(fallback);
        }
        if (raw.isBlank()) {
            return List.of();
        }
        final List<EncounterSlot> parsed = parseTargetSlotTokens(raw);
        return parsed.isEmpty() ? List.copyOf(fallback) : parsed;
    }

    private static List<EncounterSlot> parseTargetSlotTokens(final String raw) {
        final String[] parts = raw.split(",");
        final List<EncounterSlot> out = new ArrayList<>();
        for (final String part : parts) {
            final String t = part.trim();
            if (t.isEmpty()) {
                continue;
            }
            try {
                final int n = Integer.parseInt(t);
                if (n >= 1 && n <= EncounterSlot.values().length) {
                    out.add(EncounterSlot.values()[n - 1]);
                }
            } catch (final NumberFormatException ignored) {
                try {
                    final String name = t.startsWith("_") ? t : "_" + t;
                    out.add(EncounterSlot.valueOf(name));
                } catch (final IllegalArgumentException ignored2) {
                    // skip invalid token
                }
            }
        }
        return List.copyOf(out);
    }

    private static int parseInt(final String raw, final int fallback, final int min, final int max) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            final int v = Integer.parseInt(raw.trim());
            return Math.max(min, Math.min(max, v));
        } catch (final NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBool(final String raw, final boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static <E extends Enum<E>> E parseEnum(final String raw, final Class<E> type, final E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim());
        } catch (final IllegalArgumentException e) {
            return fallback;
        }
    }
}
