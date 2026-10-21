package com.fasterxml.jackson.core.constraints;

import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.async.ByteBufferFeeder;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;

import static org.junit.jupiter.api.Assertions.*;

class StringWriterReadConstraintsTest extends JUnit5TestBase
{
    @Test
    void stringsWithinLimit() throws Exception {
        JsonFactory f = factory(100);
        for (int mode = 0; mode < ALL_MODES.length + 2; ++mode) {
            for (String value : new String[] { repeat('x', 99), repeat('x', 100) }) {
                try (JsonParser p = parser(f, mode, q(value))) {
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    StringWriter w = new StringWriter();
                    assertEquals(value.length(), p.getText(w));
                    assertEquals(value, w.toString());
                    assertEquals(value, p.getText());
                }
            }
        }
    }

    @Test
    void oversizedStringsRejectedBeforeWriting() throws Exception {
        JsonFactory f = factory(100);
        for (int mode = 0; mode < ALL_MODES.length + 2; ++mode) {
            for (String doc : new String[] {
                    q(repeat('x', 150)), "\"\\u0078" + repeat('x', 149) + "\"" }) {
                try (JsonParser p = parser(f, mode, doc)) {
                    assertToken(JsonToken.VALUE_STRING, p.nextToken());
                    StringWriter w = new StringWriter();
                    assertThrows(StreamConstraintsException.class, () -> p.getText(w));
                    assertEquals("", w.toString());
                }
            }
        }
    }

    @Test
    void numbersAndNamesKeepTheirOwnLimits() throws Exception {
        JsonFactory f = factory(19);
        String name = repeat('a', 20);
        // Escape forces Reader name decoding through TextBuffer.getTextBuffer().
        String doc = "{\"\\u0061" + name.substring(1) + "\":-1234567890123456789}";
        for (int mode = 0; mode < ALL_MODES.length + 2; ++mode) {
            try (JsonParser p = parser(f, mode, doc)) {
                assertToken(JsonToken.START_OBJECT, p.nextToken());
                assertToken(JsonToken.FIELD_NAME, p.nextToken());
                StringWriter w = new StringWriter();
                assertEquals(20, p.getText(w));
                assertEquals(name, w.toString());
                assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                assertEquals(-1234567890123456789L, p.getLongValue());
                w.getBuffer().setLength(0);
                assertEquals(20, p.getText(w));
                assertEquals("-1234567890123456789", w.toString());
                assertToken(JsonToken.END_OBJECT, p.nextToken());
            }
        }
    }

    private String repeat(char ch, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, ch);
        return new String(chars);
    }

    private JsonFactory factory(int limit) {
        return JsonFactory.builder()
                .recyclerPool(JsonRecyclerPools.nonRecyclingPool())
                .streamReadConstraints(StreamReadConstraints.builder()
                        .maxStringLength(limit).maxNumberLength(limit).build())
                .build();
    }

    private JsonParser parser(JsonFactory f, int mode, String doc) throws Exception {
        if (mode < ALL_MODES.length) {
            return createParser(f, mode, doc);
        }
        byte[] input = utf8Bytes(doc);
        JsonParser p;
        if (mode == ALL_MODES.length) {
            p = f.createNonBlockingByteArrayParser();
            ((ByteArrayFeeder) p.getNonBlockingInputFeeder()).feedInput(input, 0, input.length);
        } else {
            p = f.createNonBlockingByteBufferParser();
            ((ByteBufferFeeder) p.getNonBlockingInputFeeder()).feedInput(ByteBuffer.wrap(input));
        }
        p.getNonBlockingInputFeeder().endOfInput();
        return p;
    }
}
