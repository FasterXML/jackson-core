package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.NumberInput;

/**
 * Implementation of
 * <a href="http://tools.ietf.org/html/draft-ietf-appsawg-json-pointer-03">JSON Pointer</a>
 * specification.
 * Pointer instances can be used to locate logical JSON nodes for things like
 * tree traversal (see {@link TreeNode#at}).
 * It may be used in future for filtering of streaming JSON content
 * as well (not implemented yet for 2.3).
 *<p>
 * Instances are fully immutable and can be shared, cached.
 * 
 * @author Tatu Saloranta
 * 
 * @since 2.3
 */
public class JsonPointer
{
    /**
     * Marker instance used to represent segment that matches current
     * node or position.
     */
    protected final static JsonPointer EMPTY = new JsonPointer();
    
    /**
     * Reference to rest of the pointer beyond currently matching
     * segment (if any); null if this pointer refers to a matching
     * segment.
     */
    protected final JsonPointer _nextSegment;
    
    /**
     * We will retain representation of the pointer, as a String,
     * so that {@link #toString} should be as efficient as possible.
     */
    protected final String _asString;
    
    protected final String _matchingPropertyName;

    protected final int _matchingElementIndex;

    /*
    /**********************************************************
    /* Cosntruction
    /**********************************************************
     */
    
    /**
     * Constructor used for creating "empty" instance, used to represent
     * state that matches current node.
     */
    protected JsonPointer() {
        _nextSegment = null;
        _matchingPropertyName = "";
        _matchingElementIndex = -1;
        _asString = "";
    }

    /**
     * Constructor used for creating non-empty Segments
     */
    protected JsonPointer(String fullString, String segment, JsonPointer next) {
        _asString = fullString;
        _nextSegment = next;
        // Ok; may always be a property
        _matchingPropertyName = segment;
        _matchingElementIndex = _parseIndex(segment);
    }
    
    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */
    
    /**
     * Factory method that parses given input and construct matching pointer
     * instance, if it represents a valid JSON Pointer: if not, a
     * {@link IllegalArgumentException} is thrown.
     * 
     * @throws IllegalArgumentException Thrown if the input does not present a valid JSON Pointer
     *   expression: currently the only such expression is one that does NOT start with
     *   a slash ('/').
     */
    public static JsonPointer compile(String input) throws IllegalArgumentException
    {
        // First quick checks for well-known 'empty' pointer
        if ((input == null) || input.length() == 0) {
            return EMPTY;
        }
        // And then quick validity check:
        if (input.charAt(0) != '/') {
            throw new IllegalArgumentException("Invalid input: JSON Pointer expression must start with '/': "+"\""+input+"\"");
        }
        return _parseTail(input);
    }

    /**
     * Alias for {@link #compile}; added to make instances automatically
     * deserializable by Jackson databind.
     */
    public static JsonPointer valueOf(String input) { return compile(input); }

    /* Factory method that composes a pointer instance, given a set
     * of 'raw' segments: raw meaning that no processing will be done,
     * no escaping may is present.
     * 
     * @param segments
     * 
     * @return Constructed path instance
     */
    /* TODO!
    public static JsonPointer fromSegment(String... segments)
    {
        if (segments.length == 0) {
            return EMPTY;
        }
        JsonPointer prev = null;
                
        for (String segment : segments) {
            JsonPointer next = new JsonPointer()
        }
    }
    */
    
    /*
    /**********************************************************
    /* Public API
    /**********************************************************
     */

    public boolean matches() { return _nextSegment == null; }
    public String getMatchingProperty() { return _matchingPropertyName; }
    public int getMatchingIndex() { return _matchingElementIndex; }
    public boolean mayMatchProperty() { return _matchingPropertyName != null; }
    public boolean mayMatchElement() { return _matchingElementIndex >= 0; }

    public JsonPointer matchProperty(String name) {
        if (_nextSegment == null || !_matchingPropertyName.equals(name)) {
            return null;
        }
        return _nextSegment;
    }

    public JsonPointer matchElement (int index) {
        if ((index != _matchingElementIndex) || (index < 0)) {
            return null;
        }
        return _nextSegment;
    }

    /**
     * Accessor for getting a "sub-pointer", instance where current segment
     * has been removed and pointer includes rest of segments;
     */
    public JsonPointer tail() {
        return _nextSegment;
    }
    
    /*
    /**********************************************************
    /* Standard method overrides
    /**********************************************************
     */

    @Override public String toString() { return _asString; }
    @Override public int hashCode() { return _asString.hashCode(); }

    @Override public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof JsonPointer)) return false;
        return _asString.equals(((JsonPointer) o)._asString);
    }
    
    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    private final static int _parseIndex(String str) {
        final int len = str.length();
        // [Issue#133]: beware of super long indexes; assume we never
        // have arrays over 2 billion entries so ints are fine.
        if (len == 0 || len > 10) {
            return -1;
        }
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i++);
            if (c > '9' || c < '0') {
                return -1;
            }
        }
        if (len == 10) {
            long l = NumberInput.parseLong(str);
            if (l > Integer.MAX_VALUE) {
                return -1;
            }
        }
        return NumberInput.parseInt(str);
    }
    
    protected static JsonPointer _parseTail(String input) {
        final int end = input.length();

        // first char is the contextual slash, skip
        for (int i = 1; i < end; ) {
            char c = input.charAt(i);
            if (c == '/') { // common case, got a segment
                return new JsonPointer(input, input.substring(1, i),
                        _parseTail(input.substring(i)));
            }
            ++i;
            // quoting is different; offline this case
            if (c == '~' && i < end) { // possibly, quote
                return _parseQuotedTail(input, i);
            }
            // otherwise, loop on
        }
        // end of the road, no escapes
        return new JsonPointer(input, input.substring(1), EMPTY);
    }

    /**
     * Method called to parse tail of pointer path, when a potentially
     * escaped character has been seen.
     * 
     * @param input Full input for the tail being parsed
     * @param i Offset to character after tilde
     */
    protected static JsonPointer _parseQuotedTail(String input, int i) {
        final int end = input.length();
        StringBuilder sb = new StringBuilder(Math.max(16, end));
        if (i > 2) {
            sb.append(input, 1, i-1);
        }
        _appendEscape(sb, input.charAt(i++));
        while (i < end) {
            char c = input.charAt(i);
            if (c == '/') { // end is nigh!
                return new JsonPointer(input, sb.toString(),
                        _parseTail(input.substring(i))); // need to push back slash
            }
            ++i;
            if (c == '~' && i < end) {
                _appendEscape(sb, input.charAt(i++));
                continue;
            }
            sb.append(c);
        }
        // end of the road, last segment
        return new JsonPointer(input, sb.toString(), EMPTY);
    }
    
    private static void _appendEscape(StringBuilder sb, char c) {
        if (c == '0') {
            c = '~';
        } else if (c == '1') {
            c = '/';
        } else {
            sb.append('~');
        }
        sb.append(c);
    }
}
