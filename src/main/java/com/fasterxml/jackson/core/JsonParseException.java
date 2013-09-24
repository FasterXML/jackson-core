/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

/**
 * Exception type for parsing problems, used when non-well-formed content
 * (content that does not conform to JSON syntax as per specification)
 * is encountered.
 */
public class JsonParseException
    extends JsonProcessingException
{
    private static final long serialVersionUID = 1L;
    
    public JsonParseException(String msg, JsonLocation loc)
    {
        super(msg, loc);
    }

    public JsonParseException(String msg, JsonLocation loc, Throwable root)
    {
        super(msg, loc, root);
    }
}
