package com.com.poke.rng.dsum.model.view;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Shared chrome for segmented toolbar toggles (view layout, theme appearance, etc.). */
final class ToolbarSegmentedControl {

    private ToolbarSegmentedControl() {
    }

    static JPanel newChip() {
        final JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        chip.setOpaque(true);
        chip.setBackground(UiTheme.SURFACE_ALT);
        chip.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        chip.setBorder(new EmptyBorder(4, 6, 4, 6));
        return chip;
    }

    static void styleIconToggle(final AbstractButton b) {
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        b.setFocusable(false);
        b.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }
}
