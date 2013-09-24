package com.fasterxml.jackson.core.json;

import java.util.*;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Helper class used if
 * {@link com.fasterxml.jackson.core.JsonParser.Feature#STRICT_DUPLICATE_DETECTION}
 * is enabled.
 * Optimized to try to limit memory usage and processing overhead for smallest
 * entries, but without adding trashing (immutable objects would achieve optimal
 * memory usage but lead to significant number of discarded temp objects for
 * scopes with large number of entries). Another consideration is trying to limit
 * actual number of compiled classes as it contributes significantly to overall
 * jar size (due to linkage etc).
 * 
 * @since 2.3
 */
public class DupDetector
{
    /**
     * We need to store a back-reference here, unfortunately.
     */
    protected final JsonParser _parser;

    protected String _firstName;

    protected String _secondName;
    
    /**
     * Lazily constructed set of names already seen within this context.
     */
    protected HashSet<String> _seen;

    private DupDetector(JsonParser parser) {
        _parser = parser;
    }

    public static DupDetector rootDetector(JsonParser jp) {
        return new DupDetector(jp);
    }

    public DupDetector child() {
        return new DupDetector(_parser);
    }

    public void reset() {
        _firstName = null;
        _secondName = null;
        _seen = null;
    }

    public JsonLocation findLocation() {
        return _parser.getCurrentLocation();
    }
    
    public boolean isDup(String name) throws JsonParseException
    {
        if (_firstName == null) {
            _firstName = name;
            return false;
        }
        if (name.equals(_firstName)) {
            return true;
        }
        if (_secondName == null) {
            _secondName = name;
            return false;
        }
        if (name.equals(_secondName)) {
            return true;
        }
        if (_seen == null) {
            _seen = new HashSet<String>(16); // 16 is default, seems reasonable
            _seen.add(_firstName);
            _seen.add(_secondName);
        }
        return !_seen.add(name);
    }
}
