package azgracompress.cli;

import azgracompress.compression.Range;
import azgracompress.data.V3i;

import java.util.Optional;

public abstract class ParseUtils {

    private static String removeSpacesInString(final String string) {
        return string.replaceAll("\\s", "");
    }

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
     * Try to parse integer range from string.
     *
     * @param rangeString Range string.
     * @param delimiter   Delimiter between numbers.
     * @return Optional parsed range.
     */
    public static Optional<Range<Integer>> tryParseRange(final String rangeString, final char delimiter) {
        final String string = removeSpacesInString(rangeString);
        final int delimiterIndex = string.indexOf(delimiter);
        if (delimiterIndex == -1) {
            return Optional.empty();
        }

        final Optional<Integer> maybeFrom = tryParseInt(string.substring(0, delimiterIndex));
        final Optional<Integer> maybeTo = tryParseInt(string.substring(delimiterIndex + 1));

        if (maybeFrom.isPresent() && maybeTo.isPresent()) {
            return Optional.of(new Range<>(maybeFrom.get(), maybeTo.get()));
        }
        return Optional.empty();
    }

    /**
     * Try to parse 3 dimensional vector from string.
     *
     * @param v3iString Vector string.
     * @param delimiter Delimiter between numbers.
     * @return Optional parsed vector.
     */
    public static Optional<V3i> tryParseV3i(final String v3iString, final char delimiter) {
        final String string = removeSpacesInString(v3iString);
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
