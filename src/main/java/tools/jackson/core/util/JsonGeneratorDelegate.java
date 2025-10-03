package tools.jackson.core.util;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.io.CharacterEscapes;

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

    @Override
    public boolean canOmitProperties() { return delegate.canOmitProperties(); }

    @Override
    public boolean has(StreamWriteCapability capability) { return delegate.has(capability); }

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

    @Override
    public PrettyPrinter getPrettyPrinter() { return delegate.getPrettyPrinter(); }

    /*
    /**********************************************************************
    /* Public API, write methods, structural
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeStartArray() throws JacksonException {
        delegate.writeStartArray();
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue) throws JacksonException {
        delegate.writeStartArray(forValue);
        return this;
    }

    @Override
    public JsonGenerator writeStartArray(Object forValue, int size) throws JacksonException {
        delegate.writeStartArray(forValue, size);
        return this;
    }

    @Override
    public JsonGenerator writeEndArray() throws JacksonException {
        delegate.writeEndArray();
        return this;
    }

    @Override
    public JsonGenerator writeStartObject() throws JacksonException {
        delegate.writeStartObject();
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue) throws JacksonException {
        delegate.writeStartObject(forValue);
        return this;
    }

    @Override
    public JsonGenerator writeStartObject(Object forValue, int size) throws JacksonException {
        delegate.writeStartObject(forValue, size);
        return this;
    }

    @Override
    public JsonGenerator writeEndObject() throws JacksonException {
        delegate.writeEndObject();
        return this;
    }

    @Override
    public JsonGenerator writeName(String name) throws JacksonException {
        delegate.writeName(name);
        return this;
    }

    @Override
    public JsonGenerator writeName(SerializableString name) throws JacksonException {
        delegate.writeName(name);
        return this;
    }

    @Override
    public JsonGenerator writePropertyId(long id) throws JacksonException {
        delegate.writePropertyId(id);
        return this;
    }

    @Override
    public JsonGenerator writeArray(int[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
        return this;
    }

    @Override
    public JsonGenerator writeArray(long[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
        return this;
    }

    @Override
    public JsonGenerator writeArray(double[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
        return this;
    }

    @Override
    public JsonGenerator writeArray(String[] array, int offset, int length) throws JacksonException {
        delegate.writeArray(array, offset, length);
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, text/String values
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeString(String text) throws JacksonException {
        delegate.writeString(text);
        return this;
    }

    @Override
    public JsonGenerator writeString(Reader reader, int len) throws JacksonException {
        delegate.writeString(reader, len);
        return this;
    }

    @Override
    public JsonGenerator writeString(char[] text, int offset, int len) throws JacksonException {
        delegate.writeString(text, offset, len);
        return this;
    }

    @Override
    public JsonGenerator writeString(SerializableString text) throws JacksonException {
        delegate.writeString(text);
        return this;
    }

    @Override
    public JsonGenerator writeRawUTF8String(byte[] text, int offset, int length) throws JacksonException {
        delegate.writeRawUTF8String(text, offset, length);
        return this;
    }

    @Override
    public JsonGenerator writeUTF8String(byte[] text, int offset, int length) throws JacksonException {
        delegate.writeUTF8String(text, offset, length);
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeRaw(String text) throws JacksonException {
        delegate.writeRaw(text);
        return this;
    }

    @Override
    public JsonGenerator writeRaw(String text, int offset, int len) throws JacksonException {
        delegate.writeRaw(text, offset, len);
        return this;
    }

    @Override
    public JsonGenerator writeRaw(SerializableString raw) throws JacksonException {
        delegate.writeRaw(raw);
        return this;
    }

    @Override
    public JsonGenerator writeRaw(char[] text, int offset, int len) throws JacksonException {
        delegate.writeRaw(text, offset, len);
        return this;
    }

    @Override
    public JsonGenerator writeRaw(char c) throws JacksonException {
        delegate.writeRaw(c);
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(String text) throws JacksonException {
        delegate.writeRawValue(text);
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(String text, int offset, int len) throws JacksonException {
        delegate.writeRawValue(text, offset, len);
        return this;
    }

    @Override
    public JsonGenerator writeRawValue(char[] text, int offset, int len) throws JacksonException {
        delegate.writeRawValue(text, offset, len);
        return this;
    }

    @Override
    public JsonGenerator writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws JacksonException {
        delegate.writeBinary(b64variant, data, offset, len);
        return this;
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws JacksonException {
        return delegate.writeBinary(b64variant, data, dataLength);
    }

    /*
    /**********************************************************************
    /* Public API, write methods, other value types
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeNumber(short v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(int v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(long v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(BigInteger v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(double v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(float v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(BigDecimal v) throws JacksonException {
        delegate.writeNumber(v);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(String encodedValue) throws JacksonException {
        delegate.writeNumber(encodedValue);
        return this;
    }

    @Override
    public JsonGenerator writeNumber(char[] encodedValueBuffer, int offset, int length) throws JacksonException {
        delegate.writeNumber(encodedValueBuffer, offset, length);
        return this;
    }

    @Override
    public JsonGenerator writeBoolean(boolean state) throws JacksonException {
        delegate.writeBoolean(state);
        return this;
    }

    @Override
    public JsonGenerator writeNull() throws JacksonException {
        delegate.writeNull();
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, convenience property-write methods
    /**********************************************************************
     */

    // 04-Oct-2019, tatu: Reminder: these should NOT be delegated, unless matching
    //    methods in `FilteringGeneratorDelegate` are re-defined to "split" calls again

