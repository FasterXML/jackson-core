package com.fasterxml.jackson.core.sym;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonFactory;

public class TestSymbolTables extends com.fasterxml.jackson.test.BaseTest
{
    // Test for verifying stability of hashCode, wrt collisions, using
    // synthetic field name generation and character-based input
    public void testSyntheticWithChars()
    {
        // pass seed, to keep results consistent:
        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(1);
        final int COUNT = 6000;
        for (int i = 0; i < COUNT; ++i) {
            String id = fieldNameFor(i);
            char[] ch = id.toCharArray();
            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(id));
        }

        assertEquals(8192, symbols.bucketCount());
        assertEquals(COUNT, symbols.size());
        
//System.out.printf("Char stuff: collisions %d, max-coll %d\n", symbols.collisionCount(), symbols.maxCollisionLength());
        
        // holy guacamoley... there are way too many. 31 gives 3567 (!), 33 gives 2747
        // ... at least before shuffling. Shuffling helps quite a lot, so:

        assertEquals(1401, symbols.collisionCount()); // with 33
//        assertEquals(1858, symbols.collisionCount()); // with 31

        // esp. with collisions; first got about 30;
        // with fixes 4 (for 33), 5 (for 31)

        assertEquals(4, symbols.maxCollisionLength()); // 33
//        assertEquals(5, symbols.maxCollisionLength()); // 31
    }

    // Test for verifying stability of hashCode, wrt collisions, using
    // synthetic field name generation and byte-based input (UTF-8)
    public void testSyntheticWithBytes() throws IOException
    {
        // pass seed, to keep results consistent:
        BytesToNameCanonicalizer symbols =
                BytesToNameCanonicalizer.createRoot(33333).makeChild(JsonFactory.Feature.collectDefaults());
        final int COUNT = 6000;
        for (int i = 0; i < COUNT; ++i) {
            String id = fieldNameFor(i);
            int[] quads = BytesToNameCanonicalizer.calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(8192, symbols.bucketCount());

//System.out.printf("Byte stuff: collisions %d, max-coll %d\n", symbols.collisionCount(), symbols.maxCollisionLength());
    
        // Fewer collisions than with chars, but still quite a few
        assertEquals(1686, symbols.collisionCount());
        // but not super long collision chains:
        assertEquals(9, symbols.maxCollisionLength());
    }
}
