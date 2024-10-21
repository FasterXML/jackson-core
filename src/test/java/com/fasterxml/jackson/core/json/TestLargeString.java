package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;
import net.bytebuddy.utility.RandomString;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestLargeString extends JUnit5TestBase {

    @Test
    void testLargeStringDeserialization() throws Exception
    {
        String largeString = RandomString.make(Integer.MAX_VALUE - 1024);
        JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(Integer.MAX_VALUE)
                        .build())
                .build();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        JsonGenerator g = f.createGenerator(bytes);
        g.writeString(largeString);
        g.close();

        JsonParser parser = f.createParser(bytes.toByteArray());
        String parsedString = parser.nextTextValue();

        assertEquals(largeString, parsedString);
    }
}
