package tools.jackson.core.json.async;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.core.async.AsyncTestBase;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.*;

class AsyncSimpleObjectTest extends AsyncTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final static String UNICODE_SHORT_NAME = "Unicode"+UNICODE_3BYTES+"RlzOk";

    private final static String UNICODE_LONG_NAME = "Unicode-with-"+UNICODE_3BYTES+"-much-longer";

    @Test
    void booleans()
    {
        final JsonFactory f = JSON_F;
        byte[] data = _jsonDoc(a2q(
"{ 'a':true, 'b':false, 'acdc':true, '"+UNICODE_SHORT_NAME+"':true, 'a1234567':false,"
+"'"+UNICODE_LONG_NAME+"':   true }"));
        // first, no offsets
        _testBooleans(f, data, 0, 100);
        _testBooleans(f, data, 0, 3);
        _testBooleans(f, data, 0, 1);

        // then with some
        _testBooleans(f, data, 1, 100);
        _testBooleans(f, data, 1, 3);
        _testBooleans(f, data, 1, 1);
    }

    private void _testBooleans(JsonFactory f,
            byte[] data, int offset, int readSize)
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        // start with "no token"
        assertNull(r.currentToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("a", r.currentText());
        // by default no cheap access to char[] version:
        assertFalse(r.parser().hasStringCharacters());
        // but...
        char[] ch = r.parser().getTextCharacters();
        assertEquals(0, r.parser().getTextOffset());
        assertEquals(1, r.parser().getTextLength());
        assertEquals("a", new String(ch, 0, 1));
        assertTrue(r.parser().hasStringCharacters());

        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("b", r.currentText());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("acdc", r.currentText());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals(UNICODE_SHORT_NAME, r.currentText());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("a1234567", r.currentText());
        assertToken(JsonToken.VALUE_FALSE, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals(UNICODE_LONG_NAME, r.currentText());
        assertToken(JsonToken.VALUE_TRUE, r.nextToken());

        // and for fun let's verify can't access this as number or binary
        try {
            r.getDoubleValue();
            fail("Should not pass");
        } catch (InputCoercionException e) {
            verifyException(e, "Current token (VALUE_TRUE) not numeric");
        }
        try {
            r.parser().getBinaryValue();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Current token (VALUE_TRUE) not");
            verifyException(e, "can not access as binary");
        }

        assertToken(JsonToken.END_OBJECT, r.nextToken());

        // and end up with "no token" as well
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }

    private final int NUMBER_EXP_I = -123456789;
    private final double NUMBER_EXP_D = 1024798.125;
    private final BigDecimal NUMBER_EXP_BD = new BigDecimal("1243565768679065.1247305834");

    @Test
    void numbers()
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(100);
        JsonFactory f = JSON_F;
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes);
        g.writeStartObject();
        g.writeNumberProperty("i1", NUMBER_EXP_I);
        g.writeNumberProperty("doubley", NUMBER_EXP_D);
        g.writeName("biggieDecimal");
        g.writeNumber(NUMBER_EXP_BD.toString());
        g.writeEndObject();
        g.close();
        byte[] data = bytes.toByteArray();

        // first, no offsets
        _testNumbers(f, data, 0, 100);
        _testNumbers(f, data, 0, 5);
        _testNumbers(f, data, 0, 3);
        _testNumbers(f, data, 0, 2);
        _testNumbers(f, data, 0, 1);

        // then with some
        _testNumbers(f, data, 1, 100);
        _testNumbers(f, data, 1, 3);
        _testNumbers(f, data, 1, 1);
    }

    private void _testNumbers(JsonFactory f,
            byte[] data, int offset, int readSize)
    {
        AsyncReaderWrapper r = asyncForBytes(f, readSize, data, offset);
        // start with "no token"
        assertNull(r.currentToken());
        assertToken(JsonToken.START_OBJECT, r.nextToken());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("i1", r.currentText());
        assertToken(JsonToken.VALUE_NUMBER_INT, r.nextToken());
        assertEquals(NumberType.INT, r.getNumberType());
        assertEquals(NUMBER_EXP_I, r.getIntValue());
        assertEquals((double)NUMBER_EXP_I, r.getDoubleValue());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("doubley", r.currentText());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, r.nextToken());
        assertEquals(NumberType.DOUBLE, r.getNumberType());
        assertEquals(NUMBER_EXP_D, r.getDoubleValue());
        assertEquals((long) NUMBER_EXP_D, r.getLongValue());

        assertToken(JsonToken.PROPERTY_NAME, r.nextToken());
        assertEquals("biggieDecimal", r.currentText());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, r.nextToken());
        // can't really tell double/BigDecimal apart in plain json
        assertEquals(NumberType.DOUBLE, r.getNumberType());
        assertEquals(NUMBER_EXP_BD, r.getDecimalValue());
        assertEquals(""+NUMBER_EXP_BD, r.currentText());

        assertToken(JsonToken.END_OBJECT, r.nextToken());

        // and end up with "no token" as well
        assertNull(r.nextToken());
        assertTrue(r.isClosed());
    }
}
