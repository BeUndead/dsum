package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.controller.EncounterWheelController;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.model.OverworldMovementMode;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class ApplicationFrame extends JFrame {

    private JPanel themeAppearanceChip;
    private JPanel viewLayoutChip;
    private JPanel centerHost;
    private JPanel rootContent;
    private JLabel viewLabel;
    private SlotsDisplayPanel slotsDisplayPanel;
    private SlotsSelectorPanel slotsSelectorPanel;
    private EncounterWheel wheel;
    private EncounterWheelBar wheelBar;
    private EncounterWheelController wheelController;

    public ApplicationFrame() {
        final Game initialGame = Game.RED;
        final Route initialRoute = Route.SAFARI_ZONE_CENTER;
        final EncounterSlot initialTarget = EncounterSlot._9;
        final boolean initialPika = true;
        final OverworldMovementMode initialMovementMode = OverworldMovementMode.BIKE;
        final int initialLeadLevel = 70;

        final EncounterWheelModel model = new EncounterWheelModel(initialTarget, initialGame);
        model.setRoute(initialRoute);
        wheel = new EncounterWheel(model);
        wheelBar = new EncounterWheelBar(model);

        slotsDisplayPanel = new SlotsDisplayPanel(initialGame, initialRoute, initialTarget, model::setTargetSlots);

        wheelController =
                new EncounterWheelController(model, new OverlapHumPlayer(), slotsDisplayPanel::setSuggestedSlots, wheel, wheelBar);
        model.setOnTargetSlotsChanged(wheelController::notePostConfiguration);
        slotsSelectorPanel = new SlotsSelectorPanel(
                initialGame, initialRoute, initialPika, initialMovementMode,
                initialLeadLevel,
                game -> {
                    slotsDisplayPanel.setGame(game);
                    model.setGame(game);
                },
                newRoute -> {
                    slotsDisplayPanel.setRoute(newRoute);
                    model.setRoute(newRoute);
                },
                model::setPikaLead,
                model::modifyYellowOverworldDsumCycleModifier,
                model::setLeadLevel,
                wheelController::setSoundMuted,
                model::setOverworldMovementMode
        );

        final JPanel viewPick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        viewPick.setOpaque(false);
        viewLabel = new JLabel("View:");
        viewLabel.setForeground(UiTheme.TEXT_MUTED);
        viewPick.add(viewLabel);

        viewLayoutChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        viewLayoutChip.setOpaque(true);
        viewLayoutChip.setBackground(UiTheme.SURFACE_ALT);
        viewLayoutChip.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        viewLayoutChip.setBorder(new EmptyBorder(4, 6, 4, 6));
        final ButtonGroup viewLayoutGroup = new ButtonGroup();
        final JToggleButton viewFull = new JToggleButton(new ViewLayoutIcon(ViewLayoutIcon.Kind.FULL));
        final JToggleButton viewCompact = new JToggleButton(new ViewLayoutIcon(ViewLayoutIcon.Kind.COMPACT));
        for (final JToggleButton tb : new JToggleButton[] {viewFull, viewCompact}) {
            tb.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            tb.setFocusable(false);
            tb.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
        viewFull.setToolTipText("Full view — large encounter wheel");
        viewFull.getAccessibleContext().setAccessibleName("Full view");
        viewCompact.setToolTipText("Compact view — horizontal strip");
        viewCompact.getAccessibleContext().setAccessibleName("Compact view");
        viewLayoutGroup.add(viewFull);
        viewLayoutGroup.add(viewCompact);
        viewLayoutChip.add(viewFull);
        viewLayoutChip.add(viewCompact);
        viewFull.setSelected(true);
        viewFull.addActionListener(e -> {
            if (viewFull.isSelected()) {
                applyViewLayout(false);
            }
        });
        viewCompact.addActionListener(e -> {
            if (viewCompact.isSelected()) {
                applyViewLayout(true);
            }
        });
        viewPick.add(viewLayoutChip);

        themeAppearanceChip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        themeAppearanceChip.setOpaque(true);
        themeAppearanceChip.setBackground(UiTheme.SURFACE_ALT);
        themeAppearanceChip.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        themeAppearanceChip.setBorder(new EmptyBorder(4, 6, 4, 6));
        final ButtonGroup appearanceGroup = new ButtonGroup();
        final JToggleButton appearanceLight = new JToggleButton(new ThemeAppearanceIcon(ThemeAppearanceIcon.Kind.SUN));
        final JToggleButton appearanceDark = new JToggleButton(new ThemeAppearanceIcon(ThemeAppearanceIcon.Kind.MOON));
        for (final JToggleButton tb : new JToggleButton[] {appearanceLight, appearanceDark}) {
            tb.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
            tb.setFocusable(false);
            tb.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        }
        appearanceLight.setToolTipText("Light appearance");
        appearanceLight.getAccessibleContext().setAccessibleName("Light appearance");
        appearanceDark.setToolTipText("Dark appearance");
        appearanceDark.getAccessibleContext().setAccessibleName("Dark appearance");
        appearanceGroup.add(appearanceLight);
        appearanceGroup.add(appearanceDark);
        themeAppearanceChip.add(appearanceLight);
        themeAppearanceChip.add(appearanceDark);
        if (UiTheme.isDark()) {
            appearanceDark.setSelected(true);
        } else {
            appearanceLight.setSelected(true);
        }
        appearanceLight.addActionListener(e -> {
            if (appearanceLight.isSelected()) {
                applyAppearance(UiTheme.Appearance.LIGHT);
            }
        });
        appearanceDark.addActionListener(e -> {
            if (appearanceDark.isSelected()) {
                applyAppearance(UiTheme.Appearance.DARK);
            }
        });

        final JToggleButton backgroundInputToggle = new JToggleButton("Background");
        backgroundInputToggle.setSelected(false);
        backgroundInputToggle.setFont(backgroundInputToggle.getFont().deriveFont(Font.PLAIN, 12f));
        backgroundInputToggle.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        backgroundInputToggle.setFocusable(false);
        backgroundInputToggle.setToolTipText(
                "When on, space and calibration keys work even when this window is not focused. "
                        + "When off, keys only work while the wheel or strip has focus.");
        backgroundInputToggle.getAccessibleContext().setAccessibleName("Background key capture");
        backgroundInputToggle.addActionListener(e -> {
            final boolean on = backgroundInputToggle.isSelected();
            try {
                wheelController.setBackgroundKeyboardCapture(on);
            } catch (final RuntimeException ex) {
                backgroundInputToggle.setSelected(false);
                JOptionPane.showMessageDialog(
                        ApplicationFrame.this,
                        "Could not enable global key capture:\n" + ex.getMessage(),
                        "Background input",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        final JPanel viewChrome = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        viewChrome.setOpaque(false);
        viewChrome.add(viewPick);
        viewChrome.add(themeAppearanceChip);
        viewChrome.add(backgroundInputToggle);
        slotsSelectorPanel.addLeadingToolbar(viewChrome);
        // Single child so preferred height is not max(wheel, bar) — CardLayout would keep 500×500 with the bar.
        centerHost = new JPanel(new BorderLayout());
        centerHost.setOpaque(false);
        centerHost.add(wheel, BorderLayout.CENTER);

        rootContent = new JPanel(new BorderLayout());
        rootContent.setOpaque(true);
        rootContent.setBackground(UiTheme.SURFACE_ALT);
        final Border pad = BorderFactory.createEmptyBorder(12, 16, 16, 16);
        rootContent.setBorder(pad);

        final JPanel slots = new JPanel(new BorderLayout());
        slots.setOpaque(false);
        slots.add(slotsDisplayPanel, BorderLayout.SOUTH);
        slots.add(slotsSelectorPanel, BorderLayout.NORTH);
        rootContent.add(slots, BorderLayout.NORTH);
        rootContent.add(centerHost, BorderLayout.CENTER);

        add(rootContent);

        setTitle("DSum");
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);

        SwingUtilities.invokeLater(wheel::requestFocusInWindow);
        wheelController.start();
    }

    private void applyAppearance(final UiTheme.Appearance appearance) {
        if (UiTheme.getAppearance() == appearance) {
            return;
        }
        UiTheme.install(appearance);
        SwingUtilities.updateComponentTreeUI(this);
        rootContent.setBackground(UiTheme.SURFACE_ALT);
        viewLabel.setForeground(UiTheme.TEXT_MUTED);
        themeAppearanceChip.setBackground(UiTheme.SURFACE_ALT);
        viewLayoutChip.setBackground(UiTheme.SURFACE_ALT);
        wheel.setBackground(UiTheme.SURFACE);
        wheelBar.setBackground(UiTheme.SURFACE);
        slotsSelectorPanel.applyUiThemeColors();
        slotsDisplayPanel.applyUiThemeColors();
        repaintThemeToolbarIcons(themeAppearanceChip);
        repaintThemeToolbarIcons(viewLayoutChip);
        repaint();
    }

    private static void repaintThemeToolbarIcons(final Container root) {
        for (final Component c : root.getComponents()) {
            c.repaint();
            if (c instanceof Container co) {
                repaintThemeToolbarIcons(co);
            }
        }
    }

    private void applyViewLayout(final boolean compact) {
        centerHost.removeAll();
        if (compact) {
            wheelBar.setStripMetrics(
                    EncounterWheel.diameterForGeomScale(EncounterWheel.COMPACT_GEOM_SCALE),
                    SlotsDisplayPanel.COMPACT_PREFERRED_HEIGHT);
            centerHost.add(wheelBar, BorderLayout.CENTER);
        } else {
            wheelBar.setStripMetrics(500, EncounterWheelBar.DEFAULT_PREFERRED_HEIGHT);
            centerHost.add(wheel, BorderLayout.CENTER);
        }
        centerHost.revalidate();
        slotsDisplayPanel.setCompact(compact);
        slotsSelectorPanel.setCompactChrome(compact);
        SwingUtilities.invokeLater(() -> {
            pack();
            (compact ? wheelBar : wheel).requestFocusInWindow();
        });
    }
}
