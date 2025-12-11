package tools.jackson.core.unittest.base;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link tools.jackson.core.base.ParserBase} number conversion methods,
 * particularly testing the internal number representation conversion logic.
 */
public class ParserBaseNumberConversionTest extends JacksonCoreTestBase
{
    /**
     * Test conversion from int to other number types
     */
    @Test
    void intToOtherTypes() throws Exception
    {
        _testIntToOtherTypes(MODE_READER);
        _testIntToOtherTypes(MODE_INPUT_STREAM);
    }

    private void _testIntToOtherTypes(int mode) throws Exception
    {
        String json = "{\"value\": 42}";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

            // Access as int first
            assertEquals(42, p.getIntValue());

            // Then convert to long
            assertEquals(42L, p.getLongValue());

            // Convert to BigInteger
            assertEquals(BigInteger.valueOf(42), p.getBigIntegerValue());

            // Convert to float
            assertEquals(42.0f, p.getFloatValue());

            // Convert to double
            assertEquals(42.0, p.getDoubleValue());

            // Convert to BigDecimal
            assertEquals(new BigDecimal("42"), p.getDecimalValue());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test conversion from long to other number types
     */
    @Test
    void longToOtherTypes() throws Exception
    {
        _testLongToOtherTypes(MODE_READER);
        _testLongToOtherTypes(MODE_INPUT_STREAM);
    }

    private void _testLongToOtherTypes(int mode) throws Exception
    {
        // Use a number larger than int max
        String json = "{\"value\": 9876543210}";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

            // Access as long first
            assertEquals(9876543210L, p.getLongValue());

            // Convert to BigInteger
            assertEquals(BigInteger.valueOf(9876543210L), p.getBigIntegerValue());

            // Convert to double
            assertEquals(9876543210.0, p.getDoubleValue());

            // Convert to BigDecimal
            assertEquals(new BigDecimal("9876543210"), p.getDecimalValue());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test conversion from double to other number types
     */
    @Test
    void doubleToOtherTypes() throws Exception
    {
        _testDoubleToOtherTypes(MODE_READER);
        _testDoubleToOtherTypes(MODE_INPUT_STREAM);
    }

    private void _testDoubleToOtherTypes(int mode) throws Exception
    {
        String json = "{\"value\": 123.456}";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

            // Access as double first
            assertEquals(123.456, p.getDoubleValue(), 0.001);

            // Convert to float
            assertEquals(123.456f, p.getFloatValue(), 0.001);

            // Convert to BigDecimal
            BigDecimal bd = p.getDecimalValue();
            assertTrue(bd.doubleValue() > 123.4 && bd.doubleValue() < 123.5);

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test conversion from BigInteger to other number types
     */
    @Test
    void bigIntegerToOtherTypes() throws Exception
    {
        _testBigIntegerToOtherTypes(MODE_READER);
        _testBigIntegerToOtherTypes(MODE_INPUT_STREAM);
    }

    private void _testBigIntegerToOtherTypes(int mode) throws Exception
    {
        // Use a very large number that exceeds long
        String json = "{\"value\": 12345678901234567890123456789}";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());

            // Access as BigInteger first
            BigInteger bigInt = p.getBigIntegerValue();
            assertEquals(new BigInteger("12345678901234567890123456789"), bigInt);

            // Convert to BigDecimal
            BigDecimal bd = p.getDecimalValue();
            assertEquals(new BigDecimal("12345678901234567890123456789"), bd);

            // Convert to double (will lose precision)
            double d = p.getDoubleValue();
            assertTrue(d > 1.0e28);

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test conversion from BigDecimal to other number types
     */
    @Test
    void bigDecimalToOtherTypes() throws Exception
    {
        _testBigDecimalToOtherTypes(MODE_READER);
        _testBigDecimalToOtherTypes(MODE_INPUT_STREAM);
    }

    private void _testBigDecimalToOtherTypes(int mode) throws Exception
    {
        // Use a precise decimal number
        String json = "{\"value\": 123.456789012345}";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());

            // Access as BigDecimal first
            BigDecimal bd = p.getDecimalValue();
            assertTrue(bd.toString().startsWith("123.456789"));

            // Convert to double
            assertEquals(123.456789012345, p.getDoubleValue(), 0.000001);

            // Convert to float (will lose precision)
            float f = p.getFloatValue();
            assertEquals(123.456789f, f, 0.01f);

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test number type detection for various integer sizes
     */
    @Test
    void numberTypeDetection() throws Exception
    {
        _testNumberTypeDetection(MODE_READER);
        _testNumberTypeDetection(MODE_INPUT_STREAM);
    }

    private void _testNumberTypeDetection(int mode) throws Exception
    {
        // Small int
        String json1 = "42";
        try (JsonParser p = createParser(mode, json1)) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.INT, p.getNumberType());
        }

        // Large long
        String json2 = "9876543210";
        try (JsonParser p = createParser(mode, json2)) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.LONG, p.getNumberType());
        }

