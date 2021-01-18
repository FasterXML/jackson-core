/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.exc.WrappedIOException;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.core.type.WritableTypeId.Inclusion;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;

import static com.fasterxml.jackson.core.JsonTokenId.*;

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
    /* Public API, output configuration, state access
    /**********************************************************************
     */

    /**
     * Accessor for context object that provides information about low-level
     * logical position withing output token stream.
     *
     * @return Stream output context ({@link TokenStreamContext}) associated with this generator
     */
    public abstract TokenStreamContext getOutputContext();

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
    public abstract ObjectWriteContext getObjectWriteContext();

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
     *
     * @return Output target this generator was configured with
     */
    public abstract Object getOutputTarget();

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
     *
     * @return Amount of content buffered in internal units, if amount known and
     *    accessible; -1 if not accessible.
     */
    public abstract int getOutputBuffered();

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getOutputContext().getCurrentValue();
     *</code>
     *<p>
     * Note that "current value" is NOT populated (or used) by Streaming generator;
     * it is only used by higher-level data-binding functionality.
     * The reason it is included here is that it can be stored and accessed hierarchically,
     * and gets passed through data-binding.
     *
     * @return "Current value" for the current context this generator has
     */
    public abstract Object getCurrentValue();

    /**
     * Helper method, usually equivalent to:
     *<code>
     *   getOutputContext().setCurrentValue(v);
     *</code>
     * used to assign "current value" for the current context of this generator.
     * It is usually assigned and used by higher level data-binding functionality
     * (instead of streaming parsers/generators) but is stored at streaming level.
     *
     * @param v "Current value" to assign to the current output context of this generator
     */
    public abstract void setCurrentValue(Object v);

    /*
    /**********************************************************************
    /* Public API, Feature configuration
    /**********************************************************************
     */

    /**
     * Method for enabling or disabling specified feature:
     * check {@link StreamWriteFeature} for list of available features.
     *<p>
     * NOTE: mostly left in 3.0 just to support disabling of
     * {@link StreamWriteFeature#AUTO_CLOSE_CONTENT}.
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
     *
     * @return This generator, to allow call chaining
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
     * in case it can not use native object ids.
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
     * in case it can not use native type ids.
     *
     * @return {@code True} if this generator is capable of writing "native" Type Ids
     *   (which is typically determined by capabilities of the underlying format),
     *   {@code false} if not
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
     * @return {@code True} if this generator is capable of writing "raw" Binary
     *   Content
     *   (this is typically determined by capabilities of the underlying format);
     *   {@code false} if not
     */
    public boolean canWriteBinaryNatively() { return false; }
    
    /**
     * Introspection method to call to check whether it is ok to omit
     * writing of Object fields or not. Most formats do allow omission,
     * but certain positional formats (such as CSV) require output of
     * placeholders, even if no real values are to be emitted.
     *
     * @return {@code True} if this generator is allowed to only write values
     *   of some Object fields and omit the rest; {@code false} if not
     */
    public boolean canOmitFields() { return true; }

    /**
     * Introspection method to call to check whether it is possible
     * to write numbers using {@link #writeNumber(java.lang.String)}
     * using possible custom format, or not. Typically textual formats
     * allow this (and JSON specifically does), whereas binary formats
     * do not allow this (except by writing them as Strings).
     * Usual reason for calling this method is to check whether custom
     * formatting of numbers may be applied by higher-level code (databinding)
     * or not.
     *
     * @return {@code True} if this generator is capable of writing "formatted"
     *   numbers (and if so, need to be passed using
     *   {@link #writeNumber(String)}, that is, passed as {@code String});
     *   {@code false} if not
     */
    public boolean canWriteFormattedNumbers() { return false; }

    /**
     * Accessor for getting metadata on capabilities of this generator, based on
     * underlying data format being read (directly or indirectly).
     *
     * @return Set of read capabilities for content to generate via this generator
     */
    public abstract JacksonFeatureSet<StreamWriteCapability> getWriteCapabilities();

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
     * a field name is expected.
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeStartArray() throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeStartArray(Object currentValue) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeStartArray(Object currentValue, int size) throws JacksonException;

    /**
     * Method for writing closing marker of a JSON Array value
     * (character ']'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Marker can be written if the innermost structured type
     * is Array.
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeEndArray() throws JacksonException;

    /**
     * Method for writing starting marker of an Object value
     * (character '{'; plus possible white space decoration
     * if pretty-printing is enabled).
     *<p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a field name is expected.
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeStartObject() throws JacksonException;

    /**
     * Method for writing starting marker of an Object value
     * to represent the given Java Object value.
     * Argument is offered as metadata, but more
     * importantly it should be assigned as the "current value"
     * for the Object content that gets constructed and initialized.
     *<p>
     * Object values can be written in any context where values
     * are allowed: meaning everywhere except for when
     * a field name is expected.
     *
     * @param currentValue Java Object that Object being written represents, if any
     *    (or {@code null} if not known or not applicable)
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeStartObject(Object currentValue) throws JacksonException;

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
     * a field name is expected.
     *
     * @param forValue Object value to be written (assigned as "current value" for
     *    the Object context that gets created)
     * @param size Number of key/value pairs this Object will have: actual
     *   number of entries written (before matching call to
     *   {@link #writeEndObject()} MUST match; generator MAY verify
     *   this is the case (and SHOULD if format itself encodes length)
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeStartObject(Object forValue, int size) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeEndObject() throws JacksonException;

    /**
     * Method for writing a field name (JSON String surrounded by
     * double quotes: syntactically identical to a JSON String value),
     * possibly decorated by white space if pretty-printing is enabled.
     *<p>
     * Field names can only be written in Object context (check out
     * JSON specification for details), when field name is expected
     * (field names alternate with values).
     *
     * @param name Field name to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeFieldName(String name) throws JacksonException;

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
     *
     * @param name Pre-encoded field name to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeFieldName(SerializableString name) throws JacksonException;

    /**
     * Alternative to {@link #writeFieldName(String)} that may be used
     * in cases where property key is of numeric type; either where
     * underlying format supports such notion (some binary formats do,
     * unlike JSON), or for convenient conversion into String presentation.
     * Default implementation will simply convert id into <code>String</code>
     * and call {@link #writeFieldName(String)}.
     *
     * @param id Field id to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeFieldId(long id) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeArray(int[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeArray(long[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeArray(double[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeNumber(array[i]);
        }
        writeEndArray();
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeArray(String[] array, int offset, int length) throws JacksonException
    {
        Objects.requireNonNull(array, "null 'array' argument");
        _verifyOffsets(array.length, offset, length);
        writeStartArray(array, length);
        for (int i = offset, end = offset+length; i < end; ++i) {
            writeString(array[i]);
        }
        writeEndArray();
    }

    /*
    /**********************************************************************
    /* Public API, write methods, text/String values
    /**********************************************************************
     */

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     *
     * @param value String value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeString(String value) throws JacksonException;

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     * If {@code len} is &lt; 0, then write all contents of the reader.
     * Otherwise, write only len characters.
     *<p>
     * Note: actual length of content available may exceed {@code len} but
     * can not be less than it: if not enough content available, a
     * {@link JsonGenerationException} will be thrown.
     *
     * @param reader Reader to use for reading Text value to write
     * @param len Maximum Length of Text value to read (in {@code char}s, non-negative)
     *    if known; {@code -1} to indicate "read and write it all"
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     *    (including the case where {@code reader} does not provide enough content)
     */
    public abstract void writeString(Reader reader, int len) throws JacksonException;

    /**
     * Method for outputting a String value. Depending on context
     * this means either array element, (object) field value or
     * a stand alone String; but in all cases, String will be
     * surrounded in double quotes, and contents will be properly
     * escaped as required by JSON specification.
     *
     * @param buffer Buffer that contains String value to write
     * @param offset Offset in {@code buffer} of the first character of String value to write
     * @param len Length of the String value (in characters) to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeString(char[] buffer, int offset, int len) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeString(SerializableString value) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeRawUTF8String(byte[] buffer, int offset, int len)
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeUTF8String(byte[] buffer, int offset, int len)
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
     * @param text Textual contents to include as-is in output.
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeRaw(String text) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeRaw(String text, int offset, int len) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeRaw(char[] buffer, int offset, int len) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeRaw(char c) throws JacksonException;

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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeRaw(SerializableString raw) throws JacksonException {
        writeRaw(raw.getValue());
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeRawValue(String text) throws JacksonException;

    public abstract void writeRawValue(String text, int offset, int len) throws JacksonException;

    public abstract void writeRawValue(char[] text, int offset, int len) throws JacksonException;

    /**
     * Method similar to {@link #writeRawValue(String)}, but potentially more
     * efficient as it may be able to use pre-encoded content (similar to
     * {@link #writeRaw(SerializableString)}.
     *
     * @param raw Pre-encoded textual contents to included in output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeRawValue(SerializableString raw) throws JacksonException {
        writeRawValue(raw.getValue());
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
     * @param data Buffer that contains binary data to write
     * @param offset Offset in {@code data} of the first byte of data to write
     * @param len Length of data to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeBinary(Base64Variant bv,
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeBinary(byte[] data, int offset, int len) throws JacksonException {
        writeBinary(Base64Variants.getDefaultVariant(), data, offset, len);
    }

    /**
     * Similar to {@link #writeBinary(Base64Variant,byte[],int,int)},
     * but assumes default to using the Jackson default Base64 variant 
     * (which is {@link Base64Variants#MIME_NO_LINEFEEDS}). Also
     * assumes that whole byte array is to be output.
     *
     * @param data Buffer that contains binary data to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeBinary(byte[] data) throws JacksonException {
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
     *
     * @return Number of bytes actually written
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
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
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(short v) throws JacksonException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(int v) throws JacksonException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(long v) throws JacksonException;

    /**
     * Method for outputting given value as JSON number.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(BigInteger v) throws JacksonException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(double v) throws JacksonException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(float v) throws JacksonException;

    /**
     * Method for outputting indicate JSON numeric value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param v Number value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(BigDecimal v) throws JacksonException;

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
     * @param encodedValue Textual (possibly format) number representation to write
     *
     * @throws UnsupportedOperationException If underlying data format does not
     *   support numbers serialized textually AND if generator is not allowed
     *   to just output a String instead (Schema-based formats may require actual
     *   number, for example)
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNumber(String encodedValue) throws JacksonException;

    /**
     * Overloaded version of {@link #writeNumber(String)} with same semantics
     * but possibly more efficient operation.
     *
     * @param encodedValueBuffer Buffer that contains the textual number representation to write
     * @param offset Offset of the first character of value to write
     * @param len Length of the value (in characters) to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeNumber(char[] encodedValueBuffer, int offset, int len) throws JacksonException {
        writeNumber(new String(encodedValueBuffer, offset, len));
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
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @param state Boolean value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeBoolean(boolean state) throws JacksonException;

    /**
     * Method for outputting literal JSON null value.
     * Can be called in any context where a value is expected
     * (Array value, Object field value, root-level value).
     * Additional white space may be added around the value
     * if pretty-printing is enabled.
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeNull() throws JacksonException;

    /**
     * Method that can be called on backends that support passing opaque native
     * values that some data formats support; not used with JSON backend,
     * more common with binary formats.
     *<p>
     * NOTE: this is NOT the method to call for serializing regular POJOs,
     * see {@link #writeObject} instead.
     *
     * @param object Native format-specific value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeEmbeddedObject(Object object) throws JacksonException {
        // 01-Sep-2016, tatu: As per [core#318], handle small number of cases
        if (object == null) {
            writeNull();
            return;
        }
        if (object instanceof byte[]) {
            writeBinary((byte[]) object);
            return;
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream;
     *   typically if Object ID output is not allowed
     *   (either at all, or specifically in this position in output)
     */
    public void writeObjectId(Object id) throws JacksonException {
        throw _constructWriteException("No native support for writing Object Ids");
    }

    /**
     * Method that can be called to output references to native Object Ids.
     * Note that it may only be called after ensuring this is legal
     * (with {@link #canWriteObjectId()}), as not all data formats
     * have native type id support; and some may only allow them in
     * certain positions or locations.
     * If output is not allowed by the data format in this position,
     * a {@link JsonGenerationException} will be thrown.
     *
     * @param referenced Referenced value, for which Object Id is expected to be written
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream;
     *   typically if Object ID output is not allowed
     *   (either at all, or specifically in this position in output)
     */
    public void writeObjectRef(Object referenced) throws JacksonException {
        throw _constructWriteException("No native support for writing Object Ids");
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
     * @param id Native Type Id to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeTypeId(Object id) throws JacksonException {
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
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public WritableTypeId writeTypePrefix(WritableTypeId typeIdDef)
        throws JacksonException
    {
        Object id = typeIdDef.id;

        final JsonToken valueShape = typeIdDef.valueShape;
        if (canWriteTypeId()) {
            typeIdDef.wrapperWritten = false;
            // just rely on native type output method (sub-classes likely to override)
            writeTypeId(id);
        } else {
            // No native type id; write wrappers
            // Normally we only support String type ids (non-String reserved for native type ids)
            String idStr = (id instanceof String) ? (String) id : String.valueOf(id);
            typeIdDef.wrapperWritten = true;

            Inclusion incl = typeIdDef.include;
            // first: can not output "as property" if value not Object; if so, must do "as array"
            if ((valueShape != JsonToken.START_OBJECT)
                    && incl.requiresObjectContext()) {
                typeIdDef.include = incl = WritableTypeId.Inclusion.WRAPPER_ARRAY;
            }
            
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
                writeStringField(typeIdDef.asProperty, idStr);
                return typeIdDef;

            case WRAPPER_OBJECT:
                // NOTE: this is wrapper, not directly related to value to output, so don't pass
                writeStartObject();
                writeFieldName(idStr);
                break;
            case WRAPPER_ARRAY:
            default: // should never occur but translate as "as-array"
                writeStartArray(); // wrapper, not actual array object to write
                writeString(idStr);
            }
        }
        // and finally possible start marker for value itself:
        if (valueShape == JsonToken.START_OBJECT) {
            writeStartObject(typeIdDef.forValue);
        } else if (valueShape == JsonToken.START_ARRAY) {
            // should we now set the current object?
            writeStartArray();
        }
        return typeIdDef;
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
                    writeStringField(typeIdDef.asProperty, idStr);
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
     * stream this generator manages.
     * This is done by delegating call to
     * {@link ObjectWriteContext#writeValue(JsonGenerator, Object)}.
     *
     * @param pojo General POJO value to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeObject(Object pojo) throws JacksonException;

    /**
     * Method for writing given JSON tree (expressed as a tree
     * where given {@code TreeNode} is the root) using this generator.
     * This is done by delegating call to
     * {@link ObjectWriteContext#writeTree}.
     *
     * @param rootNode {@link TreeNode} to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public abstract void writeTree(TreeNode rootNode) throws JacksonException;

    /*
    /**********************************************************************
    /* Public API, convenience field write methods
    /**********************************************************************
     */

    // 25-May-2020, tatu: NOTE! Made `final` on purpose in 3.x to prevent issues
    //    rising from complexity of overriding only some of methods (writeFieldName()
    //    and matching writeXxx() for value)

    /**
     * Convenience method for outputting a field entry ("member")
     * that contains specified data in base64-encoded form.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeBinary(value);
     *</pre>
     *
     * @param fieldName Name of Object field to write
     * @param data Binary value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeBinaryField(String fieldName, byte[] data) throws JacksonException {
        writeFieldName(fieldName);
        writeBinary(data);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has a boolean value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeBoolean(value);
     *</pre>
     *
     * @param fieldName Name of Object field to write
     * @param value Boolean value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeBooleanField(String fieldName, boolean value) throws JacksonException {
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
     *
     * @param fieldName Name of the null-valued field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNullField(String fieldName) throws JacksonException {
        writeFieldName(fieldName);
        writeNull();
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has a String value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeString(value);
     *</pre>
     *
     * @param fieldName Name of the field to write
     * @param value String value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeStringField(String fieldName, String value) throws JacksonException {
        writeFieldName(fieldName);
        writeString(value);
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * that has the specified numeric value. Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeNumber(value);
     *</pre>
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, short value) throws JacksonException {
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
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, int value) throws JacksonException {
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
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, long value) throws JacksonException {
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
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, BigInteger value) throws JacksonException {
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
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, float value) throws JacksonException {
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
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, double value) throws JacksonException {
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
     *
     * @param fieldName Name of the field to write
     * @param value Numeric value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeNumberField(String fieldName, BigDecimal value) throws JacksonException {
        writeFieldName(fieldName);
        writeNumber(value);
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
     *
     * @param fieldName Name of the Array field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeArrayFieldStart(String fieldName) throws JacksonException {
        writeFieldName(fieldName);
        writeStartArray();
    }

    /**
     * Convenience method for outputting a field entry ("member")
     * (that will contain an Object value), and the START_OBJECT marker.
     * Equivalent to:
     *<pre>
     *  writeFieldName(fieldName);
     *  writeStartObject();
     *</pre>
     *<p>
     * Note: caller still has to take care to close the Object
     * (by calling {#link #writeEndObject}) after writing all
     * entries of the value Object.
     *
     * @param fieldName Name of the Object field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeObjectFieldStart(String fieldName) throws JacksonException {
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
     *<p>
     * NOTE: see {@link #writeObject(Object)} for details on how POJO value actually
     * gets written (uses delegation)
     *
     * @param fieldName Name of the field to write
     * @param pojo POJO value of the field to write
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public final void writeObjectField(String fieldName, Object pojo) throws JacksonException {
        writeFieldName(fieldName);
        writeObject(pojo);
    }

    // // // But this method does need to be delegate so...
    
    /**
     * Method called to indicate that a property in this position was
     * skipped. It is usually only called for generators that return
     * <code>false</code> from {@link #canOmitFields()}.
     *<p>
     * Default implementation does nothing.
     *
     * @param fieldName Name of the field that is being omitted
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void writeOmittedField(String fieldName) throws JacksonException { }

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
     * @throws WrappedIOException if there is an underlying I/O problem (reading or writing)
     * @throws StreamReadException for problems with decoding of token stream
     * @throws JsonGenerationException for problems in encoding token stream
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
        case ID_FIELD_NAME:
            writeFieldName(p.currentName());
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
     *
     * @param p Parser that points to the value to copy
     *
     * @throws WrappedIOException if there is an underlying I/O problem (reading or writing)
     * @throws StreamReadException for problems with decoding of token stream
     * @throws JsonGenerationException for problems in encoding token stream
     */
    public void copyCurrentStructure(JsonParser p) throws JacksonException
    {
        JsonToken t = p.currentToken();
        // Let's handle field-name separately first
        int id = (t == null) ? ID_NOT_AVAILABLE : t.id();
        if (id == ID_FIELD_NAME) {
            writeFieldName(p.currentName());
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

    protected void _copyCurrentContents(JsonParser p) throws JacksonException
    {
        int depth = 1;
        JsonToken t;

        // Mostly copied from `copyCurrentEvent()`, but with added nesting counts
        while ((t = p.nextToken()) != null) {
            switch (t.id()) {
            case ID_FIELD_NAME:
                writeFieldName(p.currentName());
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
                throw new IllegalStateException("Internal error: unknown current token, "+t);
            }
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
     * {@link JsonGenerationException} with given base message.
     *<p>
     * Note that sub-classes may override this method to add more detail
     * or use a {@link JsonGenerationException} sub-class.
     *
     * @param <T> Bogus type parameter to "return anything" so that compiler
     *   won't complain when chaining calls
     *
     * @param msg Message to construct exception with
     *
     * @return Does not return at all as exception is always thrown, but nominally returns "anything"
     *
     * @throws JsonGenerationException that was constructed with given message
     */
    protected <T> T _reportError(String msg) throws JsonGenerationException {
        throw _constructWriteException(msg);
    }

    protected <T> T _reportUnsupportedOperation() {
        throw new UnsupportedOperationException("Operation not supported by generator of type "+getClass().getName());
    }

    // @since 3.0
    protected JsonGenerationException _constructWriteException(String msg) {
        return new JsonGenerationException(msg, this);
    }

    protected JsonGenerationException _constructWriteException(String msg, Object arg) {
        return new JsonGenerationException(String.format(msg, arg), this);
    }

    protected JsonGenerationException _constructWriteException(String msg, Object arg1, Object arg2) {
        return new JsonGenerationException(String.format(msg, arg1, arg2), this);
    }

    protected JsonGenerationException _constructWriteException(String msg, Throwable t) {
        return new JsonGenerationException(msg, t, this);
    }

    // @since 3.0
    protected JacksonException _wrapIOFailure(IOException e) {
        return WrappedIOException.construct(e);
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
