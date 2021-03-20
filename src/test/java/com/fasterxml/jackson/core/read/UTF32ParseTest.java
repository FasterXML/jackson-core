package com.fasterxml.jackson.core.read;

import java.io.CharConversionException;

import com.fasterxml.jackson.core.*;

// Tests from [jackson-core#382]
public class UTF32ParseTest extends BaseTest
{
    private final JsonFactory FACTORY = new JsonFactory();

    public void testSimpleEOFs() throws Exception
    {
        // 2 spaces
        byte[] data = { 0x00, 0x00, 0x00, 0x20,
                0x00, 0x00, 0x00, 0x20
        };

        for (int len = 5; len <= 7; ++len) {
            JsonParser parser = FACTORY.createParser(data, 0, len);
            try {
                parser.nextToken();
                fail("Should not pass");
            } catch (CharConversionException e) {
                verifyException(e, "Unexpected EOF");
                verifyException(e, "of a 4-byte UTF-32 char");
            }
            parser.close();
        }
    }

    public void testSimpleInvalidUTF32() throws Exception
    {
        // 2 characters, space, then something beyond valid Unicode set
        byte[] data = {
                0x00,
                0x00,
                0x00,
                0x20,
                (byte) 0xFE,
                (byte) 0xFF,
                0x00,
                0x01
        };

        JsonParser parser = FACTORY.createParser(data);

        try {
            parser.nextToken();
            fail("Should not pass");
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-32 character 0xfefe0001");
        }
        parser.close();
    }

    public void testSimpleSevenNullBytes() throws Exception {
        byte[] data = new byte[7];
        JsonParser parser = FACTORY.createParser(/*ObjectReadContext.empty(), */data);
        try {
            parser.nextToken();
            fail("Should not pass");
        } catch (JsonParseException e) {
            verifyException(e, "Illegal character ((CTRL-CHAR, code 0))");
        }
        parser.close();
    }
}
