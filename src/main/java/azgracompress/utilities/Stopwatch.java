package azgracompress.utilities;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch {

    private final String name;
    private Instant start;
    Duration elapsed;

    @NotNull
    public static Stopwatch startNew(final String name) {
        Stopwatch stopwatch = new Stopwatch(name);
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
        start = Instant.now();
    }

    public void stop() {
        final Instant end = Instant.now();
        elapsed = Duration.between(start, end);
    }

    public void restart() {
        elapsed = null;
        start = Instant.now();
    }

    public long totalElapsedNanoseconds() {
        return elapsed.toNanos();
    }

    public long totalElapsedMilliseconds() {
        return elapsed.toMillis();
    }

    public double totalElapsedSeconds() {
        return (elapsed.toNanos() / 1_000_000_000.0);
    }

    public String getElapsedTimeString() {
        if (elapsed == null) {
            return "No time measured yet.";
        }
        double MS = (double) elapsed.toMillis();
        double M = 0;
        double S = 0;
        double H = 0;

        S = MS / 1000.0;
        double fS = Math.floor(S);
        MS = (S - fS) * 1000.0;

        M = fS / 60.0;
        double fM = Math.floor(M);
        S = (M - fM) * 60.0;

        H = fM / 60.0;
        double fH = Math.floor(H);
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
