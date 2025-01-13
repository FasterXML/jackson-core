package tools.jackson.core.unittest.sym;

import tools.jackson.core.sym.ByteQuadsCanonicalizer;

public class TestByteQuadsCanonicalizer extends ByteQuadsCanonicalizer {
    public final static int MAX_ENTRIES_FOR_REUSE = ByteQuadsCanonicalizer.MAX_ENTRIES_FOR_REUSE;

    protected TestByteQuadsCanonicalizer(int sz, int seed) {
        super(sz, seed);
    }

    public static TestByteQuadsCanonicalizer createRoot(int seed) {
        return new TestByteQuadsCanonicalizer(ByteQuadsCanonicalizer.DEFAULT_T_SIZE, seed);
    }
}
