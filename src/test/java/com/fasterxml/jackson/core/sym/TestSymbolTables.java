package com.fasterxml.jackson.core.sym;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;

import com.fasterxml.jackson.core.*;

/**
 * Tests that directly modify/access underlying low-level symbol tables
 * (instead of indirectly using them via JsonParser).
 */
public class TestSymbolTables extends com.fasterxml.jackson.core.BaseTest
{
    // Test for verifying stability of hashCode, wrt collisions, using
    // synthetic field name generation and character-based input
    public void testSyntheticWithChars()
    {
        // pass seed, to keep results consistent:
        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(1).makeChild(-1);
        final int COUNT = 12000;
        for (int i = 0; i < COUNT; ++i) {
            String id = fieldNameFor(i);
            char[] ch = id.toCharArray();

            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(id));
        }

        assertEquals(16384, symbols.bucketCount());
        assertEquals(COUNT, symbols.size());

//System.out.printf("Char stuff: collisions %d, max-coll %d\n", symbols.collisionCount(), symbols.maxCollisionLength());

        // holy guacamoley... there are way too many. 31 gives 3567 (!), 33 gives 2747
        // ... at least before shuffling. Shuffling helps quite a lot, so:

        assertEquals(3431, symbols.collisionCount());

        assertEquals(6, symbols.maxCollisionLength());

