package com.fasterxml.jackson.core.sym;

import java.io.IOException;

public class TestSymbolTables extends com.fasterxml.jackson.test.BaseTest
{
    // 11 3-char snippets that hash to 0xFFFF (with default JDK hashCode() calc),
    // and which can be combined as
    // sequences, like, say, 11x11x11 (1331) 9-character thingies
    final static String[] CHAR_COLLISION_SNIPPETS = {
        "@~}", "@^", "A_}", "A`^", 
        "Aa?", "B@}", "BA^", "BB?", 
        "C!}", "C\"^", "C#?"
    };
    final static String[] CHAR_COLLISIONS;
    static {
        final int len = CHAR_COLLISION_SNIPPETS.length;
        CHAR_COLLISIONS = new String[len*len*len];
        int ix = 0;
        for (int i1 = 0; i1 < len; ++i1) {
            for (int i2 = 0; i2 < len; ++i2) {
                for (int i3 = 0; i3 < len; ++i3) {
                    CHAR_COLLISIONS[ix++] = CHAR_COLLISION_SNIPPETS[i1]
                            +CHAR_COLLISION_SNIPPETS[i2] + CHAR_COLLISION_SNIPPETS[i3];
                }
            }
        }
    }
    
    public void testCharBasedCollisions()
    {
        CharsToNameCanonicalizer sym = CharsToNameCanonicalizer.createRoot(0);

        // first, verify that we'd get a few collisions...
        try {
            int firstHash = 0;
            for (String str : CHAR_COLLISIONS) {
                int hash = sym.calcHash(str);
                if (firstHash == 0) {
                    firstHash = hash;
                } else {
                    assertEquals(firstHash, hash); 
                }
                sym.findSymbol(str.toCharArray(), 0, str.length(), hash);
            }
            fail("Should have thrown exception");
        } catch (IllegalStateException e) {
            verifyException(e, "exceeds maximum");
            // should fail right after addition:
            assertEquals(CharsToNameCanonicalizer.MAX_COLL_CHAIN_LENGTH+1, sym.maxCollisionLength());
            assertEquals(CharsToNameCanonicalizer.MAX_COLL_CHAIN_LENGTH+1, sym.collisionCount());
            // one "non-colliding" entry (head of collision chain), thus:
            assertEquals(CharsToNameCanonicalizer.MAX_COLL_CHAIN_LENGTH+2, sym.size());
        }
    }

    // Test for verifying stability of hashCode, wrt collisions, using
    // synthetic field name generation and character-based input
    public void testSyntheticWithChars()
    {
        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(0);
        for (int i = 0; i < 5000; ++i) {
            String id = fieldNameFor(i);
            char[] ch = id.toCharArray();
            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(id));
        }

        assertEquals(8192, symbols.bucketCount());
        assertEquals(5000, symbols.size());
        // holy guacamoley... there are way too many:
        assertEquals(3053, symbols.collisionCount());
        // but spread more evenly than byte-based ones?
        assertEquals(29, symbols.maxCollisionLength());
    }

    // Test for verifying stability of hashCode, wrt collisions, using
    // synthetic field name generation and byte-based input (UTF-8)
    public void testSyntheticWithBytes() throws IOException
    {
        BytesToNameCanonicalizer symbols = BytesToNameCanonicalizer.createRoot();
        for (int i = 0; i < 5000; ++i) {
            String id = fieldNameFor(i);
            int[] quads = BytesToNameCanonicalizer.calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(5000, symbols.size());
        assertEquals(8192, symbols.bucketCount());
        
        // holy guacamoley... even here we have too many; but surprisingly (?)
        // less than with chars
        assertEquals(1697, symbols.collisionCount());
        assertEquals(9, symbols.maxCollisionLength());
    }
}
