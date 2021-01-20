package com.fasterxml.jackson.core.exc;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;

/**
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

    protected WrappedIOException(IOException source) {
        super(source.getMessage(), source);
    }

    public static WrappedIOException construct(IOException e) {
        return new WrappedIOException(e);
    }

    @Override // just for co-variant type
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
