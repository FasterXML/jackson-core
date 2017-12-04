package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import org.junit.Ignore;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertArrayEquals;

/**
 * Unit tests for class {@link FilteringGeneratorDelegate}.
 *
 * @date 2017-09-19
 * @see FilteringGeneratorDelegate
 **/
public class FilteringGeneratorDelegateTest extends BaseTest {
    public void test_checkPropertyParentPath() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);

        assertEquals(0, filteringGeneratorDelegate.getMatchCount());

        filteringGeneratorDelegate._checkPropertyParentPath();

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteTypeIdThrowsIOException() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, byteArrayBuilder);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, false, false);
        Object object = new Object();

        assertFalse(filteringGeneratorDelegate.canWriteTypeId());

        try {
            filteringGeneratorDelegate.writeTypeId(object);
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteTypeIdDoesNotRaiseException() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, true, false);
        filteringGeneratorDelegate.writeTypeId("myId");
    }

    public void testWriteObjectRefThrowsNullPointerException() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, false, false);

        try {
            filteringGeneratorDelegate.writeObjectRef(tokenFilter);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteObjectRefDoesNotThrowException() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, true, true);
        PipedInputStream pipedInputStream = new PipedInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(pipedInputStream);

        filteringGeneratorDelegate.writeObjectRef(bufferedInputStream);
    }

    public void testWriteObjectIdThrowsNullPointerException() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, true, true);

        try {
            filteringGeneratorDelegate.writeObjectId(null);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteObjectIdDoesNotThrowException() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayBuilder);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 1666, null, objectOutputStream, byteArrayBuilder.NO_BYTES, 1, false);
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, null, false, false);

        assertFalse(filteringGeneratorDelegate.canWriteObjectId());

        filteringGeneratorDelegate.writeObjectId(objectOutputStream);

        assertFalse(filteringGeneratorDelegate.canWriteObjectId());
    }

    public void testWriteOmittedField() throws IOException {
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(null);
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(jsonGeneratorDelegate, null, false, false);
        filteringGeneratorDelegate.writeOmittedField("asdf");
        filteringGeneratorDelegate.writeOmittedField(null);

        assertEquals(0, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteNull() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeNull();

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());

        filteringGeneratorDelegate.writeNull();

        assertEquals(2, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteBoolean() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeBoolean(true);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteNumberTakingStringOne() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, true, true);

        assertFalse(filteringGeneratorDelegate.canWriteFormattedNumbers());

        filteringGeneratorDelegate.writeNumber("");
        filteringGeneratorDelegate.writeNumber("1.1");

        assertFalse(filteringGeneratorDelegate.canWriteFormattedNumbers());
    }

    public void testWriteNumberTakingStringTwo() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeNumber("");
        filteringGeneratorDelegate.writeNumber("1.1");

        assertEquals(2, filteringGeneratorDelegate.getMatchCount());
    }

    @Ignore("I assume this to be a defect.")
    public void testWriteNumberTakingBigDecimalThrowsNullPointerException() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, true, true);

        try {
            filteringGeneratorDelegate.writeNumber((BigDecimal) null);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testSetFieldNameAndWriteNumberTakingFloatWithNegative() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, false, false);
        filteringGeneratorDelegate._checkBinaryWrite();
        filteringGeneratorDelegate.writeFieldName("test field");
        filteringGeneratorDelegate.writeNumber((float) (-1675));

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteNumberTakingFloatWithPositive() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeNumber((float) 57343);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(7, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteNumberTakingDoubleWithNegative() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder();
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 0, null, byteArrayBuilder);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeNumber((-1038.44));

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(8, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteNumberTakingBigInteger() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        byte[] byteArray = new byte[1];
        BigInteger bigInteger = new BigInteger(byteArray);
        filteringGeneratorDelegate.writeNumber(bigInteger);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(1, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteNumberTakingLongWithPositive() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 0, null, null);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeNumber(3156L);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(4, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteNumberTakingIntWithZero() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 1, null, pipedOutputStream);
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(uTF8JsonGenerator);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(jsonGeneratorDelegate, tokenFilter, false, true);
        filteringGeneratorDelegate.writeNumber(0);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(1, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteNumberTakingShort() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        Object object = new Object();
        IOContext iOContext = new IOContext(bufferRecycler, object, true);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder();
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 0, null, byteArrayBuilder);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeNumber((short) 855);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(3, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteBinaryTaking3ArgumentsThrowsNullPointerException() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(pipedOutputStream, 91);
        byte[] byteArray = new byte[6];
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 0, null, bufferedOutputStream, byteArray, 3, false);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, false, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray, 1, 55296);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(byteArrayInputStream);
        DataInputStream dataInputStream = new DataInputStream(bufferedInputStream);

        try {
            filteringGeneratorDelegate.writeBinary(null, dataInputStream, 2);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(UTF8JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteRawValueThrowsNullPointerException() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, false, false);
        char[] charArray = new char[7];

        try {
            filteringGeneratorDelegate.writeRawValue(charArray, 100, 100);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteRawValueOne() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, false, true);
        char[] charArray = new char[7];
        filteringGeneratorDelegate.writeRawValue(charArray, 2923, 0);

        assertEquals(0, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteRawValueThrowsNullPointerExceptionTwo() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, false, false);

        try {
            filteringGeneratorDelegate.writeRawValue("o<jwxaa?Lf:$|9", (-317), 1380);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteRawValueTwo() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, false, true);
        filteringGeneratorDelegate.writeRawValue("", 125, 125);

        assertEquals(0, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteRawValueTakingNonEmptyStringW() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, byteArrayBuilder);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, false, false);
        filteringGeneratorDelegate.writeRawValue("OgzE");

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(4, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteEmptyUTF8String() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, false, false);
        byte[] byteArray = new byte[0];
        filteringGeneratorDelegate.writeUTF8String(byteArray, 0, 0);

        assertArrayEquals(new byte[]{}, byteArray);
    }

    public void testWriteRawUTF8StringThrowsNullPointerException() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, false, false);
        byte[] byteArray = new byte[4];

        try {
            filteringGeneratorDelegate.writeRawUTF8String(byteArray, (byte) 0, (byte) 0);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteRawUTF8String() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayBuilder);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 1666, null, objectOutputStream, byteArrayBuilder.NO_BYTES, 1, false);
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, null, false, false);
        filteringGeneratorDelegate.writeRawUTF8String(byteArrayBuilder.NO_BYTES, 49, (-1));

        assertEquals(0, filteringGeneratorDelegate.getMatchCount());
        assertEquals(1, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteStringTakingSerializableStringThrowsNullPointerException() throws IOException {
        TokenFilter tokenFilter = new TokenFilter();
        SerializedString serializedString = DefaultPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR;
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, tokenFilter, false, false);

        try {
            filteringGeneratorDelegate.writeString(serializedString);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(FilteringGeneratorDelegate.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteStringTaking3ArgumentsWithNegative() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, false, false);
        char[] charArray = new char[4];
        filteringGeneratorDelegate.writeString(charArray, (-1), 1);

        assertEquals(0, filteringGeneratorDelegate.getMatchCount());
    }

    public void testWriteStringTaking3ArgumentsWithPositive() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, byteArrayBuilder);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, false, false);
        char[] charArray = new char[4];
        filteringGeneratorDelegate.writeString(charArray, 3, 1);

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(8, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteStringTakingNonEmptyString() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, byteArrayBuilder);
        TokenFilter tokenFilter = new TokenFilter();
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(uTF8JsonGenerator, tokenFilter, false, false);
        filteringGeneratorDelegate.writeString("OgzE");

        assertEquals(1, filteringGeneratorDelegate.getMatchCount());
        assertEquals(6, filteringGeneratorDelegate.getOutputBuffered());
    }
}