        // and final validation
        symbols.verifyInternalConsistency();
    }

    public void testSyntheticWithBytesNew() throws IOException
    {
        // pass seed, to keep results consistent:
        final int SEED = 33333;
        ByteQuadsCanonicalizer symbols =
                ByteQuadsCanonicalizer.createRoot(SEED).makeChild(JsonFactory.Feature.collectDefaults());
        assertTrue(symbols.isCanonicalizing());

        final int COUNT = 12000;
        for (int i = 0; i < COUNT; ++i) {
            String id = fieldNameFor(i);
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(16384, symbols.bucketCount());

        // fragile, but essential to verify low collision counts;
        // anywhere between 70-80% primary matches
        assertEquals(8534, symbols.primaryCount());
        // secondary between 10-20%
        assertEquals(2534, symbols.secondaryCount());
        // and most of remaining in tertiary
        assertEquals(932, symbols.tertiaryCount());
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
            // validate further, just to make sure
            symbolsC.verifyInternalConsistency();

            symbolsC.release();
            exp += 250;
            if (exp > CharsToNameCanonicalizer.MAX_ENTRIES_FOR_REUSE) {
                exp = 0;
            }
            assertEquals(exp, symbolsCRoot.size());
        }

        // Note: can not validate root instance, is not set up same way
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
            symbolsB.release();

            exp += 250;
            if (exp > ByteQuadsCanonicalizer.MAX_ENTRIES_FOR_REUSE) {
                exp = 0;
            }
            assertEquals(exp, symbolsBRoot.size());
        }

        // 05-Feb-2015, tatu: Fragile, but it is important to ensure that collision
        //   rates are not accidentally increased...
        assertEquals(6250, symbolsB.size());
        assertEquals(4761, symbolsB.primaryCount()); // 80% primary hit rate
        assertEquals(1190, symbolsB.secondaryCount()); // 13% secondary
        assertEquals(299, symbolsB.tertiaryCount()); // 7% tertiary
        assertEquals(0, symbolsB.spilloverCount()); // and couple of leftovers
    }

    // And then one more test just for Bytes-based symbol table
    public void testByteBasedSymbolTable() throws Exception
    {
        // combination of short, medium1/2, long names...
        final String JSON = a2q("{'abc':1, 'abc\\u0000':2, '\\u0000abc':3, "
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

    // [core#187]: unexpectedly high number of collisions for straight numbers
    public void testCollisionsWithChars187() throws IOException
    {
        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(1).makeChild(-1);
        final int COUNT = 30000;
        for (int i = 0; i < COUNT; ++i) {
            String id = String.valueOf(10000 + i);
            char[] ch = id.toCharArray();
            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(id));
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(65536, symbols.bucketCount());

        // collision count rather high, but has to do
        assertEquals(7127, symbols.collisionCount());
        // as well as collision counts
        assertEquals(4, symbols.maxCollisionLength());
    }

    // [core#187]: unexpectedly high number of collisions for straight numbers
    public void testCollisionsWithBytesNew187a() throws IOException
    {
        ByteQuadsCanonicalizer symbols =
                ByteQuadsCanonicalizer.createRoot(1).makeChild(JsonFactory.Feature.collectDefaults());

        final int COUNT = 43000;
        for (int i = 0; i < COUNT; ++i) {
            String id = String.valueOf(10000 + i);
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }

        assertEquals(COUNT, symbols.size());
        assertEquals(65536, symbols.bucketCount());

        /* 29-Mar-2015, tatu: To get collision counts down for this
         *    test took quite a bit of tweaking...
         */
        assertEquals(32342, symbols.primaryCount());
        assertEquals(8863, symbols.secondaryCount());
        assertEquals(1795, symbols.tertiaryCount());

        // finally managed to get this to 0; other variants produced thousands
        assertEquals(0, symbols.spilloverCount());
    }

    // Another variant, but with 1-quad names
    public void testCollisionsWithBytesNew187b() throws IOException
    {
        ByteQuadsCanonicalizer symbols =
                ByteQuadsCanonicalizer.createRoot(1).makeChild(JsonFactory.Feature.collectDefaults());

        final int COUNT = 10000;
        for (int i = 0; i < COUNT; ++i) {
            String id = String.valueOf(i);
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(COUNT, symbols.size());

        assertEquals(16384, symbols.bucketCount());

        // fragile, but essential to verify low collision counts;
        // here bit low primary, 55%
        assertEquals(5402, symbols.primaryCount());
        // secondary higher than usual, above 25%
        assertEquals(2744, symbols.secondaryCount());
        // and most of remaining in tertiary
        assertEquals(1834, symbols.tertiaryCount());
        // with a bit of spillover
        assertEquals(20, symbols.spilloverCount());
    }

    // [core#191]: similarly, but for "short" symbols:
    public void testShortNameCollisionsViaParser() throws Exception
    {
        JsonFactory f = new JsonFactory();
        String json = _shortDoc191();
        JsonParser p;

        // First: ensure that char-based is fine
        p = f.createParser(json);
        while (p.nextToken() != null) { }
        p.close();

        // and then that byte-based
        p = f.createParser(json.getBytes("UTF-8"));
        while (p.nextToken() != null) { }
        p.close();
    }

    private String _shortDoc191() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (int i = 0; i < 400; ++i) {
            if (i > 0) {
                sb.append(",\n");
            }
            sb.append('"');
            char c = (char) i;
            if (Character.isLetterOrDigit(c)) {
                sb.append((char) i);
            } else {
                sb.append(String.format("\\u%04x", i));
            }
            sb.append("\" : "+i);
        }
        sb.append("}\n");
        return sb.toString();
    }

    // [core#191]
    public void testShortQuotedDirectChars() throws IOException
    {
        final int COUNT = 400;

        CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(1).makeChild(-1);
        for (int i = 0; i < COUNT; ++i) {
            String id = String.format("\\u%04x", i);
            char[] ch = id.toCharArray();
            symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(id));
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(1024, symbols.bucketCount());

        assertEquals(50, symbols.collisionCount());
        assertEquals(2, symbols.maxCollisionLength());
    }

    public void testShortQuotedDirectBytes() throws IOException
    {
        final int COUNT = 400;
        ByteQuadsCanonicalizer symbols =
                ByteQuadsCanonicalizer.createRoot(123).makeChild(JsonFactory.Feature.collectDefaults());
        for (int i = 0; i < COUNT; ++i) {
            String id = String.format("\\u%04x", i);
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbols.addName(id, quads, quads.length);
        }
        assertEquals(COUNT, symbols.size());
        assertEquals(512, symbols.bucketCount());

        assertEquals(285, symbols.primaryCount());
        assertEquals(90, symbols.secondaryCount());
        assertEquals(25, symbols.tertiaryCount());
        assertEquals(0, symbols.spilloverCount());
    }

    // [core#191]
    public void testShortNameCollisionsDirect() throws IOException
    {
        final int COUNT = 600;

        // First, char-based
        {
            CharsToNameCanonicalizer symbols = CharsToNameCanonicalizer.createRoot(1).makeChild(-1);
            for (int i = 0; i < COUNT; ++i) {
                String id = String.valueOf((char) i);
                char[] ch = id.toCharArray();
                symbols.findSymbol(ch, 0, ch.length, symbols.calcHash(id));
            }
            assertEquals(COUNT, symbols.size());
            assertEquals(1024, symbols.bucketCount());

            assertEquals(16, symbols.collisionCount());
            assertEquals(1, symbols.maxCollisionLength());
        }
    }

    public void testShortNameCollisionsDirectNew() throws IOException
    {
        final int COUNT = 700;
        {
            ByteQuadsCanonicalizer symbols =
                    ByteQuadsCanonicalizer.createRoot(333).makeChild(JsonFactory.Feature.collectDefaults());
            for (int i = 0; i < COUNT; ++i) {
                String id = String.valueOf((char) i);
                int[] quads = calcQuads(id.getBytes("UTF-8"));
                symbols.addName(id, quads, quads.length);
            }
            assertEquals(COUNT, symbols.size());

            assertEquals(1024, symbols.bucketCount());

            // Primary is good, but secondary spills cluster in nasty way...
            assertEquals(564, symbols.primaryCount());
            assertEquals(122, symbols.secondaryCount());
            assertEquals(14, symbols.tertiaryCount());
            assertEquals(0, symbols.spilloverCount());

            assertEquals(COUNT,
                    symbols.primaryCount() + symbols.secondaryCount() + symbols.tertiaryCount() + symbols.spilloverCount());
        }
    }

    // to verify [jackson-core#213] -- did not fail, but ruled out low-level bug

    public void testLongSymbols17Bytes() throws Exception
    {
        ByteQuadsCanonicalizer symbolsB =
                ByteQuadsCanonicalizer.createRoot(3).makeChild(JsonFactory.Feature.collectDefaults());
        CharsToNameCanonicalizer symbolsC = CharsToNameCanonicalizer.createRoot(3).makeChild(-1);

        for (int i = 1001; i <= 1050; ++i) {
            String id = "lengthmatters"+i;
            int[] quads = calcQuads(id.getBytes("UTF-8"));
            symbolsB.addName(id, quads, quads.length);
            char[] idChars = id.toCharArray();
            symbolsC.findSymbol(idChars, 0, idChars.length, symbolsC.calcHash(id));
        }
        assertEquals(50, symbolsB.size());
        assertEquals(50, symbolsC.size());
    }
}
