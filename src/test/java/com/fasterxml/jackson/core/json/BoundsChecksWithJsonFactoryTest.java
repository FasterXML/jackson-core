package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.TokenStreamFactory;

public class BoundsChecksWithJsonFactoryTest
    extends BaseTest
{
    interface ByteBackedCreation {
        void call(byte[] data, int offset, int len) throws Exception;
    }

    interface CharBackedCreation {
        void call(char[] data, int offset, int len) throws Exception;
    }

    /*
    /**********************************************************************
    /* Test methods, byte[] backed
    /**********************************************************************
     */

    public void testBoundsWithByteArrayInput() throws Exception {
        final TokenStreamFactory PARSER_F = newStreamFactory();

        _testBoundsWithByteArrayInput(
                (data,offset,len)->PARSER_F.createParser(data, offset, len));
    }

    private void _testBoundsWithByteArrayInput(ByteBackedCreation creator) throws Exception
    {
        final byte[] DATA = new byte[10];
        _testBoundsWithByteArrayInput(creator, DATA, -1, 1);
        _testBoundsWithByteArrayInput(creator, DATA, 4, -1);
        _testBoundsWithByteArrayInput(creator, DATA, 4, -6);
        _testBoundsWithByteArrayInput(creator, DATA, 9, 5);
        // and the integer overflow, too
        _testBoundsWithByteArrayInput(creator, DATA, Integer.MAX_VALUE, 4);
        _testBoundsWithByteArrayInput(creator, DATA, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // and null checks too
        _testBoundsWithByteArrayInput(creator, null, 0, 3);
    }

    private void _testBoundsWithByteArrayInput(ByteBackedCreation creator,
            byte[] data, int offset, int len) throws Exception
    {
        try {
            creator.call(data, offset, len);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            if (data == null) {
                verifyException(e, "Invalid `byte[]` argument: `null`");
            } else {
                verifyException(e, "Invalid 'offset'");
                verifyException(e, "'len'");
                verifyException(e, "arguments for `byte[]` of length "+data.length);
            }
        }
    }

    /*
    /**********************************************************************
    /* Test methods, char[] backed
    /**********************************************************************
     */

    public void testBoundsWithCharArrayInput() throws Exception {
        final TokenStreamFactory PARSER_F = newStreamFactory();

        testBoundsWithCharArrayInput(
                (data,offset,len)->PARSER_F.createParser(data, offset, len));
    }

    private void testBoundsWithCharArrayInput(CharBackedCreation creator) throws Exception
    {
        final char[] DATA = new char[10];
        testBoundsWithCharArrayInput(creator, DATA, -1, 1);
        testBoundsWithCharArrayInput(creator, DATA, 4, -1);
        testBoundsWithCharArrayInput(creator, DATA, 4, -6);
        testBoundsWithCharArrayInput(creator, DATA, 9, 5);
        // and the integer overflow, too
        testBoundsWithCharArrayInput(creator, DATA, Integer.MAX_VALUE, 4);
        testBoundsWithCharArrayInput(creator, DATA, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // and null checks too
        testBoundsWithCharArrayInput(creator, null, 0, 3);
    }

    private void testBoundsWithCharArrayInput(CharBackedCreation creator,
            char[] data, int offset, int len) throws Exception
    {
        try {
            creator.call(data, offset, len);
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
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
