package com.fasterxml.jackson.core.testsupport;

import com.fasterxml.jackson.core.ErrorReportConfiguration;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

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
                new BufferRecycler(), ContentReference.unknown(), false);
    }
}
