package com.fasterxml.jackson.core.util;

import java.nio.charset.Charset;

/**
 * Container object used to contain optional information on content
 * being parsed, passed to {@link com.fasterxml.jackson.core.JsonParseException} in case of
 * exception being thrown; this may be useful for caller to display
 * information on failure.
 *
 * @since 2.8
 */
public class RequestPayload
    implements java.io.Serializable // just in case, even though likely included as transient
{
    private static final long serialVersionUID = 1L;

    // request payload as byte[]
    protected byte[] _payloadAsBytes;

    // request payload as String
    protected CharSequence _payloadAsText;

    // Charset if the request payload is set in bytes
    protected Charset _charset;

    public RequestPayload(byte[] bytes, Charset charset) {
        if (bytes == null) {
            throw new IllegalArgumentException();
        }
        _payloadAsBytes = bytes;
        _charset = charset;
    }

    public RequestPayload(CharSequence str) {
        if (str == null) {
            throw new IllegalArgumentException();
        }
        _payloadAsText = str;
    }

    /**
     * Returns the raw request payload object i.e, either byte[] or String
     * 
     * @return Object which is a raw request payload i.e, either byte[] or
     *         String
     */
    public Object getRawPayload() {
        if (_payloadAsBytes != null) {
            return _payloadAsBytes;
        }

        return _payloadAsText;
    }

    @Override
    public String toString() {
        if (_payloadAsBytes != null) {
            return new String(_payloadAsBytes, _charset);
        }
        return _payloadAsText.toString();
    }
}
