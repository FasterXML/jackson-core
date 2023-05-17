package tools.jackson.core.filter;

import tools.jackson.core.JsonPointer;

/**
 * Simple {@link TokenFilter} implementation that takes a single
 * {@link JsonPointer} and matches a single value accordingly.
 * Instances are immutable and fully thread-safe, shareable,
 * and efficient to use.
 *
 * @since 2.6
 */
public class JsonPointerBasedFilter extends TokenFilter
{
    protected final JsonPointer _pathToMatch;
    /**
     * if true include all array elements by ignoring the array index match and advancing the JsonPointer to the next level
     */
    protected final boolean _includeAllElements;

    public JsonPointerBasedFilter(String ptrExpr) {
        this(JsonPointer.compile(ptrExpr), false);
    }

    public JsonPointerBasedFilter(JsonPointer match) {
        this(match, false);
    }

    /**
     * @param ptrExpr to extract.
     * @param includeAllElements if true array indexes in <code>ptrExpr</code> are ignored and all elements will be matched. default: false
     */
    public JsonPointerBasedFilter(String ptrExpr, boolean includeAllElements) {
        this(JsonPointer.compile(ptrExpr), includeAllElements);
    }

    public JsonPointerBasedFilter(JsonPointer match, boolean includeAllElements) {
        _pathToMatch = match;
        _includeAllElements = includeAllElements;
    }

    @Override
    public TokenFilter includeElement(int index) {
        JsonPointer next;
        if (_includeAllElements && ! _pathToMatch.mayMatchElement()) {
            next = _pathToMatch.tail();
        } else {
            next = _pathToMatch.matchElement(index);
        }
        if (next == null) {
            return null;
        }
        if (next.matches()) {
            return TokenFilter.INCLUDE_ALL;
        }
        return new JsonPointerBasedFilter(next, _includeAllElements);
    }

    @Override
    public TokenFilter includeProperty(String name) {
        JsonPointer next = _pathToMatch.matchProperty(name);
        if (next == null) {
            return null;
        }
        if (next.matches()) {
            return TokenFilter.INCLUDE_ALL;
        }
        return new JsonPointerBasedFilter(next, _includeAllElements);
    }

    @Override
    public TokenFilter filterStartArray() {
        return this;
    }

    @Override
    public TokenFilter filterStartObject() {
        return this;
    }

    @Override
    protected boolean _includeScalar() {
        // should only occur for root-level scalars, path "/"
        return _pathToMatch.matches();
    }

    @Override
    public String toString() {
        return "[JsonPointerFilter at: "+_pathToMatch+"]";
    }
}
