package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectReadContext;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.sym.ByteQuadsCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;
import org.junit.Test;

import java.io.*;

// !!! 11-Oct-2018, tatu: I don't think these are valid tests. Presumably were added
//   to increase code coverage but it's not a good way to exercise functionality to
//   into arbitrary internal methods. Instead, functionality should be exercised using
//   public API. Left half of tests but will probably just delete in future.

/**
 * Unit tests for class {@link UTF8DataInputJsonParser}.
 *
 * @see UTF8DataInputJsonParser
 */
@SuppressWarnings("resource")
public class UTF8DataInputJsonParserTest extends BaseTest
{
    @Test
    public void testNextFieldNameThrowsIOException() {
        IOContext ioContext = new IOContext(new BufferRecycler(), this, false);
        byte[] byteArray = new byte[20];
        byteArray[0] = (byte) 47;
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 100, 0, dataInputStream, null, -2624);

        try {
            uTF8DataInputJsonParser.nextFieldName();
            fail("Expecting exception: IOException");
        } catch (IOException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
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
                ioContext, 131, 0, dataInputStream, byteQuadsCanonicalizer, (byte) 57);
        int[] intArray = new int[3];

        try {
            uTF8DataInputJsonParser.parseEscapedName(intArray, 56, (byte) 72, (byte) 127, (byte) 57);
            fail("Expecting exception: ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException e) {
            assertEquals(UTF8DataInputJsonParser.class.getName(), e.getStackTrace()[0].getClassName());
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
                ioContext, 42, 0, dataInputStream, byteQuadsCanonicalizer, 0);
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
                ioContext, 42, 0, dataInputStream, ByteQuadsCanonicalizer.createRoot(), 0);
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
                ioContext, 500, 0, dataInputStream, ByteQuadsCanonicalizer.createRoot(), 1);

        assertEquals(466, uTF8DataInputJsonParser.getValueAsInt(466));
    }

    @Test
    public void testGetValueAsIntTakingNoArguments() throws IOException {
        byte[] byteArray = new byte[2];
        IOContext ioContext = new IOContext(new BufferRecycler(), byteArray, false);
        InputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);
        DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
        UTF8DataInputJsonParser uTF8DataInputJsonParser = new UTF8DataInputJsonParser(ObjectReadContext.empty(),
                ioContext, 42, 0, dataInputStream, ByteQuadsCanonicalizer.createRoot(), 0);

        assertEquals(0, uTF8DataInputJsonParser.getValueAsInt());
    }
}