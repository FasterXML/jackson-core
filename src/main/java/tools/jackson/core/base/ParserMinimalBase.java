package tools.jackson.core.base;

import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.exc.UnexpectedEndOfInputException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.ByteArrayBuilder;
import tools.jackson.core.util.JacksonFeatureSet;
import tools.jackson.core.util.VersionUtil;

import static tools.jackson.core.JsonTokenId.*;

/**
 * Intermediate base class used by all Jackson {@link JsonParser}
 * implementations, but does not add any additional fields that depend
 * on particular method of obtaining input.
 *<p>
 * Note that 'minimal' here mostly refers to minimal number of fields
 * (size) and functionality that is specific to certain types
 * of parser implementations; but not necessarily to number of methods.
 */
public abstract class ParserMinimalBase extends JsonParser
{
    // Control chars:
    protected final static int INT_TAB = '\t';
    protected final static int INT_LF = '\n';
    protected final static int INT_CR = '\r';
    protected final static int INT_RS = 0x001E;
    protected final static int INT_SPACE = 0x0020;

    // Markup
    protected final static int INT_LBRACKET = '[';
    protected final static int INT_RBRACKET = ']';
    protected final static int INT_LCURLY = '{';
    protected final static int INT_RCURLY = '}';
    protected final static int INT_QUOTE = '"';
    protected final static int INT_APOS = '\'';
    protected final static int INT_BACKSLASH = '\\';
    protected final static int INT_SLASH = '/';
    protected final static int INT_ASTERISK = '*';
    protected final static int INT_COLON = ':';
    protected final static int INT_COMMA = ',';
    protected final static int INT_HASH = '#';

    // Number chars
    protected final static int INT_0 = '0';
    protected final static int INT_9 = '9';
    protected final static int INT_MINUS = '-';
    protected final static int INT_PLUS = '+';

    protected final static int INT_PERIOD = '.';
    protected final static int INT_e = 'e';
    protected final static int INT_E = 'E';

    protected final static char CHAR_NULL = '\0';

    protected final static byte[] NO_BYTES = new byte[0];

    protected final static int[] NO_INTS = new int[0];

    /*
    /**********************************************************************
    /* Constants and fields wrt number handling
    /**********************************************************************
     */

    protected final static int NR_UNKNOWN = 0;

    // First, integer types

    protected final static int NR_INT = 0x0001;
    protected final static int NR_LONG = 0x0002;
    protected final static int NR_BIGINT = 0x0004;

    // And then floating point types

    protected final static int NR_DOUBLE = 0x008;
    protected final static int NR_BIGDECIMAL = 0x0010;

    /**
     * NOTE! Not used by JSON implementation but used by many of binary codecs
     */
    protected final static int NR_FLOAT = 0x020;

    // Also, we need some numeric constants

