package tools.jackson.core.filter;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.util.JsonGeneratorDelegate;

// for [core#609]
public class GeneratorFiltering609Test
    extends tools.jackson.core.BaseTest
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
        public JsonGenerator writeString(String text) {
            if (text == null) {
                writeNull();
            } else if (maxStringLength <= 0 || maxStringLength >= text.length()) {
                super.writeString(text);
            } else {
                StringReader textReader = new StringReader(text);
                super.writeString(textReader, maxStringLength);
            }
            return this;
        }

        @Override
        public JsonGenerator writeName(String name) {
            if (maxStringLength <= 0 || maxStringLength >= name.length()) {
                super.writeName(name);
            } else {
                String truncatedName = name.substring(0, maxStringLength);
                super.writeName(truncatedName);
            }
            return this;
        }

    }

    // for [core#609]: will pass in 2.10 for some cases
    @SuppressWarnings("resource")
    public void testIssue609() throws Exception
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JsonGenerator g = createGenerator(outputStream);
        g = new FilteringGeneratorDelegate(
                g, NullExcludingTokenFilter.INSTANCE,
                TokenFilter.Inclusion.INCLUDE_ALL_AND_PATH, true);
        int maxStringLength = 10;
        g = new StringTruncatingGeneratorDelegate(
                g, maxStringLength);
        g.writeStartObject();
        g.writeName("message");
        g.writeString("1234567890!");
        g.writeEndObject();
        g.close();

        String json = outputStream.toString("US-ASCII");
        assertEquals("{\"message\":\"1234567890\"}", json);
    }
}
