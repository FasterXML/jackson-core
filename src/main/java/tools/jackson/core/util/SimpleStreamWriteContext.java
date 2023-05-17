package tools.jackson.core.util;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.json.DupDetector;

/**
 * Basic implementation of {@link TokenStreamContext} useful for most
 * format backend {@link JsonGenerator} implementations
 * (with notable exception of JSON that needs bit more advanced state).
 *
 * @since 3.0
 */
public final class SimpleStreamWriteContext extends TokenStreamContext
{
    /**
     * Parent context for this context; null for root context.
     */
    protected final SimpleStreamWriteContext _parent;

    // // // Optional duplicate detection

    protected DupDetector _dups;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speed up things a bit (10-15%)
    /* for docs with lots of small arrays/objects
    /**********************************************************************
     */

    protected SimpleStreamWriteContext _childToRecycle;

    /*
    /**********************************************************************
    /* Location/state information (minus source reference)
    /**********************************************************************
     */

    /**
     * Name of the property of which value is to be written; only
     * used for OBJECT contexts.
     */
    protected String _currentName;

    protected Object _currentValue;

    /**
     * Marker used to indicate that we just wrote a property name (or possibly
     * property id for some backends) and now expect a value to write.
     */
    protected boolean _gotPropertyId;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected SimpleStreamWriteContext(int type, SimpleStreamWriteContext parent,
            DupDetector dups, Object currentValue) {
        super();
        _type = type;
        _parent = parent;
        _dups = dups;
        _index = -1;
        _currentValue = currentValue;
    }

    private SimpleStreamWriteContext reset(int type, Object currentValue) {
        _type = type;
        _index = -1;
        _currentName = null;
        _gotPropertyId = false;
        _currentValue = currentValue;
        if (_dups != null) { _dups.reset(); }
        return this;
    }

    public SimpleStreamWriteContext withDupDetector(DupDetector dups) {
        _dups = dups;
        return this;
    }

    @Override
    public Object currentValue() {
        return _currentValue;
    }

    @Override
    public void assignCurrentValue(Object v) {
        _currentValue = v;
    }

    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */

    public static SimpleStreamWriteContext createRootContext(DupDetector dd) {
        return new SimpleStreamWriteContext(TYPE_ROOT, null, dd, null);
    }

    public SimpleStreamWriteContext createChildArrayContext(Object currentValue) {
        SimpleStreamWriteContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleStreamWriteContext(TYPE_ARRAY, this,
                    (_dups == null) ? null : _dups.child(), currentValue);
            return ctxt;
        }
        return ctxt.reset(TYPE_ARRAY, currentValue);
    }

    public SimpleStreamWriteContext createChildObjectContext(Object currentValue) {
        SimpleStreamWriteContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleStreamWriteContext(TYPE_OBJECT, this,
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

    @Override public final SimpleStreamWriteContext getParent() { return _parent; }
    @Override public final String currentName() {
        // 15-Aug-2019, tatu: Should NOT check this status because otherwise name
        //    in parent context is not accessible after new structured scope started
//        if (_gotPropertyId) { ... }
        return _currentName;
    }

    @Override public boolean hasCurrentName() { return _gotPropertyId; }

    /**
     * Method that can be used to both clear the accumulated references
     * (specifically value set with {@link #assignCurrentValue(Object)})
     * that should not be retained, and returns parent (as would
     * {@link #getParent()} do). Typically called when closing the active
     * context when encountering {@link JsonToken#END_ARRAY} or
     * {@link JsonToken#END_OBJECT}.
     *
     * @return Parent context of this context node, if any; {@code null} for root context
     */
    public SimpleStreamWriteContext clearAndGetParent() {
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
     * Method that writer is to call before it writes an Object Property name.
     *
     * @param name Name of Object property name being written
     *
     * @return {@code True} if name writing should proceed; {@code false} if not
     *
     * @throws StreamWriteException If write fails due to duplicate check
     */
    public boolean writeName(String name) throws StreamWriteException {
        if ((_type != TYPE_OBJECT) || _gotPropertyId) {
            return false;
        }
        _gotPropertyId = true;
        _currentName = name;
        if (_dups != null) { _checkDup(_dups, name); }
        return true;
    }

    private final void _checkDup(DupDetector dd, String name) throws StreamWriteException {
        if (dd.isDup(name)) {
            Object src = dd.getSource();
            throw new StreamWriteException(((src instanceof JsonGenerator) ? ((JsonGenerator) src) : null),
                    "Duplicate Object property \""+name+"\"");
        }
    }

    public boolean writeValue() {
        // Only limitation is with OBJECTs:
        if (_type == TYPE_OBJECT) {
            if (!_gotPropertyId) {
                return false;
            }
            _gotPropertyId = false;
        }
        ++_index;
        return true;
    }
}