    protected final static BigInteger BI_MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    protected final static BigInteger BI_MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);

    protected final static BigInteger BI_MIN_LONG = BigInteger.valueOf(Long.MIN_VALUE);
    protected final static BigInteger BI_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    protected final static BigDecimal BD_MIN_LONG = new BigDecimal(BI_MIN_LONG);
    protected final static BigDecimal BD_MAX_LONG = new BigDecimal(BI_MAX_LONG);

    protected final static BigDecimal BD_MIN_INT = new BigDecimal(BI_MIN_INT);
    protected final static BigDecimal BD_MAX_INT = new BigDecimal(BI_MAX_INT);

    protected final static int MIN_BYTE_I = (int) Byte.MIN_VALUE;
    // Allow range up to and including 255, to support signed AND unsigned bytes
    protected final static int MAX_BYTE_I = (int) 255;

    protected final static int MIN_SHORT_I = (int) Short.MIN_VALUE;
    protected final static int MAX_SHORT_I = (int) Short.MAX_VALUE;

    protected final static long MIN_INT_L = (long) Integer.MIN_VALUE;
    protected final static long MAX_INT_L = (long) Integer.MAX_VALUE;

    // These are not very accurate, but have to do... (for bounds checks)

    protected final static double MIN_LONG_D = (double) Long.MIN_VALUE;
    protected final static double MAX_LONG_D = (double) Long.MAX_VALUE;

    protected final static double MIN_INT_D = (double) Integer.MIN_VALUE;
    protected final static double MAX_INT_D = (double) Integer.MAX_VALUE;

    /*
    /**********************************************************************
    /* Misc other constants
    /**********************************************************************
     */

    protected final static int STREAM_READ_FEATURE_DEFAULTS = StreamReadFeature.collectDefaults();

    /*
    /**********************************************************************
    /* Minimal configuration state
    /**********************************************************************
     */

    /**
     * Bit flag composed of bits that indicate which
     * {@link tools.jackson.core.StreamReadFeature}s
     * are enabled.
     */
    protected final int _streamReadFeatures;

    /**
     * Constraints to use for this parser.
     */
    protected final StreamReadConstraints _streamReadConstraints;

    /*
    /**********************************************************************
    /* Minimal generally useful state
    /**********************************************************************
     */

    /**
     * Context object provided by higher level functionality like
     * databinding for two reasons: passing configuration information
     * during construction, and to allow calling of some databind
     * operations via parser instance.
     *
     * @since 3.0
     */
    protected final ObjectReadContext _objectReadContext;

    /**
     * I/O context for this reader. It handles buffer allocation
     * for the reader, including possible reuse/recycling.
     *
     * @since 3.0 (at this level)
     */
    protected final IOContext _ioContext;

    /**
     * Flag that indicates whether parser is closed or not. Gets
     * set when parser is either closed by explicit call
     * ({@link #close}) or when end-of-input is reached.
     */
    protected boolean _closed;
    
    /**
     * Last token retrieved via {@link #nextToken}, if any.
     * Null before the first call to <code>nextToken()</code>,
     * as well as if token has been explicitly cleared
     */
    protected JsonToken _currToken;

    /**
     * Last cleared token, if any: that is, value that was in
     * effect when {@link #clearCurrentToken} was called.
     */
    protected JsonToken _lastClearedToken;

    /**
     * Current count of tokens, if tracked (see {@link #_trackMaxTokenCount})
     */
    protected long _tokenCount;

    /**
     * Whether or not to track the token count due a {@link StreamReadConstraints} maxTokenCount > 0.
     */
    protected final boolean _trackMaxTokenCount;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    /**
     * Main constructor for sub-classes to use
     *
     * @param readCtxt Context for databinding
     * @param ioCtxt Context for I/O handling, buffering
     * @param streamReadFeatures Bit set of {@link StreamReadFeature}s.
     */
    protected ParserMinimalBase(ObjectReadContext readCtxt,
            IOContext ioCtxt, int streamReadFeatures)
    {
        super();
        _objectReadContext = readCtxt;
        _ioContext = ioCtxt;
        _streamReadFeatures = streamReadFeatures;
        _streamReadConstraints = ioCtxt.streamReadConstraints();
        _trackMaxTokenCount = _streamReadConstraints.hasMaxTokenCount();
    }

    /**
     * Alternate constructors for cases where there is no real {@link IOContext}
     * in use; typically for abstractions that operate over non-streaming/incremental
     * sources (such as jackson-databind {@code TokenBuffer}).
     * 
     * @param readCtxt Context for databinding
     */
    protected ParserMinimalBase(ObjectReadContext readCtxt) {
        super();
        _objectReadContext = readCtxt;
        _ioContext = null;
        _streamReadFeatures = readCtxt.getStreamReadFeatures(STREAM_READ_FEATURE_DEFAULTS);
        _streamReadConstraints = readCtxt.streamReadConstraints();
        _trackMaxTokenCount = _streamReadConstraints.hasMaxTokenCount();
    }
    
    /*
    /**********************************************************************
    /* Configuration overrides if any
    /**********************************************************************
     */

    // from base class:

    /*
    @Override
    public JsonParser enable(StreamReadFeature f) {
        _streamReadFeatures |= f.getMask();
        return this;
    }

    @Override
    public JsonParser disable(StreamReadFeature f) {
        _streamReadFeatures &= ~f.getMask();
        return this;
    }
    */

    @Override
    public boolean isEnabled(StreamReadFeature f) { return f.enabledIn(_streamReadFeatures); }

    /*
    /**********************************************************************
    /* Config access, capability introspection
    /**********************************************************************
     */

    @Override
    public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
        return DEFAULT_READ_CAPABILITIES;
    }

    @Override
    public int streamReadFeatures() {
        return _streamReadFeatures;
    }

    @Override
    public StreamReadConstraints streamReadConstraints() {
        return _streamReadConstraints;
    }

    /*
    /**********************************************************************
    /* JsonParser impl: open / close / release
    /**********************************************************************
     */

    // public JsonToken getCurrentToken()
    // public boolean hasCurrentToken()

    @Override
    public boolean isClosed() { return _closed; }

    @Override
    public void close() throws JacksonException
    {
        if (_closed) {
            return;
        }
        _closed = true;
        // 30-May-2025, tatu: was missing before 2.20
        if (StreamReadFeature.CLEAR_CURRENT_TOKEN_ON_CLOSE.enabledIn(_streamReadFeatures)) {
            _currToken = null;
        }

        // Ordering is important: needs to be done in following order:
        //
        // 1. Close actual input source that may use buffering
        // 2. Close (release) underlying buffers (to/via IOContext)
        // 3. Close IOContext which will then release recycler (if any)
        try {
            _closeInput();
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        } finally {
            // Do in finally block so occurs even if stream closing fails
            // with exception
            _releaseBuffers();
            if (_ioContext != null) { // is case for some "virtual" parsers
                _ioContext.close();
            }
        }
    }

    /**
     * Abstract method for sub-classes to implement; to be called by
     * {@link #close()} implementation here.
     *
     * @throws IOException from underlying input source if thrown
     */
    protected abstract void _closeInput() throws IOException;

    /**
     * Method called to release internal buffers owned by the base
     * reader. This is expected to be called after {@link #_closeInput}
     * since the buffers are expected not to be needed any longer.
     */
    protected abstract void _releaseBuffers();

    /*
    /**********************************************************************
    /* JsonParser impl: basic state access
    /**********************************************************************
     */

    // public abstract TokenStreamContext getParsingContext();

    // public abstract TokenStreamLocation currentTokenLocation();
    // public abstract TokenStreamLocation currentLocation();

    @Override
    public ObjectReadContext objectReadContext() {
        return _objectReadContext;
    }

    /**
     * Method sub-classes need to implement to check whether end-of-content is allowed
     * at the current decoding position: formats often want to verify the all
     * start/end token pairs match, for example.
     *
     * @throws JacksonException if end-of-content not allowed at current position.
     */
    protected abstract void _handleEOF() throws JacksonException;

    // public abstract String currentName();

    /*
    /**********************************************************************
    /* JsonParser impl: basic stream iteration
    /**********************************************************************
     */

    // public abstract JsonToken nextToken() throws JacksonException;

    @Override public void finishToken() throws JacksonException { ; /* nothing */ }

    @Override public JsonToken currentToken() { return _currToken; }
    @Override public int currentTokenId() {
        final JsonToken t = _currToken;
        return (t == null) ? JsonTokenId.ID_NO_TOKEN : t.id();
    }

    @Override public boolean hasCurrentToken() { return _currToken != null; }
    @Override public boolean hasTokenId(int id) {
        final JsonToken t = _currToken;
        if (t == null) {
            return (JsonTokenId.ID_NO_TOKEN == id);
        }
        return t.id() == id;
    }

    @Override
    public boolean hasToken(JsonToken t) {
        return (_currToken == t);
    }

    @Override
    public long currentTokenCount() {
        return _tokenCount;
    }

    @Override public boolean isExpectedStartArrayToken() { return _currToken == JsonToken.START_ARRAY; }
    @Override public boolean isExpectedStartObjectToken() { return _currToken == JsonToken.START_OBJECT; }
    @Override public boolean isExpectedNumberIntToken() { return _currToken == JsonToken.VALUE_NUMBER_INT; }

    @Override
    public JsonToken nextValue() throws JacksonException {
        // Implementation should be as trivial as follows; only needs to change if
        // we are to skip other tokens (for example, if comments were exposed as tokens)
        JsonToken t = nextToken();
        if (t == JsonToken.PROPERTY_NAME) {
            t = nextToken();
        }
        return t;
    }

    @Override
    public JsonParser skipChildren() throws JacksonException
    {
        if (_currToken != JsonToken.START_OBJECT
            && _currToken != JsonToken.START_ARRAY) {
            return this;
        }
        int open = 1;

        // Since proper matching of start/end markers is handled
        // by nextToken(), we'll just count nesting levels here
        while (true) {
            JsonToken t = nextToken();
            if (t == null) {
                _handleEOF();
                // given constraints, above should never return; however, FindBugs
                // doesn't know about it and complains... so let's add dummy break here
                return this;
            }
            if (t.isStructStart()) {
                ++open;
            } else if (t.isStructEnd()) {
                if (--open == 0) {
                    return this;
                }
                // 23-May-2018, tatu: [core#463] Need to consider non-blocking case...
            } else if (t == JsonToken.NOT_AVAILABLE) {
                // Nothing much we can do except to either return `null` (which seems wrong),
                // or, what we actually do, signal error
                _reportError("Not enough content available for `skipChildren()`: non-blocking parser? (%s)",
                            getClass().getName());
            }
        }
    }

    /*
    /**********************************************************************
    /* JsonParser impl: stream iteration, property names
    /**********************************************************************
     */

    @Override
    public String nextName() throws JacksonException {
        return (nextToken() == JsonToken.PROPERTY_NAME) ? currentName() : null;
    }

    @Override
    public boolean nextName(SerializableString str) throws JacksonException {
        return (nextToken() == JsonToken.PROPERTY_NAME) && str.getValue().equals(currentName());
    }

    // Base implementation that should work well for most implementations but that
    // is typically overridden for performance optimization purposes
    @Override
    public int nextNameMatch(PropertyNameMatcher matcher) throws JacksonException {
        String str = nextName();
        if (str != null) {
            return matcher.matchName(str);
        }
        if (_currToken == JsonToken.END_OBJECT) {
            return PropertyNameMatcher.MATCH_END_OBJECT;
        }
        return PropertyNameMatcher.MATCH_ODD_TOKEN;
    }

    @Override
    public int currentNameMatch(PropertyNameMatcher matcher) {
        if (_currToken == JsonToken.PROPERTY_NAME) {
            return matcher.matchName(currentName());
        }
        if (_currToken == JsonToken.END_OBJECT) {
            return PropertyNameMatcher.MATCH_END_OBJECT;
        }
        return PropertyNameMatcher.MATCH_ODD_TOKEN;
    }

    /*
    /**********************************************************************
    /* Public API, token state overrides
    /**********************************************************************
     */

    @Override public void clearCurrentToken() {
        if (_currToken != null) {
            _lastClearedToken = _currToken;
            _currToken = null;
        }
    }

    @Override public JsonToken getLastClearedToken() { return _lastClearedToken; }

