package com.fasterxml.jackson.core.sym;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies that a document rejected for filling up the spill-over area of
 * {@code ByteQuadsCanonicalizer} does not leave the shared root symbol table
 * in a corrupt state for later parsers.
 */
class SymbolTableSpilloverMergeTest
    extends JUnit5TestBase
{
    @Test
    void spilloverFailureDoesNotCorruptRootTable() throws Exception
    {
        final JsonFactory f = new JsonFactory();

        // Names that only differ by order of quads 4-9 have same hash, regardless of seed
        for (String prefix : new String[] { "x0__________", "x1__________" }) {
            byte[] doc = _doc(_collidingNames(prefix, 700));
            assertThrows(StreamConstraintsException.class, () -> _parseAll(f, doc));
        }

        List<String> names = new ArrayList<>();
        for (int i = 0; i < 3000; ++i) {
            names.add("field" + i);
        }
        _parseAll(f, _doc(names));
    }

    private void _parseAll(JsonFactory f, byte[] doc) throws Exception
    {
        try (JsonParser p = f.createParser(doc)) {
            while (p.nextToken() != null) { }
        }
    }

    private List<String> _collidingNames(String prefix, int count)
    {
        List<String> result = new ArrayList<>();
        _permute(new ArrayList<>(Arrays.asList("aaaa", "bbbb", "cccc", "dddd", "eeee", "ffff")),
                0, prefix, result);
        return result.subList(0, count);
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
