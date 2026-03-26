package ext;

import javax.swing.*;
import java.awt.*;

/**
 * Paints a delegate {@link Icon} centered inside a fixed-width / fixed-height box. Unused when the
 * delegate is {@code null} (box still occupies space for consistent layout).
 */
public final class CenteredBoxIcon implements Icon {

    private final Icon delegate;
    private final int boxWidth;
    private final int boxHeight;

    public CenteredBoxIcon(final Icon delegate, final int boxWidth, final int boxHeight) {
        this.delegate = delegate;
        this.boxWidth = Math.max(0, boxWidth);
        this.boxHeight = Math.max(0, boxHeight);
    }

    @Override
    public int getIconWidth() {
        return boxWidth;
    }

    @Override
    public int getIconHeight() {
        return boxHeight;
    }

    @Override
    public void paintIcon(final Component c, final Graphics g, final int x, final int y) {
        if (delegate == null) {
            return;
        }
        final int iw = delegate.getIconWidth();
        final int ih = delegate.getIconHeight();
        final int px = x + (boxWidth - iw) / 2;
        final int py = y + (boxHeight - ih) / 2;
        delegate.paintIcon(c, g, px, py);
    }
}
