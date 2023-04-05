package tools.jackson.core.json;

import java.util.Arrays;

import tools.jackson.core.*;
import tools.jackson.core.base.ParserBase;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.NumberInput;
import tools.jackson.core.util.JacksonFeatureSet;

/**
 * Another intermediate base class, only used by actual JSON-backed parser
 * implementations.
 *
 * @since 3.0
 */
public abstract class JsonParserBase
    extends ParserBase
{
    private final static char[] NO_CHARS = new char[0];

    /*
    /**********************************************************************
    /* JSON-specific configuration
    /**********************************************************************
     */

    /**
     * Bit flag for {@link JsonReadFeature}s that are enabled.
     */
    protected int _formatReadFeatures;

    /*
    /**********************************************************************
    /* Parsing state
    /**********************************************************************
     */

    /**
     * Information about parser context, context in which
     * the next token is to be parsed (root, array, object).
     */
    protected JsonReadContext _streamReadContext;

    /**
     * Secondary token related to the next token after current one;
     * used if its type is known. This may be value token that
     * follows {@link JsonToken#PROPERTY_NAME}, for example.
     */
    protected JsonToken _nextToken;

    /*
    /**********************************************************************
    /* Helper buffer recycling
    /**********************************************************************
     */

    /**
     * Temporary buffer that is needed if an Object property name is accessed
     * using {@link #getTextCharacters} method (instead of String
     * returning alternatives)
     */
    private char[] _nameCopyBuffer = NO_CHARS;

    /**
     * Flag set to indicate whether the Object property name is available
     * from the name copy buffer or not (in addition to its String
     * representation  being available via read context)
     */
    protected boolean _nameCopied;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected JsonParserBase(ObjectReadContext readCtxt,
            IOContext ctxt, int streamReadFeatures, int formatReadFeatures) {
        super(readCtxt, ctxt, streamReadFeatures);
        _formatReadFeatures = formatReadFeatures;
        DupDetector dups = StreamReadFeature.STRICT_DUPLICATE_DETECTION.enabledIn(streamReadFeatures)
                ? DupDetector.rootDetector(this) : null;
        _streamReadContext = JsonReadContext.createRootContext(dups);
    }

    /*
    /**********************************************************************
    /* Versioned, capabilities, config
    /**********************************************************************
     */

    @Override public Version version() { return PackageVersion.VERSION; }

    @Override
    public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() {
        // For now, JSON settings do not differ from general defaults:
        return DEFAULT_READ_CAPABILITIES;
    }

    /*
    /**********************************************************************
    /* ParserBase method implementions/overrides
    /**********************************************************************
     */

    @Override public TokenStreamContext streamReadContext() { return _streamReadContext; }

    @Override
    public Object currentValue() {
        return _streamReadContext.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        _streamReadContext.assignCurrentValue(v);
    }

    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    @Override public String currentName() {
        // [JACKSON-395]: start markers require information from parent
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JsonReadContext parent = _streamReadContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _streamReadContext.currentName();
    }

    @Override
    public boolean hasTextCharacters() {
        if (_currToken == JsonToken.VALUE_STRING) { return true; } // usually true
        if (_currToken == JsonToken.PROPERTY_NAME) { return _nameCopied; }
        return false;
    }

    // 03-Nov-2019, tatu: Will not recycle "name copy buffer" any more as it seems
    //   unlikely to be of much real benefit
    /*
    @Override
    protected void _releaseBuffers() {
        super._releaseBuffers();
        char[] buf = _nameCopyBuffer;
        if (buf != null) {
            _nameCopyBuffer = null;
            _ioContext.releaseNameCopyBuffer(buf);
        }
    }
    */

    /*
    /**********************************************************************
    /* Internal/package methods: Context handling
    /**********************************************************************
     */

    protected void createChildArrayContext(final int lineNr, final int colNr) throws JacksonException {
        _streamReadContext = _streamReadContext.createChildArrayContext(lineNr, colNr);
        _streamReadConstraints.validateNestingDepth(_streamReadContext.getNestingDepth());
    }

    protected void createChildObjectContext(final int lineNr, final int colNr) throws JacksonException {
        _streamReadContext = _streamReadContext.createChildObjectContext(lineNr, colNr);
        _streamReadConstraints.validateNestingDepth(_streamReadContext.getNestingDepth());
    }

    /*
    /**********************************************************************
    /* Numeric parsing method implementations
    /**********************************************************************
     */

    @Override
    protected void _parseNumericValue(int expType)
        throws JacksonException, InputCoercionException
    {
        // Int or float?
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            int len = _intLength;
            // First: optimization for simple int
            if (len <= 9) {
                int i = _textBuffer.contentsAsInt(_numberNegative);
                _numberInt = i;
                _numTypesValid = NR_INT;
                return;
            }
            if (len <= 18) { // definitely fits AND is easy to parse using 2 int parse calls
                long l = _textBuffer.contentsAsLong(_numberNegative);
                // Might still fit in int, need to check
                if (len == 10) {
                    if (_numberNegative) {
                        if (l >= MIN_INT_L) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    } else {
                        if (l <= MAX_INT_L) {
                            _numberInt = (int) l;
                            _numTypesValid = NR_INT;
                            return;
                        }
                    }
                }
                _numberLong = l;
                _numTypesValid = NR_LONG;
                return;
            }
             // For [core#865]: handle remaining 19-char cases as well
            if (len == 19) {
                char[] buf = _textBuffer.getTextBuffer();
                int offset = _textBuffer.getTextOffset();
                if (_numberNegative) {
                    ++offset;
                }
                if (NumberInput.inLongRange(buf, offset, len, _numberNegative)) {
                    _numberLong = NumberInput.parseLong19(buf, offset, _numberNegative);
                    _numTypesValid = NR_LONG;
                    return;
                }
            }
            _parseSlowInt(expType);
            return;
        }
        if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
            _parseSlowFloat(expType);
            return;
        }
        throw _constructNotNumericType(_currToken, expType);
    }

    @Override
    protected int _parseIntValue() throws JacksonException
    {
        // Inlined variant of: _parseNumericValue(NR_INT)
        if (_currToken == JsonToken.VALUE_NUMBER_INT) {
            if (_intLength <= 9) {
                int i = _textBuffer.contentsAsInt(_numberNegative);
                _numberInt = i;
                _numTypesValid = NR_INT;
                return i;
            }
        }
        // if not optimizable, use more generic
        _parseNumericValue(NR_INT);
        if ((_numTypesValid & NR_INT) == 0) {
            convertNumberToInt();
        }
        return _numberInt;
    }

    private void _parseSlowFloat(int expType) throws JacksonException
    {
        /* Nope: floating point. Here we need to be careful to get
         * optimal parsing strategy: choice is between accurate but
         * slow (BigDecimal) and lossy but fast (Double). For now
         * let's only use BD when explicitly requested -- it can
         * still be constructed correctly at any point since we do
         * retain textual representation
         */
        if (expType == NR_BIGDECIMAL) {
            // 04-Dec-2022, tatu: Let's defer actual decoding until it is certain
            //    value is actually needed.
            _numberBigDecimal = null;
            _numberString = _textBuffer.contentsAsString();
            _numTypesValid = NR_BIGDECIMAL;
        } else if (expType == NR_FLOAT) {
            _numberFloat = 0.0f;
            _numberString = _textBuffer.contentsAsString();
            _numTypesValid = NR_FLOAT;
        } else {
            // Otherwise double has to do
            // 04-Dec-2022, tatu: We can get all kinds of values here, NR_DOUBLE
            //    but also NR_INT or even NR_UNKNOWN. Shouldn't we try further
            //    deferring some typing?
            _numberDouble = 0.0;
            _numberString = _textBuffer.contentsAsString();
            _numTypesValid = NR_DOUBLE;
        }
    }

    private void _parseSlowInt(int expType) throws JacksonException
    {
        final String numStr = _textBuffer.contentsAsString();
        // 16-Oct-2018, tatu: Need to catch "too big" early due to [jackson-core#488]
        if ((expType == NR_INT) || (expType == NR_LONG)) {
            _reportTooLongIntegral(expType, numStr);
        }
        if ((expType == NR_DOUBLE) || (expType == NR_FLOAT)) {
            _numberString = numStr;
            _numTypesValid = NR_DOUBLE;
        } else {
            // nope, need the heavy guns... (rare case) - since Jackson v2.14, BigInteger parsing is lazy
            _numberBigInt = null;
            _numberString = numStr;
            _numTypesValid = NR_BIGINT;
        }
    }

    protected void _reportTooLongIntegral(int expType, String rawNum) throws JacksonException
    {
        if (expType == NR_INT) {
            _reportOverflowInt(rawNum);
        }
        _reportOverflowLong(rawNum);
    }

    /*
    /**********************************************************************
    /* Internal/package methods: config access
    /**********************************************************************
     */

    public boolean isEnabled(JsonReadFeature f) { return f.enabledIn(_formatReadFeatures); }

    /*
    /**********************************************************************
    /* Internal/package methods: buffer handling
    /**********************************************************************
     */

    protected char[] currentNameInBuffer() {
        if (_nameCopied) {
            return _nameCopyBuffer;
        }
        final String name = _streamReadContext.currentName();
        final int nameLen = name.length();
        if (_nameCopyBuffer.length < nameLen) {
            _nameCopyBuffer = new char[Math.max(32, nameLen)];
        }
        name.getChars(0, nameLen, _nameCopyBuffer, 0);
        _nameCopied = true;
        return _nameCopyBuffer;
    }

    /*
    /**********************************************************************
    /* Internal/package methods: Error reporting
    /**********************************************************************
     */

    protected char _handleUnrecognizedCharacterEscape(char ch) throws StreamReadException {
        // It is possible we allow all kinds of non-standard escapes...
        if (isEnabled(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
            return ch;
        }
        // and if allowing single-quoted names, String values, single-quote needs to be escapable regardless
        if (ch == '\'' && isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES)) {
            return ch;
        }
        _reportError("Unrecognized character escape "+_getCharDesc(ch));
        return ch;
    }

    // Promoted from `ParserBase` in 3.0
    protected void _reportMismatchedEndMarker(int actCh, char expCh) throws StreamReadException {
        TokenStreamContext ctxt = streamReadContext();
        _reportError(String.format(
                "Unexpected close marker '%s': expected '%c' (for %s starting at %s)",
                (char) actCh, expCh, ctxt.typeDesc(), ctxt.startLocation(_contentReference())));
    }

    // Method called to report a problem with unquoted control character.
    // Note: it is possible to suppress some instances of
    // exception by enabling {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS}.
    protected void _throwUnquotedSpace(int i, String ctxtDesc) throws StreamReadException {
        // It is possible to allow unquoted control chars:
        if (!isEnabled(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS) || i > INT_SPACE) {
            char c = (char) i;
            String msg = "Illegal unquoted character ("+_getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
            _reportError(msg);
        }
    }

    // @return Description to use as "valid tokens" in an exception message about
    //    invalid (unrecognized) JSON token: called when parser finds something that
    //    looks like unquoted textual token
    protected String _validJsonTokenList() {
        return _validJsonValueList();
    }

    // @return Description to use as "valid JSON values" in an exception message about
    //   invalid (unrecognized) JSON value: called when parser finds something that
    //    does not look like a value or separator.
    protected String _validJsonValueList() {
        if (isEnabled(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)) {
            return "(JSON String, Number (or 'NaN'/'INF'/'+INF'), Array, Object or token 'null', 'true' or 'false')";
        }
        return "(JSON String, Number, Array, Object or token 'null', 'true' or 'false')";
    }

    /*
    /**********************************************************************
    /* Internal/package methods: Other
    /**********************************************************************
     */

    protected static int[] growArrayBy(int[] arr, int more)
    {
        if (arr == null) {
            return new int[more];
        }
        return Arrays.copyOf(arr, arr.length + more);
    }
}
