package com.fasterxml.jackson.core.fuzz;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.WrappedIOException;
import com.fasterxml.jackson.core.json.JsonFactory;

// Trying to repro: https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=32216
// but so far without success (fails on seemingly legit validation problem)
public class Fuzz32208UTF32ParseTest extends BaseTest
{
    public void testFuzz32208() throws Exception
    {
        final JsonFactory f = newStreamFactory();
        final byte[] doc = readResource("/data/fuzz-json-utf32-32208.json");

        JsonParser p = f.createParser(ObjectReadContext.empty(), doc);
        try {
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            String text = p.getText();
            fail("Should not have passed; got text with length of: "+text.length());
        } catch (WrappedIOException e) {
//e.printStackTrace();
            verifyException(e, "Invalid UTF-32 character ");
        }
        p.close();
    }
}
