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
     * If caller expects wrapping style to be used -- either "as Object" or "as Array" --
     * identifier of wrapping style; `null` if no wrapping is to be used.
     * Wrapping means use of additional wrapping structure (Array, Object) to contain
     * type id (as first array element, or as property name), and value for which type id
     * applies to be contained (as second array element, or as value for the property with
     * type id as name).
     *<p>
     * Valid values are `null`, {@link JsonToken#START_ARRAY}, {@link JsonToken#START_OBJECT}.
     */
    public JsonToken wrapStyle;

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
