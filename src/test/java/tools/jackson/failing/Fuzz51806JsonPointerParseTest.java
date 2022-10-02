package tools.jackson.failing;

import tools.jackson.core.BaseTest;
import tools.jackson.core.JsonPointer;

// For https://bugs.chromium.org/p/oss-fuzz/issues/detail?id=51806
// (via databind, but we don't need that)
public class Fuzz51806JsonPointerParseTest extends BaseTest
{
    // Before fix, looks like this is enough to cause StackOverflowError
    private final static int TOO_DEEP_PATH = 6000;

    // Verify that very deep/long (by number of segments) JsonPointer
    // may still be parsed ok
    public void testJsonPointerParseTail() throws Exception
    {
        JsonPointer p = JsonPointer.compile(_generatePath(TOO_DEEP_PATH));
        assertNotNull(p);
    }

    private String _generatePath(int depth) {
        StringBuilder sb = new StringBuilder(4 * depth);
        for (int i = 0; i < depth; ++i) {
            sb.append("/a").append(depth);
        }
        return sb.toString();
    }
}
