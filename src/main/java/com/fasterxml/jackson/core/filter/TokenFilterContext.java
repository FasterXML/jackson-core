package com.fasterxml.jackson.core.filter;

import java.io.IOException;

import com.fasterxml.jackson.core.*;

/**
 * Alternative variant of {@link JsonStreamContext}, used when filtering
 * content being read or written (based on {@link TokenFilter}).
 * 
 * @since 2.6
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

    /**
     * State of this context.
     */
    protected int _filterState;

    /**
     * Flag that indicates that start token has been written, so
     * that matching close token needs to be written as well,
     * regardless of inclusion status.
     */
    protected boolean _startWritten;

    /**
     * Flag that indicates that when context is closed, a call needs
     * to be made to {@link TokenFilter}
     */
    protected boolean _needCloseCheck;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected TokenFilterContext(int type, TokenFilterContext parent,
            int fstate, boolean startWritten)
    {
        super();
        _type = type;
        _parent = parent;
        _filterState = fstate;
        _index = -1;
        _startWritten = false;
    }

    protected TokenFilterContext reset(int type,
            int fstate, boolean startWritten)
    {
        _type = type;
        _filterState = fstate;
        _index = -1;
        _currentName = null;
        _startWritten = startWritten;
        return this;
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    public static TokenFilterContext createRootContext(int fstate) {
        return new TokenFilterContext(TYPE_ROOT, null, fstate, false);
    }

    public TokenFilterContext createChildArrayContext(int fstate, boolean writeStart) {
        TokenFilterContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TokenFilterContext(TYPE_ARRAY, this, fstate, writeStart);
            return ctxt;
        }
        return ctxt.reset(TYPE_ARRAY, fstate, writeStart);
    }

    public TokenFilterContext createChildObjectContext(int fstate, boolean writeStart) {
        TokenFilterContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TokenFilterContext(TYPE_OBJECT, this, fstate, writeStart);
            return ctxt;
        }
        return ctxt.reset(TYPE_OBJECT, fstate, writeStart);
    }

    /*
    /**********************************************************
    /* State changes
    /**********************************************************
     */
    
    public int setFieldName(String name) throws JsonProcessingException {
        _currentName = name;
        return _filterState;
    }

    /**
     * Method called to check whether value is to be included at current output
     * position, either as Object property, Array element, or root value.
     */
    public int checkValue(TokenFilter filter) {
        // First, checks for Object properties have been made earlier:
        if (_type == TYPE_OBJECT) {
            return TokenFilter.FILTER_CHECK;
        }
        int ix = ++_index;
        if (_type == TYPE_ARRAY) {
            return filter.includeElement(ix);
        }
        return filter.includeRootValue(ix);
    }

    /**
     * Method called to ensure that parent path from root is written up to
     * and including this node.
     */
    public void writePath(JsonGenerator gen) throws IOException
    {
        if (_filterState != TokenFilter.FILTER_CHECK) {
            return;
        }
//System.err.println("writePath(), startWritten? "+_startWritten+" at "+toString());
        if (_parent != null) {
            _parent._writePath(gen);
        }
        if (_startWritten) {
            // even if Object started, need to start leaf-level name
            if (_type == TYPE_OBJECT) {
//System.err.println(" write field name '"+_currentName+"'");                
                gen.writeFieldName(_currentName);
            }
        } else {
            _startWritten = true;
            if (_type == TYPE_OBJECT) {
//System.err.println(" write object start, field '"+_currentName+"'");                
                gen.writeStartObject();
                gen.writeFieldName(_currentName);
            } else if (_type == TYPE_ARRAY) {
                gen.writeStartArray();
            }
        }
    }

    private void _writePath(JsonGenerator gen) throws IOException
    {
//System.err.println("_writePath(), startWritten? "+_startWritten+" at "+toString());
        if (_filterState != TokenFilter.FILTER_CHECK) {
            return;
        }
        if (_parent != null) {
            _parent._writePath(gen);
        }
        if (!_startWritten) {
            _startWritten = true;
            if (_type == TYPE_OBJECT) {
System.err.println(" write object start, field '"+_currentName+"'");                
                gen.writeStartObject();
                gen.writeFieldName(_currentName);
            } else if (_type == TYPE_ARRAY) {
                gen.writeStartArray();
            }
        }
    }
    
    public void skipParentChecks() {
        _filterState = TokenFilter.FILTER_SKIP;
        for (TokenFilterContext ctxt = _parent; ctxt != null; ctxt = ctxt._parent) {
            _parent._filterState = TokenFilter.FILTER_SKIP;
        }
    }

    /*
    /**********************************************************
    /* Accessors, mutators
    /**********************************************************
     */

    @Override
    public Object getCurrentValue() { return null; }

    @Override
    public void setCurrentValue(Object v) { }

    @Override public final TokenFilterContext getParent() { return _parent; }
    @Override public final String getCurrentName() { return _currentName; }

    public int getFilterState() { return _filterState; }
    public boolean needsCloseToken() { return _startWritten; }

    public void markNeedsCloseCheck() { _needCloseCheck = true; }
    public boolean needsCloseCheck() { return _needCloseCheck; }
    
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
