package tools.jackson.core.unittest.json;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
        final String quoted = "'" + new String(Character.toChars(SEE_NO_EVIL)) + "'";
        final String truncated = "'" + (char) SEE_NO_EVIL + "'";

        try (JsonParser p = JSON_FACTORY.createParser(ObjectReadContext.empty(), SEE_NO_EVIL_UTF8)) {
            p.nextToken();
            fail("unquoted supplementary character must not be a JSON value");
        } catch (StreamReadException e) {
            _assertFullCodePoint(e.getMessage(), quoted, truncated);
        }

        for (int mode : ALL_BINARY_MODES) {
            try (JsonParser p = createParser(mode, SEE_NO_EVIL_UTF8)) {
                p.nextToken();
                fail("unquoted supplementary character must not be a JSON value, mode " + mode);
            } catch (StreamReadException e) {
                _assertFullCodePoint(e.getMessage(), quoted, truncated);
            }
        }
    }

    @Test
    void unexpectedSupplementaryPropertyNameQuotesFullCodePoint() throws Exception
    {
        final String quoted = "'" + new String(Character.toChars(SEE_NO_EVIL)) + "'";
        final String truncated = "'" + (char) SEE_NO_EVIL + "'";
        final byte[] doc = new byte[SEE_NO_EVIL_UTF8.length + 1];
        doc[0] = '{';
        System.arraycopy(SEE_NO_EVIL_UTF8, 0, doc, 1, SEE_NO_EVIL_UTF8.length);

        try (JsonParser p = JSON_FACTORY.createParser(ObjectReadContext.empty(), doc)) {
            p.nextToken();
            p.nextToken();
            fail("unquoted supplementary character must not start a property name");
        } catch (StreamReadException e) {
            _assertFullCodePoint(e.getMessage(), quoted, truncated);
        }

        for (int mode : ALL_BINARY_MODES) {
            try (JsonParser p = createParser(mode, doc)) {
                p.nextToken();
                p.nextToken();
                fail("unquoted supplementary character must not start a property name, mode " + mode);
            } catch (StreamReadException e) {
                _assertFullCodePoint(e.getMessage(), quoted, truncated);
            }
        }
    }

    private static void _assertFullCodePoint(String msg, String quoted, String truncated)
    {
        assertTrue(msg.contains("Unexpected character (" + quoted), msg);
        assertTrue(msg.contains("code " + SEE_NO_EVIL), msg);
        assertTrue(msg.contains("0x" + Integer.toHexString(SEE_NO_EVIL)), msg);
        assertFalse(msg.contains(truncated), msg);
    }
}
