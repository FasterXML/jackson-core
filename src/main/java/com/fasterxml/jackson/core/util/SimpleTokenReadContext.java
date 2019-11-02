package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.DupDetector;

public class SimpleTokenReadContext extends TokenStreamContext
{
    /**
     * Parent context for this context; null for root context.
     */
    protected final SimpleTokenReadContext _parent;

    // // // Optional duplicate detection

    protected DupDetector _dups;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speed up things a bit (10-15%)
    /* for docs with lots of small arrays/objects
    /**********************************************************************
     */

    protected SimpleTokenReadContext _childToRecycle;

    /*
    /**********************************************************************
    /* Location/state information (minus source reference)
    /**********************************************************************
     */

    protected String _currentName;

    protected Object _currentValue;
    
    protected int _lineNr;
    protected int _columnNr;

    /*
    /**********************************************************
    /* Instance construction, config, reuse
    /**********************************************************
     */

    public SimpleTokenReadContext(int type, SimpleTokenReadContext parent, DupDetector dups,
             int lineNr, int colNr) {
        super();
        _parent = parent;
        _dups = dups;
        _type = type;
        _lineNr = lineNr;
        _columnNr = colNr;
        _index = -1;
    }

    protected void reset(int type, int lineNr, int colNr) {
        _type = type;
        _currentValue = null;
        _lineNr = lineNr;
        _columnNr = colNr;
        _index = -1;
        _currentName = null;
        if (_dups != null) {
            _dups.reset();
        }
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

    public static SimpleTokenReadContext createRootContext(int lineNr, int colNr, DupDetector dups) {
        return new SimpleTokenReadContext(TYPE_ROOT, null, dups, lineNr, colNr);
    }

    public static SimpleTokenReadContext createRootContext(DupDetector dups) {
        return createRootContext(1, 0, dups);
    }
    
    public SimpleTokenReadContext createChildArrayContext(int lineNr, int colNr) {
        SimpleTokenReadContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleTokenReadContext(TYPE_ARRAY, this,
                    (_dups == null) ? null : _dups.child(), lineNr, colNr);
        } else {
            ctxt.reset(TYPE_ARRAY, lineNr, colNr);
        }
        return ctxt;
    }

    public SimpleTokenReadContext createChildObjectContext(int lineNr, int colNr) {
        SimpleTokenReadContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleTokenReadContext(TYPE_OBJECT, this,
                    (_dups == null) ? null : _dups.child(), lineNr, colNr);
            return ctxt;
        }
        ctxt.reset(TYPE_OBJECT, lineNr, colNr);
        return ctxt;
    }

    /*
    /**********************************************************************
    /* Abstract method implementations, overrides
    /**********************************************************************
     */

    /**
     * @since 3.0
     */
    @Override public String currentName() { return _currentName; }

    @Override public boolean hasCurrentName() { return _currentName != null; }

    @Override public SimpleTokenReadContext getParent() { return _parent; }

    @Override
    public JsonLocation getStartLocation(Object srcRef) {
        // We don't keep track of offsets at this level (only reader does)
        long totalChars = -1L;
        return new JsonLocation(srcRef, totalChars, _lineNr, _columnNr);
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Method that can be used to both clear the accumulated references
     * (specifically value set with {@link #setCurrentValue(Object)})
     * that should not be retained, and returns parent (as would
     * {@link #getParent()} do). Typically called when closing the active
     * context when encountering {@link JsonToken#END_ARRAY} or
     * {@link JsonToken#END_OBJECT}.
     */
    public SimpleTokenReadContext clearAndGetParent() {
        _currentValue = null;
        // could also clear the current name, but seems cheap enough to leave?
        return _parent;
    }

    public DupDetector getDupDetector() {
        return _dups;
    }

    /*
    /**********************************************************************
    /* State changes
    /**********************************************************************
     */

    /**
     * Method to call to advance index within current context: to be called
     * when a new token found within current context (field name for objects,
     * value for root and array contexts)
     */
    public int valueRead() {
        return ++_index; // starts from -1
    }

    public void setCurrentName(String name) throws JsonProcessingException {
        _currentName = name;
        if (_dups != null) { _checkDup(_dups, name); }
    }

    private void _checkDup(DupDetector dd, String name) throws JsonProcessingException {
        if (dd.isDup(name)) {
            Object src = dd.getSource();
            throw new JsonParseException(((src instanceof JsonParser) ? ((JsonParser) src) : null),
                    "Duplicate field '"+name+"'");
        }
    }    
}
