package com.com.poke.rng.dsum.model.view;

import com.com.poke.rng.dsum.constants.EncounterSlot;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public final class TargetSlotPanel extends JPanel {

    private final JList<EncounterSlot> slotList;

    public TargetSlotPanel(final EncounterSlot initialTarget, final Consumer<List<EncounterSlot>> onTargetSelected) {
        super(new FlowLayout(FlowLayout.LEFT));

        slotList = new JList<>(EncounterSlot.values());
        slotList.setFocusable(false);
        slotList.setSelectedValue(initialTarget, true);
        slotList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
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

        slotList.addListSelectionListener(e -> {
            List<EncounterSlot> selectedSlots = slotList.getSelectedValuesList();
            if (selectedSlots != null && !selectedSlots.isEmpty()) {
                onTargetSelected.accept(selectedSlots);
            }
        });

        add(new JLabel("Target slot:"));
        final JScrollPane scroller = new JScrollPane(slotList);
        scroller.setPreferredSize(new Dimension(150, 60));
        scroller.setMaximumSize(new Dimension(150, 60));
        add(scroller);
    }
}
