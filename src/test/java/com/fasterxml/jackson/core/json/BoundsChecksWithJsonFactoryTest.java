package com.fasterxml.jackson.core.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.TokenStreamFactory;

import static org.junit.jupiter.api.Assertions.fail;

class BoundsChecksWithJsonFactoryTest
        extends JUnit5TestBase
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

    @Test
    void boundsWithByteArrayInput() throws Exception {
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

    @Test
    void boundsWithCharArrayInput() throws Exception {
        final TokenStreamFactory PARSER_F = newStreamFactory();

        boundsWithCharArrayInput(
                (data,offset,len)->PARSER_F.createParser(data, offset, len));
    }

    @Test
    private void boundsWithCharArrayInput(CharBackedCreation creator) throws Exception
    {
        final char[] DATA = new char[10];
        boundsWithCharArrayInput(creator, DATA, -1, 1);
        boundsWithCharArrayInput(creator, DATA, 4, -1);
        boundsWithCharArrayInput(creator, DATA, 4, -6);
        boundsWithCharArrayInput(creator, DATA, 9, 5);
        // and the integer overflow, too
        boundsWithCharArrayInput(creator, DATA, Integer.MAX_VALUE, 4);
        boundsWithCharArrayInput(creator, DATA, Integer.MAX_VALUE, Integer.MAX_VALUE);
        // and null checks too
        boundsWithCharArrayInput(creator, null, 0, 3);
    }

    @Test
    private void boundsWithCharArrayInput(CharBackedCreation creator,
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
