/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package tools.jackson.core;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.async.NonBlockingInputFeeder;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.*;

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
    /**
     * Enumeration of possible "native" (optimal) types that can be
     * used for numbers.
     */
    public enum NumberType {
        INT, LONG, BIG_INTEGER, FLOAT, DOUBLE, BIG_DECIMAL
    }

    /**
     * Enumeration of possible physical Floating-Point types that
     * underlying format uses. Used to indicate most accurate (and
     * efficient) representation if known (if not known,
     * {@link NumberTypeFP#UNKNOWN} is used).
     */
    public enum NumberTypeFP {
        /**
         * Special "mini-float" that some binary formats support.
         */
        FLOAT16,

        /**
         * Standard IEEE-754 single-precision 32-bit binary value
         */
        FLOAT32,

        /**
         * Standard IEEE-754 double-precision 64-bit binary value
         */
        DOUBLE64,

        /**
         * Unlimited precision, decimal (10-based) values
         */
        BIG_DECIMAL,

        /**
         * Constant used when type is not known, or there is no specific
         * type to match: most commonly used for textual formats like JSON
         * where representation does not necessarily have single easily detectable
         * optimal representation (for example, value {@code 0.1} has no
         * exact binary representation whereas {@code 0.25} has exact representation
         * in every binary type supported)
         */
        UNKNOWN;
    }

    /**
     * Set of default {@link StreamReadCapability}ies enabled: usable as basis
     * for format-specific instances or placeholder if non-null instance needed.
     */
    protected final static JacksonFeatureSet<StreamReadCapability> DEFAULT_READ_CAPABILITIES
        = JacksonFeatureSet.fromDefaults(StreamReadCapability.values());

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JsonParser() { }

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
    /* Public API: basic context access
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
     *<p>
     * NOTE: method was called {@code getParsingContext()} in Jackson 2.x
     *
     * @return Stream output context ({@link TokenStreamContext}) associated with this parser
     */
    public abstract TokenStreamContext streamReadContext();

    /**
     * Accessor for context object provided by higher level data-binding
     * functionality (or, in some cases, simple placeholder of the same)
     * that allows some level of interaction including ability to trigger
     * deserialization of Object values through generator instance.
     *<p>
     * Context object is used by parser to implement some methods,
     * like {@code readValueAs(...)}
     *
     * @return Object write context ({@link ObjectReadContext}) associated with this parser
     *
     * @since 3.0
     */
    public abstract ObjectReadContext objectReadContext();

    /*
    /**********************************************************************
    /* Public API, input source, location access
    /**********************************************************************
     */

    /**
     * Method that return the <b>starting</b> location of the current
     * token; that is, position of the first character from input
     * that starts the current token.
     *<p>
     * Note that the location is not guaranteed to be accurate (although most
     * implementation will try their best): some implementations may only
     * return {@link TokenStreamLocation#NA} due to not having access
     * to input location information (when delegating actual decoding work
     * to other library).
     *
     * @return Starting location of the token parser currently points to
     */
    public abstract TokenStreamLocation currentTokenLocation();

    /**
     * Method that returns location of the last processed character;
     * usually for error reporting purposes.
     *<p>
     * Note that the location is not guaranteed to be accurate (although most
     * implementation will try their best): some implementations may only
     * report specific boundary locations (start or end locations of tokens)
     * and others only return {@link TokenStreamLocation#NA} due to not having access
     * to input location information (when delegating actual decoding work
     * to other library).
     *
     * @return Location of the last processed input unit (byte or character)
     */
    public abstract TokenStreamLocation currentLocation();

    /**
     * Get an approximate count of the number of tokens that have been read.
     * This count is likely to be only updated if {@link StreamReadConstraints.Builder#maxTokenCount(long)}
     * has been used to set a limit on the number of tokens that can be read.
     *
     * @return the number of tokens that have been read (-1 if the count is not available)
     */
    public abstract long currentTokenCount();

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
     *<p>
     * NOTE: was named {@code getInputSource()} in Jackson 2.x.
     *
     * @return Input source this parser was configured with
     */
    public abstract Object streamReadInputSource();

    /*
    /**********************************************************************
    /* Attaching additional metadata: current value
    /**********************************************************************
     */

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getParsingContext().currentValue();
     *</code>
     *<p>
     * Note that "current value" is NOT populated (or used) by Streaming parser;
     * it is only used by higher-level data-binding functionality.
     * The reason it is included here is that it can be stored and accessed hierarchically,
     * and gets passed through data-binding.
     *
     * @return "Current value" for the current input context this parser has
     */
    public abstract Object currentValue();

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getParsingContext().assignCurrentValue(v);
     *</code>
     *
     * @param v "Current value" to assign to the current input context of this parser
     */
    public abstract void assignCurrentValue(Object v);

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
     * If non-blocking decoding is {@code true}, it is possible to call
     * {@link #nonBlockingInputFeeder()} to obtain object to use
     * for feeding input; otherwise (<code>false</code> returned)
     * input is read by blocking.
     *
     * @return True if this is a non-blocking ("asynchronous") parser
     */
    public boolean canParseAsync() { return false; }

    /**
     * Method that will either return a feeder instance (if parser uses
     * non-blocking, aka asynchronous access); or <code>null</code> for
     * parsers that use blocking I/O.
     *
     * @return Input feeder to use with non-blocking (async) parsing
     */
    public NonBlockingInputFeeder nonBlockingInputFeeder() {
        return null;
    }

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
     * {@link tools.jackson.core.json.JsonFactory} for details,
     * but the general
     * idea is that if caller passes in closable resource (such
     * as {@link InputStream} or {@link Reader}) parser does NOT
     * own the source; but if it passes a reference (such as
     * {@link java.io.File} or {@link java.net.URL} and creates
     * stream or reader it does own them.
     */
    @Override
    public abstract void close();

    /**
     * Method that can be called to determine whether this parser
     * is closed or not. If it is closed, no new tokens can be
     * retrieved by calling {@link #nextToken} (and the underlying
     * stream may be closed). Closing may be due to an explicit
     * call to {@link #close} or because parser has encountered
     * end of input.
     *
     * @return {@code True} if this parser instance has been closed
     */
    public abstract boolean isClosed();

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
     * it cannot.
     *
     * @param out OutputStream to which buffered, undecoded content is written to
     *
     * @return -1 if the underlying content source is not byte based
     *    (that is, input cannot be sent to {@link OutputStream};
     *    otherwise number of bytes released (0 if there was nothing to release)
     *
     * @throws JacksonException if write to stream threw exception
     */
    public int releaseBuffered(OutputStream out) throws JacksonException {
        return -1;
    }

    /**
     * Method that can be called to push back any content that
     * has been read but not consumed by the parser.
     * This is usually
     * done after reading all content of interest using parser.
     * Content is released by writing it to given writer if possible;
     * if underlying input is char-based it can released, if not (byte-based)
     * it cannot.
     *
     * @param w Writer to which buffered but unprocessed content is written to
     *
     * @return -1 if the underlying content source is not char-based
     *    (that is, input cannot be sent to {@link Writer};
     *    otherwise number of chars released (0 if there was nothing to release)
     *
     * @throws JacksonException if write using Writer threw exception
     */
    public int releaseBuffered(Writer w) throws JacksonException { return -1; }

    /*
    /**********************************************************************
    /* Public API, configuration
    /**********************************************************************
     */

    /**
     * Method for checking whether specified {@link StreamReadFeature} is enabled.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} otherwise
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
     * Method for accessing Schema that this parser uses, if any.
     * Default implementation returns null.
     *
     * @return {@link FormatSchema} assigned to this parser, if any; {@code null} if none
     */
    public FormatSchema getSchema() { return null; }

    /**
     * Accessor for getting metadata on capabilities of this parser, based on
     * underlying data format being read (directly or indirectly).
     *
     * @return Set of read capabilities for content to read via this parser
     */
    public abstract JacksonFeatureSet<StreamReadCapability> streamReadCapabilities();

    /**
     * Get the constraints to apply when performing streaming reads.
     *
     * @return Constraints applied to this parser
     */
    public abstract StreamReadConstraints streamReadConstraints();

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
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract JsonToken nextToken() throws JacksonException;

    /**
     * Iteration method that will advance stream enough
     * to determine type of the next token that is a value type
     * (including JSON Array and Object start/end markers).
     * Or put another way, nextToken() will be called once,
     * and if {@link JsonToken#PROPERTY_NAME} is returned, another
     * time to get the value of the property.
     * Method is most useful for iterating over value entries
     * of JSON objects; Object property name will still be available
     * by calling {@link #currentName} when parser points to
     * the value.
     *
     * @return Next non-field-name token from the stream, if any found,
     *   or null to indicate end-of-input (or, for non-blocking
     *   parsers, {@link JsonToken#NOT_AVAILABLE} if no tokens were
     *   available yet)
     *
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract JsonToken nextValue() throws JacksonException;

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
     *
     * @return This parser, to allow call chaining
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract JsonParser skipChildren() throws JacksonException;

    /**
     * Method that may be used to force full handling of the current token
     * so that even if lazy processing is enabled, the whole contents are
     * read for possible retrieval. This is usually used to ensure that
     * the token end location is available, as well as token contents
     * (similar to what calling, say {@link #getStringCharacters()}, would
     * achieve).
     *<p>
     * Note that for many dataformat implementations this method
     * will not do anything; this is the default implementation unless
     * overridden by sub-classes.
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract void finishToken() throws JacksonException;

    /*
    /**********************************************************************
    /* Public API, iterating accessors: property names
    /**********************************************************************
     */

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * verifies whether it is {@link JsonToken#PROPERTY_NAME}; if it is,
     * returns same as {@link #currentName()}, otherwise null.
     *<P>
     * NOTE: in Jackson 2.x method was called {@code nextFieldName()}
     *
     * @return Name of the the {@code JsonToken.PROPERTY_NAME} parser advanced to, if any;
     *   {@code null} if next token is of some other type
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract String nextName() throws JacksonException;

    /**
     * Method that fetches next token (as if calling {@link #nextToken}) and
     * verifies whether it is {@link JsonToken#PROPERTY_NAME} with specified name
     * and returns result of that comparison.
     * It is functionally equivalent to:
     *<pre>
     *  return (nextToken() == JsonToken.PROPERTY_NAME) &amp;&amp; str.getValue().equals(currentName());
     *</pre>
     * but may be faster for parser to verify, and can therefore be used if caller
     * expects to get such a property name from input next.
     *<P>
     * NOTE: in Jackson 2.x method was called {@code nextFieldName()}
     *
     * @param str Property name to compare next token to (if next token is
     *   <code>JsonToken.PROPERTY_NAME</code>)
     *
     * @return {@code True} if parser advanced to {@code JsonToken.PROPERTY_NAME} with
     *    specified name; {@code false} otherwise (different token or non-matching name)
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract boolean nextName(SerializableString str) throws JacksonException;

    /**
     * Method that tries to match next token from stream as {@link JsonToken#PROPERTY_NAME},
     * and if so, further match it to one of pre-specified (field) names.
     * If match succeeds, property index (non-negative `int`) is returned; otherwise one of
     * marker constants from {@link PropertyNameMatcher}.
     *
     * @param matcher Matcher that will handle actual matching
     *
     * @return Index of the matched property name, if non-negative, or a negative error
     *   code otherwise (see {@link PropertyNameMatcher} for details)
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     *
     * @since 3.0
     */
    public abstract int nextNameMatch(PropertyNameMatcher matcher) throws JacksonException;

    /**
     * Method that verifies that the current token (see {@link #currentToken}) is
     * {@link JsonToken#PROPERTY_NAME} and if so, further match that associated name
     * (see {@link #currentName}) to one of pre-specified (property) names.
     * If there is a match succeeds, the property index (non-negative {@code int}) is returned;
     * otherwise one of marker constants from {@link PropertyNameMatcher} is returned.
     *
     * @param matcher Matcher that will handle actual matching
     *
     * @return Index of the matched property name, if non-negative, or a negative error
     *   code otherwise (see {@link PropertyNameMatcher} for details)
     *
     * @since 3.0
     */
    public abstract int currentNameMatch(PropertyNameMatcher matcher);

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
     *  return (nextToken() == JsonToken.VALUE_STRING) ? getString() : null;
     *</pre>
     * but may be faster for parser to process, and can therefore be used if caller
     * expects to get a String value next from input.
     *
     * @return String value of {@code JsonToken.VALUE_STRING} token parser advanced
     *   to; or {@code null} if next token is of some other type
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public String nextStringValue() throws JacksonException {
        return (nextToken() == JsonToken.VALUE_STRING) ? getString() : null;
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
     *<p>
     * NOTE: value checks are performed similar to {@link #getIntValue()}
     *
     * @param defaultValue Value to return if next token is NOT of type {@code JsonToken.VALUE_NUMBER_INT}
     *
     * @return Integer ({@code int}) value of the {@code JsonToken.VALUE_NUMBER_INT} token parser advanced
     *   to; or {@code defaultValue} if next token is of some other type
     *
     * @throws JacksonException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     * @throws tools.jackson.core.exc.InputCoercionException if integer number does not fit in Java {@code int}
     */
    public int nextIntValue(int defaultValue) throws JacksonException {
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
     *<p>
     * NOTE: value checks are performed similar to {@link #getLongValue()}
     *
     * @param defaultValue Value to return if next token is NOT of type {@code JsonToken.VALUE_NUMBER_INT}
     *
     * @return {@code long} value of the {@code JsonToken.VALUE_NUMBER_INT} token parser advanced
     *   to; or {@code defaultValue} if next token is of some other type
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     * @throws tools.jackson.core.exc.InputCoercionException if integer number does not fit in Java {@code long}
     */
    public long nextLongValue(long defaultValue) throws JacksonException {
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
     *
     * @return {@code Boolean} value of the {@code JsonToken.VALUE_TRUE} or {@code JsonToken.VALUE_FALSE}
     *   token parser advanced to; or {@code null} if next token is of some other type
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public Boolean nextBooleanValue() throws JacksonException {
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
     * Method similar to {@link #currentToken()} but that returns an
     * <code>int</code> instead of {@link JsonToken} (enum value).
     *<p>
     * Use of int directly is typically more efficient on switch statements,
     * so this method may be useful when building low-overhead codecs.
     * Note, however, that effect may not be big enough to matter: make sure
     * to profile performance before deciding to use this method.
     *
     * @return {@code int} matching one of constants from {@link JsonTokenId}.
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
     *
     * @param id Token id to match (from (@link JsonTokenId})
     *
     * @return {@code True} if the parser current points to specified token
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
     *
     * @param t Token to match
     *
     * @return {@code True} if the parser current points to specified token
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
     *   {@code false} if not
     */
    public abstract boolean isExpectedStartArrayToken();

    /**
     * Similar to {@link #isExpectedStartArrayToken()}, but checks whether stream
     * currently points to {@link JsonToken#START_OBJECT}.
     *
     * @return True if the current token can be considered as a
     *   start-array marker (such {@link JsonToken#START_OBJECT});
     *   {@code false} if not
     */
    public abstract boolean isExpectedStartObjectToken();

    /**
     * Similar to {@link #isExpectedStartArrayToken()}, but checks whether stream
     * currently points to {@link JsonToken#VALUE_NUMBER_INT}.
     *<p>
     * The initial use case is for XML backend to efficiently (attempt to) coerce
     * textual content into numbers.
     *
     * @return True if the current token can be considered as a
     *   start-array marker (such {@link JsonToken#VALUE_NUMBER_INT});
     *   {@code false} if not
     */
    public abstract boolean isExpectedNumberIntToken();

    /**
     * Accessor for checking whether current token is a special
     * "not-a-number" (NaN) token (including both "NaN" AND
     * positive/negative infinity!). These values are not supported by all formats:
     * JSON, for example, only supports them if
     * {@link JsonReadFeature#ALLOW_NON_NUMERIC_NUMBERS} is enabled.
     *<p>
     * NOTE: in case where numeric value is outside range of requested type --
     * most notably {@link java.lang.Float} or {@link java.lang.Double} -- and
     * decoding results effectively in a NaN value, this method DOES NOT return
     * {@code true}: only explicit incoming markers do.
     * This is because value could still be accessed as a valid {@link BigDecimal}.
     *
     * @return {@code True} if the current token is reported as {@link JsonToken#VALUE_NUMBER_FLOAT}
     *   and represents a "Not a Number" value; {@code false} for other tokens and regular
     *   floating-point numbers.
     */
    public abstract boolean isNaN();

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
     *
     * @return Last cleared token, if any; {@code null} otherwise
     */
    public abstract JsonToken getLastClearedToken();

    /*
    /**********************************************************************
    /* Public API, access to token information, Strings
    /**********************************************************************
     */

    /**
     * Method that can be called to get the name associated with
     * the current token: for {@link JsonToken#PROPERTY_NAME}s it will
     * be the same as what {@link #getString()} returns;
     * for Object property values it will be the preceding property name;
     * and for others (array element, root-level values) null.
     *
     * @return Name of the current property name, if any, in the parsing context ({@code null} if none)
     */
    public abstract String currentName();

    /**
     * Method for accessing textual representation of the current token;
     * if no current token (before first call to {@link #nextToken}, or
     * after encountering end-of-input), returns null.
     * Method can be called for any token type.
     *
     * @return String value associated with the current token (one returned
     *   by {@link #nextToken()} or other iteration methods)
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract String getString() throws JacksonException;

    /**
     * Method to read the textual representation of the current token in chunks and
     * pass it to the given Writer.
     * Functionally same as calling:
     *<pre>
     *  writer.write(parser.getString());
     *</pre>
     * but should typically be more efficient as longer content does need to
     * be combined into a single <code>String</code> to return, and write
     * can occur directly from intermediate buffers Jackson uses.
     *<p>
     * NOTE: String value <b>will</b> still be buffered (usually
     * using {@link TextBuffer}) and <b>will</b> be accessible with
     * other {@code getString()} calls (that is, it will not be consumed).
     * So this accessor only avoids construction of {@link java.lang.String}
     * compared to plain {@link #getString()} method.
     *<p>
     * NOTE: In Jackson 2.x this method was called {@code getString(Writer)}.
     *
     * @param writer Writer to write String value to
     *
     * @return The number of characters written to the Writer
     *
     * @throws JacksonIOException for low-level read issues, or failed write using {@link Writer}
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract int getString(Writer writer) throws JacksonException;

    /**
     * Method similar to {@link #getString()}, but that will return
     * underlying (unmodifiable) character array that contains
     * textual value, instead of constructing a String object
     * to contain this information.
     * Note, however, that:
     *<ul>
     * <li>String contents are not guaranteed to start at
     *   index 0 (rather, call {@link #getStringOffset}) to
     *   know the actual offset
     *  </li>
     * <li>Length of string contents may be less than the
     *  length of returned buffer: call {@link #getStringLength}
     *  for actual length of returned content.
     *  </li>
     * </ul>
     *<p>
     * Note that caller <b>MUST NOT</b> modify the returned
     * character array in any way -- doing so may corrupt
     * current parser state and render parser instance useless.
     *<p>
     * The only reason to call this method (over {@link #getString()})
     * is to avoid construction of a String object (which
     * will make a copy of contents).
     *
     * @return Buffer that contains the current textual value (but not necessarily
     *    at offset 0, and not necessarily until the end of buffer)
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract char[] getStringCharacters() throws JacksonException;

    /**
     * Accessor used with {@link #getStringCharacters}, to know length
     * of String stored in returned buffer.
     *
     * @return Number of characters within buffer returned
     *   by {@link #getStringCharacters} that are part of
     *   textual content of the current token.
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract int getStringLength() throws JacksonException;

    /**
     * Accessor used with {@link #getStringCharacters}, to know offset
     * of the first text content character within buffer.
     *
     * @return Offset of the first character within buffer returned
     *   by {@link #getStringCharacters} that is part of
     *   textual content of the current token.
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract int getStringOffset() throws JacksonException;

    /**
     * Method that can be used to determine whether calling of
     * {@link #getStringCharacters} would be the most efficient
     * way to access String value for the event parser currently
     * points to (compared to {@link #getString()}).
     *
     * @return True if parser currently has character array that can
     *   be efficiently returned via {@link #getStringCharacters}; false
     *   means that it may or may not exist
     */
    public abstract boolean hasStringCharacters();

    /*
    /**********************************************************************
    /* Deprecated Public API String access methods (deprecated in 3.0)
    /**********************************************************************
     */
    
    /**
     * Deprecated alias for {@link #getString()}:
     * MAY be removed in a 3.x version past 3.0; only included to help initial migration.
     *
     * @deprecated since 3.0 use {@link #getString()} instead.
     */
    @Deprecated // since 3.0
    public String getText() throws JacksonException {
        return getString();
    }

    /**
     * Deprecated alias for {@link #getStringCharacters()}:
     * MAY be removed in a 3.x version past 3.0; only included to help initial migration.
     *
     * @deprecated since 3.0 use {@link #getStringCharacters()} instead.
     */
    @Deprecated // since 3.0
    public char[] getTextCharacters() throws JacksonException {
        return getStringCharacters();
    }

    /**
     * Deprecated alias for {@link #getStringLength()}:
     * MAY be removed in a 3.x version past 3.0; only included to help initial migration.
     *
     * @deprecated since 3.0 use {@link #getStringLength()} instead.
     */
    @Deprecated // since 3.0
    public int getTextLength() throws JacksonException {
        return getStringLength();
    }

    /**
     * Deprecated alias for {@link #getStringOffset()}:
     * MAY be removed in a 3.x version past 3.0; only included to help initial migration.
     *
     * @deprecated since 3.0 use {@link #getStringOffset()} instead.
     */
    @Deprecated // since 3.0
    public int getTextOffset() throws JacksonException {
        return getStringOffset();
    }

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
     *
     * @return Numeric value of the current token in its most optimal
     *   representation
     *
     * @throws InputCoercionException If the current token is not of numeric type
     */
    public abstract Number getNumberValue() throws InputCoercionException;

    /**
     * Method similar to {@link #getNumberValue} with the difference that
     * for floating-point numbers value returned may be {@link BigDecimal}
     * if the underlying format does not store floating-point numbers using
     * native representation: for example, textual formats represent numbers
     * as Strings (which are 10-based), and conversion to {@link java.lang.Double}
     * is potentially lossy operation.
     *<p>
     * Default implementation simply returns {@link #getNumberValue()}
     *
     * @return Numeric value of the current token using most accurate representation
     *
     * @throws InputCoercionException If the current token is not of numeric type
     */
    public abstract Number getNumberValueExact() throws InputCoercionException;

    /**
     * Method similar to {@link #getNumberValue} but that returns
     * <b>either</b> same {@link Number} value as {@link #getNumberValue()}
     * (if already decoded), <b>or</b> {@code String} representation of
     * as-of-yet undecoded number.
     * Typically textual formats allow deferred decoding from String, whereas
     * binary formats either decode numbers eagerly or have binary representation
     * from which to decode value to return.
     *<p>
     * Same constraints apply to calling this method as to {@link #getNumberValue()}:
     * current token must be either
     * {@link JsonToken#VALUE_NUMBER_INT} or
     * {@link JsonToken#VALUE_NUMBER_FLOAT};
     * otherwise an exception is thrown
     *<p>
     * Default implementation simply returns {@link #getNumberValue()}
     *
     * @return Either {@link Number} (for already decoded numbers) or
     *   {@link String} (for deferred decoding).
     *
     * @throws InputCoercionException If the current token is not of numeric type
     */
    public abstract Object getNumberValueDeferred() throws InputCoercionException;

    /**
     * If current token is of type
     * {@link JsonToken#VALUE_NUMBER_INT} or
     * {@link JsonToken#VALUE_NUMBER_FLOAT}, returns
     * one of {@link NumberType} constants; otherwise returns null.
     *<p>
     * NOTE: in Jackson 2.x, an exception was wrong if called for non-numeric token.
     *
     * @return Type of current number, if parser points to numeric token; {@code null} otherwise
     */
    public abstract NumberType getNumberType();

    /**
     * If current token is of type
     * {@link JsonToken#VALUE_NUMBER_FLOAT}, returns
     * one of {@link NumberTypeFP} constants; otherwise returns
     * {@link NumberTypeFP#UNKNOWN}.
     *
     * @return Type of current number, if parser points to numeric token; {@code null} otherwise
     */
    public abstract NumberTypeFP getNumberTypeFP();

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a value of Java byte primitive type.
     * Note that in addition to "natural" input range of {@code [-128, 127]},
     * this also allows "unsigned 8-bit byte" values {@code [128, 255]}:
     * but for this range value will be translated by truncation, leading
     * to sign change.
     *<p>
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the resulting integer value falls outside range of
     * {@code [-128, 255]},
     * a {@link InputCoercionException}
     * will be thrown to indicate numeric overflow/underflow.
     *
     * @return Current number value as {@code byte} (if numeric token within
     *   range of {@code [-128, 255]}); otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number OR numeric
     *    value exceeds allowed range
     */
    public abstract byte getByteValue() throws InputCoercionException;

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
     * Java short, a {@link InputCoercionException}
     * will be thrown to indicate numeric overflow/underflow.
     *
     * @return Current number value as {@code short} (if numeric token within
     *   Java 16-bit signed {@code short} range); otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number OR numeric
     *    value exceeds allowed range
     */
    public abstract short getShortValue() throws InputCoercionException;

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
     * Java {@code int}, a {@link InputCoercionException}
     * may be thrown to indicate numeric overflow/underflow.
     *
     * @return Current number value as {@code int} (if numeric token within
     *   Java 32-bit signed {@code int} range); otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number OR numeric
     *    value exceeds allowed range
     */
    public abstract int getIntValue() throws InputCoercionException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it can be expressed as a Java long primitive type.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDoubleValue}
     * and then casting to {@code int}; except for possible overflow/underflow
     * exception.
     *<p>
     * Note: if the token is an integer, but its value falls
     * outside of range of Java long, a {@link InputCoercionException}
     * may be thrown to indicate numeric overflow/underflow.
     *
     * @return Current number value as {@code long} (if numeric token within
     *   Java 32-bit signed {@code long} range); otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number OR numeric
     *    value exceeds allowed range
     */
    public abstract long getLongValue() throws InputCoercionException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_INT} and
     * it cannot be used as a Java long primitive type due to its
     * magnitude.
     * It can also be called for {@link JsonToken#VALUE_NUMBER_FLOAT};
     * if so, it is equivalent to calling {@link #getDecimalValue}
     * and then constructing a {@link BigInteger} from that value.
     *
     * @return Current number value as {@link BigInteger} (if numeric token);
     *     otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number
     */
    public abstract BigInteger getBigIntegerValue() throws InputCoercionException;

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
     * outside of range of Java float, a {@link InputCoercionException}
     * will be thrown to indicate numeric overflow/underflow.
     *
     * @return Current number value as {@code float} (if numeric token within
     *   Java {@code float} range); otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number OR numeric
     *    value exceeds allowed range
     */
    public abstract float getFloatValue() throws InputCoercionException;

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
     * outside of range of Java double, a {@link InputCoercionException}
     * will be thrown to indicate numeric overflow/underflow.
     *
     * @return Current number value as {@code double} (if numeric token within
     *   Java {@code double} range); otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number OR numeric
     *    value exceeds allowed range
     */
    public abstract double getDoubleValue() throws InputCoercionException;

    /**
     * Numeric accessor that can be called when the current
     * token is of type {@link JsonToken#VALUE_NUMBER_FLOAT} or
     * {@link JsonToken#VALUE_NUMBER_INT}. No under/overflow exceptions
     * are ever thrown.
     *
     * @return Current number value as {@link BigDecimal} (if numeric token);
     *   otherwise exception thrown
     *
     * @throws InputCoercionException If either token type is not a number
     */
    public abstract BigDecimal getDecimalValue() throws InputCoercionException;

    /*
    /**********************************************************************
    /* Public API, access to token information, other
    /**********************************************************************
     */

    /**
     * Convenience accessor that can be called when the current
     * token is {@link JsonToken#VALUE_TRUE} or
     * {@link JsonToken#VALUE_FALSE}, to return matching {@code boolean}
     * value.
     * If the current token is of some other type, {@link InputCoercionException}
     * will be thrown
     *
     * @return {@code True} if current token is {@code JsonToken.VALUE_TRUE},
     *   {@code false} if current token is {@code JsonToken.VALUE_FALSE};
     *   otherwise throws {@link InputCoercionException}
     *
     * @throws InputCoercionException if the current token is not of boolean type
     */
    public abstract boolean getBooleanValue() throws InputCoercionException;

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
     *
     * @return Embedded value (usually of "native" type supported by format)
     *   for the current token, if any; {@code null otherwise}
     */
    public abstract Object getEmbeddedObject();

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
     * It works similar to getting String value via {@link #getString()}
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
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public abstract byte[] getBinaryValue(Base64Variant bv) throws JacksonException;

    /**
     * Convenience alternative to {@link #getBinaryValue(Base64Variant)}
     * that defaults to using
     * {@link Base64Variants#getDefaultVariant} as the default encoding.
     *
     * @return Decoded binary data
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public byte[] getBinaryValue() throws JacksonException {
        return getBinaryValue(Base64Variants.getDefaultVariant());
    }

    /**
     * Method that can be used as an alternative to {@link #getBinaryValue()},
     * especially when value can be large. The main difference (beyond method
     * of returning content using {@link OutputStream} instead of as byte array)
     * is that content will NOT remain accessible after method returns: any content
     * processed will be consumed and is not buffered in any way. If caller needs
     * buffering, it has to implement it.
     *
     * @param out Output stream to use for passing decoded binary data
     *
     * @return Number of bytes that were decoded and written via {@link OutputStream}
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public int readBinaryValue(OutputStream out) throws JacksonException {
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
     *
     * @throws JacksonIOException for low-level read issues
     * @throws tools.jackson.core.exc.StreamReadException for decoding problems
     */
    public int readBinaryValue(Base64Variant bv, OutputStream out) throws JacksonException {
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
     * <b>boolean</b>.
     * JSON booleans map naturally; integer numbers other than 0 map to true, and
     * 0 maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     *<p>
     * If representation cannot be converted to a boolean value (including structured types
     * like Objects and Arrays),
     * default value of {@code false} will be returned; no exceptions are thrown.
     *
     * @return {@code boolean} value current token is converted to, if possible; or {@code false} if not
     */
    public boolean getValueAsBoolean() {
        return getValueAsBoolean(false);
    }

    /**
     * Method that will try to convert value of current token to a
     * <b>boolean</b>.
     * JSON booleans map naturally; integer numbers other than 0 map to true, and
     * 0 maps to false
     * and Strings 'true' and 'false' map to corresponding values.
     *<p>
     * If representation cannot be converted to a boolean value (including structured types
     * like Objects and Arrays),
     * specified <b>def</b> will be returned; no exceptions are thrown.
     *
     * @param def Default value to return if conversion to {@code boolean} is not possible
     *
     * @return {@code boolean} value current token is converted to, if possible; {@code def} otherwise
     */
    public abstract boolean getValueAsBoolean(boolean def);

    /**
     * Method that will try to convert value of current token to a
     * Java {@code int} value.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an int (including structured type
     * markers like start/end Object/Array)
     * default value of <b>0</b> will be returned; no exceptions are thrown.
     *
     * @return {@code int} value current token is converted to, if possible; default value otherwise
     *    otherwise
     *
     * @throws InputCoercionException If numeric value exceeds {@code int} range
     */
    public int getValueAsInt() throws InputCoercionException {
        return getValueAsInt(0);
    }

    /**
     * Method that will try to convert value of current token to a
     * {@code int}.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to an {@code int} (including structured type
     * markers like start/end Object/Array)
     * specified <b>def</b> will be returned; no exceptions are thrown.
     *
     * @param def Default value to return if conversion to {@code int} is not possible
     *
     * @return {@code int} value current token is converted to, if possible; {@code def} otherwise
     *
     * @throws InputCoercionException If numeric value exceeds {@code int} range
     */
    public int getValueAsInt(int def) throws InputCoercionException { return def; }

    /**
     * Method that will try to convert value of current token to a
     * {@code long}.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to a long (including structured type
     * markers like start/end Object/Array)
     * default value of <b>0L</b> will be returned; no exceptions are thrown.
     *
     * @return {@code long} value current token is converted to, if possible; default value otherwise
     *
     * @throws InputCoercionException If numeric value exceeds {@code long} range
     */
    public long getValueAsLong() throws InputCoercionException {
        return getValueAsLong(0);
    }

    /**
     * Method that will try to convert value of current token to a
     * {@code long}.
     * Numbers are coerced using default Java rules; booleans convert to 0 (false)
     * and 1 (true), and Strings are parsed using default Java language integer
     * parsing rules.
     *<p>
     * If representation cannot be converted to a long (including structured type
     * markers like start/end Object/Array)
     * specified <b>def</b> will be returned; no exceptions are thrown.
     *
     * @param def Default value to return if conversion to {@code long} is not possible
     *
     * @return {@code long} value current token is converted to, if possible; {@code def} otherwise
     *
     * @throws InputCoercionException If numeric value exceeds {@code long} range
     */
    public long getValueAsLong(long def) throws InputCoercionException {
        return def;
    }

    /**
     * Method that will try to convert value of current token to a Java
     * <b>double</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0.0 (false)
     * and 1.0 (true), and Strings are parsed using default Java language floating
     * point parsing rules.
     *<p>
     * If representation cannot be converted to a double (including structured types
     * like Objects and Arrays),
     * default value of <b>0.0</b> will be returned; no exceptions are thrown.
     *
     * @return {@code double} value current token is converted to, if possible;
     *    default value otherwise
     *
     * @throws InputCoercionException If numeric value exceeds {@code double} range
     */
    public double getValueAsDouble() throws InputCoercionException {
        return getValueAsDouble(0.0);
    }

    /**
     * Method that will try to convert value of current token to a
     * Java <b>double</b>.
     * Numbers are coerced using default Java rules; booleans convert to 0.0 (false)
     * and 1.0 (true), and Strings are parsed using default Java language floating
     * point parsing rules.
     *<p>
     * If representation cannot be converted to a double (including structured types
     * like Objects and Arrays),
     * specified <b>def</b> will be returned; no exceptions are thrown.
     *
     * @param def Default value to return if conversion to {@code double} is not possible
     *
     * @return {@code double} value current token is converted to, if possible; {@code def} otherwise
     *
     * @throws InputCoercionException If numeric value exceeds {@code double} range
     */
    public double getValueAsDouble(double def) throws InputCoercionException {
        return def;
    }

    /**
     * Method that will try to convert value of current token to a
     * {@link java.lang.String}.
     * JSON Strings map naturally; scalar values get converted to
     * their textual representation.
     * If representation cannot be converted to a String value (including structured types
     * like Objects and Arrays and {@code null} token), default value of
     * <b>null</b> will be returned; no exceptions are thrown.
     *
     * @return {@link String} value current token is converted to, if possible; {@code null} otherwise
     */
    public String getValueAsString() {
        return getValueAsString(null);
    }

    /**
     * Method that will try to convert value of current token to a
     * {@link java.lang.String}.
     * JSON Strings map naturally; scalar values get converted to
     * their textual representation.
     * If representation cannot be converted to a String value (including structured types
     * like Objects and Arrays and {@code null} token), specified default value
     * will be returned; no exceptions are thrown.
     *
     * @param def Default value to return if conversion to {@code String} is not possible
     *
     * @return {@link String} value current token is converted to, if possible; {@code def} otherwise
     */
    public abstract String getValueAsString(String def);

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
     * in case it cannot use native object ids.
     *
     * @return {@code True} if the format being read supports native Object Ids;
     *    {@code false} if not
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
     * in case it cannot use native type ids.
     *
     * @return {@code True} if the format being read supports native Type Ids;
     *    {@code false} if not
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
     *
     * @return Native Object id associated with the current token, if any; {@code null} if none
     */
    public Object getObjectId() { return null; }

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
     *
     * @return Native Type Id associated with the current token, if any; {@code null} if none
     */
    public Object getTypeId() { return null; }

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
     * cannot be introspected when using this method.
     *
     * @param <T> Nominal type parameter to specify expected node type to
     *    reduce need to cast result value
     * @param valueType Type to bind content to
     *
     * @return Java value read from content
     *
     * @throws JacksonException if there is either an underlying I/O problem or decoding
     *    issue at format layer
     */
    public abstract <T> T readValueAs(Class<T> valueType) throws JacksonException;

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
     *
     * @param <T> Nominal type parameter to specify expected node type to
     *    reduce need to cast result value
     * @param valueTypeRef Type to bind content to
     *
     * @return Java value read from content
     *
     * @throws JacksonException if there is either an underlying I/O problem or decoding
     *    issue at format layer
     */
    public abstract <T> T readValueAs(TypeReference<T> valueTypeRef) throws JacksonException;

    /**
     * Similar to {@link #readValueAs(Class)} but takes {@link ResolvedType}.
     */
    public abstract <T> T readValueAs(ResolvedType type) throws JacksonException;

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
     * @param <T> Nominal type parameter for result node type (to reduce need for casting)
     *
     * @return root of the document, or null if empty or whitespace.
     *
     * @throws JacksonException if there is either an underlying I/O problem or decoding
     *    issue at format layer
     */
    public abstract <T extends TreeNode> T readValueAsTree() throws JacksonException;

    /*
    /**********************************************************************
    /* Internal methods
    /**********************************************************************
     */

    /**
     * Helper method to call for operations that are not supported by
     * parser implementation.
     */
    protected void _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Operation not supported by parser of type "+getClass().getName());
    }

    /**
     * Helper method for constructing {@link StreamReadException}
     * based on current state of the parser
     *
     * @param msg Base exception message to construct exception with
     *
     * @return {@link StreamReadException} constructed
     */
    protected StreamReadException _constructReadException(String msg) {
        return new StreamReadException(this, msg);
    }

    protected StreamReadException _constructReadException(String msg, Object arg) {
        return _constructReadException(String.format(msg, arg));
    }

    protected StreamReadException _constructReadException(String msg, Object arg1, Object arg2) {
        return _constructReadException(String.format(msg, arg1, arg2));
    }

    protected StreamReadException _constructReadException(String msg,
            Object arg1, Object arg2, Object arg3) {
        return _constructReadException(String.format(msg, arg1, arg2, arg3));
    }

    /**
     * Helper method for constructing {@link StreamReadException}
     * based on current state of the parser and indicating that the given
     * {@link Throwable} is the root cause.
     *
     * @param msg Base exception message to construct exception with
     * @param t Root cause to assign
     *
     * @return Read exception (of type {@link StreamReadException}) constructed
     */
    protected StreamReadException _constructReadException(String msg, Throwable t) {
        return new StreamReadException(this, msg, currentLocation(), t);
    }

    /**
     * Helper method for constructing {@link StreamReadException}
     * based on current state of the parser, except for specified
     * {@link TokenStreamLocation} for problem location (which may not be
     * the exact current location)
     *
     * @param msg Base exception message to construct exception with
     * @param loc Error location to report
     *
     * @return Read exception (of type {@link StreamReadException}) constructed
     *
     * @since 2.13
     */
    protected StreamReadException _constructReadException(String msg, TokenStreamLocation loc) {
        return new StreamReadException(this, msg, loc);
    }
}
