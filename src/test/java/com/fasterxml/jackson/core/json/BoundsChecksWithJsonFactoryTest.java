package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;

public class BoundsChecksWithJsonFactoryTest
    extends BaseTest
{
    final static JsonFactory JSON_F = new JsonFactory();

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
        // To implement!!!
    }

    /*
    /**********************************************************************
    /* Test methods, char[] backed
    /**********************************************************************
     */

    public void testBoundsWithCharArrayInput() throws Exception {
        // To implement!!!
    }
}