//    @Override public abstract void overrideCurrentName(String name);

    /*
    /**********************************************************************
    /* Public API, access to token information, text
    /**********************************************************************
     */

//    @Override public abstract String getText();
//    @Override public abstract char[] getTextCharacters();
//    @Override public abstract boolean hasTextCharacters();
//    @Override public abstract int getTextLength();
//    @Override public abstract int getTextOffset();

    @Override
    public int getString(Writer writer) throws JacksonException
    {
        String str = getString();
        if (str == null) {
            return 0;
        }
        try {
            writer.write(str);
        } catch (IOException e) {
            throw _wrapIOFailure(e);
        }
        return str.length();
    }

    /*
    /**********************************************************************
    /* Public API, access to token information, numeric
    /**********************************************************************
     */

//    public abstract Number getNumberValue();
//    public abstract NumberType getNumberType();

    @Override
    public NumberTypeFP getNumberTypeFP() {
        return NumberTypeFP.UNKNOWN;
    }

    @Override
    public Number getNumberValueExact() throws InputCoercionException {
        return getNumberValue();
    }

    @Override
    public Object getNumberValueDeferred() throws InputCoercionException {
        return getNumberValue();
    }

    @Override
    public byte getByteValue() throws InputCoercionException {
        int value = getIntValue();
        // So far so good: but does it fit?
        // Let's actually allow range of [-128, 255] instead of just signed range of [-128, 127]
        // since "unsigned" usage quite common for bytes (but Java may use signed range, too)
        if (value < MIN_BYTE_I || value > MAX_BYTE_I) {
            _reportOverflowByte(getString(), currentToken());
        }
        return (byte) value;
    }

    @Override
    public short getShortValue() throws InputCoercionException
    {
        int value = getIntValue();
        if (value < MIN_SHORT_I || value > MAX_SHORT_I) {
            _reportOverflowShort(getString(), currentToken());
        }
        return (short) value;
    }

