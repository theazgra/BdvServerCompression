package azgracompress.utilities;

import java.time.Duration;
import java.time.Instant;

public class Stopwatch {

    private Instant start;
    private Instant end;
    Duration elapsed;

    public Stopwatch() {
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
        return String.format("%dH %dmin %dsec %dms %dns", elapsed.toHoursPart(), elapsed.toMinutesPart(), elapsed.toSecondsPart(), elapsed.toMillisPart(), elapsed.toNanosPart());

    }

    @Override
    public String toString() {
        return getElapsedTimeString();
    }
}
