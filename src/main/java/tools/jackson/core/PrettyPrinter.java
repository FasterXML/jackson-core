/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */

package tools.jackson.core;

import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.exc.WrappedIOException;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.util.Separators;

/**
 * Interface for objects that implement pretty printer functionality, such
 * as indentation.
 * Pretty printers are used to add white space in output JSON content,
 * to make results more human readable. Usually this means things like adding
 * linefeeds and indentation.
 *<p>
 * Note: stateful implementations MUST implement
 * {@link tools.jackson.core.util.Instantiatable} interface,
 * to allow for constructing  per-generation instances and avoid
 * state corruption.
 * Stateless implementations need not do this; but those are less common.
 */
public interface PrettyPrinter
{
    public final static Separators DEFAULT_SEPARATORS = Separators.createDefaultInstance();

    /**
     * Default String used for separating root values is single space.
     */
    public final static SerializedString DEFAULT_ROOT_VALUE_SEPARATOR = new SerializedString(" ");

    /*
    /**********************************************************************
    /* First methods that act both as events, and expect
    /* output for correct functioning (i.e something gets
    /* output even when not pretty-printing)
    /**********************************************************************
     */

    // // // Root-level handling:

    /**
     * Method called after a root-level value has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a space, to
     * allow values to be parsed correctly. Pretty-printer is
     * to output some other suitable and nice-looking separator
     * (tab(s), space(s), linefeed(s) or any combination thereof).
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeRootValueSeparator(JsonGenerator g) throws JacksonException;

    // // Object handling

    /**
     * Method called when an Object value is to be output, before
     * any fields are output.
     *<p>
     * Default handling (without pretty-printing) will output
     * the opening curly bracket.
     * Pretty-printer is
     * to output a curly bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeStartObject(JsonGenerator g) throws JacksonException;

    /**
     * Method called after an Object value has been completely output
     * (minus closing curly bracket).
     *<p>
     * Default handling (without pretty-printing) will output
     * the closing curly bracket.
     * Pretty-printer is
     * to output a curly bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param g Generator used for output
     * @param nrOfEntries Number of direct members of the Object that
     *   have been output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeEndObject(JsonGenerator g, int nrOfEntries) throws JacksonException;

    /**
     * Method called after an Object entry (field:value) has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeObjectEntrySeparator(JsonGenerator g) throws JacksonException;

    /**
     * Method called after an Object property name has been output, but
     * before the value is output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * colon to separate the two. Pretty-printer is
     * to output a colon as well, but can surround that with other
     * (white-space) decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeObjectNameValueSeparator(JsonGenerator g) throws JacksonException;

    // // // Array handling

    /**
     * Method called when an Array value is to be output, before
     * any member/child values are output.
     *<p>
     * Default handling (without pretty-printing) will output
     * the opening bracket.
     * Pretty-printer is
     * to output a bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeStartArray(JsonGenerator g) throws JacksonException;

    /**
     * Method called after an Array value has been completely output
     * (minus closing bracket).
     *<p>
     * Default handling (without pretty-printing) will output
     * the closing bracket.
     * Pretty-printer is
     * to output a bracket as well, but can surround that
     * with other (white-space) decoration.
     *
     * @param g Generator used for output
     * @param nrOfValues Number of direct members of the array that
     *   have been output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeEndArray(JsonGenerator g, int nrOfValues) throws JacksonException;

    /**
     * Method called after an array value has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two. Pretty-printer is
     * to output a comma as well, but can surround that with other
     * (white-space) decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void writeArrayValueSeparator(JsonGenerator g) throws JacksonException;

    /*
    /**********************************************************************
    /* Then events that by default do not produce any output but that are
    /* often overridden to add white space in pretty-printing mode
    /**********************************************************************
     */

    /**
     * Method called after array start marker has been output,
     * and right before the first value is to be output.
     * It is <b>not</b> called for arrays with no values.
     *<p>
     * Default handling does not output anything, but pretty-printer
     * is free to add any white space decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void beforeArrayValues(JsonGenerator g) throws JacksonException;

    /**
     * Method called after object start marker has been output,
     * and right before the Name of the first property is
     * to be output.
     * It is <b>not</b> called for objects without properties.
     *<p>
     * Default handling does not output anything, but pretty-printer
     * is free to add any white space decoration.
     *
     * @param g Generator used for output
     *
     * @throws WrappedIOException if there is an underlying I/O problem
     * @throws StreamWriteException for problems in encoding token stream
     */
    void beforeObjectEntries(JsonGenerator g) throws JacksonException;
}
