package com.fasterxml.jackson.core.filter;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

/**
 * Specialized {@link JsonGeneratorDelegate} that allows use of
 * {@link TokenFilter} for outputting a subset of content that
 * caller tries to generate.
 * 
 * @since 2.6
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
     * State that applies to the item within container, used where applicable.
     * Specifically used to pass inclusion state between property name and
     * property, and also used for array elements.
     */
    protected int _itemState;
    
    /**
     * Number of tokens for which {@link TokenFilter#FILTER_INCLUDE}
     * has been returned
     */
    protected int _matchCount;

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
        _itemState = TokenFilter.FILTER_CHECK;
        _filterContext = TokenFilterContext.createRootContext(_itemState);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    public TokenFilter getTokenFilter() { return filter; }

    /**
     * Accessor for finding number of matches, where specific token and sub-tree
     * starting (if structured type) are passed.
     */
    public int getMatchCount() {
        return _matchCount;
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
        if (_itemState == TokenFilter.FILTER_SKIP) {
            _filterContext = _filterContext.createChildArrayContext(_itemState, false);
            return;
        }
        if (_itemState == TokenFilter.FILTER_INCLUDE) { // include the whole sub-tree?
            _filterContext = _filterContext.createChildArrayContext(_itemState, true);
            delegate.writeStartArray();
            return;
        }
        // Ok; regular checking state then
        _itemState = filter.filterStartArray();
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            // First: may need to re-create path
            _checkParentPath();
            _filterContext = _filterContext.createChildArrayContext(_itemState, true);
            delegate.writeStartArray();
        } else { // filter out
            _filterContext = _filterContext.createChildArrayContext(_itemState, false);
        }
        if (_itemState != TokenFilter.FILTER_SKIP) {
            _filterContext.markNeedsCloseCheck();
        }
    }
        
    @Override
    public void writeStartArray(int size) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            _filterContext = _filterContext.createChildArrayContext(_itemState, false);
            return;
        }
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            _filterContext = _filterContext.createChildArrayContext(_itemState, true);
            delegate.writeStartArray(size);
            return;
        }
        _itemState = filter.filterStartArray();
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            _checkParentPath();
            _filterContext = _filterContext.createChildArrayContext(_itemState, true);
            delegate.writeStartArray(size);
        } else {
            _filterContext = _filterContext.createChildArrayContext(_itemState, false);
        }
        if (_itemState != TokenFilter.FILTER_SKIP) {
            _filterContext.markNeedsCloseCheck();
        }
    }
    
    @Override
    public void writeEndArray() throws IOException
    {
        if (_filterContext.needsCloseToken()) {
            delegate.writeEndArray();
        }
        if (_filterContext.needsCloseCheck()) {
            filter.filterFinishArray();
        }
        _filterContext = _filterContext.getParent();
        if (_filterContext != null) {
            _itemState = _filterContext.getFilterState();
        }
    }

    @Override
    public void writeStartObject() throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            _filterContext = _filterContext.createChildObjectContext(_itemState, false);
            return;
        }
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            _filterContext = _filterContext.createChildArrayContext(_itemState, true);
            delegate.writeStartObject();
            return;
        }
        _itemState = filter.filterStartObject();
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            _checkParentPath();
            _filterContext = _filterContext.createChildObjectContext(_itemState, true);
            delegate.writeStartObject();
        } else { // filter out
            _filterContext = _filterContext.createChildObjectContext(_itemState, false);
        }
        if (_itemState != TokenFilter.FILTER_SKIP) {
            _filterContext.markNeedsCloseCheck();
        }
    }
    
    @Override
    public void writeEndObject() throws IOException
    {
        if (_filterContext.needsCloseToken()) {
            delegate.writeEndArray();
        }
        if (_filterContext.needsCloseCheck()) {
            filter.filterFinishObject();
        }
        _filterContext = _filterContext.getParent();
        if (_filterContext != null) {
            _itemState = _filterContext.getFilterState();
        }
    }

    @Override
    public void writeFieldName(String name) throws IOException
    {
        // Bit different here: we will actually need state of parent container
        int state = _filterContext.setFieldName(name);

        // used as-is for basic include/skip, but not if checking is needed
        if (state == TokenFilter.FILTER_CHECK) {
            state = filter.includeProperty(name);
            if (state == TokenFilter.FILTER_INCLUDE) {
                _checkParentPath();
            }
        }
        _itemState = state;
        if (state == TokenFilter.FILTER_INCLUDE) {
            delegate.writeFieldName(name);
        }
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException
    {
        int state = _filterContext.setFieldName(name.getValue());

        if (state == TokenFilter.FILTER_CHECK) {
            state = filter.includeProperty(name.getValue());
            if (state == TokenFilter.FILTER_INCLUDE) {
                _checkParentPath();
            }
        }
        _itemState = state;
        if (state == TokenFilter.FILTER_INCLUDE) {
            delegate.writeFieldName(name);
        }
    }

    /*
    /**********************************************************
    /* Public API, write methods, text/String values
    /**********************************************************
     */

    @Override
    public void writeString(String value) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            int state = _filterContext.checkValue(filter);
            if (state == TokenFilter.FILTER_SKIP) {
                return;
            }
            if (state == TokenFilter.FILTER_CHECK) {
                if (!filter.includeString(value)) {
                    return;
                }
            }
            _checkParentPath();
            // one important thing: may need to write element name now
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeString(value);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            String value = new String(text, offset, len);
            int state = _filterContext.checkValue(filter);
            if (state == TokenFilter.FILTER_SKIP) {
                return;
            }
            if (state == TokenFilter.FILTER_CHECK) {
                if (!filter.includeString(value)) {
                    return;
                }
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeString(text, offset, len);
    }

    @Override
    public void writeString(SerializableString value) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            int state = _filterContext.checkValue(filter);
            if (state == TokenFilter.FILTER_SKIP) {
                return;
            }
            if (state == TokenFilter.FILTER_CHECK) {
                if (!filter.includeString(value.getValue())) {
                    return;
                }
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeString(value);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRawUTF8String(text, offset, length);
        }
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException
    {
        // not exact match, but best we can do
        if (_checkRawValueWrite()) {
            delegate.writeRawUTF8String(text, offset, length);
        }
    }

    /*
    /**********************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text);
        }
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text);
        }
    }

    @Override
    public void writeRaw(SerializableString text) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text);
        }
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeRaw(char c) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(c);
        }
    }

    @Override
    public void writeRawValue(String text) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text);
        }
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException
    {
        if (_checkBinaryWrite()) {
            delegate.writeBinary(b64variant, data, offset, len);
        }
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException
    {
        if (_checkBinaryWrite()) {
            return delegate.writeBinary(b64variant, data, dataLength);
        }
        return -1;
    }

    /*
    /**********************************************************
    /* Public API, write methods, other value types
    /**********************************************************
     */

    @Override
    public void writeNumber(short v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(int v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(double v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(float v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNumber(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, UnsupportedOperationException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeRawValue()) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeNumber(encodedValue);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeBoolean(v)) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
        } 
        delegate.writeBoolean(v);
    }

    @Override
    public void writeNull() throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return;
        }
        if (_itemState == TokenFilter.FILTER_CHECK) {
            if (!filter.includeNull()) { // close enough?
                return;
            }
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
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
        // Hmmh. Not sure how this would work but...
        if (_itemState != TokenFilter.FILTER_SKIP) {
            return;
        }
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
        if (_itemState != TokenFilter.FILTER_SKIP) {
            delegate.writeObjectId(id);
        }
    }

    @Override
    public void writeObjectRef(Object id) throws IOException {
        if (_itemState != TokenFilter.FILTER_SKIP) {
            delegate.writeObjectRef(id);
        }
    }
    
    @Override
    public void writeTypeId(Object id) throws IOException {
        if (_itemState != TokenFilter.FILTER_SKIP) {
            delegate.writeTypeId(id);
        }
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
    
    protected void _checkParentPath() throws IOException
    {
        ++_matchCount;
        // only need to construct path if parent wasn't written
        if (_includePath) {
            _filterContext.writePath(delegate);
        }
        // also: if no multiple matches desired, short-cut checks
        if (!_filterAll) {
            // Mark parents as "skip" so that further check calls are not made
            _filterContext.skipParentChecks();
        }
    }

    protected boolean _checkBinaryWrite() throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return false;
        }
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            return true;
        }
        if (filter.includeBinary()) { // close enough?
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
            return true;
        }
        return false;
    }
    
    protected boolean _checkRawValueWrite() throws IOException
    {
        if (_itemState == TokenFilter.FILTER_SKIP) {
            return false;
        }
        if (_itemState == TokenFilter.FILTER_INCLUDE) {
            return true;
        }
        if (filter.includeRawValue()) { // close enough?
            _checkParentPath();
            if (_filterContext.inObject()) {
                delegate.writeFieldName(_filterContext.getCurrentName());
            }
            return true;
        }
        return false;
    }
}
