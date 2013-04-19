/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Interface that defines how Jackson package can interact with efficient
 * pre-serialized or lazily-serialized and reused String representations.
 * Typically implementations store possible serialized version(s) so that
 * serialization of String can be done more efficiently, especially when
 * used multiple times.
 * 
 * @see com.fasterxml.jackson.core.io.SerializedString
 */
public interface SerializableString
{
    /**
     * Returns unquoted String that this object represents (and offers
     * serialized forms for)
     */
    String getValue();
    
    /**
     * Returns length of the (unquoted) String as characters.
     * Functionally equvalent to:
     *<pre>
     *   getValue().length();
     *</pre>
     */
    int charLength();

    
    /*
    /**********************************************************
    /* Accessors for byte sequences
    /**********************************************************
     */
    
    /**
     * Returns JSON quoted form of the String, as character array. Result
     * can be embedded as-is in textual JSON as property name or JSON String.
     */
    char[] asQuotedChars();

    /**
     * Returns UTF-8 encoded version of unquoted String.
     * Functionally equivalent to (but more efficient than):
     *<pre>
     * getValue().getBytes("UTF-8");
     *</pre>
     */
    byte[] asUnquotedUTF8();

    /**
     * Returns UTF-8 encoded version of JSON-quoted String.
     * Functionally equivalent to (but more efficient than):
     *<pre>
     * new String(asQuotedChars()).getBytes("UTF-8");
     *</pre>
     */
    byte[] asQuotedUTF8();

    /*
    /**********************************************************
    /* Helper methods for appending byte/char sequences
    /**********************************************************
     */

    /**
     * Method that will append quoted UTF-8 bytes of this String into given
     * buffer, if there is enough room; if not, returns -1.
     * Functionally equivalent to:
     *<pre>
     *  byte[] bytes = str.asQuotedUTF8();
     *  System.arraycopy(bytes, 0, buffer, offset, bytes.length);
     *  return bytes.length;
     *</pre>
     * 
     * @return Number of bytes appended, if successful, otherwise -1
     */
    int appendQuotedUTF8(byte[] buffer, int offset);

    /**
     * Method that will append quoted characters of this String into given
     * buffer. Functionally equivalent to:
     *<pre>
     *  char[] ch = str.asQuotedChars();
     *  System.arraycopy(ch, 0, buffer, offset, ch.length);
     *  return ch.length;
     *</pre>
     * 
     * @return Number of characters appended, if successful, otherwise -1
     */
    int appendQuoted(char[] buffer, int offset);
    
    /**
     * Method that will append unquoted ('raw') UTF-8 bytes of this String into given
     * buffer. Functionally equivalent to:
     *<pre>
     *  byte[] bytes = str.asUnquotedUTF8();
     *  System.arraycopy(bytes, 0, buffer, offset, bytes.length);
     *  return bytes.length;
     *</pre>
     * 
     * @return Number of bytes appended, if successful, otherwise -1
     */
    int appendUnquotedUTF8(byte[] buffer, int offset);

    
    /**
     * Method that will append unquoted characters of this String into given
     * buffer. Functionally equivalent to:
     *<pre>
     *  char[] ch = str.getValue().toCharArray();
     *  System.arraycopy(bytes, 0, buffer, offset, ch.length);
     *  return ch.length;
     *</pre>
     * 
     * @return Number of characters appended, if successful, otherwise -1
     */
    int appendUnquoted(char[] buffer, int offset);

    /*
    /**********************************************************
    /* Helper methods for writing out byte sequences
    /**********************************************************
     */

    /**
     * @return Number of bytes written
     */
    int writeQuotedUTF8(OutputStream out) throws IOException;

    /**
     * @return Number of bytes written
     */
    int writeUnquotedUTF8(OutputStream out) throws IOException;

    /**
     * @return Number of bytes put, if successful, otherwise -1
     */
    int putQuotedUTF8(ByteBuffer buffer) throws IOException;

    /**
     * @return Number of bytes put, if successful, otherwise -1
     */
    int putUnquotedUTF8(ByteBuffer out) throws IOException;
}
