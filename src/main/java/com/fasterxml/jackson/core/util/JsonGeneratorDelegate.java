package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

public class JsonGeneratorDelegate extends JsonGenerator
{
    /**
     * Delegate object that method calls are delegated to.
     */
    protected JsonGenerator delegate;

    /**
     * Whether copy methods
     * ({@link #copyCurrentEvent}, {@link #copyCurrentStructure},
     * {@link #writeTree} and {@link #writePOJO})
     * are to be called (true), or handled by this object (false).
     */
    protected boolean delegateCopyMethods;

    /*
    /**********************************************************************
    /* Construction, initialization
    /**********************************************************************
     */

    public JsonGeneratorDelegate(JsonGenerator d) {
        this(d, true);
    }

    /**
     * @param d Underlying generator to delegate calls to
     * @param delegateCopyMethods Flag assigned to <code>delagateCopyMethod</code>
     *   and which defines whether copy methods are handled locally (false), or
     *   delegated to configured 
     */
    public JsonGeneratorDelegate(JsonGenerator d, boolean delegateCopyMethods) {
        delegate = d;
        this.delegateCopyMethods = delegateCopyMethods;
    }

    @Override
    public Object currentValue() {
        return delegate.currentValue();
    }

    @Override
    public void assignCurrentValue(Object v) {
        delegate.assignCurrentValue(v);
    }

    /*
    /**********************************************************************
    /* Public API, metadata
    /**********************************************************************
     */

    @Override public FormatSchema getSchema() { return delegate.getSchema(); }
    @Override public Version version() { return delegate.version(); }
    @Override public Object streamWriteOutputTarget() { return delegate.streamWriteOutputTarget(); }
    @Override public int streamWriteOutputBuffered() { return delegate.streamWriteOutputBuffered(); }

    /*
    /**********************************************************************
    /* Public API, capability introspection
    /**********************************************************************
     */

    @Override
    public boolean canWriteTypeId() { return delegate.canWriteTypeId(); }

    @Override
    public boolean canWriteObjectId() { return delegate.canWriteObjectId(); }

    @Deprecated
    @Override
    public boolean canWriteBinaryNatively() { return delegate.canWriteBinaryNatively(); }
    
    @Override
    public boolean canOmitProperties() { return delegate.canOmitProperties(); }

    @Override
    public boolean canWriteFormattedNumbers() { return delegate.canWriteFormattedNumbers(); }

    @Override
    public JacksonFeatureSet<StreamWriteCapability> streamWriteCapabilities() {
        return delegate.streamWriteCapabilities();
    }

    /*
    /**********************************************************************
    /* Public API, configuration
    /**********************************************************************
     */

    @Override
    public boolean isEnabled(StreamWriteFeature f) { return delegate.isEnabled(f); }

    @Override
    public int streamWriteFeatures() { return delegate.streamWriteFeatures(); }

    @Override
    public JsonGenerator configure(StreamWriteFeature f, boolean state) {
        delegate.configure(f, state);
        return this;
    }

    /*
    /**********************************************************************
    /* Configuring generator
    /**********************************************************************
      */

    @Override
    public int getHighestNonEscapedChar() { return delegate.getHighestNonEscapedChar(); }

    @Override
    public CharacterEscapes getCharacterEscapes() {  return delegate.getCharacterEscapes(); }

    /*
    /**********************************************************************
    /* Public API, write methods, structural
    /**********************************************************************
     */

    @Override
    public void writeStartArray() throws JacksonException { delegate.writeStartArray(); }

    @Override
    public void writeStartArray(Object forValue) throws JacksonException { delegate.writeStartArray(forValue); }

    @Override
    public void writeStartArray(Object forValue, int size) throws JacksonException { delegate.writeStartArray(forValue, size); }

    @Override
    public void writeEndArray() throws JacksonException { delegate.writeEndArray(); }

    @Override
    public void writeStartObject() throws JacksonException { delegate.writeStartObject(); }

    @Override
    public void writeStartObject(Object forValue) throws JacksonException { delegate.writeStartObject(forValue); }

    @Override
    public void writeStartObject(Object forValue, int size) throws JacksonException {
        delegate.writeStartObject(forValue, size);
    }

    @Override
    public void writeEndObject() throws JacksonException { delegate.writeEndObject(); }

    @Override
    public void writeName(String name) throws JacksonException {
        delegate.writeName(name);
    }

    @Override
    public void writeName(SerializableString name) throws JacksonException {
        delegate.writeName(name);
    }

    @Override
    public void writePropertyId(long id) throws JacksonException {
        delegate.writePropertyId(id);
    }

