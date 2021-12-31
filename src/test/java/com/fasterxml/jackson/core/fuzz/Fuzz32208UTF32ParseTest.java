package com.fasterxml.jackson.core.fuzz;

import java.io.ByteArrayInputStream;
import java.io.CharConversionException;
import java.io.InputStream;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.UTF32Reader;
import com.fasterxml.jackson.core.testsupport.ThrottledInputStream;

// Trying to repro: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=32216
// but so far without success (fails on seemingly legit validation problem)
public class Fuzz32208UTF32ParseTest extends BaseTest
{
    private final byte[] DOC = readResource("/data/fuzz-json-utf32-32208.json");

    public void testFuzz32208ViaParser() throws Exception
    {
        final JsonFactory f = new JsonFactory();

        JsonParser p = f.createParser(/*ObjectReadContext.empty(), */ DOC);
        try {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            String text = p.getText();
            fail("Should not have passed; got text with length of: "+text.length());
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-32 character ");
        }
        p.close();
    }

    // How about through UTF32Reader itself?
    public void testFuzz32208Direct() throws Exception
    {
        _testFuzz32208Direct(1);
        _testFuzz32208Direct(2);
        _testFuzz32208Direct(3);
        _testFuzz32208Direct(7);
        _testFuzz32208Direct(13);
        _testFuzz32208Direct(67);
        _testFuzz32208Direct(111);
        _testFuzz32208Direct(337);
        _testFuzz32208Direct(991);
    }

    public void testFuzz32208DirectSingleByte() throws Exception
    {
        UTF32Reader r = new UTF32Reader(null, new ByteArrayInputStream(DOC),
                new byte[500], 0, 0, false);

        int count = 0;
        try {
            int ch;
            while ((ch = r.read()) >= 0) {
                count += ch;
            }
            fail("Should have failed, got all "+count+" characters, last 0x"+Integer.toHexString(ch));
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-32 character ");
        }
        r.close();
    }

    private void _testFuzz32208Direct(int readSize) throws Exception
    {
        InputStream in = new ThrottledInputStream(DOC, readSize);
        // apparently input is NOT big-endian so:
        UTF32Reader r = new UTF32Reader(null, in, new byte[500], 0, 0, false);

        int count = 0;
        int ch;

        try {
            final char[] chunkBuffer = new char[19];

            while (true) {
                ch = r.read(chunkBuffer);
                if (ch == -1) {
                    break;
                }
                if (ch == 0) {
                    fail("Received 0 chars; broken reader");
                }
                count += ch;
            }
            fail("Should have failed, got all "+count+" characters, last 0x"+Integer.toHexString(ch));
        } catch (CharConversionException e) {
            verifyException(e, "Invalid UTF-32 character ");
        }
        r.close();
    }
}
