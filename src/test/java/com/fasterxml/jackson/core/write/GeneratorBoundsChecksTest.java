package com.fasterxml.jackson.core.write;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamWriteException;

public class GeneratorBoundsChecksTest
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
    
    public void testBoundsWithByteArrayInput() throws Exception
    {
        if (true) return;
        _testBoundsWithByteArrayInput(BYTE_GENERATOR_CREATOR);
        _testBoundsWithByteArrayInput(CHAR_GENERATOR_CREATOR);
    }

    private void _testBoundsWithByteArrayInput(GeneratorCreator genc) throws Exception {
        _testBoundsWithByteArrayInput(genc, WRITE_BINARY_FROM_BYTES);
    }

    private void _testBoundsWithByteArrayInput(GeneratorCreator genc,
            ByteBackedOperation oper) throws Exception {
        final byte[] BYTES10 = new byte[10];
        _testBoundsWithByteArrayInput(genc, oper, BYTES10, -1, 1);
    }

    private void _testBoundsWithByteArrayInput(GeneratorCreator genc,
            ByteBackedOperation oper,
            byte[] data, int offset, int len) throws Exception
    {
        try (JsonGenerator gen = genc.create()) {
            try {
                oper.call(gen, data, offset, len);
            } catch (StreamWriteException e) {
                verifyException(e, "Invalid 'offset'");
                verifyException(e, "'len'");
                verifyException(e, "arguments for String of length "+data.length);
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods, char[] backed
    /**********************************************************************
     */

    // // // Individual generator calls to check, char[]-backed

    private final CharBackedOperation WRITE_RAW_FROM_CHARS = new CharBackedOperation() {
        @Override
        public void call(JsonGenerator g, char[] data, int offset, int len) throws Exception {
            g.writeRawValue(data, offset, len);
        }
    };
    
    public void testBoundsWithCharArrayInput() throws Exception
    {
        if (true) return;
        _testBoundsWithCharArrayInput(BYTE_GENERATOR_CREATOR);
        _testBoundsWithCharArrayInput(CHAR_GENERATOR_CREATOR);
    }

    private void _testBoundsWithCharArrayInput(GeneratorCreator genc) throws Exception {
        _testBoundsWithCharArrayInput(genc, WRITE_RAW_FROM_CHARS);
    }

    private void _testBoundsWithCharArrayInput(GeneratorCreator genc,
            CharBackedOperation oper) throws Exception {
        final char[] CHARS10 = new char[10];
        _testBoundsWithCharArrayInput(genc, oper, CHARS10, -1, 1);
    }

    private void _testBoundsWithCharArrayInput(GeneratorCreator genc,
            CharBackedOperation oper,
            char[] data, int offset, int len) throws Exception
    {
        try (JsonGenerator gen = genc.create()) {
            try {
                oper.call(gen, data, offset, len);
            } catch (StreamWriteException e) {
                verifyException(e, "Invalid 'offset'");
                verifyException(e, "'len'");
                verifyException(e, "arguments for `char[]` of length "+data.length);
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods, String backed
    /**********************************************************************
     */

    // // // Individual generator calls to check, String-backed

    private final StringBackedOperation WRITE_RAW_FROM_STRING = new StringBackedOperation() {
        @Override
        public void call(JsonGenerator g, String data, int offset, int len) throws Exception {
            g.writeRawValue(data, offset, len);
        }
    };
    
    public void testBoundsWithStringInput() throws Exception
    {
        if (true) return;
        _testBoundsWithStringInput(BYTE_GENERATOR_CREATOR);
        _testBoundsWithStringInput(CHAR_GENERATOR_CREATOR);
    }

    private void _testBoundsWithStringInput(GeneratorCreator genc) throws Exception {
        _testBoundsWithStringInput(genc, WRITE_RAW_FROM_STRING);
    }

    private void _testBoundsWithStringInput(GeneratorCreator genc,
            StringBackedOperation oper) throws Exception {
        final String STRING10 = new String(new char[10]);
        _testBoundsWithStringInput(genc, oper, STRING10, -1, 1);
    }

    private void _testBoundsWithStringInput(GeneratorCreator genc,
            StringBackedOperation oper,
            String data, int offset, int len) throws Exception
    {
        try (JsonGenerator gen = genc.create()) {
            try {
                oper.call(gen, data, offset, len);
            } catch (StreamWriteException e) {
                verifyException(e, "Invalid 'offset'");
                verifyException(e, "'len'");
                verifyException(e, "arguments for `String` of length "+data.length());
            }
        }
    }

}
