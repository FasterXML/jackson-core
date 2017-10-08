package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectReadContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import org.junit.Test;

import java.io.*;

/**
 * Unit tests for class {@link UTF8DataInputJsonParser}.
 *
 * @see UTF8DataInputJsonParser
 */
@SuppressWarnings("resource")
public class UTF8DataInputJsonParserTest extends BaseTest
{
    @Test
    public void test_decodeBase64ThrowsEOFException() throws IOException {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, true);
        byte[] byteArray = new byte[5];
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, (byte) 26, dataInputStream, byteQuadsCanonicalizer, 3);

        try {
            uTF8DataInputJsonParser._decodeBase64(null);
            fail("Expecting exception: EOFException");
        } catch (EOFException e) {
            assertEquals(DataInputStream.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void test_skipStringThrowsIOException() {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[12];
        byteArray[4] = (byte) (-10);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 100, dataInputStream, null, 11);

        try {
            uTF8DataInputJsonParser._skipString();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testNextBooleanValueThrowsIOException() {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[12];
        byteArray[4] = (byte) (-10);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 100, dataInputStream, null, 11);

        try {
            uTF8DataInputJsonParser.nextBooleanValue();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testNextTextValueThrowsIOException() {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[20];
        byteArray[0] = (byte) 47;
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 915, dataInputStream, null, (byte) 47);

        try {
            uTF8DataInputJsonParser.nextTextValue();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testNextFieldNameThrowsIOException() {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[20];
        byteArray[0] = (byte) 47;
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 100, dataInputStream, null, -2624);

        try {
            uTF8DataInputJsonParser.nextFieldName();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void test_handleAposThrowsIOException() {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[7];
        byteArray[0] = (byte) (-80);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 3, dataInputStream, byteQuadsCanonicalizer, 1);

        try {
            uTF8DataInputJsonParser._handleApos();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void test_parseAposNameThrowsEOFException() throws IOException {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[17];
        byteArray[4] = (byte) 43;
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 42, dataInputStream, byteQuadsCanonicalizer, 0);

        try {
            uTF8DataInputJsonParser._parseAposName();
            fail("Expecting exception: EOFException");
        } catch (EOFException e) {
            assertEquals(DataInputStream.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testParseEscapedNameThrowsArrayIndexOutOfBoundsException() throws IOException {
        IOContext ioContext = new IOContext((BufferRecycler) null, null, false);
        PipedOutputStream pipedOutputStream = new PipedOutputStream();
        PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream, 131);
        DataInputStream dataInputStream = new DataInputStream(pipedInputStream);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 131, dataInputStream, byteQuadsCanonicalizer, (byte) 57);
        int[] intArray = new int[3];

        try {
            uTF8DataInputJsonParser.parseEscapedName(intArray, 56, (byte) 72, (byte) 127, (byte) 57);
            fail("Expecting exception: ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(UTF8DataInputJsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void test_parseNegNumberThrowsIOException() throws IOException {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[20];
        byteArray[2] = (byte) 73;
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 100, dataInputStream, null, 3);
        dataInputStream.readUnsignedShort();

        try {
            uTF8DataInputJsonParser._parseNegNumber();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void test_parsePosNumber() throws IOException {
        byte[] byteArray = new byte[2];
        byteArray[0] = (byte) 51;
        byteArray[1] = (byte) 22;
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 1568, dataInputStream, byteQuadsCanonicalizer, 13);
        JsonToken jsonToken = uTF8DataInputJsonParser._parsePosNumber(7);

        assertEquals(7, jsonToken.id());
        assertNull(jsonToken.asString());
    }

    @Test
    public void test_readBinaryThrowsNullPointerException() throws IOException {
        byte[] byteArray = new byte[5];
        byteArray[4] = (byte) 43;
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 500, dataInputStream, byteQuadsCanonicalizer, 1);

        try {
            uTF8DataInputJsonParser._readBinary(null, null, byteArray);
            fail("Expecting exception: NullPointerException");
        } catch (NullPointerException e) {
            assertEquals(UTF8DataInputJsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testReadBinaryValueThrowsIOException() {
        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext ioContext = new IOContext(bufferRecycler, this, false);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, (-53), null, byteQuadsCanonicalizer, 48);
        ByteArrayBuilder byteArrayBuilder = new ByteArrayBuilder(bufferRecycler, 1);

        try {
            uTF8DataInputJsonParser.readBinaryValue(null, byteArrayBuilder);
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }
    }

    @Test
    public void testGetTextOffsetAndNextFieldName() throws IOException {
        byte[] byteArray = new byte[2];
        byteArray[1] = (byte) 91;
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        ByteQuadsCanonicalizer byteQuadsCanonicalizer = ByteQuadsCanonicalizer.createRoot();
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 42, dataInputStream, byteQuadsCanonicalizer, 0);
        assertEquals(0, uTF8DataInputJsonParser.getTextOffset());
        assertNull(uTF8DataInputJsonParser.nextFieldName());
    }

    @Test
    public void testGetNextFieldNameAndGetTextLength() throws IOException {
        byte[] byteArray = new byte[2];
        byteArray[1] = (byte) 91;
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 42, dataInputStream, ByteQuadsCanonicalizer.createRoot(), 0);
        uTF8DataInputJsonParser.nextFieldName();

        assertNull(uTF8DataInputJsonParser.getObjectId());
        assertEquals(1, uTF8DataInputJsonParser.getTextLength());
    }

    @Test
    public void testGetValueAsIntTakingInt() throws IOException {
        byte[] byteArray = new byte[5];
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 500, dataInputStream, ByteQuadsCanonicalizer.createRoot(), 1);

        assertEquals(466, uTF8DataInputJsonParser.getValueAsInt(466));
    }

    @Test
    public void testGetValueAsIntTakingNoArguments() throws IOException {
        byte[] byteArray = new byte[2];
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 42, dataInputStream, ByteQuadsCanonicalizer.createRoot(), 0);

        assertEquals(0, uTF8DataInputJsonParser.getValueAsInt());
    }
}