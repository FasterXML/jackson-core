package com.fasterxml.jackson.core.read;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
@SuppressWarnings("resource")
public class NumberParsingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory FACTORY = new JsonFactory();

    public void testSimpleBoolean() throws Exception
    {
        _testSimpleBoolean(MODE_INPUT_STREAM);
        _testSimpleBoolean(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleBoolean(MODE_READER);
        _testSimpleBoolean(MODE_DATA_INPUT);
    }

    private void _testSimpleBoolean(int mode) throws Exception
    {
        JsonParser p = createParser(mode, "[ true ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(true, p.getBooleanValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    public void testSimpleInt() throws Exception
    {
        for (int EXP_I : new int[] { 1234, -999, 0, 1, -2 }) {
            _testSimpleInt(EXP_I, MODE_INPUT_STREAM);
            _testSimpleInt(EXP_I, MODE_INPUT_STREAM_THROTTLED);
            _testSimpleInt(EXP_I, MODE_READER);
            _testSimpleInt(EXP_I, MODE_DATA_INPUT);
        }
    }

    private void _testSimpleInt(int EXP_I, int mode) throws Exception
    {
        String DOC = "[ "+EXP_I+" ]";
        JsonParser p = createParser(mode, DOC);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertEquals(""+EXP_I, p.getText());

        assertEquals(EXP_I, p.getIntValue());
        assertEquals(EXP_I, p.getValueAsInt(EXP_I + 3));
        assertEquals(EXP_I, p.getValueAsInt());
        assertEquals((long) EXP_I, p.getLongValue());
        assertEquals((double) EXP_I, p.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), p.getDecimalValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        DOC = String.valueOf(EXP_I);
        p = createParser(mode, DOC + " "); // DataInput requires separator
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(DOC, p.getText());

        int i = 0;
        
        try {
            i = p.getIntValue();
        } catch (Exception e) {
            throw new Exception("Failed to parse input '"+DOC+"' (parser of type "+p.getClass().getSimpleName()+")", e);
        }
        assertEquals(EXP_I, i);
        assertEquals((long) EXP_I, p.getLongValue());
        assertEquals((double) EXP_I, p.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), p.getDecimalValue());
        p.close();
    }

    public void testIntRange() throws Exception
    {
        // let's test with readers and streams, separate code paths:
        for (int mode : ALL_MODES) {
            String DOC = "[ "+Integer.MAX_VALUE+","+Integer.MIN_VALUE+" ]";
            JsonParser p = createParser(mode, DOC);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals(Integer.MAX_VALUE, p.getIntValue());
    
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
            assertEquals(Integer.MIN_VALUE, p.getIntValue());
            p.close();
        }
    }

    public void testSimpleLong() throws Exception
    {
        _testSimpleLong(MODE_INPUT_STREAM);
        _testSimpleLong(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleLong(MODE_READER);
        _testSimpleLong(MODE_DATA_INPUT);
    }
    
    private void _testSimpleLong(int mode) throws Exception
    {
        long EXP_L = 12345678907L;
        
        JsonParser p = createParser(mode, "[ "+EXP_L+" ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        // beyond int, should be long
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals(""+EXP_L, p.getText());

        assertEquals(EXP_L, p.getLongValue());
        // Should get an exception if trying to convert to int 
        try {
            p.getIntValue();
        } catch (JsonParseException pe) {
            verifyException(pe, "out of range");
        }
        assertEquals((double) EXP_L, p.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_L), p.getDecimalValue());
        p.close();
    }

    public void testLongRange() throws Exception
    {
        for (int mode : ALL_MODES) {
            long belowMinInt = -1L + Integer.MIN_VALUE;
            long aboveMaxInt = 1L + Integer.MAX_VALUE;
            String input = "[ "+Long.MAX_VALUE+","+Long.MIN_VALUE+","+aboveMaxInt+", "+belowMinInt+" ]";
            JsonParser p = createParser(mode, input);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
            assertEquals(Long.MAX_VALUE, p.getLongValue());
        
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
            assertEquals(Long.MIN_VALUE, p.getLongValue());

            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
            assertEquals(aboveMaxInt, p.getLongValue());

            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
            assertEquals(belowMinInt, p.getLongValue());

            
            assertToken(JsonToken.END_ARRAY, p.nextToken());        
            p.close();
        }
    }

    public void testBigDecimalRange() throws Exception
    {
        for (int mode : ALL_MODES) {
            // let's test first values outside of Long range
            BigInteger small = new BigDecimal(Long.MIN_VALUE).toBigInteger();
            small = small.subtract(BigInteger.ONE);
            BigInteger big = new BigDecimal(Long.MAX_VALUE).toBigInteger();
            big = big.add(BigInteger.ONE);
            String input = "[ "+small+"  ,  "+big+"]";
            JsonParser p = createParser(mode, input);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(small, p.getBigIntegerValue());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(big, p.getBigIntegerValue());
            assertToken(JsonToken.END_ARRAY, p.nextToken());        
            p.close();
        }
    }

    // for [core#78]
    public void testBigNumbers() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 520; ++i) { // input buffer is 512 bytes by default
            sb.append('1');
        }
        final String NUMBER_STR = sb.toString();
        BigInteger biggie = new BigInteger(NUMBER_STR);

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(mode, NUMBER_STR +" ");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(NUMBER_STR, p.getText());
            assertEquals(biggie, p.getBigIntegerValue());
            p.close();
        }
    }
    
    public void testSimpleDouble() throws Exception
    {
        final String[] INPUTS = new String[] {
            "1234.00", "2.1101567E-16", "1.0e5", "0.0", "1.0", "-1.0", 
            "-0.5", "-12.9", "-999.0",
            "2.5e+5", "9e4", "-12e-3", "0.25",
        };
        for (int mode : ALL_MODES) {
            for (int i = 0; i < INPUTS.length; ++i) {

                // First in array
                
                String STR = INPUTS[i];
                double EXP_D = Double.parseDouble(STR);
                String DOC = "["+STR+"]";

                JsonParser p = createParser(mode, DOC+" ");
                assertToken(JsonToken.START_ARRAY, p.nextToken());

                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                assertEquals(STR, p.getText());
                assertEquals(EXP_D, p.getDoubleValue());
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                if (mode != MODE_DATA_INPUT) {
                    assertNull(p.nextToken());
                }
                p.close();

                // then outside
                p = createParser(mode, STR + " ");
                JsonToken t = null;

                try {
                    t = p.nextToken();
                } catch (Exception e) {
                    throw new Exception("Failed to parse input '"+STR+"' (parser of type "+p.getClass().getSimpleName()+")", e);
                }
                
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
                assertEquals(STR, p.getText());
                if (mode != MODE_DATA_INPUT) {
                    assertNull(p.nextToken());
                }
                p.close();
            }
        }
    }

    public void testNumbers() throws Exception
    {
        _testNumbers(MODE_INPUT_STREAM);
        _testNumbers(MODE_INPUT_STREAM_THROTTLED);
        _testNumbers(MODE_READER);
        _testNumbers(MODE_DATA_INPUT);
    }
    
    private void _testNumbers(int mode) throws Exception
    {
        final String DOC = "[ -13, 8100200300, 13.5, 0.00010, -2.033 ]";

        JsonParser p = createParser(mode, DOC);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(-13, p.getIntValue());
        assertEquals(-13L, p.getLongValue());
        assertEquals(-13., p.getDoubleValue());
        assertEquals("-13", p.getText());
        
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(8100200300L, p.getLongValue());
        // Should get exception for overflow:
        try {
            /*int x =*/ p.getIntValue();
            fail("Expected an exception for overflow");
        } catch (Exception e) {
            verifyException(e, "out of range of int");
        }
        assertEquals(8100200300.0, p.getDoubleValue());
        assertEquals("8100200300", p.getText());
        
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(13, p.getIntValue());
        assertEquals(13L, p.getLongValue());
        assertEquals(13.5, p.getDoubleValue());
        assertEquals("13.5", p.getText());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertEquals(0L, p.getLongValue());
        assertEquals(0.00010, p.getDoubleValue());
        assertEquals("0.00010", p.getText());
        
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(-2, p.getIntValue());
        assertEquals(-2L, p.getLongValue());
        assertEquals(-2.033, p.getDoubleValue());
        assertEquals("-2.033", p.getText());

        assertToken(JsonToken.END_ARRAY, p.nextToken());

        p.close();
    }

    public void testLongOverflow() throws Exception
    {
        BigInteger below = BigInteger.valueOf(Long.MIN_VALUE);
        below = below.subtract(BigInteger.ONE);
        BigInteger above = BigInteger.valueOf(Long.MAX_VALUE);
        above = above.add(BigInteger.ONE);

        String DOC_BELOW = below.toString() + " ";
        String DOC_ABOVE = below.toString() + " ";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(mode, DOC_BELOW);
            p.nextToken();
            try {
                long x = p.getLongValue();
                fail("Expected an exception for underflow (input "+p.getText()+"): instead, got long value: "+x);
            } catch (JsonParseException e) {
                verifyException(e, "out of range of long");
            }
            p.close();

            p = createParser(mode, DOC_ABOVE);
            p.nextToken();
            try {
                long x = p.getLongValue();
                fail("Expected an exception for underflow (input "+p.getText()+"): instead, got long value: "+x);
            } catch (JsonParseException e) {
                verifyException(e, "out of range of long");
            }
            p.close();
            
        }
    }
    
    /**
     * Method that tries to test that number parsing works in cases where
     * input is split between buffer boundaries.
     */
    public void testParsingOfLongerSequences() throws Exception
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
            JsonParser p;

            if (input == 0) {
                p = createParserUsingStream(DOC, "UTF-8");
            } else {
                p = FACTORY.createParser(DOC);
            }

            assertToken(JsonToken.START_ARRAY, p.nextToken());
            for (int i = 0; i < COUNT; ++i) {
                for (double d : values) {
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    assertEquals(d, p.getDoubleValue());
                }
            }
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
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
        JsonParser p = useStream
                ? f.createParser(doc.getBytes("UTF-8"))
                        : f.createParser(doc);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(num, p.getText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
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
        JsonParser p = useStream
                ? FACTORY.createParser(doc.getBytes("UTF-8"))
                        : FACTORY.createParser(doc);
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        BigInteger v = p.getBigIntegerValue();
        assertNull(p.nextToken());
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
                JsonParser p;
                if (input == 0) {
                    p = createParserUsingStream(factory, DOC, "UTF-8");
                } else {
                    p = factory.createParser(DOC);
                }
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                for (int j = 0; j < VCOUNT; ++j) {
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    assertEquals(values[i], p.getDoubleValue());
                }
                assertToken(JsonToken.END_ARRAY, p.nextToken());
                p.close();
            }
        }
    }

    /*
    /**********************************************************
    /* Tests for invalid access
    /**********************************************************
     */

    public void testInvalidBooleanAccess() throws Exception {
        _testInvalidBooleanAccess(MODE_INPUT_STREAM);
        _testInvalidBooleanAccess(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidBooleanAccess(MODE_READER);
        _testInvalidBooleanAccess(MODE_DATA_INPUT);
    }

    private void _testInvalidBooleanAccess(int mode) throws Exception
    {
        JsonParser p = createParser(mode, "[ \"abc\" ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        try {
            p.getBooleanValue();
            fail("Expected error trying to call getBooleanValue on non-boolean value");
        } catch (JsonParseException e) {
            verifyException(e, "not of boolean type");
        }
        p.close();
    }

    public void testInvalidIntAccess() throws Exception {
        _testInvalidIntAccess(MODE_INPUT_STREAM);
        _testInvalidIntAccess(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidIntAccess(MODE_READER);
        _testInvalidIntAccess(MODE_DATA_INPUT);
    }
    
    private void _testInvalidIntAccess(int mode) throws Exception
    {
        JsonParser p = createParser(mode, "[ \"abc\" ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        try {
            p.getIntValue();
            fail("Expected error trying to call getIntValue on non-numeric value");
        } catch (JsonParseException e) {
            verifyException(e, "can not use numeric value accessors");
        }
        p.close();
    }

    public void testInvalidLongAccess() throws Exception {
        _testInvalidLongAccess(MODE_INPUT_STREAM);
        _testInvalidLongAccess(MODE_INPUT_STREAM_THROTTLED);
        _testInvalidLongAccess(MODE_READER);
        _testInvalidLongAccess(MODE_DATA_INPUT);
    }
    
    private void _testInvalidLongAccess(int mode) throws Exception
    {
        JsonParser p = createParser(mode, "[ false ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());
        try {
            p.getLongValue();
            fail("Expected error trying to call getLongValue on non-numeric value");
        } catch (JsonParseException e) {
            verifyException(e, "can not use numeric value accessors");
        }
        p.close();
    }

    // [core#317]
    public void testLongerFloatingPoint() throws Exception
    {
        StringBuilder input = new StringBuilder();
        for (int i = 1; i < 201; i++) {
            input.append(1);
        }
        input.append(".0");
        final String DOC = input.toString();

        // test out with both Reader and ByteArrayInputStream
        JsonParser p;

        p = FACTORY.createParser(new StringReader(DOC));
        _testLongerFloat(p, DOC);
        p.close();
        
        p = FACTORY.createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")));
        _testLongerFloat(p, DOC);
        p.close();
    }

    private void _testLongerFloat(JsonParser p, String text) throws IOException
    {
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(text, p.getText());
        assertNull(p.nextToken());
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
