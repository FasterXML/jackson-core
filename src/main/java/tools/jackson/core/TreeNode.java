/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package tools.jackson.core;

import java.util.Iterator;

/**
 * Marker interface used to denote JSON Tree nodes, as far as
 * the core package knows them (which is very little): mostly
 * needed to allow {@link ObjectReadContext} and {@link ObjectWriteContext}
 * to have some level of interoperability.
 * Most functionality is within {@code JsonNode}
 * base class in {@code databind} package.
 */
public interface TreeNode
{
    /*
    /**********************************************************************
    /* Minimal introspection methods
    /**********************************************************************
     */

    /**
     * Method that can be used for efficient type detection
     * when using stream abstraction for traversing nodes.
     * Will return the first {@link JsonToken} that equivalent
     * stream event would produce (for most nodes there is just
     * one token but for structured/container types multiple)
     *
     * @return {@link JsonToken} that is most closely associated with the node type
     */
    JsonToken asToken();

    /**
     * If this node is a numeric type (as per {@link JsonToken#isNumeric}),
     * returns native type that node uses to store the numeric value;
     * otherwise returns null.
     *
     * @return Type of number contained, if any; or null if node does not
     *  contain numeric value.
     */
    JsonParser.NumberType numberType();

    /**
     * Method that returns number of child nodes this node contains:
     * for Array nodes, number of child elements, for Object nodes,
     * number of properties, and for all other nodes 0.
     *
     * @return For non-container nodes returns 0; for arrays number of
     *   contained elements, and for objects number of properties.
     */
    int size();

    /**
     * Method that returns true for all value nodes: ones that
     * are not containers, and that do not represent "missing" nodes
     * in the path. Such value nodes represent String, Number, Boolean
     * and null values from JSON.
     *<p>
     * Note: one and only one of methods {@link #isValueNode},
     * {@link #isContainerNode} and {@link #isMissingNode} ever
     * returns true for any given node.
     *
     * @return True if this node is considered a value node; something that
     *    represents either a scalar value or explicit {@code null}
     */
    boolean isValueNode();

    /**
     * Method that returns true for container nodes: Arrays and Objects.
     *<p>
     * Note: one and only one of methods {@link #isValueNode},
     * {@link #isContainerNode} and {@link #isMissingNode} ever
     * returns true for any given node.
     *
     * @return {@code True} for Array and Object nodes, {@code false} otherwise
     */
    boolean isContainerNode();

    /**
     * Method that returns true for "virtual" nodes which represent
     * missing entries constructed by path accessor methods when
     * there is no actual node matching given criteria.
     *<p>
     * Note: one and only one of methods {@link #isValueNode},
     * {@link #isContainerNode} and {@link #isMissingNode} ever
     * returns true for any given node.
     *
     * @return {@code True} if this node represents a "missing" node
     */
    boolean isMissingNode();

    /**
     * Method that returns true if this node is an Array node, false
     * otherwise.
     * Note that if true is returned, {@link #isContainerNode}
     * must also return true.
     *
     * @return {@code True} for Array nodes, {@code false} for everything else
     */
    boolean isArray();

    /**
     * Method that returns true if this node is an Object node, false
     * otherwise.
     * Note that if true is returned, {@link #isContainerNode}
     * must also return true.
     *
     * @return {@code True} for Object nodes, {@code false} for everything else
     */
    boolean isObject();

    /**
     * Method that returns true if this node is a node that represents
     * logical {@code null} value.
     *
     * @return {@code True} for nodes representing explicit input {@code null},
     *    {@code false} for everything else
     *
     * @since 3.0
     */
    boolean isNull();

    /**
     * Method that returns true if this node represents an embedded
     * "foreign" (or perhaps native?) object (like POJO), not represented
     * as regular content. Such nodes are used to pass information that
     * either native format can not express as-is, metadata not included within
     * at all, or something else that requires special handling.
     *
     * @return {@code True} for nodes representing "embedded" (or format-specific, native)
     *    value -- ones that streaming api exposes as {@link JsonToken#VALUE_EMBEDDED_OBJECT}
     *    -- {@code false} for other nodes
     *
     * @since 3.0
     */
    boolean isEmbeddedValue();

    /*
    /**********************************************************************
    /* Basic traversal through structured entries (Arrays, Objects)
    /**********************************************************************
     */

