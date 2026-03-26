package com.com.poke.rng.dsum;

import com.com.poke.rng.dsum.model.view.ApplicationFrame;
import com.com.poke.rng.dsum.model.view.UiTheme;

public final class DsumApplication {

    public static void main(final String[] args) {
        boolean light = false;
        for (final String a : args) {
            if ("--light".equals(a) || "--theme=light".equals(a)) {
                light = true;
                break;
            }
        }
        UiTheme.install(light ? UiTheme.Appearance.LIGHT : UiTheme.Appearance.DARK);
        new ApplicationFrame().setVisible(true);
    }
}
