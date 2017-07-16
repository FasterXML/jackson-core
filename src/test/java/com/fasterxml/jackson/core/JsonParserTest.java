package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.ReaderBasedJsonParser;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.util.BufferRecycler;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for class {@link JsonParser}.
 *
 * @date 16.07.2017
 * @see JsonParser
 **/
public class JsonParserTest {


    @Test
    public void testReadValueAsTreeThrowsIllegalStateException() throws IOException {

        BufferRecycler bufferRecycler = new BufferRecycler();
        IOContext iOContext = new IOContext(bufferRecycler, bufferRecycler, false);

        JsonParser jsonParser = new ReaderBasedJsonParser(iOContext,
                -1681,
                new StringReader(""),
                null,
                CharsToNameCanonicalizer.createRoot(),
                new char[1],
                11,
                3,
                false);

        try {
            jsonParser.readValueAsTree();
            fail("Expecting exception: IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals(JsonParser.class.getName(), e.getStackTrace()[0].getClassName());
        }

    }


}