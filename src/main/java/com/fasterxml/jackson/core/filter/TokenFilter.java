package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.JsonToken;

/**
 * @since 2.6
 */
public abstract class TokenFilter
{
    // Constants

    public final static int FILTER_SKIP_TREE = 1;
    public final static int FILTER_SKIP_CURRENT = 2;
    public final static int FILTER_INCLUDE_CURRENT = 3;
    public final static int FILTER_INCLUDE_TREE = 4;

    // API, scalar values

    public int writeRootScalarValue(JsonToken type) {
        return FILTER_SKIP_TREE;
    }

    public int writeScalarProperty(String name, JsonToken type) {
        return FILTER_SKIP_TREE;
    }

    public int writeScalarElement(int index, JsonToken type) {
        return FILTER_SKIP_TREE;
    }

    // API, Objects
    
    public int startRootObject() {
        return FILTER_SKIP_TREE;
    }
    
    public int startObjectProperty(String name) {
        return FILTER_SKIP_TREE;
    }
    
    public int startObjectElement(int index) {
        return FILTER_SKIP_TREE;
    }
    
    public void finishObject() { }

    // API, Arrays
    
    public int startRootArray() {
        return FILTER_SKIP_TREE;
    }
    
    public int startArrayProperty(String name) {
        return FILTER_SKIP_TREE;
    }
    
    public int startArrayElement(int index) {
        return FILTER_SKIP_TREE;
    }
    
    public void finishArray() { }
}
