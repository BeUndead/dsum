package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.audio.OverlapHumPlayer;
import com.com.poke.rng.dsum.constants.DsumPreset;
import com.com.poke.rng.dsum.constants.EncounterSlot;
import com.com.poke.rng.dsum.constants.Game;
import com.com.poke.rng.dsum.constants.Route;
import com.com.poke.rng.dsum.controller.EncounterWheelController;
import com.com.poke.rng.dsum.model.EncounterWheelModel;
import com.com.poke.rng.dsum.model.OverworldMovementMode;
import com.com.poke.rng.dsum.util.SpriteImageUtil;
import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.List;

public final class ApplicationFrame extends JFrame {

    private final JPanel themeAppearanceChip;
    private final JPanel viewLayoutChip;
    private final JPanel centerHost;
    private final JPanel rootContent;
    private final JLabel viewLabel;
    private final SlotsDisplayPanel slotsDisplayPanel;
    private final SlotsSelectorPanel slotsSelectorPanel;
    private final RouteSettingsBlindsHandle routeControlsHandle;

    private boolean routeControlsExpanded = true;
    private final EncounterWheel wheel;
    private final EncounterWheelBar wheelBar;
    private final EncounterWheelController wheelController;

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
                model::setOverworldMovementMode,
                wheelController::requestCalibrationSurfaceFocus,
                preset -> slotsDisplayPanel.applyPresetTargets(List.of(preset.targetSlot()))
        );

        final LeadingToolbarParts leadingToolbar = buildLeadingToolbar();
        viewLabel = leadingToolbar.viewLabel();
        viewLayoutChip = leadingToolbar.viewLayoutChip();
        themeAppearanceChip = leadingToolbar.themeAppearanceChip();
        slotsSelectorPanel.addLeadingToolbar(leadingToolbar.toolbarRow());
        // Single child so preferred height is not max(wheel, bar) — CardLayout would keep 500×500 with the bar.
        centerHost = new JPanel(new BorderLayout());
        centerHost.setOpaque(false);
        centerHost.add(wheel, BorderLayout.CENTER);

        rootContent = new JPanel(new BorderLayout());
        rootContent.setOpaque(true);
        rootContent.setBackground(UiTheme.SURFACE_ALT);
        final Border pad = BorderFactory.createEmptyBorder(12, 16, 16, 16);
        rootContent.setBorder(pad);

        routeControlsHandle = new RouteSettingsBlindsHandle(this::toggleRouteControlsExpansion);

        final JPanel routeControlsBundle = new JPanel();
        routeControlsBundle.setLayout(new BoxLayout(routeControlsBundle, BoxLayout.Y_AXIS));
        routeControlsBundle.setOpaque(false);
        routeControlsBundle.add(slotsSelectorPanel);

        final JPanel handleRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
        handleRow.setOpaque(false);
        handleRow.add(routeControlsHandle);
        routeControlsBundle.add(handleRow);

        final JPanel slots = new JPanel(new BorderLayout());
        slots.setOpaque(false);
        slots.add(routeControlsBundle, BorderLayout.NORTH);
        slots.add(slotsDisplayPanel, BorderLayout.SOUTH);
        rootContent.add(slots, BorderLayout.NORTH);
        rootContent.add(centerHost, BorderLayout.CENTER);

        add(rootContent);

        setTitle("DSum");
        pack();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationByPlatform(true);

        setIconImage(SpriteImageUtil.loadWithTransparentBackground(getClass()
                .getResource("/sprites/rb/27.png")).getImage());

        SwingUtilities.invokeLater(wheel::requestFocusInWindow);
        wheelController.start();
    }

    private void toggleRouteControlsExpansion() {
        routeControlsExpanded = !routeControlsExpanded;
        slotsSelectorPanel.setVisible(routeControlsExpanded);
        routeControlsHandle.setExpanded(routeControlsExpanded);
        slotsSelectorPanel.getParent().revalidate();
        SwingUtilities.invokeLater(this::pack);
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
        routeControlsHandle.repaint();
        slotsSelectorPanel.applyUiThemeColors();
        slotsDisplayPanel.applyUiThemeColors();
        repaintToolbarIconSubtree(themeAppearanceChip);
        repaintToolbarIconSubtree(viewLayoutChip);
        repaint();
    }

    private record LeadingToolbarParts(
            JPanel toolbarRow,
            JLabel viewLabel,
            JPanel viewLayoutChip,
            JPanel themeAppearanceChip) {}

    private LeadingToolbarParts buildLeadingToolbar() {
        final JPanel viewPick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        viewPick.setOpaque(false);
        final JLabel viewLabel = new JLabel("View:");
        viewLabel.setForeground(UiTheme.TEXT_MUTED);
        viewPick.add(viewLabel);

        final JPanel viewLayoutChip = ToolbarSegmentedControl.newChip();
        final ButtonGroup viewLayoutGroup = new ButtonGroup();
        final JToggleButton viewFull = new JToggleButton(new ViewLayoutIcon(ViewLayoutIcon.Kind.FULL));
        final JToggleButton viewCompact = new JToggleButton(new ViewLayoutIcon(ViewLayoutIcon.Kind.COMPACT));
        for (final JToggleButton tb : new JToggleButton[] {viewFull, viewCompact}) {
            ToolbarSegmentedControl.styleIconToggle(tb);
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

        final JPanel themeAppearanceChip = ToolbarSegmentedControl.newChip();
        final ButtonGroup appearanceGroup = new ButtonGroup();
        final JToggleButton appearanceLight = new JToggleButton(new ThemeAppearanceIcon(ThemeAppearanceIcon.Kind.SUN));
        final JToggleButton appearanceDark = new JToggleButton(new ThemeAppearanceIcon(ThemeAppearanceIcon.Kind.MOON));
        for (final JToggleButton tb : new JToggleButton[] {appearanceLight, appearanceDark}) {
            ToolbarSegmentedControl.styleIconToggle(tb);
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
        ToolbarSegmentedControl.styleIconToggle(backgroundInputToggle);
        backgroundInputToggle.setToolTipText(
                "When on, space and calibration keys work even when this window is not focused. "
                        + "When off, keys are handled on the main window unless typing in a field or a menu is open. "
                        + "P pauses / unpauses the wheel.");
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

        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.add(viewPick);
        row.add(themeAppearanceChip);
        row.add(backgroundInputToggle);
        return new LeadingToolbarParts(row, viewLabel, viewLayoutChip, themeAppearanceChip);
    }

    private static void repaintToolbarIconSubtree(final Container root) {
        for (final Component c : root.getComponents()) {
            c.repaint();
            if (c instanceof Container co) {
                repaintToolbarIconSubtree(co);
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
