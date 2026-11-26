package com.fasterxml.jackson.core.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

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
    }

    // ByteQuadsCanonicalizer: a child whose spill-over area is exhausted must fail,
    // and must not be merged back into the shared root table.
    //
    // Exercised directly against the canonicalizer with a fixed seed: unlike the
    // CharsToNameCanonicalizer case, same-bucket byte collisions are seed-specific,
    // so they are generated here by searching rather than relying on a seed-
    // independent construction.
    @Test
    void failedByteSymbolTableNotMerged() throws Exception
    {
        final ByteQuadsCanonicalizer root = ByteQuadsCanonicalizer.createRoot(SEED);

        final ByteQuadsCanonicalizer failing =
                root.makeChild(JsonFactory.Feature.collectDefaults());
        assertThrows(StreamConstraintsException.class, () -> _fillSpillover(root, failing));
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

    // Adds names that all fall into the same primary bucket (sharing the low hash
    // bits), forcing the shared spill-over area to fill up and trip the guard.
    private void _fillSpillover(ByteQuadsCanonicalizer hasher, ByteQuadsCanonicalizer target)
            throws Exception
    {
        final int mask = 0x7FF; // low 11 bits -> same bucket up to a 2048-slot table
        int added = 0;
        for (int i = 0; added < 50_000; ++i) {
            // keep names long enough to be multi-quad (4+ quads)
            String name = "coll_" + Integer.toString(i, 36) + "____________";
            int[] q = calcQuads(name);
            if ((hasher.calcHash(q, q.length) & mask) == 0) {
                target.addName(name, q, q.length);
                ++added;
            }
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
