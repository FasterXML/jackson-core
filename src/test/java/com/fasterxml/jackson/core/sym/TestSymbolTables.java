package com.fasterxml.jackson.core.sym;

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
}
