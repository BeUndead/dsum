package com.com.poke.rng.dsum.audio;

import javax.sound.sampled.*;

public final class OverlapHumPlayer {

    private static final int SAMPLE_RATE = 44_100;
    private static final int FREQUENCY_HZ = 440;
    private static final int AMPLITUDE = 2_000;

    private volatile boolean running;
    private Thread thread;

    public void start() {
        if (thread != null && thread.isAlive() && running) {
            return;
        }

        running = true;
        thread = new Thread(this::runHum, "overlap-hum");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    private void runHum() {
        final AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        SourceDataLine line = null;

        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                return;
            }

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            final int bufferSamples = SAMPLE_RATE / 20;
            final byte[] buffer = new byte[bufferSamples * 2];
            final double phaseIncrement = 2 * Math.PI * FREQUENCY_HZ / SAMPLE_RATE;
            double phase = 0.0;

            while (running) {
                for (int i = 0; i < bufferSamples; i++) {
                    final int sample = (int) (AMPLITUDE * Math.sin(phase));
                    phase += phaseIncrement;
                    buffer[i * 2] = (byte) (sample & 0xFF);
                    buffer[i * 2 + 1] = (byte) (sample >> 8);
                }

                if (line.isOpen()) {
                    line.write(buffer, 0, buffer.length);
                }
            }
        } catch (final LineUnavailableException ignored) {
        } finally {
            if (line != null && line.isOpen()) {
                line.stop();
                line.close();
            }
        }
    }
}
