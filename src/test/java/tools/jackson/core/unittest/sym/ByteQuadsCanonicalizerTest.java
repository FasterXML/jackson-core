package tools.jackson.core.unittest.sym;

import org.junit.jupiter.api.Test;

import tools.jackson.core.sym.ByteQuadsCanonicalizer;

import static org.junit.jupiter.api.Assertions.fail;

class ByteQuadsCanonicalizerTest
{
    @Test
    void multiplyByFourFifths()
    {
        int i = 0;
        for (; i >= 0; i += 7) {
            int expected = (int) (i * 0.80);
            int actual = ByteQuadsCanonicalizer.multiplyByFourFifths(i);
            if (expected != actual) {
                fail("Input for 80% of "+i+" differs: expected="+expected+", actual="+actual);
            }
        }
    }
}