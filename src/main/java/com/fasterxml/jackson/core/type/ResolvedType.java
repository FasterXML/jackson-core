package com.fasterxml.jackson.core.type;

/**
 * Type abstraction that represents Java type that has been resolved
 * (i.e. has all generic information, if any, resolved to concrete
 * types).
 * Note that this is an intermediate type, and all concrete instances
 * MUST be of type <code>JavaType</code> from "databind" bundle -- this
 * abstraction is only needed so that types can be passed through
 * {@link com.fasterxml.jackson.core.JsonParser#readValueAs} methods.
 * 
 * @since 2.0
 */
public abstract class ResolvedType
{
    /*
    /**********************************************************
    /* Public API, simple property accessors
    /**********************************************************
     */

    /**
     * Accessor for type-erased {@link Class} of resolved type.
     */
    public abstract Class<?> getRawClass();

    public abstract boolean hasRawClass(Class<?> clz);

    public abstract boolean isAbstract();
    
    public abstract boolean isConcrete();

    public abstract boolean isThrowable();

    public abstract boolean isArrayType();

    public abstract boolean isEnumType();

    public abstract boolean isInterface();

    public abstract boolean isPrimitive();

    public abstract boolean isFinal();

    public abstract boolean isContainerType();

    public abstract boolean isCollectionLikeType();

    public abstract boolean isMapLikeType();

    /*
    /**********************************************************
    /* Public API, type parameter access
    /**********************************************************
     */
    
    /**
     * Method that can be used to find out if the type directly declares generic
     * parameters (for its direct super-class and/or super-interfaces).
     */
    public abstract boolean hasGenericTypes();
    
    /**
     * Method for accessing key type for this type, assuming type
     * has such a concept (only Map types do)
     */
    public abstract ResolvedType getKeyType();

    /**
     * Method for accessing content type of this type, if type has
     * such a thing: simple types do not, structured types do
     * (like arrays, Collections and Maps)
     */
    public abstract ResolvedType getContentType();

    /**
     * Method for checking how many contained types this type
     * has. Contained types are usually generic types, so that
     * generic Maps have 2 contained types.
     */
    public abstract int containedTypeCount();

    /**
     * Method for accessing definitions of contained ("child")
     * types.
     * 
     * @param index Index of contained type to return
     * 
     * @return Contained type at index, or null if no such type
     *    exists (no exception thrown)
     */
    public abstract ResolvedType containedType(int index);
    
    /**
     * Method for accessing name of type variable in indicated
     * position. If no name is bound, will use placeholders (derived
     * from 0-based index); if no type variable or argument exists
     * with given index, null is returned.
     * 
     * @param index Index of contained type to return
     * 
     * @return Contained type at index, or null if no such type
     *    exists (no exception thrown)
     */
    public abstract String containedTypeName(int index);

    /*
    /**********************************************************
    /* Public API, other
    /**********************************************************
     */

    /**
     * Method that can be used to serialize type into form from which
     * it can be fully deserialized from at a later point (using
     * <code>TypeFactory</code> from mapper package).
     * For simple types this is same as calling
     * {@link Class#getName}, but for structured types it may additionally
     * contain type information about contents.
     */
    public abstract String toCanonical();
}
