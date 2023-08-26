package tools.jackson.core.testsupport;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;

/**
 * Container for various factories needed by (unit) tests.
 *
 * @since 2.16
 */
public class TestSupport
{
    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext() {
        return new IOContext(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(), ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }

    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext(StreamWriteConstraints swc) {
        return new IOContext(StreamReadConstraints.defaults(), swc,
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(), ContentReference.unknown(), false,
                JsonEncoding.UTF8);
    }
}
