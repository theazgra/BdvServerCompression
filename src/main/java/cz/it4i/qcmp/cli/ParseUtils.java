package cz.it4i.qcmp.cli;

import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;

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
        } catch (final NumberFormatException ignored) {
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
        return tryParseV2i(rangeString, delimiter).map(v2i -> new Range<>(v2i.getX(), v2i.getY()));
    }


    /**
     * Try to parse two dimensional vector from string.
     *
     * @param v2iString Vector string.
     * @param delimiter Delimiter between numbers.
     * @return Optional parsed vector.
     */
    public static Optional<V2i> tryParseV2i(final String v2iString, final char delimiter) {
        final String string = removeSpacesInString(v2iString);
        final int delimiterIndex = string.indexOf(delimiter);
        if (delimiterIndex == -1) {
            return Optional.empty();
        }

        final Optional<Integer> maybeN1 = tryParseInt(string.substring(0, delimiterIndex));
        final Optional<Integer> maybeN2 = tryParseInt(string.substring(delimiterIndex + 1));

        if (maybeN1.isPresent() && maybeN2.isPresent()) {
            return Optional.of(new V2i(maybeN1.get(), maybeN2.get()));
        }
        return Optional.empty();
    }

    /**
     * Try to parse three dimensional vector from string.
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
