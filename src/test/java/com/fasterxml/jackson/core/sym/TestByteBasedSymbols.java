package com.fasterxml.jackson.core.sym;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Random;

import com.fasterxml.jackson.core.*;

/**
 * Unit test(s) to verify that handling of (byte-based) symbol tables
 * is working.
 */
public class TestByteBasedSymbols
    extends com.fasterxml.jackson.core.BaseTest
{
    final static String[] FIELD_NAMES = new String[] {
        "a", "b", "c", "x", "y", "b13", "abcdefg", "a123",
        "a0", "b0", "c0", "d0", "e0", "f0", "g0", "h0",
        "x2", "aa", "ba", "ab", "b31", "___x", "aX", "xxx",
        "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
        "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
        "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1",
    };

    /**
     * This unit test checks that [JACKSON-5] is fixed; if not, a
     * symbol table corruption should result in odd problems.
     */
    public void testSharedSymbols() throws Exception
    {
        // MUST share a single json factory
        JsonFactory jf = new JsonFactory();

        // First things first: parse a dummy doc to populate
        // shared symbol table with some stuff
        String DOC0 = "{ \"a\" : 1, \"x\" : [ ] }";
        JsonParser jp0 = createParser(jf, DOC0);

        /* Important: don't close, don't traverse past end.
         * This is needed to create partial still-in-use symbol
         * table...
         */
        while (jp0.nextToken() != JsonToken.START_ARRAY) { }

        String doc1 = createDoc(FIELD_NAMES, true);
        String doc2 = createDoc(FIELD_NAMES, false);

        // Let's run it twice... shouldn't matter
        for (int x = 0; x < 2; ++x) {
            JsonParser jp1 = createParser(jf, doc1);
            JsonParser jp2 = createParser(jf, doc2);

            assertToken(JsonToken.START_OBJECT, jp1.nextToken());
            assertToken(JsonToken.START_OBJECT, jp2.nextToken());

            int len = FIELD_NAMES.length;
            for (int i = 0; i < len; ++i) {
                assertToken(JsonToken.FIELD_NAME, jp1.nextToken());
                assertToken(JsonToken.FIELD_NAME, jp2.nextToken());
                assertEquals(FIELD_NAMES[i], jp1.getCurrentName());
                assertEquals(FIELD_NAMES[len-(i+1)], jp2.getCurrentName());
                assertToken(JsonToken.VALUE_NUMBER_INT, jp1.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, jp2.nextToken());
                assertEquals(i, jp1.getIntValue());
                assertEquals(i, jp2.getIntValue());
            }

            assertToken(JsonToken.END_OBJECT, jp1.nextToken());
            assertToken(JsonToken.END_OBJECT, jp2.nextToken());

            jp1.close();
            jp2.close();
        }
        jp0.close();
    }

    public void testAuxMethodsWithNewSymboTable() throws Exception
    {
        final int A_BYTES = 0x41414141; // "AAAA"
        final int B_BYTES = 0x42424242; // "BBBB"

        ByteQuadsCanonicalizer nc = ByteQuadsCanonicalizer.createRoot()
                .makeChild(JsonFactory.Feature.collectDefaults());
        assertNull(nc.findName(A_BYTES));
        assertNull(nc.findName(A_BYTES, B_BYTES));

        nc.addName("AAAA", new int[] { A_BYTES }, 1);
        String n1 = nc.findName(A_BYTES);
        assertEquals("AAAA", n1);
        nc.addName("AAAABBBB", new int[] { A_BYTES, B_BYTES }, 2);
        String n2 = nc.findName(A_BYTES, B_BYTES);
        assertEquals("AAAABBBB", n2);
        assertNotNull(n2);

        /* and let's then just exercise this method so it gets covered;
         * it's only used for debugging.
         */
        assertNotNull(nc.toString());
    }

    // as per name, for [core#207]
    public void testIssue207() throws Exception
    {
        ByteQuadsCanonicalizer nc = ByteQuadsCanonicalizer.createRoot(-523743345);
        Field byteSymbolCanonicalizerField = JsonFactory.class.getDeclaredField("_byteSymbolCanonicalizer");
        byteSymbolCanonicalizerField.setAccessible(true);
        JsonFactory jsonF = new JsonFactory();
        byteSymbolCanonicalizerField.set(jsonF, nc);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\n");
        stringBuilder.append("    \"expectedGCperPosition\": null");
        for (int i = 0; i < 60; ++i) {
            stringBuilder.append(",\n    \"").append(i + 1).append("\": null");
        }
        stringBuilder.append("\n}");

        JsonParser p = jsonF.createParser(stringBuilder.toString().getBytes("UTF-8"));
        while (p.nextToken() != null) { }
        p.close();
    }

    // [core#548]
    public void testQuadsIssue548()
    {
        Random r = new Random(42);
        ByteQuadsCanonicalizer root = ByteQuadsCanonicalizer.createRoot();
        ByteQuadsCanonicalizer canon = root.makeChild(JsonFactory.Feature.collectDefaults());

        int n_collisions = 25;
        int[] collisions = new int[n_collisions];

        // generate collisions
        {
            int maybe = r.nextInt();
            int hash = canon.calcHash(maybe);
            int target = ((hash & (2048-1)) << 2);

            for (int i = 0; i < collisions.length; ) {
                maybe = r.nextInt();
                hash = canon.calcHash(maybe);
                int offset = ((hash & (2048-1)) << 2);

                if (offset == target) {
                    collisions[i++] = maybe;
                }
            }
        }

        // fill spillover area until _needRehash is true.
        for(int i = 0; i < 22 ; i++) {
            canon.addName(Integer.toString(i), collisions[i]);
        }
        // canon._needRehash is now true, since the spillover is full

        // release table to update tableinfo with canon's data
        canon.release();

        // new table pulls data from new tableinfo, that has a full spillover, but set _needRehash to false
        canon = root.makeChild(JsonFactory.Feature.collectDefaults());

        // canon._needRehash == false, so this will try to add another item to the spillover area, even though it is full
        canon.addName(Integer.toString(22), collisions[22]);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected JsonParser createParser(JsonFactory jf, String input) throws IOException
    {
        byte[] data = input.getBytes("UTF-8");
        InputStream is = new ByteArrayInputStream(data);
        return jf.createParser(is);
    }

    private String createDoc(String[] fieldNames, boolean add)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");

        int len = fieldNames.length;
        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('"');
            sb.append(add ? fieldNames[i] : fieldNames[len - (i+1)]);
            sb.append("\" : ");
            sb.append(i);
        }
        sb.append(" }");
        return sb.toString();
    }
}


