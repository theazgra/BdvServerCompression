package cz.it4i.qcmp.cli;

import cz.it4i.qcmp.data.HyperStackDimensions;
import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;

import java.util.ArrayList;
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

    /**
     * Find all occurrences of c in string.
     *
     * @param string String to look for c in.
     * @param c      Characted to find in the string.
     * @return Array of all indexes in the string.
     */
    private static int[] findAllIndexesOfChar(final String string, final char c) {
        final ArrayList<Integer> indexes = new ArrayList<>(2);

        int index = string.indexOf(c);
        while (index >= 0) {
            indexes.add(index);
            index = string.indexOf(c, index + 1);
        }

        return indexes.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Tries to parse HyperStackDimensions from a string with delimiters.
     *
     * @param hyperStackDimensionsString String containing the dimensions.
     * @param delimiter                  Delimiter.
     * @return Parsed HyperStackDimensions or empty optional.
     */
    public static Optional<HyperStackDimensions> tryParseHyperStackDimensions(final String hyperStackDimensionsString,
                                                                              final char delimiter) {
        final String string = (delimiter != ' ') ? removeSpacesInString(hyperStackDimensionsString) : hyperStackDimensionsString;

        final int[] indexes = findAllIndexesOfChar(string, delimiter);

        if ((indexes.length < 1) || (indexes.length > 3))
            return Optional.empty();

        int x = -1;
        int y = -1;
        int z = 1;
        int t = 1;

        // Required part.
        final Optional<Integer> maybeX = tryParseInt(string.substring(0, indexes[0]));
        final Optional<Integer> maybeY = tryParseInt(string.substring(indexes[0] + 1, indexes.length > 1 ? indexes[1] : string.length()));
        if (!maybeX.isPresent() || !maybeY.isPresent())
            return Optional.empty();
        x = maybeX.get();
        y = maybeY.get();

        if (indexes.length > 1) {
            final Optional<Integer> maybeZ = tryParseInt(string.substring(indexes[1] + 1,
                                                                          indexes.length > 2 ? indexes[2] : string.length()));
            if (!maybeZ.isPresent())
                return Optional.empty();
            z = maybeZ.get();

            if (indexes.length > 2) {
                final Optional<Integer> maybeT = tryParseInt(string.substring(indexes[2] + 1));
                if (!maybeT.isPresent())
                    return Optional.empty();
                t = maybeT.get();
            }
        }
        return Optional.of(new HyperStackDimensions(x, y, z, t));
    }
}
