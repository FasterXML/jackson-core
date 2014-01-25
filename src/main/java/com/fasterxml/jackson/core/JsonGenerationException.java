/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

/**
 * Exception type for exceptions during JSON writing, such as trying
 * to output  content in wrong context (non-matching end-array or end-object,
 * for example).
 */
public class JsonGenerationException
    extends JsonProcessingException
{
    private final static long serialVersionUID = 123; // Stupid eclipse...
    
    public JsonGenerationException(Throwable rootCause)
    {
        super(rootCause);
    }

    public JsonGenerationException(String msg)
    {
        super(msg, (JsonLocation)null);
    }

    public JsonGenerationException(String msg, Throwable rootCause)
    {
        super(msg, null, rootCause);
    }
}
