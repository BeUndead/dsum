package com.com.dsum.ui;

import com.com.dsum.model.SelectionsConfig;
import com.com.dsum.sim.DSumDriver;
import com.com.dsum.sim.DSumSlotComputer;
import com.com.dsum.sim.DSumTracker;
import com.com.dsum.sim.ToolTipProvider;
import com.com.dsum.ui.util.KeyManager;
import com.com.dsum.util.Colours;
import com.com.dsum.util.SpriteImageUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ApplicationFrame extends JFrame {

    public ApplicationFrame() {
        super("DSum");

        final DSumTracker tracker = new DSumTracker();
        final SelectionsConfig config = new SelectionsConfig();
        final DSumSlotComputer slotCompute = new DSumSlotComputer(config);
        final DSumDriver driver = new DSumDriver(tracker, slotCompute, config);
        final Colours colours = new Colours(config);
        final ToolTipProvider toolTipProvider = new ToolTipProvider(driver, config);

        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();

        final ConfigPanel configPanel = new ConfigPanel(config);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 1.0;
        c.weighty = 0.0;
        add(configPanel, c);

        final TargetSlotsPanel targetSlotsPanel = new TargetSlotsPanel(config, driver, colours);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 1.0;
        c.weighty = 0.0;
        add(targetSlotsPanel, c);

        final DSumWheelPanel dsumWheelPanel = new DSumWheelPanel(config, driver, tracker, slotCompute, colours, toolTipProvider);
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 1.0;
        c.weighty = 1.0;
        dsumWheelPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dsumWheelPanel.requestFocusInWindow();
            }
        });
        add(dsumWheelPanel, c);

        pack();

        setLocationByPlatform(true);

        setIconImage(SpriteImageUtil.loadWithTransparentBackground(getClass()
                .getResource("/sprites/rb/27.png")).getImage());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        KeyManager.install(dsumWheelPanel, driver);

        driver.resume();
    }
}
