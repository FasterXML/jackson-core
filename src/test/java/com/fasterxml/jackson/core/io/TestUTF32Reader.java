package com.fasterxml.jackson.core.io;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.CharConversionException;
import java.io.IOException;

public class TestUTF32Reader extends BaseTest {

    // Make sure that invalid input is handled reasonably.
    public void testInvalidInput() throws IOException {
        byte[] data = {
                0x00,
                0x00,
                0x00,
                0x20,
                (byte) 0xFE,
                (byte) 0xFF,
                0x00,
                0x01,
                (byte) 0xFB
        };

        JsonFactory factory = new JsonFactory();
        JsonParser parser = factory.createParser(data);
        try {
            parser.nextToken();
        } catch (CharConversionException e) {
            return;
        }
        fail("Should have thrown a CharConversionException");
    }
}