        // Float
        String json3 = "123.456";
        try (JsonParser p = createParser(mode, json3)) {
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            JsonParser.NumberType nt = p.getNumberType();
            assertTrue(nt == JsonParser.NumberType.DOUBLE || nt == JsonParser.NumberType.BIG_DECIMAL);
        }

        // Very large BigInteger
        String json4 = "12345678901234567890123456789";
        try (JsonParser p = createParser(mode, json4)) {
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(JsonParser.NumberType.BIG_INTEGER, p.getNumberType());
        }
    }

    /**
     * Test number value retrieval with different access patterns
     */
    @Test
    void numberValueAccessPatterns() throws Exception
    {
        _testNumberValueAccessPatterns(MODE_READER);
        _testNumberValueAccessPatterns(MODE_INPUT_STREAM);
    }

    private void _testNumberValueAccessPatterns(int mode) throws Exception
    {
        String json = "[42, 123.456, 9876543210, 12345678901234567890123456789]";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());

            // First: small int
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(42, p.getIntValue());
            assertEquals(42L, p.getLongValue());
            assertEquals(42.0, p.getDoubleValue());

            // Second: decimal
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(123.456, p.getDoubleValue(), 0.001);
            assertEquals(123.456f, p.getFloatValue(), 0.001);

            // Third: long
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(9876543210L, p.getLongValue());

            // Fourth: BigInteger
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(new BigInteger("12345678901234567890123456789"),
                        p.getBigIntegerValue());

            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }

    /**
     * Test edge cases for number conversion
     */
    @Test
    void numberConversionEdgeCases() throws Exception
    {
        _testNumberConversionEdgeCases(MODE_READER);
        _testNumberConversionEdgeCases(MODE_INPUT_STREAM);
    }

    private void _testNumberConversionEdgeCases(int mode) throws Exception
    {
        // Zero
        String json = "{\"zero\": 0, \"negZero\": -0, \"one\": 1, \"negOne\": -1}";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            // zero
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("zero", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(0, p.getIntValue());
            assertEquals(0L, p.getLongValue());
            assertEquals(0.0, p.getDoubleValue());
            assertEquals(BigInteger.ZERO, p.getBigIntegerValue());

            // -0
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("negZero", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(0, p.getIntValue());

            // 1
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("one", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());

            // -1
            assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
            assertEquals("negOne", p.currentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(-1, p.getIntValue());

            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
    }

    /**
     * Test negative numbers conversion
     */
    @Test
    void negativeNumberConversions() throws Exception
    {
        _testNegativeNumberConversions(MODE_READER);
        _testNegativeNumberConversions(MODE_INPUT_STREAM);
    }

    private void _testNegativeNumberConversions(int mode) throws Exception
    {
        String json = "[-42, -123.456, -9876543210]";
        try (JsonParser p = createParser(mode, json)) {
            assertToken(JsonToken.START_ARRAY, p.nextToken());

            // Negative int
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(-42, p.getIntValue());
            assertEquals(-42L, p.getLongValue());
            assertEquals(-42.0, p.getDoubleValue());

            // Negative decimal
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, p.nextToken());
            assertEquals(-123.456, p.getDoubleValue(), 0.001);
            assertEquals(-123.456f, p.getFloatValue(), 0.001);

            // Negative long
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(-9876543210L, p.getLongValue());
            assertEquals(-9876543210.0, p.getDoubleValue());

            assertToken(JsonToken.END_ARRAY, p.nextToken());
        }
    }
}
