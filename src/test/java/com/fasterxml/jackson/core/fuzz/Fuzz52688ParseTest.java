package com.fasterxml.jackson.core.fuzz;

import java.io.*;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testsupport.ThrottledInputStream;

// Reproducing: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=52688
// (reported as [core#834]
public class Fuzz52688ParseTest extends BaseTest
{
    private final JsonFactory JSON_F = new JsonFactory();

    private final static BigInteger BIG_NUMBER = new BigInteger("3222"
            +"2222"
            +"2222"
            +"2222"
            +"222");

    public void testBigNumberUTF16Parse() throws Exception
    {
        // 41 bytes as UTF16-LE; becomes 21 characters (last broken)
        final byte[] DOC = {
                0x33, 0, 0x32, 0, 0x32, 0, 0x32, 0,
                0x32, 0, 0x32, 0, 0x32, 0, 0x32, 0,
                0x32, 0, 0x32, 0, 0x32, 0, 0x32, 0,
                0x32, 0, 0x32, 0, 0x32, 0, 0x32, 0,
                0x32, 0, 0x32, 0, 0x32, 0, 0xd, 0,
                0x32
        };

        try (JsonParser p = JSON_F.createParser(/*ObjectReadContext.empty(), */
                new ByteArrayInputStream(DOC))) {
            assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(BIG_NUMBER, p.getBigIntegerValue());
            assertEquals(1, p.currentLocation().getLineNr());

            // and now we should fail for the weird character
            try {
                JsonToken t = p.nextToken();
                fail("Should not pass, next token = "+t);
            } catch (StreamReadException e) {
                verifyException(e, "Unexpected character");
                assertEquals(2, p.currentLocation().getLineNr());
                assertEquals(2, e.getLocation().getLineNr());
            }
        }
    }

    public void testBigNumberUTF8Parse() throws Exception
    {
        // Similar to UTF-16 case
        final byte[] DOC = {
                0x33, 0x32, 0x32, 0x32,
                0x32, 0x32, 0x32, 0x32,
                0x32, 0x32, 0x32, 0x32,
                0x32, 0x32, 0x32, 0x32,
                0x32, 0x32, 0x32, 0xd,
                (byte) '@'
        };

        // Try to force buffer condition
        try (ThrottledInputStream in = new ThrottledInputStream(DOC, 1)) {
            try (JsonParser p = JSON_F.createParser(/*ObjectReadContext.empty(), */ in)) {
                assertEquals(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(BIG_NUMBER, p.getBigIntegerValue());
                assertEquals(1, p.currentLocation().getLineNr());

                // and now we should fail for the weird character
                try {
                    JsonToken t = p.nextToken();
                    fail("Should not pass, next token = "+t);
                } catch (StreamReadException e) {
                    verifyException(e, "Unexpected character");
                    assertEquals(2, p.currentLocation().getLineNr());
                    assertEquals(2, e.getLocation().getLineNr());
                }
            }
        }
    }
}
