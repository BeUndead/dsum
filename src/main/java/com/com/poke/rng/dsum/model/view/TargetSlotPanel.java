package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.EncounterSlot;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class TargetSlotPanel extends JPanel {

    private final JComboBox<EncounterSlot> slotCombo;

    public TargetSlotPanel(final EncounterSlot initialTarget, final Consumer<EncounterSlot> onTargetSelected) {
        super(new FlowLayout(FlowLayout.LEFT));

        slotCombo = new JComboBox<>(EncounterSlot.values());
        slotCombo.setFocusable(false);
        slotCombo.setSelectedItem(initialTarget);
        slotCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            final JLabel label = new JLabel(value == null ? "" : "Slot " + value.name().replace("_", ""));
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(list.getSelectionBackground());
                label.setForeground(list.getSelectionForeground());
            } else {
                label.setBackground(list.getBackground());
                label.setForeground(list.getForeground());
            }

            return label;
        });

        slotCombo.addActionListener(e -> {
            EncounterSlot selected = (EncounterSlot) slotCombo.getSelectedItem();
            if (selected != null) {
                onTargetSelected.accept(selected);
            }
        });

        add(new JLabel("Target slot:"));
        add(slotCombo);
    }

    public EncounterSlot getSelectedSlot() {
        return (EncounterSlot) slotCombo.getSelectedItem();
    }
}
