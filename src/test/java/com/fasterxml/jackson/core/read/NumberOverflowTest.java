package com.fasterxml.jackson.core.read;

import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.InputCoercionException;

public class NumberOverflowTest
    extends com.fasterxml.jackson.core.BaseTest
{
    private final JsonFactory FACTORY = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(1000000).build())
            .build();

    // NOTE: this should be long enough to trigger perf problems
    // 19-
    private final static int BIG_NUM_LEN = 199999;
    private final static String BIG_POS_INTEGER;
    static {
        StringBuilder sb = new StringBuilder(BIG_NUM_LEN);
        for (int i = 0; i < BIG_NUM_LEN; ++i) {
            sb.append('9');
        }
        BIG_POS_INTEGER = sb.toString();
    }

    private final static String BIG_POS_DOC = "["+BIG_POS_INTEGER+"]";
    private final static String BIG_NEG_DOC = "[ -"+BIG_POS_INTEGER+"]";

    public void testSimpleLongOverflow() throws Exception
    {
        BigInteger below = BigInteger.valueOf(Long.MIN_VALUE);
        below = below.subtract(BigInteger.ONE);
        BigInteger above = BigInteger.valueOf(Long.MAX_VALUE);
        above = above.add(BigInteger.ONE);

        String DOC_BELOW = below.toString() + " ";
        String DOC_ABOVE = below.toString() + " ";

        for (int mode : ALL_MODES) {
            JsonParser p = createParser(FACTORY, mode, DOC_BELOW);
            p.nextToken();
            try {
                long x = p.getLongValue();
                fail("Expected an exception for underflow (input "+p.getText()+"): instead, got long value: "+x);
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of long");
            }
            p.close();

            p = createParser(mode, DOC_ABOVE);
            p.nextToken();
            try {
                long x = p.getLongValue();
                fail("Expected an exception for underflow (input "+p.getText()+"): instead, got long value: "+x);
            } catch (InputCoercionException e) {
                verifyException(e, "out of range of long");
            }
            p.close();
        }
    }

    // Note: only 4 cardinal types; `short`, `byte` and `char` use same code paths
    // Note: due to [jackson-core#493], we'll skip DataInput-backed parser

    // [jackson-core#488]
    public void testMaliciousLongOverflow() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            for (String doc : new String[] { BIG_POS_DOC, BIG_NEG_DOC }) {
                JsonParser p = createParser(FACTORY, mode, doc);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                try {
                    p.getLongValue();
                    fail("Should not pass");
                } catch (InputCoercionException e) {
                    verifyException(e, "out of range of long");
                    verifyException(e, "Integer with "+BIG_NUM_LEN+" digits");
                }
                p.close();
            }
        }
    }

    // [jackson-core#488]
    public void testMaliciousIntOverflow() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            for (String doc : new String[] { BIG_POS_DOC, BIG_NEG_DOC }) {
                JsonParser p = createParser(FACTORY, mode, doc);
                assertToken(JsonToken.START_ARRAY, p.nextToken());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                try {
                    p.getIntValue();
                    fail("Should not pass");
                } catch (InputCoercionException e) {
                    verifyException(e, "out of range of int");
                    verifyException(e, "Integer with "+BIG_NUM_LEN+" digits");
                }
                p.close();
            }
        }
    }

    // [jackson-core#488]
    public void testMaliciousBigIntToDouble() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            final String doc = BIG_POS_DOC;
            JsonParser p = createParser(FACTORY, mode, doc);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            double d = p.getDoubleValue();
            assertEquals(Double.valueOf(BIG_POS_INTEGER), d);
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    // [jackson-core#488]
    public void testMaliciousBigIntToFloat() throws Exception
    {
        for (int mode : ALL_STREAMING_MODES) {
            final String doc = BIG_POS_DOC;
            JsonParser p = createParser(FACTORY, mode, doc);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            float f = p.getFloatValue();
            assertEquals(Float.valueOf(BIG_POS_INTEGER), f);
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }
}
