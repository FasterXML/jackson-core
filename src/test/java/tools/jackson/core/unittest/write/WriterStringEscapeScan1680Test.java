package tools.jackson.core.unittest.write;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.io.CharTypes;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.unittest.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [core#1680]: {@code WriterBasedJsonGenerator} tests constants instead of the
 * escape table when the default table is in use. Verify that every 7-bit char
 * is escaped the same as by the (table-driven) UTF-8 generator, for default and
 * non-default tables, across short-String, long-String and {@code char[]} paths.
 */
class WriterStringEscapeScan1680Test extends JacksonCoreTestBase
{
    // Longer than the Writer output buffer, to go through _writeSegment()
    private final static int LONG_LEN = 20_000;

    @Test
    void defaultTableMatchesConstantCheck()
    {
        // Must agree with WriterBasedJsonGenerator._needsEscape() for default table
        final int[] table = CharTypes.get7BitOutputEscapes();
        assertThat(table).hasSize(128);
        for (int c = 0; c < table.length; ++c) {
            boolean expEscape = (c < 0x20) || (c == '"') || (c == '\\');
            assertThat(table[c] != 0)
                .as("char 0x%02X", c)
                .isEqualTo(expEscape);
        }
    }

    @Test
    void defaultEscapes()
    {
        _verifyAllAscii(JsonFactory.builder().build());
    }

    @Test
    void apostropheQuoting()
    {
        _verifyAllAscii(JsonFactory.builder().quoteChar('\'').build());
    }

    @Test
    void escapeForwardSlashes()
    {
        _verifyAllAscii(JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES).build());
    }

    @Test
    void apostropheQuotingWithEscapedSlashes()
    {
        _verifyAllAscii(JsonFactory.builder().quoteChar('\'')
                .enable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES).build());
    }

    private void _verifyAllAscii(JsonFactory f)
    {
        StringBuilder sb = new StringBuilder(LONG_LEN + 10);
        for (int c = 0; c < 128; ++c) {
            String shortStr = "a" + (char) c + "b";
            _verifySame(f, shortStr, c);

            // Long: escapable char at start, middle and end
            sb.setLength(0);
            sb.append((char) c);
            while (sb.length() < LONG_LEN / 2) {
                sb.append('x');
            }
            sb.append((char) c);
            while (sb.length() < LONG_LEN) {
                sb.append('y');
            }
            sb.append((char) c);
            _verifySame(f, sb.toString(), c);
        }
    }

    private void _verifySame(JsonFactory f, String value, int c)
    {
        final String exp = _writeUTF8(f, value);
        assertThat(_writeWriter(f, value, false))
            .as("String, char 0x%02X, length %d", c, value.length())
            .isEqualTo(exp);
        assertThat(_writeWriter(f, value, true))
            .as("char[], char 0x%02X, length %d", c, value.length())
            .isEqualTo(exp);
    }

    private String _writeWriter(JsonFactory f, String value, boolean asChars)
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw)) {
            g.writeStartArray();
            if (asChars) {
                char[] ch = value.toCharArray();
                g.writeString(ch, 0, ch.length);
            } else {
                g.writeString(value);
            }
            g.writeEndArray();
        }
        return sw.toString();
    }

    private String _writeUTF8(JsonFactory f, String value)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes)) {
            g.writeStartArray();
            g.writeString(value);
            g.writeEndArray();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
