package com.fasterxml.jackson.core.read;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.InputCoercionException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.json.JsonReadFeature;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
@SuppressWarnings("resource")
public class NumberParsingTest
    extends com.fasterxml.jackson.core.BaseTest
{
    protected JsonFactory jsonFactory() {
        return sharedStreamFactory();
    }

    /*
    /**********************************************************************
    /* Tests, Boolean
    /**********************************************************************
     */

    public void testSimpleBoolean() throws Exception
    {
        _testSimpleBoolean(MODE_INPUT_STREAM);
        _testSimpleBoolean(MODE_INPUT_STREAM_THROTTLED);
        _testSimpleBoolean(MODE_READER);
        _testSimpleBoolean(MODE_DATA_INPUT);
    }

    private void _testSimpleBoolean(int mode) throws Exception
    {
        JsonParser p = createParser(jsonFactory(), mode, "[ true ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertEquals(true, p.getBooleanValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    /*
    /**********************************************************************
    /* Tests, Int
    /**********************************************************************
     */

    public void testSimpleInt() throws Exception
    {
        for (int EXP_I : new int[] { 1234, -999, 0, 1, -2, 123456789 }) {
            _testSimpleInt(EXP_I, MODE_INPUT_STREAM);
            _testSimpleInt(EXP_I, MODE_INPUT_STREAM_THROTTLED);
            _testSimpleInt(EXP_I, MODE_READER);
            _testSimpleInt(EXP_I, MODE_DATA_INPUT);
        }
    }

    private void _testSimpleInt(int EXP_I, int mode) throws Exception
    {
        String DOC = "[ "+EXP_I+" ]";
        JsonParser p = createParser(jsonFactory(), mode, DOC);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        assertTrue(p.isExpectedNumberIntToken());
        assertEquals(""+EXP_I, p.getText());

        if (((short) EXP_I) == EXP_I) {
            assertEquals((short) EXP_I, p.getShortValue());
            if (((byte) EXP_I) == EXP_I) {
                assertEquals((byte) EXP_I, p.getByteValue());
            } else {
                // verify overflow
                try {
                    p.getByteValue();
                    fail("Should get exception for non-byte value "+EXP_I);
                } catch (InputCoercionException e) {
                    verifyException(e, "Numeric value");
                    verifyException(e, "out of range");
                }
            }
        } else {
            // verify overflow
            try {
                p.getShortValue();
                fail("Should get exception for non-short value "+EXP_I);
            } catch (InputCoercionException e) {
                verifyException(e, "Numeric value");
                verifyException(e, "out of range");
            }
        }

        assertEquals(EXP_I, p.getIntValue());
        assertEquals(EXP_I, p.getValueAsInt(EXP_I + 3));
        assertEquals(EXP_I, p.getValueAsInt());
        assertEquals((long) EXP_I, p.getLongValue());
        assertEquals((double) EXP_I, p.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), p.getDecimalValue());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();

        DOC = String.valueOf(EXP_I);
        p = createParser(jsonFactory(), mode, DOC + " "); // DataInput requires separator
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertTrue(p.isExpectedNumberIntToken());
        assertEquals(DOC, p.getText());

        int i = p.getIntValue();

        assertEquals(EXP_I, i);
        assertEquals((long) EXP_I, p.getLongValue());
        assertEquals((double) EXP_I, p.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), p.getDecimalValue());
        p.close();

        // and finally, coercion from double to int; couple of variants
        DOC = String.valueOf(EXP_I)+".0";
        p = createParser(jsonFactory(), mode, DOC + " ");
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertFalse(p.isExpectedNumberIntToken());
        assertEquals(EXP_I, p.getValueAsInt());
        p.close();

        p = createParser(jsonFactory(), mode, DOC + " ");
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(EXP_I, p.getValueAsInt(0));
        p.close();
    }

    public void testIntRange() throws Exception
    {
        // let's test with readers and streams, separate code paths:
        for (int mode : ALL_MODES) {
            String DOC = "[ "+Integer.MAX_VALUE+","+Integer.MIN_VALUE+" ]";
            JsonParser p = createParser(jsonFactory(), mode, DOC);
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

    public void testIntParsing() throws Exception
    {
        char[] testChars = "123456789".toCharArray();

        assertEquals(3, NumberInput.parseInt(testChars, 2, 1));
        assertEquals(123, NumberInput.parseInt(testChars, 0, 3));
        assertEquals(2345, NumberInput.parseInt(testChars, 1, 4));
        assertEquals(9, NumberInput.parseInt(testChars, 8, 1));
        assertEquals(456789, NumberInput.parseInt(testChars, 3, 6));
        assertEquals(23456, NumberInput.parseInt(testChars, 1, 5));
        assertEquals(123456789, NumberInput.parseInt(testChars, 0, 9));

        testChars = "32".toCharArray();
        assertEquals(32, NumberInput.parseInt(testChars, 0, 2));
        testChars = "189".toCharArray();
        assertEquals(189, NumberInput.parseInt(testChars, 0, 3));

        testChars = "10".toCharArray();
        assertEquals(10, NumberInput.parseInt(testChars, 0, 2));
        assertEquals(0, NumberInput.parseInt(testChars, 1, 1));
    }

    public void testIntParsingWithStrings() throws Exception
    {
        assertEquals(3, NumberInput.parseInt("3"));
        assertEquals(3, NumberInput.parseInt("+3"));
        assertEquals(0, NumberInput.parseInt("0"));
        assertEquals(-3, NumberInput.parseInt("-3"));
        assertEquals(27, NumberInput.parseInt("27"));
        assertEquals(-31, NumberInput.parseInt("-31"));
        assertEquals(271, NumberInput.parseInt("271"));
        assertEquals(-131, NumberInput.parseInt("-131"));
        assertEquals(2709, NumberInput.parseInt("2709"));
        assertEquals(-9999, NumberInput.parseInt("-9999"));
        assertEquals(Integer.MIN_VALUE, NumberInput.parseInt(""+Integer.MIN_VALUE));
        assertEquals(Integer.MAX_VALUE, NumberInput.parseInt(""+Integer.MAX_VALUE));
    }

    public void testLongParsingWithStrings() throws Exception
    {
        assertEquals(3, NumberInput.parseLong("3"));
        assertEquals(3, NumberInput.parseLong("+3"));
        assertEquals(0, NumberInput.parseLong("0"));
        assertEquals(-3, NumberInput.parseLong("-3"));
        assertEquals(27, NumberInput.parseLong("27"));
        assertEquals(-31, NumberInput.parseLong("-31"));
        assertEquals(271, NumberInput.parseLong("271"));
        assertEquals(-131, NumberInput.parseLong("-131"));
        assertEquals(2709, NumberInput.parseLong("2709"));
        assertEquals(-9999, NumberInput.parseLong("-9999"));
        assertEquals(1234567890123456789L, NumberInput.parseLong("1234567890123456789"));
        assertEquals(-1234567890123456789L, NumberInput.parseLong("-1234567890123456789"));
        assertEquals(Long.MIN_VALUE, NumberInput.parseLong(""+Long.MIN_VALUE));
        assertEquals(Integer.MIN_VALUE-1, NumberInput.parseLong(""+(Integer.MIN_VALUE-1)));
        assertEquals(Long.MAX_VALUE, NumberInput.parseLong(""+Long.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE+1, NumberInput.parseLong(""+(Integer.MAX_VALUE+1)));
    }

    public void testIntOverflow() {
        try {
            // Integer.MAX_VALUE + 1
            NumberInput.parseInt("2147483648");
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            verifyException(nfe, "For input string: \"2147483648\"");
        }
        try {
            // Integer.MIN_VALUE - 1
            NumberInput.parseInt("-2147483649");
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            verifyException(nfe, "For input string: \"-2147483649\"");
        }
    }

    public void testLongOverflow() {
        try {
            // Long.MAX_VALUE + 1
            NumberInput.parseLong("9223372036854775808");
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            verifyException(nfe, "For input string: \"9223372036854775808\"");
        }
        try {
            // Long.MIN_VALUE - 1
            NumberInput.parseLong("-9223372036854775809");
            fail("expected NumberFormatException");
        } catch (NumberFormatException nfe) {
            verifyException(nfe, "For input string: \"-9223372036854775809\"");
        }
    }

    // Found by oss-fuzzer
    public void testVeryLongIntRootValue() throws Exception
    {
        // For some reason running multiple will tend to hide the issue;
        // possibly due to re-use of some buffers
        _testVeryLongIntRootValue(newStreamFactory(), MODE_DATA_INPUT);
    }

    private void _testVeryLongIntRootValue(JsonFactory jsonF, int mode) throws Exception
    {
        StringBuilder sb = new StringBuilder(250);
        sb.append("-2");
        for (int i = 0; i < 220; ++i) {
            sb.append('0');
        }
        sb.append(' '); // mostly important for DataInput
        String DOC = sb.toString();

        try (JsonParser p = createParser(jsonF, mode, DOC)) {
            assertToken(p.nextToken(), JsonToken.VALUE_NUMBER_INT);
            assertNotNull(p.getBigIntegerValue());
        }
    }

    /*
    /**********************************************************************
    /* Tests, Long
    /**********************************************************************
     */

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

        JsonParser p = createParser(jsonFactory(), mode, "[ "+EXP_L+" ]");
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        // beyond int, should be long
        assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        assertEquals(""+EXP_L, p.getText());

        assertEquals(EXP_L, p.getLongValue());
        // Should get an exception if trying to convert to int
        try {
            p.getIntValue();
        } catch (InputCoercionException e) {
            verifyException(e, "out of range");
            assertEquals(JsonToken.VALUE_NUMBER_INT, e.getInputType());
            assertEquals(Integer.TYPE, e.getTargetType());
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
            long belowMaxLong = -1L + Long.MAX_VALUE;
            long aboveMinLong = 1L + Long.MIN_VALUE;
            String input = "[ "+Long.MAX_VALUE+","+Long.MIN_VALUE+","+aboveMaxInt+", "+belowMinInt+","+belowMaxLong+", "+aboveMinLong+" ]";
            JsonParser p = createParser(jsonFactory(), mode, input);
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

            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
            assertEquals(belowMaxLong, p.getLongValue());

            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
            assertEquals(aboveMinLong, p.getLongValue());


            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    public void testLongParsing() throws Exception
    {
        char[] testChars = "123456789012345678".toCharArray();

        assertEquals(123456789012345678L, NumberInput.parseLong(testChars, 0, testChars.length));
    }

    public void testLongBoundsChecks() throws Exception
    {
        String minLong = String.valueOf(Long.MIN_VALUE).substring(1);
        String belowMinLong = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE)
                .toString().substring(1);
        String maxLong = String.valueOf(Long.MAX_VALUE);
        String aboveMaxLong = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).toString();
        final String VALUE_491 = "1323372036854775807"; // is within range (JACKSON-491)
        final String OVERFLOW =  "9999999999999999999"; // and this one is clearly out

        assertTrue(NumberInput.inLongRange(minLong, true));
        assertTrue(NumberInput.inLongRange(maxLong, false));
        assertTrue(NumberInput.inLongRange(VALUE_491, true));
        assertTrue(NumberInput.inLongRange(VALUE_491, false));

        assertFalse(NumberInput.inLongRange(OVERFLOW, false));
        assertFalse(NumberInput.inLongRange(OVERFLOW, true));
        assertFalse(NumberInput.inLongRange(belowMinLong, true));
        assertFalse(NumberInput.inLongRange(aboveMaxLong, false));

        char[] cbuf = minLong.toCharArray();
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, true));
        cbuf = maxLong.toCharArray();
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));
        cbuf = VALUE_491.toCharArray();
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, true));
        assertTrue(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));

        cbuf = OVERFLOW.toCharArray();
        assertFalse(NumberInput.inLongRange(cbuf, 0, cbuf.length, true));
        assertFalse(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));

        cbuf = aboveMaxLong.toCharArray();
        assertFalse(NumberInput.inLongRange(cbuf, 0, cbuf.length, false));
    }

    /*
    /**********************************************************************
    /* Tests, BigXxx
    /**********************************************************************
     */

    public void testBigDecimalRange() throws Exception
    {
        for (int mode : ALL_MODES) {
            // let's test first values outside of Long range
            BigInteger small = new BigDecimal(Long.MIN_VALUE).toBigInteger();
            small = small.subtract(BigInteger.ONE);
            BigInteger big = new BigDecimal(Long.MAX_VALUE).toBigInteger();
            big = big.add(BigInteger.ONE);
            String input = "[ "+small+"  ,  "+big+"]";
            JsonParser p = createParser(jsonFactory(), mode, input);
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
            JsonParser p = createParser(jsonFactory(), mode, NUMBER_STR +" ");
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
            assertEquals(NUMBER_STR, p.getText());
            assertEquals(biggie, p.getBigIntegerValue());
            p.close();
        }
    }

    /*
    /**********************************************************************
    /* Tests, floating point (basic)
    /**********************************************************************
     */

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

                JsonParser p = createParser(jsonFactory(), mode, DOC+" ");
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
                p = createParser(jsonFactory(), mode, STR + " ");
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

    public void testFloatBoundary146Chars() throws Exception
    {
        final char[] arr = new char[50005];
        for(int i = 500; i != 9000; ++i) {
          java.util.Arrays.fill(arr, 0, i, ' ');
          arr[i] = '-';
          arr[i + 1] = '1';
          arr[i + 2] = 'e';
          arr[i + 3] = '-';
          arr[i + 4] = '1';
          CharArrayReader r = new CharArrayReader(arr, 0, i+5);
          JsonParser p = jsonFactory().createParser(r);
          assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
          p.close();
        }
    }

    public void testFloatBoundary146Bytes() throws Exception
    {
        final byte[] arr = new byte[50005];
        for(int i = 500; i != 9000; ++i) {
            java.util.Arrays.fill(arr, 0, i, (byte) 0x20);
            arr[i] = '-';
            arr[i + 1] = '1';
            arr[i + 2] = 'e';
            arr[i + 3] = '-';
            arr[i + 4] = '1';
            ByteArrayInputStream in = new ByteArrayInputStream(arr, 0, i+5);
            JsonParser p = jsonFactory().createParser(in);
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            p.close();
        }
    }

    /*
    /**********************************************************************
    /* Tests, BigDecimal (core#677)
    /**********************************************************************
     */

    public void testBigBigDecimalsBytesFailByDefault() throws Exception
    {
        _testBigBigDecimals(MODE_INPUT_STREAM, true);
        _testBigBigDecimals(MODE_INPUT_STREAM_THROTTLED, true);
    }

    public void testBigBigDecimalsBytes() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_INPUT_STREAM, false);
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Invalid numeric value ", "exceeds the maximum length");
        }
        try {
            _testBigBigDecimals(MODE_INPUT_STREAM_THROTTLED, false);
            fail("Should not pass");
        } catch (StreamReadException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum length");
        }
    }

    public void testBigBigDecimalsCharsFailByDefault() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_READER, false);
            fail("Should not pass");
        } catch (StreamReadException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum length");
        }
    }

    public void testBigBigDecimalsChars() throws Exception
    {
        _testBigBigDecimals(MODE_READER, true);
    }

    public void testBigBigDecimalsDataInputFailByDefault() throws Exception
    {
        try {
            _testBigBigDecimals(MODE_DATA_INPUT, false);
            fail("Should not pass");
        } catch (StreamReadException jpe) {
            verifyException(jpe, "Invalid numeric value ", "exceeds the maximum length");
        }
    }

    public void testBigBigDecimalsDataInput() throws Exception
    {
        _testBigBigDecimals(MODE_DATA_INPUT, true);
    }

    private void _testBigBigDecimals(final int mode, final boolean enableUnlimitedNumberLen) throws Exception
    {
        final String BASE_FRACTION =
 "01610253934481930774151441507943554511027782188707463024288149352877602369090537"
+"80583522838238149455840874862907649203136651528841378405339370751798532555965157588"
+"51877960056849468879933122908090021571162427934915567330612627267701300492535817858"
+"36107216979078343419634586362681098115326893982589327952357032253344676618872460059"
+"52652865429180458503533715200184512956356092484787210672008123556320998027133021328"
+"04777044107393832707173313768807959788098545050700242134577863569636367439867566923"
+"33479277494056927358573496400831024501058434838492057410330673302052539013639792877"
+"76670882022964335417061758860066263335250076803973514053909274208258510365484745192"
+"39425298649420795296781692303253055152441850691276044546565109657012938963181532017"
+"97420631515930595954388119123373317973532146157980827838377034575940814574561703270"
+"54949003909864767732479812702835339599792873405133989441135669998398892907338968744"
+"39682249327621463735375868408190435590094166575473967368412983975580104741004390308"
+"45302302121462601506802738854576700366634229106405188353120298347642313881766673834"
+"60332729485083952142460470270121052469394888775064758246516888122459628160867190501"
+"92476878886543996441778751825677213412487177484703116405390741627076678284295993334"
+"23142914551517616580884277651528729927553693274406612634848943914370188078452131231"
+"17351787166509190240927234853143290940647041705485514683182501795615082930770566118"
+"77488417962195965319219352314664764649802231780262169742484818333055713291103286608"
+"64318433253572997833038335632174050981747563310524775762280529871176578487487324067"
+"90242862159403953039896125568657481354509805409457993946220531587293505986329150608"
+"18702520420240989908678141379300904169936776618861221839938283876222332124814830207"
+"073816864076428273177778788053613345444299361357958409716099682468768353446625063";

        for (String asText : new String[] {
                "50."+BASE_FRACTION,
                "-37."+BASE_FRACTION,
                "0.00"+BASE_FRACTION,
                "-0.012"+BASE_FRACTION,
                "9999998."+BASE_FRACTION,
                "-8888392."+BASE_FRACTION,
        }) {
            final String DOC = "[ "+asText+" ]";

            JsonFactory jsonFactory = jsonFactory();
            if (enableUnlimitedNumberLen) {
                jsonFactory = jsonFactory
                        .rebuild()
                        .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                        .build();
            }

            try (JsonParser p = createParser(jsonFactory, mode, DOC)) {
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                final BigDecimal exp = new BigDecimal(asText);
                assertEquals(exp, p.getDecimalValue());
            }
        }
    }

    /*
    /**********************************************************************
    /* Tests, misc other
    /**********************************************************************
     */

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

        JsonParser p = createParser(jsonFactory(), mode, DOC);

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
                p = createParserUsingStream(jsonFactory(), DOC, "UTF-8");
            } else {
                p = jsonFactory().createParser(DOC);
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
    // 19-Dec-2022, tatu: Reduce length so as not to hit too-long-number limit
    public void testLongNumbers() throws Exception
    {
        StringBuilder sb = new StringBuilder(900);
        for (int i = 0; i < 900; ++i) {
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
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(10000).build())
                .build();
        _testIssue160LongNumbers(f, DOC, false);
        _testIssue160LongNumbers(f, DOC, true);
    }

    private void _testIssue160LongNumbers(JsonFactory f, String doc, boolean useStream) throws Exception
    {
        JsonParser p = useStream
                ? f.createParser(doc.getBytes("UTF-8"))
                : f.createParser(doc);
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
        JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
                .build();
        _testParsingOfLongerSequencesWithNonNumeric(f, MODE_INPUT_STREAM);
        _testParsingOfLongerSequencesWithNonNumeric(f, MODE_INPUT_STREAM_THROTTLED);
        _testParsingOfLongerSequencesWithNonNumeric(f, MODE_READER);
        _testParsingOfLongerSequencesWithNonNumeric(f, MODE_DATA_INPUT);
    }

    private void _testParsingOfLongerSequencesWithNonNumeric(JsonFactory f, int mode) throws Exception
    {
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
                JsonParser p = createParser(f, mode, DOC);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                for (int j = 0; j < VCOUNT; ++j) {
                    assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
                    double exp = values[i];
                    double act = p.getDoubleValue();
                    if (Double.compare(exp, act) != 0) {
                        fail("Expected at #"+j+" value "+exp+", instead got "+act);
                    }
                    if (Double.isNaN(exp) || Double.isInfinite(exp)) {
                        assertTrue(p.isNaN());
                    } else {
                        assertFalse(p.isNaN());
                    }
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
        JsonParser p = createParser(jsonFactory(), mode, "[ \"abc\" ]");
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
        JsonParser p = createParser(jsonFactory(), mode, "[ \"abc\" ]");
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
        JsonParser p = createParser(jsonFactory(), mode, "[ false ]");
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

        p = jsonFactory().createParser(new StringReader(DOC));
        _testLongerFloat(p, DOC);
        p.close();

        p = jsonFactory().createParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")));
        _testLongerFloat(p, DOC);
        p.close();
    }

    private void _testLongerFloat(JsonParser p, String text) throws IOException
    {
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
        assertEquals(text, p.getText());
        assertNull(p.nextToken());
    }

    public void testInvalidNumber() throws Exception {
        for (int mode : ALL_MODES) {
            JsonParser p = createParser(jsonFactory(), mode, " -foo ");
            try {
                p.nextToken();
                fail("Should not pass");
            } catch (JsonParseException e) {
                verifyException(e, "Unexpected character ('f'");
            }
            p.close();
        }
    }

    public void testNegativeMaxNumberLength() {
        try {
            StreamReadConstraints src = StreamReadConstraints.builder().maxNumberLength(-1).build();
            fail("expected IllegalArgumentException; instead built: "+src);
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "Cannot set maxNumberLength to a negative value");
        }
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
