package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;

/**
 * @since 2.6.0
 */
public class FilteringGeneratorDelegate extends JsonGeneratorDelegate
{
    protected TokenFilter filter;

    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */

    public FilteringGeneratorDelegate(JsonGenerator d, TokenFilter f) {
        // By default, do NOT delegate copy methods
        super(d, false);
        filter = f;
    }

    /*
    /**********************************************************
    /* Public API, write methods, structural
    /**********************************************************
     */

    @Override
    public void writeStartArray() throws IOException {
        delegate.writeStartArray();
    }

    @Override
    public void writeStartArray(int size) throws IOException {
        delegate.writeStartArray(size);
    }
    
    @Override
    public void writeEndArray() throws IOException {
        delegate.writeEndArray();
    }

    @Override
    public void writeStartObject() throws IOException {
        delegate.writeStartObject();
    }
    
    @Override
    public void writeEndObject() throws IOException {
        delegate.writeEndObject();
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        delegate.writeFieldName(name);
    }

    @Override
    public void writeFieldName(SerializableString name) throws IOException {
        delegate.writeFieldName(name);
    }

    /*
    /**********************************************************
    /* Public API, write methods, text/String values
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException {
        delegate.writeString(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException {
        delegate.writeString(text, offset, len);
    }

    @Override
    public void writeString(SerializableString text) throws IOException {
        delegate.writeString(text);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws IOException {
        delegate.writeRawUTF8String(text, offset, length);
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws IOException {
        delegate.writeUTF8String(text, offset, length);
    }

    /*
    /**********************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException {
        delegate.writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(SerializableString raw) throws IOException {
        delegate.writeRaw(raw);
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws IOException {
        delegate.writeRaw(c);
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        delegate.writeRawValue(text);
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException {
        delegate.writeBinary(b64variant, data, offset, len);
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException {
        return delegate.writeBinary(b64variant, data, dataLength);
    }

    /*
    /**********************************************************
    /* Public API, write methods, other value types
    /**********************************************************
     */

    @Override
    public void writeNumber(short v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(int v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(double v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(float v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, UnsupportedOperationException {
        delegate.writeNumber(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException {
        delegate.writeBoolean(state);
    }

    @Override
    public void writeNull() throws IOException {
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
}
