/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core;

import static com.fasterxml.jackson.core.JsonTokenId.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * Base class that defines public API for writing JSON content.
 * Instances are created using factory methods of
 * a {@link JsonFactory} instance.
 *
 * @author Tatu Saloranta
 */
public abstract class JsonGenerator
    implements Closeable, Flushable, Versioned
{
    /**
     * Enumeration that defines all togglable features for generators.
     */
    public enum Feature {
        // // Low-level I/O / content features
        
        /**
         * Feature that determines whether generator will automatically
         * close underlying output target that is NOT owned by the
         * generator.
         * If disabled, calling application has to separately
         * close the underlying {@link OutputStream} and {@link Writer}
         * instances used to create the generator. If enabled, generator
         * will handle closing, as long as generator itself gets closed:
         * this happens when end-of-input is encountered, or generator
         * is closed by a call to {@link JsonGenerator#close}.
         *<p>
         * Feature is enabled by default.
         */
        AUTO_CLOSE_TARGET(true),

        /**
         * Feature that determines what happens when the generator is
         * closed while there are still unmatched
         * {@link JsonToken#START_ARRAY} or {@link JsonToken#START_OBJECT}
         * entries in output content. If enabled, such Array(s) and/or
         * Object(s) are automatically closed; if disabled, nothing
         * specific is done.
         *<p>
         * Feature is enabled by default.
         */
        AUTO_CLOSE_JSON_CONTENT(true),

        /**
         * Feature that specifies that calls to {@link #flush} will cause
         * matching <code>flush()</code> to underlying {@link OutputStream}
         * or {@link Writer}; if disabled this will not be done.
         * Main reason to disable this feature is to prevent flushing at
         * generator level, if it is not possible to prevent method being
         * called by other code (like <code>ObjectMapper</code> or third
         * party libraries).
         *<p>
         * Feature is enabled by default.
         */
        FLUSH_PASSED_TO_STREAM(true),

        // // Quoting-related features
        
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
         * Feature that determines whether "exceptional" (not real number)
         * float/double values are output as quoted strings.
         * The values checked are Double.Nan,
         * Double.POSITIVE_INFINITY and Double.NEGATIVE_INIFINTY (and 
         * associated Float values).
         * If feature is disabled, these numbers are still output using
         * associated literal values, resulting in non-conformant
         * output.
         *<p>
         * Feature is enabled by default.
         */
        QUOTE_NON_NUMERIC_NUMBERS(true),

        /**
         * Feature that forces all Java numbers to be written as JSON strings.
         * Default state is 'false', meaning that Java numbers are to
         * be serialized using basic numeric serialization (as JSON
         * numbers, integral or floating point). If enabled, all such
         * numeric values are instead written out as JSON Strings.
         *<p>
         * One use case is to avoid problems with Javascript limitations:
         * since Javascript standard specifies that all number handling
         * should be done using 64-bit IEEE 754 floating point values,
         * result being that some 64-bit integer values can not be
         * accurately represent (as mantissa is only 51 bit wide).
         *<p>
         * Feature is disabled by default.
         */
        WRITE_NUMBERS_AS_STRINGS(false),

        /**
         * Feature that determines whether {@link java.math.BigDecimal} entries are
         * serialized using {@link java.math.BigDecimal#toPlainString()} to prevent
         * values to be written using scientific notation.
         *<p>
         * Feature is disabled by default, so default output mode is used; this generally
         * depends on how {@link BigDecimal} has been created.
         * 
         * @since 2.3
         */
        WRITE_BIGDECIMAL_AS_PLAIN(false),
        
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

// 23-Nov-2015, tatu: for [core#223], if and when it gets implemented
        /**
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
         *
         * @since Xxx
         */
//        ESCAPE_UTF8_SURROGATES(false),
        
        // // Schema/Validity support features

        /**
         * Feature that determines whether {@link JsonGenerator} will explicitly
         * check that no duplicate JSON Object field names are written.
         * If enabled, generator will check all names within context and report
         * duplicates by throwing a {@link JsonGenerationException}; if disabled,
         * no such checking will be done. Assumption in latter case is
         * that caller takes care of not trying to write duplicate names.
         *<p>
         * Note that enabling this feature will incur performance overhead
         * due to having to store and check additional information.
         *<p>
         * Feature is disabled by default.
         * 
         * @since 2.3
         */
        STRICT_DUPLICATE_DETECTION(false),
        
        /**
         * Feature that determines what to do if the underlying data format requires knowledge
         * of all properties to output, and if no definition is found for a property that
         * caller tries to write. If enabled, such properties will be quietly ignored;
         * if disabled, a {@link JsonProcessingException} will be thrown to indicate the
         * problem.
         * Typically most textual data formats do NOT require schema information (although
         * some do, such as CSV), whereas many binary data formats do require definitions
         * (such as Avro, protobuf), although not all (Smile, CBOR, BSON and MessagePack do not).
         *<p>
         * Note that support for this feature is implemented by individual data format
         * module, if (and only if) it makes sense for the format in question. For JSON,
         * for example, this feature has no effect as properties need not be pre-defined.
         *<p>
         * Feature is disabled by default, meaning that if the underlying data format
         * requires knowledge of all properties to output, attempts to write an unknown
         * property will result in a {@link JsonProcessingException}
         *
         * @since 2.5
         */
        IGNORE_UNKNOWN(false),
        ;

        private final boolean _defaultState;
        private final int _mask;
        
        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
            _mask = (1 << ordinal());
        }

        public boolean enabledByDefault() { return _defaultState; }

        /**
         * @since 2.3
         */
        public boolean enabledIn(int flags) { return (flags & _mask) != 0; }

        public int getMask() { return _mask; }
    }

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Object that handles pretty-printing (usually additional
     * white space to make results more human-readable) during
     * output. If null, no pretty-printing is done.
     */
    protected PrettyPrinter _cfgPrettyPrinter;

    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */
    
    protected JsonGenerator() { }

    /**
     * Method that can be called to set or reset the object to
     * use for writing Java objects as JsonContent
     * (using method {@link #writeObject}).
     *
     * @return Generator itself (this), to allow chaining
     */
    public abstract JsonGenerator setCodec(ObjectCodec oc);

    /**
     * Method for accessing the object used for writing Java
     * object as JSON content
     * (using method {@link #writeObject}).
     */
    public abstract ObjectCodec getCodec();

    /**
     * Accessor for finding out version of the bundle that provided this generator instance.
     */
    @Override
    public abstract Version version();

    /*
    /**********************************************************
    /* Forward-compatibility additions in 2.7.5: placeholders
    /* for additions that will be in 2.8.0
    /**********************************************************
     */

    // @since 2.7.5 (as placeholder, NOT full impl)
    public boolean canWriteFormattedNumbers() { return false; }

    // @since 2.7.5: default impl that should work fine
    public void writeStartObject(Object forValue) throws IOException
    {
        writeStartObject();
        setCurrentValue(forValue);
    }

    // @since 2.7.5: default impl that should work fine
    public void writeArray(int[] array, int offset, int length) throws IOException
    {
        writeStartArray();
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
    }

    // @since 2.7.5: default impl that should work fine
    public void writeArray(long[] array, int offset, int length) throws IOException
    {
        writeStartArray();
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
    }

    // @since 2.7.5: default impl that should work fine
    public void writeArray(double[] array, int offset, int length) throws IOException
    {
        writeStartArray();
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
    }

    /*
    /**********************************************************
    /* Public API, Feature configuration
    /**********************************************************
     */

    /**
     * Method for enabling specified parser features:
     * check {@link Feature} for list of available features.
     *
     * @return Generator itself (this), to allow chaining
     */
    public abstract JsonGenerator enable(Feature f);

    /**
     * Method for disabling specified  features
     * (check {@link Feature} for list of features)
     *
     * @return Generator itself (this), to allow chaining
     */
    public abstract JsonGenerator disable(Feature f);

    /**
     * Method for enabling or disabling specified feature:
     * check {@link Feature} for list of available features.
     *
     * @return Generator itself (this), to allow chaining
     */
    public final JsonGenerator configure(Feature f, boolean state) {
        if (state) enable(f); else disable(f);
        return this;
    }

    /**
     * Method for checking whether given feature is enabled.
     * Check {@link Feature} for list of available features.
     */
    public abstract boolean isEnabled(Feature f);

    /**
     * Bulk access method for getting state of all standard (non-dataformat-specific)
     * {@link JsonGenerator.Feature}s.
     * 
     * @return Bit mask that defines current states of all standard {@link JsonGenerator.Feature}s.
     * 
     * @since 2.3
     */
    public abstract int getFeatureMask();

    /**
     * Bulk set method for (re)setting states of all standard {@link Feature}s
     * 
     * @since 2.3
     * 
     * @param values Bitmask that defines which {@link Feature}s are enabled
     *    and which disabled
     *
     * @return This parser object, to allow chaining of calls
     *
     * @deprecated Since 2.7, use {@link #overrideStdFeatures(int, int)} instead
     */
    @Deprecated
    public abstract JsonGenerator setFeatureMask(int values);

    /**
     * Bulk set method for (re)setting states of features specified by <code>mask</code>.
     * Functionally equivalent to
     *<code>
     *    int oldState = getFeatureMask();
     *    int newState = (oldState &amp; ~mask) | (values &amp; mask);
     *    setFeatureMask(newState);
     *</code>
     * but preferred as this lets caller more efficiently specify actual changes made.
     * 
     * @param values Bit mask of set/clear state for features to change
     * @param mask Bit mask of features to change
     * 
     * @since 2.6
     */
    public JsonGenerator overrideStdFeatures(int values, int mask) {
        int oldState = getFeatureMask();
        int newState = (oldState & ~mask) | (values & mask);
        return setFeatureMask(newState);
    }

    /**
     * Bulk access method for getting state of all {@link FormatFeature}s, format-specific
     * on/off configuration settings.
     * 
     * @return Bit mask that defines current states of all standard {@link FormatFeature}s.
     * 
     * @since 2.6
     */
    public int getFormatFeatures() {
        return 0;
    }
    
    /**
     * Bulk set method for (re)setting states of {@link FormatFeature}s,
     * by specifying values (set / clear) along with a mask, to determine
     * which features to change, if any.
     *<p>
     * Default implementation will simply throw an exception to indicate that
     * the generator implementation does not support any {@link FormatFeature}s.
     * 
     * @param values Bit mask of set/clear state for features to change
     * @param mask Bit mask of features to change
     * 
     * @since 2.6
     */
    public JsonGenerator overrideFormatFeatures(int values, int mask) {
        throw new IllegalArgumentException("No FormatFeatures defined for generator of type "+getClass().getName());
        /*
        int oldState = getFeatureMask();
        int newState = (oldState & ~mask) | (values & mask);
        return setFeatureMask(newState);
        */
    }
    
    /*
    /**********************************************************
    /* Public API, Schema configuration
    /**********************************************************
     */

    /**
     * Method to call to make this generator use specified schema.
     * Method must be called before generating any content, right after instance
     * has been created.
     * Note that not all generators support schemas; and those that do usually only
     * accept specific types of schemas: ones defined for data format this generator
     * produces.
     *<p>
     * If generator does not support specified schema, {@link UnsupportedOperationException}
     * is thrown.
     * 
     * @param schema Schema to use
     * 
     * @throws UnsupportedOperationException if generator does not support schema
     */
    public void setSchema(FormatSchema schema) {
        throw new UnsupportedOperationException("Generator of type "+getClass().getName()+" does not support schema of type '"
                +schema.getSchemaType()+"'");
    }

    /**
     * Method for accessing Schema that this parser uses, if any.
     * Default implementation returns null.
     *
     * @since 2.1
     */
    public FormatSchema getSchema() { return null; }

    /*
    /**********************************************************
    /* Public API, other configuration
    /**********************************************************
      */

    /**
     * Method for setting a custom pretty printer, which is usually
     * used to add indentation for improved human readability.
     * By default, generator does not do pretty printing.
     *<p>
     * To use the default pretty printer that comes with core
     * Jackson distribution, call {@link #useDefaultPrettyPrinter}
     * instead.
     *
     * @return Generator itself (this), to allow chaining
     */
    public JsonGenerator setPrettyPrinter(PrettyPrinter pp) {
        _cfgPrettyPrinter = pp;
        return this;
    }

    /**
     * Accessor for checking whether this generator has a configured
     * {@link PrettyPrinter}; returns it if so, null if none configured.
     * 
     * @since 2.1
     */
    public PrettyPrinter getPrettyPrinter() {
        return _cfgPrettyPrinter;
    }
    
    /**
     * Convenience method for enabling pretty-printing using
     * the default pretty printer
     * ({@link com.fasterxml.jackson.core.util.DefaultPrettyPrinter}).
     *
     * @return Generator itself (this), to allow chaining
     */
    public abstract JsonGenerator useDefaultPrettyPrinter();

    /**
     * Method that can be called to request that generator escapes
     * all character codes above specified code point (if positive value);
     * or, to not escape any characters except for ones that must be
     * escaped for the data format (if -1).
     * To force escaping of all non-ASCII characters, for example,
     * this method would be called with value of 127.
     *<p>
     * Note that generators are NOT required to support setting of value
     * higher than 127, because there are other ways to affect quoting
     * (or lack thereof) of character codes between 0 and 127.
     * Not all generators support concept of escaping, either; if so,
     * calling this method will have no effect.
     *<p>
     * Default implementation does nothing; sub-classes need to redefine
     * it according to rules of supported data format.
     * 
     * @param charCode Either -1 to indicate that no additional escaping
     *   is to be done; or highest code point not to escape (meaning higher
     *   ones will be), if positive value.
     */
    public JsonGenerator setHighestNonEscapedChar(int charCode) { return this; }

    /**
     * Accessor method for testing what is the highest unescaped character
     * configured for this generator. This may be either positive value
     * (when escaping configuration has been set and is in effect), or
     * 0 to indicate that no additional escaping is in effect.
     * Some generators may not support additional escaping: for example,
     * generators for binary formats that do not use escaping should
     * simply return 0.
     * 
     * @return Currently active limitation for highest non-escaped character,
     *   if defined; or -1 to indicate no additional escaping is performed.
     */
    public int getHighestEscapedChar() { return 0; }

    /**
     * Method for accessing custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     */
    public CharacterEscapes getCharacterEscapes() { return null; }

    /**
     * Method for defining custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     *<p>
     * Default implementation does nothing and simply returns this instance.
     */
    public JsonGenerator setCharacterEscapes(CharacterEscapes esc) { return this; }

    /**
     * Method that allows overriding String used for separating root-level
     * JSON values (default is single space character)
     *<p>
     * Default implementation throws {@link UnsupportedOperationException}.
     * 
     * @param sep Separator to use, if any; null means that no separator is
     *   automatically added
     * 
     * @since 2.1
     */
    public JsonGenerator setRootValueSeparator(SerializableString sep) {
        throw new UnsupportedOperationException();
    }

    /*
    /**********************************************************
    /* Public API, output state access
    /**********************************************************
     */
    
    /**
     * Method that can be used to get access to object that is used
     * as target for generated output; this is usually either
     * {@link OutputStream} or {@link Writer}, depending on what
     * generator was constructed with.
     * Note that returned value may be null in some cases; including
     * case where implementation does not want to exposed raw
     * source to caller.
     * In cases where output has been decorated, object returned here
     * is the decorated version; this allows some level of interaction
     * between users of generator and decorator object.
     *<p>
     * In general use of this accessor should be considered as
     * "last effort", i.e. only used if no other mechanism is applicable.
     */
    public Object getOutputTarget() {
        return null;
    }

    /**
     * Method for verifying amount of content that is buffered by generator
     * but not yet flushed to the underlying target (stream, writer),
     * in units (byte, char) that the generator implementation uses for buffering;
     * or -1 if this information is not available.
     * Unit used is often the same as the unit of underlying target (that is,
     * `byte` for {@link java.io.OutputStream}, `char` for {@link java.io.Writer}),
     * but may differ if buffering is done before encoding.
     * Default JSON-backed implementations do use matching units.
     *<p>
     * Note: non-JSON implementations will be retrofitted for 2.6 and beyond;
     * please report if you see -1 (missing override)
     *
     * @return Amount of content buffered in internal units, if amount known and
     *    accessible; -1 if not accessible.
     *
     * @since 2.6
     */
    public int getOutputBuffered() {
        return -1;
    }

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getOutputContext().getCurrentValue();
     *</code>
     *<p>
     * Note that "current value" is NOT populated (or used) by Streaming parser;
     * it is only used by higher-level data-binding functionality.
     * The reason it is included here is that it can be stored and accessed hierarchically,
     * and gets passed through data-binding.
     * 
     * @since 2.5
     */
    public Object getCurrentValue() {
        JsonStreamContext ctxt = getOutputContext();
        return (ctxt == null) ? null : ctxt.getCurrentValue();
    }

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getOutputContext().setCurrentValue(v);
     *</code>
     * 
     * @since 2.5
     */
    public void setCurrentValue(Object v) {
        JsonStreamContext ctxt = getOutputContext();
        if (ctxt != null) {
            ctxt.setCurrentValue(v);
        }
    }
    
    /*
    /**********************************************************
    /* Public API, capability introspection methods
    /**********************************************************
     */

    /**
     * Method that can be used to verify that given schema can be used with
     * this generator (using {@link #setSchema}).
     * 
     * @param schema Schema to check
     * 
     * @return True if this generator can use given schema; false if not
     */
    public boolean canUseSchema(FormatSchema schema) { return false; }
    
    /**
     * Introspection method that may be called to see if the underlying
     * data format supports some kind of Object Ids natively (many do not;
     * for example, JSON doesn't).
     * This method <b>must</b> be called prior to calling
     * {@link #writeObjectId} or {@link #writeObjectRef}.
     *<p>
     * Default implementation returns false; overridden by data formats
     * that do support native Object Ids. Caller is expected to either
     * use a non-native notation (explicit property or such), or fail,
     * in case it can not use native object ids.
     * 
     * @since 2.3
     */
    public boolean canWriteObjectId() { return false; }

    /**
     * Introspection method that may be called to see if the underlying
     * data format supports some kind of Type Ids natively (many do not;
     * for example, JSON doesn't).
     * This method <b>must</b> be called prior to calling
     * {@link #writeTypeId}.
     *<p>
     * Default implementation returns false; overridden by data formats
     * that do support native Type Ids. Caller is expected to either
     * use a non-native notation (explicit property or such), or fail,
     * in case it can not use native type ids.
     * 
     * @since 2.3
     */
    public boolean canWriteTypeId() { return false; }

    /**
     * Introspection method that may be called to see if the underlying
     * data format supports "native" binary data; that is, an efficient
     * output of binary content without encoding.
     *<p>
     * Default implementation returns false; overridden by data formats
     * that do support native binary content.
     * 
     * @since 2.3
     */
    public boolean canWriteBinaryNatively() { return false; }
    
    /**
     * Introspection method to call to check whether it is ok to omit
     * writing of Object fields or not. Most formats do allow omission,
     * but certain positional formats (such as CSV) require output of
     * placeholders, even if no real values are to be emitted.
     * 
     * @since 2.3
     */
    public boolean canOmitFields() { return true; }

    /*
    /**********************************************************
    /* Public API, write methods, structural
    /**********************************************************
     */

    /**
     * Method for writing starting marker of a Array value
     * (for JSON this is character '['; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Array values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a field name is expected.
     */
    public abstract void writeStartArray() throws IOException;

    /**
     * Method for writing start marker of an Array value, similar
     * to {@link #writeStartArray()}, but also specifying how many
     * elements will be written for the array before calling
     * {@link #writeEndArray()}.
     *<p>
     * Default implementation simply calls {@link #writeStartArray()}.
     * 
     * @param size Number of elements this array will have: actual
     *   number of values written (before matching call to
     *   {@link #writeEndArray()} MUST match; generator MAY verify
     *   this is the case.
     *   
     * @since 2.4
     */
    public void writeStartArray(int size) throws IOException {
        writeStartArray();
    }
    
    /**
     * Method for writing closing marker of a JSON Array value
     * (character ']'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Marker can be written if the innermost structured type
     * is Array.
     */
    public abstract void writeEndArray() throws IOException;

    /**
     * Method for writing starting marker of a JSON Object value
     * (character '{'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a field name is expected.
     */
    public abstract void writeStartObject() throws IOException;

    /**
     * Method for writing closing marker of a JSON Object value
     * (character '}'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Marker can be written if the innermost structured type
     * is Object, and the last written event was either a
     * complete value, or START-OBJECT marker (see JSON specification
     * for more details).
     */
    public abstract void writeEndObject() throws IOException;

    /**
     * Method for writing a field name (JSON String surrounded by
     * double quotes: syntactically identical to a JSON String value),
     * possibly decorated by white space if pretty-printing is enabled.
     *<p>
     * Field names can only be written in Object context (check out
     * JSON specification for details), when field name is expected
     * (field names alternate with values).
     */
    public abstract void writeFieldName(String name) throws IOException;

    /**
     * Method similar to {@link #writeFieldName(String)}, main difference
     * being that it may perform better as some of processing (such as
     * quoting of certain characters, or encoding into external encoding
     * if supported by generator) can be done just once and reused for
     * later calls.
     *<p>
     * Default implementation simple uses unprocessed name container in
     * serialized String; implementations are strongly encouraged to make
     * use of more efficient methods argument object has.
     */
    public abstract void writeFieldName(SerializableString name) throws IOException;

    /*
    /**********************************************************
    /* Public API, write methods, text/String values
    /**********************************************************
     */

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     */
    public abstract void writeString(String text) throws IOException;

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     */
    public abstract void writeString(char[] text, int offset, int len) throws IOException;

    /**
     * Method similar to {@link #writeString(String)}, but that takes
     * {@link SerializableString} which can make this potentially
     * more efficient to call as generator may be able to reuse
     * quoted and/or encoded representation.
     *<p>
     * Default implementation just calls {@link #writeString(String)};
     * sub-classes should override it with more efficient implementation
     * if possible.
     */
    public abstract void writeString(SerializableString text) throws IOException;

    /**
     * Method similar to {@link #writeString(String)} but that takes as
     * its input a UTF-8 encoded String that is to be output as-is, without additional
     * escaping (type of which depends on data format; backslashes for JSON).
     * However, quoting that data format requires (like double-quotes for JSON) will be added
     * around the value if and as necessary.
     *<p>
     * Note that some backends may choose not to support this method: for
     * example, if underlying destination is a {@link java.io.Writer}
     * using this method would require UTF-8 decoding.
     * If so, implementation may instead choose to throw a
     * {@link UnsupportedOperationException} due to ineffectiveness
     * of having to decode input.
     */
    public abstract void writeRawUTF8String(byte[] text, int offset, int length)
        throws IOException;

    /**
     * Method similar to {@link #writeString(String)} but that takes as its input
     * a UTF-8 encoded String which has <b>not</b> been escaped using whatever
     * escaping scheme data format requires (for JSON that is backslash-escaping
     * for control characters and double-quotes; for other formats something else).
     * This means that textual JSON backends need to check if value needs
     * JSON escaping, but otherwise can just be copied as is to output.
     * Also, quoting that data format requires (like double-quotes for JSON) will be added
     * around the value if and as necessary.
     *<p>
     * Note that some backends may choose not to support this method: for
     * example, if underlying destination is a {@link java.io.Writer}
     * using this method would require UTF-8 decoding.
     * In this case
     * generator implementation may instead choose to throw a
     * {@link UnsupportedOperationException} due to ineffectiveness
     * of having to decode input.
     */
    public abstract void writeUTF8String(byte[] text, int offset, int length)
        throws IOException;

    /*
    /**********************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************
     */

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     *<p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     */
    public abstract void writeRaw(String text) throws IOException;

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     *<p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     */
    public abstract void writeRaw(String text, int offset, int len) throws IOException;

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     *<p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     */
    public abstract void writeRaw(char[] text, int offset, int len) throws IOException;

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     *<p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     */
    public abstract void writeRaw(char c) throws IOException;

    /**
     * Method that will force generator to copy
     * input text verbatim with <b>no</b> modifications (including
     * that no escaping is done and no separators are added even
     * if context [array, object] would otherwise require such).
     * If such separators are desired, use
     * {@link #writeRawValue(String)} instead.
     *<p>
     * Note that not all generator implementations necessarily support
     * such by-pass methods: those that do not will throw
     * {@link UnsupportedOperationException}.
     *<p>
     * The default implementation delegates to {@link #writeRaw(String)};
     * other backends that support raw inclusion of text are encouraged
     * to implement it in more efficient manner (especially if they
     * use UTF-8 encoding).
     * 
     * @since 2.1
     */
    public void writeRaw(SerializableString raw) throws IOException {
        writeRaw(raw.getValue());
    }

    /**
     * Method that will force generator to copy
     * input text verbatim without any modifications, but assuming
     * it must constitute a single legal JSON value (number, string,
     * boolean, null, Array or List). Assuming this, proper separators
     * are added if and as needed (comma or colon), and generator
     * state updated to reflect this.
     */
    public abstract void writeRawValue(String text) throws IOException;

    public abstract void writeRawValue(String text, int offset, int len) throws IOException;

    public abstract void writeRawValue(char[] text, int offset, int len) throws IOException;

    /**
     * Method similar to {@link #writeRawValue(String)}, but potentially more
     * efficient as it may be able to use pre-encoded content (similar to
     * {@link #writeRaw(SerializableString)}.
     * 
     * @since 2.5
     */
    public void writeRawValue(SerializableString raw) throws IOException {
        writeRawValue(raw.getValue());
    }

    /**
     * Method that will output given chunk of binary data as base64
     * encoded, as a complete String value (surrounded by double quotes).
     * This method defaults
     *<p>
     * Note: because JSON Strings can not contain unescaped linefeeds,
     * if linefeeds are included (as per last argument), they must be
     * escaped. This adds overhead for decoding without improving
     * readability.
     * Alternatively if linefeeds are not included,
     * resulting String value may violate the requirement of base64
     * RFC which mandates line-length of 76 characters and use of
     * linefeeds. However, all {@link JsonParser} implementations
     * are required to accept such "long line base64"; as do
     * typical production-level base64 decoders.
     *
     * @param bv Base64 variant to use: defines details such as
     *   whether padding is used (and if so, using which character);
     *   what is the maximum line length before adding linefeed,
     *   and also the underlying alphabet to use.
     */
    public abstract void writeBinary(Base64Variant bv,
            byte[] data, int offset, int len) throws IOException;

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but default to using the Jackson default Base64 variant 
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}).
     */
    public void writeBinary(byte[] data, int offset, int len) throws IOException {
        writeBinary(Base64Variants.getDefaultVariant(), data, offset, len);
    }

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but assumes default to using the Jackson default Base64 variant 
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}). Also
     * assumes that whole byte array is to be output.
     */
    public void writeBinary(byte[] data) throws IOException {
        writeBinary(Base64Variants.getDefaultVariant(), data, 0, data.length);
    }

    /**
     * Similar to {@link #writeBinary(Base64Variant,InputStream,int)},
     * but assumes default to using the Jackson default Base64 variant 
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}).
     * 
     * @param data InputStream to use for reading binary data to write.
     *    Will not be closed after successful write operation
     * @param dataLength (optional) number of bytes that will be available;
     *    or -1 to be indicate it is not known. Note that implementations
     *    need not support cases where length is not known in advance; this
     *    depends on underlying data format: JSON output does NOT require length,
     *    other formats may
     */
    public int writeBinary(InputStream data, int dataLength)
        throws IOException {
        return writeBinary(Base64Variants.getDefaultVariant(), data, dataLength);
    }
    
    /**
     * Method similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but where input is provided through a stream, allowing for incremental
     * writes without holding the whole input in memory.
     * 
     * @param bv Base64 variant to use
     * @param data InputStream to use for reading binary data to write.
     *    Will not be closed after successful write operation
     * @param dataLength (optional) number of bytes that will be available;
     *    or -1 to be indicate it is not known.
     *    If a positive length is given, <code>data</code> MUST provide at least
     *    that many bytes: if not, an exception will be thrown.
     *    Note that implementations
     *    need not support cases where length is not known in advance; this
     *    depends on underlying data format: JSON output does NOT require length,
     *    other formats may.
     * 
     * @return Number of bytes read from <code>data</code> and written as binary payload
     * 
     * @since 2.1
     */
    public abstract int writeBinary(Base64Variant bv,
            InputStream data, int dataLength) throws IOException;

    /*
    /**********************************************************
    /* Public API, write methods, other value types
    /**********************************************************
     */

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @since 2.2
     */
    public void writeNumber(short v) throws IOException { writeNumber((int) v); }

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    public abstract void writeNumber(int v) throws IOException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    public abstract void writeNumber(long v) throws IOException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    public abstract void writeNumber(BigInteger v) throws IOException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    public abstract void writeNumber(double v) throws IOException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    public abstract void writeNumber(float v) throws IOException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     */
    public abstract void writeNumber(BigDecimal v) throws IOException;

    /**
     * Write method that can be used for custom numeric types that can
     * not be (easily?) converted to "standard" Java number types.
     * Because numbers are not surrounded by double quotes, regular
     * {@link #writeString} method can not be used; nor
     * {@link #writeRaw} because that does not properly handle
     * value separators needed in Array or Object contexts.
     *<p>
     * Note: because of lack of type safety, some generator
     * implementations may not be able to implement this
     * method. For example, if a binary JSON format is used,
     * it may require type information for encoding; similarly
     * for generator-wrappers around Java objects or JSON nodes.
     * If implementation does not implement this method,
     * it needs to throw {@link UnsupportedOperationException}.
     * 
     * @throws UnsupportedOperationException If underlying data format does not
     *   support numbers serialized textually AND if generator is not allowed
     *   to just output a String instead (Schema-based formats may require actual
     *   number, for example)
     */
    public abstract void writeNumber(String encodedValue) throws IOException;

    /**
     * Method for outputting literal JSON boolean value (one of
     * Strings 'true' and 'false').
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     */
    public abstract void writeBoolean(boolean state) throws IOException;

    /**
     * Method for outputting literal JSON null value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     */
    public abstract void writeNull() throws IOException;

    /*
    /**********************************************************
    /* Public API, write methods, Native Ids (type, object)
    /**********************************************************
     */

    /**
     * Method that can be called to output so-called native Object Id.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteObjectId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     * If output is not allowed by the data format in this position,
     * a {@link JsonGenerationException} will be thrown.
     * 
     * @since 2.3
     */
    public void writeObjectId(Object id) throws IOException {
        throw new JsonGenerationException("No native support for writing Object Ids", this);
    }

    /**
     * Method that can be called to output references to native Object Ids.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteObjectId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     * If output is not allowed by the data format in this position,
     * a {@link JsonGenerationException} will be thrown.
     */
    public void writeObjectRef(Object id) throws IOException {
        throw new JsonGenerationException("No native support for writing Object Ids", this);
    }
    
    /**
     * Method that can be called to output so-called native Type Id.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteTypeId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     * If output is not allowed by the data format in this position,
     * a {@link JsonGenerationException} will be thrown.
     * 
     * @since 2.3
     */
    public void writeTypeId(Object id) throws IOException {
        throw new JsonGenerationException("No native support for writing Type Ids", this);
    }

    /*
    /**********************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************
     */

    /**
     * Method for writing given Java object (POJO) as Json.
     * Exactly how the object gets written depends on object
     * in question (ad on codec, its configuration); for most
     * beans it will result in JSON Object, but for others JSON
     * Array, or String or numeric value (and for nulls, JSON
     * null literal.
     * <b>NOTE</b>: generator must have its <b>object codec</b>
     * set to non-null value; for generators created by a mapping
     * factory this is the case, for others not.
     */
    public abstract void writeObject(Object pojo) throws IOException;

    /**
     * Method for writing given JSON tree (expressed as a tree
     * where given JsonNode is the root) using this generator.
     * This will generally just call
     * {@link #writeObject} with given node, but is added
     * for convenience and to make code more explicit in cases
     * where it deals specifically with trees.
     */
    public abstract void writeTree(TreeNode rootNode) throws IOException;

    /*
    /**********************************************************
    /* Public API, convenience field write methods
    /**********************************************************
     */

    /**
     * Convenience method for outputting a field entry ("member")
     * that has a String value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeString(value);
     *</pre>
     *<p>
     * Note: many performance-sensitive implementations override this method
     */
    public void writeStringField(String fieldName, String value) throws IOException {
        writeFieldName(fieldName);
        writeString(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has a boolean value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeBoolean(value);
     *</pre>
     */
    public final void writeBooleanField(String fieldName, boolean value) throws IOException {
        writeFieldName(fieldName);
        writeBoolean(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has JSON literal value null. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNull();
     *</pre>
     */
    public final void writeNullField(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeNull();
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNumber(value);
     *</pre>
     */
    public final void writeNumberField(String fieldName, int value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNumber(value);
     *</pre>
     */
    public final void writeNumberField(String fieldName, long value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNumber(value);
     *</pre>
     */
    public final void writeNumberField(String fieldName, double value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNumber(value);
     *</pre>
     */
    public final void writeNumberField(String fieldName, float value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has the specified numeric value.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNumber(value);
     *</pre>
     */
    public final void writeNumberField(String fieldName, BigDecimal value) throws IOException {
        writeFieldName(fieldName);
        writeNumber(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that contains specified data in base64-encoded form.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeBinary(value);
     *</pre>
     */
    public final void writeBinaryField(String fieldName, byte[] data) throws IOException {
        writeFieldName(fieldName);
        writeBinary(data);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * (that will contain a JSON Array value), and the START_ARRAY marker.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeStartArray();
     *</pre>
     *<p>
     * Note: caller still has to take care to close the array
     * (by calling {#link #writeEndArray}) after writing all values
     * of the value Array.
     */
    public final void writeArrayFieldStart(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeStartArray();
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * (that will contain a JSON Object value), and the START_OBJECT marker.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeStartObject();
     *</pre>
     *<p>
     * Note: caller still has to take care to close the Object
     * (by calling {#link #writeEndObject}) after writing all
     * entries of the value Object.
     */
    public final void writeObjectFieldStart(String fieldName) throws IOException {
        writeFieldName(fieldName);
        writeStartObject();
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has contents of specific Java object as its value.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeObject(pojo);
     *</pre>
     */
    public final void writeObjectField(String fieldName, Object pojo) throws IOException {
        writeFieldName(fieldName);
        writeObject(pojo);
    }

    /**
     * Method called to indicate that a property in this position was
     * skipped. It is usually only called for generators that return
     * <code>false</code> from {@link #canOmitFields()}.
     *<p>
     * Default implementation does nothing.
     * 
     * @since 2.3
     */
    public void writeOmittedField(String fieldName) throws IOException { }
    
    /*
    /**********************************************************
    /* Public API, copy-through methods
    /**********************************************************
     */

    /**
     * Method for copying contents of the current event that
     * the given parser instance points to.
     * Note that the method <b>will not</b> copy any other events,
     * such as events contained within JSON Array or Object structures.
     *<p>
     * Calling this method will not advance the given
     * parser, although it may cause parser to internally process
     * more data (if it lazy loads contents of value events, for example)
     */
    public void copyCurrentEvent(JsonParser p) throws IOException
    {
        JsonToken t = p.getCurrentToken();
        // sanity check; what to do?
        if (t == null) {
            _reportError("No current event to copy");
        }
        switch (t.id()) {
        case ID_NOT_AVAILABLE:
            _reportError("No current event to copy");
        case ID_START_OBJECT:
            writeStartObject();
            break;
        case ID_END_OBJECT:
            writeEndObject();
            break;
        case ID_START_ARRAY:
            writeStartArray();
            break;
        case ID_END_ARRAY:
            writeEndArray();
            break;
        case ID_FIELD_NAME:
            writeFieldName(p.getCurrentName());
            break;
        case ID_STRING:
            if (p.hasTextCharacters()) {
                writeString(p.getTextCharacters(), p.getTextOffset(), p.getTextLength());
            } else {
                writeString(p.getText());
            }
            break;
        case ID_NUMBER_INT:
        {
            NumberType n = p.getNumberType();
            if (n == NumberType.INT) {
                writeNumber(p.getIntValue());
            } else if (n == NumberType.BIG_INTEGER) {
                writeNumber(p.getBigIntegerValue());
            } else {
                writeNumber(p.getLongValue());
            }
            break;
        }
        case ID_NUMBER_FLOAT:
        {
            NumberType n = p.getNumberType();
            if (n == NumberType.BIG_DECIMAL) {
                writeNumber(p.getDecimalValue());
            } else if (n == NumberType.FLOAT) {
                writeNumber(p.getFloatValue());
            } else {
                writeNumber(p.getDoubleValue());
            }
            break;
        }
        case ID_TRUE:
            writeBoolean(true);
            break;
        case ID_FALSE:
            writeBoolean(false);
            break;
        case ID_NULL:
            writeNull();
            break;
        case ID_EMBEDDED_OBJECT:
            writeObject(p.getEmbeddedObject());
            break;
        default:
            _throwInternal();
        }
    }

    /**
     * Method for copying contents of the current event
     * <b>and following events that it encloses</b>
     * the given parser instance points to.
     *<p>
     * So what constitutes enclosing? Here is the list of
     * events that have associated enclosed events that will
     * get copied:
     *<ul>
     * <li>{@link JsonToken#START_OBJECT}:
     *   all events up to and including matching (closing)
     *   {@link JsonToken#END_OBJECT} will be copied
     *  </li>
     * <li>{@link JsonToken#START_ARRAY}
     *   all events up to and including matching (closing)
     *   {@link JsonToken#END_ARRAY} will be copied
     *  </li>
     * <li>{@link JsonToken#FIELD_NAME} the logical value (which
     *   can consist of a single scalar value; or a sequence of related
     *   events for structured types (JSON Arrays, Objects)) will
     *   be copied along with the name itself. So essentially the
     *   whole <b>field entry</b> (name and value) will be copied.
     *  </li>
     *</ul>
     *<p>
     * After calling this method, parser will point to the
     * <b>last event</b> that was copied. This will either be
     * the event parser already pointed to (if there were no
     * enclosed events), or the last enclosed event copied.
     */
    public void copyCurrentStructure(JsonParser p) throws IOException
    {
        JsonToken t = p.getCurrentToken();
        if (t == null) {
            _reportError("No current event to copy");
        }
        // Let's handle field-name separately first
        int id = t.id();
        if (id == ID_FIELD_NAME) {
            writeFieldName(p.getCurrentName());
            t = p.nextToken();
            id = t.id();
            // fall-through to copy the associated value
        }
        switch (id) {
        case ID_START_OBJECT:
            writeStartObject();
            while (p.nextToken() != JsonToken.END_OBJECT) {
                copyCurrentStructure(p);
            }
            writeEndObject();
            break;
        case ID_START_ARRAY:
            writeStartArray();
            while (p.nextToken() != JsonToken.END_ARRAY) {
                copyCurrentStructure(p);
            }
            writeEndArray();
            break;
        default:
            copyCurrentEvent(p);
        }
    }

    /*
    /**********************************************************
    /* Public API, context access
    /**********************************************************
     */

    /**
     * @return Context object that can give information about logical
     *   position within generated json content.
     */
    public abstract JsonStreamContext getOutputContext();

    /*
    /**********************************************************
    /* Public API, buffer handling
    /**********************************************************
     */

    /**
     * Method called to flush any buffered content to the underlying
     * target (output stream, writer), and to flush the target itself
     * as well.
     */
    @Override
    public abstract void flush() throws IOException;

    /**
     * Method that can be called to determine whether this generator
     * is closed or not. If it is closed, no more output can be done.
     */
    public abstract boolean isClosed();

    /*
    /**********************************************************
    /* Closeable implementation
    /**********************************************************
     */

    /**
     * Method called to close this generator, so that no more content
     * can be written.
     *<p>
     * Whether the underlying target (stream, writer) gets closed depends
     * on whether this generator either manages the target (i.e. is the
     * only one with access to the target -- case if caller passes a
     * reference to the resource such as File, but not stream); or
     * has feature {@link Feature#AUTO_CLOSE_TARGET} enabled.
     * If either of above is true, the target is also closed. Otherwise
     * (not managing, feature not enabled), target is not closed.
     */
    @Override
    public abstract void close() throws IOException;

    /*
    /**********************************************************
    /* Helper methods for sub-classes
    /**********************************************************
     */

    /**
     * Helper method used for constructing and throwing
     * {@link JsonGenerationException} with given base message.
     *<p>
     * Note that sub-classes may override this method to add more detail
     * or use a {@link JsonGenerationException} sub-class.
     */
    protected void _reportError(String msg) throws JsonGenerationException {
        throw new JsonGenerationException(msg, this);
    }

    protected final void _throwInternal() { VersionUtil.throwInternal(); }

    protected void _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Operation not supported by generator of type "+getClass().getName());
    }

    /**
     * Helper method to try to call appropriate write method for given
     * untyped Object. At this point, no structural conversions should be done,
     * only simple basic types are to be coerced as necessary.
     *
     * @param value Non-null value to write
     */
    protected void _writeSimpleObject(Object value)  throws IOException
    {
        /* 31-Dec-2009, tatu: Actually, we could just handle some basic
         *    types even without codec. This can improve interoperability,
         *    and specifically help with TokenBuffer.
         */
        if (value == null) {
            writeNull();
            return;
        }
        if (value instanceof String) {
            writeString((String) value);
            return;
        }
        if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                writeNumber(n.intValue());
                return;
            } else if (n instanceof Long) {
                writeNumber(n.longValue());
                return;
            } else if (n instanceof Double) {
                writeNumber(n.doubleValue());
                return;
            } else if (n instanceof Float) {
                writeNumber(n.floatValue());
                return;
            } else if (n instanceof Short) {
                writeNumber(n.shortValue());
                return;
            } else if (n instanceof Byte) {
                writeNumber(n.byteValue());
                return;
            } else if (n instanceof BigInteger) {
                writeNumber((BigInteger) n);
                return;
            } else if (n instanceof BigDecimal) {
                writeNumber((BigDecimal) n);
                return;

            // then Atomic types
            } else if (n instanceof AtomicInteger) {
                writeNumber(((AtomicInteger) n).get());
                return;
            } else if (n instanceof AtomicLong) {
                writeNumber(((AtomicLong) n).get());
                return;
            }
        } else if (value instanceof byte[]) {
            writeBinary((byte[]) value);
            return;
        } else if (value instanceof Boolean) {
            writeBoolean((Boolean) value);
            return;
        } else if (value instanceof AtomicBoolean) {
            writeBoolean(((AtomicBoolean) value).get());
            return;
        }
        throw new IllegalStateException("No ObjectCodec defined for the generator, can only serialize simple wrapper types (type passed "
                +value.getClass().getName()+")");
    }    
}
