package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.JsonPointer;

/**
 * Helper class that implements simple single-match filter based
 * on a {@link JsonPointer} instance.
 */
class JsonPointerBasedFilter extends TokenFilter
{
    protected final JsonPointer _pathToMatch;

    protected final boolean _includeParent;

    public JsonPointerBasedFilter(String ptrExpr, boolean inclParent) {
        this(JsonPointer.compile(ptrExpr), inclParent);
    }

    public JsonPointerBasedFilter(JsonPointer match, boolean inclParent) {
        _pathToMatch = match;
        _includeParent = inclParent;
    }

    @Override
    public TokenFilter includeElement(int index) {
        JsonPointer next = _pathToMatch.matchElement(index);
        if (next == null) {
            return null;
        }
        if (next.matches()) {
            return TokenFilter.INCLUDE_ALL;
        }
        return new JsonPointerBasedFilter(next, _includeParent);
    }

    @Override
    public TokenFilter includeProperty(String name) {
        JsonPointer next = _pathToMatch.matchProperty(name);
        if (next == null) {
            return null;
        }
        if (next.matches()) {
            if (_includeParent) {
                return TokenFilter.INCLUDE_ALL;
            }
            // Minor complication, for non-path-including case
            return IncludeAny.instance;
        }
        return new JsonPointerBasedFilter(next, _includeParent);
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

    /**
     * Helper class needed to include value of an Object property, without
     * including surrounding Object. Used when path is not to be included.
     */
    static class IncludeAny extends TokenFilter {
        public final static IncludeAny instance = new IncludeAny();

        @Override
        public TokenFilter filterStartArray() {
            return TokenFilter.INCLUDE_ALL;
        }
        
        @Override
        public TokenFilter filterStartObject() {
            return TokenFilter.INCLUDE_ALL;
        }
        
        @Override
        protected boolean _includeScalar() {
            return true;
        }
    }
}
