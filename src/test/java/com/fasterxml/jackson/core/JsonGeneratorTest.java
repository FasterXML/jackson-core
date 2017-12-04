package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
import com.fasterxml.jackson.core.json.WriterBasedJsonGenerator;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Unit tests for class {@link JsonGenerator}.
 *
 * @date 2017-09-19
 * @see JsonGenerator
 **/
public class JsonGeneratorTest extends BaseTest {
    public void testWriteObjectThrowsIllegalStateException() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler.BYTE_WRITE_CONCAT_BUFFER, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 2, null, null);

        try {
            uTF8JsonGenerator.writeObject(iOContext);
            fail("Expecting exception: IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testCopyCurrentStructureThrowsIOException() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        CharsToNameCanonicalizer charsToNameCanonicalizer = CharsToNameCanonicalizer.createRoot();
        ReaderBasedJsonParser readerBasedJsonParser = new ReaderBasedJsonParser(iOContext, 0, null, null, charsToNameCanonicalizer);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 55296, null, null);

        try {
            uTF8JsonGenerator.copyCurrentStructure(readerBasedJsonParser);
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteTypeSuffixThrowsIOExceptionOne() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler.CHAR_TEXT_BUFFER, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 2, null, null);
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(uTF8JsonGenerator);
        JsonToken jsonToken = JsonToken.START_ARRAY;
        Object object = new Object();
        WritableTypeId writableTypeId = new WritableTypeId(uTF8JsonGenerator, jsonToken, object);

        try {
            jsonGeneratorDelegate.writeTypeSuffix(writableTypeId);
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteTypeSuffixThrowsIOExceptionTwo() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler.CHAR_TOKEN_BUFFER, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 2, null, null);
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(uTF8JsonGenerator);
        JsonToken jsonToken = JsonToken.START_OBJECT;
        Object object = new Object();
        WritableTypeId writableTypeId = new WritableTypeId(uTF8JsonGenerator, jsonToken, object);

        try {
            jsonGeneratorDelegate.writeTypeSuffix(writableTypeId);
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteEmbeddedObjectWithNull() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler.BYTE_BASE64_CODEC_BUFFER, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 2, null, null);
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(uTF8JsonGenerator);
        jsonGeneratorDelegate.writeEmbeddedObject(null);

        assertEquals(0, jsonGeneratorDelegate.getHighestEscapedChar());
        assertEquals(4, jsonGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteArrayThrowsIllegalArgumentExceptionOne() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler.BYTE_READ_IO_BUFFER, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 2, null, null);

        try {
            uTF8JsonGenerator.writeArray((long[]) null, 2, 3);
            fail("Expecting exception: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteArrayThrowsIllegalArgumentExceptionTwo() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(2);
        PrintStream printStream = new PrintStream(byteArrayBuilder, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, printStream);
        long[] longArray = new long[2];

        try {
            uTF8JsonGenerator.writeArray(longArray, 2, 789);
            fail("Expecting exception: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteArrayThrowsIllegalArgumentExceptionThree() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);
        PrintStream printStream = new PrintStream("g,SR~Z!r&q]mX.`");
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 1, null, printStream);

        try {
            uTF8JsonGenerator.writeArray((int[]) null, 32, 2);
            fail("Expecting exception: IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteOmmittedField() throws IOException {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        CharArrayWriter charArrayWriter = new CharArrayWriter(1);
        WriterBasedJsonGenerator writerBasedJsonGenerator = new WriterBasedJsonGenerator(iOContext, 3, null, charArrayWriter);
        TokenFilter tokenFilter = TokenFilter.INCLUDE_ALL;
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(writerBasedJsonGenerator, tokenFilter, true, true);
        filteringGeneratorDelegate.writeOmittedField("No native support for writing Type Ids");

        assertEquals(0, filteringGeneratorDelegate.getHighestEscapedChar());
        assertEquals(0, filteringGeneratorDelegate.getOutputBuffered());
    }

    public void testWriteBinaryField() throws IOException {
        FilteringGeneratorDelegate filteringGeneratorDelegate = new FilteringGeneratorDelegate(null, null, true, false);
        byte[] byteArray = new byte[1];
        filteringGeneratorDelegate.writeBinaryField("3'O=_albatC' 1|8#", byteArray);

        assertEquals(0, filteringGeneratorDelegate.getFormatFeatures());
    }

    public void testSetSchemaThrowsNullPointerException() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(2);
        PrintStream printStream = new PrintStream(byteArrayBuilder, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, printStream);
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(uTF8JsonGenerator);

        try {
            jsonGeneratorDelegate.setSchema(null);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteObjectRefThrowsIOException() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler.BYTE_WRITE_ENCODING_BUFFER, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 2, null, null);
        JsonGeneratorDelegate jsonGeneratorDelegate = new JsonGeneratorDelegate(uTF8JsonGenerator);

        try {
            jsonGeneratorDelegate.writeObjectRef("Z");
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    public void testWriteObjectIdThrowsIOException() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, true);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(2);
        PrintStream printStream = new PrintStream(byteArrayBuilder, false);
        UTF8JsonGenerator uTF8JsonGenerator = new UTF8JsonGenerator(iOContext, 3, null, printStream);

        try {
            uTF8JsonGenerator.writeObjectId(iOContext);
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonGenerator.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }
}