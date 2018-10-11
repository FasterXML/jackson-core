package com.fasterxml.jackson.core.base;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.PackageVersion;

/**
 * Another intermediate base class aimed at ONLY json-backed parser.
 *
 * @since 3.0
 */
public abstract class JsonParserBase
    extends ParserBase
{
    /**
     * Bit flag composed of bits that indicate which
     * {@link JsonReadFeature}s are enabled.
     */
    protected int _formatReadFeatures;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected JsonParserBase(ObjectReadContext readCtxt,
            IOContext ctxt, int streamReadFeatures, int formatReadFeatures) {
        super(readCtxt, ctxt, streamReadFeatures);
        _formatReadFeatures = formatReadFeatures;
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override public Version version() { return PackageVersion.VERSION; }

    public boolean isEnabled(JsonReadFeature f) { return f.enabledIn(_formatReadFeatures); }

    /*
    /**********************************************************
    /* Internal/package methods: Error reporting
    /**********************************************************
     */

    protected char _handleUnrecognizedCharacterEscape(char ch) throws JsonProcessingException {
        // as per [JACKSON-300]
        if (isEnabled(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)) {
            return ch;
        }
        // and [JACKSON-548]
        if (ch == '\'' && isEnabled(JsonReadFeature.ALLOW_SINGLE_QUOTES)) {
            return ch;
        }
        _reportError("Unrecognized character escape "+_getCharDesc(ch));
        return ch;
    }

    /**
     * Method called to report a problem with unquoted control character.
     * Note: it is possible to suppress some instances of
     * exception by enabling {@link Feature#ALLOW_UNQUOTED_CONTROL_CHARS}.
     */
    protected void _throwUnquotedSpace(int i, String ctxtDesc) throws JsonParseException {
        // JACKSON-208; possible to allow unquoted control chars:
        if (!isEnabled(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS) || i > INT_SPACE) {
            char c = (char) i;
            String msg = "Illegal unquoted character ("+_getCharDesc(c)+"): has to be escaped using backslash to be included in "+ctxtDesc;
            _reportError(msg);
        }
    }
}
