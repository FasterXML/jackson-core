package com.fasterxml.jackson.core.write;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamWriteException;

public class BoundsChecksWithGeneratorTest
    extends com.fasterxml.jackson.core.BaseTest
{
    final static JsonFactory JSON_F = new JsonFactory();

    interface GeneratorCreator {
        JsonGenerator create() throws Exception;
    }

    private final GeneratorCreator BYTE_GENERATOR_CREATOR = new GeneratorCreator() {
        @Override
        public JsonGenerator create() throws Exception {
            return JSON_F.createGenerator(new ByteArrayOutputStream());
        }
    };

    private final GeneratorCreator CHAR_GENERATOR_CREATOR = new GeneratorCreator() {
        @Override
        public JsonGenerator create() throws Exception {
            return JSON_F.createGenerator(new StringWriter());
        }
    };

    interface ByteBackedOperation {
        void call(JsonGenerator g, byte[] data, int offset, int len) throws Exception;
    }

    interface CharBackedOperation {
        void call(JsonGenerator g, char[] data, int offset, int len) throws Exception;
    }

    interface StringBackedOperation {
        public void call(JsonGenerator g, String data, int offset, int len) throws Exception;
    }

    /*
    /**********************************************************************
    /* Test methods, byte[] backed
    /**********************************************************************
     */

    // // // Individual generator calls to check, byte[]-backed

    private final ByteBackedOperation WRITE_BINARY_FROM_BYTES = new ByteBackedOperation() {
        @Override
        public void call(JsonGenerator g, byte[] data, int offset, int len) throws Exception {
            g.writeBinary(data, offset, len);
        }
    };

    private final ByteBackedOperation WRITE_RAW_UTF8_FROM_BYTES = new ByteBackedOperation() {
        @Override
        public void call(JsonGenerator g, byte[] data, int offset, int len) throws Exception {
            g.writeRawUTF8String(data, offset, len);
        }
    };

    private final ByteBackedOperation WRITE_UTF8_STRING_FROM_BYTES = new ByteBackedOperation() {
        @Override
        public void call(JsonGenerator g, byte[] data, int offset, int len) throws Exception {
            g.writeUTF8String(data, offset, len);
        }
    };

    public void testBoundsWithByteArrayInputFromBytes() throws Exception {
        _testBoundsWithByteArrayInput(BYTE_GENERATOR_CREATOR, true);
    }

    public void testBoundsWithByteArrayInputFromChars() throws Exception {
        _testBoundsWithByteArrayInput(CHAR_GENERATOR_CREATOR, false);
    }

    private void _testBoundsWithByteArrayInput(GeneratorCreator genc, boolean byteBacked)
            throws Exception {
        _testBoundsWithByteArrayInput(genc, WRITE_BINARY_FROM_BYTES);
        // NOTE: byte[] writes of pre-encoded UTF-8 not supported for Writer-backed
        // generator, so:
        if (byteBacked) {
            _testBoundsWithByteArrayInput(genc, WRITE_RAW_UTF8_FROM_BYTES);
            _testBoundsWithByteArrayInput(genc, WRITE_UTF8_STRING_FROM_BYTES);
        }
    }

    private void _testBoundsWithByteArrayInput(GeneratorCreator genc,
            ByteBackedOperation oper) throws Exception {
        final byte[] BYTES10 = new byte[10];
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, -1, 1);
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, 4, -1);
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, 4, -6);
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, 9, 5);
        // and the integer overflow, too
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, Integer.MAX_VALUE, 4);
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // and null checks too
        _testBoundsWithByteArrayInput(genc, oper, null, 0, 3);
    }

    private void _testBoundsWithByteArrayInput(GeneratorCreator genc,
            ByteBackedOperation oper,
            byte[] data, int offset, int len) throws Exception
    {
        try (JsonGenerator gen = genc.create()) {
            try {
                oper.call(gen, data, offset, len);
                fail("Should not pass");
            } catch (StreamWriteException e) {
                if (data == null) {
                    verifyException(e, "Invalid `byte[]` argument: `null`");
                } else {
                    verifyException(e, "Invalid 'offset'");
                    verifyException(e, "'len'");
                    verifyException(e, "arguments for `byte[]` of length "+data.length);
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods, char[] backed
    /**********************************************************************
     */

    // // // Individual generator calls to check, char[]-backed

    private final CharBackedOperation WRITE_NUMBER_FROM_CHARS = new CharBackedOperation() {
        @Override
        public void call(JsonGenerator g, char[] data, int offset, int len) throws Exception {
            g.writeNumber(data, offset, len);
        }
    };

    private final CharBackedOperation WRITE_RAW_FROM_CHARS = new CharBackedOperation() {
        @Override
        public void call(JsonGenerator g, char[] data, int offset, int len) throws Exception {
            g.writeRaw(data, offset, len);
        }
    };

    private final CharBackedOperation WRITE_RAWVALUE_FROM_CHARS = new CharBackedOperation() {
        @Override
        public void call(JsonGenerator g, char[] data, int offset, int len) throws Exception {
            g.writeRawValue(data, offset, len);
        }
    };

    public void testBoundsWithCharArrayInputFromBytes() throws Exception {
        _testBoundsWithCharArrayInput(BYTE_GENERATOR_CREATOR);
    }

    public void testBoundsWithCharArrayInputFromChars() throws Exception {
        _testBoundsWithCharArrayInput(CHAR_GENERATOR_CREATOR);
    }

    private void _testBoundsWithCharArrayInput(GeneratorCreator genc) throws Exception {
        _testBoundsWithCharArrayInput(genc, WRITE_NUMBER_FROM_CHARS);
        _testBoundsWithCharArrayInput(genc, WRITE_RAW_FROM_CHARS);
        _testBoundsWithCharArrayInput(genc, WRITE_RAWVALUE_FROM_CHARS);
    }

    private void _testBoundsWithCharArrayInput(GeneratorCreator genc,
            CharBackedOperation oper) throws Exception {
        final char[] CHARS10 = new char[10];
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, -1, 1);
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, 4, -1);
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, 4, -6);
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, 9, 5);
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, Integer.MAX_VALUE, 4);
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, Integer.MAX_VALUE, Integer.MAX_VALUE);
        _testBoundsWithCharArrayInput(genc, oper, null, 0, 3);
    }

    private void _testBoundsWithCharArrayInput(GeneratorCreator genc,
            CharBackedOperation oper,
            char[] data, int offset, int len) throws Exception
    {
        try (JsonGenerator gen = genc.create()) {
            try {
                oper.call(gen, data, offset, len);
                fail("Should not pass");
            } catch (StreamWriteException e) {
                if (data == null) {
                    verifyException(e, "Invalid `char[]` argument: `null`");
                } else {
                    verifyException(e, "Invalid 'offset'");
                    verifyException(e, "'len'");
                    verifyException(e, "arguments for `char[]` of length "+data.length);
                }
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods, String backed
    /**********************************************************************
     */

    // // // Individual generator calls to check, String-backed.
    // // //
    // // // Only two methods (and one seems to delegate to the other)

    private final StringBackedOperation WRITE_RAW_FROM_STRING = new StringBackedOperation() {
        @Override
        public void call(JsonGenerator g, String data, int offset, int len) throws Exception {
            g.writeRaw(data, offset, len);
        }
    };

    private final StringBackedOperation WRITE_RAWVALUE_FROM_STRING = new StringBackedOperation() {
        @Override
        public void call(JsonGenerator g, String data, int offset, int len) throws Exception {
            g.writeRawValue(data, offset, len);
        }
    };

    public void testBoundsWithStringInputFromBytes() throws Exception {
        _testBoundsWithStringInput(BYTE_GENERATOR_CREATOR);
    }

    public void testBoundsWithStringInputFromChar() throws Exception {
        _testBoundsWithStringInput(CHAR_GENERATOR_CREATOR);
    }

    private void _testBoundsWithStringInput(GeneratorCreator genc) throws Exception {
        _testBoundsWithStringInput(genc, WRITE_RAW_FROM_STRING);
        _testBoundsWithStringInput(genc, WRITE_RAWVALUE_FROM_STRING);
    }

    private void _testBoundsWithStringInput(GeneratorCreator genc,
            StringBackedOperation oper) throws Exception {
        final String STRING10 = new String(new char[10]);
        _testBoundsWithStringInput(genc, oper, STRING10, -1, 1);
        _testBoundsWithStringInput(genc, oper, STRING10, 4, -1);
        _testBoundsWithStringInput(genc, oper, STRING10, 4, -6);
        _testBoundsWithStringInput(genc, oper, STRING10, 9, 5);
        _testBoundsWithStringInput(genc, oper, STRING10, Integer.MAX_VALUE, 4);
        _testBoundsWithStringInput(genc, oper, STRING10, Integer.MAX_VALUE, Integer.MAX_VALUE);
        _testBoundsWithStringInput(genc, oper, null, 0, 3);
    }

    private void _testBoundsWithStringInput(GeneratorCreator genc,
            StringBackedOperation oper,
            String data, int offset, int len) throws Exception
    {
        try (JsonGenerator gen = genc.create()) {
            try {
                oper.call(gen, data, offset, len);
                fail("Should not pass");
            } catch (StreamWriteException e) {
                if (data == null) {
                    verifyException(e, "Invalid `String` argument: `null`");
                } else {
                    verifyException(e, "Invalid 'offset'");
                    verifyException(e, "'len'");
                    verifyException(e, "arguments for `String` of length "+data.length());
                }
            }
        }
    }

}
