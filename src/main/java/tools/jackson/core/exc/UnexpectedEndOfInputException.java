package tools.jackson.core.exc;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;

/**
 * Specialized {@link StreamReadException} that is thrown when end-of-input
 * is reached unexpectedly, usually within token being decoded, but possibly
 * within intervening non-token content (for formats that have that, such
 * as whitespace for textual formats)
 */
public class UnexpectedEndOfInputException
    extends StreamReadException
{
    private static final long serialVersionUID = 3L;

    /**
     * Type of token that was being decoded, if parser had enough information
     * to recognize type (such as starting double-quote for Strings)
     */
    protected final JsonToken _token;

    public UnexpectedEndOfInputException(JsonParser p, JsonToken token, String msg) {
        super(p, msg);
        _token = token;
    }

    /**
     * Accessor for possibly available information about token that was being
     * decoded while encountering end of input.
     *
     * @return JsonToken that was being decoded while encountering end-of-input
     */
    public JsonToken getTokenBeingDecoded() {
        return _token;
    }
}
