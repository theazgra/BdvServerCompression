package quantization.utilities;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

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
        return String.format("%dH %dMin %d Sec %d Ms", elapsed.toHoursPart(), elapsed.toMinutesPart(), elapsed.toSecondsPart(), elapsed.toMillisPart());
    }

    @Override
    public String toString() {
        return getElapsedTimeString();
    }
}
