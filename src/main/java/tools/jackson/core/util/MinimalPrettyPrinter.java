package tools.jackson.core.util;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.PrettyPrinter;
import tools.jackson.core.util.Separators.Spacing;

/**
 * {@link PrettyPrinter} implementation that adds no indentation,
 * just implements everything necessary for value output to work
 * as expected, and provide simpler extension points to allow
 * for creating simple custom implementations that add specific
 * decoration or overrides. Since behavior then is very similar
 * to using no pretty printer at all, usually sub-classes are used.
 *<p>
 * Beyond purely minimal implementation, there is limited amount of
 * configurability which may be useful for actual use: for example,
 * it is possible to redefine separator used between root-level
 * values (default is single space; can be changed to line-feed).
 *<p>
 * Note: does NOT implement {@link Instantiatable} since this is
 * a stateless implementation; that is, a single instance can be
 * shared between threads.
 */
public class MinimalPrettyPrinter
    implements PrettyPrinter, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected String _rootValueSeparator;

    protected Separators _separators;

    /*
    /**********************************************************************
    /* Life-cycle, construction, configuration
    /**********************************************************************
     */

    public MinimalPrettyPrinter() {
        this(DEFAULT_ROOT_VALUE_SEPARATOR.toString());
    }

    public MinimalPrettyPrinter(String rootValueSeparator) {
        _rootValueSeparator = rootValueSeparator;
        _separators = DEFAULT_SEPARATORS.withObjectNameValueSpacing(Spacing.NONE);
    }

    public void setRootValueSeparator(String sep) {
        _rootValueSeparator = sep;
    }

    /**
     * @param separators Separator definitions
     *
     * @return This pretty-printer instance to allow call chaining
     */
    public MinimalPrettyPrinter setSeparators(Separators separators) {
        _separators = separators;
        return this;
    }

    /*
    /**********************************************************************
    /* PrettyPrinter impl
    /**********************************************************************
     */

    @Override
    public void writeRootValueSeparator(JsonGenerator g) throws JacksonException
    {
        if (_rootValueSeparator != null) {
            g.writeRaw(_rootValueSeparator);
        }
    }

    @Override
    public void writeStartObject(JsonGenerator g) throws JacksonException
    {
        g.writeRaw('{');
    }

    @Override
    public void beforeObjectEntries(JsonGenerator g) throws JacksonException
    {
        // nothing special, since no indentation is added
    }

    /**
     * Method called after the Object property name has been output, but
     * before the value is output.
     *<p>
     * Default handling will just output a single
     * colon to separate the two, without additional spaces.
     */
    @Override
    public void writeObjectNameValueSeparator(JsonGenerator g) throws JacksonException
    {
        g.writeRaw(_separators.getObjectNameValueSeparator());
    }

    /**
     * Method called after an object entry (field:value) has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate the two.
     */
    @Override
    public void writeObjectEntrySeparator(JsonGenerator g) throws JacksonException
    {
        g.writeRaw(_separators.getObjectEntrySeparator());
    }

    @Override
    public void writeEndObject(JsonGenerator g, int nrOfEntries) throws JacksonException
    {
        g.writeRaw('}');
    }

    @Override
    public void writeStartArray(JsonGenerator g) throws JacksonException
    {
        g.writeRaw('[');
    }

    @Override
    public void beforeArrayValues(JsonGenerator g) throws JacksonException
    {
        // nothing special, since no indentation is added
    }

    /**
     * Method called after an array value has been completely
     * output, and before another value is to be output.
     *<p>
     * Default handling (without pretty-printing) will output a single
     * comma to separate values.
     */
    @Override
    public void writeArrayValueSeparator(JsonGenerator g) throws JacksonException
    {
        g.writeRaw(_separators.getArrayElementSeparator());
    }

    @Override
    public void writeEndArray(JsonGenerator g, int nrOfValues) throws JacksonException
    {
        g.writeRaw(']');
    }
}
