package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.*;

/**
 * Token writer features specific to JSON backend.
 *
 * @since 2.10
 */
public enum JsonWriteFeature
    implements FormatFeature
{
    // // // Support for non-standard data format constructs: comments

    // // Quoting/escaping-related features

    /**
     * Feature that specifies that all characters beyond 7-bit ASCII
     * range (i.e. code points of 128 and above) need to be output
     * using format-specific escapes (for JSON, backslash escapes),
     * if format uses escaping mechanisms (which is generally true
     * for textual formats but not for binary formats).
     *<p>
     * Feature is disabled by default.
     */
    @SuppressWarnings("deprecation")
    ESCAPE_NON_ASCII(false, JsonGenerator.Feature.ESCAPE_NON_ASCII),

    /**
     * Feature that determines whether JSON Object field names are
     * quoted using double-quotes, as specified by JSON specification
     * or not. Ability to disable quoting was added to support use
     * cases where they are not usually expected, which most commonly
     * occurs when used straight from Javascript.
     *<p>
     * Feature is enabled by default (since it is required by JSON specification).
     */
    @SuppressWarnings("deprecation")
    QUOTE_FIELD_NAMES(true, JsonGenerator.Feature.QUOTE_FIELD_NAMES),

    /**
     * Feature that specifies that hex values are encoded with upper-case letters.
     *<p>
     * Can be disabled to have a better possibility to compare between other JSON
     * writer libraries, such as JSON.stringify from Javascript.
     *<p>
     * Feature is enabled by default for backwards compatibility with earlier
     * versions.
     *
     * @since 2.14
     */
    @SuppressWarnings("deprecation")
    WRITE_HEX_UPPER_CASE(true, JsonGenerator.Feature.WRITE_HEX_UPPER_CASE),

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
    @SuppressWarnings("deprecation")
    WRITE_NAN_AS_STRINGS(true, JsonGenerator.Feature.QUOTE_NON_NUMERIC_NUMBERS),

    /**
     * Feature that forces all regular number values to be written as JSON Strings,
     * instead of as JSON Numbers.
     * Default state is 'false', meaning that Java numbers are to
     * be serialized using basic numeric representation but
     * if enabled all such numeric values are instead written out as
     * JSON Strings instead.
     *<p>
     * One use case is to avoid problems with Javascript limitations:
     * since Javascript standard specifies that all number handling
     * should be done using 64-bit IEEE 754 floating point values,
     * result being that some 64-bit integer values cannot be
     * accurately represent (as mantissa is only 51 bit wide).
     *<p>
     * Feature is disabled by default.
     */
    @SuppressWarnings("deprecation")
    WRITE_NUMBERS_AS_STRINGS(false, JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS),

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

    /**
     * Feature that specifies whether {@link JsonGenerator} should escape forward slashes.
     * <p>
     * Feature is disabled by default for Jackson 2.x version, and enabled by default in Jackson 3.0.
     *
     * @since 2.17
     */
    ESCAPE_FORWARD_SLASHES(false, JsonGenerator.Feature.ESCAPE_FORWARD_SLASHES),

    /**
     * Feature that specifies how characters outside "Basic Multilingual Plane" (BMP) -- ones encoded
     * as 4-byte UTF-8 sequences but represented in JVM memory as 2 16-bit "surrogate" {@code chars} --
     * should be encoded as UTF-8 by {@link JsonGenerator}.
     * If enabled, surrogate pairs are combined and flushed as a
     * single, 4-byte UTF-8 character.
     * If disabled, each {@code char} of pair is written as 2 separate characters: that is, as 2
     * separate 3-byte UTF-8 characters with values in Surrogate character ranges
     * ({@code 0xD800} - {@code 0xDBFF} and {@code 0xDC00} - {@code 0xDFFF})
     * <p>
     * Note that this feature only has effect for {@link JsonGenerator}s that directly encode
     * {@code byte}-based output, as UTF-8 (target {@link java.io.OutputStream}, {@code byte[]}
     * and so on); it will not (cannot) change handling of
     * {@code char}-based output (like {@link java.io.Writer} or {@link java.lang.String}).
     * <p>
     * Feature is disabled by default in 2.x for backwards-compatibility (will be enabled
     * in 3.0).
     *
     * @since 2.18
     */
    COMBINE_UNICODE_SURROGATES_IN_UTF8(false, JsonGenerator.Feature.COMBINE_UNICODE_SURROGATES_IN_UTF8),

    ;

    private final boolean _defaultState;
    private final int _mask;

    /**
     * For backwards compatibility we may need to map to one of existing {@link JsonGenerator.Feature}s;
     * if so, this is the feature to enable/disable.
     */
    private final JsonGenerator.Feature _mappedFeature;

    /**
     * Method that calculates bit set (flags) of all features that
     * are enabled by default.
     *
     * @return Bit mask of all features that are enabled by default
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

    private JsonWriteFeature(boolean defaultState,
            JsonGenerator.Feature  mapTo) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
        _mappedFeature = mapTo;
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public int getMask() { return _mask; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }

    public JsonGenerator.Feature mappedFeature() { return _mappedFeature; }
}
