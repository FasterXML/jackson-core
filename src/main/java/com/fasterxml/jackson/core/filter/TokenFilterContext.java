package com.fasterxml.jackson.core.filter;

import com.fasterxml.jackson.core.*;

/**
 * Alternative variant of {@link JsonStreamContext}, used when filtering
 * content being read or written (based on {@link TokenFilter}).
 */
public class TokenFilterContext extends JsonStreamContext
{
    /**
     * Parent context for this context; null for root context.
     */
    protected final TokenFilterContext _parent;

    /*
    /**********************************************************
    /* Simple instance reuse slots; speed up things
    /* a bit (10-15%) for docs with lots of small
    /* arrays/objects
    /**********************************************************
     */

    protected TokenFilterContext _child = null;

    /*
    /**********************************************************
    /* Location/state information
    /**********************************************************
     */
    
    /**
     * Name of the field of which value is to be parsed; only
     * used for OBJECT contexts
     */
    protected String _currentName;

    protected int _filterState;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected TokenFilterContext(int type, TokenFilterContext parent, int fstate) {
        super();
        _type = type;
        _parent = parent;
        _filterState = fstate;
        _index = -1;
    }

    protected TokenFilterContext reset(int type, int fstate) {
        _type = type;
        _filterState = fstate;
        _index = -1;
        _currentName = null;
        return this;
    }

    @Override
    public Object getCurrentValue() {
        return null;
    }

    @Override
    public void setCurrentValue(Object v) { }
    
    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    public static TokenFilterContext createRootContext(int fstate) {
        return new TokenFilterContext(TYPE_ROOT, null, fstate);
    }

    public TokenFilterContext createChildArrayContext(int fstate) {
        TokenFilterContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TokenFilterContext(TYPE_ARRAY, this, fstate);
            return ctxt;
        }
        return ctxt.reset(TYPE_ARRAY, fstate);
    }

    public TokenFilterContext createChildObjectContext(int fstate) {
        TokenFilterContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TokenFilterContext(TYPE_OBJECT, this, fstate);
            return ctxt;
        }
        return ctxt.reset(TYPE_OBJECT, fstate);
    }

    @Override public final TokenFilterContext getParent() { return _parent; }
    @Override public final String getCurrentName() { return _currentName; }

    public int getFilterState() { return _filterState; }

    public void writeFieldName(String name) throws JsonProcessingException {
        _currentName = name;
    }

    public void writeValue() {
        ++_index;
    }

    // // // Internally used abstract methods

    protected void appendDesc(StringBuilder sb) {
        if (_type == TYPE_OBJECT) {
            sb.append('{');
            if (_currentName != null) {
                sb.append('"');
                // !!! TODO: Name chars should be escaped?
                sb.append(_currentName);
                sb.append('"');
            } else {
                sb.append('?');
            }
            sb.append('}');
        } else if (_type == TYPE_ARRAY) {
            sb.append('[');
            sb.append(getCurrentIndex());
            sb.append(']');
        } else {
            // nah, ROOT:
            sb.append("/");
        }
    }

    // // // Overridden standard methods

    /**
     * Overridden to provide developer writeable "JsonPath" representation
     * of the context.
     */
    @Override public String toString() {
        StringBuilder sb = new StringBuilder(64);
        appendDesc(sb);
        return sb.toString();
    }
}
