package tools.jackson.core.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * {@link OutputStream} wrapper that forwards written content, but never flushes or
 * closes the underlying stream; writes after {@link #close()} fail.
 *<p>
 * Used by textual format backends that wrap the caller's {@link OutputStream} in a
 * {@link java.io.Writer} of their own, which buffers content and only hands it over
 * when flushed or closed. That {@code Writer} therefore has to be flushed by
 * {@link tools.jackson.core.JsonGenerator#flush()} and closed by
 * {@link tools.jackson.core.JsonGenerator#close()} regardless of
 * {@link tools.jackson.core.StreamWriteFeature#FLUSH_PASSED_TO_STREAM} and
 * {@link tools.jackson.core.StreamWriteFeature#AUTO_CLOSE_TARGET} -- but doing so must
 * not flush or close the caller's stream. Whether that is to be done is decided by
 * the generator, when flushed or closed (since those features may be changed on the
 * generator after construction): generator needs to keep a separate reference to
 * the caller's stream for this purpose.
 *
 * @since 3.3
 */
public final class GuardedOutputStream extends OutputStream
{
    private final OutputStream _out;

    private boolean _closed;

    public GuardedOutputStream(OutputStream out) {
        _out = Objects.requireNonNull(out, "Cannot pass `null` OutputStream");
    }

    @Override
    public void write(int b) throws IOException {
        _verifyOpen();
        _out.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        _verifyOpen();
        _out.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        _verifyOpen();
        _out.write(b, off, len);
    }

    @Override
    public void flush() {
        // Deliberately does NOT flush the stream we wrap
    }

    @Override
    public void close() {
        // Deliberately does NOT close (nor flush) the stream we wrap; but
        // must not pass any further writes to it either
        _closed = true;
    }

    private void _verifyOpen() throws IOException {
        if (_closed) {
            throw new IOException("Stream closed");
        }
    }
}
