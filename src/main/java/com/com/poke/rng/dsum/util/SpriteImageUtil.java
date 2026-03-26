package com.com.poke.rng.dsum.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayDeque;

/**
 * Loads PNG sprites and removes a uniform light backdrop (e.g. white) by flood-filling from the image edges.
 * Pixels that match the corner color within a tolerance and are connected to the border become transparent, so
 * white details on the Pokémon that do not touch the outer frame stay opaque.
 */
public final class SpriteImageUtil {

    private static final int BG_TOLERANCE = 40;

    private SpriteImageUtil() {
    }

    public static ImageIcon loadWithTransparentBackground(final URL resource) {
        if (resource == null) {
            return null;
        }
        try (InputStream in = resource.openStream()) {
            final BufferedImage decoded = ImageIO.read(in);
            if (decoded == null) {
                return new ImageIcon(resource);
            }
            final int w = decoded.getWidth();
            final int h = decoded.getHeight();
            final BufferedImage argb = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = argb.createGraphics();
            try {
                g.drawImage(decoded, 0, 0, null);
            } finally {
                g.dispose();
            }
            stripEdgeConnectedBackground(argb);
            return new ImageIcon(argb);
        } catch (IOException e) {
            return new ImageIcon(resource);
        }
    }

    private static void stripEdgeConnectedBackground(final BufferedImage im) {
        final int w = im.getWidth();
        final int h = im.getHeight();
        final int[] px = im.getRGB(0, 0, w, h, null, 0, w);

        final int c0 = px[0];
        final int c1 = px[w - 1];
        final int c2 = px[(h - 1) * w];
        final int c3 = px[h * w - 1];
        final int ref = cornersAgreeOnBackground(c0, c1, c2, c3) ? averageArgb(c0, c1, c2, c3) : c0;
        final int br = (ref >> 16) & 0xFF;
        final int bg = (ref >> 8) & 0xFF;
        final int bb = ref & 0xFF;

        final boolean[] queued = new boolean[w * h];
        final ArrayDeque<Integer> dq = new ArrayDeque<>();

        for (int x = 0; x < w; x++) {
            offerIfBackground(px, queued, dq, w, h, x, 0, br, bg, bb);
            offerIfBackground(px, queued, dq, w, h, x, h - 1, br, bg, bb);
        }
        for (int y = 0; y < h; y++) {
            offerIfBackground(px, queued, dq, w, h, 0, y, br, bg, bb);
            offerIfBackground(px, queued, dq, w, h, w - 1, y, br, bg, bb);
        }

        while (!dq.isEmpty()) {
            final int i = dq.poll();
            px[i] = 0;
            final int x = i % w;
            final int y = i / w;
            if (x > 0) {
                offerIfBackground(px, queued, dq, w, h, x - 1, y, br, bg, bb);
            }
            if (x + 1 < w) {
                offerIfBackground(px, queued, dq, w, h, x + 1, y, br, bg, bb);
            }
            if (y > 0) {
                offerIfBackground(px, queued, dq, w, h, x, y - 1, br, bg, bb);
            }
            if (y + 1 < h) {
                offerIfBackground(px, queued, dq, w, h, x, y + 1, br, bg, bb);
            }
        }
        im.setRGB(0, 0, w, h, px, 0, w);
    }

    private static void offerIfBackground(
            final int[] px,
            final boolean[] queued,
            final ArrayDeque<Integer> dq,
            final int w,
            final int h,
            final int x,
            final int y,
            final int br,
            final int bg,
            final int bb) {
        final int i = y * w + x;
        if (queued[i]) {
            return;
        }
        if (!matchesBackground(px[i], br, bg, bb)) {
            return;
        }
        queued[i] = true;
        dq.add(i);
    }

    private static boolean cornersAgreeOnBackground(final int c0, final int c1, final int c2, final int c3) {
        return pixelsSimilar(c0, c1) && pixelsSimilar(c0, c2) && pixelsSimilar(c0, c3);
    }

    private static int averageArgb(final int... colors) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (final int c : colors) {
            r += (c >> 16) & 0xFF;
            g += (c >> 8) & 0xFF;
            b += c & 0xFF;
        }
        final int n = colors.length;
        return 0xFF000000 | ((r / n) << 16) | ((g / n) << 8) | (b / n);
    }

    private static boolean pixelsSimilar(final int a, final int b) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF)) <= BG_TOLERANCE
                && Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) <= BG_TOLERANCE
                && Math.abs((a & 0xFF) - (b & 0xFF)) <= BG_TOLERANCE;
    }

    private static boolean matchesBackground(final int argb, final int br, final int bg, final int bb) {
        final int r = (argb >> 16) & 0xFF;
        final int g = (argb >> 8) & 0xFF;
        final int b = argb & 0xFF;
        return Math.abs(r - br) <= BG_TOLERANCE
                && Math.abs(g - bg) <= BG_TOLERANCE
                && Math.abs(b - bb) <= BG_TOLERANCE;
    }
}
