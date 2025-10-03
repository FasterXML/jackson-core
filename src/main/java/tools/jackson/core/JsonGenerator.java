/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package tools.jackson.core;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import tools.jackson.core.JsonParser.NumberType;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.core.type.WritableTypeId.Inclusion;
import tools.jackson.core.util.JacksonFeatureSet;

import static tools.jackson.core.JsonTokenId.*;

/**
 * Base class that defines public API for writing JSON content.
 * Instances are created using factory methods of
 * a {@link TokenStreamFactory} instance.
 *
 * @author Tatu Saloranta
 */
public abstract class JsonGenerator
    implements Closeable, Flushable, Versioned
{
    /*
    /**********************************************************************
    /* Construction, initialization
    /**********************************************************************
     */

    protected JsonGenerator() { }

    /*
    /**********************************************************************
    /* Versioned
    /**********************************************************************
     */

    /**
     * Accessor for finding out version of the bundle that provided this generator instance.
     */
    @Override
    public abstract Version version();

    /*
    /**********************************************************************
    /* Constraints violation checking
    /**********************************************************************
     */

    /**
     * Get the constraints to apply when performing streaming writes.
     *
     * @return Constraints used for this generator
     */
    public StreamWriteConstraints streamWriteConstraints() {
        return StreamWriteConstraints.defaults();
    }

    /*
    /**********************************************************************
    /* Public API, output configuration, state access
    /**********************************************************************
     */

    /**
     * Accessor for context object that provides information about low-level
     * logical position withing output token stream.
     *<p>
     * NOTE: method was called {@code getOutputContext()} in Jackson 2.x
     *
     * @return Stream output context ({@link TokenStreamContext}) associated with this generator
     */
    public abstract TokenStreamContext streamWriteContext();

    /**
     * Accessor for context object provided by higher-level databinding
     * functionality (or, in some cases, simple placeholder of the same)
     * that allows some level of interaction including ability to trigger
     * serialization of Object values through generator instance.
     *
     * @return Object write context ({@link ObjectWriteContext}) associated with this generator
     *
     * @since 3.0
     */
    public abstract ObjectWriteContext objectWriteContext();

    /**
     * Method that can be used to get access to object that is used
     * as target for generated output; this is usually either
     * {@link java.io.OutputStream} or {@link java.io.Writer}, depending on what
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
     *<p>
     * NOTE: was named {@code getOutputTarget()} in Jackson 2.x.
     *
     * @return Output target this generator was configured with
     */
    public abstract Object streamWriteOutputTarget();

    /**
     * Method for verifying amount of content that is buffered by generator
     * but not yet flushed to the underlying target (stream, writer),
     * in units (byte, char) that the generator implementation uses for buffering;
     * or -1 if this information is not available.
     * Unit used is often the same as the unit of underlying target (that is,
     * {@code byte} for {@link java.io.OutputStream},
     * {@code char} for {@link java.io.Writer}),
     * but may differ if buffering is done before encoding.
     * Default JSON-backed implementations do use matching units.
     *<p>
     * NOTE: was named {@code getOutputBuffered()} in Jackson 2.x.
     *
     * @return Amount of content buffered in internal units, if amount known and
     *    accessible; -1 if not accessible.
     */
    public abstract int streamWriteOutputBuffered();

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getOutputContext().currentValue();
     *</code>
     *<p>
     * Note that "current value" is NOT populated (or used) by Streaming generator;
     * it is only used by higher-level data-binding functionality.
     * The reason it is included here is that it can be stored and accessed hierarchically,
     * and gets passed through data-binding.
     *
     * @return "Current value" for the current context this generator has
     */
    public abstract Object currentValue();

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getOutputContext().assignCurrentValue(v);
     *</code>
     * used to assign "current value" for the current context of this generator.
     * It is usually assigned and used by higher level data-binding functionality
     * (instead of streaming parsers/generators) but is stored at streaming level.
     *
     * @param v "Current value" to assign to the current output context of this generator
     */
    public abstract void assignCurrentValue(Object v);

    /*
    /**********************************************************************
    /* Public API, Feature configuration
    /**********************************************************************
     */

    // 25-Jan-2021, tatu: Still called by `ClassUtil` of jackson-databind, to
    //    prevent secondary issues when closing generator. Should probably figure
    //    out alternate means of safe closing...

    /**
     * Method for enabling or disabling specified feature:
     * check {@link StreamWriteFeature} for list of available features.
     *<p>
     * NOTE: mostly left in 3.0 just to support disabling of
     * {@link StreamWriteFeature#AUTO_CLOSE_CONTENT} by {@code jackson-databind}
     *
     * @param f Feature to enable or disable
     * @param state Whether to enable the feature ({@code true}) or disable ({@code false})
     *
     * @return This generator, to allow call chaining
     */
    public abstract JsonGenerator configure(StreamWriteFeature f, boolean state);

    /**
     * Method for checking whether given feature is enabled.
     * Check {@link StreamWriteFeature} for list of available features.
     *
     * @param f Feature to check
     *
     * @return {@code True} if feature is enabled; {@code false} if not
     */
    public abstract boolean isEnabled(StreamWriteFeature f);

    /**
     * Bulk access method for getting state of all standard (format-agnostic)
     * {@link StreamWriteFeature}s.
     *
     * @return Bit mask that defines current states of all standard {@link StreamWriteFeature}s.
     *
     * @since 3.0
     */
    public abstract int streamWriteFeatures();

    /*
    /**********************************************************************
    /* Public API, other configuration
    /**********************************************************************
      */

    /**
     * Method for accessing Schema that this generator uses, if any.
     * Default implementation returns null.
     *
     * @return {@link FormatSchema} this generator is configured to use, if any; {@code null} if none
     */
    public FormatSchema getSchema() { return null; }

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
     *   if defined; or 0 to indicate no additional escaping is performed.
     */
    public int getHighestNonEscapedChar() { return 0; }

    /**
     * Method for accessing custom escapes generator uses for {@link JsonGenerator}s
     * it creates.
     *
     * @return {@link CharacterEscapes} this generator is configured to use, if any; {@code null} if none
     */
    public CharacterEscapes getCharacterEscapes() { return null; }

    // 04-Oct-2017, tatu: Would like to remove this method, but alas JSONP-support
    //    does require it...
    /**
     * Method for defining custom escapes factory uses for {@link JsonGenerator}s
     * it creates.
     *<p>
     * Default implementation does nothing and simply returns this instance.
     *
     * @param esc {@link CharacterEscapes} to configure this generator to use, if any; {@code null} if none
     *
     * @return This generator, to allow call chaining
     */
    public JsonGenerator setCharacterEscapes(CharacterEscapes esc) { return this; }

    /**
     * Accessor for object that handles pretty-printing (usually additional white space to make
     * results more human-readable) during output. If {@code null}, no pretty-printing is
     * done.
     * <p>
     * NOTE: this may be {@link PrettyPrinter} that {@link TokenStreamFactory} was
     * configured with (if stateless), OR an instance created via
     * {@link tools.jackson.core.util.Instantiatable#createInstance()} (if
     * stateful).
     *<p>
     * Default implementation returns null so pretty-printing capable generators
     * need to override it.
     *
     * @return Pretty printer used by this generator, if any; {@code null} if none
     */
    public PrettyPrinter getPrettyPrinter()  { return null; }

    /*
    /**********************************************************************
    /* Public API, capability introspection methods
    /**********************************************************************
     */

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
     * in case it cannot use native object ids.
     *
     * @return {@code True} if this generator is capable of writing "native" Object Ids
     *   (which is typically determined by capabilities of the underlying format),
     *   {@code false} if not
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
     * in case it cannot use native type ids.
     *
     * @return {@code True} if this generator is capable of writing "native" Type Ids
     *   (which is typically determined by capabilities of the underlying format),
     *   {@code false} if not
     */
    public boolean canWriteTypeId() { return false; }

    /**
     * Introspection method to call to check whether it is ok to omit
     * writing of Object properties or not. Most formats do allow omission,
     * but certain positional formats (such as CSV) require output of
     * place holders, even if no real values are to be emitted.
     *<p>
     * NOTE: in Jackson 2.x method was {@code canOmitFields()}.
     *
     * @return {@code True} if this generator is allowed to only write values
     *   of some Object properties and omit the rest; {@code false} if not
     */
    public boolean canOmitProperties() { return true; }

    /**
     * Accessor for checking whether this generator has specified capability.
     * Short-hand for:
     * {@code return getWriteCapabilities().isEnabled(capability); }
     *
     * @param capability Capability to check
     *
     * @return True if this generator has specified capability; false if not
     */
    public abstract boolean has(StreamWriteCapability capability);

    /**
     * Accessor for getting metadata on capabilities of this generator, based on
     * underlying data format being read (directly or indirectly).
     *
     * @return Set of read capabilities for content to generate via this generator
     */
    public abstract JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities();

    /*
    /**********************************************************************
    /* Public API, write methods, structural
    /**********************************************************************
     */

    /**
     * Method for writing starting marker of a Array value
     * (for JSON this is character '['; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Array values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a property name is expected.
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeStartArray() throws JacksonException;

    /**
     * Method for writing start marker of an Array value, similar
     * to {@link #writeStartArray()}, but also specifying what is the
     * Java object that the Array Object being written represents (if any);
     * {@code null} may be passed if not known or not applicable.
     * This value is accessible from context as "current value"
     *
     * @param currentValue Java Object that Array being written represents, if any
     *    (or {@code null} if not known or not applicable)
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     *
     * @return This generator, to allow call chaining
     */
    public abstract JsonGenerator writeStartArray(Object currentValue) throws JacksonException;

    /**
     * Method for writing start marker of an Array value, similar
     * to {@link #writeStartArray()}, but also specifying what is the
     * Java object that the Array Object being written represents (if any)
     * and how many elements will be written for the array before calling
     * {@link #writeEndArray()}.
     *
     * @param currentValue Java Object that Array being written represents, if any
     *    (or {@code null} if not known or not applicable)
     * @param size Number of elements this Array will have: actual
     *   number of values written (before matching call to
     *   {@link #writeEndArray()} MUST match; generator MAY verify
     *   this is the case (and SHOULD if format itself encodes length)
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeStartArray(Object currentValue, int size) throws JacksonException;

    /**
     * Method for writing closing marker of a JSON Array value
     * (character ']'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Marker can be written if the innermost structured type is Array.
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeEndArray() throws JacksonException;

    /**
     * Method for writing starting marker of an Object value
     * (character '{'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a property name is expected.
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeStartObject() throws JacksonException;

    /**
     * Method for writing starting marker of an Object value
     * to represent the given Java Object value.
     * Argument is offered as metadata, but more
     * importantly it should be assigned as the "current value"
     * for the Object content that gets constructed and initialized.
     *<p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a property name is expected.
     *
     * @param currentValue Java Object that Object being written represents, if any
     *    (or {@code null} if not known or not applicable)
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeStartObject(Object currentValue) throws JacksonException;

    /**
     * Method for writing starting marker of an Object value
     * to represent the given Java Object value.
     * Argument is offered as metadata, but more
     * importantly it should be assigned as the "current value"
     * for the Object content that gets constructed and initialized.
     * In addition, caller knows number of key/value pairs ("properties")
     * that will get written for the Object value: this is relevant for
     * some format backends (but not, as an example, for JSON).
     *<p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a property name is expected.
     *
     * @param forValue Object value to be written (assigned as "current value" for
     *    the Object context that gets created)
     * @param size Number of key/value pairs this Object will have: actual
     *   number of entries written (before matching call to
     *   {@link #writeEndObject()} MUST match; generator MAY verify
     *   this is the case (and SHOULD if format itself encodes length)
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeStartObject(Object forValue, int size) throws JacksonException;

    /**
     * Method for writing closing marker of an Object value
     * (character '}'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Marker can be written if the innermost structured type
     * is Object, and the last written event was either a
     * complete value, or START-OBJECT marker (see JSON specification
     * for more details).
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeEndObject() throws JacksonException;

    /**
     * Method for writing an Object Property name (JSON String surrounded by
     * double quotes: syntactically identical to a JSON String value),
     * possibly decorated by white space if pretty-printing is enabled.
     *<p>
     * Property names can only be written in Object context (check out
     * JSON specification for details), when Object Property name is expected
     * (property names alternate with values).
     *
     * @param name Name of the Object Property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeName(String name) throws JacksonException;

    /**
     * Method similar to {@link #writeName(String)}, main difference
     * being that it may perform better as some of processing (such as
     * quoting of certain characters, or encoding into external encoding
     * if supported by generator) can be done just once and reused for
     * later calls.
     *<p>
     * Default implementation simple uses unprocessed name container in
     * serialized String; implementations are strongly encouraged to make
     * use of more efficient methods argument object has.
     *
     * @param name Pre-encoded name of the Object Property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeName(SerializableString name) throws JacksonException;

    /**
     * Alternative to {@link #writeName(String)} that may be used
     * in cases where Object Property key is of numeric type; usually where
     * underlying format supports such notion (some binary formats do,
     * unlike JSON).
     * Default implementation will simply convert id into {@code String}
     * and call {@link #writeName(String)}.
     *
     * @param id Property key id to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writePropertyId(long id) throws JacksonException;

    /*
    /**********************************************************************
    /* Public API, write methods, scalar arrays
    /**********************************************************************
     */

    /**
     * Value write method that can be called to write a single
     * array (sequence of {@link JsonToken#START_ARRAY}, zero or
     * more {@link JsonToken#VALUE_NUMBER_INT}, {@link JsonToken#END_ARRAY})
     *
     * @param array Array that contains values to write
     * @param offset Offset of the first element to write, within array
     * @param length Number of elements in array to write, from `offset` to `offset + len - 1`
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeArray(int[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
        return this;
    }

    /**
     * Value write method that can be called to write a single
     * array (sequence of {@link JsonToken#START_ARRAY}, zero or
     * more {@link JsonToken#VALUE_NUMBER_INT}, {@link JsonToken#END_ARRAY})
     *
     * @param array Array that contains values to write
     * @param offset Offset of the first element to write, within array
     * @param length Number of elements in array to write, from `offset` to `offset + len - 1`
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeArray(long[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
        return this;
    }

    /**
     * Value write method that can be called to write a single
     * array (sequence of {@link JsonToken#START_ARRAY}, zero or
     * more {@link JsonToken#VALUE_NUMBER_FLOAT}, {@link JsonToken#END_ARRAY})
     *
     * @param array Array that contains values to write
     * @param offset Offset of the first element to write, within array
     * @param length Number of elements in array to write, from `offset` to `offset + len - 1`
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeArray(double[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
        return this;
    }

    /**
     * Value write method that can be called to write a single
     * array (sequence of {@link JsonToken#START_ARRAY}, zero or
     * more {@link JsonToken#VALUE_STRING}, {@link JsonToken#END_ARRAY})
     *
     * @param array Array that contains values to write
     * @param offset Offset of the first element to write, within array
     * @param length Number of elements in array to write, from `offset` to `offset + len - 1`
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeArray(String[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeString(array[i]);
        }
        writeEndArray();
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, text/String values
    /**********************************************************************
     */

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) property value or
     * a stand-alone (root-level value) String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     *
     * @param value String value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeString(String value) throws JacksonException;

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) property value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     * If {@code len} is &lt; 0, then write all contents of the reader.
     * Otherwise, write only len characters.
     *<p>
     * Note: actual length of content available may exceed {@code len} but
     * cannot be less than it: if not enough content available, a
     * {@link StreamWriteException} will be thrown.
     *
     * @param reader Reader to use for reading Text value to write
     * @param len Maximum Length of Text value to read (in {@code char}s, non-negative)
     *    if known; {@code -1} to indicate "read and write it all"
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     *    (including the case where {@code reader} does not provide enough content)
     */
    public abstract JsonGenerator writeString(Reader reader, int len) throws JacksonException;

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) property value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     *
     * @param buffer Buffer that contains String value to write
     * @param offset Offset in {@code buffer} of the first character of String value to write
     * @param len Length of the String value (in characters) to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeString(char[] buffer, int offset, int len) throws JacksonException;

    /**
     * Method similar to {@link #writeString(String)}, but that takes
     * {@link SerializableString} which can make this potentially
     * more efficient to call as generator may be able to reuse
     * quoted and/or encoded representation.
     *<p>
     * Default implementation just calls {@link #writeString(String)};
     * sub-classes should override it with more efficient implementation
     * if possible.
     *
     * @param value Pre-encoded String value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeString(SerializableString value) throws JacksonException;

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
     *
     * @param buffer Buffer that contains String value to write
     * @param offset Offset in {@code buffer} of the first byte of String value to write
     * @param len Length of the String value (in characters) to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeRawUTF8String(byte[] buffer, int offset, int len)
        throws JacksonException;

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
     *
     * @param buffer Buffer that contains String value to write
     * @param offset Offset in {@code buffer} of the first byte of String value to write
     * @param len Length of the String value (in characters) to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeUTF8String(byte[] buffer, int offset, int len)
        throws JacksonException;

    /*
    /**********************************************************************
    /* Public API, write methods, raw content
    /**********************************************************************
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
     *
     * @return This generator, to allow call chaining
     *
     * @param text Textual contents to include as-is in output.
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeRaw(String text) throws JacksonException;

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
     *
     * @param text String that has contents to include as-is in output
     * @param offset Offset within {@code text} of the first character to output
     * @param len Length of content (from {@code text}, starting at offset {@code offset}) to output
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeRaw(String text, int offset, int len) throws JacksonException;

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
     *
     * @param buffer Buffer that has contents to include as-is in output
     * @param offset Offset within {@code text} of the first character to output
     * @param len Length of content (from {@code text}, starting at offset {@code offset}) to output
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeRaw(char[] buffer, int offset, int len) throws JacksonException;

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
     *
     * @param c Character to included in output
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeRaw(char c) throws JacksonException;

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
     * @param raw Pre-encoded textual contents to included in output
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeRaw(SerializableString raw) throws JacksonException {
        return writeRaw(raw.getValue());
    }

    /**
     * Method that will force generator to copy
     * input text verbatim without any modifications, but assuming
     * it must constitute a single legal JSON value (number, string,
     * boolean, null, Array or List). Assuming this, proper separators
     * are added if and as needed (comma or colon), and generator
     * state updated to reflect this.
     *
     * @param text Textual contents to included in output
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeRawValue(String text) throws JacksonException;

    public abstract JsonGenerator writeRawValue(String text, int offset, int len) throws JacksonException;

    public abstract JsonGenerator writeRawValue(char[] text, int offset, int len) throws JacksonException;

    /**
     * Method similar to {@link #writeRawValue(String)}, but potentially more
     * efficient as it may be able to use pre-encoded content (similar to
     * {@link #writeRaw(SerializableString)}.
     *
     * @param raw Pre-encoded textual contents to included in output
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeRawValue(SerializableString raw) throws JacksonException {
        return writeRawValue(raw.getValue());
    }

    /*
    /**********************************************************************
    /* Public API, write methods, Binary values
    /**********************************************************************
     */

    /**
     * Method that will output given chunk of binary data as base64
     * encoded, as a complete String value (surrounded by double quotes).
     * This method defaults
     *<p>
     * Note: because JSON Strings cannot contain unescaped linefeeds,
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
     * @param data Buffer that contains binary data to write
     * @param offset Offset in {@code data} of the first byte of data to write
     * @param len Length of data to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeBinary(Base64Variant bv,
            byte[] data, int offset, int len) throws JacksonException;

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but default to using the Jackson default Base64 variant
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}).
     *
     * @param data Buffer that contains binary data to write
     * @param offset Offset in {@code data} of the first byte of data to write
     * @param len Length of data to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeBinary(byte[] data, int offset, int len) throws JacksonException {
        return writeBinary(Base64Variants.getDefaultVariant(), data, offset, len);
    }

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but assumes default to using the Jackson default Base64 variant
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}). Also
     * assumes that whole byte array is to be output.
     *
     * @param data Buffer that contains binary data to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeBinary(byte[] data) throws JacksonException {
        return writeBinary(Base64Variants.getDefaultVariant(), data, 0, data.length);
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
     *
     * @return Number of bytes actually written
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public int writeBinary(InputStream data, int dataLength) throws JacksonException {
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
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract int writeBinary(Base64Variant bv,
            InputStream data, int dataLength) throws JacksonException;

    /*
    /**********************************************************************
    /* Public API, write methods, numeric
    /**********************************************************************
     */

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(short v) throws JacksonException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(int v) throws JacksonException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(long v) throws JacksonException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(BigInteger v) throws JacksonException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(double v) throws JacksonException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(float v) throws JacksonException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(BigDecimal v) throws JacksonException;

    /**
     * Write method that can be used for custom numeric types that can
     * not be (easily?) converted to "standard" Java number types.
     * Because numbers are not surrounded by double quotes, regular
     * {@link #writeString} method cannot be used; nor
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
     * @param encodedValue Textual (possibly format) number representation to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws UnsupportedOperationException If underlying data format does not
     *   support numbers serialized textually AND if generator is not allowed
     *   to just output a String instead (Schema-based formats may require actual
     *   number, for example)
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNumber(String encodedValue) throws JacksonException;

    /**
     * Overloaded version of {@link #writeNumber(String)} with same semantics
     * but possibly more efficient operation.
     *
     * @param encodedValueBuffer Buffer that contains the textual number representation to write
     * @param offset Offset of the first character of value to write
     * @param len Length of the value (in characters) to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeNumber(char[] encodedValueBuffer, int offset, int len) throws JacksonException {
        return writeNumber(new String(encodedValueBuffer, offset, len));
    }

    /*
    /**********************************************************************
    /* Public API, write methods, other value types
    /**********************************************************************
     */

    /**
     * Method for outputting literal JSON boolean value (one of
     * Strings 'true' and 'false').
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param state Boolean value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeBoolean(boolean state) throws JacksonException;

    /**
     * Method for outputting literal JSON null value.
     * Can be called in any context where a value is expected
     * (Array value, Object property value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeNull() throws JacksonException;

    /**
     * Method that can be called on backends that support passing opaque native
     * values that some data formats support; not used with JSON backend,
     * more common with binary formats.
     *<p>
     * NOTE: this is NOT the method to call for serializing regular POJOs,
     * see {@link #writePOJO} instead.
     *
     * @param object Native format-specific value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeEmbeddedObject(Object object) throws JacksonException {
        // 01-Sep-2016, tatu: As per [core#318], handle small number of cases
        if (object == null) {
            writeNull();
            return this;
        }
        if (object instanceof byte[]) {
            writeBinary((byte[]) object);
            return this;
        }
        throw _constructWriteException("No native support for writing embedded objects of type %s",
                object.getClass().getName());
    }

    /*
    /**********************************************************************
    /* Public API, write methods, Native Ids (type, object)
    /**********************************************************************
     */

    /**
     * Method that can be called to output so-called native Object Id.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteObjectId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     *
     * @param id Native Object Id to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream;
     *   typically if Object ID output is not allowed
     *   (either at all, or specifically in this position in output)
     */
    public JsonGenerator writeObjectId(Object id) throws JacksonException {
        throw _constructWriteException("No native support for writing Object Ids");
    }

    /**
     * Method that can be called to output references to native Object Ids.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteObjectId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     * If output is not allowed by the data format in this position,
     * a {@link StreamWriteException} will be thrown.
     *
     * @param referenced Referenced value, for which Object Id is expected to be written
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream;
     *   typically if Object ID output is not allowed
     *   (either at all, or specifically in this position in output)
     */
    public JsonGenerator writeObjectRef(Object referenced) throws JacksonException {
        throw _constructWriteException("No native support for writing Object Ids");
    }

    /**
     * Method that can be called to output so-called native Type Id.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteTypeId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     * If output is not allowed by the data format in this position,
     * a {@link StreamWriteException} will be thrown.
     *
     * @param id Native Type Id to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeTypeId(Object id) throws JacksonException {
        throw _constructWriteException("No native support for writing Type Ids");
    }

    /**
     * Replacement method for {@link #writeTypeId(Object)} which is called
     * regardless of whether format has native type ids. If it does have native
     * type ids, those are to be used (if configuration allows this), if not,
     * structural type id inclusion is to be used. For JSON, for example, no
     * native type ids exist and structural inclusion is always used.
     *<p>
     * NOTE: databind may choose to skip calling this method for some special cases
     * (and instead included type id via regular write methods and/or {@link #writeTypeId}
     * -- this is discouraged, but not illegal, and may be necessary as a work-around
     * in some cases.
     *
     * @param typeIdDef Full Type Id definition
     *
     * @return {@link WritableTypeId} for caller to retain and pass to matching
     *   {@link #writeTypeSuffix} call
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public WritableTypeId writeTypePrefix(WritableTypeId typeIdDef)
        throws JacksonException
    {
        final boolean wasStartObjectWritten = canWriteTypeId()
                ? _writeTypePrefixUsingNative(typeIdDef)
                : _writeTypePrefixUsingWrapper(typeIdDef);

        // And then possible start marker for value itself:
        switch (typeIdDef.valueShape) {
        case START_OBJECT:
            if (!wasStartObjectWritten) {
                writeStartObject(typeIdDef.forValue);
            }
            break;
        case START_ARRAY:
            writeStartArray(typeIdDef.forValue);
            break;
        default: // otherwise: no start marker
        }

        return typeIdDef;
    }

    /**
     * Writes a native type id (when supported by format).
     *
     * @return True if start of an object has been written, False otherwise.
     */
    protected boolean _writeTypePrefixUsingNative(WritableTypeId typeIdDef) throws JacksonException {
        typeIdDef.wrapperWritten = false;
        writeTypeId(typeIdDef.id);
        return false;
    }

    /**
     * Writes a wrapper for the type id if necessary.
     *
     * @return True if start of an object has been written, false otherwise.
     */
    protected boolean _writeTypePrefixUsingWrapper(WritableTypeId typeIdDef) throws JacksonException {
        // Normally we only support String type ids (non-String reserved for native type ids)
        final String id = Objects.toString(typeIdDef.id, null);

        // If we don't have Type ID we don't write a wrapper.
        if (id == null) {
            return false;
        }
        Inclusion incl = typeIdDef.include;

        // first: cannot output "as property" if value not Object; if so, must do "as array"
        if ((typeIdDef.valueShape != JsonToken.START_OBJECT) && incl.requiresObjectContext()) {
            typeIdDef.include = incl = WritableTypeId.Inclusion.WRAPPER_ARRAY;
        }

        typeIdDef.wrapperWritten = true;
        switch (incl) {
        case PARENT_PROPERTY:
            // nothing to do here, as it has to be written in suffix...
            break;
        case PAYLOAD_PROPERTY:
            // only output as native type id; otherwise caller must handle using some
            // other mechanism, so...
            break;
        case METADATA_PROPERTY:
            // must have Object context by now, so simply write as field name
            // Note, too, that it's bit tricky, since we must print START_OBJECT that is part
            // of value first -- and then NOT output it later on: hence return "early"
            writeStartObject(typeIdDef.forValue);
            writeStringProperty(typeIdDef.asProperty, id);
            return true;

        case WRAPPER_OBJECT:
            // NOTE: this is wrapper, not directly related to value to output, so
            // do NOT pass "typeIdDef.forValue"
            writeStartObject();
            writeName(id);
            break;
        case WRAPPER_ARRAY:
        default: // should never occur but translate as "as-array"
            writeStartArray(); // wrapper, not actual array object to write
            writeString(id);
        }
        return false;
    }
    
    public WritableTypeId writeTypeSuffix(WritableTypeId typeIdDef) throws JacksonException
    {
        final JsonToken valueShape = typeIdDef.valueShape;
        // First: does value need closing?
        if (valueShape == JsonToken.START_OBJECT) {
            writeEndObject();
        } else if (valueShape == JsonToken.START_ARRAY) {
            writeEndArray();
        }

        if (typeIdDef.wrapperWritten) {
            switch (typeIdDef.include) {
            case WRAPPER_ARRAY:
                writeEndArray();
                break;
            case PARENT_PROPERTY:
                // unusually, need to output AFTER value. And no real wrapper...
                {
                    Object id = typeIdDef.id;
                    String idStr = (id instanceof String) ? (String) id : String.valueOf(id);
                    writeStringProperty(typeIdDef.asProperty, idStr);
                }
                break;
            case METADATA_PROPERTY:
            case PAYLOAD_PROPERTY:
                // no actual wrapper; included within Object itself
                break;
            case WRAPPER_OBJECT:
            default: // should never occur but...
                writeEndObject();
                break;
            }
        }
        return typeIdDef;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************************
     */

    /**
     * Method for writing given Java object (POJO) as tokens into
     * stream this generator manages; serialization must be a valid JSON Value
     * (Object, Array, null, Number, String or Boolean).
     * This is done by delegating call to
     * {@link ObjectWriteContext#writeValue(JsonGenerator, Object)}.
     *
     * @param pojo Java Object (POJO) value to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writePOJO(Object pojo) throws JacksonException;

    /**
     * Method for writing given JSON tree (expressed as a tree
     * where given {@code TreeNode} is the root) using this generator.
     * This is done by delegating call to
     * {@link ObjectWriteContext#writeTree}.
     *
     * @param rootNode {@link TreeNode} to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public abstract JsonGenerator writeTree(TreeNode rootNode) throws JacksonException;

    /*
    /**********************************************************************
    /* Public API, convenience property write methods
    /**********************************************************************
     */

    // 25-May-2020, tatu: NOTE! Made `final` on purpose in 3.x to prevent issues
    //    rising from complexity of overriding only some of methods (writeName()
    //    and matching writeXxx() for value)

    /**
     * Convenience method for outputting an Object property
     * that contains specified data in base64-encoded form.
     * Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeBinary(value);
     *</pre>
     *
     * @param propertyName Name of Object Property to write
     * @param data Binary value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeBinaryProperty(String propertyName, byte[] data) throws JacksonException {
        writeName(propertyName);
        return writeBinary(data);
    }

    /**
     * Convenience method for outputting an Object property
     * that has a boolean value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeBoolean(value);
     *</pre>
     *
     * @param propertyName Name of Object Property to write
     * @param value Boolean value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeBooleanProperty(String propertyName, boolean value) throws JacksonException {
        writeName(propertyName);
        return writeBoolean(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has JSON literal value null. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNull();
     *</pre>
     *
     * @param propertyName Name of the null-valued property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNullProperty(String propertyName) throws JacksonException {
        writeName(propertyName);
        return writeNull();
    }

    /**
     * Convenience method for outputting an Object property
     * that has a String value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeString(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value String value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeStringProperty(String propertyName, String value) throws JacksonException {
        writeName(propertyName);
        return writeString(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, short value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, int value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, long value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, BigInteger value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, float value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, double value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * that has the specified numeric value.
     * Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeNumber(value);
     *</pre>
     *
     * @param propertyName Name of the property to write
     * @param value Numeric value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeNumberProperty(String propertyName, BigDecimal value) throws JacksonException {
        writeName(propertyName);
        return writeNumber(value);
    }

    /**
     * Convenience method for outputting an Object property
     * (that will contain a JSON Array value), and the START_ARRAY marker.
     * Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeStartArray();
     *</pre>
     *<p>
     * Note: caller still has to take care to close the array
     * (by calling {#link #writeEndArray}) after writing all values
     * of the value Array.
     *
     * @param propertyName Name of the Array property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeArrayPropertyStart(String propertyName) throws JacksonException {
        writeName(propertyName);
        return writeStartArray();
    }

    /**
     * Convenience method for outputting an Object property
     * (that will contain an Object value), and the START_OBJECT marker.
     * Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeStartObject();
     *</pre>
     *<p>
     * Note: caller still has to take care to close the Object
     * (by calling {#link #writeEndObject}) after writing all
     * entries of the value Object.
     *
     * @param propertyName Name of the Object property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writeObjectPropertyStart(String propertyName) throws JacksonException {
        writeName(propertyName);
        return writeStartObject();
    }

    /**
     * Convenience method for outputting am Object property
     * that has contents of specific Java object (POJO) as its value.
     * Equivalent to:
     *<pre>
     *  writeName(propertyName);
     *  writeObject(pojo);
     *</pre>
     *<p>
     * NOTE: see {@link #writePOJO(Object)} for details on how POJO value actually
     * gets written (uses delegation).
     *
     * @param propertyName Name of the property to write
     * @param pojo POJO value of the property to write
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public final JsonGenerator writePOJOProperty(String propertyName, Object pojo) throws JacksonException {
        writeName(propertyName);
        return writePOJO(pojo);
    }

    // // // But this method does need to be delegate so...

    /**
     * Method called to indicate that a property in this position was
     * skipped. It is usually only called for generators that return
     * <code>false</code> from {@link #canOmitProperties()}.
     *<p>
     * Default implementation does nothing; method is overriden by some format
     * backends.
     *
     * @param propertyName Name of the property that is being omitted
     *
     * @return This generator, to allow call chaining
     *
     * @throws JacksonIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    public JsonGenerator writeOmittedProperty(String propertyName) throws JacksonException {
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, copy-through methods
    /*
    /* NOTE: need to remain here for `JsonGeneratorDelegate` to call
    /* (or refactor to have "JsonGeneratorMinimalBase" or such)
    /**********************************************************************
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
     *
     * @param p Parser that points to the event to copy
     *
     * @throws JacksonIOException if there is an underlying I/O problem (reading or writing)
     * @throws StreamReadException for problems with decoding of token stream
     * @throws StreamWriteException for problems in encoding token stream
     */
    public void copyCurrentEvent(JsonParser p) throws JacksonException
    {
        JsonToken t = p.currentToken();
        final int token = (t == null) ? ID_NOT_AVAILABLE : t.id();
        switch (token) {
        case ID_NOT_AVAILABLE:
            _reportError("No current event to copy");
            break; // never gets here
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
        case ID_PROPERTY_NAME:
            writeName(p.currentName());
            break;
        case ID_STRING:
            _copyCurrentStringValue(p);
            break;
        case ID_NUMBER_INT:
            _copyCurrentIntValue(p);
            break;
        case ID_NUMBER_FLOAT:
            // Different from "copyCurrentEventExact"!
            _copyCurrentFloatValue(p);
            break;
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
            writePOJO(p.getEmbeddedObject());
            break;
        default:
            throw new IllegalStateException("Internal error: unknown current token, "+t);
        }
    }

    /**
     * Same as {@link #copyCurrentEvent} with the exception that copying of numeric
     * values tries to avoid any conversion losses; in particular for floating-point
     * numbers. This usually matters when transcoding from textual format like JSON
     * to a binary format.
     * See {@link #_copyCurrentFloatValueExact} for details.
     *
     * @param p Parser that points to the event to copy
     *
     * @throws JacksonIOException if there is an underlying I/O problem (reading or writing)
     * @throws StreamReadException for problems with decoding of token stream
     * @throws StreamWriteException for problems in encoding token stream
     */
    public void copyCurrentEventExact(JsonParser p) throws JacksonException
    {
        JsonToken t = p.currentToken();
        final int token = (t == null) ? ID_NOT_AVAILABLE : t.id();
        switch (token) {
        case ID_NOT_AVAILABLE:
            _reportError("No current event to copy");
            break; // never gets here
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
        case ID_PROPERTY_NAME:
            writeName(p.currentName());
            break;
        case ID_STRING:
            _copyCurrentStringValue(p);
            break;
        case ID_NUMBER_INT:
            _copyCurrentIntValue(p);
            break;
        case ID_NUMBER_FLOAT:
            // Different from "copyCurrentEvent"!
            _copyCurrentFloatValueExact(p);
            break;
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
            writePOJO(p.getEmbeddedObject());
            break;
        default:
            throw new IllegalStateException("Internal error: unknown current token, "+t);
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
     * <li>{@link JsonToken#PROPERTY_NAME} the logical value (which
     *   can consist of a single scalar value; or a sequence of related
     *   events for structured types (JSON Arrays, Objects)) will
     *   be copied along with the name itself. So essentially the
     *   whole <b>Object property</b> (name and value) will be copied.
     *  </li>
     *</ul>
     *<p>
     * After calling this method, parser will point to the
     * <b>last event</b> that was copied. This will either be
     * the event parser already pointed to (if there were no
     * enclosed events), or the last enclosed event copied.
     *
     * @param p Parser that points to the value to copy
     *
     * @throws JacksonIOException if there is an underlying I/O problem (reading or writing)
     * @throws StreamReadException for problems with decoding of token stream
     * @throws StreamWriteException for problems in encoding token stream
     */
    public void copyCurrentStructure(JsonParser p) throws JacksonException
    {
        JsonToken t = p.currentToken();
        // Let's handle property-name separately first
        int id = (t == null) ? ID_NOT_AVAILABLE : t.id();
        if (id == ID_PROPERTY_NAME) {
            writeName(p.currentName());
            t = p.nextToken();
            id = (t == null) ? ID_NOT_AVAILABLE : t.id();
            // fall-through to copy the associated value
        }
        switch (id) {
        case ID_START_OBJECT:
            writeStartObject();
            _copyCurrentContents(p);
            return;
        case ID_START_ARRAY:
            writeStartArray();
            _copyCurrentContents(p);
            return;

        default:
            copyCurrentEvent(p);
        }
    }

    /**
     * Same as {@link #copyCurrentStructure} with the exception that copying of numeric
     * values tries to avoid any conversion losses; in particular for floating-point
     * numbers. This usually matters when transcoding from textual format like JSON
     * to a binary format.
     * See {@link #_copyCurrentFloatValueExact} for details.
     *
     * @param p Parser that points to the value to copy
     *
     * @throws JacksonIOException if there is an underlying I/O problem (reading or writing)
     * @throws StreamReadException for problems with decoding of token stream
     * @throws StreamWriteException for problems in encoding token stream
     */
    public void copyCurrentStructureExact(JsonParser p)  throws JacksonException
    {
        JsonToken t = p.currentToken();
        // Let's handle property-name separately first
        int id = (t == null) ? ID_NOT_AVAILABLE : t.id();
        if (id == ID_PROPERTY_NAME) {
            writeName(p.currentName());
            t = p.nextToken();
            id = (t == null) ? ID_NOT_AVAILABLE : t.id();
            // fall-through to copy the associated value
        }
        switch (id) {
        case ID_START_OBJECT:
            writeStartObject();
            _copyCurrentContentsExact(p);
            return;
        case ID_START_ARRAY:
            writeStartArray();
            _copyCurrentContentsExact(p);
            return;

        default:
            copyCurrentEventExact(p);
        }
    }

    protected void _copyCurrentContents(JsonParser p) throws JacksonException
    {
        int depth = 1;
        JsonToken t;

        // Mostly copied from `copyCurrentEvent()`, but with added nesting counts
        while ((t = p.nextToken()) != null) {
            switch (t.id()) {
            case ID_PROPERTY_NAME:
                writeName(p.currentName());
                break;

            case ID_START_ARRAY:
                writeStartArray();
                ++depth;
                break;

            case ID_START_OBJECT:
                writeStartObject();
                ++depth;
                break;

            case ID_END_ARRAY:
                writeEndArray();
                if (--depth == 0) {
                    return;
                }
                break;
            case ID_END_OBJECT:
                writeEndObject();
                if (--depth == 0) {
                    return;
                }
                break;

            case ID_STRING:
                _copyCurrentStringValue(p);
                break;
            case ID_NUMBER_INT:
                _copyCurrentIntValue(p);
                break;
            case ID_NUMBER_FLOAT:
                _copyCurrentFloatValue(p);
                break;
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
                writePOJO(p.getEmbeddedObject());
                break;
            default:
                throw new IllegalStateException("Internal error: unknown current token, "+t);
            }
        }
    }

    protected void _copyCurrentContentsExact(JsonParser p) throws JacksonException
    {
        int depth = 1;
        JsonToken t;

        // Mostly copied from `copyCurrentEventExact()`, but with added nesting counts
        while ((t = p.nextToken()) != null) {
            switch (t.id()) {
            case ID_PROPERTY_NAME:
                writeName(p.currentName());
                break;

            case ID_START_ARRAY:
                writeStartArray();
                ++depth;
                break;

            case ID_START_OBJECT:
                writeStartObject();
                ++depth;
                break;

            case ID_END_ARRAY:
                writeEndArray();
                if (--depth == 0) {
                    return;
                }
                break;
            case ID_END_OBJECT:
                writeEndObject();
                if (--depth == 0) {
                    return;
                }
                break;

            case ID_STRING:
                _copyCurrentStringValue(p);
                break;
            case ID_NUMBER_INT:
                _copyCurrentIntValue(p);
                break;
            case ID_NUMBER_FLOAT:
                _copyCurrentFloatValueExact(p);
                break;
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
                writePOJO(p.getEmbeddedObject());
                break;
            default:
                throw new IllegalStateException("Internal error: unknown current token, "+t);
            }
        }
    }
    
    /**
     * Method for copying current {@link JsonToken#VALUE_NUMBER_FLOAT} value;
     * overridable by format backend implementations.
     * Implementation checks
     * {@link JsonParser#getNumberType()} for declared type and uses matching
     * accessors: this may cause inexact conversion for some textual formats
     * (depending on settings). If this is problematic, use
     * {@link #_copyCurrentFloatValueExact} instead (note that doing so may add
     * overhead).
     *
     * @param p Parser that points to the value to copy
     */
    protected void _copyCurrentFloatValue(JsonParser p) throws JacksonException
    {
        NumberType t = p.getNumberType();
        if (t == NumberType.BIG_DECIMAL) {
            writeNumber(p.getDecimalValue());
        } else if (t == NumberType.FLOAT) {
            writeNumber(p.getFloatValue());
        } else {
            writeNumber(p.getDoubleValue());
        }
    }

    /**
     * Method for copying current {@link JsonToken#VALUE_NUMBER_FLOAT} value;
     * overridable by format backend implementations.
     * Implementation ensures it uses most accurate accessors necessary to retain
     * exact value in case of possible numeric conversion: in practice this means
     * that {@link BigDecimal} is usually used as the representation accessed from
     * {@link JsonParser}, regardless of whether {@link Double} might be accurate
     * (since detecting lossy conversion is not possible to do efficiently).
     * If minimal overhead is desired, use {@link #_copyCurrentFloatValue} instead.
     *
     * @param p Parser that points to the value to copy
     *
     * @since 2.15
     */
    protected void _copyCurrentFloatValueExact(JsonParser p) throws JacksonException
    {
        Number n = p.getNumberValueExact();
        if (n instanceof BigDecimal) {
            writeNumber((BigDecimal) n);
        } else if (n instanceof Double) {
            writeNumber(n.doubleValue());
        } else {
            writeNumber(n.floatValue());
        }
    }

    /**
     * Method for copying current {@link JsonToken#VALUE_NUMBER_FLOAT} value;
     * overridable by format backend implementations.
     *
     * @param p Parser that points to the value to copy
     */
    protected void _copyCurrentIntValue(JsonParser p) throws JacksonException
    {
        NumberType n = p.getNumberType();
        if (n == NumberType.INT) {
            writeNumber(p.getIntValue());
        } else if (n == NumberType.LONG) {
            writeNumber(p.getLongValue());
        } else {
            writeNumber(p.getBigIntegerValue());
        }
    }

    /**
     * Method for copying current {@link JsonToken#VALUE_STRING} value;
     * overridable by format backend implementations.
     *
     * @param p Parser that points to the value to copy
     */
    protected void _copyCurrentStringValue(JsonParser p) throws JacksonException
    {
        if (p.hasStringCharacters()) {
            writeString(p.getStringCharacters(), p.getStringOffset(), p.getStringLength());
        } else {
            writeString(p.getString());
        }
    }

    /*
    /**********************************************************************
    /* Public API, buffer handling
    /**********************************************************************
     */

    /**
     * Method called to flush any buffered content to the underlying
     * target (output stream, writer), and to flush the target itself
     * as well.
     */
    @Override
    public abstract void flush();

    /**
     * Method that can be called to determine whether this generator
     * is closed or not. If it is closed, no more output can be done.
     *
     * @return {@code True} if this generator has been closed; {@code false} if not
     */
    public abstract boolean isClosed();

    /*
    /**********************************************************************
    /* Closeable implementation
    /**********************************************************************
     */

    /**
     * Method called to close this generator, so that no more content
     * can be written.
     *<p>
     * Whether the underlying target (stream, writer) gets closed depends
     * on whether this generator either manages the target (i.e. is the
     * only one with access to the target -- case if caller passes a
     * reference to the resource such as File, but not stream); or
     * has feature {@link StreamWriteFeature#AUTO_CLOSE_TARGET} enabled.
     * If either of above is true, the target is also closed. Otherwise
     * (not managing, feature not enabled), target is not closed.
     */
    @Override
    public abstract void close();

    /*
    /**********************************************************************
    /* Helper methods for sub-classes
    /*
    /* NOTE: some could be moved out in 3.0 if there was "JsonGeneratorMinimalBase"
    /**********************************************************************
     */

    /**
     * Helper method used for constructing and throwing
     * {@link StreamWriteException} with given message.
     *<p>
     * Note that sub-classes may override this method to add more detail
     * or use a {@link StreamWriteException} sub-class.
     *
     * @param <T> Bogus type parameter to "return anything" so that compiler
     *   won't complain when chaining calls
     *
     * @param msg Message to construct exception with
     *
     * @return Does not return at all as exception is always thrown, but nominally returns "anything"
     *
     * @throws StreamWriteException that was constructed with given message
     */
    protected <T> T _reportError(String msg) throws StreamWriteException {
        throw _constructWriteException(msg);
    }

    protected <T> T _reportUnsupportedOperation() {
        return _reportUnsupportedOperation("Operation not supported by `JsonGenerator` of type "+getClass().getName());
    }

    protected <T> T _reportUnsupportedOperation(String msg) {
        throw new UnsupportedOperationException(msg);
    }

    /**
     * Helper method used for constructing and throwing
     * {@link StreamWriteException} with given message, in cases where
     * argument(s) used for a call (usually one of {@code writeXxx()} methods)
     * is invalid.
     * Default implementation simply delegates to {@link #_reportError(String)}.
     *
     * @param <T> Bogus type parameter to "return anything" so that compiler
     *   won't complain when chaining calls
     *
     * @param msg Message to construct exception with
     *
     * @return Does not return at all as exception is always thrown, but nominally returns "anything"
     *
     * @throws StreamWriteException that was constructed with given message
     */
    protected <T> T _reportArgumentError(String msg) throws StreamWriteException {
        return _reportError(msg);
    }

    // @since 3.0
    protected StreamWriteException _constructWriteException(String msg) {
        return new StreamWriteException(this, msg);
    }

    protected StreamWriteException _constructWriteException(String msg, Object arg) {
        return _constructWriteException(String.format(msg, arg));
    }

    protected StreamWriteException _constructWriteException(String msg, Object arg1, Object arg2) {
        return _constructWriteException(String.format(msg, arg1, arg2));
    }

    protected StreamWriteException _constructWriteException(String msg, Throwable t) {
        return new StreamWriteException(this, msg, t);
    }

    // @since 3.0
    protected JacksonException _wrapIOFailure(IOException e) {
        return JacksonIOException.construct(e, this);
    }

    protected final void _verifyOffsets(int arrayLength, int offset, int length)
    {
        if ((offset < 0) || (offset + length) > arrayLength) {
            throw new IllegalArgumentException(String.format(
                    "invalid argument(s) (offset=%d, length=%d) for input array of %d element",
                    offset, length, arrayLength));
        }
    }
}
