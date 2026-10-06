package tools.jackson.core.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;

/**
 * {@link OutputStream} wrapper that forwards written content, but never flushes or
 * closes the underlying stream.
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
public final class GuardedOutputStream extends FilterOutputStream
{
    public GuardedOutputStream(OutputStream out) {
        super(Objects.requireNonNull(out, "Cannot pass `null` OutputStream"));
    }

    // NOTE: must override; `FilterOutputStream` otherwise writes one byte at a time
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
    }

    @Override
    public void flush() {
        // Deliberately does NOT flush the stream we wrap
    }

    @Override
    public void close() {
        // Deliberately does NOT close (nor flush) the stream we wrap
    }
}
