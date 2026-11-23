package tools.jackson.core.unittest.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.unittest.async.AsyncTestBase;
import tools.jackson.core.unittest.testutil.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a document rejected for hash collisions in a symbol table
 * does not leave the shared root symbol table in a corrupt state for later parsers.
 * [core#1835]
 */
class SymbolTableSpilloverMergeTest
    extends JacksonCoreTestBase
{
    private final static String[] PREFIXES = { "x0__________", "x1__________" };

    // `CharsToNameCanonicalizer.MAX_COLL_CHAIN_LENGTH` (150) + 50: enough names to
    // overflow collision chain once, but not twice
    private final static int ONE_OVERFLOW_NAME_COUNT = 200;

    static class SymbolCountingJsonFactory extends JsonFactory
    {
        private static final long serialVersionUID = 1L;

        int charSymbolCount() { return _rootCharSymbols.size(); }
    }

    // ByteQuadsCanonicalizer: byte-backed blocking parsers
    @Test
    void failedByteSymbolTableNotMerged() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            final JsonFactory f = new JsonFactory();
            for (String prefix : PREFIXES) {
                byte[] doc = _doc(_quadCollidingNames(prefix));
                assertThrows(StreamConstraintsException.class, () -> _parseAll(f, mode, doc));
            }
            _parseAll(f, mode, _validDoc());
        }
    }

    // ByteQuadsCanonicalizer: non-blocking parser
    @Test
    void failedByteSymbolTableNotMergedAsync() throws Exception
    {
        for (int bytesPerRead : new int[] { 1, 1000 }) {
            final JsonFactory f = new JsonFactory();
            for (String prefix : PREFIXES) {
                byte[] doc = _doc(_quadCollidingNames(prefix));
                assertThrows(StreamConstraintsException.class,
                        () -> _parseAllAsync(f, bytesPerRead, doc));
            }
            _parseAllAsync(f, bytesPerRead, _validDoc());
        }
    }

    // CharsToNameCanonicalizer: char-backed parsers
    @Test
    void failedCharSymbolTableNotMerged() throws Exception
    {
        for (int mode : ALL_TEXT_MODES) {
            final JsonFactory f = new JsonFactory();
            for (String prefix : PREFIXES) {
                byte[] doc = _doc(_charCollidingNames(prefix, 512));
                assertThrows(StreamConstraintsException.class, () -> _parseAll(f, mode, doc));
            }
            _parseAll(f, mode, _validDoc());
        }
    }

    // CharsToNameCanonicalizer: nor should table with overflowed (but not failed)
    // collision chain be merged
    @Test
    void overflowedCharSymbolTableNotMerged() throws Exception
    {
        for (int mode : ALL_TEXT_MODES) {
            final SymbolCountingJsonFactory f = new SymbolCountingJsonFactory();
            _parseAll(f, mode, _doc(_charCollidingNames(PREFIXES[0], ONE_OVERFLOW_NAME_COUNT)));
            assertEquals(0, f.charSymbolCount());

            // but other tables still are
            _parseAll(f, mode, _doc(Arrays.asList("a", "b", "c")));
            assertEquals(3, f.charSymbolCount());
        }
    }

    private void _parseAll(JsonFactory f, int mode, byte[] doc)
    {
        try (JsonParser p = createParser(f, mode, doc)) {
            while (p.nextToken() != null) { }
        }
    }

    private void _parseAllAsync(JsonFactory f, int bytesPerRead, byte[] doc)
    {
        AsyncReaderWrapper r = AsyncTestBase.asyncForBytes(f, bytesPerRead, doc, 0);
        try {
            while (r.nextToken() != null) { }
        } finally {
            r.close();
        }
    }

    // Names that only differ by order of quads 4-9 have same hash, regardless of seed
    private List<String> _quadCollidingNames(String prefix)
    {
        List<String> result = new ArrayList<>();
        _permute(new ArrayList<>(Arrays.asList("aaaa", "bbbb", "cccc", "dddd", "eeee", "ffff")),
                0, prefix, result);
        return result.subList(0, 700);
    }

    private void _permute(List<String> quads, int index, String prefix, List<String> result)
    {
        if (index == quads.size()) {
            result.add(prefix + String.join("", quads));
            return;
        }
        for (int i = index; i < quads.size(); ++i) {
            Collections.swap(quads, index, i);
            _permute(quads, index + 1, prefix, result);
            Collections.swap(quads, index, i);
        }
    }

    // "Ab" and "BA" hash the same (65*33+98 == 66*33+65), so all equal-length
    // sequences of them collide, regardless of seed
    private List<String> _charCollidingNames(String prefix, int count)
    {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            StringBuilder sb = new StringBuilder(prefix);
            for (int bit = 0; bit < 9; ++bit) {
                sb.append(((i >> bit) & 1) == 0 ? "Ab" : "BA");
            }
            result.add(sb.toString());
        }
        return result;
    }

    private byte[] _validDoc()
    {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < 3000; ++i) {
            names.add("field" + i);
        }
        return _doc(names);
    }

    private byte[] _doc(List<String> names)
    {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < names.size(); ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"').append(names.get(i)).append("\":0");
        }
        return utf8Bytes(sb.append('}').toString());
    }
}
