package com.fasterxml.jackson.core.json.async;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Async variant of [jackson-core#1787] tests: non-ASCII chars must not be
 * accepted as hex digits of a {@code \}{@code u} escape, and should be
 * reported as decoded chars, not as UTF-8 lead bytes.
 */
class AsyncHexEscapeNonAscii1787Test extends AsyncTestBase
{
    private final JsonFactory F = newStreamFactory();

    private final static int[] READ_SIZES = new int[] { 1, 2, 3, 5, 99, 1000 };

    @Test
    void nonAsciiHexDigitsRejected() throws Exception
    {
        _testBroken("\\u\u2042\u2042\u2042\u2042", "'\u2042' (code 8258 / 0x2042)");
        _testBroken("\\u\u0141BCD", "'\u0141' (code 321 / 0x141)");
        _testBroken("\\u00\u01300", "'\u0130' (code 304 / 0x130)");
        _testBroken("\\u\uD83D\uDE00", "'\uD83D\uDE00' (code 128512 / 0x1f600)");
    }

    private void _testBroken(String escape, String expCharDesc) throws Exception
    {
        for (int readSize : READ_SIZES) {
            for (boolean byteBuffer : new boolean[] { false, true }) {
                for (String doc : new String[] { "[\"" + escape + "\"]", "{\"" + escape + "\":1}" }) {
                    byte[] data = _jsonDoc(doc);
                    try (AsyncReaderWrapper p = byteBuffer ? asyncForByteBuffer(F, readSize, data, 0)
                            : asyncForBytes(F, readSize, data, 0)) {
                        while (p.nextToken() != null) {
                            p.currentText();
                        }
                        fail("Should not pass for readSize " + readSize + ", doc: " + doc);
                    } catch (StreamReadException e) {
                        verifyException(e, "expected a hex-digit for character escape sequence");
                        // best-effort decoding needs whole char buffered
                        if (readSize >= 99) {
                            verifyException(e, "Unexpected character (" + expCharDesc + ")");
                        }
                    }
                }
            }
        }
    }
}
