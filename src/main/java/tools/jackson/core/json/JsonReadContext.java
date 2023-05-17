package tools.jackson.core.json;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;

/**
 * Extension of {@link TokenStreamContext}, which implements
 * core methods needed, and also exposes
 * more complete API to parser implementation classes.
 */
public final class JsonReadContext extends TokenStreamContext
{
    // // // Configuration

    /**
     * Parent context for this context; null for root context.
     */
    protected final JsonReadContext _parent;

    // // // Optional duplicate detection

    protected DupDetector _dups;

    /*
    /**********************************************************************
    /* Simple instance reuse slots; speeds up things a bit (10-15%)
    /* for docs with lots of small arrays/objects (for which
    /* allocation was visible in profile stack frames)
    /**********************************************************************
     */

    protected JsonReadContext _child;

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

    /**
     * @since 2.15
     */
    public JsonReadContext(JsonReadContext parent, int nestingDepth,
            DupDetector dups, int type, int lineNr, int colNr) {
        super();
        _parent = parent;
        _dups = dups;
        _type = type;
        _lineNr = lineNr;
        _columnNr = colNr;
        _index = -1;
        _nestingDepth = nestingDepth;
    }

    /**
     * Internal method to allow instance reuse: DO NOT USE unless you absolutely
     * know what you are doing.
     * Clears up state (including "current value"), changes type to one specified;
     * resets current duplicate-detection state (if any).
     * Parent link left as-is since it is {@code final}.
     *
     * @param type Type to assign to this context node
     * @param lineNr Line of the starting position of this context
     * @param colNr Column of the starting position of this context
     *
     * @return This context instance (to allow call-chaining)
     */
    public JsonReadContext reset(int type, int lineNr, int colNr) {
        _type = type;
        _index = -1;
        _lineNr = lineNr;
        _columnNr = colNr;
        _currentName = null;
        _currentValue = null;
        if (_dups != null) {
            _dups.reset();
        }
        return this;
    }

    /*
    public void trackDups(JsonParser p) {
        _dups = DupDetector.rootDetector(p);
    }
    */

    public JsonReadContext withDupDetector(DupDetector dups) {
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

    public static JsonReadContext createRootContext(int lineNr, int colNr, DupDetector dups) {
        return new JsonReadContext(null, 0, dups, TYPE_ROOT, lineNr, colNr);
    }

    public static JsonReadContext createRootContext(DupDetector dups) {
        return new JsonReadContext(null, 0, dups, TYPE_ROOT, 1, 0);
    }

    public JsonReadContext createChildArrayContext(int lineNr, int colNr) {
        JsonReadContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new JsonReadContext(this, _nestingDepth+1,
                    (_dups == null) ? null : _dups.child(), TYPE_ARRAY, lineNr, colNr);
        } else {
            ctxt.reset(TYPE_ARRAY, lineNr, colNr);
        }
        return ctxt;
    }

    public JsonReadContext createChildObjectContext(int lineNr, int colNr) {
        JsonReadContext ctxt = _child;
        if (ctxt == null) {
            _child = ctxt = new JsonReadContext(this, _nestingDepth+1,
                    (_dups == null) ? null : _dups.child(), TYPE_OBJECT, lineNr, colNr);
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

    @Override public JsonReadContext getParent() { return _parent; }

    @Override
    public JsonLocation startLocation(ContentReference srcRef) {
        // We don't keep track of offsets at this level (only reader does)
        long totalChars = -1L;
        return new JsonLocation(ContentReference.rawReference(srcRef),
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
    public JsonReadContext clearAndGetParent() {
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

    public boolean expectComma() {
        /* Assumption here is that we will be getting a value (at least
         * before calling this method again), and
         * so will auto-increment index to avoid having to do another call
         */
        int ix = ++_index; // starts from -1
        return (_type != TYPE_ROOT && ix > 0);
    }

    /**
     * Method that parser is to call when it reads a name of Object property.
     *
     * @param name Property name that has been read
     *
     * @throws StreamReadException if duplicate check restriction is violated (which
     *    assumes that duplicate-detection is enabled)
     */
    public void setCurrentName(String name) throws StreamReadException
    {
        _currentName = name;
        if (_dups != null) { _checkDup(_dups, name); }
    }

    private void _checkDup(DupDetector dd, String name) throws StreamReadException
    {
        if (dd.isDup(name)) {
            Object src = dd.getSource();
            throw new StreamReadException(((src instanceof JsonParser) ? ((JsonParser) src) : null),
                    "Duplicate Object property \""+name+"\"");
        }
    }
}
