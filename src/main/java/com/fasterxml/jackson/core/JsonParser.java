/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package com.fasterxml.jackson.core;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.async.NonBlockingInputFeeder;
import com.fasterxml.jackson.core.json.JsonFactory;
import com.fasterxml.jackson.core.sym.FieldNameMatcher;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.RequestPayload;

/**
 * Base class that defines public API for reading JSON content.
 * Instances are created using factory methods of
 * a {@link JsonFactory} instance.
 *
 * @author Tatu Saloranta
 */
public abstract class JsonParser
    implements Closeable, Versioned
{
    private final static int MIN_BYTE_I = (int) Byte.MIN_VALUE;
    // Allow range up to and including 255, to support signed AND unsigned bytes
    private final static int MAX_BYTE_I = (int) 255;

    private final static int MIN_SHORT_I = (int) Short.MIN_VALUE;
    private final static int MAX_SHORT_I = (int) Short.MAX_VALUE;

    /**
     * Enumeration of possible "native" (optimal) types that can be
     * used for numbers.
     */
    public enum NumberType {
        INT, LONG, BIG_INTEGER, FLOAT, DOUBLE, BIG_DECIMAL
    }

    /*
    /**********************************************************************
    /* Minimal configuration state
    /**********************************************************************
     */

    /**
     * Optional container that holds the request payload which will be displayed on JSON parsing error.
     */
    protected transient RequestPayload _requestPayload;

    /*
    /**********************************************************************
    /* Construction, configuration, initialization
    /**********************************************************************
     */

    protected JsonParser() { }

    /**
     * Accessor for context object provided by higher-level databinding
     * functionality (or, in some cases, simple placeholder of the same)
     * that allows some level of interaction including ability to trigger
     * deserialization of Object values through generator instance.
     *<p>
     * Context object is used by parser to implement some methods,
     * like {@code readValueAs(...)}
     *
     * @since 3.0
     */
    public abstract ObjectReadContext getObjectReadContext();
    
    /**
     * Method that can be used to get access to object that is used
     * to access input being parsed; this is usually either
     * {@link InputStream} or {@link Reader}, depending on what
     * parser was constructed with.
     * Note that returned value may be null in some cases; including
     * case where parser implementation does not want to exposed raw
     * source to caller.
     * In cases where input has been decorated, object returned here
     * is the decorated version; this allows some level of interaction
     * between users of parser and decorator object.
     *<p>
     * In general use of this accessor should be considered as
     * "last effort", i.e. only used if no other mechanism is applicable.
     */
    public abstract Object getInputSource();

    /*
    /**********************************************************************
    /* Attaching additional metadata
    /**********************************************************************
     */
    
    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getParsingContext().getCurrentValue();
     *</code>
     *<p>
     * Note that "current value" is NOT populated (or used) by Streaming parser;
     * it is only used by higher-level data-binding functionality.
     * The reason it is included here is that it can be stored and accessed hierarchically,
     * and gets passed through data-binding.
     */
    public Object getCurrentValue() {
        TokenStreamContext ctxt = getParsingContext();
        return (ctxt == null) ? null : ctxt.getCurrentValue();
    }

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getParsingContext().setCurrentValue(v);
     *</code>
     */
    public void setCurrentValue(Object v) {
        TokenStreamContext ctxt = getParsingContext();
        if (ctxt != null) {
            ctxt.setCurrentValue(v);
        }
    }

    /**
     * Sets the payload to be passed if {@link JsonParseException} is thrown.
     */
    public void setRequestPayloadOnError(RequestPayload payload) {
        _requestPayload = payload;
    }
    
    /**
     * Sets the byte[] request payload and the charset
     */
     public void setRequestPayloadOnError(byte[] payload, String charset) {
         _requestPayload = (payload == null) ? null : new RequestPayload(payload, charset);
     }

     /**
     * Sets the String request payload
     */
    public void setRequestPayloadOnError(String payload) {
        _requestPayload = (payload == null) ? null : new RequestPayload(payload);
    }

    /*
    /**********************************************************************
    /* General capability introspection
    /**********************************************************************
     */

    /**
     * Specialized capability accessor used to check whether parsers for
     * this factory can create "synthetic nulls" since format does not
     * have native notation (or at least not unambiguous one).
     * This is true for XML backend.
     *
     * @since 3.0
     */
    public boolean canSynthesizeNulls() { return false; }

    /*
    /**********************************************************************
    /* Format support
    /**********************************************************************
     */

    /**
     * Method to call to make this parser use specified schema. Method must
     * be called before trying to parse any content, right after parser instance
     * has been created.
     * Note that not all parsers support schemas; and those that do usually only
     * accept specific types of schemas: ones defined for data format parser can read.
     *<p>
     * If parser does not support specified schema, {@link UnsupportedOperationException}
     * is thrown.
     * 
     * @param schema Schema to use
     * 
     * @throws UnsupportedOperationException if parser does not support schema
     */
    public void setSchema(FormatSchema schema) {
        String desc = (schema == null) ? "NULL" : String.format("'%s'", schema.getSchemaType());
        throw new UnsupportedOperationException("Parser of type "+getClass().getName()
                +" does not support schema of type "+desc);
    }

    /**
     * Method for accessing Schema that this parser uses, if any.
     * Default implementation returns null.
     */
    public FormatSchema getSchema() { return null; }
    
    /**
     * Method that can be used to verify that given schema can be used with
     * this parser (using {@link #setSchema}).
     * 
     * @param schema Schema to check
     * 
     * @return True if this parser can use given schema; false if not
     */
    public boolean canUseSchema(FormatSchema schema) { return false; }

    /*
    /**********************************************************************
    /* Optional support for non-blocking parsing
    /**********************************************************************
     */

    /**
     * Method that can be called to determine if this parser instance
     * uses non-blocking ("asynchronous") input access for decoding or not.
     * Access mode is determined by earlier calls via {@link JsonFactory};
     * it may not be changed after construction.
     *<p>
     * If non-blocking decoding is u (@code true}, it is possible to call
     * {@link #getNonBlockingInputFeeder()} to obtain object to use
     * for feeding input; otherwise (<code>false</code> returned)
     * input is read by blocking 
     */
    public boolean canParseAsync() { return false; }

    /**
     * Method that will either return a feeder instance (if parser uses
     * non-blocking, aka asynchronous access); or <code>null</code> for
     * parsers that use blocking I/O.
     */
    public NonBlockingInputFeeder getNonBlockingInputFeeder() {
        return null;
    }

    /*
    /**********************************************************************
    /* Versioned
    /**********************************************************************
     */
    
    /**
     * Accessor for getting version of the core package, given a parser instance.
     * Left for sub-classes to implement.
     */
    @Override
    public abstract Version version();
    
    /*
    /**********************************************************************
    /* Closeable implementation
    /**********************************************************************
     */

    /**
     * Closes the parser so that no further iteration or data access
     * can be made; will also close the underlying input source
     * if parser either <b>owns</b> the input source, or feature
     * {@link StreamReadFeature#AUTO_CLOSE_SOURCE} is enabled.
     * Whether parser owns the input source depends on factory
     * method that was used to construct instance (so check
     * {@link com.fasterxml.jackson.core.json.JsonFactory} for details,
     * but the general
     * idea is that if caller passes in closable resource (such
     * as {@link InputStream} or {@link Reader}) parser does NOT
     * own the source; but if it passes a reference (such as
     * {@link java.io.File} or {@link java.net.URL} and creates
     * stream or reader it does own them.
     */
    @Override
    public abstract void close() throws IOException;

    /**
     * Method that can be called to determine whether this parser
     * is closed or not. If it is closed, no new tokens can be
     * retrieved by calling {@link #nextToken} (and the underlying
     * stream may be closed). Closing may be due to an explicit
     * call to {@link #close} or because parser has encountered
     * end of input.
     */
    public abstract boolean isClosed();

    /*
    /**********************************************************************
    /* Public API, simple location, context accessors
    /**********************************************************************
     */

    /**
     * Method that can be used to access current parsing context reader
     * is in. There are 3 different types: root, array and object contexts,
     * with slightly different available information. Contexts are
     * hierarchically nested, and can be used for example for figuring
     * out part of the input document that correspond to specific
     * array or object (for highlighting purposes, or error reporting).
     * Contexts can also be used for simple xpath-like matching of
     * input, if so desired.
     */
    public abstract TokenStreamContext getParsingContext();

    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     */
    public abstract JsonLocation getTokenLocation();

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes.
     */
    public abstract JsonLocation getCurrentLocation();

    /*
    /**********************************************************************
    /* Buffer handling
    /**********************************************************************
     */

    /**
     * Method that can be called to push back any content that
     * has been read but not consumed by the parser. This is usually
     * done after reading all content of interest using parser.
     * Content is released by writing it to given stream if possible;
     * if underlying input is byte-based it can released, if not (char-based)
     * it can not.
     * 
     * @return -1 if the underlying content source is not byte based
     *    (that is, input can not be sent to {@link OutputStream};
     *    otherwise number of bytes released (0 if there was nothing to release)
     *    
     * @throws IOException if write to stream threw exception
     */    
    public int releaseBuffered(OutputStream out) throws IOException {
        return -1;
    }

    /**
     * Method that can be called to push back any content that
     * has been read but not consumed by the parser.
     * This is usually
     * done after reading all content of interest using parser.
     * Content is released by writing it to given writer if possible;
     * if underlying input is char-based it can released, if not (byte-based)
     * it can not.
     * 
     * @return -1 if the underlying content source is not char-based
     *    (that is, input can not be sent to {@link Writer};
     *    otherwise number of chars released (0 if there was nothing to release)
     *    
     * @throws IOException if write using Writer threw exception
     */    
    public int releaseBuffered(Writer w) throws IOException { return -1; }

    /*
    /**********************************************************************
    /* Public API, configuration
    /**********************************************************************
     */

    /**
     * Method for enabling specified parser feature
     * (check {@link StreamReadFeature} for list of features)
     */
    public abstract JsonParser enable(StreamReadFeature f);

    /**
     * Method for disabling specified  feature
     * (check {@link StreamReadFeature} for list of features)
     */
    public abstract JsonParser disable(StreamReadFeature f);

    /**
     * Method for checking whether specified {@link StreamReadFeature} is enabled.
     */
    public abstract boolean isEnabled(StreamReadFeature f);

    /**
     * Bulk access method for getting state of all standard {@link StreamReadFeature}s.
     * 
     * @return Bit mask that defines current states of all standard {@link StreamReadFeature}s.
     *
     * @since 3.0
     */
    public abstract int streamReadFeatures();

    /**
     * Bulk access method for getting state of all {@link FormatFeature}s, format-specific
     * on/off configuration settings.
     * 
     * @return Bit mask that defines current states of all standard {@link FormatFeature}s.
     *
     * @since 3.0
     */
    public abstract int formatReadFeatures();

    /*
    /**********************************************************************
    /* Public API, iterating accessors: general
    /**********************************************************************
     */

    /**
     * Main iteration method, which will advance stream enough
     * to determine type of the next token, if any. If none
     * remaining (stream has no content other than possible
     * white space before ending), null will be returned.
     *
     * @return Next token from the stream, if any found, or null
     *   to indicate end-of-input
     */
    public abstract JsonToken nextToken() throws IOException;

    /**
     * Iteration method that will advance stream enough
     * to determine type of the next token that is a value type
     * (including JSON Array and Object start/end markers).
     * Or put another way, nextToken() will be called once,
     * and if {@link JsonToken#FIELD_NAME} is returned, another
     * time to get the value for the field.
     * Method is most useful for iterating over value entries
     * of JSON objects; field name will still be available
     * by calling {@link #currentName} when parser points to
     * the value.
     *
     * @return Next non-field-name token from the stream, if any found,
     *   or null to indicate end-of-input (or, for non-blocking
     *   parsers, {@link JsonToken#NOT_AVAILABLE} if no tokens were
     *   available yet)
     */
    public abstract JsonToken nextValue() throws IOException;

    /**
     * Method that will skip all child tokens of an array or
     * object token that the parser currently points to,
     * iff stream points to
     * {@link JsonToken#START_OBJECT} or {@link JsonToken#START_ARRAY}.
     * If not, it will do nothing.
     * After skipping, stream will point to <b>matching</b>
     * {@link JsonToken#END_OBJECT} or {@link JsonToken#END_ARRAY}
     * (possibly skipping nested pairs of START/END OBJECT/ARRAY tokens
     * as well as value tokens).
     * The idea is that after calling this method, application
     * will call {@link #nextToken} to point to the next
     * available token, if any.
     */
    public abstract JsonParser skipChildren() throws IOException;

    /**
     * Method that may be used to force full handling of the current token
     * so that even if lazy processing is enabled, the whole contents are
     * read for possible retrieval. This is usually used to ensure that
     * the token end location is available, as well as token contents
     * (similar to what calling, say {@link #getTextCharacters()}, would
     * achieve).
     *<p>
     * Note that for many dataformat implementations this method
     * will not do anything; this is the default implementation unless
     * overridden by sub-classes.
     */
    public abstract void finishToken() throws IOException;

    /*
    /**********************************************************************
    /* Public API, iterating accessors: field names
    /**********************************************************************
     */

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * verifies whether it is {@link JsonToken#FIELD_NAME}; if it is,
     * returns same as {@link #currentName()}, otherwise null.
     */
    public abstract String nextFieldName() throws IOException;

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * verifies whether it is {@link JsonToken#FIELD_NAME} with specified name
     * and returns result of that comparison.
     * It is functionally equivalent to:
     *<pre>
     *  return (nextToken() == JsonToken.FIELD_NAME) &amp;&amp; str.getValue().equals(currentName());
     *</pre>
     * but may be faster for parser to verify, and can therefore be used if caller
     * expects to get such a property name from input next.
     * 
     * @param str Property name to compare next token to (if next token is
     *   <code>JsonToken.FIELD_NAME</code>)
     */
    public abstract boolean nextFieldName(SerializableString str) throws IOException;

    /**
     * Method that tries to match next token from stream as {@link JsonToken#FIELD_NAME},
     * and if so, further match it to one of pre-specified (field) names.
     * If match succeeds, field index (non-negative `int`) is returned; otherwise one of
     * marker constants from {@link FieldNameMatcher}.
     *
     * @since 3.0
     */
    public abstract int nextFieldName(FieldNameMatcher matcher) throws IOException;

    /**
     * Method that verifies that the current token (see {@link #currentToken} is
     * {@link JsonToken#FIELD_NAME} and if so, further match it to one of pre-specified (field) names.
     * If match succeeds, field index (non-negative `int`) is returned; otherwise one of
     * marker constants from {@link FieldNameMatcher}.
     *
     * @since 3.0
     */
    public abstract int currentFieldName(FieldNameMatcher matcher) throws IOException;

    /*
    /**********************************************************************
    /* Public API, iterating accessors: typed values
    /**********************************************************************
     */

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * if it is {@link JsonToken#VALUE_STRING} returns contained String value;
     * otherwise returns null.
     * It is functionally equivalent to:
     *<pre>
     *  return (nextToken() == JsonToken.VALUE_STRING) ? getText() : null;
     *</pre>
     * but may be faster for parser to process, and can therefore be used if caller
     * expects to get a String value next from input.
     */
    public String nextTextValue() throws IOException {
        return (nextToken() == JsonToken.VALUE_STRING) ? getText() : null;
    }

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * if it is {@link JsonToken#VALUE_NUMBER_INT} returns 32-bit int value;
     * otherwise returns specified default value
     * It is functionally equivalent to:
     *<pre>
     *  return (nextToken() == JsonToken.VALUE_NUMBER_INT) ? getIntValue() : defaultValue;
     *</pre>
     * but may be faster for parser to process, and can therefore be used if caller
     * expects to get an int value next from input.
     */
    public int nextIntValue(int defaultValue) throws IOException {
        return (nextToken() == JsonToken.VALUE_NUMBER_INT) ? getIntValue() : defaultValue;
    }

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * if it is {@link JsonToken#VALUE_NUMBER_INT} returns 64-bit long value;
     * otherwise returns specified default value
     * It is functionally equivalent to:
     *<pre>
     *  return (nextToken() == JsonToken.VALUE_NUMBER_INT) ? getLongValue() : defaultValue;
     *</pre>
     * but may be faster for parser to process, and can therefore be used if caller
     * expects to get a long value next from input.
     */
    public long nextLongValue(long defaultValue) throws IOException {
        return (nextToken() == JsonToken.VALUE_NUMBER_INT) ? getLongValue() : defaultValue;
    }

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * if it is {@link JsonToken#VALUE_TRUE} or {@link JsonToken#VALUE_FALSE}
     * returns matching Boolean value; otherwise return null.
     * It is functionally equivalent to:
     *<pre>
     *  JsonToken t = nextToken();
     *  if (t == JsonToken.VALUE_TRUE) return Boolean.TRUE;
     *  if (t == JsonToken.VALUE_FALSE) return Boolean.FALSE;
     *  return null;
     *</pre>
     * but may be faster for parser to process, and can therefore be used if caller
     * expects to get a Boolean value next from input.
     */
    public Boolean nextBooleanValue() throws IOException {
        JsonToken t = nextToken();
        if (t == JsonToken.VALUE_TRUE) { return Boolean.TRUE; }
        if (t == JsonToken.VALUE_FALSE) { return Boolean.FALSE; }
        return null;
    }

    /*
    /**********************************************************************
    /* Public API, simple token id/type access
    /**********************************************************************
     */

    /**
     * Accessor to find which token parser currently points to, if any;
     * null will be returned if none.
     * If return value is non-null, data associated with the token
     * is available via other accessor methods.
     *
     * @return Type of the token this parser currently points to,
     *   if any: null before any tokens have been read, and
     *   after end-of-input has been encountered, as well as
     *   if the current token has been explicitly cleared.
     */
    public abstract JsonToken currentToken();

    /**
     * @deprecated Since 3.0 use {@link #currentToken} instead.
     */
    @Deprecated
    public JsonToken getCurrentToken() { return currentToken(); }

    /**
     * Method similar to {@link #currentToken()} but that returns an
     * <code>int</code> instead of {@link JsonToken} (enum value).
     *<p>
     * Use of int directly is typically more efficient on switch statements,
     * so this method may be useful when building low-overhead codecs.
     * Note, however, that effect may not be big enough to matter: make sure
     * to profile performance before deciding to use this method.
     * 
     * @return <code>int</code> matching one of constants from {@link JsonTokenId}.
     */
    public abstract int currentTokenId();

    /**
     * Method for checking whether parser currently points to
     * a token (and data for that token is available).
     * Equivalent to check for <code>parser.getCurrentToken() != null</code>.
     *
     * @return True if the parser just returned a valid
     *   token via {@link #nextToken}; false otherwise (parser
     *   was just constructed, encountered end-of-input
     *   and returned null from {@link #nextToken}, or the token
     *   has been consumed)
     */
    public abstract boolean hasCurrentToken();

    /**
     * Method that is functionally equivalent to:
     *<code>
     *  return currentTokenId() == id
     *</code>
     * but may be more efficiently implemented.
     *<p>
     * Note that no traversal or conversion is performed; so in some
     * cases calling method like {@link #isExpectedStartArrayToken()}
     * is necessary instead.
     */
    public abstract boolean hasTokenId(int id);

    /**
     * Method that is functionally equivalent to:
     *<code>
     *  return currentToken() == t
     *</code>
     * but may be more efficiently implemented.
     *<p>
     * Note that no traversal or conversion is performed; so in some
     * cases calling method like {@link #isExpectedStartArrayToken()}
     * is necessary instead.
     */
    public abstract boolean hasToken(JsonToken t);

    /**
     * Specialized accessor that can be used to verify that the current
     * token indicates start array (usually meaning that current token
     * is {@link JsonToken#START_ARRAY}) when start array is expected.
     * For some specialized parsers this can return true for other cases
     * as well; this is usually done to emulate arrays in cases underlying
     * format is ambiguous (XML, for example, has no format-level difference
     * between Objects and Arrays; it just has elements).
     *<p>
     * Default implementation is equivalent to:
     *<pre>
     *   currentToken() == JsonToken.START_ARRAY
     *</pre>
     * but may be overridden by custom parser implementations.
     *
     * @return True if the current token can be considered as a
     *   start-array marker (such {@link JsonToken#START_ARRAY});
     *   false if not.
     */
    public boolean isExpectedStartArrayToken() { return currentToken() == JsonToken.START_ARRAY; }

    /**
     * Similar to {@link #isExpectedStartArrayToken()}, but checks whether stream
     * currently points to {@link JsonToken#START_OBJECT}.
     */
    public boolean isExpectedStartObjectToken() { return currentToken() == JsonToken.START_OBJECT; }

    /**
     * Access for checking whether current token is a numeric value token, but
     * one that is of "not-a-number" (NaN) variety (including both "NaN" AND
     * positive/negative infinity!): not supported by all formats,
     * but often supported for {@link JsonToken#VALUE_NUMBER_FLOAT}.
     * NOTE: roughly equivalent to calling <code>!Double.isFinite()</code>
     * on value you would get from calling {@link #getDoubleValue()}.
     */
    public boolean isNaN() throws IOException {
        return false;
    }

    /*
    /**********************************************************************
    /* Public API, token state overrides
    /**********************************************************************
     */

    /**
     * Method called to "consume" the current token by effectively
     * removing it so that {@link #hasCurrentToken} returns false, and
     * {@link #currentToken} null).
     * Cleared token value can still be accessed by calling
     * {@link #getLastClearedToken} (if absolutely needed), but
     * usually isn't.
     *<p>
     * Method was added to be used by the optional data binder, since
     * it has to be able to consume last token used for binding (so that
     * it will not be used again).
     */
    public abstract void clearCurrentToken();

    /**
     * Method that can be called to get the last token that was
     * cleared using {@link #clearCurrentToken}. This is not necessarily
     * the latest token read.
     * Will return null if no tokens have been cleared,
     * or if parser has been closed.
     */
    public abstract JsonToken getLastClearedToken();
    
    /**
     * Method that can be used to change what is considered to be
     * the current (field) name.
     * May be needed to support non-JSON data formats or unusual binding
     * conventions; not needed for typical processing.
     *<p>
     * Note that use of this method should only be done as sort of last
     * resort, as it is a work-around for regular operation.
     * 
     * @param name Name to use as the current name; may be null.
     */
    public abstract void overrideCurrentName(String name);

    /*
    /**********************************************************************
    /* Public API, access to token information, text
    /**********************************************************************
     */

    /**
     * Method that can be called to get the name associated with
     * the current token: for {@link JsonToken#FIELD_NAME}s it will
     * be the same as what {@link #getText} returns;
     * for field values it will be the preceding field name;
     * and for others (array values, root-level values) null.
     *
     * @since 3.0
     */
    public abstract String currentName() throws IOException;

    /**
     * @deprecated Since 3.0 use {@link #currentName} instead
     */
    @Deprecated
    public String getCurrentName() throws IOException { return currentName(); }

    /**
     * Method for accessing textual representation of the current token;
     * if no current token (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any token type.
     */
    public abstract String getText() throws IOException;

    /**
     * Method to read the textual representation of the current token in chunks and 
     * pass it to the given Writer.
     * Conceptually same as calling:
     *<pre>
     *  writer.write(parser.getText());
     *</pre>
     * but should typically be more efficient as longer content does need to
     * be combined into a single <code>String</code> to return, and write
     * can occur directly from intermediate buffers Jackson uses.
     * 
     * @return The number of characters written to the Writer
     */
    public int getText(Writer writer) throws IOException, UnsupportedOperationException
    {
        String str = getText();
        if (str == null) {
            return 0;
        }
        writer.write(str);
        return str.length();
    }

    /**
     * Method similar to {@link #getText}, but that will return
     * underlying (unmodifiable) character array that contains
     * textual value, instead of constructing a String object
     * to contain this information.
     * Note, however, that:
     *<ul>
     * <li>Textual contents are not guaranteed to start at
     *   index 0 (rather, call {@link #getTextOffset}) to
     *   know the actual offset
     *  </li>
     * <li>Length of textual contents may be less than the
     *  length of returned buffer: call {@link #getTextLength}
     *  for actual length of returned content.
     *  </li>
     * </ul>
     *<p>
     * Note that caller <b>MUST NOT</b> modify the returned
     * character array in any way -- doing so may corrupt
     * current parser state and render parser instance useless.
     *<p>
     * The only reason to call this method (over {@link #getText})
     * is to avoid construction of a String object (which
     * will make a copy of contents).
     */
    public abstract char[] getTextCharacters() throws IOException;

    /**
     * Accessor used with {@link #getTextCharacters}, to know length
     * of String stored in returned buffer.
     *
     * @return Number of characters within buffer returned
     *   by {@link #getTextCharacters} that are part of
     *   textual content of the current token.
     */
    public abstract int getTextLength() throws IOException;

    /**
     * Accessor used with {@link #getTextCharacters}, to know offset
     * of the first text content character within buffer.
     *
     * @return Offset of the first character within buffer returned
     *   by {@link #getTextCharacters} that is part of
     *   textual content of the current token.
     */
    public abstract int getTextOffset() throws IOException;

    /**
     * Method that can be used to determine whether calling of
     * {@link #getTextCharacters} would be the most efficient
     * way to access textual content for the event parser currently
     * points to.
     *<p> 
     * Default implementation simply returns false since only actual
     * implementation class has knowledge of its internal buffering
     * state.
     * Implementations are strongly encouraged to properly override
     * this method, to allow efficient copying of content by other
     * code.
     * 
     * @return True if parser currently has character array that can
     *   be efficiently returned via {@link #getTextCharacters}; false
     *   means that it may or may not exist
     */
    public abstract boolean hasTextCharacters();

    /*
    /**********************************************************************
    /* Public API, access to token information, numeric
    /**********************************************************************
     */

    /**
     * Generic number value accessor method that will work for
     * all kinds of numeric values. It will return the optimal
     * (simplest/smallest possible) wrapper object that can
     * express the numeric value just parsed.
     */
    public abstract Number getNumberValue() throws IOException;

    /**
     * If current token is of type 
     * {@link JsonToken#VALUE_NUMBER_INT} or
     * {@link JsonToken#VALUE_NUMBER_FLOAT}, returns
     * one of {@link NumberType} constants; otherwise returns null.
     */
    public abstract NumberType getNumberType() throws IOException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java byte primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * Java byte, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public byte getByteValue() throws IOException {
        int value = getIntValue();
        // So far so good: but does it fit?
        // Let's actually allow range of [-128, 255] instead of just signed range of [-128, 127]
        // since "unsigned" usage quite common for bytes (but Java may use signed range, too)
        if (value < MIN_BYTE_I || value > MAX_BYTE_I) {
            throw _constructError("Numeric value (%s) out of range of `byte`", getText());
        }
        return (byte) value;
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java short primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * Java short, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public short getShortValue() throws IOException
    {
        int value = getIntValue();
        if (value < MIN_SHORT_I || value > MAX_SHORT_I) {
            throw _constructError("Numeric value (%s) out of range of `short`", getText());
        }
        return (short) value;
    }

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java int primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * Java int, a {@link JsonParseException}
     * may be thrown to indicate numeric overflow/underflow.
     */
    public abstract int getIntValue() throws IOException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a Java long primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting to int; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the token is an integer, but its value falls
     * outside of range of Java long, a {@link JsonParseException}
     * may be thrown to indicate numeric overflow/underflow.
     */
    public abstract long getLongValue() throws IOException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can not be used as a Java long primitive type due to its
     * magnitude.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDecimalValue}
     * and then constructing a {@link BigInteger} from that value.
     */
    public abstract BigInteger getBigIntegerValue() throws IOException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} and
     * it can be expressed as a Java float primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_INT};
     * if so, it is equivalent to calling {@link #getLongValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the value falls
     * outside of range of Java float, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract float getFloatValue() throws IOException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} and
     * it can be expressed as a Java double primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_INT};
     * if so, it is equivalent to calling {@link #getLongValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the value falls
     * outside of range of Java double, a {@link JsonParseException}
     * will be thrown to indicate numeric overflow/underflow.
     */
    public abstract double getDoubleValue() throws IOException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} or
     * {@link JsonToken#VALUE_NUMBER_INT}. No under/overflow exceptions
     * are ever thrown.
     */
    public abstract BigDecimal getDecimalValue() throws IOException;

    /*
    /**********************************************************************
    /* Public API, access to token information, other
    /**********************************************************************
     */
    
    /**
     * Convenience accessor that can be called when the current
     * token is {@link JsonToken#VALUE_TRUE} or
     * {@link JsonToken#VALUE_FALSE}.
     *<p>
     * Note: if the token is not of above-mentioned boolean types,
 an integer, but its value falls
     * outside of range of Java long, a {@link JsonParseException}
     * may be thrown to indicate numeric overflow/underflow.
     */
    public boolean getBooleanValue() throws IOException {
        JsonToken t = currentToken();
        if (t == JsonToken.VALUE_TRUE) return true;
        if (t == JsonToken.VALUE_FALSE) return false;
        throw new JsonParseException(this,
            String.format("Current token (%s) not of boolean type", t))
                .withRequestPayload(_requestPayload);
    }

    /**
     * Accessor that can be called if (and only if) the current token
     * is {@link JsonToken#VALUE_EMBEDDED_OBJECT}. For other token types,
     * null is returned.
     *<p>
     * Note: only some specialized parser implementations support
     * embedding of objects (usually ones that are facades on top
     * of non-streaming sources, such as object trees). One exception
     * is access to binary content (whether via base64 encoding or not)
     * which typically is accessible using this method, as well as
     * {@link #getBinaryValue()}.
     */
    public Object getEmbeddedObject() throws IOException { return null; }

    /*
    /**********************************************************************
    /* Public API, access to token information, binary
    /**********************************************************************
     */

    /**
     * Method that can be used to read (and consume -- results
     * may not be accessible using other methods after the call)
     * base64-encoded binary data
     * included in the current textual JSON value.
     * It works similar to getting String value via {@link #getText}
     * and decoding result (except for decoding part),
     * but should be significantly more performant.
     *<p>
     * Note that non-decoded textual contents of the current token
     * are not guaranteed to be accessible after this method
     * is called. Current implementation, for example, clears up
     * textual content during decoding.
     * Decoded binary content, however, will be retained until
     * parser is advanced to the next event.
     *
     * @param bv Expected variant of base64 encoded
     *   content (see {@link Base64Variants} for definitions
     *   of "standard" variants).
     *
     * @return Decoded binary data
     */
    public abstract byte[] getBinaryValue(Base64Variant bv) throws IOException;

    /**
     * Convenience alternative to {@link #getBinaryValue(Base64Variant)}
     * that defaults to using
     * {@link Base64Variants#getDefaultVariant} as the default encoding.
     */
    public byte[] getBinaryValue() throws IOException {
        return getBinaryValue(Base64Variants.getDefaultVariant());
    }

    /**
     * Method that can be used as an alternative to {@link #getBigIntegerValue()},
     * especially when value can be large. The main difference (beyond method
     * of returning content using {@link OutputStream} instead of as byte array)
     * is that content will NOT remain accessible after method returns: any content
     * processed will be consumed and is not buffered in any way. If caller needs
     * buffering, it has to implement it.
     * 
     * @param out Output stream to use for passing decoded binary data
     * 
     * @return Number of bytes that were decoded and written via {@link OutputStream}
     */
    public int readBinaryValue(OutputStream out) throws IOException {
        return readBinaryValue(Base64Variants.getDefaultVariant(), out);
    }

    /**
     * Similar to {@link #readBinaryValue(OutputStream)} but allows explicitly
     * specifying base64 variant to use.
     * 
     * @param bv base64 variant to use
     * @param out Output stream to use for passing decoded binary data
     * 
     * @return Number of bytes that were decoded and written via {@link OutputStream}
     */
    public int readBinaryValue(Base64Variant bv, OutputStream out) throws IOException {
        _reportUnsupportedOperation();
        return 0; // never gets here
    }
    
    /*
    /**********************************************************************
    /* Public API, access to token information, coercion/conversion
    /**********************************************************************
     */
    
    /**
     * Method that will try to convert value of current token to a
     * <b>int</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation can not be converted to an int (including structured type
     * markers like start/end Object/Array)
     * default value of <b>0</b> will be returned; no exceptions are thrown.
     */
    public int getValueAsInt() throws IOException {
        return getValueAsInt(0);
    }
    
    /**
     * Method that will try to convert value of current token to a
     * <b>int</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation can not be converted to an int (including structured type
     * markers like start/end Object/Array)
     * specified <b>def</b> will be returned; no exceptions are thrown.
     */
    public int getValueAsInt(int def) throws IOException { return def; }

    /**
     * Method that will try to convert value of current token to a
     * <b>long</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation can not be converted to a long (including structured type
     * markers like start/end Object/Array)
     * default value of <b>0L</b> will be returned; no exceptions are thrown.
     */
    public long getValueAsLong() throws IOException {
        return getValueAsLong(0);
    }
    
    /**
     * Method that will try to convert value of current token to a
     * <b>long</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation can not be converted to a long (including structured type
     * markers like start/end Object/Array)
     * specified <b>def</b> will be returned; no exceptions are thrown.
     */
    public long getValueAsLong(long def) throws IOException {
        return def;
    }
    
    /**
     * Method that will try to convert value of current token to a Java
     * <b>double</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0.0 (false)
     * and 1.0 (true), and Strings are parsed using default Java language floating
     * point parsing rules.
     *<p>
     * If representation can not be converted to a double (including structured types
     * like Objects and Arrays),
     * default value of <b>0.0</b> will be returned; no exceptions are thrown.
     */
    public double getValueAsDouble() throws IOException {
        return getValueAsDouble(0.0);
    }
    
    /**
     * Method that will try to convert value of current token to a
     * Java <b>double</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0.0 (false)
     * and 1.0 (true), and Strings are parsed using default Java language floating
     * point parsing rules.
     *<p>
     * If representation can not be converted to a double (including structured types
     * like Objects and Arrays),
     * specified <b>def</b> will be returned; no exceptions are thrown.
     */
    public double getValueAsDouble(double def) throws IOException {
        return def;
    }

    /**
     * Method that will try to convert value of current token to a
     * <b>boolean</b>.
     * JSON booleans map naturally; integer numbers other than 0 map to true, and
     * 0 maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     *<p>
     * If representation can not be converted to a boolean value (including structured types
     * like Objects and Arrays),
     * default value of <b>false</b> will be returned; no exceptions are thrown.
     */
    public boolean getValueAsBoolean() throws IOException {
        return getValueAsBoolean(false);
    }

    /**
     * Method that will try to convert value of current token to a
     * <b>boolean</b>.
     * JSON booleans map naturally; integer numbers other than 0 map to true, and
     * 0 maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     *<p>
     * If representation can not be converted to a boolean value (including structured types
     * like Objects and Arrays),
     * specified <b>def</b> will be returned; no exceptions are thrown.
     */
    public boolean getValueAsBoolean(boolean def) throws IOException {
        return def;
    }

    /**
     * Method that will try to convert value of current token to a
     * {@link java.lang.String}.
     * JSON Strings map naturally; scalar values get converted to
     * their textual representation.
     * If representation can not be converted to a String value (including structured types
     * like Objects and Arrays and null token), default value of
     * <b>null</b> will be returned; no exceptions are thrown.
     */
    public String getValueAsString() throws IOException {
        return getValueAsString(null);
    }
    
    /**
     * Method that will try to convert value of current token to a
     * {@link java.lang.String}.
     * JSON Strings map naturally; scalar values get converted to
     * their textual representation.
     * If representation can not be converted to a String value (including structured types
     * like Objects and Arrays and null token), specified default value
     * will be returned; no exceptions are thrown.
     */
    public abstract String getValueAsString(String def) throws IOException;

    /*
    /**********************************************************************
    /* Public API, Native Ids (type, object)
    /**********************************************************************
     */

    /**
     * Introspection method that may be called to see if the underlying
     * data format supports some kind of Object Ids natively (many do not;
     * for example, JSON doesn't).
     *<p>
     * Default implementation returns true; overridden by data formats
     * that do support native Object Ids. Caller is expected to either
     * use a non-native notation (explicit property or such), or fail,
     * in case it can not use native object ids.
     */
    public boolean canReadObjectId() { return false; }

    /**
     * Introspection method that may be called to see if the underlying
     * data format supports some kind of Type Ids natively (many do not;
     * for example, JSON doesn't).
     *<p>
     * Default implementation returns true; overridden by data formats
     * that do support native Type Ids. Caller is expected to either
     * use a non-native notation (explicit property or such), or fail,
     * in case it can not use native type ids.
     */
    public boolean canReadTypeId() { return false; }

    /**
     * Method that can be called to check whether current token
     * (one that was just read) has an associated Object id, and if
     * so, return it.
     * Note that while typically caller should check with {@link #canReadObjectId}
     * first, it is not illegal to call this method even if that method returns
     * true; but if so, it will return null. This may be used to simplify calling
     * code.
     *<p>
     * Default implementation will simply return null.
     */
    public Object getObjectId() throws IOException { return null; }

    /**
     * Method that can be called to check whether current token
     * (one that was just read) has an associated type id, and if
     * so, return it.
     * Note that while typically caller should check with {@link #canReadTypeId}
     * first, it is not illegal to call this method even if that method returns
     * true; but if so, it will return null. This may be used to simplify calling
     * code.
     *<p>
     * Default implementation will simply return null.
     */
    public Object getTypeId() throws IOException { return null; }

    /*
    /**********************************************************************
    /* Public API, optional data binding functionality
    /**********************************************************************
     */

    /**
     * Method to deserialize stream content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<br>
     * <b>Note</b>: method can only be called if the parser has
     * been constructed with a linkage to
     * {@link ObjectReadContext}; this is true if constructed by
     * databinding layer above, or by factory method that takes in
     * context object.
     *<p>
     * This method may advance the event stream, for structured values
     * the current token will be the closing end marker (END_ARRAY,
     * END_OBJECT) of the bound structure. For non-structured values
     * (and for {@link JsonToken#VALUE_EMBEDDED_OBJECT})
     * stream is not advanced.
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    public abstract <T> T readValueAs(Class<T> valueType) throws IOException;

    /**
     * Method to deserialize stream content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token"
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     *<br>
     * <b>Note</b>: method can only be called if the parser has
     * been constructed with a linkage to
     * {@link ObjectReadContext}; this is true if constructed by
     * databinding layer above, or by factory method that takes in
     * context object.
     *<p>
     * This method may advance the event stream, for structured types
     * the current token will be the closing end marker (END_ARRAY,
     * END_OBJECT) of the bound structure. For non-structured types
     * (and for {@link JsonToken#VALUE_EMBEDDED_OBJECT})
     * stream is not advanced.
     */
    public abstract <T> T readValueAs(TypeReference<?> valueTypeRef) throws IOException;

    /**
     * @since 3.0
     */
    public abstract <T> T readValueAs(ResolvedType type) throws IOException;
    
    /**
     * Method to deserialize stream content into equivalent "tree model",
     * represented by root {@link TreeNode} of resulting model.
     * For Array values it will an array node (with child nodes),
     * for Object values object node (with child nodes), and for other types
     * matching leaf node type. Empty or whitespace documents are null.
     *<br>
     * <b>Note</b>: method can only be called if the parser has
     * been constructed with a linkage to
     * {@link ObjectReadContext}; this is true if constructed by
     * databinding layer above, or by factory method that takes in
     * context object.
     *
     * @return root of the document, or null if empty or whitespace.
     */
    public abstract <T extends TreeNode> T readValueAsTree() throws IOException;

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    /**
     * Helper method for constructing {@link JsonParseException}s
     * based on current state of the parser
     */
    protected JsonParseException _constructError(String msg) {
        return new JsonParseException(this, msg)
            .withRequestPayload(_requestPayload);
    }

    protected JsonParseException _constructError(String msg, Object arg) {
        return new JsonParseException(this, String.format(msg, arg))
            .withRequestPayload(_requestPayload);
    }

    protected JsonParseException _constructError(String msg, Object arg1, Object arg2) {
        return new JsonParseException(this, String.format(msg, arg1, arg2))
            .withRequestPayload(_requestPayload);
    }
    
    /**
     * Helper method to call for operations that are not supported by
     * parser implementation.
     */
    protected void _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Operation not supported by parser of type "+getClass().getName());
    }
}
