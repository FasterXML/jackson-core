package tools.jackson.core.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.StreamReadConstraints;

public final class ReadConstrainedTextBuffer extends TextBuffer {

    private final StreamReadConstraints _streamReadConstraints;

    public ReadConstrainedTextBuffer(StreamReadConstraints streamReadConstraints, BufferRecycler bufferRecycler) {
        super(bufferRecycler);
        _streamReadConstraints = streamReadConstraints;
    }

    /*
    /**********************************************************************
    /* Convenience methods for validation
    /**********************************************************************
     */

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateStringLength(int length) throws JacksonException
    {
        _streamReadConstraints.validateStringLength(length);
    }
}
