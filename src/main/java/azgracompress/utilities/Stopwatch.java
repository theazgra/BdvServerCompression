package azgracompress.utilities;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch {

    private final String name;
    private Instant start;
    private Instant end;
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
        end = Instant.now();
        elapsed = Duration.between(start, end);
    }

    public void restart() {
        start = Instant.now();
    }

    public long totalElapsedNanoseconds() {
        return elapsed.toNanos();
    }

    public long totalElapsedMilliseconds() {
        return elapsed.toMillis();
    }

    public long totalElapsedSeconds() {
        return elapsed.toSeconds();
    }

    public String getElapsedTimeString() {
        if (elapsed == null) {
            return "No time measured yet.";
        }
        return String.format("%dH %dmin %dsec %dms %dns",
                             elapsed.toHoursPart(),
                             elapsed.toMinutesPart(),
                             elapsed.toSecondsPart(),
                             elapsed.toMillisPart(),
                             elapsed.toNanosPart());

    }

    @Override
    public String toString() {
        if (name != null) {
            return name + ": " + getElapsedTimeString();
        }
        return getElapsedTimeString();
    }
}
