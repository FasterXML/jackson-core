package com.fasterxml.jackson.core.read;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// https://github.com/FasterXML/jackson-core/pull/1350
class TestLargeString extends JUnit5TestBase {

    // disabled because it takes too much memory to run
    @Disabled
    @Test
    void testLargeStringDeserialization() throws Exception {
        final int len = Integer.MAX_VALUE - 1024;
        final byte[] largeByteString = makeLongByteString(len);
        final JsonFactory f = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(Integer.MAX_VALUE)
                        .build())
                .build();

        try (JsonParser parser = f.createParser(largeByteString)) {
            final String parsedString = parser.nextTextValue();
            assertEquals(len, parsedString.length());
            for (int i = 0; i < len; i++) {
                assertEquals('a', parsedString.charAt(i));
            }
        }

    }

    private byte[] makeLongByteString(int length) {
        final byte[] result = new byte[length + 2];
        final byte b = 'a';
        final int last = length + 1;
        result[0] = '\"';
        for (int i = 1; i < last; i++) {
            result[i] = b;
        }
        result[last] = result[0];
        return result;
    }
}
