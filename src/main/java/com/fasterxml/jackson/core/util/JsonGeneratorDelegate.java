package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.CharacterEscapes;

public class JsonGeneratorDelegate extends JsonGenerator
{
    /**
     * Delegate object that method calls are delegated to.
     */
    protected JsonGenerator delegate;

    /*
    /**********************************************************
    /* Construction, initialization
    /**********************************************************
     */
    
    public JsonGeneratorDelegate(JsonGenerator d) {
        delegate = d;
    }

    @Override
    public ObjectCodec getCodec() {
        return delegate.getCodec();
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec oc) {
        delegate.setCodec(oc);
        return this;
    }
    
    @Override
    public void setSchema(FormatSchema schema) {
        delegate.setSchema(schema);
    }

    @Override
    public FormatSchema getSchema() {
        return delegate.getSchema();
    }

    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Object getOutputTarget() {
        return delegate.getOutputTarget();
    }

    /*
    /**********************************************************
    /* Public API, capability introspection (since 2.3, mostly)
    /**********************************************************
     */

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return delegate.canUseSchema(schema);
    }

    @Override
    public boolean canWriteTypeId() {
        return delegate.canWriteTypeId();
    }

    @Override
    public boolean canWriteObjectId() {
        return delegate.canWriteObjectId();
    }

    @Override
    public boolean canOmitFields() {
        return delegate.canOmitFields();
    }
    
    /*
    /**********************************************************
    /* Public API, configuration
    /**********************************************************
     */

    @Override
    public JsonGenerator enable(Feature f) {
        delegate.enable(f);
        return this;
    }
    
    @Override
    public JsonGenerator disable(Feature f) {
        delegate.disable(f);
        return this;
    }

    @Override
    public boolean isEnabled(Feature f) {
        return delegate.isEnabled(f);
    }

    // final, can't override (and no need to)
    //public final JsonGenerator configure(Feature f, boolean state)

    @Override
    public int getFeatureMask() {
        return delegate.getFeatureMask();
    }

    @Override
    public JsonGenerator setFeatureMask(int mask) {
        delegate.setFeatureMask(mask);
        return this;
    }

    /*
    /**********************************************************
    /* Configuring generator
    /**********************************************************
      */

    @Override
    public JsonGenerator setPrettyPrinter(PrettyPrinter pp) {
        delegate.setPrettyPrinter(pp);
        return this;
    }

    @Override
    public PrettyPrinter getPrettyPrinter() {
        return delegate.getPrettyPrinter();
    }
    
    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        delegate.useDefaultPrettyPrinter();
        return this;
    }

    @Override
    public JsonGenerator setHighestNonEscapedChar(int charCode) {
        delegate.setHighestNonEscapedChar(charCode);
        return this;
    }

    @Override
    public int getHighestEscapedChar() {
        return delegate.getHighestEscapedChar();
    }

    @Override
    public CharacterEscapes getCharacterEscapes() {
        return delegate.getCharacterEscapes();
    }

    @Override
    public JsonGenerator setCharacterEscapes(CharacterEscapes esc) {
        delegate.setCharacterEscapes(esc);
        return this;
    }

    @Override
    public JsonGenerator setRootValueSeparator(SerializableString sep) {
        delegate.setRootValueSeparator(sep);
        return this;
    }

    /*
    /**********************************************************
    /* Public API, write methods, structural
    /**********************************************************
     */

    @Override
    public void writeStartArray() throws IOException, JsonGenerationException {
         delegate.writeStartArray();
    }


    @Override
    public void writeEndArray() throws IOException, JsonGenerationException {
        delegate.writeEndArray();
    }

    @Override
    public void writeStartObject() throws IOException, JsonGenerationException {
        delegate.writeStartObject();
    }
    
    @Override
    public void writeEndObject() throws IOException, JsonGenerationException {
        delegate.writeEndObject();
    }

    @Override
    public void writeFieldName(String name)
        throws IOException, JsonGenerationException
    {
        delegate.writeFieldName(name);
    }

    @Override
    public void writeFieldName(SerializableString name)
        throws IOException, JsonGenerationException
    {
        delegate.writeFieldName(name);
    }
    
    /*
    /**********************************************************
    /* Public API, write methods, text/String values
    /**********************************************************
     */

    @Override
    public void writeString(String text) throws IOException,JsonGenerationException {
        delegate.writeString(text);
    }

    @Override
    public void writeString(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeString(text, offset, len);
    }

    @Override
    public void writeString(SerializableString text) throws IOException, JsonGenerationException {
        delegate.writeString(text);
    }

    @Override
    public void writeRawUTF8String(byte[] text, int offset, int length)
        throws IOException, JsonGenerationException
    {
        delegate.writeRawUTF8String(text, offset, length);
    }

    @Override
    public void writeUTF8String(byte[] text, int offset, int length)
        throws IOException, JsonGenerationException
    {
        delegate.writeUTF8String(text, offset, length);
    }

    /*
    /**********************************************************
    /* Public API, write methods, binary/raw content
    /**********************************************************
     */

    @Override
    public void writeRaw(String text) throws IOException, JsonGenerationException {
        delegate.writeRaw(text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(SerializableString raw)
        throws IOException, JsonGenerationException {
        delegate.writeRaw(raw);
    }
    
    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException, JsonGenerationException {
        delegate.writeRaw(text, offset, len);
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        delegate.writeRaw(c);
    }

    @Override
    public void writeRawValue(String text) throws IOException, JsonGenerationException {
        delegate.writeRawValue(text);
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException, JsonGenerationException {
         delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException, JsonGenerationException {
         delegate.writeRawValue(text, offset, len);
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len)
        throws IOException, JsonGenerationException
    {
        delegate.writeBinary(b64variant, data, offset, len);
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength)
        throws IOException, JsonGenerationException {
        return delegate.writeBinary(b64variant, data, dataLength);
    }
    
    /*
    /**********************************************************
    /* Public API, write methods, other value types
    /**********************************************************
     */

    @Override
    public void writeNumber(short v) throws IOException, JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(int v) throws IOException, JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(long v) throws IOException, JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigInteger v) throws IOException,
            JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(double v) throws IOException,
            JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(float v) throws IOException,
            JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException,
            JsonGenerationException {
        delegate.writeNumber(v);
    }

    @Override
    public void writeNumber(String encodedValue) throws IOException, JsonGenerationException, UnsupportedOperationException {
        delegate.writeNumber(encodedValue);
    }

    @Override
    public void writeBoolean(boolean state) throws IOException, JsonGenerationException {
        delegate.writeBoolean(state);
    }
    
    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        delegate.writeNull();
    }

    /*
    /**********************************************************
    /* Overridden field methods
    /**********************************************************
     */

    @Override
    public void writeOmittedField(String fieldName)
        throws IOException, JsonGenerationException
    {
        delegate.writeOmittedField(fieldName);
    }
    
    /*
    /**********************************************************
    /* Public API, write methods, Native Ids
    /**********************************************************
     */
    
    @Override
    public void writeObjectId(Object id)
        throws IOException, JsonGenerationException {
        delegate.writeObjectId(id);
    }

    @Override
    public void writeObjectRef(Object id)
            throws IOException, JsonGenerationException {
        delegate.writeObjectRef(id);
    }
    
    @Override
    public void writeTypeId(Object id)
        throws IOException, JsonGenerationException {
        delegate.writeTypeId(id);
    }
    
    /*
    /**********************************************************
    /* Public API, write methods, serializing Java objects
    /**********************************************************
     */
    
    @Override
    public void writeObject(Object pojo) throws IOException,JsonProcessingException {
        delegate.writeObject(pojo);
    }
    
    @Override
    public void writeTree(TreeNode rootNode) throws IOException, JsonProcessingException {
        delegate.writeTree(rootNode);
    }

    /*
    /**********************************************************
    /* Public API, convenience field write methods
    /**********************************************************
     */

    // // These are fine, just delegate to other methods...

    /*
    /**********************************************************
    /* Public API, copy-through methods
    /**********************************************************
     */

    @Override
    public void copyCurrentEvent(JsonParser jp) throws IOException, JsonProcessingException {
        delegate.copyCurrentEvent(jp);
    }

    @Override
    public void copyCurrentStructure(JsonParser jp) throws IOException, JsonProcessingException {
        delegate.copyCurrentStructure(jp);
    }

    /*
    /**********************************************************
    /* Public API, context access
    /**********************************************************
     */

    @Override
    public JsonStreamContext getOutputContext() {
        return delegate.getOutputContext();
    }

    /*
    /**********************************************************
    /* Public API, buffer handling
    /**********************************************************
     */
    
    @Override
    public void flush() throws IOException {
        delegate.flush();
    }
    
    @Override
    public void close() throws IOException {
        delegate.close();
    }

    /*
    /**********************************************************
    /* Closeable implementation
    /**********************************************************
     */
    
    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }
}
