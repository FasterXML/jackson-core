package com.fasterxml.jackson.core.filter;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;

/**
 * Specialized {@link JsonGeneratorDelegate} that allows use of
 * {@link TokenFilter} for outputting a subset of content that
 * caller tries to generate.
 */
public class FilteringGeneratorDelegate extends JsonGeneratorDelegate
{
    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Object consulted to determine whether to write parts of content generator
     * is asked to write or not.
     */
    protected TokenFilter rootFilter;

    /**
     * Flag that determines whether filtering will continue after the first
     * match is indicated or not: if `false`, output is based on just the first
     * full match (returning {@link TokenFilter#INCLUDE_ALL}) and no more
     * checks are made; if `true` then filtering will be applied as necessary
     * until end of content.
     */
    protected boolean _allowMultipleMatches;

    /**
     * Flag that determines whether path leading up to included content should
     * also be automatically included or not. If `false`, no path inclusion is
     * done and only explicitly included entries are output; if `true` then
     * path from main level down to match is also included as necessary.
     */
    protected TokenFilter.Inclusion _inclusion;

    /*
    /**********************************************************************
    /* Additional state
    /**********************************************************************
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
    protected TokenFilter _itemFilter;
    
    /**
     * Number of tokens for which {@link TokenFilter#INCLUDE_ALL}
     * has been returned
     */
    protected int _matchCount;

    /*
    /**********************************************************************
    /* Construction, initialization
    /**********************************************************************
     */

    /**
     * @param d Generator to delegate calls to
     * @param f Filter to use
     * @param inclusion Definition of inclusion criteria
     * @param allowMultipleMatches Whether to allow multiple matches
     */
    public FilteringGeneratorDelegate(JsonGenerator d, TokenFilter f,
            TokenFilter.Inclusion inclusion, boolean allowMultipleMatches)
    {
        // By default, do NOT delegate copy methods
        super(d, false);
        rootFilter = f;
        // and this is the currently active filter for root values
        _itemFilter = f;
        _filterContext = TokenFilterContext.createRootContext(f);
        _inclusion = inclusion;
        _allowMultipleMatches = allowMultipleMatches;
    }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    public TokenFilter getFilter() { return rootFilter; }

    public TokenStreamContext getFilterContext() {
        return _filterContext;
    }
    
    /**
     * Accessor for finding number of matches, where specific token and sub-tree
     * starting (if structured type) are passed.
     *
     * @return Number of matches
     */
    public int getMatchCount() {
        return _matchCount;
    }

    /*
    /**********************************************************************
    /* Public API, accessors
    /**********************************************************************
     */
    
    @Override
    public TokenStreamContext getOutputContext() {
        /* 11-Apr-2015, tatu: Choice is between pre- and post-filter context;
         *   let's expose post-filter context that correlates with the view
         *   of caller.
         */
        return _filterContext;
    }
    
    /*
    /**********************************************************************
    /* Public API, write methods, structural
    /**********************************************************************
     */
    
