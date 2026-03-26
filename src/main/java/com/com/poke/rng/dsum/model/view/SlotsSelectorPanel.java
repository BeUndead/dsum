package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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
    private boolean compactChrome;

    public SlotsSelectorPanel(
            final Game game,
            final Route route,
            final boolean defaultPika,
            final boolean defaultOnBike,
            final int initialLeadLevel,
            final Consumer<Game> onGameChanged,
            final Consumer<Route> onRouteChanged,
            final Consumer<Boolean> onPikachuChanged,
            final Consumer<Integer> onYellowModifierChanged,
            final Consumer<Integer> onLeadLevelChanged,
            final Consumer<Boolean> onSoundMutedChanged,
            final Consumer<Boolean> onOnBikeChanged) {

        final JLabel yellowModifierLabel = new JLabel("Mod:");
        final JSpinner yellowModifier = new JSpinner();
        yellowModifierLabel.setLabelFor(yellowModifier);
        yellowModifierLabel.setForeground(UiTheme.TEXT_MUTED);
        yellowModifierLabel.setToolTipText("Yellow overworld DSum cycle modifier");

        final SpinnerNumberModel spinModel = new SpinnerNumberModel(0, -100, 100, 10);
        yellowModifier.setModel(spinModel);
        yellowModifier.addChangeListener(e -> onYellowModifierChanged.accept((Integer) spinModel.getValue()));
        if (yellowModifier.getEditor() instanceof JSpinner.NumberEditor editor) {
            editor.getTextField().setColumns(3);
        }
        yellowModifier.setPreferredSize(new Dimension(76, 32));

        final JCheckBox pikachu = new JCheckBox();
        pikachu.setText("Pika lead");
        pikachu.setForeground(UiTheme.TEXT_PRIMARY);
        pikachu.setToolTipText("Pikachu in party lead (Yellow)");
        pikachu.setSelected(defaultPika);
        pikachu.addActionListener(e -> onPikachuChanged.accept(pikachu.isSelected()));

        final JLabel leadLabel = new JLabel("Lead Lv:");
        leadLabel.setForeground(UiTheme.TEXT_MUTED);
        final JSpinner leadLevelSpinner = new JSpinner();
        leadLabel.setLabelFor(leadLevelSpinner);
        final SpinnerNumberModel leadModel = new SpinnerNumberModel(initialLeadLevel, 1, 100, 1);
        leadLevelSpinner.setModel(leadModel);
        leadLevelSpinner.addChangeListener(e -> onLeadLevelChanged.accept((Integer) leadModel.getValue()));
        if (leadLevelSpinner.getEditor() instanceof JSpinner.NumberEditor leadEditor) {
            leadEditor.getTextField().setColumns(2);
        }
        leadLevelSpinner.setPreferredSize(new Dimension(64, 32));

        final JCheckBox onBikeCheck = new JCheckBox("On bike");
        onBikeCheck.setForeground(UiTheme.TEXT_PRIMARY);
        onBikeCheck.setToolTipText(
                "Suggested slots only: step delay %d frames on bike, %d on foot (Game Boy)."
                        .formatted(
                                EncounterWheelModel.SUGGESTION_STEP_LAG_FRAMES_BIKE,
                                EncounterWheelModel.SUGGESTION_STEP_LAG_FRAMES_FOOT));
        onBikeCheck.setSelected(defaultOnBike);
        onBikeCheck.addActionListener(e -> onOnBikeChanged.accept(onBikeCheck.isSelected()));

        final Runnable refreshYellowDetailVisibility = () -> {
            final boolean yellowGame = gameCombo.getSelectedItem() == Game.YELLOW;
            yellowModifierLabel.setVisible(yellowGame);
            yellowModifier.setVisible(yellowGame);
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
        });

        routesCombo.setSelectedItem(route);
        routesCombo.setRenderer(new RenamingListCellRenderer<>(r -> " " + r.name().replace("_", " ")));
        routesCombo.putClientProperty(FlatClientProperties.STYLE_CLASS, "compact");
        routesCombo.setPreferredSize(new Dimension(200, 32));
        routesCombo.addActionListener(e -> onRouteChanged.accept((Route) routesCombo.getSelectedItem()));

        final JToggleButton soundMute = new JToggleButton();
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

        detailsPanel.setOpaque(true);
        detailsPanel.setBackground(UiTheme.SURFACE_ALT);
        detailsPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        detailsPanel.setBorder(new EmptyBorder(10, 12, 12, 12));
        detailsPanel.add(onBikeCheck);
        detailsPanel.add(leadLabel);
        detailsPanel.add(leadLevelSpinner);
        detailsPanel.add(yellowModifierLabel);
        detailsPanel.add(yellowModifier);
        detailsPanel.add(pikachu);
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
        onOnBikeChanged.accept(onBikeCheck.isSelected());
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

    private static final class RenamingListCellRenderer<T> implements ListCellRenderer<T> {

        private final Function<T, String> renamer;

        private RenamingListCellRenderer(final Function<T, String> renamer) {
            this.renamer = renamer;
        }

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
