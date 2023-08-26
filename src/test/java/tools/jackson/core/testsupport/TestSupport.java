package tools.jackson.core.testsupport;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;

/**
 * Container for various factories needed by (unit) tests.
 */
public class TestSupport
{
    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext() {
        return testIOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext(StreamReadConstraints src) {
        return testIOContext(src,
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults());
    }

    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext(StreamWriteConstraints swc) {
        return testIOContext(StreamReadConstraints.defaults(),
                swc,
                ErrorReportConfiguration.defaults());
    }

    private static IOContext testIOContext(StreamReadConstraints src,
            StreamWriteConstraints swc,
            ErrorReportConfiguration erc) {
        return new IOContext(src, swc, erc,
                new BufferRecycler(), ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }
}