    @Override
    public void writeStartArray() throws JacksonException
    {
        // First things first: whole-sale skipping easy
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildArrayContext(null, null, false);
            return;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) { // include the whole sub-tree?
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, null, true);
            delegate.writeStartArray();
            return;
        }
        // Ok; regular checking state then
        _itemFilter = _filterContext.checkValue(_itemFilter);
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildArrayContext(null, null, false);
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            _itemFilter = _itemFilter.filterStartArray();
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _checkParentPath();
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, null, true);
            delegate.writeStartArray();
        } else if (_itemFilter != null && _inclusion == Inclusion.INCLUDE_NON_NULL) {
            _checkParentPath(false /* isMatch */);
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, null, true);
            delegate.writeStartArray();
        } else {
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, null, false);
        }
    }

    @Override
    public void writeStartArray(Object currValue) throws JacksonException
    {
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildArrayContext(null, currValue, false);
            return;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, true);
            delegate.writeStartArray(currValue);
            return;
        }
        _itemFilter = _filterContext.checkValue(_itemFilter);
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildArrayContext(null, currValue, false);
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            _itemFilter = _itemFilter.filterStartArray();
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _checkParentPath();
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, true);
            delegate.writeStartArray(currValue);
        } else if (_itemFilter != null && _inclusion == Inclusion.INCLUDE_NON_NULL) {
            _checkParentPath(false /* isMatch */);
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, true);
            delegate.writeStartArray(currValue);
        } else {
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, false);
        }
    }

    @Override
    public void writeStartArray(Object currValue, int size) throws JacksonException
    {
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildArrayContext(null, currValue, false);
            return;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, true);
            delegate.writeStartArray(currValue, size);
            return;
        }
        _itemFilter = _filterContext.checkValue(_itemFilter);
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildArrayContext(null, currValue, false);
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            _itemFilter = _itemFilter.filterStartArray();
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _checkParentPath();
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, true);
            delegate.writeStartArray(currValue, size);
        } else {
            _filterContext = _filterContext.createChildArrayContext(_itemFilter, currValue, false);
        }
    }

    @Override
    public void writeEndArray() throws JacksonException
    {
        _filterContext = _filterContext.closeArray(delegate);

        if (_filterContext != null) {
            _itemFilter = _filterContext.getFilter();
        }
    }

    @Override
    public void writeStartObject() throws JacksonException
    {
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildObjectContext(_itemFilter, null, false);
            return;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _filterContext = _filterContext.createChildObjectContext(_itemFilter, null, true);
            delegate.writeStartObject();
            return;
        }

        TokenFilter f = _filterContext.checkValue(_itemFilter);
        if (f == null) {
            return;
        }
        
        if (f != TokenFilter.INCLUDE_ALL) {
            f = f.filterStartObject();
        }
        if (f == TokenFilter.INCLUDE_ALL) {
            _checkParentPath();
            _filterContext = _filterContext.createChildObjectContext(f, null, true);
            delegate.writeStartObject();
        } else if (f != null && _inclusion == Inclusion.INCLUDE_NON_NULL) {
            _checkParentPath(false /* isMatch */);
            _filterContext = _filterContext.createChildObjectContext(f, null, true);
            delegate.writeStartObject();
        } else { // filter out
            _filterContext = _filterContext.createChildObjectContext(f, null, false);
        }
    }

    @Override
    public void writeStartObject(Object currValue) throws JacksonException
    {
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildObjectContext(_itemFilter, currValue, false);
            return;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _filterContext = _filterContext.createChildObjectContext(_itemFilter, currValue, true);
            delegate.writeStartObject(currValue);
            return;
        }

        TokenFilter f = _filterContext.checkValue(_itemFilter);
        if (f == null) {
            return;
        }

        if (f != TokenFilter.INCLUDE_ALL) {
            f = f.filterStartObject();
        }
        if (f == TokenFilter.INCLUDE_ALL) {
            _checkParentPath();
            _filterContext = _filterContext.createChildObjectContext(f, currValue, true);
            delegate.writeStartObject(currValue);
        } else if (f != null && _inclusion == Inclusion.INCLUDE_NON_NULL) {
            _checkParentPath(false /* isMatch */);
            _filterContext = _filterContext.createChildObjectContext(f, currValue, true);
            delegate.writeStartObject(currValue);
        } else { // filter out
            _filterContext = _filterContext.createChildObjectContext(f, currValue, false);
        }
    }

    @Override
    public void writeStartObject(Object currValue, int size) throws JacksonException
    {
        if (_itemFilter == null) {
            _filterContext = _filterContext.createChildObjectContext(_itemFilter, currValue, false);
            return;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            _filterContext = _filterContext.createChildObjectContext(_itemFilter, currValue, true);
            delegate.writeStartObject(currValue, size);
            return;
        }

        TokenFilter f = _filterContext.checkValue(_itemFilter);
        if (f == null) {
            return;
        }

        if (f != TokenFilter.INCLUDE_ALL) {
            f = f.filterStartObject();
        }
        if (f == TokenFilter.INCLUDE_ALL) {
            _checkParentPath();
            _filterContext = _filterContext.createChildObjectContext(f, currValue, true);
            delegate.writeStartObject(currValue, size);
        } else { // filter out
            _filterContext = _filterContext.createChildObjectContext(f, currValue, false);
        }
    }

    @Override
    public void writeEndObject() throws JacksonException
    {
        _filterContext = _filterContext.closeObject(delegate);
        if (_filterContext != null) {
            _itemFilter = _filterContext.getFilter();
        }
    }

    @Override
    public void writeFieldName(String name) throws JacksonException
    {
        TokenFilter state = _filterContext.setFieldName(name);
        if (state == null) {
            _itemFilter = null;
            return;
        }
        if (state == TokenFilter.INCLUDE_ALL) {
            _itemFilter = state;
            delegate.writeFieldName(name);
            return;
        }
        state = state.includeProperty(name);
        _itemFilter = state;
        if (state == TokenFilter.INCLUDE_ALL) {
            _checkPropertyParentPath();
        }
    }

    @Override
    public void writeFieldName(SerializableString name) throws JacksonException
    {
        TokenFilter state = _filterContext.setFieldName(name.getValue());
        if (state == null) {
            _itemFilter = null;
            return;
        }
        if (state == TokenFilter.INCLUDE_ALL) {
            _itemFilter = state;
            delegate.writeFieldName(name);
            return;
        }
        state = state.includeProperty(name.getValue());
        _itemFilter = state;
        if (state == TokenFilter.INCLUDE_ALL) {
            _checkPropertyParentPath();
        }
    }

    // 02-Dec-2019, tatu: Not sure what else to do... so use default impl from base class
    @Override
    public void writeFieldId(long id) throws JacksonException {
        writeFieldName(Long.toString(id));
    }

    /*
    /**********************************************************************
    /* Public API, write methods, text/String values
    /**********************************************************************
     */

    @Override
    public void writeString(String value) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeString(value)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeString(value);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            String value = new String(text, offset, len);
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeString(value)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeString(text, offset, len);
    }

    @Override
    public void writeString(SerializableString value) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeString(value.getValue())) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeString(value);
    }

    @Override
    public void writeString(Reader reader, int len) throws JacksonException {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                // [core#609]: do need to implement, but with 2.10.x TokenFilter no
                // useful method to call so will be mostly unfiltered
                if (!state.includeString(reader, len)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeString(reader, len);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRawUTF8String(text, offset, length);
        }
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws JacksonException
    {
        // not exact match, but best we can do
        if (_checkRawValueWrite()) {
            delegate.writeUTF8String(text, offset, length);
        }
    }

    /*
    /**********************************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************************
     */

    @Override
    public void writeRaw(String text) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text);
        }
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeRaw(SerializableString text) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text);
        }
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(text, offset, len);
        }
    }

    @Override
    public void writeRaw(char c) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRaw(c);
        }
    }

    @Override
    public void writeRawValue(String text) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRawValue(text);
        }
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRawValue(text, offset, len);
        }
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws JacksonException
    {
        if (_checkRawValueWrite()) {
            delegate.writeRawValue(text, offset, len);
        }
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws JacksonException
    {
        if (_checkBinaryWrite()) {
            delegate.writeBinary(b64variant, data, offset, len);
        }
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws JacksonException
    {
        if (_checkBinaryWrite()) {
            return delegate.writeBinary(b64variant, data, dataLength);
        }
        return -1;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, other value types
    /**********************************************************************
     */

    @Override
    public void writeNumber(short v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(int v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(double v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(float v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigDecimal v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNumber(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(String encodedValue) throws JacksonException, UnsupportedOperationException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeRawValue()) { // close enough?
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(encodedValue);
    }

    @Override
    public void writeNumber(char[] encodedValueBuffer, int offset, int length) throws JacksonException, UnsupportedOperationException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeRawValue()) { // close enough?
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNumber(encodedValueBuffer, offset, length);
    }

    @Override
    public void writeBoolean(boolean v) throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeBoolean(v)) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeBoolean(v);
    }

    @Override
    public void writeNull() throws JacksonException
    {
        if (_itemFilter == null) {
            return;
        }
        if (_itemFilter != TokenFilter.INCLUDE_ALL) {
            TokenFilter state = _filterContext.checkValue(_itemFilter);
            if (state == null) {
                return;
            }
            if (state != TokenFilter.INCLUDE_ALL) {
                if (!state.includeNull()) {
                    return;
                }
            }
            _checkParentPath();
        }
        delegate.writeNull();
    }

    /*
    /**********************************************************************
    /* Overridden field methods
    /**********************************************************************
     */

    @Override
    public void writeOmittedField(String fieldName) throws JacksonException {
        // Hmmh. Not sure how this would work but...
        if (_itemFilter != null) {
            delegate.writeOmittedField(fieldName);
        }
    }

    /*
    /**********************************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************************
     */

    // 25-Mar-2015, tatu: These are tricky as they sort of predate actual filtering calls.
    //   Let's try to use current state as a clue at least...
    
    @Override
    public void writeObjectId(Object id) throws JacksonException {
        if (_itemFilter != null) {
            delegate.writeObjectId(id);
        }
    }

    @Override
    public void writeObjectRef(Object id) throws JacksonException {
        if (_itemFilter != null) {
            delegate.writeObjectRef(id);
        }
    }
    
    @Override
    public void writeTypeId(Object id) throws JacksonException {
        if (_itemFilter != null) {
            delegate.writeTypeId(id);
        }
    }

    /*
    /**********************************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************************
     */

    // Base class definitions for these seems correct to me, iff not directly delegating:

    /*
    @Override
    public void writeObject(Object pojo) {
...
    }
    
    @Override
    public void writeTree(TreeNode rootNode) {
...    
    }
    */

    /*
    /**********************************************************************
    /* Public API, copy-through methods
    /**********************************************************************
     */

    // Base class definitions for these seems correct to me, iff not directly delegating:

    /*
    @Override
    public void copyCurrentEvent(JsonParser p) {
        if (delegateCopyMethods) delegate.copyCurrentEvent(p);
        else super.copyCurrentEvent(p);
    }

    @Override
    public void copyCurrentStructure(JsonParser p) {
        if (delegateCopyMethods) delegate.copyCurrentStructure(p);
        else super.copyCurrentStructure(p);
    }
    */

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    protected void _checkParentPath() throws JacksonException
    {
        _checkParentPath(true);
    }

    protected void  _checkParentPath(boolean isMatch) throws JacksonException
    {
        if (isMatch) {
            ++_matchCount;
        }
        // only need to construct path if parent wasn't written
        if (_inclusion == Inclusion.INCLUDE_ALL_AND_PATH) {
            _filterContext.writePath(delegate);
        } else if (_inclusion == Inclusion.INCLUDE_NON_NULL) {
            // path has already been written, except for maybe field name
            _filterContext.ensureFieldNameWritten(delegate);
        }
        // also: if no multiple matches desired, short-cut checks
        if (isMatch && !_allowMultipleMatches) {
            // Mark parents as "skip" so that further check calls are not made
            _filterContext.skipParentChecks();
        }
    }

    /**
     * Specialized variant of {@link #_checkParentPath} used when checking
     * parent for a property name to be included with value: rules are slightly
     * different.
     *
     * @throws JacksonException If there is an issue with possible resulting read
     */
    protected void _checkPropertyParentPath() throws JacksonException
    {
        ++_matchCount;
        if (_inclusion == Inclusion.INCLUDE_ALL_AND_PATH) {
            _filterContext.writePath(delegate);
        } else if (_inclusion == Inclusion.INCLUDE_NON_NULL) {
            // path has already been written, except for maybe field name
            _filterContext.ensureFieldNameWritten(delegate);
        }
        // also: if no multiple matches desired, short-cut checks
        if (!_allowMultipleMatches) {
            // Mark parents as "skip" so that further check calls are not made
            _filterContext.skipParentChecks();
        }
    }
    
    protected boolean _checkBinaryWrite() throws JacksonException
    {
        if (_itemFilter == null) {
            return false;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            return true;
        }
        if (_itemFilter.includeBinary()) { // close enough?
            _checkParentPath();
            return true;
        }
        return false;
    }
    
    protected boolean _checkRawValueWrite() throws JacksonException
    {
        if (_itemFilter == null) {
            return false;
        }
        if (_itemFilter == TokenFilter.INCLUDE_ALL) {
            return true;
        }
        if (_itemFilter.includeRawValue()) { // close enough?
            _checkParentPath();
            return true;
        }
        return false;
    }
}