//    public abstract int getIntValue();
//    public abstract long getLongValue();

    /*
    /**********************************************************************
    /* Public API, access to token information, other
    /**********************************************************************
     */

    @Override
    public boolean getBooleanValue() throws InputCoercionException {
        JsonToken t = currentToken();
        if (t == JsonToken.VALUE_TRUE) return true;
        if (t == JsonToken.VALUE_FALSE) return false;

        throw _constructInputCoercion(String.format("Current token (%s) not of boolean type", t),
            t, Boolean.TYPE);
    }

    /*
    /**********************************************************************
    /* Public API, access to token information, binary
    /**********************************************************************
     */

//    public abstract byte[] getBinaryValue(Base64Variant b64variant);

    @Override
    public Object getEmbeddedObject() { return null; }

    /*
    /**********************************************************************
    /* Public API, access with conversion/coercion
    /**********************************************************************
     */

    @Override
    public boolean getValueAsBoolean(boolean defaultValue)
    {
        JsonToken t = _currToken;
        if (t != null) {
            switch (t.id()) {
            case ID_STRING:
                String str = getString().trim();
                if ("true".equals(str)) {
                    return true;
                }
                if ("false".equals(str)) {
                    return false;
                }
                if (_hasTextualNull(str)) {
                    return false;
                }
                break;
            case ID_NUMBER_INT:
                return getIntValue() != 0;
            case ID_TRUE:
                return true;
            case ID_FALSE:
            case ID_NULL:
                return false;
            case ID_EMBEDDED_OBJECT:
                Object value = getEmbeddedObject();
                if (value instanceof Boolean) {
                    return (Boolean) value;
                }
                break;
            default:
            }
        }
        return defaultValue;
    }


    @Override
    public int getValueAsInt()
    {
        JsonToken t = _currToken;
        if ((t == JsonToken.VALUE_NUMBER_INT) || (t == JsonToken.VALUE_NUMBER_FLOAT)) {
            return getIntValue();
        }
        return getValueAsInt(0);
    }

    @Override
    public int getValueAsInt(int defaultValue)
    {
        JsonToken t = _currToken;
        if ((t == JsonToken.VALUE_NUMBER_INT) || (t == JsonToken.VALUE_NUMBER_FLOAT)) {
            return getIntValue();
        }
        if (t != null) {
            switch (t.id()) {
            case ID_STRING:
                String str = getString();
                if (_hasTextualNull(str)) {
                    return 0;
                }
                return NumberInput.parseAsInt(str, defaultValue);
            case ID_TRUE:
                return 1;
            case ID_FALSE:
                return 0;
            case ID_NULL:
                return 0;
            case ID_EMBEDDED_OBJECT:
                Object value = getEmbeddedObject();
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
            }
        }
        return defaultValue;
    }

    @Override
    public long getValueAsLong()
    {
        JsonToken t = _currToken;
        if ((t == JsonToken.VALUE_NUMBER_INT) || (t == JsonToken.VALUE_NUMBER_FLOAT)) {
            return getLongValue();
        }
        return getValueAsLong(0L);
    }

    @Override
    public long getValueAsLong(long defaultValue)
    {
        JsonToken t = _currToken;
        if ((t == JsonToken.VALUE_NUMBER_INT) || (t == JsonToken.VALUE_NUMBER_FLOAT)) {
            return getLongValue();
        }
        if (t != null) {
            switch (t.id()) {
            case ID_STRING:
                String str = getString();
                if (_hasTextualNull(str)) {
                    return 0L;
                }
                return NumberInput.parseAsLong(str, defaultValue);
            case ID_TRUE:
                return 1L;
            case ID_FALSE:
            case ID_NULL:
                return 0L;
            case ID_EMBEDDED_OBJECT:
                Object value = getEmbeddedObject();
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
            }
        }
        return defaultValue;
    }

    @Override
    public double getValueAsDouble(double defaultValue)
    {
        JsonToken t = _currToken;
        if (t != null) {
            switch (t.id()) {
            case ID_STRING:
                String str = getString();
                if (_hasTextualNull(str)) {
                    return 0L;
                }
                return NumberInput.parseAsDouble(str, defaultValue,
                        isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            case ID_NUMBER_INT:
            case ID_NUMBER_FLOAT:
                return getDoubleValue();
            case ID_TRUE:
                return 1.0;
            case ID_FALSE:
            case ID_NULL:
                return 0.0;
            case ID_EMBEDDED_OBJECT:
                Object value = getEmbeddedObject();
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                }
            }
        }
        return defaultValue;
    }

    @Override
    public String getValueAsString() {
        // sub-classes tend to override so...
        return getValueAsString(null);
    }

    @Override
    public String getValueAsString(String defaultValue) {
        if (_currToken == JsonToken.VALUE_STRING) {
            return getString();
        }
        if (_currToken == JsonToken.PROPERTY_NAME) {
            return currentName();
        }
        if (_currToken == null || _currToken == JsonToken.VALUE_NULL || !_currToken.isScalarValue()) {
            return defaultValue;
        }
        return getString();
    }

    /*
    /**********************************************************************
    /* Databind callbacks
    /**********************************************************************
     */

    @Override
    public <T> T readValueAs(Class<T> valueType) throws JacksonException {
        return _objectReadContext.readValue(this, valueType);
    }

    @Override
    public <T> T readValueAs(TypeReference<T> valueTypeRef) throws JacksonException {
        return _objectReadContext.readValue(this, valueTypeRef);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValueAs(ResolvedType type) throws JacksonException {
        return (T) _objectReadContext.readValue(this, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TreeNode> T readValueAsTree() throws JacksonException {
        return (T) _objectReadContext.readTree(this);
    }

    /*
    /**********************************************************************
    /* Helper methods: Base64 decoding
    /**********************************************************************
     */

    /**
     * Helper method that can be used for base64 decoding in cases where
     * encoded content has already been read as a String.
     *
     * @param str String to decode
     * @param builder Builder used to buffer binary content decoded
     * @param b64variant Base64 variant expected in content
     *
     * @throws JacksonIOException for low-level read issues
     * @throws StreamReadException for decoding problems
     */
    protected void _decodeBase64(String str, ByteArrayBuilder builder, Base64Variant b64variant)
        throws JacksonException
    {
        try {
            b64variant.decode(str, builder);
        } catch (IllegalArgumentException e) {
            _reportError(e.getMessage());
        }
    }

    /*
    /**********************************************************************
    /* Coercion helper methods (overridable)
    /**********************************************************************
     */

    /**
     * Helper method used to determine whether we are currently pointing to
     * a String value of "null" (NOT a null token); and, if so, that parser
     * is to recognize and return it similar to if it was real null token.
     *<p>
     * Default implementation accepts exact string {@code "null"} and nothing else
     *
     * @param value String value to check
     *
     * @return True if given value contains "null equivalent" String value (for
     *   content this parser handles).
     */
    protected boolean _hasTextualNull(String value) { return "null".equals(value); }

    /*
    /**********************************************************************
    /* Error reporting, numeric conversion/parsing issues
    /**********************************************************************
     */

    /**
     * Method called to throw an exception for input token that looks like a number
     * based on first character(s), but is not valid according to rules of format.
     * In case of JSON this also includes invalid forms like positive sign and
     * leading zeroes.
     *
     * @param <T> Nominal type parameter for bogus return value
     * @param msg Base exception message to use
     *
     * @return Nothing: never returns; declared so caller can use fake return
     *   to keep compiler happy
     *
     * @throws StreamReadException Exception that describes problem with number validity
     */
    protected <T> T _reportInvalidNumber(String msg) throws StreamReadException {
        return _reportError("Invalid numeric value: "+msg);
    }

    protected void _reportOverflowByte(String numDesc, JsonToken inputType) throws InputCoercionException {
        throw _constructInputCoercion(String.format(
                "Numeric value (%s) out of range of `byte` (%d - %s)",
                _longIntegerDesc(numDesc), MIN_BYTE_I, MAX_BYTE_I),
                inputType, Byte.TYPE);
    }

    protected void _reportOverflowShort(String numDesc, JsonToken inputType) throws InputCoercionException {
        throw _constructInputCoercion(String.format(
                "Numeric value (%s) out of range of `short` (%d - %s)",
                _longIntegerDesc(numDesc), MIN_SHORT_I, MAX_SHORT_I),
                inputType, Short.TYPE);
    }

    /**
     * Method called to throw an exception for integral (not floating point) input
     * token with value outside of Java signed 32-bit range when requested as {@code int}.
     * Result will be {@link InputCoercionException} being thrown.
     *
     * @throws InputCoercionException Exception that describes problem with number range validity
     */
    protected void _reportOverflowInt() throws InputCoercionException {
        _reportOverflowInt(getString());
    }

    protected void _reportOverflowInt(String numDesc) throws InputCoercionException {
        _reportOverflowInt(numDesc, currentToken());
    }

    protected void _reportOverflowInt(String numDesc, JsonToken inputType) throws InputCoercionException {
        throw _constructInputCoercion(String.format(
                "Numeric value (%s) out of range of `int` (%d - %s)",
                _longIntegerDesc(numDesc), Integer.MIN_VALUE, Integer.MAX_VALUE),
                inputType, Integer.TYPE);
    }

    /**
     * Method called to throw an exception for integral (not floating point) input
     * token with value outside of Java signed 64-bit range when requested as {@code long}.
     * Result will be {@link InputCoercionException} being thrown.
     *
     * @throws InputCoercionException Exception that describes problem with number range validity
     */
    protected void _reportOverflowLong() throws InputCoercionException {
        _reportOverflowLong(getString());
    }

    protected void _reportOverflowLong(String numDesc) throws InputCoercionException {
        _reportOverflowLong(numDesc, currentToken());
    }

    protected void _reportOverflowLong(String numDesc, JsonToken inputType) throws InputCoercionException {
        throw _constructInputCoercion(String.format(
                "Numeric value (%s) out of range of `long` (%d - %s)",
                _longIntegerDesc(numDesc), Long.MIN_VALUE, Long.MAX_VALUE),
                inputType, Long.TYPE);
    }

    protected String _longIntegerDesc(String rawNum) {
        int rawLen = rawNum.length();
        if (rawLen < 1000) {
            return rawNum;
        }
        if (rawNum.startsWith("-")) {
            rawLen -= 1;
        }
        return String.format("[Integer with %d digits]", rawLen);
    }

    protected String _longNumberDesc(String rawNum) {
        int rawLen = rawNum.length();
        if (rawLen < 1000) {
            return rawNum;
        }
        if (rawNum.startsWith("-")) {
            rawLen -= 1;
        }
        return String.format("[number with %d characters]", rawLen);
    }

    /*
    /**********************************************************************
    /* Error reporting, EOF, unexpected chars/content
    /**********************************************************************
     */

    // @since 3.0
    protected <T> T _reportBadInputStream(int readLen) throws StreamReadException
    {
        // 12-Jan-2021, tatu: May need to think about this bit more but for now
        //    do double-wrapping
        throw _wrapIOFailure(new IOException(
"Bad input source: InputStream.read() returned 0 bytes when trying to read "+readLen+" bytes"));
    }

    // @since 3.0
    protected <T> T _reportBadReader(int readLen) throws StreamReadException
    {
        // 12-Jan-2021, tatu: May need to think about this bit more but for now
        //    do double-wrapping
        throw _wrapIOFailure(new IOException(
"Bad input source: Reader.read() returned 0 bytes when trying to read "+readLen+" bytes"));
    }

    protected <T> T _reportInvalidEOF() throws StreamReadException {
        return _reportInvalidEOF(" in "+_currToken, _currToken);
    }

    protected <T> T _reportInvalidEOFInValue(JsonToken type) throws StreamReadException {
        String msg;
        if (type == JsonToken.VALUE_STRING) {
            msg = " in a String value";
        } else if ((type == JsonToken.VALUE_NUMBER_INT)
                || (type == JsonToken.VALUE_NUMBER_FLOAT)) {
            msg = " in a Number value";
        } else {
            msg = " in a value";
        }
        return _reportInvalidEOF(msg, type);
    }

    protected <T> T _reportInvalidEOF(String msg, JsonToken currToken) throws StreamReadException {
        throw new UnexpectedEndOfInputException(this, currToken, "Unexpected end-of-input"+msg);
    }

    protected <T> T _reportInvalidSpace(int i) throws StreamReadException {
        char c = (char) i;
        String msg = "Illegal character ("+_getCharDesc(c)
            +"): only regular white space (\\r, \\n, \\t) is allowed between tokens";
        if (i == INT_RS) {
            msg += " (consider enabling `JsonReadFeature.ALLOW_RS_CONTROL_CHAR` to allow use of Record Separators (\\u001E))";
        }
        throw _constructReadException(msg, _currentLocationMinusOne());
    }

    protected <T> T _reportMissingRootWS(int ch) throws StreamReadException {
        return _reportUnexpectedChar(ch, "Expected space separating root-level values");
    }

    protected <T> T _reportUnexpectedChar(int ch, String comment) throws StreamReadException
    {
        if (ch < 0) { // sanity check
            _reportInvalidEOF();
        }
        String msg = String.format("Unexpected character (%s)", _getCharDesc(ch));
        if (comment != null) {
            msg += ": "+comment;
        }
        throw _constructReadException(msg, _currentLocationMinusOne());
    }

    protected <T> T _reportUnexpectedNumberChar(int ch, String comment) throws StreamReadException {
        String msg = String.format("Unexpected character (%s) in numeric value", _getCharDesc(ch));
        if (comment != null) {
            msg += ": "+comment;
        }
        throw _constructReadException(msg, _currentLocationMinusOne());
    }

    /**
     * Method called to throw an exception for invalid UTF-8 surrogate character: case
     * where a surrogate character (between U+D800 and U+DFFF) is decoded from UTF-8
     * bytes (but NOT from JSON entity!)
     *
     * @param ch Character code (int) that is invalid surrogate
     *
     * @throws StreamReadException Exception that describes problem with UTF-8 surrogate
     */
    protected void _reportInvalidUTF8Surrogate(int ch) throws StreamReadException {
        throw _constructReadException(
                "Invalid UTF-8: Illegal surrogate character 0x"+Integer.toHexString(ch));
    }

    /**
     * Factory method used to provide location for cases where we must read
     * and consume a single "wrong" character (to possibly allow error recovery),
     * but need to report accurate location for that character: if so, the
     * current location is past location we want, and location we want will be
     * "one location earlier".
     *<p>
     * Default implementation simply returns {@link #currentLocation()}
     *
     * @since 2.17
     *
     * @return Same as {@link #currentLocation()} except offset by -1
     */
    protected TokenStreamLocation _currentLocationMinusOne() {
        return currentLocation();
    }

    protected final static String _getCharDesc(int ch)
    {
        char c = (char) ch;
        if (Character.isISOControl(c)) {
            return "(CTRL-CHAR, code "+ch+")";
        }
        if (ch > 255) {
            return "'"+c+"' (code "+ch+" / 0x"+Integer.toHexString(ch)+")";
        }
        return "'"+c+"' (code "+ch+")";
    }

    /*
    /**********************************************************************
    /* Error reporting, input coercion support
    /**********************************************************************
     */

    // @since 3.0
    protected InputCoercionException _constructNotNumericType(JsonToken actualToken, int expNumericType)
    {
        final String msg = String.format(
"Current token (%s) not numeric, cannot use numeric value accessors", actualToken);

        Class<?> targetType;

        switch (expNumericType) {
        case NR_INT:
            targetType = Integer.TYPE;
            break;
        case NR_LONG:
            targetType = Long.TYPE;
            break;
        case NR_BIGINT:
            targetType = BigInteger.class;
            break;
        case NR_FLOAT:
            targetType = Float.TYPE;
            break;
        case NR_DOUBLE:
            targetType = Double.TYPE;
            break;
        case NR_BIGDECIMAL:
            targetType = BigDecimal.class;
            break;
        default:
            targetType = Number.class;
            break;
        }
        return _constructInputCoercion(msg, actualToken, targetType);
    }

    protected InputCoercionException _constructInputCoercion(String msg, JsonToken inputType, Class<?> targetType) {
        return new InputCoercionException(this, msg, inputType, targetType);
    }

    /*
    /**********************************************************************
    /* Error reporting, generic
    /**********************************************************************
     */

    protected <T> T _reportError(String msg) throws StreamReadException {
        throw _constructReadException(msg);
   }

    protected <T> T _reportError(String msg, Object arg) throws StreamReadException {
        throw _constructReadException(String.format(msg, arg));
    }

    protected <T> T _reportError(String msg, Object arg1, Object arg2) throws StreamReadException {
        throw _constructReadException(String.format(msg, arg1, arg2));
    }

    protected <T> T _reportError(String msg, Object arg1, Object arg2, Object arg3) throws StreamReadException {
        throw _constructReadException(String.format(msg, arg1, arg2, arg3));
    }

    // @since 3.0
    protected JacksonException _wrapIOFailure(IOException e) {
        return JacksonIOException.construct(e, this);
    }

    protected <T> T _throwInternal() {
        return VersionUtil.throwInternalReturnAny();
    }

    /*
    /**********************************************************
    /* Helper methods, other
    /**********************************************************
     */

    // for performance reasons, this method assumes that the input token is non-null
    protected final JsonToken _updateToken(final JsonToken token)
        throws StreamConstraintsException
    {
        _currToken = token;
        if (_trackMaxTokenCount) {
            _streamReadConstraints.validateTokenCount(++_tokenCount);
        }
        return token;
    }

    // only updates the token count if input token is non-null
    protected final JsonToken _nullSafeUpdateToken(final JsonToken token)
        throws StreamConstraintsException
    {
        _currToken = token;
        if (_trackMaxTokenCount && token != null) {
            _streamReadConstraints.validateTokenCount(++_tokenCount);
        }
        return token;
    }

    protected final JsonToken _updateTokenToNull() {
        return (_currToken = null);
    }

    protected final JsonToken _updateTokenToNA() {
        return (_currToken = JsonToken.NOT_AVAILABLE);
    }
}
