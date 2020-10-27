package cz.it4i.qcmp.utilities;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilsTest {

    @Test
    void multiplyExact() {
        final int a = 200;
        final int b = 54;
        final int c = 783;

        final int refResult = Math.multiplyExact(a, Math.multiplyExact(b, c));
        final int result = Utils.multiplyExact(a, b, c);
        Assertions.assertEquals(refResult, result);
    }
}