    /**
     * Method for accessing value of the specified property of
     * an Object node. If this node is not an Object (or it
     * does not have a value for specified property) {@code null} is returned.
     *<p>
     * NOTE: handling of explicit null values may vary between
     * implementations; some trees may retain explicit nulls, others not.
     *
     * @param propertyName Name of the property to access
     *
     * @return Node that represent value of the specified property,
     *   if this node is an Object and has value for the specified
     *   property; {@code null} otherwise.
     */
    TreeNode get(String propertyName);

    /**
     * Method for accessing value of the specified element of
     * an array node. For other nodes, null is returned.
     *<p>
     * For array nodes, index specifies
     * exact location within array and allows for efficient iteration
     * over child elements (underlying storage is guaranteed to
     * be efficiently indexable, i.e. has random-access to elements).
     * If index is less than 0, or equal-or-greater than
     * <code>node.size()</code>, null is returned; no exception is
     * thrown for any index.
     *
     * @param index Index of the Array node element to access
     *
     * @return Node that represent value of the specified element,
     *   if this node is an array and has specified element;
     *   {@code null} otherwise.
     */
    TreeNode get(int index);

    /**
     * Method for accessing value of the specified property of
     * an Object node.
     * For other nodes, a "missing node" (virtual node
     * for which {@link #isMissingNode} returns true) is returned.
     *
     * @param propertyName Name of the property to access
     *
     * @return Node that represent value of the specified Object property,
     *   if this node is an object and has value for the specified property;
     *   otherwise "missing node" is returned.
     */
    TreeNode path(String propertyName);

    /**
     * Method for accessing value of the specified element of
     * an array node.
     * For other nodes, a "missing node" (virtual node
     * for which {@link #isMissingNode} returns true) is returned.
     *<p>
     * For array nodes, index specifies
     * exact location within array and allows for efficient iteration
     * over child elements (underlying storage is guaranteed to
     * be efficiently indexable, i.e. has random-access to elements).
     * If index is less than 0, or equal-or-greater than
     * <code>node.size()</code>, "missing node" is returned; no exception is
     * thrown for any index.
     *
     * @param index Index of the Array node element to access
     *
     * @return Node that represent value of the specified element,
     *   if this node is an array and has specified element;
     *   otherwise "missing node" is returned.
     */
    TreeNode path(int index);

    /**
     * Method for accessing names of all properties for this node, iff
     * this node is an Object node. Number of property names accessible
     * will be {@link #size}.
     *
     * @return An iterator for traversing names of all properties this Object node
     *   has (if Object node); empty {@link Iterator} otherwise (never {@code null}).
     */
    Iterator<String> propertyNames();

    /**
     * Method for locating node specified by given JSON pointer instances.
     * Method will never return null; if no matching node exists,
     * will return a node for which {@link TreeNode#isMissingNode()} returns true.
     *
     * @param ptr {@link JsonPointer} expression for descendant node to return
     *
     * @return Node that matches given JSON Pointer, if any: if no match exists,
     *   will return a "missing" node (for which {@link TreeNode#isMissingNode()}
     *   returns {@code true}).
     */
    TreeNode at(JsonPointer ptr);

    /**
     * Convenience method that is functionally equivalent to:
     *<pre>
     *   return at(JsonPointer.valueOf(jsonPointerExpression));
     *</pre>
     *<p>
     * Note that if the same expression is used often, it is preferable to construct
     * {@link JsonPointer} instance once and reuse it: this method will not perform
     * any caching of compiled expressions.
     *
     * @param ptrExpr Expression to compile as a {@link JsonPointer}
     *   instance
     *
     * @return Node that matches given JSON Pointer, if any: if no match exists,
     *   will return a "missing" node (for which {@link TreeNode#isMissingNode()}
     *   returns {@code true}).
     */
    TreeNode at(String ptrExpr) throws IllegalArgumentException;

    /*
    /**********************************************************************
    /* Converting to/from Streaming API
    /**********************************************************************
     */

    /**
     * Method for constructing a {@link JsonParser} instance for
     * iterating over contents of the tree that this node is root of.
     * Functionally equivalent to first serializing tree and then re-parsing but
     * more efficient.
     *<p>
     * NOTE: constructed parser instance will NOT initially point to a token,
     * so before passing it to deserializers, it is typically necessary to
     * advance it to the first available token by calling {@link JsonParser#nextToken()}.
     *
     * @param readCtxt {@link ObjectReadContext} to associate with parser constructed
     *  (to allow seamless databinding functionality)
     *
     * @return {@link JsonParser} that will stream over contents of this node
     */
    JsonParser traverse(ObjectReadContext readCtxt);
}
