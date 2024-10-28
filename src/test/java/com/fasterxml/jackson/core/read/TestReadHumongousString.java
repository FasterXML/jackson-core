package com.fasterxml.jackson.core.read;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

// https://github.com/FasterXML/jackson-core/pull/1352
class TestReadHumongousString extends JUnit5TestBase
{
    // disabled because it takes too much memory to run
    @Disabled
    // Since we might get infinite loop:
    @Timeout(value = 10, unit = TimeUnit.SECONDS, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
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
            assertToken(JsonToken.VALUE_STRING, parser.nextToken());
            // Let's not construct String but just check that length is
            // expected: this avoids having to allocate 4 gig more of heap
            // for test -- should still trigger problem if fix not valid
            assertEquals(len, parser.getTextLength());
            // TODO: could use streaming accessor (`JsonParser.getText(Writer)`)
            assertNull(parser.nextToken());
        }

    }

    private byte[] makeLongByteString(int length) {
        final byte[] result = new byte[length + 2];
        Arrays.fill(result, (byte) 'a');
        result[0] = '\"';
        result[length + 1] = '\"';
        return result;
    }
}
