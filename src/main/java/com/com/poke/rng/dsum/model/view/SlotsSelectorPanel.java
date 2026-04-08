package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.DsumPreset;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.model.OverworldMovementMode;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SlotsSelectorPanel extends JPanel {

    private final JComboBox<Game> gameCombo = new JComboBox<>(Game.values());
    private final JComboBox<Route> routesCombo = new JComboBox<>(Route.values());
    private final JPanel leadingToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JButton detailsToggle = new JButton("Details…");
    private final JButton setupButton = new JButton("Setup…");
    private final JPanel body = new JPanel();
    private final JPanel detailsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
    private final JPanel mainRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
    private final boolean[] detailsExpanded = {false};
    private volatile boolean compactChrome;

    private final JLabel modifierLabel;
    private final JLabel leadLabel;
    private final JCheckBox pikachu;
    private final JToggleButton soundMute;
    private JLabel movementModeLabel;
    private JPanel movementModeChip;
    private final JToggleButton[] movementModeButtons = new JToggleButton[OverworldMovementMode.values().length];
    private boolean suppressMovementCallbacks;

    private final SpinnerNumberModel modifierModel;
    private final SpinnerNumberModel leadModel;
    private final JSpinner modifierSpinner;
    private final JSpinner leadLevelSpinner;

    private final Consumer<OverworldMovementMode> onMovementModeChanged;
    private final Runnable onRefocusCalibrationSurface;
    private final Consumer<DsumPreset> onPresetApplied;

    public SlotsSelectorPanel(
            final Game game,
            final Route route,
            final boolean defaultPika,
            final OverworldMovementMode defaultMovementMode,
            final int initialLeadLevel,
            final Consumer<Game> onGameChanged,
            final Consumer<Route> onRouteChanged,
            final Consumer<Boolean> onPikachuChanged,
            final Consumer<Integer> onModifierChanged,
            final Consumer<Integer> onLeadLevelChanged,
            final Consumer<Boolean> onSoundMutedChanged,
            final Consumer<OverworldMovementMode> onMovementModeChanged,
            final Runnable onRefocusCalibrationSurface,
            final Consumer<DsumPreset> onPresetApplied) {

        this.onMovementModeChanged = onMovementModeChanged;
        this.onRefocusCalibrationSurface = onRefocusCalibrationSurface;
        this.onPresetApplied = onPresetApplied;

        modifierLabel = new JLabel("Mod:");
        modifierSpinner = new JSpinner();
        modifierLabel.setLabelFor(modifierSpinner);
        modifierLabel.setForeground(UiTheme.TEXT_MUTED);
        modifierLabel.setToolTipText("Overworld DSum cycle modifier");

        modifierModel = new SpinnerNumberModel(0, -150, 100, 10);
        modifierSpinner.setModel(modifierModel);
        modifierSpinner.addChangeListener(e -> onModifierChanged.accept((Integer) modifierModel.getValue()));
        if (modifierSpinner.getEditor() instanceof JSpinner.NumberEditor editor) {
            editor.getTextField().setColumns(3);
        }
        modifierSpinner.setPreferredSize(new Dimension(76, 32));
        modifierSpinner.setToolTipText("Change cycle length if you're seeming consistently ahead / behind (mostly Yellow)");

        pikachu = new JCheckBox();
        pikachu.setText("Pika lead");
        pikachu.setForeground(UiTheme.TEXT_PRIMARY);
        pikachu.setToolTipText("Pikachu in party lead (Yellow)");
        pikachu.setSelected(defaultPika);
        pikachu.addActionListener(e -> onPikachuChanged.accept(pikachu.isSelected()));

        leadLabel = new JLabel("Lead Lv:");
        leadLabel.setForeground(UiTheme.TEXT_MUTED);
        leadLevelSpinner = new JSpinner();
        leadLabel.setLabelFor(leadLevelSpinner);
        leadModel = new SpinnerNumberModel(initialLeadLevel, 1, 100, 1);
        leadLevelSpinner.setModel(leadModel);
        leadLevelSpinner.addChangeListener(e -> onLeadLevelChanged.accept((Integer) leadModel.getValue()));
        if (leadLevelSpinner.getEditor() instanceof JSpinner.NumberEditor leadEditor) {
            leadEditor.getTextField().setColumns(2);
        }
        leadLevelSpinner.setPreferredSize(new Dimension(64, 32));

        final Runnable refreshYellowDetailVisibility = () -> {
            final boolean yellowGame = gameCombo.getSelectedItem() == Game.YELLOW;
            pikachu.setVisible(yellowGame);
        };

        gameCombo.setSelectedItem(game);
        gameCombo.setRenderer(new RenamingListCellRenderer<>(Enum::name));
        gameCombo.putClientProperty(FlatClientProperties.STYLE_CLASS, "compact");
        gameCombo.setPreferredSize(new Dimension(108, 32));
        gameCombo.addActionListener(e -> {
            final Game newGame = (Game) gameCombo.getSelectedItem();
            onGameChanged.accept(newGame);
            refreshYellowDetailVisibility.run();
            if (newGame == Game.YELLOW) {
                routesCombo.removeItem(Route.SURFING);
            } else {
                routesCombo.removeItem(Route.SURFING);
                routesCombo.addItem(Route.SURFING);
            }
            SwingUtilities.invokeLater(onRefocusCalibrationSurface);
        });

        routesCombo.setSelectedItem(route);
        routesCombo.setRenderer(new RenamingListCellRenderer<>(r -> " " + r.name().replace("_", " ")));
        routesCombo.putClientProperty(FlatClientProperties.STYLE_CLASS, "compact");
        routesCombo.setPreferredSize(new Dimension(200, 32));
        routesCombo.addActionListener(e -> {
            onRouteChanged.accept((Route) routesCombo.getSelectedItem());
            SwingUtilities.invokeLater(onRefocusCalibrationSurface);
        });

        soundMute = new JToggleButton();
        soundMute.setIcon(new VolumeToolbarIcon(false));
        soundMute.setSelectedIcon(new VolumeToolbarIcon(true));
        soundMute.setForeground(UiTheme.TEXT_PRIMARY);
        soundMute.setToolTipText("Mute beeps and overlap hum");
        soundMute.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 2));
        soundMute.setContentAreaFilled(false);
        soundMute.setBorderPainted(false);
        soundMute.setFocusPainted(false);
        soundMute.setOpaque(false);
        soundMute.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        soundMute.getAccessibleContext().setAccessibleName("Sound");
        soundMute.addActionListener(e -> {
            final boolean muted = soundMute.isSelected();
            onSoundMutedChanged.accept(muted);
            soundMute.setToolTipText(muted ? "Unmute sound" : "Mute beeps and overlap hum");
        });

        setLayout(new BorderLayout(0, 6));
        setOpaque(true);
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.SELECTOR_DIVIDER),
                new EmptyBorder(12, 14, 14, 14)));

        leadingToolbar.setOpaque(false);

        final JPanel topStrip = new JPanel(new BorderLayout(0, 0));
        topStrip.setOpaque(false);
        topStrip.add(leadingToolbar, BorderLayout.WEST);
        final JPanel muteTrailing = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        muteTrailing.setOpaque(false);
        muteTrailing.add(soundMute);
        topStrip.add(muteTrailing, BorderLayout.EAST);

        mainRow.setOpaque(false);
        mainRow.add(gameCombo);
        mainRow.add(routesCombo);
        mainRow.add(buildMovementModeSelector(defaultMovementMode, onMovementModeChanged));

        detailsPanel.setOpaque(true);
        detailsPanel.setBackground(UiTheme.SURFACE_ALT);
        detailsPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        detailsPanel.setBorder(new EmptyBorder(10, 12, 12, 12));
        detailsPanel.add(leadLabel);
        detailsPanel.add(leadLevelSpinner);
        detailsPanel.add(modifierLabel);
        detailsPanel.add(modifierSpinner);
        detailsPanel.add(pikachu);

        final JLabel presetsLink = new JLabel("Presets");
        presetsLink.setForeground(UiTheme.ACCENT);
        presetsLink.setFont(presetsLink.getFont().deriveFont(Font.PLAIN, 13f));
        presetsLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        presetsLink.setToolTipText("Apply a bundled game / route / movement / slot setup");
        presetsLink.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                showPresetMenu(presetsLink);
            }
        });
        detailsPanel.add(presetsLink);
        detailsPanel.getAccessibleContext().setAccessibleName("Detailed settings");

        detailsToggle.setForeground(UiTheme.ACCENT);
        detailsToggle.setFont(detailsToggle.getFont().deriveFont(Font.PLAIN, 13f));
        detailsToggle.setBorder(new EmptyBorder(4, 6, 4, 6));
        detailsToggle.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        detailsToggle.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailsToggle.setHorizontalAlignment(SwingConstants.LEFT);
        detailsToggle.addActionListener(e -> onDetailsToggle());
        mainRow.add(detailsToggle);

        setupButton.setForeground(UiTheme.ACCENT);
        setupButton.setFont(setupButton.getFont().deriveFont(Font.PLAIN, 13f));
        setupButton.setBorder(new EmptyBorder(4, 6, 4, 6));
        setupButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        setupButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setupButton.setVisible(false);
        setupButton.addActionListener(e -> showSetupDialog(refreshYellowDetailVisibility));
        mainRow.add(setupButton);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        mainRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(mainRow);
        body.add(detailsPanel);

        detailsPanel.setVisible(false);

        add(topStrip, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);

        refreshYellowDetailVisibility.run();
        onMovementModeChanged.accept(defaultMovementMode);
    }

    private void showPresetMenu(final Component invoker) {
        final JPopupMenu menu = new JPopupMenu();
        for (final DsumPreset p : DsumPreset.values()) {
            menu.add(new AbstractAction(p.menuLabel()) {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    applyPreset(p);
                }
            });
        }
        menu.show(invoker, 0, invoker.getHeight());
    }

    private void applyPreset(final DsumPreset preset) {
        gameCombo.setSelectedItem(preset.game());
        routesCombo.setSelectedItem(preset.route());
        modifierModel.setValue(0);
        leadModel.setValue(preset.leadLevel());
        pikachu.setSelected(preset.pikaLead());
        applyMovementModeUi(preset.movementMode());
        onPresetApplied.accept(preset);
        SwingUtilities.invokeLater(onRefocusCalibrationSurface);
    }

    private void applyMovementModeUi(final OverworldMovementMode mode) {
        suppressMovementCallbacks = true;
        try {
            movementModeButtons[mode.ordinal()].setSelected(true);
        } finally {
            suppressMovementCallbacks = false;
        }
        onMovementModeChanged.accept(mode);
    }

    private JPanel buildMovementModeSelector(
            final OverworldMovementMode initial,
            final Consumer<OverworldMovementMode> onChanged) {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setOpaque(false);
        movementModeLabel = new JLabel("Move:");
        movementModeLabel.setForeground(UiTheme.TEXT_MUTED);
        movementModeLabel.setFont(movementModeLabel.getFont().deriveFont(Font.PLAIN, 12f));
        movementModeLabel.setToolTipText(
                "Step lag (0 / 9 / 17 frames): rotates the wheel artwork by that much DSum advance (opposite ring motion) so the ring matches after a step. Overlap timing and suggestions use the live counter.");
        row.add(movementModeLabel);

        final JPanel group = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        group.setOpaque(true);
        group.setBackground(UiTheme.SURFACE_ALT);
        group.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        group.setBorder(new EmptyBorder(4, 6, 4, 6));
        movementModeChip = group;

        final ButtonGroup bg = new ButtonGroup();
        for (final OverworldMovementMode m : OverworldMovementMode.values()) {
            final JToggleButton tb = new JToggleButton(new MovementModeIcon(m));
            tb.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            tb.setFocusable(false);
            tb.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            tb.setToolTipText(switch (m) {
                case CORNER_BONK -> "Corner bonking — 0 frame step lag";
                case BIKE -> "Bike — 9 frame step lag";
                case WALKING -> "Walking — 17 frame step lag";
            });
            tb.getAccessibleContext().setAccessibleName(switch (m) {
                case CORNER_BONK -> "Corner bonking movement";
                case BIKE -> "Bicycle movement";
                case WALKING -> "Walking movement";
            });
            bg.add(tb);
            group.add(tb);
            movementModeButtons[m.ordinal()] = tb;
            final OverworldMovementMode mode = m;
            tb.addActionListener(e -> {
                if (suppressMovementCallbacks) {
                    return;
                }
                if (tb.isSelected()) {
                    onChanged.accept(mode);
                }
            });
        }
        ((JToggleButton) group.getComponent(initial.ordinal())).setSelected(true);
        row.add(group);
        return row;
    }

    public void applyUiThemeColors() {
        setBackground(UiTheme.SURFACE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UiTheme.SELECTOR_DIVIDER),
                new EmptyBorder(12, 14, 14, 14)));
        detailsPanel.setBackground(UiTheme.SURFACE_ALT);
        modifierLabel.setForeground(UiTheme.TEXT_MUTED);
        leadLabel.setForeground(UiTheme.TEXT_MUTED);
        pikachu.setForeground(UiTheme.TEXT_PRIMARY);
        soundMute.setForeground(UiTheme.TEXT_PRIMARY);
        soundMute.repaint();
        detailsToggle.setForeground(UiTheme.ACCENT);
        setupButton.setForeground(UiTheme.ACCENT);
        movementModeLabel.setForeground(UiTheme.TEXT_MUTED);
        movementModeChip.setBackground(UiTheme.SURFACE_ALT);
        for (final Component c : movementModeChip.getComponents()) {
            c.repaint();
        }
    }

    public void addLeadingToolbar(final JComponent group) {
        leadingToolbar.add(group);
    }

    public void setCompactChrome(final boolean compact) {
        if (compactChrome == compact) {
            return;
        }
        compactChrome = compact;
        detailsToggle.setVisible(!compact);
        setupButton.setVisible(compact);
        if (compact) {
            detailsExpanded[0] = false;
            detailsToggle.setText("Details…");
            if (detailsPanel.getParent() == body) {
                body.remove(detailsPanel);
            }
        } else {
            if (detailsPanel.getParent() != null) {
                detailsPanel.getParent().remove(detailsPanel);
            }
            body.add(detailsPanel);
            detailsPanel.setVisible(detailsExpanded[0]);
        }
        final Window w = SwingUtilities.getWindowAncestor(this);
        if (w != null) {
            w.pack();
        } else {
            revalidate();
        }
    }

    private void onDetailsToggle() {
        detailsExpanded[0] = !detailsExpanded[0];
        detailsPanel.setVisible(detailsExpanded[0]);
        detailsToggle.setText(detailsExpanded[0] ? "Hide details" : "Details…");
        final Window w = SwingUtilities.getWindowAncestor(SlotsSelectorPanel.this);
        if (w != null) {
            w.pack();
        } else {
            revalidate();
        }
    }

    private void showSetupDialog(final Runnable refreshYellowDetailVisibility) {
        refreshYellowDetailVisibility.run();
        final Window owner = SwingUtilities.getWindowAncestor(this);
        final JDialog d = new JDialog(owner, "Encounter setup", Dialog.ModalityType.APPLICATION_MODAL);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        final JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBorder(new EmptyBorder(12, 14, 14, 14));
        if (detailsPanel.getParent() != null) {
            detailsPanel.getParent().remove(detailsPanel);
        }
        // Panel stays false from "Details…" collapsed state; must be true or the dialog looks empty.
        detailsPanel.setVisible(true);
        wrap.add(detailsPanel, BorderLayout.CENTER);
        final JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        final JButton ok = new JButton("Done");
        ok.addActionListener(ev -> d.dispose());
        actions.add(ok);
        wrap.add(actions, BorderLayout.SOUTH);
        d.setContentPane(wrap);
        d.pack();
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }

    private record RenamingListCellRenderer<T>(Function<T, String> renamer) implements ListCellRenderer<T> {

        @Override
            public Component getListCellRendererComponent(final JList<? extends T> list,
                                                          final T value,
                                                          final int index,
                                                          final boolean isSelected,
                                                          final boolean cellHasFocus) {
                final String valueString = value == null ? "" : renamer.apply(value);
                final JLabel label = new JLabel(valueString);
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
                return label;
            }
        }
}
