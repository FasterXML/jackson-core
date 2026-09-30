package tools.jackson.core.unittest.json;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// [core#1728]: unexpected-character messages must quote the supplementary
// code point, not the 16-bit truncation of it.
class SupplementaryCharDesc1728Test extends JacksonCoreTestBase
{
    // U+1F648 SEE-NO-EVIL MONKEY. Low 16 bits are U+F648, a different character.
    private static final int SEE_NO_EVIL = 0x1F648;

    private static final byte[] SEE_NO_EVIL_UTF8 = new byte[] {
        (byte) 0xF0, (byte) 0x9F, (byte) 0x99, (byte) 0x88
    };

    @Test
    void unexpectedSupplementaryValueQuotesFullCodePoint() throws Exception
    {
        _assertFullCodePoint(() -> JSON_FACTORY.createParser(ObjectReadContext.empty(),
                SEE_NO_EVIL_UTF8), 1);
        for (int mode : ALL_BINARY_MODES) {
            _assertFullCodePoint(() -> createParser(mode, SEE_NO_EVIL_UTF8), 1);
        }
    }

    @Test
    void unexpectedSupplementaryPropertyNameQuotesFullCodePoint() throws Exception
    {
        final byte[] doc = new byte[SEE_NO_EVIL_UTF8.length + 1];
        doc[0] = '{';
        System.arraycopy(SEE_NO_EVIL_UTF8, 0, doc, 1, SEE_NO_EVIL_UTF8.length);

        _assertFullCodePoint(() -> JSON_FACTORY.createParser(ObjectReadContext.empty(), doc), 2);
        for (int mode : ALL_BINARY_MODES) {
            _assertFullCodePoint(() -> createParser(mode, doc), 2);
        }
    }

    private static void _assertFullCodePoint(Supplier<JsonParser> parserSupplier, int tokens)
    {
        final String quoted = "'" + new String(Character.toChars(SEE_NO_EVIL)) + "'";
        final String truncated = "'" + (char) SEE_NO_EVIL + "'";

        assertThatThrownBy(() -> {
            try (JsonParser p = parserSupplier.get()) {
                for (int i = 0; i < tokens; ++i) {
                    p.nextToken();
                }
            }
        })
                .isInstanceOf(StreamReadException.class)
                .hasMessageContaining("Unexpected character (" + quoted)
                .hasMessageContaining("code " + SEE_NO_EVIL)
                .hasMessageContaining("0x" + Integer.toHexString(SEE_NO_EVIL))
                .hasMessageNotContaining(truncated);
    }
}
