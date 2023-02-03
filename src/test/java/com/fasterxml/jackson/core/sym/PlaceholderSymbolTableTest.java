package com.fasterxml.jackson.core.sym;

// Simple tests to verify "placeholder" variant added in 2.13
public class PlaceholderSymbolTableTest extends com.fasterxml.jackson.core.BaseTest
{
    // Test to verify it is ok to try to find names, and that none
    // are ever found
    public void testBasicPlaceholderLookups() throws Exception
    {
        final ByteQuadsCanonicalizer root = ByteQuadsCanonicalizer.createRoot(137);
        assertEquals(0, root.size());
        assertFalse(root.isCanonicalizing());

        ByteQuadsCanonicalizer placeholder = root.makeChildOrPlaceholder(0);

        assertEquals(-1, placeholder.size());
        assertFalse(placeholder.isCanonicalizing());

        final int[] quads = calcQuads("abcd1234efgh5678"); // 4 ints

        assertNull(placeholder.findName(quads[0]));
        assertNull(placeholder.findName(quads[0], quads[1]));
        assertNull(placeholder.findName(quads[0], quads[1], quads[2]));

        assertNull(placeholder.findName(quads, quads.length));
    }

    // Also: should not allow additions
    public void testBasicPlaceholderAddFails() throws Exception
    {
        final ByteQuadsCanonicalizer root = ByteQuadsCanonicalizer.createRoot(137);
        ByteQuadsCanonicalizer placeholder = root.makeChildOrPlaceholder(0);

        final int[] quads = calcQuads("abcd1234efgh5678"); // 4 ints

        // try all variations
        try {
            placeholder.addName("abcd", placeholder.calcHash(quads[0]));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Cannot add names to Placeholder");
        }
        try {
            placeholder.addName("abcd1234",
                    placeholder.calcHash(quads[0], quads[1]));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Cannot add names to Placeholder");
        }
        try {
            placeholder.addName("abcd1234efgh",
                    placeholder.calcHash(quads[0], quads[1], quads[2]));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Cannot add names to Placeholder");
        }
        try {
            placeholder.addName("abcd1234efgh5678",
                    placeholder.calcHash(quads, quads.length));
            fail("Should not pass");
        } catch (Exception e) {
            verifyException(e, "Cannot add names to Placeholder");
        }


        // Verify nothing changed about state
        assertEquals(-1, placeholder.size());
        assertFalse(placeholder.isCanonicalizing());

        // and that "release" is fine too
        placeholder.release();

        // without changing root state either
        assertEquals(0, root.size());
        assertFalse(root.isCanonicalizing());
    }
}
