package com.fasterxml.jackson.core.type;

import com.fasterxml.jackson.core.JsonToken;

/**
 * This is a simple value class used between core streaming and higher level
 * databinding to pass information about type ids to write.
 * Properties are exposed and mutable on purpose: they are only used for communication
 * over serialization of a single value, and neither retained across calls nor shared
 * between threads.
 *<p>
 * Usual usage pattern is such that instance of this class is passed on two calls that are
 * needed for outputting type id (and possible additional wrapping, depending on format;
 * JSON, for example, requires wrapping as type id is part of regular data): first, a "prefix"
 * write (which usually includes actual id), performed before value write; and then
 * matching "suffix" write after value serialization.
 *
 * @since 2.9
 */
public class WritableTypeId
{
    /**
     * Enumeration of values that matches enum `As` from annotation
     * `JsonTypeInfo`: separate definition to avoid dependency between
     * streaming core and annotations packages; also allows more flexibility
     * in case new values needed at this level of internal API.
     *<p>
     * NOTE: in most cases this only matters with formats that do NOT have native
     * type id capabilities, and require type id to be included within regular
     * data (whether exposed as Java properties or not). Formats with native
     * types usually use native type id functionality regardless, unless
     * overridden by a feature to use "non-native" type inclusion.
     */
    public enum Inclusion {
        /**
         * Inclusion as wrapper Array (1st element type id, 2nd element value).
         *<p>
         * Corresponds to <code>JsonTypeInfo.As.WRAPPER_ARRAY</code>.
         */
        WRAPPER_ARRAY,

        /**
         * Inclusion as wrapper Object that has one key/value pair where type id
         * is the key for typed value.
         *<p>
         * Corresponds to <code>JsonTypeInfo.As.WRAPPER_OBJECT</code>.
         */
        WRAPPER_OBJECT,

        /**
         * Inclusion as a property within Object to write (in case value is output
         * as Object); but if format has distinction between data, metadata, will
         * use latter (for example: attributes in XML) instead of data (XML elements).
         *<p>
         * NOTE: if shape of typed value to write is NOT Object, will instead use
         * {@link #WRAPPER_ARRAY} inclusion.
         *<p>
         * Corresponds to <code>JsonTypeInfo.As.PROPERTY</code>.
         */
        METADATA_PROPERTY,

        /**
         * Inclusion as a property within Object to write (in case value is output
         * as Object); but if format has distinction between data, metadata, will
         * use formetr (for example: Element in XML) instead of metadata (XML attribute).
         * In addition, it is possible that in some cases databinding may omit calling
         * type id writes for this case, and write them: if so, it will have to use
         * regular property write methods.
         *<p>
         * NOTE: if shape of typed value to write is NOT Object, will instead use
         * {@link #WRAPPER_ARRAY} inclusion.
         *<p>
         * Corresponds to <code>JsonTypeInfo.As.EXISTING_PROPERTY</code>.
         */
        PAYLOAD_PROPERTY,

        /**
         * Inclusion as a property within "parent" Object of value Object to write.
         * This typically requires slightly convoluted processing in which property
         * that contains type id is actually written <b>after</b> typed value object
         * itself is written.
         *<br />
         * Note that it is illegal to call write method if the current (parent) write context
         * is not Object: no coercion is done for other inclusion types (unlike with
         * other <code>xxx_PROPERTY</code> choices.
         * This also means that root values MAY NOT use this type id inclusion mechanism
         * (as they have no parent context).
         *<p>
         * Corresponds to <code>JsonTypeInfo.As.EXTERNAL_PROPERTY</code>.
         */
        PARENT_PROPERTY,
    }
    
    /**
     * Java object for which type id is being written. Not needed by default handling,
     * but may be useful for customized format handling.
     */
    public Object forValue;

    /**
     * (optional) Super-type of {@link #forValue} to use for type id generation (if no
     * explicit id passed): used instead of actual class of {@link #forValue} in cases
     * where we do not want to use the "real" type but something more generic, usually
     * to work around specific problem with implementation type, or its deserializer.
     */
    public Class<?> forValueType;

    /**
     * Actual type id to use: usually {link java.lang.String}.
     */
    public Object id;

    /**
     * If type id is to be embedded as a regular property, name of the property;
     * otherwise `null`.
     *<p>
     * NOTE: if "wrap-as-Object" is used, this does NOT contain property name to
     * use but `null`.
     */
    public String asProperty;

    /**
     * Property used to indicate style of inclusion for this type id, in cases where
     * no native type id may be used (either because format has none, like JSON; or
     * because use of native type ids is disabled [with YAML]).
     */
    public Inclusion include;

    /**
     * Information about intended shape of the value being written (that is, {@link #forValue});
     * in case of structured values, start token of the structure; for scalars, value token.
     * Main difference is between structured values
     * ({@link JsonToken#START_ARRAY}, {@link JsonToken#START_OBJECT})
     * and scalars ({@link JsonToken#VALUE_STRING}): specific scalar type may not be
     * important for processing.
     */
    public JsonToken valueShape;

    /**
     * Optional additional information that generator may add during "prefix write",
     * to be available on matching "suffix write".
     */
    public Object extra;
    
    public WritableTypeId() { }

    /**
     * Constructor used when calling a method for generating and writing Type Id;
     * caller only knows value object and its intended shape.
     */
    public WritableTypeId(Object value, JsonToken valueShape0) {
        this(value, valueShape0, null);
    }

    /**
     * Constructor used when calling a method for generating and writing Type Id,
     * but where actual type to use for generating id is NOT the type of value
     * (but its supertype).
     */
    public WritableTypeId(Object value, Class<?> valueType0, JsonToken valueShape0) {
        this(value, valueShape0, null);
        forValueType = valueType0;
    }

    /**
     * Constructor used when calling a method for writing Type Id;
     * caller knows value object, its intended shape as well as id to
     * use; but not details of wrapping (if any).
     */
    public WritableTypeId(Object value, JsonToken valueShape0, Object id0)
    {
        forValue = value;
        id = id0;
        valueShape = valueShape0;
    }
}
