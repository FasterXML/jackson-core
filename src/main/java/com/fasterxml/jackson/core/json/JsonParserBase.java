package com.fasterxml.jackson.core.json;

import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.PackageVersion;

/**
 * Another intermediate base class aimed at ONLY json-backed parser.
 *
 * @since 3.0
 */
public abstract class JsonParserBase
    extends ParserBase
{
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
    protected JsonReadContext _parsingContext;

    /**
     * Secondary token related to the next token after current one;
     * used if its type is known. This may be value token that
     * follows FIELD_NAME, for example.
     */
    protected JsonToken _nextToken;

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
        _parsingContext = JsonReadContext.createRootContext(dups);
    }

    /*
    /**********************************************************************
    /* Versioned
    /**********************************************************************
     */

    @Override public Version version() { return PackageVersion.VERSION; }

    /*
    /**********************************************************************
    /* ParserBase method implementions
    /**********************************************************************
     */

    public boolean isEnabled(JsonReadFeature f) { return f.enabledIn(_formatReadFeatures); }

    @Override public TokenStreamContext getParsingContext() { return _parsingContext; }

    @Override
    public Object getCurrentValue() {
        return _parsingContext.getCurrentValue();
    }

    @Override
    public void setCurrentValue(Object v) {
        _parsingContext.setCurrentValue(v);
    }

    /**
     * Method that can be called to get the name associated with
     * the current event.
     */
    @Override public String currentName() throws IOException {
        // [JACKSON-395]: start markers require information from parent
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            JsonReadContext parent = _parsingContext.getParent();
            if (parent != null) {
                return parent.currentName();
            }
        }
        return _parsingContext.currentName();
    }
/*
    @Override public void overrideCurrentName(String name) throws IOException {
        // Simple, but need to look for START_OBJECT/ARRAY's "off-by-one" thing:
        JsonReadContext ctxt = _parsingContext;
        if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
            ctxt = ctxt.getParent();
        }
        ctxt.setCurrentName(name);
    }
    */

    /*
    /**********************************************************************
    /* Internal/package methods: Error reporting
    /**********************************************************************
     */

    protected char _handleUnrecognizedCharacterEscape(char ch) throws JsonProcessingException {
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
    protected void _reportMismatchedEndMarker(int actCh, char expCh) throws JsonParseException {
        TokenStreamContext ctxt = getParsingContext();
        _reportError(String.format(
                "Unexpected close marker '%s': expected '%c' (for %s starting at %s)",
                (char) actCh, expCh, ctxt.typeDesc(), ctxt.getStartLocation(_getSourceReference())));
    }

    /**
     * Method called to report a problem with unquoted control character.
     * Note: it is possible to suppress some instances of
     * exception by enabling {@link JsonReadFeature#ALLOW_UNESCAPED_CONTROL_CHARS}.
     */
    protected void _throwUnquotedSpace(int i, String ctxtDesc) throws JsonParseException {
        // It is possible to allow unquoted control chars:
        if (!isEnabled(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS) || i > INT_SPACE) {
            char c = (char) i;
            String msg = "Illegal unquoted character ("+_getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
            _reportError(msg);
        }
    }

    /**
     * @return Description to use as "valid tokens" in an exception message about
     *    invalid (unrecognized) JSON token: called when parser finds something that
     *    looks like unquoted textual token
     *
     * @since 2.10
     */
    protected String _validJsonTokenList() throws IOException {
        return _validJsonValueList();
    }

    /**
     * @return Description to use as "valid JSON values" in an exception message about
     *    invalid (unrecognized) JSON value: called when parser finds something that
     *    does not look like a value or separator.
     *
     * @since 2.10
     */
    protected String _validJsonValueList() throws IOException {
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
