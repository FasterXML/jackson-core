package com.fasterxml.jackson.core.jsonptr;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonPointer;

// For https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=51806
// (reported as [core#818]
public class Fuzz51806JsonPointerParse818Test extends BaseTest
{
    // Before fix, StackOverflowError with 6_000 or so,
    // and OOME with 20_000.
    // After fixes will get progressively slower so limit size to
    // keep test runs from making suite too slow
    private final static int TOO_DEEP_PATH = 25_000;

    // Verify that a very deep/long (by number of segments) JsonPointer
    // may still be parsed ok, for "simple" case (no quoted chars)
    public void testJsonPointerParseTailSimple()
    {
        _testJsonPointer(_generatePath(TOO_DEEP_PATH, false));
    }

    public void testJsonPointerParseTailWithQuoted()
    {
        _testJsonPointer(_generatePath(TOO_DEEP_PATH, true));
    }

    private void _testJsonPointer(String pathExpr)
    {
        JsonPointer p = JsonPointer.compile(pathExpr);
        assertNotNull(p);
        // But also verify it didn't change
        assertEquals(pathExpr, p.toString());

        // And then verify segment by segment, easiest way is to
        // check that tail segment is proper substring
        JsonPointer curr = p;

        while ((curr = curr.tail()) != null) {
            String act = curr.toString();
            String exp = pathExpr.substring(pathExpr.length() - act.length());
            assertEquals(exp, act);
        }
    }

    private String _generatePath(int depth, boolean escaped) {
        StringBuilder sb = new StringBuilder(4 * depth);
        for (int i = 0; i < depth; ++i) {
            sb.append('/')
                .append((char) ('a' + i%25))
                .append(i);

            if (escaped) {
                switch (i & 7) {
                case 1:
                    sb.append("~0x");
                    break;
                case 4:
                    sb.append("~1y");
                    break;
                }
            }
        }
        return sb.toString();
    }
}
