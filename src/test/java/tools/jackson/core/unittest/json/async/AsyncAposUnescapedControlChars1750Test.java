package tools.jackson.core.unittest.json.async;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Async variant of [jackson-core#1750] tests: {@code ALLOW_UNESCAPED_CONTROL_CHARS}
 * with single-quoted String values (and names).
 */
class AsyncAposUnescapedControlChars1750Test extends AsyncTestBase
{
    private final JsonFactory APOS_F = JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .build();

    private final JsonFactory APOS_CTRL_F = APOS_F.rebuild()
            .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
            .build();

    private final static int[] READ_SIZES = new int[] { 1, 2, 3, 5, 99, 1000 };

    private final static String[] VALUES = new String[] {
            "a\tb", "\t", "a\u0001b", "x\ny\rz", "é\t中", "\t\uD83D\uDE00\t",
            _longValue()
    };

    @Test
    void controlCharsInAposValueAllowed() throws Exception
    {
        for (int readSize : READ_SIZES) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                for (String value : VALUES) {
                    try (AsyncReaderWrapper p = _parser(APOS_CTRL_F, byteBuffer, readSize,
                            "['" + value + "']")) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        assertEquals(value, p.currentText(), "readSize " + readSize);
                        assertToken(JsonToken.END_ARRAY, p.nextToken());
                    }
                    try (AsyncReaderWrapper p = _parser(APOS_CTRL_F, byteBuffer, readSize,
                            "{'a':'" + value + "'}")) {
                        assertToken(JsonToken.START_OBJECT, p.nextToken());
                        assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        assertEquals(value, p.currentText(), "readSize " + readSize);
                        assertToken(JsonToken.END_OBJECT, p.nextToken());
                    }
                }
            }
        }
    }

    @Test
    void controlCharsInAposNameAllowed() throws Exception
    {
        for (int readSize : READ_SIZES) {
            for (String name : new String[] { "a\tb", "\t", "x\ny" }) {
                try (AsyncReaderWrapper p = _parser(APOS_CTRL_F, false, readSize,
                        "{'" + name + "':1}")) {
                    assertToken(JsonToken.START_OBJECT, p.nextToken());
                    assertToken(JsonToken.PROPERTY_NAME, p.nextToken());
                    assertEquals(name, p.currentName(), "readSize " + readSize);
                    assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                    assertToken(JsonToken.END_OBJECT, p.nextToken());
                }
            }
        }
    }

    @Test
    void controlCharsInAposDisallowed() throws Exception
    {
        for (int readSize : READ_SIZES) {
            for (String doc : new String[] { "['a\tb']", "{'a':'a\tb'}", "['" + _longValue() + "']" }) {
                try (AsyncReaderWrapper p = _parser(APOS_F, false, readSize, doc)) {
                    p.nextToken();
                    p.nextToken();
                    p.nextToken();
                    fail("Should not pass, readSize " + readSize);
                } catch (StreamReadException e) {
                    verifyException(e, "Illegal unquoted character");
                    verifyException(e, "string value");
                }
            }
            try (AsyncReaderWrapper p = _parser(APOS_F, false, readSize, "{'a\tb':1}")) {
                p.nextToken();
                p.nextToken();
                fail("Should not pass, readSize " + readSize);
            } catch (StreamReadException e) {
                verifyException(e, "Illegal unquoted character");
                verifyException(e, "name");
            }
        }
    }

    // Linefeeds in String values must update row number
    @Test
    void linefeedsUpdateRow() throws Exception
    {
        for (int readSize : READ_SIZES) {
            for (String lf : new String[] { "\n", "\r", "\r\n" }) {
                for (char q : new char[] { '\'', '"' }) {
                    final String doc = "[" + q + "x" + lf + "y" + q + ", 1]";
                    try (AsyncReaderWrapper p = _parser(APOS_CTRL_F, false, readSize, doc)) {
                        assertToken(JsonToken.START_ARRAY, p.nextToken());
                        assertToken(JsonToken.VALUE_STRING, p.nextToken());
                        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
                        assertEquals(2, p.parser().currentTokenLocation().getLineNr(),
                                "readSize " + readSize + ", doc "
                                + doc.replace("\r", "\\r").replace("\n", "\\n"));
                    }
                }
            }
        }
    }

    private AsyncReaderWrapper _parser(JsonFactory f, boolean byteBuffer, int readSize,
            String doc) throws Exception
    {
        byte[] data = _jsonDoc(doc);
        return byteBuffer ? asyncForByteBuffer(f, readSize, data, 0)
                : asyncForBytes(f, readSize, data, 0);
    }

    // Long enough to span input chunk and text buffer segment boundaries,
    // with control chars at varying offsets
    private static String _longValue() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6000; ++i) {
            switch (i % 7) {
            case 0:
                sb.append('\t');
                break;
            case 3:
                sb.append('é');
                break;
            default:
                sb.append((char) ('a' + (i % 26)));
            }
        }
        return sb.toString();
    }
}
