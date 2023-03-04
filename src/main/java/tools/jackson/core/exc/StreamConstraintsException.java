package tools.jackson.core.exc;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonLocation;

/**
 * Exception type used to indicate violations of stream constraints
 * (for example {@link tools.jackson.core.StreamReadConstraints})
 * when reading or writing content.
 */
public class StreamConstraintsException
    extends JacksonException
{
    private final static long serialVersionUID = 2L;

    public StreamConstraintsException(String msg) {
        super(msg);
    }

    public StreamConstraintsException(String msg, JsonLocation loc) {
        super(msg, loc, null);
    }

    // !!! TODO? Allow setting parser/generator
    @Override
    public Object processor() { return null; }
}
