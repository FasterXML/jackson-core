package com.fasterxml.jackson.core.sym;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ByteQuadsCanonicalizerTest
{
    // For property names of 4+ quads the trailing quads must be combined in an
    // order-dependent way: two names built from the same quads in a different
    // order should hash to different values, regardless of the table seed.
    @Test
    void tailQuadOrderingAffectsLongNameHash()
    {
        final int[] base = {
                0x61626364, 0x65666768, 0x696a6b6c, // q0..q2
                0x41414141, 0x42424242, 0x43434343, 0x44444444 // tail q3..q6
        };
        final int qlen = base.length;

        for (int seed : new int[] { 1, 137, -523743345, 0x7fffffff, 99 }) {
            ByteQuadsCanonicalizer canon = ByteQuadsCanonicalizer.createRoot(seed);
            int baseHash = canon.calcHash(base, qlen);

            // swap two trailing quads -> same quads, different order
            int[] swapped = base.clone();
            int tmp = swapped[3]; swapped[3] = swapped[5]; swapped[5] = tmp;
            assertNotEquals(baseHash, canon.calcHash(swapped, qlen),
                    "Reordering trailing quads should change hash (seed=" + seed + ")");

            // and a rotation of the whole tail
            int[] rotated = { base[0], base[1], base[2], base[4], base[5], base[6], base[3] };
            assertNotEquals(baseHash, canon.calcHash(rotated, qlen),
                    "Rotating trailing quads should change hash (seed=" + seed + ")");
        }
    }

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