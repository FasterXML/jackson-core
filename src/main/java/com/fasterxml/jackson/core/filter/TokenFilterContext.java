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
     * Filter to use for items in this state (for properties of Objects,
     * elements of Arrays, and root-level values of root context)
     */
    protected TokenFilter _filter;

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

    /**
     * Flag that indicates that the current name of this context
     * still needs to be written, if path from root is desired.
     */
    protected boolean _needToWriteName;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected TokenFilterContext(int type, TokenFilterContext parent,
            TokenFilter filter, boolean startWritten)
    {
        super();
        _type = type;
        _parent = parent;
        _filter = filter;
        _index = -1;
        _startWritten = startWritten;
        _needToWriteName = false;
        _needCloseCheck = false;
    }

    protected TokenFilterContext reset(int type,
            TokenFilter filter, boolean startWritten)
    {
        _type = type;
        _filter = filter;
        _index = -1;
        _currentName = null;
        _startWritten = startWritten;
        _needToWriteName = false;
        _needCloseCheck = false;
        return this;
    }

    /*
    /**********************************************************
    /* Factory methods
    /**********************************************************
     */

    public static TokenFilterContext createRootContext(TokenFilter filter) {
        return new TokenFilterContext(TYPE_ROOT, null, filter, false);
    }

    public TokenFilterContext createChildArrayContext(TokenFilter filter, boolean writeStart) {
        TokenFilterContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TokenFilterContext(TYPE_ARRAY, this, filter, writeStart);
            return ctxt;
        }
        return ctxt.reset(TYPE_ARRAY, filter, writeStart);
    }

    public TokenFilterContext createChildObjectContext(TokenFilter filter, boolean writeStart) {
        TokenFilterContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new TokenFilterContext(TYPE_OBJECT, this, filter, writeStart);
            return ctxt;
        }
        return ctxt.reset(TYPE_OBJECT, filter, writeStart);
    }

    /*
    /**********************************************************
    /* State changes
    /**********************************************************
     */
    
    public TokenFilter setFieldName(String name) throws JsonProcessingException {
        _currentName = name;
        _needToWriteName = true;
        return _filter;
    }

    /**
     * Method called to check whether value is to be included at current output
     * position, either as Object property, Array element, or root value.
     */
    public TokenFilter checkValue(TokenFilter filter) {
        // First, checks for Object properties have been made earlier:
        if (_type == TYPE_OBJECT) {
            return filter;
        }
        // We increaase it first because at the beginning of array, value is -1
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
        if ((_filter == null) || (_filter == TokenFilter.INCLUDE_ALL)) {
            return;
        }
        if (_parent != null) {
            _parent._writePath(gen);
        }
        if (_startWritten) {
            // even if Object started, need to start leaf-level name
            if (_needToWriteName) {
                gen.writeFieldName(_currentName);
            }
        } else {
            _startWritten = true;
            if (_type == TYPE_OBJECT) {
                gen.writeStartObject();
                gen.writeFieldName(_currentName); // we know name must be written
            } else if (_type == TYPE_ARRAY) {
                gen.writeStartArray();
            }
        }
    }

    /**
     * Variant of {@link #writePath(JsonGenerator)} called when all we
     * need is immediately surrounding Object. Method typically called
     * when including a single property but not including full path
     * to root.
     */
    public void writeImmediatePath(JsonGenerator gen) throws IOException
    {
        if ((_filter == null) || (_filter == TokenFilter.INCLUDE_ALL)) {
            return;
        }
        if (_startWritten) {
            // even if Object started, need to start leaf-level name
            if (_needToWriteName) {
                gen.writeFieldName(_currentName);
            }
        } else {
            _startWritten = true;
            if (_type == TYPE_OBJECT) {
                gen.writeStartObject();
                if (_needToWriteName) {
                    gen.writeFieldName(_currentName);
                }
            } else if (_type == TYPE_ARRAY) {
                gen.writeStartArray();
            }
        }
    }

    private void _writePath(JsonGenerator gen) throws IOException
    {
//System.err.println("_writePath("+_type+"), startWritten? "+_startWritten+", writeName? "+_needToWriteName+" at "+toString());
        if ((_filter == null) || (_filter == TokenFilter.INCLUDE_ALL)) {
            return;
        }
        if (_parent != null) {
            _parent._writePath(gen);
        }
        if (_startWritten) {
            // even if Object started, need to start leaf-level name
            if (_needToWriteName) {
                _needToWriteName = false; // at parent must explicitly clear
                gen.writeFieldName(_currentName);
            }
        } else {
            _startWritten = true;
            if (_type == TYPE_OBJECT) {
//System.err.println(" write object start, field '"+_currentName+"'");                
                gen.writeStartObject();
                if (_needToWriteName) {
                    _needToWriteName = false; // at parent must explicitly clear
                    gen.writeFieldName(_currentName);
                }
            } else if (_type == TYPE_ARRAY) {
                gen.writeStartArray();
            }
        }
    }
    
    public void skipParentChecks() {
        _filter = null;
        for (TokenFilterContext ctxt = _parent; ctxt != null; ctxt = ctxt._parent) {
            _parent._filter = null;
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

    public TokenFilter getFilterState() { return _filter; }

    public void markNeedsCloseCheck() { _needCloseCheck = true; }

    public final TokenFilterContext closeArray(JsonGenerator gen) throws IOException
    {
//System.err.println("closeArray, started? "+_startWritten+"; filter = "+_filter);        
        if (_startWritten) {
            gen.writeEndArray();
        }
        if ((_filter != null) && (_filter != TokenFilter.INCLUDE_ALL)) {
            _filter.filterFinishArray();
        }
        return _parent;
    }
    
    public final TokenFilterContext closeObject(JsonGenerator gen) throws IOException
    {
        if (_startWritten) {
            gen.writeEndObject();
        }
        if ((_filter != null) && (_filter != TokenFilter.INCLUDE_ALL)) {
            _filter.filterFinishObject();
        }
        return _parent;
    }

    // // // Internally used abstract methods

    protected void appendDesc(StringBuilder sb) {
        if (_parent != null) {
            _parent.appendDesc(sb);
        }
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
