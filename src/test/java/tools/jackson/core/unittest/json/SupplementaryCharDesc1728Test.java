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

    // U+1D400 MATHEMATICAL BOLD CAPITAL A: a supplementary Java identifier char.
    // Low 16 bits are U+D400, a Hangul syllable (also identifier char).
    private static final int MATH_BOLD_A = 0x1D400;

    private static final byte[] MATH_BOLD_A_UTF8 = new byte[] {
        (byte) 0xF0, (byte) 0x9D, (byte) 0x90, (byte) 0x80
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
        final byte[] doc = _concat(new byte[] { '{' }, SEE_NO_EVIL_UTF8);

        _assertFullCodePoint(() -> JSON_FACTORY.createParser(ObjectReadContext.empty(), doc), 2);
        for (int mode : ALL_BINARY_MODES) {
            _assertFullCodePoint(() -> createParser(mode, doc), 2);
        }
    }

    // Unexpected supplementary char right after a matched keyword
    @Test
    void unexpectedSupplementaryAfterTokenQuotesFullCodePoint() throws Exception
    {
        final byte[] doc = _concat(_concat("true".getBytes("UTF-8"), SEE_NO_EVIL_UTF8),
                new byte[] { ' ' }); // trailing space for DataInput

        _assertFullCodePoint(() -> JSON_FACTORY.createParser(ObjectReadContext.empty(), doc), 2);
        for (int mode : ALL_BINARY_MODES) {
            _assertFullCodePoint(() -> createParser(mode, doc), 2);
        }
    }

    // Supplementary identifier char included in "Unrecognized token" text:
    // at token start, directly after keyword, and later in token
    @Test
    void invalidTokenIncludesFullSupplementaryCodePoint() throws Exception
    {
        final String letter = new String(Character.toChars(MATH_BOLD_A));
        for (String prefix : new String[] { "", "true", "truex" }) {
            final byte[] doc = _concat(_concat(prefix.getBytes("UTF-8"), MATH_BOLD_A_UTF8),
                    new byte[] { ' ' }); // trailing space for DataInput
            final String expected = "Unrecognized token '" + prefix + letter + "'";

            _assertInvalidToken(() -> JSON_FACTORY.createParser(ObjectReadContext.empty(), doc),
                    expected);
            for (int mode : ALL_BINARY_MODES) {
                _assertInvalidToken(() -> createParser(mode, doc), expected);
            }
        }
    }

    // Lead bytes that would decode above U+10FFFF must be reported as invalid UTF-8
    @Test
    void codePointAboveMaxReportedAsInvalidUtf8() throws Exception
    {
        _assertError(new byte[] { (byte) 0xF5, (byte) 0x80, (byte) 0x80, (byte) 0x80, ' ' },
                "Invalid UTF-8 4-byte sequence (0xF5 0x80 ...): code point exceeds U+10FFFF");
        _assertError(new byte[] { (byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80, ' ' },
                "Invalid UTF-8 4-byte sequence (0xF4 0x90 ...): code point exceeds U+10FFFF");
        // but U+10FFFF itself is valid
        _assertError(new byte[] { (byte) 0xF4, (byte) 0x8F, (byte) 0xBF, (byte) 0xBF, ' ' },
                "Unexpected character ('" + new String(Character.toChars(0x10FFFF))
                + "' (code 1114111 / 0x10ffff)");
    }

    private void _assertError(byte[] doc, String expected)
    {
        assertThatThrownBy(() -> _readTokens(
                () -> JSON_FACTORY.createParser(ObjectReadContext.empty(), doc), 1))
                .isInstanceOf(StreamReadException.class)
                .hasMessageContaining(expected);
        for (int mode : ALL_BINARY_MODES) {
            assertThatThrownBy(() -> _readTokens(() -> createParser(mode, doc), 1))
                    .isInstanceOf(StreamReadException.class)
                    .hasMessageContaining(expected);
        }
    }

    private static void _assertInvalidToken(Supplier<JsonParser> parserSupplier, String expected)
    {
        assertThatThrownBy(() -> _readTokens(parserSupplier, 2))
                .isInstanceOf(StreamReadException.class)
                .hasMessageContaining(expected)
                .hasMessageNotContaining(String.valueOf((char) MATH_BOLD_A));
    }

    private static void _readTokens(Supplier<JsonParser> parserSupplier, int tokens)
    {
        try (JsonParser p = parserSupplier.get()) {
            for (int i = 0; i < tokens; ++i) {
                p.nextToken();
            }
        }
    }

    private static byte[] _concat(byte[] a, byte[] b)
    {
        final byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static void _assertFullCodePoint(Supplier<JsonParser> parserSupplier, int tokens)
    {
        final String quoted = "'" + new String(Character.toChars(SEE_NO_EVIL)) + "'";
        final String truncated = "'" + (char) SEE_NO_EVIL + "'";

        assertThatThrownBy(() -> _readTokens(parserSupplier, tokens))
                .isInstanceOf(StreamReadException.class)
                .hasMessageContaining("Unexpected character (" + quoted)
                .hasMessageContaining("code " + SEE_NO_EVIL)
                .hasMessageContaining("0x" + Integer.toHexString(SEE_NO_EVIL))
                .hasMessageNotContaining(truncated);
    }
}
