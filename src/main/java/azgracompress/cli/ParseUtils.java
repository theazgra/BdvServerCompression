package azgracompress.cli;

import azgracompress.data.V3i;

import java.util.Optional;

public abstract class ParseUtils {
    /**
     * Try to parse int from string.
     *
     * @param string Possible integer value.
     * @return Parse result.
     */
    public static Optional<Integer> tryParseInt(final String string) {
        try {
            return Optional.of(Integer.parseInt(string));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Try to parse 3 dimensional vector from string.
     *
     * @param string    Vector string.
     * @param delimiter Delimiter between numbers.
     * @return Optional parse result.
     */
    public static Optional<V3i> tryParseV3i(final String string, final char delimiter) {
        final int firstDelimiterIndex = string.indexOf(delimiter);
        if (firstDelimiterIndex == -1) {
            return Optional.empty();
        }

        final String num1String = string.substring(0, firstDelimiterIndex);
        final String secondPart = string.substring(firstDelimiterIndex + 1);

        final int secondDelimiterIndex = secondPart.indexOf(delimiter);
        if (secondDelimiterIndex == -1) {
            return Optional.empty();
        }

        final String num2String = secondPart.substring(0, secondDelimiterIndex);
        final String num3String = secondPart.substring(secondDelimiterIndex + 1);

        final Optional<Integer> maybeN1 = tryParseInt(num1String);
        final Optional<Integer> maybeN2 = tryParseInt(num2String);
        final Optional<Integer> maybeN3 = tryParseInt(num3String);

        if (maybeN1.isPresent() && maybeN2.isPresent() && maybeN3.isPresent()) {
            return Optional.of(new V3i(maybeN1.get(), maybeN2.get(), maybeN3.get()));
        }
        return Optional.empty();
    }
}
