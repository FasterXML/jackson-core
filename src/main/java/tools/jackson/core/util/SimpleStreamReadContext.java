package tools.jackson.core.util;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.json.DupDetector;

/**
 * Basic implementation of {@link TokenStreamContext} useful for most
 * format backend {@link JsonParser} implementations
 * (with notable exception of JSON that needs bit more advanced state).
 *
 * @since 3.0
 */
public class SimpleStreamReadContext extends TokenStreamContext
{
    /**
     * Parent context for this context; null for root context.
     */
    protected final SimpleStreamReadContext _parent;

    // // // Optional duplicate detection

    protected DupDetector _dups;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speed up things a bit (10-15%)
    /* for docs with lots of small arrays/objects
    /**********************************************************************
     */

    protected SimpleStreamReadContext _childToRecycle;

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
    /**********************************************************************
    /* Instance construction, config, reuse
    /**********************************************************************
     */

    public SimpleStreamReadContext(int type, SimpleStreamReadContext parent, int nestingDepth,
            DupDetector dups,
             int lineNr, int colNr) {
        super();
        _parent = parent;
        _dups = dups;
        _type = type;
        _lineNr = lineNr;
        _columnNr = colNr;
        _index = -1;
        _nestingDepth = nestingDepth;
    }

    // REMOVE as soon as nothing uses this
    @Deprecated
    public SimpleStreamReadContext(int type, SimpleStreamReadContext parent,
            DupDetector dups,
             int lineNr, int colNr) {
        super();
        _parent = parent;
        _dups = dups;
        _type = type;
        _lineNr = lineNr;
        _columnNr = colNr;
        _index = -1;
        _nestingDepth = -1;
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

    public static SimpleStreamReadContext createRootContext(int lineNr, int colNr, DupDetector dups) {
        return new SimpleStreamReadContext(TYPE_ROOT, null, 0, dups, lineNr, colNr);
    }

    public static SimpleStreamReadContext createRootContext(DupDetector dups) {
        return createRootContext(1, 0, dups);
    }

    public SimpleStreamReadContext createChildArrayContext(int lineNr, int colNr) {
        SimpleStreamReadContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleStreamReadContext(TYPE_ARRAY, this,
                    _nestingDepth + 1,
                    (_dups == null) ? null : _dups.child(), lineNr, colNr);
        } else {
            ctxt.reset(TYPE_ARRAY, lineNr, colNr);
        }
        return ctxt;
    }

    public SimpleStreamReadContext createChildObjectContext(int lineNr, int colNr) {
        SimpleStreamReadContext ctxt = _childToRecycle;
        if (ctxt == null) {
            _childToRecycle = ctxt = new SimpleStreamReadContext(TYPE_OBJECT, this,
                    _nestingDepth + 1,
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

    @Override public SimpleStreamReadContext getParent() { return _parent; }

    @Override
    public JsonLocation startLocation(ContentReference srcRef) {
        // We don't keep track of offsets at this level (only reader does)
        long totalChars = -1L;
        return new JsonLocation(srcRef,
                totalChars, _lineNr, _columnNr);
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

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
    public SimpleStreamReadContext clearAndGetParent() {
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
     * when a new token found within current context (property name for objects,
     * value for root and array contexts)
     *
     * @return Index after increment
     */
    public int valueRead() {
        return ++_index; // starts from -1
    }

    /**
     * Method called to indicate what the "current" name (Object property name
     * just decoded) is: may also trigger duplicate detection.
     *
     * @param name Name of Object property encountered
     *
     * @throws StreamReadException If there is a duplicate name violation
     */
    public void setCurrentName(String name)
        throws StreamReadException
    {
        _currentName = name;
        if (_dups != null) { _checkDup(_dups, name); }
    }

    protected void _checkDup(DupDetector dd, String name)
        throws StreamReadException
    {
        if (dd.isDup(name)) {
            Object src = dd.getSource();
            throw new StreamReadException(((src instanceof JsonParser) ? ((JsonParser) src) : null),
                    "Duplicate Object property \""+name+"\"");
        }
    }
}
