package com.fasterxml.jackson.core.filter;

import java.io.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [core#609]
class GeneratorFiltering609Test
        extends com.fasterxml.jackson.core.JUnit5TestBase
{
    static class NullExcludingTokenFilter extends TokenFilter {
        static final NullExcludingTokenFilter INSTANCE =
                new NullExcludingTokenFilter();

        @Override
        public boolean includeNull() {
            return false;
        }

    }

    static class StringTruncatingGeneratorDelegate
        extends JsonGeneratorDelegate
    {
        private final int maxStringLength;

        StringTruncatingGeneratorDelegate(
                JsonGenerator jsonGenerator,
                int maxStringLength) {
            super(jsonGenerator);
            this.maxStringLength = maxStringLength;
        }

        @Override
        public void writeString(String text) throws IOException {
            if (text == null) {
                writeNull();
            } else if (maxStringLength <= 0 || maxStringLength >= text.length()) {
                super.writeString(text);
            } else {
                StringReader textReader = new StringReader(text);
                super.writeString(textReader, maxStringLength);
            }
        }

        @Override
        public void writeFieldName(String name) throws IOException {
            if (maxStringLength <= 0 || maxStringLength >= name.length()) {
                super.writeFieldName(name);
            } else {
                String truncatedName = name.substring(0, maxStringLength);
                super.writeFieldName(truncatedName);
            }
        }

    }

    // for [core#609]: will pass in 2.10 for some cases
    @Test
    void issue609() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator g = createGenerator(outputStream);
        g = new FilteringGeneratorDelegate(
                g, NullExcludingTokenFilter.INSTANCE, Inclusion.INCLUDE_ALL_AND_PATH, true);
        int maxStringLength = 10;
        g = new StringTruncatingGeneratorDelegate(
                g, maxStringLength);
        g.writeStartObject();
        g.writeFieldName("message");
        g.writeString("1234567890!");
        g.writeEndObject();
        g.close();
        outputStream.close();

        String json = outputStream.toString("US-ASCII");
        assertEquals("{\"message\":\"1234567890\"}", json);
    }
}
