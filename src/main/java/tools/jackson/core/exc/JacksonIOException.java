package tools.jackson.core.exc;

import java.io.Closeable;
import java.io.IOException;

import tools.jackson.core.JacksonException;

/**
 * Exception type used to wrap low-level I/O issues that are reported
 * (as {@link IOException}) on reading and writing content using JDK streams
 * and other sources and targets.
 * This exception is only used for wrapping {@link java.io.IOException}s
 * for re-throwing: for actual problem reporting there are alternate
 * {@link JacksonException} subtypes available.
 *<p>
 * NOTE: use of {@link java.io.UncheckedIOException} would seem like
 * an alternative, but cannot be used as it is a checked exception
 * unlike {@link JacksonException} used for other read/write problems.
 * Because of this, an alternative is used.
 * Additionally extending {@link JacksonException} allows bit more convenient
 * catching of everything Jackson throws or re-throws.
 *
 * @since 3.0
 */
public class JacksonIOException extends JacksonException
{
    private final static long serialVersionUID = 1L;

    protected JacksonIOException(Closeable processor, IOException source) {
        super(source.getMessage(), source);
        _processor = processor;
    }

    public static JacksonIOException construct(IOException e) {
        return construct(e, null);
    }

    public static JacksonIOException construct(IOException e, Closeable processor) {
        return new JacksonIOException(processor, e);
    }

    public JacksonIOException withProcessor(Closeable processor) {
        _processor = processor;
        return this;
    }

    @Override // just for co-variant type
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
