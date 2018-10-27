package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

/**
 * Token writer features specific to JSON backend.
 */
public enum JsonWriteFeature
    implements FormatFeature
{
    // // // Support for non-standard JSON constructs: quoting

    /**
     * Feature that determines whether JSON Object field names are
     * quoted using double-quotes, as specified by JSON specification
     * or not. Ability to disable quoting was added to support use
     * cases where they are not usually expected, which most commonly
     * occurs when used straight from Javascript.
     *<p>
     * Feature is enabled by default (since it is required by JSON specification).
     */
    QUOTE_FIELD_NAMES(true),

    /**
     * Feature that determines whether "NaN" ("not a number", that is, not
     * real number) float/double values are output as JSON strings.
     * The values checked are Double.Nan,
     * Double.POSITIVE_INFINITY and Double.NEGATIVE_INIFINTY (and 
     * associated Float values).
     * If feature is disabled, these numbers are still output using
     * associated literal values, resulting in non-conforming
     * output.
     *<p>
     * Feature is enabled by default.
     */
    WRITE_NAN_AS_STRINGS(true),

    // // // Support for escaping variations
    
    /**
     * Feature that specifies that all characters beyond 7-bit ASCII
     * range (i.e. code points of 128 and above) need to be output
     * using format-specific escapes (for JSON, backslash escapes),
     * if format uses escaping mechanisms (which is generally true
     * for textual formats but not for binary formats).
     *<p>
     * Note that this setting may not necessarily make sense for all
     * data formats (for example, binary formats typically do not use
     * any escaping mechanisms; and some textual formats do not have
     * general-purpose escaping); if so, settings is simply ignored.
     * Put another way, effects of this feature are data-format specific.
     *<p>
     * Feature is disabled by default.
     */
    ESCAPE_NON_ASCII(false),

//23-Nov-2015, tatu: for [core#223], if and when it gets implemented
    /*
     * Feature that specifies handling of UTF-8 content that contains
     * characters beyond BMP (Basic Multilingual Plane), which are
     * represented in UCS-2 (Java internal character encoding) as two
     * "surrogate" characters. If feature is enabled, these surrogate
     * pairs are separately escaped using backslash escapes; if disabled,
     * native output (4-byte UTF-8 sequence, or, with char-backed output
     * targets, writing of surrogates as is which is typically converted
     * by {@link java.io.Writer} into 4-byte UTF-8 sequence eventually)
     * is used.
     *<p>
     * Note that the original JSON specification suggests use of escaping;
     * but that this is not correct from standard UTF-8 handling perspective.
     * Because of two competing goals, this feature was added to allow either
     * behavior to be used, but defaulting to UTF-8 specification compliant
     * mode.
     *<p>
     * Feature is disabled by default.
     */
//    ESCAPE_UTF8_SURROGATES(false, JsonGenerator.Feature.ESCAPE_UTF8_SURROGATES),

    ;

    final private boolean _defaultState;
    final private int _mask;

    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     */
    public static int collectDefaults()
    {
        int flags = 0;
        for (JsonWriteFeature f : values()) {
            if (f.enabledByDefault()) {
                flags |= f.getMask();
            }
        }
        return flags;
    }
    
    private JsonWriteFeature(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public int getMask() { return _mask; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
}
