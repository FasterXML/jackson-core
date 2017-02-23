/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import java.nio.charset.Charset;

/**
 * Object that encapsulates Location information used for reporting
 * parsing (or potentially generation) errors, as well as current location
 * within input streams.
 */
public class JsonLocation
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * Include at most first 500 characters/bytes from contents; should be enough
     * to give context, but not cause unfortunate side effects in things like
     * logs.
     *
     * @since 2.9
     */
    public static final int MAX_CONTENT_SNIPPET = 500;
    
    /**
     * Shared immutable "N/A location" that can be returned to indicate
     * that no location information is available.
     *<p>
     * NOTE: before 2.9, Location was given as String "N/A"; with 2.9 it was
     * removed so that source should be indicated as "UNKNOWN".
     */
    public final static JsonLocation NA = new JsonLocation(null, -1L, -1L, -1, -1);

    protected final long _totalBytes;
    protected final long _totalChars;

    protected final int _lineNr;
    protected final int _columnNr;

    /**
     * Displayable description for input source: file path, URL.
     *<p>
     * NOTE: <code>transient</code> since 2.2 so that Location itself is Serializable.
     */
    final transient Object _sourceRef;

    public JsonLocation(Object srcRef, long totalChars, int lineNr, int colNr)
    {
        /* Unfortunately, none of legal encodings are straight single-byte
         * encodings. Could determine offset for UTF-16/UTF-32, but the
         * most important one is UTF-8...
         * so for now, we'll just not report any real byte count
         */
        this(srcRef, -1L, totalChars, lineNr, colNr);
    }

    public JsonLocation(Object sourceRef, long totalBytes, long totalChars,
            int lineNr, int columnNr)
    {
        _sourceRef = sourceRef;
        _totalBytes = totalBytes;
        _totalChars = totalChars;
        _lineNr = lineNr;
        _columnNr = columnNr;
    }

    /**
     * Reference to the original resource being read, if one available.
     * For example, when a parser has been constructed by passing
     * a {@link java.io.File} instance, this method would return
     * that File. Will return null if no such reference is available,
     * for example when {@link java.io.InputStream} was used to
     * construct the parser instance.
     */
    public Object getSourceRef() { return _sourceRef; }

    /**
     * @return Line number of the location (1-based)
     */
    public int getLineNr() { return _lineNr; }

    /**
     * @return Column number of the location (1-based)
     */
    public int getColumnNr() { return _columnNr; }

    /**
     * @return Character offset within underlying stream, reader or writer,
     *   if available; -1 if not.
     */
    public long getCharOffset() { return _totalChars; }

    /**
     * @return Byte offset within underlying stream, reader or writer,
     *   if available; -1 if not.
     */
    public long getByteOffset()
    {
        return _totalBytes;
    }

    /**
     * Accessor for getting a textual description of source reference
     * (Object returned by {@link #getSourceRef()}), as included in
     * description returned by {@link #toString()}.
     *<p>
     * NOTE: not added as a "getter" to prevent it from getting serialized.
     *
     * @since 2.9
     */
    public String sourceDescription() {
        return _appendSourceDesc(new StringBuilder(100)).toString();
    }

    /*
    /**********************************************************
    /* Std method overrides
    /**********************************************************
     */

    @Override
    public int hashCode()
    {
        int hash = (_sourceRef == null) ? 1 : _sourceRef.hashCode();
        hash ^= _lineNr;
        hash += _columnNr;
        hash ^= (int) _totalChars;
        hash += (int) _totalBytes;
        return hash;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;
        if (other == null) return false;
        if (!(other instanceof JsonLocation)) return false;
        JsonLocation otherLoc = (JsonLocation) other;

        if (_sourceRef == null) {
            if (otherLoc._sourceRef != null) return false;
        } else if (!_sourceRef.equals(otherLoc._sourceRef)) return false;

        return (_lineNr == otherLoc._lineNr)
            && (_columnNr == otherLoc._columnNr)
            && (_totalChars == otherLoc._totalChars)
            && (getByteOffset() == otherLoc.getByteOffset())
            ;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append("[Source: ");
        _appendSourceDesc(sb);
        sb.append("; line: ");
        sb.append(_lineNr);
        sb.append(", column: ");
        sb.append(_columnNr);
        sb.append(']');
        return sb.toString();
    }

    protected StringBuilder _appendSourceDesc(StringBuilder sb)
    {
        final Object srcRef = _sourceRef;

        if (srcRef == null) {
            sb.append("UNKNOWN");
            return sb;
        }
        // First, figure out what name to use as source type
        Class<?> srcType = (srcRef instanceof Class<?>) ?
                ((Class<?>) srcRef) : srcRef.getClass();
        String tn = srcType.getName();
        // standard JDK types without package
        if (tn.startsWith("java.")) {
            tn = srcType.getSimpleName();
        } else if (srcRef instanceof byte[]) { // then some other special cases
            tn = "byte[]";
        } else if (srcRef instanceof char[]) {
            tn = "char[]";
        }
        sb.append('(').append(tn).append(')');
        // and then, include (part of) contents for selected types:
        int len;
        String charStr = " chars";

        if (srcRef instanceof CharSequence) {
            CharSequence cs = (CharSequence) srcRef;
            len = cs.length();
            len -= _append(sb, cs.subSequence(0, Math.min(len, MAX_CONTENT_SNIPPET)).toString());
        } else if (srcRef instanceof char[]) {
            char[] ch = (char[]) srcRef;
            len = ch.length;
            len -= _append(sb, new String(ch, 0, Math.min(len, MAX_CONTENT_SNIPPET)));
        } else if (srcRef instanceof byte[]) {
            byte[] b = (byte[]) srcRef;
            int maxLen = Math.min(b.length, MAX_CONTENT_SNIPPET);
            _append(sb, new String(b, 0, maxLen, Charset.forName("UTF-8")));
            len = b.length - maxLen;
            charStr = " bytes";
        } else {
            len = 0;
        }
        if (len > 0) {
            sb.append("[truncated ").append(len).append(charStr).append(']');
        }
        return sb;
    }

    private int _append(StringBuilder sb, String content) {
        sb.append('"').append(content).append('"');
        return content.length();
    }
}
