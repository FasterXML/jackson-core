package tools.jackson.core.unittest.io;

import org.junit.jupiter.api.Test;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.util.BufferRecycler;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestIOContext
    extends tools.jackson.core.unittest.JacksonCoreTestBase
{
    @Test
    void allocations() throws Exception
    {
        IOContext ctxt = new IOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference("N/A"), true,
                JsonEncoding.UTF8);

        /* I/O Read buffer */

        // First succeeds:
        assertNotNull(ctxt.allocReadIOBuffer());
        // second fails
        try {
            ctxt.allocReadIOBuffer();
        } catch (IllegalStateException e) {
            verifyException(e, "second time");
        }
        // Also: can't succeed with different buffer
        try {
            ctxt.releaseReadIOBuffer(new byte[1]);
        } catch (IllegalArgumentException e) {
            verifyException(e, "smaller than original");
        }
        // but call with null is a NOP for convenience
        ctxt.releaseReadIOBuffer(null);

        /* I/O Write buffer */

        assertNotNull(ctxt.allocWriteEncodingBuffer());
        try {
            ctxt.allocWriteEncodingBuffer();
        } catch (IllegalStateException e) {
            verifyException(e, "second time");
        }
        try {
            ctxt.releaseWriteEncodingBuffer(new byte[1]);
        } catch (IllegalArgumentException e) {
            verifyException(e, "smaller than original");
        }
        ctxt.releaseWriteEncodingBuffer(null);

        /* Token (read) buffer */

        assertNotNull(ctxt.allocTokenBuffer());
        try {
            ctxt.allocTokenBuffer();
        } catch (IllegalStateException e) {
            verifyException(e, "second time");
        }
        try {
            ctxt.releaseTokenBuffer(new char[1]);
        } catch (IllegalArgumentException e) {
            verifyException(e, "smaller than original");
        }
        ctxt.releaseTokenBuffer(null);

        /* Concat (write?) buffer */

        assertNotNull(ctxt.allocConcatBuffer());
        try {
            ctxt.allocConcatBuffer();
        } catch (IllegalStateException e) {
            verifyException(e, "second time");
        }
        try {
            ctxt.releaseConcatBuffer(new char[1]);
        } catch (IllegalArgumentException e) {
            verifyException(e, "smaller than original");
        }
        ctxt.releaseConcatBuffer(null);

        /* NameCopy (write?) buffer */

        assertNotNull(ctxt.allocNameCopyBuffer(100));
        try {
            ctxt.allocNameCopyBuffer(100);
        } catch (IllegalStateException e) {
            verifyException(e, "second time");
        }
        try {
            ctxt.releaseNameCopyBuffer(new char[1]);
        } catch (IllegalArgumentException e) {
            verifyException(e, "smaller than original");
        }
        ctxt.releaseNameCopyBuffer(null);
        ctxt.close();
    }
}

