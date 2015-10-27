package com.fasterxml.jackson.core.json;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
@SuppressWarnings("resource")
public class TestNumericValues
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory FACTORY = new JsonFactory();
    
    public void testSimpleBoolean() throws Exception
    {
        JsonParser jp = FACTORY.createParser("[ true ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertEquals(true, jp.getBooleanValue());
        jp.close();
    }
    
    public void testSimpleInt() throws Exception
    {
        for (int EXP_I : new int[] { 1234, -999, 0, 1, -2 }) {
            _testSimpleInt(EXP_I, false);
            _testSimpleInt(EXP_I, true);
        }
    }

    private void _testSimpleInt(int EXP_I, boolean useStream) throws Exception
    {
        String DOC = "[ "+EXP_I+" ]";
        JsonParser jp = useStream
                ? FACTORY.createParser(DOC)
                : FACTORY.createParser(DOC.getBytes("UTF-8"));
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(JsonParser.NumberType.INT, jp.getNumberType());
        assertEquals(""+EXP_I, jp.getText());

        assertEquals(EXP_I, jp.getIntValue());
        assertEquals((long) EXP_I, jp.getLongValue());
        assertEquals((double) EXP_I, jp.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), jp.getDecimalValue());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
        jp.close();

        DOC = String.valueOf(EXP_I);
        jp = useStream
                ? FACTORY.createParser(DOC)
                : FACTORY.createParser(DOC.getBytes("UTF-8"));
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(DOC, jp.getText());

        int i = 0;
        
        try {
            i = jp.getIntValue();
        } catch (Exception e) {
            throw new Exception("Failed to parse input '"+DOC+"' (parser of type "+jp.getClass().getSimpleName()+")", e);
        }
        
        assertEquals(EXP_I, i);

        assertEquals((long) EXP_I, jp.getLongValue());
        assertEquals((double) EXP_I, jp.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), jp.getDecimalValue());
        assertNull(jp.nextToken());
        jp.close();
    }

    public void testIntRange() throws Exception
    {
        // let's test with readers and streams, separate code paths:
        for (int i = 0; i < 2; ++i) {
            String input = "[ "+Integer.MAX_VALUE+","+Integer.MIN_VALUE+" ]";
            JsonParser jp;
            if (i == 0) {
                jp = FACTORY.createParser(input);                
            } else {
                jp = createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.INT, jp.getNumberType());
            assertEquals(Integer.MAX_VALUE, jp.getIntValue());
    
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.INT, jp.getNumberType());
            assertEquals(Integer.MIN_VALUE, jp.getIntValue());
            jp.close();
        }
    }

    public void testSimpleLong() throws Exception
    {
        long EXP_L = 12345678907L;

        JsonParser jp = FACTORY.createParser("[ "+EXP_L+" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        // beyond int, should be long
        assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
        assertEquals(""+EXP_L, jp.getText());

        assertEquals(EXP_L, jp.getLongValue());
        // Should get an exception if trying to convert to int 
        try {
            jp.getIntValue();
        } catch (JsonParseException jpe) {
            verifyException(jpe, "out of range");
        }
        assertEquals((double) EXP_L, jp.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_L), jp.getDecimalValue());
        jp.close();
    }

    public void testLongRange() throws Exception
    {
        for (int i = 0; i < 2; ++i) {
            long belowMinInt = -1L + Integer.MIN_VALUE;
            long aboveMaxInt = 1L + Integer.MAX_VALUE;
            String input = "[ "+Long.MAX_VALUE+","+Long.MIN_VALUE+","+aboveMaxInt+", "+belowMinInt+" ]";
            JsonParser jp;
            if (i == 0) {
                jp = FACTORY.createParser(input);                
            } else {
                jp = this.createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
            assertEquals(Long.MAX_VALUE, jp.getLongValue());
        
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
            assertEquals(Long.MIN_VALUE, jp.getLongValue());

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
            assertEquals(aboveMaxInt, jp.getLongValue());

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.LONG, jp.getNumberType());
            assertEquals(belowMinInt, jp.getLongValue());

            
            assertToken(JsonToken.END_ARRAY, jp.nextToken());        
            jp.close();
        }
    }

    public void testBigDecimalRange()
        throws Exception
    {
        for (int i = 0; i < 2; ++i) {
            // let's test first values outside of Long range
            BigInteger small = new BigDecimal(Long.MIN_VALUE).toBigInteger();
            small = small.subtract(BigInteger.ONE);
            BigInteger big = new BigDecimal(Long.MAX_VALUE).toBigInteger();
            big = big.add(BigInteger.ONE);
            String input = "[ "+small+"  ,  "+big+"]";
            JsonParser jp;
            if (i == 0) {
                jp = FACTORY.createParser(input);                
            } else {
                jp = this.createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, jp.getNumberType());
            assertEquals(small, jp.getBigIntegerValue());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, jp.getNumberType());
            assertEquals(big, jp.getBigIntegerValue());
            assertToken(JsonToken.END_ARRAY, jp.nextToken());        
            jp.close();
        }
    }

    // for [Issue#78]
    public void testBigNumbers() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 520; ++i) { // input buffer is 512 bytes by default
            sb.append('1');
        }
        final String NUMBER_STR = sb.toString();
        BigInteger biggie = new BigInteger(NUMBER_STR);
        
        for (int i = 0; i < 2; ++i) {
            JsonParser jp;
            if (i == 0) {
                jp = FACTORY.createParser(NUMBER_STR);                
            } else {
                jp = this.createParserUsingStream(NUMBER_STR, "UTF-8");
            }
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, jp.getNumberType());
            assertEquals(NUMBER_STR, jp.getText());
            assertEquals(biggie, jp.getBigIntegerValue());
            jp.close();
        }
    }
    
    public void testSimpleDouble() throws Exception
    {
        final String[] INPUTS = new String[] {
            "1234.00", "2.1101567E-16", "1.0e5", "0.0", "1.0", "-1.0", 
            "-0.5", "-12.9", "-999.0",
            "2.5e+5", "9e4", "-12e-3", "0.25",
        };
        for (int input = 0; input < 2; ++input) {
            for (int i = 0; i < INPUTS.length; ++i) {

                // First in array
                
                String STR = INPUTS[i];
                double EXP_D = Double.parseDouble(STR);
                String DOC = "["+STR+"]";

                JsonParser jp;
                
                if (input == 0) {
                    jp = createParserUsingStream(DOC, "UTF-8");
                } else {
                    jp = FACTORY.createParser(DOC);
                }
                assertToken(JsonToken.START_ARRAY, jp.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
                assertEquals(STR, jp.getText());
                assertEquals(EXP_D, jp.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, jp.nextToken());
                assertNull(jp.nextToken());
                jp.close();

                // then outside
                if (input == 0) {
                    jp = createParserUsingStream(STR, "UTF-8");
                } else {
                    jp = FACTORY.createParser(STR);
                }
                JsonToken t = null;

                try {
                    t = jp.nextToken();
                } catch (Exception e) {
                    throw new Exception("Failed to parse input '"+STR+"' (parser of type "+jp.getClass().getSimpleName()+")", e);
                }
                
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
                assertEquals(STR, jp.getText());
                assertNull(jp.nextToken());
                jp.close();
            }
        }
    }

    public void testNumbers() throws Exception
    {
        final String DOC = "[ -13, 8100200300, 13.5, 0.00010, -2.033 ]";

        for (int input = 0; input < 2; ++input) {
            JsonParser jp;

            if (input == 0) {
                jp = createParserUsingStream(DOC, "UTF-8");
            } else {
                jp = FACTORY.createParser(DOC);
            }

            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(-13, jp.getIntValue());
            assertEquals(-13L, jp.getLongValue());
            assertEquals(-13., jp.getDoubleValue());
            assertEquals("-13", jp.getText());
            
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(8100200300L, jp.getLongValue());
            // Should get exception for overflow:
            try {
                /*int x =*/ jp.getIntValue();
                fail("Expected an exception for overflow");
            } catch (Exception e) {
                verifyException(e, "out of range of int");
            }
            assertEquals(8100200300., jp.getDoubleValue());
            assertEquals("8100200300", jp.getText());
            
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
            assertEquals(13, jp.getIntValue());
            assertEquals(13L, jp.getLongValue());
            assertEquals(13.5, jp.getDoubleValue());
            assertEquals("13.5", jp.getText());
            
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
            assertEquals(0, jp.getIntValue());
            assertEquals(0L, jp.getLongValue());
            assertEquals(0.00010, jp.getDoubleValue());
            assertEquals("0.00010", jp.getText());
            
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
            assertEquals(-2, jp.getIntValue());
            assertEquals(-2L, jp.getLongValue());
            assertEquals(-2.033, jp.getDoubleValue());
            assertEquals("-2.033", jp.getText());

            assertToken(JsonToken.END_ARRAY, jp.nextToken());

            jp.close();
        }
    }

    public void testLongOverflow() throws Exception
    {
        BigInteger below = BigInteger.valueOf(Long.MIN_VALUE);
        below = below.subtract(BigInteger.ONE);
        BigInteger above = BigInteger.valueOf(Long.MAX_VALUE);
        above = above.add(BigInteger.ONE);

        String DOC_BELOW = below.toString() + " ";
        String DOC_ABOVE = below.toString() + " ";
        for (int input = 0; input < 2; ++input) {
            JsonParser jp;

            if (input == 0) {
                jp = createParserUsingStream(DOC_BELOW, "UTF-8");
            } else {
                jp = FACTORY.createParser(DOC_BELOW);
            }
            jp.nextToken();
            try {
                long x = jp.getLongValue();
                fail("Expected an exception for underflow (input "+jp.getText()+"): instead, got long value: "+x);
            } catch (JsonParseException e) {
                verifyException(e, "out of range of long");
            }
            jp.close();

            if (input == 0) {
                jp = createParserUsingStream(DOC_ABOVE, "UTF-8");
            } else {
                jp = createParserUsingReader(DOC_ABOVE);
            }
            jp.nextToken();
            try {
                long x = jp.getLongValue();
                fail("Expected an exception for underflow (input "+jp.getText()+"): instead, got long value: "+x);
            } catch (JsonParseException e) {
                verifyException(e, "out of range of long");
            }
            jp.close();
            
        }
    }
    
    /**
     * Method that tries to test that number parsing works in cases where
     * input is split between buffer boundaries.
     */
    public void testParsingOfLongerSequences()
        throws Exception
    {
        double[] values = new double[] { 0.01, -10.5, 2.1e9, 4.0e-8 };
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < values.length; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(values[i]);
        }
        String segment = sb.toString();

        int COUNT = 1000;
        sb = new StringBuilder(COUNT * segment.length() + 20);
        sb.append("[");
        for (int i = 0; i < COUNT; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(segment);
            sb.append('\n');
            // let's add somewhat arbitrary number of spaces
            int x = (i & 3);
            if (i > 300) {
                x += i % 5;
            }
            while (--x > 0) {
                sb.append(' ');
            }
        }
        sb.append("]");
        String DOC = sb.toString();

        for (int input = 0; input < 2; ++input) {
            JsonParser jp;

            if (input == 0) {
                jp = createParserUsingStream(DOC, "UTF-8");
            } else {
                jp = FACTORY.createParser(DOC);
            }

            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = 0; i < COUNT; ++i) {
                for (double d : values) {
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
                    assertEquals(d, jp.getDoubleValue());
                }
            }
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    // [jackson-core#157]
    public void testLongNumbers() throws Exception
    {
        StringBuilder sb = new StringBuilder(9000);
        for (int i = 0; i < 9000; ++i) {
            sb.append('9');
        }
        String NUM = sb.toString();
        // force use of new factory, just in case (might still recycle same buffers tho?)
        JsonFactory f = new JsonFactory();
        _testLongNumbers(f, NUM, false);
        _testLongNumbers(f, NUM, true);
    }
    
    private void _testLongNumbers(JsonFactory f, String num, boolean useStream) throws Exception
    {
        final String doc = "[ "+num+" ]";
        JsonParser jp = useStream
                ? f.createParser(doc.getBytes("UTF-8"))
                        : f.createParser(doc);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(num, jp.getText());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }

    // and alternate take on for #157 (with negative num)
    public void testLongNumbers2() throws Exception
    {
        StringBuilder input = new StringBuilder();
        // test this with negative
        input.append('-');
        for (int i = 0; i < 2100; i++) {
            input.append(1);
        }
        final String DOC = input.toString();
        JsonFactory f = new JsonFactory();
        _testIssue160LongNumbers(f, DOC, false);
        _testIssue160LongNumbers(f, DOC, true);
    }

    private void _testIssue160LongNumbers(JsonFactory f, String doc, boolean useStream) throws Exception
    {
        JsonParser jp = useStream
                ? FACTORY.createParser(doc.getBytes("UTF-8"))
                        : FACTORY.createParser(doc);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        BigInteger v = jp.getBigIntegerValue();
        assertNull(jp.nextToken());
        assertEquals(doc, v.toString());
    }

    // for [jackson-core#181]
    /**
     * Method that tries to test that number parsing works in cases where
     * input is split between buffer boundaries.
     */
    public void testParsingOfLongerSequencesWithNonNumeric() throws Exception
    {
        JsonFactory factory = new JsonFactory();
        factory.enable(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS);
        double[] values = new double[] {
                0.01, -10.5, 2.1e9, 4.0e-8,
                Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY
        };
        for (int i = 0; i < values.length; ++i) {
            int COUNT = 4096;
            // Don't see the failure with a multiple of 1
            int VCOUNT = 2 * COUNT;
            String arrayJson = toJsonArray(values[i], VCOUNT);
            StringBuilder sb = new StringBuilder(COUNT + arrayJson.length() + 20);
            for (int j = 0; j < COUNT; ++j) {
                sb.append(' ');
            }
            sb.append(arrayJson);
            String DOC = sb.toString();
            for (int input = 0; input < 2; ++input) {
                JsonParser jp;
                if (input == 0) {
                    jp = createParserUsingStream(factory, DOC, "UTF-8");
                } else {
                    jp = factory.createParser(DOC);
                }
                assertToken(JsonToken.START_ARRAY, jp.nextToken());
                for (int j = 0; j < VCOUNT; ++j) {
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
                    assertEquals(values[i], jp.getDoubleValue());
                }
                assertToken(JsonToken.END_ARRAY, jp.nextToken());
                jp.close();
            }
        }
    }

    /*
    /**********************************************************
    /* Tests for invalid access
    /**********************************************************
     */
    
    public void testInvalidBooleanAccess() throws Exception
    {
        JsonParser jp = FACTORY.createParser("[ \"abc\" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        try {
            jp.getBooleanValue();
            fail("Expected error trying to call getBooleanValue on non-boolean value");
        } catch (JsonParseException e) {
            verifyException(e, "not of boolean type");
        }
        jp.close();
    }

    public void testInvalidIntAccess() throws Exception
    {
        JsonParser jp = FACTORY.createParser("[ \"abc\" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        try {
            jp.getIntValue();
            fail("Expected error trying to call getIntValue on non-numeric value");
        } catch (JsonParseException e) {
            verifyException(e, "can not use numeric value accessors");
        }
        jp.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private String toJsonArray(double v, int n) {
        StringBuilder sb = new StringBuilder().append('[').append(v);
        for (int i = 1; i < n; ++i) {
            sb.append(',').append(v);
        }
        return sb.append(']').toString();
    }
}
