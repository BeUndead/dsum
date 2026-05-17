package com.com.dsum;

import com.com.dsum.ui.ApplicationFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class DsumApplication {

    public static void main(String[] args) {
        System.setProperty("flatlaf.useNativeLibrary", "false");
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (final UnsupportedLookAndFeelException ulafEx) {
            // Oh well.
        }

        new ApplicationFrame().setVisible(true);
    }

}
