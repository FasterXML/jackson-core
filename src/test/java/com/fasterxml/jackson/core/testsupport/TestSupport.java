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
        return new IOContext(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(), ContentReference.unknown(), false);
    }

    /**
     * Factory method for creating {@link IOContext}s for tests
     */
    public static IOContext testIOContext(StreamWriteConstraints swc) {
        return new IOContext(StreamReadConstraints.defaults(), swc,
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(), ContentReference.unknown(), false);
    }
}
