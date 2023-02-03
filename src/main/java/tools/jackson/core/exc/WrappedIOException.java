package tools.jackson.core.exc;

import java.io.IOException;

import tools.jackson.core.JacksonException;

/**
 * Exception type used to wrap low-level I/O issues that are reported
 * on reading and writing content using JDK streams and other sources
 * and targets.
 *<p>
 * NOTE: use of {@link java.io.UncheckedIOException} would seem like
 * an alternative, but cannot be used as it is a checked exception
 * unlike {@link JacksonException} used for other read/write problems.
 * Because of this, an alternative is used.
 *
 * @since 3.0
 */
public class WrappedIOException extends JacksonException
{
    private final static long serialVersionUID = 1L;

    /**
     * Optional processor, often of parser, generator type
     * (or equivalent read/write context from databinding).
     */
    protected transient Object _processor;

    protected WrappedIOException(Object processor, IOException source) {
        super(source.getMessage(), source);
        _processor = processor;
    }

    public static WrappedIOException construct(IOException e) {
        return construct(e, null);
    }

    public static WrappedIOException construct(IOException e, Object processor) {
        return new WrappedIOException(processor, e);
    }

    public WrappedIOException withProcessor(Object processor) {
        _processor = processor;
        return this;
    }

    @Override
    public Object processor() { return _processor; }

    @Override // just for co-variant type
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
