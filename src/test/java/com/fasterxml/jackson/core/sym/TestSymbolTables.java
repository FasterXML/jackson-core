package com.fasterxml.jackson.core.sym;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.*;

public class TestSymbolTables extends com.fasterxml.jackson.core.BaseTest
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
    @SuppressWarnings("deprecation")
    public void testSyntheticWithBytesOld() throws IOException
    {
        // pass seed, to keep results consistent:
        final int SEED = 33333;
        BytesToNameCanonicalizer symbols =
                BytesToNameCanonicalizer.createRoot(SEED).makeChild(JsonFactory.Feature.collectDefaults());

        final int COUNT = 6000;
        for (int i = 0; i < COUNT; ++i) {
            String id = fieldNameFor(i);
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(8192, symbols.bucketCount());

//System.out.printf("Byte stuff: collisions %d, max-coll %d\n", symbols.collisionCount(), symbols.maxCollisionLength());
    
        // Fewer collisions than with chars, but still quite a few
        assertEquals(1715, symbols.collisionCount());
        // but not super long collision chains:
        assertEquals(9, symbols.maxCollisionLength());

        // But also verify entries are actually found?
    }

    public void testSyntheticWithBytesNew() throws IOException
    {
        // pass seed, to keep results consistent:
        final int SEED = 33333;
        ByteQuadsCanonicalizer symbols =
                ByteQuadsCanonicalizer.createRoot(SEED).makeChild(JsonFactory.Feature.collectDefaults());

        final int COUNT = 6000;
        for (int i = 0; i < COUNT; ++i) {
            String id = fieldNameFor(i);
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(8192, symbols.bucketCount());

        // fragile, but essential to verify low collision counts;
        // anywhere between 70-80% primary matches
        assertEquals(4270, symbols.primaryCount());
        // secondary between 10-20%
        assertEquals(1234, symbols.secondaryCount());
        // and most of remaining in tertiary
        assertEquals(496, symbols.tertiaryCount());
        // so that spill-over is empty or close to
        assertEquals(0, symbols.spilloverCount());
    }
    
    // [Issue#145]
    public void testThousandsOfSymbolsWithChars() throws IOException
    {
        final int SEED = 33333;

        CharsToNameCanonicalizer symbolsCRoot = CharsToNameCanonicalizer.createRoot(SEED);
        int exp = 0;
        
        for (int doc = 0; doc < 100; ++doc) {
            CharsToNameCanonicalizer symbolsC =
                    symbolsCRoot.makeChild(JsonFactory.Feature.collectDefaults());
            for (int i = 0; i < 250; ++i) {
                String name = "f_"+doc+"_"+i;
                char[] ch = name.toCharArray();
                String str = symbolsC.findSymbol(ch, 0, ch.length,
                        symbolsC.calcHash(name));
                assertNotNull(str);
            }
            symbolsC.release();
            exp += 250;
            if (exp > CharsToNameCanonicalizer.MAX_ENTRIES_FOR_REUSE) {
                exp = 0;
            }
            assertEquals(exp, symbolsCRoot.size());
        }
    }
    
    @SuppressWarnings("deprecation")
    public void testThousandsOfSymbolsWithOldBytes() throws IOException
    {
        final int SEED = 33333;

        BytesToNameCanonicalizer symbolsBRoot = BytesToNameCanonicalizer.createRoot(SEED);
        final Charset utf8 = Charset.forName("UTF-8");
        int exp = 0;
        
        for (int doc = 0; doc < 100; ++doc) {
            BytesToNameCanonicalizer symbolsB =
                    symbolsBRoot.makeChild(JsonFactory.Feature.collectDefaults());
            for (int i = 0; i < 250; ++i) {
                String name = "f_"+doc+"_"+i;

                int[] quads = BytesToNameCanonicalizer.calcQuads(name.getBytes(utf8));
                symbolsB.addName(name, quads, quads.length);
                Name n = symbolsB.findName(quads, quads.length);
                assertEquals(name, n.getName());
            }
            symbolsB.release();
            exp += 250;
            if (exp > BytesToNameCanonicalizer.MAX_ENTRIES_FOR_REUSE) {
                exp = 0;
            }
            assertEquals(exp, symbolsBRoot.size());
        }
    }

    // Since 2.6
    public void testThousandsOfSymbolsWithNew() throws IOException
    {
        final int SEED = 33333;

        ByteQuadsCanonicalizer symbolsBRoot = ByteQuadsCanonicalizer.createRoot(SEED);
        final Charset utf8 = Charset.forName("UTF-8");
        int exp = 0;
        ByteQuadsCanonicalizer symbolsB = null;

        // loop to get 
        for (int doc = 0; doc < 100; ++doc) {
            symbolsB = symbolsBRoot.makeChild(JsonFactory.Feature.collectDefaults());
            for (int i = 0; i < 250; ++i) {
                String name = "f_"+doc+"_"+i;

                int[] quads = calcQuads(name.getBytes(utf8));
                
                symbolsB.addName(name, quads, quads.length);
                String n = symbolsB.findName(quads, quads.length);
                assertEquals(name, n);
            }
//System.out.println("New symbols: "+symbolsB);
            symbolsB.release();
            
            exp += 250;
            if (exp > ByteQuadsCanonicalizer.MAX_ENTRIES_FOR_REUSE) {
                exp = 0;
            }
            assertEquals(exp, symbolsBRoot.size());
        }

        /* 05-Feb-2015, tatu: Fragile, but it is important to ensure that collision
         *   rates are not accidentally increased...
         */
        assertEquals(6250, symbolsB.size());
        assertEquals(4851, symbolsB.primaryCount()); // 77% primary hit rate
        assertEquals(872, symbolsB.secondaryCount()); // 14% secondary
        assertEquals(510, symbolsB.tertiaryCount()); // 8% tertiary
        assertEquals(17, symbolsB.spilloverCount()); // and couple of leftovers
    }
    
    // And then one more test just for Bytes-based symbol table
    public void testByteBasedSymbolTable() throws Exception
    {
        // combination of short, medium1/2, long names...
        final String JSON = aposToQuotes("{'abc':1, 'abc\\u0000':2, '\\u0000abc':3, "
                // then some medium
                +"'abc123':4,'abcd1234':5,"
                +"'abcd1234a':6,'abcd1234abcd':7,"
                +"'abcd1234abcd1':8"
                +"}");

        JsonFactory f = new JsonFactory();
        JsonParser p = f.createParser(JSON.getBytes("UTF-8"));
        ByteQuadsCanonicalizer symbols = _findSymbols(p);
        assertEquals(0, symbols.size());
        _streamThrough(p);
        assertEquals(8, symbols.size());
        p.close();

        // and, for fun, try again
        p = f.createParser(JSON.getBytes("UTF-8"));
        _streamThrough(p);
        symbols = _findSymbols(p);
        assertEquals(8, symbols.size());
        p.close();

        p = f.createParser(JSON.getBytes("UTF-8"));
        _streamThrough(p);
        symbols = _findSymbols(p);
        assertEquals(8, symbols.size());
        p.close();
    }

    private void _streamThrough(JsonParser p) throws IOException
    {
        while (p.nextToken() != null) { }
    }
    
    private ByteQuadsCanonicalizer _findSymbols(JsonParser p) throws Exception
    {
        Field syms = p.getClass().getDeclaredField("_symbols");
        syms.setAccessible(true);
        return ((ByteQuadsCanonicalizer) syms.get(p));
    }

    // Method only used by unit tests
    protected static int[] calcQuads(byte[] wordBytes) {
        int blen = wordBytes.length;
        int[] result = new int[(blen + 3) / 4];
        for (int i = 0; i < blen; ++i) {
            int x = wordBytes[i] & 0xFF;

            if (++i < blen) {
                x = (x << 8) | (wordBytes[i] & 0xFF);
                if (++i < blen) {
                    x = (x << 8) | (wordBytes[i] & 0xFF);
                    if (++i < blen) {
                        x = (x << 8) | (wordBytes[i] & 0xFF);
                    }
                }
            }
            result[i >> 2] = x;
        }
        return result;
    }
}