    @Override
    public void writeArray(int[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
    }

    @Override
    public void writeArray(long[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
    }

    @Override
    public void writeArray(double[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
    }

    @Override
    public void writeArray(String[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
    }

    /*
    /**********************************************************************
    /* Public API, write methods, text/String values
    /**********************************************************************
     */

    @Override
    public void writeString(String text) throws JacksonException { delegate.writeString(text); }

    @Override
    public void writeString(Reader reader, int len) throws JacksonException {
        delegate.writeString(reader, len);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws JacksonException { delegate.writeString(text, offset, len); }

    @Override
    public void writeString(SerializableString text) throws JacksonException { delegate.writeString(text); }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length) throws JacksonException { delegate.writeRawUTF8String(text, offset, length); }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length) throws JacksonException { delegate.writeUTF8String(text, offset, length); }

    /*
    /**********************************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************************
     */

    @Override
    public void writeRaw(String text) throws JacksonException { delegate.writeRaw(text); }

    @Override
    public void writeRaw(String text, int offset, int len) throws JacksonException { delegate.writeRaw(text, offset, len); }

    @Override
    public void writeRaw(SerializableString raw) throws JacksonException { delegate.writeRaw(raw); }
    
    @Override
    public void writeRaw(char[] text, int offset, int len) throws JacksonException { delegate.writeRaw(text, offset, len); }

    @Override
    public void writeRaw(char c) throws JacksonException { delegate.writeRaw(c); }

    @Override
    public void writeRawValue(String text) throws JacksonException { delegate.writeRawValue(text); }

    @Override
    public void writeRawValue(String text, int offset, int len) throws JacksonException { delegate.writeRawValue(text, offset, len); }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws JacksonException { delegate.writeRawValue(text, offset, len); }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws JacksonException { delegate.writeBinary(b64variant, data, offset, len); }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws JacksonException { return delegate.writeBinary(b64variant, data, dataLength); }

    /*
    /**********************************************************************
    /* Public API, write methods, other value types
    /**********************************************************************
     */

    @Override
    public void writeNumber(short v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(int v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(long v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(BigInteger v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(double v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(float v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(BigDecimal v) throws JacksonException { delegate.writeNumber(v); }

    @Override
    public void writeNumber(String encodedValue) throws JacksonException, UnsupportedOperationException { delegate.writeNumber(encodedValue); }

    @Override
    public void writeNumber(char[] encodedValueBuffer, int offset, int length) throws JacksonException, UnsupportedOperationException { delegate.writeNumber(encodedValueBuffer, offset, length); }

    @Override
    public void writeBoolean(boolean state) throws JacksonException { delegate.writeBoolean(state); }
    
    @Override
    public void writeNull() throws JacksonException { delegate.writeNull(); }

    /*
    /**********************************************************************
    /* Public API, convenience property-write methods
    /**********************************************************************
     */

    // 04-Oct-2019, tatu: Reminder: these should NOT be delegated, unless matching
    //    methods in `FilteringGeneratorDelegate` are re-defined to "split" calls again

//    public void writeBinaryProperty(String propName, byte[] data) throws JacksonException {
//    public void writeBooleanProperty(String propName, boolean value) throws JacksonException {
//    public void writeNullProperty(String propName) throws JacksonException {
//    public void writeStringProperty(String propName, String value) throws JacksonException {
//    public void writeNumberProperty(String propName, short value) throws JacksonException {

//    public void writeArrayPropertyStart(String propName) throws JacksonException {
//    public void writeObjectPropertyStart(String propName) throws JacksonException {
//    public void writePOJOProperty(String propName, Object pojo) throws JacksonException {

    // Sole exception being this method as it is not a "combo" method
    
    @Override
    public void writeOmittedProperty(String propName) throws JacksonException {
        delegate.writeOmittedProperty(propName);
    }

    /*
    /**********************************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************************
     */

    @Override
    public void writeObjectId(Object id) throws JacksonException { delegate.writeObjectId(id); }

    @Override
    public void writeObjectRef(Object id) throws JacksonException { delegate.writeObjectRef(id); }

    @Override
    public void writeTypeId(Object id) throws JacksonException { delegate.writeTypeId(id); }

    @Override
    public void writeEmbeddedObject(Object object) throws JacksonException { delegate.writeEmbeddedObject(object); }

    /*
    /**********************************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************************
     */
    
    @Override
    public void writePOJO(Object pojo) throws JacksonException {
        if (delegateCopyMethods) {
            delegate.writePOJO(pojo);
            return;
        }
        if (pojo == null) {
            writeNull();
        } else {
            objectWriteContext().writeValue(this, pojo);
        }
    }
    
    @Override
    public void writeTree(TreeNode tree) throws JacksonException {
        if (delegateCopyMethods) {
            delegate.writeTree(tree);
            return;
        }
        // As with 'writeObject()', we are not check if write would work
        if (tree == null) {
            writeNull();
        } else {
            objectWriteContext().writeTree(this, tree);
        }
    }

    /*
    /**********************************************************************
    /* Public API, convenience property write methods
    /**********************************************************************
     */

    // // These are fine, just delegate to other methods...

    /*
    /**********************************************************************
    /* Public API, copy-through methods
    /**********************************************************************
     */

    @Override
    public void copyCurrentEvent(JsonParser p) throws JacksonException {
        if (delegateCopyMethods) delegate.copyCurrentEvent(p);
        else super.copyCurrentEvent(p);
    }

    @Override
    public void copyCurrentStructure(JsonParser p) throws JacksonException {
        if (delegateCopyMethods) delegate.copyCurrentStructure(p);
        else super.copyCurrentStructure(p);
    }

    /*
    /**********************************************************************
    /* Public API, context access
    /**********************************************************************
     */

    @Override public TokenStreamContext streamWriteContext() { return delegate.streamWriteContext(); }
    @Override public ObjectWriteContext objectWriteContext() { return delegate.objectWriteContext(); }

    /*
    /**********************************************************************
    /* Public API, buffer handling
    /**********************************************************************
     */
    
    @Override public void flush() { delegate.flush(); }
    @Override public void close() { delegate.close(); }

    /*
    /**********************************************************************
    /* Closeable implementation
    /**********************************************************************
     */
    
    @Override public boolean isClosed() { return delegate.isClosed(); }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * @return Underlying generator that calls are delegated to
     */
    public JsonGenerator delegate() { return delegate; }
}
