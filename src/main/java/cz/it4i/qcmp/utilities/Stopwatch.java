package cz.it4i.qcmp.utilities;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class Stopwatch {

    private final String name;
    private boolean isMeasuring = false;
    private long startTick;
    private long elapsedTicks;

    @NotNull
    public static Stopwatch startNew(final String name) {
        final Stopwatch stopwatch = new Stopwatch(name);
        stopwatch.start();
        return stopwatch;
    }

    @NotNull
    public static Stopwatch startNew() {
        return startNew(null);
    }

    public Stopwatch(final String name) {
        this.name = name;
    }

    public Stopwatch() {
        name = null;
    }

    public void start() {
        isMeasuring = true;
        startTick = System.nanoTime();
    }

    public void stop() {
        final long endTick = System.nanoTime();
        isMeasuring = false;
        elapsedTicks += endTick - startTick;
    }

    public void reset() {
        isMeasuring = false;
        elapsedTicks = 0;
    }

    public void restart() {
        isMeasuring = true;
        elapsedTicks = 0;
        startTick = System.nanoTime();
    }

    public long getElapsedInUnit(final TimeUnit timeUnit) {
        return timeUnit.convert(elapsedTicks, TimeUnit.NANOSECONDS);
    }

    public String getElapsedTimeString() {
        if (isMeasuring || (elapsedTicks == 0)) {
            return "No time measured yet.";
        }
        double MS = (double) getElapsedInUnit(TimeUnit.MILLISECONDS);
        double M = 0;
        double S = 0;
        double H = 0;

        S = MS / 1000.0;
        final double fS = Math.floor(S);
        MS = (S - fS) * 1000.0;

        M = fS / 60.0;
        final double fM = Math.floor(M);
        S = (M - fM) * 60.0;

        H = fM / 60.0;
        final double fH = Math.floor(H);
        M = (H - fH) * 60.0;

        H = fH;

        if (H > 0) {
            return String.format("%dH %dmin %dsec %dms", (long) H, (long) M, (long) S, (long) MS);
        } else if (M > 0) {
            return String.format("%dmin %dsec %dms", (long) M, (long) S, (long) MS);
        } else if (S > 0) {
            return String.format("%dsec %dms", (long) S, (long) MS);
        } else {
            return String.format("%dms", (long) MS);
        }
    }

    @Override
    public String toString() {
        if (name != null) {
            return name + ": " + getElapsedTimeString();
        }
        return getElapsedTimeString();
    }
}
