package com.fasterxml.jackson.core.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a document rejected for hash collisions in a symbol table
 * does not leave the shared root symbol table in a corrupt state for later parsers.
 * [core#1835]
 */
class SymbolTableSpilloverMergeTest
    extends JUnit5TestBase
{
    private final static String[] PREFIXES = { "x0__________", "x1__________" };

    private final static int SEED = 33333;

    @SuppressWarnings("serial")
    static class SymbolCountingJsonFactory extends JsonFactory
    {
        int charSymbolCount() { return _rootCharSymbols.size(); }
        int byteSymbolSeed() { return _byteSymbolCanonicalizer.hashSeed(); }

        // needed for non-blocking and DataInput parsers to be allowed for sub-class
        @Override
        public String getFormatName() { return FORMAT_NAME_JSON; }
    }

    // ByteQuadsCanonicalizer: byte-backed blocking parsers
    @Test
    void failedByteSymbolTableNotMerged() throws Exception
    {
        for (int mode : ALL_BINARY_MODES) {
            final SymbolCountingJsonFactory f = new SymbolCountingJsonFactory();
            for (String prefix : PREFIXES) {
                byte[] doc = _doc(_sameBucketNames(f.byteSymbolSeed(), prefix));
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
            final SymbolCountingJsonFactory f = new SymbolCountingJsonFactory();
            for (String prefix : PREFIXES) {
                byte[] doc = _doc(_sameBucketNames(f.byteSymbolSeed(), prefix));
                assertThrows(StreamConstraintsException.class,
                        () -> _parseAllAsync(f, bytesPerRead, doc));
            }
            _parseAllAsync(f, bytesPerRead, _validDoc());
        }
    }

    // ByteQuadsCanonicalizer: same, but directly against the symbol table
    @Test
    void failedByteSymbolTableNotMergedDirect() throws Exception
    {
        final ByteQuadsCanonicalizer root = ByteQuadsCanonicalizer.createRoot(SEED);

        final ByteQuadsCanonicalizer failing =
                root.makeChild(JsonFactory.Feature.collectDefaults());
        assertThrows(StreamConstraintsException.class, () -> {
            for (String name : _sameBucketNames(SEED, PREFIXES[0])) {
                int[] q = calcQuads(name);
                failing.addName(name, q, q.length);
            }
        });
        // Released failing table must not corrupt (or even reach) the root:
        failing.release();
        assertEquals(0, root.size());

        // ... and the root stays usable: a normal child still merges
        final ByteQuadsCanonicalizer ok =
                root.makeChild(JsonFactory.Feature.collectDefaults());
        for (String name : Arrays.asList("a", "b", "c")) {
            int[] q = calcQuads(name);
            ok.addName(name, q, q.length);
        }
        ok.release();
        assertEquals(3, root.size());
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
            // enough names to overflow collision chain once, but not twice
            _parseAll(f, mode, _doc(_charCollidingNames(PREFIXES[0],
                    CharsToNameCanonicalizer.MAX_COLL_CHAIN_LENGTH + 50)));
            assertEquals(0, f.charSymbolCount());

            // but other tables still are
            _parseAll(f, mode, _doc(Arrays.asList("a", "b", "c")));
            assertEquals(3, f.charSymbolCount());
        }
    }

    private void _parseAll(JsonFactory f, int mode, byte[] doc) throws Exception
    {
        try (JsonParser p = createParser(f, mode, doc)) {
            while (p.nextToken() != null) { }
        }
    }

    private void _parseAllAsync(JsonFactory f, int bytesPerRead, byte[] doc) throws Exception
    {
        AsyncReaderWrapper r = AsyncTestBase.asyncForBytes(f, bytesPerRead, doc, 0);
        try {
            while (r.nextToken() != null) { }
        } finally {
            r.close();
        }
    }

    // Names (of 4+ quads) that share low bits of hash for given seed, and so
    // end up in the same primary bucket, eventually filling the spill-over area
    private List<String> _sameBucketNames(int seed, String prefix)
    {
        final ByteQuadsCanonicalizer hasher = ByteQuadsCanonicalizer.createRoot(seed);
        final int mask = 0x7FF; // same bucket for tables of up to 2048 slots
        List<String> result = new ArrayList<>();
        for (int i = 0; result.size() < 700; ++i) {
            String name = prefix + Integer.toString(i, 36);
            int[] q = calcQuads(name);
            if ((hasher.calcHash(q, q.length) & mask) == 0) {
                result.add(name);
            }
        }
        return result;
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
