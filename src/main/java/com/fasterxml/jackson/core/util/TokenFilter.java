package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.JsonToken;

/**
 * @since 2.6
 */
public abstract class TokenFilter
{
    // Constants
    
    public int FILTER_SKIP_TREE = 1;
    public int FILTER_SKIP_CURRENT = 2;
    public int FILTER_INCLUDE_TREE = 3;
    public int FILTER_INCLUDE_CURRENT = 4;

    // API

    public int writeScalar(JsonToken type) {
        return FILTER_SKIP_TREE;
    }

    public int startObject() {
        return FILTER_SKIP_TREE;
    }

    public void finishObject() { }

    public int startArray() {
        return FILTER_SKIP_TREE;
    }

    public void finishArray() { }

    public int startObjectProperty(String name) {
        return FILTER_SKIP_TREE;
    }

    public void finishObjectProperty() { }

    public int startArrayElement(int index) {
        return FILTER_SKIP_TREE;
    }

    public void finishArrayElement() { }
}
