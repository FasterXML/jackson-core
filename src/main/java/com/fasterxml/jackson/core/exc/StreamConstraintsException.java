package com.fasterxml.jackson.core.exc;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Exception type used to indicate violations of stream constraints
 * (for example {@link com.fasterxml.jackson.core.StreamReadConstraints})
 * when reading or writing content.
 *
 * @since 2.15
 */
public class StreamConstraintsException
    extends JsonProcessingException
{
    private final static long serialVersionUID = 2L;

    public StreamConstraintsException(String msg) {
        super(msg);
    }

    public StreamConstraintsException(String msg, JsonLocation loc) {
        super(msg, loc);
    }
}