//    public JsonGenerator writeBinaryProperty(String propName, byte[] data) throws JacksonException {
//    public JsonGenerator writeBooleanProperty(String propName, boolean value) throws JacksonException {
//    public JsonGenerator writeNullProperty(String propName) throws JacksonException {
//    public JsonGenerator writeStringProperty(String propName, String value) throws JacksonException {
//    public JsonGenerator writeNumberProperty(String propName, short value) throws JacksonException {

//    public JsonGenerator writeArrayPropertyStart(String propName) throws JacksonException {
//    public JsonGenerator writeObjectPropertyStart(String propName) throws JacksonException {
//    public JsonGenerator writePOJOProperty(String propName, Object pojo) throws JacksonException {

    // Sole exception being this method as it is not a "combo" method

    @Override
    public JsonGenerator writeOmittedProperty(String propName) throws JacksonException {
        delegate.writeOmittedProperty(propName);
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************************
     */

    @Override
    public JsonGenerator writeObjectId(Object id) throws JacksonException {
        delegate.writeObjectId(id);
        return this;
    }

    @Override
    public JsonGenerator writeObjectRef(Object id) throws JacksonException {
        delegate.writeObjectRef(id);
        return this;
    }

    @Override
    public JsonGenerator writeTypeId(Object id) throws JacksonException {
        delegate.writeTypeId(id);
        return this;
    }

    @Override
    public JsonGenerator writeEmbeddedObject(Object object) throws JacksonException {
        delegate.writeEmbeddedObject(object);
        return this;
    }

    /*
    /**********************************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************************
     */

    @Override
    public JsonGenerator writePOJO(Object pojo) throws JacksonException {
        if (delegateCopyMethods) {
            delegate.writePOJO(pojo);
            return this;
        }
        if (pojo == null) {
            writeNull();
        } else {
            objectWriteContext().writeValue(this, pojo);
        }
        return this;
    }

    @Override
    public JsonGenerator writeTree(TreeNode tree) throws JacksonException {
        if (delegateCopyMethods) {
            delegate.writeTree(tree);
            return this;
        }
        // As with 'writeObject()', we are not check if write would work
        if (tree == null) {
            writeNull();
        } else {
            objectWriteContext().writeTree(this, tree);
        }
        return this;
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
