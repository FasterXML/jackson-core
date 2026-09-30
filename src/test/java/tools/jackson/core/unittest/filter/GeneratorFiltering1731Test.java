package tools.jackson.core.unittest.filter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.filter.FilteringGeneratorDelegate;
import tools.jackson.core.filter.TokenFilter;
import tools.jackson.core.filter.TokenFilter.Inclusion;

import static org.junit.jupiter.api.Assertions.assertEquals;

// for [core#1731]
class GeneratorFiltering1731Test
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    /**
     * Drops the string {@code drop}. Raw values stay included, which is how
     * UTF-8 string writes currently bypass {@code includeString}.
     */
    static class DropStringFilter extends TokenFilter {
        @Override
        public boolean includeString(String value) {
            return !"drop".equals(value);
        }
    }

    /**
     * Strings are included; raw values are not.
     */
    static class StringsNotRawFilter extends TokenFilter {
        @Override
        public boolean includeRawValue() {
            return false;
        }
    }

    /**
     * Includes only an exact string. Raw values are excluded so a raw-value
     * check cannot accidentally accept the UTF-8 write.
     */
    static class ExactStringFilter extends TokenFilter {
        private final String accepted;

        ExactStringFilter(String accepted) {
            this.accepted = accepted;
        }

        @Override
        public boolean includeString(String value) {
            return accepted.equals(value);
        }

        @Override
        public boolean includeRawValue() {
            return false;
        }
    }

    @Test
    void utf8StringWritesUseStringFilter() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator g = filtered(out, new DropStringFilter());
        byte[] drop = utf8("drop");

        g.writeStartObject();
        g.writeName("plain");
        g.writeString("keep");
        g.writeName("utf8drop");
        g.writeUTF8String(drop, 0, drop.length);
        g.writeName("rawdrop");
        g.writeRawUTF8String(drop, 0, drop.length);
        g.writeName("utf8keep");
        byte[] padded = utf8("xxkeep");
        g.writeUTF8String(padded, 2, 4);
        g.writeEndObject();
        g.close();

        assertEquals("{\"plain\":\"keep\",\"utf8keep\":\"keep\"}", utf8(out));
    }

    @Test
    void utf8StringWritesAreNotRawValues() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator g = filtered(out, new StringsNotRawFilter());
        byte[] yes = utf8("yes");
        byte[] yes2 = utf8("yes2");

        g.writeStartObject();
        g.writeName("raw");
        g.writeRaw("1");
        g.writeName("utf8");
        g.writeUTF8String(yes, 0, yes.length);
        g.writeName("rawutf8");
        g.writeRawUTF8String(yes2, 0, yes2.length);
        g.writeEndObject();
        g.close();

        assertEquals("{\"utf8\":\"yes\",\"rawutf8\":\"yes2\"}", utf8(out));
    }

    @Test
    void utf8StringFilterDecodesTheWrittenSlice() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator g = filtered(out, new ExactStringFilter("héllo"));
        byte[] hello = utf8("héllo");
        // Surrounding bytes would not match; only the slice is the value.
        byte[] padded = new byte[hello.length + 4];
        padded[0] = 'x';
        padded[1] = 'x';
        System.arraycopy(hello, 0, padded, 2, hello.length);
        padded[padded.length - 2] = 'y';
        padded[padded.length - 1] = 'y';

        g.writeStartArray();
        g.writeUTF8String(padded, 2, hello.length);
        // Pre-escaped text decodes to a\"b, not héllo, so it stays excluded.
        byte[] escaped = utf8("a\\\"b");
        g.writeRawUTF8String(escaped, 0, escaped.length);
        g.writeEndArray();
        g.close();

        assertEquals("[\"héllo\"]", utf8(out));
    }

    @Test
    void utf8StringWritesRespectPropertyFilter() throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JsonGenerator g = filtered(out, new TokenFilter() {
            @Override
            public TokenFilter includeProperty(String name) {
                return "keep".equals(name) ? TokenFilter.INCLUDE_ALL : null;
            }
        });
        byte[] no = utf8("no");
        byte[] yes = utf8("yes");

        g.writeStartObject();
        g.writeName("drop");
        g.writeUTF8String(no, 0, no.length);
        g.writeName("keep");
        g.writeRawUTF8String(yes, 0, yes.length);
        g.writeEndObject();
        g.close();

        assertEquals("{\"keep\":\"yes\"}", utf8(out));
    }

    private static JsonGenerator filtered(ByteArrayOutputStream out, TokenFilter filter) throws Exception
    {
        return new FilteringGeneratorDelegate(createGenerator(out),
                filter, Inclusion.INCLUDE_ALL_AND_PATH, true);
    }

    private static byte[] utf8(String text) {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    private static String utf8(ByteArrayOutputStream out) {
        return out.toString(StandardCharsets.UTF_8);
    }
}
