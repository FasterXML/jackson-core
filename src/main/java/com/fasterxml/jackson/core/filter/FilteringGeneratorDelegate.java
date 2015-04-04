package com.fasterxml.jackson.core.filter;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

/**
 * @since 2.6.0
 */
public class FilteringGeneratorDelegate extends JsonGeneratorDelegate
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */
    
    /**
     * Object consulted to determine whether to write parts of content generator
     * is asked to write or not.
     */
    protected TokenFilter filter;

    /**
     * Flag that determines whether filtering will continue after the first
     * match is indicated or not: if `false`, output is based on just the first
     * full match (returning {@link TokenFilter#FILTER_INCLUDE}) and no more
     * checks are made; if `true` then filtering will be applied as necessary
     * until end of content.
     */
    protected boolean _filterAll;

    /**
     * Flag that determines whether path leading up to included content should
     * also be automatically included or not. If `false`, no path inclusion is
     * done and only explicitly included entries are output; if `true` then
     * path from main level down to match is also included as necessary.
     */
    protected boolean _includePath;
    
    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */

    /**
     * Although delegate has its own output context it is not sufficient since we actually
     * have to keep track of excluded (filtered out) structures as well as ones delegate
     * actually outputs.
     */
    protected TokenFilterContext _filterContext;
    
    /**
     * The current state constant is kept here as well,
     * not just at the tip of {@link #_filterContext}.
     */
    protected int _currentState;

    /**
     * Number of tokens for which {@link TokenFilter#FILTER_INCLUDE}
     * has been returned
     */
    protected int _fullMatchCount;

    /**
     * Number of tokens for which {@link TokenFilter#FILTER_INCLUDE_BUT_CHECK}
     * has been returned
     */
    protected int _partialMatchCount;

    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */

    public FilteringGeneratorDelegate(JsonGenerator d, TokenFilter f)
    {
        // By default, do NOT delegate copy methods
        super(d, false);
        filter = f;
        // Doesn't matter if it's include or exclude current, but shouldn't be including/excluding sub-tree
        _currentState = TokenFilter.FILTER_CHECK;
        _filterContext = TokenFilterContext.createRootContext(_currentState);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public TokenFilter getTokenFilter() { return filter; }

    /**
     * Accessor for finding number of "full" matches, where specific token and sub-tree
     * starting (if structured type) are passed.
     */
    public int getFullMatchCount() {
        return _partialMatchCount + _fullMatchCount;
    }
    
    /**
     * Accessor for finding number of total matches; both full matches (see
     * {@link #getFullMatchCount()}) and partial matches, latter meaning inclusion
     * of intermediate containers but not necessarily whole sub-tree.
     * This method can be called to check if any content was passed: if <code>0</code>
     * is returned, all content was filtered out and nothing was copied.
     */
    public int getMatchCount() {
        return _partialMatchCount + _fullMatchCount;
    }
    
    /*
    /**********************************************************
    /* Public API, write methods, structural
    /**********************************************************
     */
    
    @Override
    public void writeStartArray() throws IOException
    {
        // First things first: whole-sale skipping easy
        if (_currentState == TokenFilter.FILTER_SKIP) {
            _filterContext = _filterContext.createChildArrayContext(_currentState, false);
            return;
        }
        
        switch (_currentState) {
        case TokenFilter.FILTER_CHECK: // may or may not include, need to check
            int oldState = _currentState;
            _currentState = filter.filterStartArray();
            if (_currentState == TokenFilter.FILTER_INCLUDE) {
                // First: may need to re-create path
                _checkContainerParentPath(oldState, _currentState);
                _filterContext = _filterContext.createChildArrayContext(_currentState, true);
                delegate.writeStartArray();
            } else { // filter out
                _filterContext = _filterContext.createChildArrayContext(_currentState, false);
            }
            return;
        case TokenFilter.FILTER_INCLUDE: // include the whole sub-tree?
        default:
            _filterContext = _filterContext.createChildArrayContext(_currentState, true);
            delegate.writeStartArray();
            return;
        }
    }
        
    @Override
    public void writeStartArray(int size) throws IOException
    {
        // First things first: whole-sale skipping easy
        if (_currentState == TokenFilter.FILTER_SKIP) {
            _filterContext = _filterContext.createChildArrayContext(_currentState, false);
            return;
        }

        switch (_currentState) {
        case TokenFilter.FILTER_CHECK: // may or may not include, need to check
            int oldState = _currentState;
            _currentState = filter.filterStartArray();
            if (_currentState == TokenFilter.FILTER_INCLUDE) {
                // First: may need to re-create path
                _checkContainerParentPath(oldState, _currentState);
                _filterContext = _filterContext.createChildArrayContext(_currentState, true);
                delegate.writeStartArray(size);
            } else { // filter out
                _filterContext = _filterContext.createChildArrayContext(_currentState, false);
            }
            return;
        case TokenFilter.FILTER_INCLUDE: // include the whole sub-tree?
        default:
            _filterContext = _filterContext.createChildArrayContext(_currentState, true);
            delegate.writeStartArray(size);
            return;
        }
    }
    
    @Override
    public void writeEndArray() throws IOException
    {
        if (_filterContext.needsCloseToken()) {
            delegate.writeEndArray();
        }
        _filterContext = _filterContext.getParent();
        if (_filterContext != null) {
            _currentState = _filterContext.getFilterState();
        }
    }

    @Override
    public void writeStartObject() throws IOException
    {
        // First things first: whole-sale skipping easy
        if (_currentState == TokenFilter.FILTER_SKIP) {
            _filterContext = _filterContext.createChildObjectContext(_currentState, false);
            return;
        }

        switch (_currentState) {
        case TokenFilter.FILTER_CHECK: // may or may not include, need to check
            int oldState = _currentState;
            _currentState = filter.filterStartArray();
            if (_currentState == TokenFilter.FILTER_INCLUDE) {
                // First: may need to re-create path
                _checkContainerParentPath(oldState, _currentState);
                _filterContext = _filterContext.createChildObjectContext(_currentState, true);
                delegate.writeStartObject();
            } else { // filter out
                _filterContext = _filterContext.createChildObjectContext(_currentState, false);
            }
            return;
        case TokenFilter.FILTER_INCLUDE: // include the whole sub-tree?
        default:
            _filterContext = _filterContext.createChildObjectContext(_currentState, true);
            delegate.writeStartObject();
            return;
        }
    }
    
    @Override
    public void writeEndObject() throws IOException
    {
        if (_filterContext.needsCloseToken()) {
            delegate.writeEndArray();
        }
        _filterContext = _filterContext.getParent();
        if (_filterContext != null) {
            _currentState = _filterContext.getFilterState();
        }
    }

    @Override
    public void writeFieldName(String name) throws IOException
    {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        
        // !!! TODO
        
        switch (_currentState) {
        case TokenFilter.FILTER_CHECK:
            return;
        case TokenFilter.FILTER_INCLUDE:
        default:
            return;
        }
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException
    {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }

        // !!! TODO
        
        switch (_currentState) {
        case TokenFilter.FILTER_CHECK:
            return;
        case TokenFilter.FILTER_INCLUDE:
        default:
            return;
        }
//        delegate.writeFieldName(name);
    }

    /*
    /**********************************************************
    /* Public API, write methods, text/String values
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException
    {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }

        
        if (_currentState == TokenFilter.FILTER_INCLUDE) {
            delegate.writeString(text);
        } else if (_currentState == TokenFilter.FILTER_CHECK) {
//            if (filter.includeString(value))
        } 
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeString(text, offset, len);
    }

    @Override
    public void writeString(SerializableString text) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeString(text);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRawUTF8String(text, offset, length);
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeUTF8String(text, offset, length);
    }

    /*
    /**********************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(SerializableString raw) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRaw(raw);
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRaw(c);
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRawValue(text);
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeBinary(b64variant, data, offset, len);
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return 0;
        }
        return delegate.writeBinary(b64variant, data, dataLength);
    }

    /*
    /**********************************************************
    /* Public API, write methods, other value types
    /**********************************************************
     */

    @Override
    public void writeNumber(short v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(int v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(double v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(float v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, UnsupportedOperationException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNumber(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeBoolean(state);
    }

    @Override
    public void writeNull() throws IOException {
        if (_currentState == TokenFilter.FILTER_SKIP) {
            return;
        }
        delegate.writeNull();
    }

    /*
    /**********************************************************
    /* Overridden field methods
    /**********************************************************
     */

    @Override
    public void writeOmittedField(String fieldName) throws IOException {
        delegate.writeOmittedField(fieldName);
    }
    
    /*
    /**********************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************
     */

    // 25-Mar-2015, tatu: These are tricky as they sort of predate actual filtering calls.
    //   Let's try to use current state as a clue at least...
    
    @Override
    public void writeObjectId(Object id) throws IOException {
        delegate.writeObjectId(id);
    }

    @Override
    public void writeObjectRef(Object id) throws IOException {
        delegate.writeObjectRef(id);
    }
    
    @Override
    public void writeTypeId(Object id) throws IOException {
        delegate.writeTypeId(id);
    }

    /*
    /**********************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************
     */

    // Base class definitions for these seems correct to me, iff not directly delegating:

    /*
    @Override
    public void writeObject(Object pojo) throws IOException,JsonProcessingException {
        if (delegateCopyMethods) {
            delegate.writeObject(pojo);
            return;
        }
        // NOTE: copied from 
        if (pojo == null) {
            writeNull();
        } else {
            if (getCodec() != null) {
                getCodec().writeValue(this, pojo);
                return;
            }
            _writeSimpleObject(pojo);
        }
    }
    
    @Override
    public void writeTree(TreeNode rootNode) throws IOException {
        if (delegateCopyMethods) {
            delegate.writeTree(rootNode);
            return;
        }
        // As with 'writeObject()', we are not check if write would work
        if (rootNode == null) {
            writeNull();
        } else {
            if (getCodec() == null) {
                throw new IllegalStateException("No ObjectCodec defined");
            }
            getCodec().writeValue(this, rootNode);
        }
    }
    */

    /*
    /**********************************************************
    /* Public API, copy-through methods
    /**********************************************************
     */

    // Base class definitions for these seems correct to me, iff not directly delegating:

    /*
    @Override
    public void copyCurrentEvent(JsonParser jp) throws IOException {
        if (delegateCopyMethods) delegate.copyCurrentEvent(jp);
        else super.copyCurrentEvent(jp);
    }

    @Override
    public void copyCurrentStructure(JsonParser jp) throws IOException {
        if (delegateCopyMethods) delegate.copyCurrentStructure(jp);
        else super.copyCurrentStructure(jp);
    }
    */

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    protected void _checkContainerParentPath(int oldState, int newState)
        throws IOException
    {
        if (newState == TokenFilter.FILTER_INCLUDE) {
            ++_fullMatchCount;
        } else {
            ++_partialMatchCount;
        }
        // only need to construct path if parent wasn't written
        if (oldState == TokenFilter.FILTER_CHECK) {
            // and even then only if parent path is actually desired...
            if (_includePath) {
                _filterContext.writePath(delegate);
            }
            // also: if no multiple matches desired, short-cut checks
            if (newState == TokenFilter.FILTER_INCLUDE && !_filterAll) {
                // Mark parents as "skip" so that further check calls are not made
                _filterContext.skipParentChecks();
            }
        }
    }

    protected void _checkScalarParentPath(int oldState, int newState)
            throws IOException
    {
        if (newState == TokenFilter.FILTER_INCLUDE) {
            ++_fullMatchCount;
        } else {
            ++_partialMatchCount;
        }
        if (oldState == TokenFilter.FILTER_CHECK) {
            // and even then only if parent path is actually desired...
            if (_includePath) {
                _filterContext.writePath(delegate);
            }
            // also: if no multiple matches desired, short-cut checks
            if (newState == TokenFilter.FILTER_INCLUDE && !_filterAll) {
                // Mark parents as "skip" so that further check calls are not made
                _filterContext.skipParentChecks();
            }
        }
    }
}
