package cz.it4i.qcmp.cli;

import cz.it4i.qcmp.data.HyperStackDimensions;
import cz.it4i.qcmp.data.Range;
import cz.it4i.qcmp.data.V2i;
import cz.it4i.qcmp.data.V3i;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ParseUtilsTest {

    private void tryParseHyperStackDimensionsHelper(final String string,
                                                    final boolean shouldSucceed,
                                                    final HyperStackDimensions expectedResult) {
        final Optional<HyperStackDimensions> parseResult = ParseUtils.tryParseHyperStackDimensions(string, 'x');
        assertEquals(shouldSucceed, parseResult.isPresent());

        //noinspection OptionalIsPresent
        if (parseResult.isPresent()) {
            assertEquals(parseResult.get(), expectedResult);
        }
    }


    @Test
    void tryParseHyperStackDimensions() {
        tryParseHyperStackDimensionsHelper("100x100", true, new HyperStackDimensions(100, 100));
        tryParseHyperStackDimensionsHelper("100x200x300", true, new HyperStackDimensions(100, 200, 300));
        tryParseHyperStackDimensionsHelper("100x200x300x400", true, new HyperStackDimensions(100, 200, 300, 400));
        tryParseHyperStackDimensionsHelper("100", false, null);
        tryParseHyperStackDimensionsHelper("100x", false, null);
        tryParseHyperStackDimensionsHelper("x50", false, null);
        tryParseHyperStackDimensionsHelper("100x100x", false, null);
        tryParseHyperStackDimensionsHelper("100xx", false, null);
        tryParseHyperStackDimensionsHelper("100x100x100x", false, null);
    }

    @Test
    void tryParseInt() {
        assertTrue(ParseUtils.tryParseInt("10").isPresent());
        assertFalse(ParseUtils.tryParseInt("q10").isPresent());
        assertEquals(ParseUtils.tryParseInt("10").get(), 10);
    }

    @Test
    void tryParseRange() {
        final Optional<Range<Integer>> t1 = ParseUtils.tryParseRange("10-20", '-');
        assertTrue(t1.isPresent());
        assertEquals(t1.get(), new Range<>(10, 20));

        final Optional<Range<Integer>> t2 = ParseUtils.tryParseRange("999x10045", 'x');
        assertTrue(t2.isPresent());
        assertEquals(t2.get(), new Range<>(999, 10045));

        assertFalse(ParseUtils.tryParseRange("999x10045", '-').isPresent());
        assertFalse(ParseUtils.tryParseRange("99910045", 'x').isPresent());
    }

    @Test
    void tryParseV2i() {
        final Optional<V2i> c1 = ParseUtils.tryParseV2i("10x20", 'x');
        assertTrue(c1.isPresent());
        assertEquals(c1.get(), new V2i(10, 20));

        final Optional<V2i> c2 = ParseUtils.tryParseV2i("-20x-89", 'x');
        assertTrue(c2.isPresent());
        assertEquals(c2.get(), new V2i(-20, -89));

        final Optional<V2i> i1 = ParseUtils.tryParseV2i("10x20", '-');
        assertFalse(i1.isPresent());

        final Optional<V2i> i2 = ParseUtils.tryParseV2i("15", 'x');
        assertFalse(i2.isPresent());
    }

    @Test
    void tryParseV3i() {
        final Optional<V3i> c1 = ParseUtils.tryParseV3i("10x20x30", 'x');
        assertTrue(c1.isPresent());
        assertEquals(c1.get(), new V3i(10, 20, 30));

        final Optional<V3i> c2 = ParseUtils.tryParseV3i("-20x-89x999", 'x');
        assertTrue(c2.isPresent());
        assertEquals(c2.get(), new V3i(-20, -89, 999));

        final Optional<V3i> i1 = ParseUtils.tryParseV3i("10x20", 'x');
        assertFalse(i1.isPresent());

        final Optional<V3i> i2 = ParseUtils.tryParseV3i("15", 'x');
        assertFalse(i2.isPresent());

        final Optional<V3i> i3 = ParseUtils.tryParseV3i("10x20x", 'x');
        assertFalse(i3.isPresent());
    }
}