package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.DupDetector;

/**
 * Basic implementation of {@code TokenStreamContext} useful for most
 * format backends (with notable exception of JSON that needs bit more
 * advanced state).
 *
 * @since 3.0
 */
public final class SimpleTokenWriteContext extends TokenStreamContext
{
    /**
     * Parent context for this context; null for root context.
     */
    protected final SimpleTokenWriteContext _parent;

    // // // Optional duplicate detection

    protected DupDetector _dups;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speed up things a bit (10-15%)
    /* for docs with lots of small arrays/objects
    /**********************************************************************
     */

    protected SimpleTokenWriteContext _childToRecycle;

    /*
    /**********************************************************************
    /* Location/state information (minus source reference)
    /**********************************************************************
     */

    /**
     * Name of the field of which value is to be written; only
     * used for OBJECT contexts
     */
    protected String _currentName;

    protected Object _currentValue;

    /**
     * Marker used to indicate that we just wrote a field name
     * and now expect a value to write
     */
    protected boolean _gotFieldId;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected SimpleTokenWriteContext(int type, SimpleTokenWriteContext parent, DupDetector dups,
            Object currentValue) {
        super();
        _type = type;
        _parent = parent;
        _dups = dups;
        _index = -1;
        _currentValue = currentValue;
    }

    private SimpleTokenWriteContext reset(int type, Object currentValue) {
        _type = type;
        _index = -1;
        _currentName = null;
        _gotFieldId = false;
        _currentValue = currentValue;
        if (_dups != null) { _dups.reset(); }
        return this;
    }

    public SimpleTokenWriteContext withDupDetector(DupDetector dups) {
        _dups = dups;
        return this;
    }

    @Override
    public Object getCurrentValue() {
        return _currentValue;
    }

    @Override
    public void setCurrentValue(Object v) {
        _currentValue = v;
    }

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    public static SimpleTokenWriteContext createRootContext(DupDetector dd) {
        return new SimpleTokenWriteContext(TYPE_ROOT, null, dd, null);
    }

    public SimpleTokenWriteContext createChildArrayContext(Object currentValue) {
        SimpleTokenWriteContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleTokenWriteContext(TYPE_ARRAY, this,
                    (_dups == null) ? null : _dups.child(), currentValue);
            return ctxt;
        }
        return ctxt.reset(TYPE_ARRAY, currentValue);
    }

    public SimpleTokenWriteContext createChildObjectContext(Object currentValue) {
        SimpleTokenWriteContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleTokenWriteContext(TYPE_OBJECT, this,
                    (_dups == null) ? null : _dups.child(), currentValue);
            return ctxt;
        }
        return ctxt.reset(TYPE_OBJECT, currentValue);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */
    
    @Override public final SimpleTokenWriteContext getParent() { return _parent; }
    @Override public final String currentName() {
        // 15-Aug-2019, tatu: Should NOT check this status because otherwise name
        //    in parent context is not accessible after new structured scope started
//        if (_gotFieldId) { ... }
        return _currentName;
    }

    @Override public boolean hasCurrentName() { return _gotFieldId; }

    /**
     * Method that can be used to both clear the accumulated references
     * (specifically value set with {@link #setCurrentValue(Object)})
     * that should not be retained, and returns parent (as would
     * {@link #getParent()} do). Typically called when closing the active
     * context when encountering {@link JsonToken#END_ARRAY} or
     * {@link JsonToken#END_OBJECT}.
     */
    public SimpleTokenWriteContext clearAndGetParent() {
        _currentValue = null;
        // could also clear the current name, but seems cheap enough to leave?
        return _parent;
    }

    public DupDetector getDupDetector() {
        return _dups;
    }

    /*
    /**********************************************************************
    /* State changing
    /**********************************************************************
     */
    
    /**
     * Method that writer is to call before it writes a field name.
     *
     * @return Ok if name writing should proceed
     */
    public boolean writeFieldName(String name) throws JsonProcessingException {
        if ((_type != TYPE_OBJECT) || _gotFieldId) {
            return false;
        }
        _gotFieldId = true;
        _currentName = name;
        if (_dups != null) { _checkDup(_dups, name); }
        return true;
    }

    private final void _checkDup(DupDetector dd, String name) throws JsonProcessingException {
        if (dd.isDup(name)) {
            Object src = dd.getSource();
            throw new JsonGenerationException("Duplicate field '"+name+"'",
                    ((src instanceof JsonGenerator) ? ((JsonGenerator) src) : null));
        }
    }
    
    public boolean writeValue() {
        // Only limitation is with OBJECTs:
        if (_type == TYPE_OBJECT) {
            if (!_gotFieldId) {
                return false;
            }
            _gotFieldId = false;
        }
        ++_index;
        return true;
    }
}
