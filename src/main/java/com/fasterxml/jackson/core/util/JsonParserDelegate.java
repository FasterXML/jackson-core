package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Helper class that implements
 * <a href="http://en.wikipedia.org/wiki/Delegation_pattern">delegation pattern</a> for {@link JsonParser},
 * to allow for simple overridability of basic parsing functionality.
 * The idea is that any functionality to be modified can be simply
 * overridden; and anything else will be delegated by default.
 */
public class JsonParserDelegate extends JsonParser
{
    /**
     * Delegate object that method calls are delegated to.
     */
    protected JsonParser delegate;

    public JsonParserDelegate(JsonParser d) {
        delegate = d;
    }

    /*
    /**********************************************************
    /* Public API, configuration
    /**********************************************************
     */

    @Override
    public void setCodec(ObjectCodec c) {
        delegate.setCodec(c);
    }

    @Override
    public ObjectCodec getCodec() {
        return delegate.getCodec();
    }

    @Override
    public JsonParser enable(Feature f) {
        delegate.enable(f);
        return this;
    }

    @Override
    public JsonParser disable(Feature f) {
        delegate.disable(f);
        return this;
    }
 
    @Override
    public boolean isEnabled(Feature f) {
        return delegate.isEnabled(f);
    }

    public JsonParser configure(Feature f, boolean state) {
        return delegate.configure(f, state);
    }

    @Override
    public int getFeatureMask() {
        return delegate.getFeatureMask();
    }

    @Override
    public JsonParser setFeatureMask(int mask) {
        delegate.setFeatureMask(mask);
        return this;
    }

    @Override
    public FormatSchema getSchema() {
        return delegate.getSchema();
    }
    
    @Override
    public void setSchema(FormatSchema schema) {
        delegate.setSchema(schema);
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return delegate.canUseSchema(schema);
    }
    
    @Override
    public Version version() {
        return delegate.version();
    }

    @Override
    public Object getInputSource() {
        return delegate.getInputSource();
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    @Override
    public boolean requiresCustomCodec() {
        return delegate.requiresCustomCodec();
    }

    /*
    /**********************************************************
    /* Closeable impl
    /**********************************************************
     */

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    /*
    /**********************************************************
    /* Public API, token accessors
    /**********************************************************
     */

    @Override
    public JsonToken getCurrentToken() {
        return delegate.getCurrentToken();
    }

    @Override
    public int getCurrentTokenId() {
        return delegate.getCurrentTokenId();
    }
    
    @Override
    public boolean hasCurrentToken() {
        return delegate.hasCurrentToken();
    }

    @Override
    public String getCurrentName() throws IOException, JsonParseException {
        return delegate.getCurrentName();
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return delegate.getCurrentLocation();
    }

    @Override
    public JsonStreamContext getParsingContext() {
        return delegate.getParsingContext();
    }
    
    @Override public boolean isExpectedStartArrayToken() { return delegate.isExpectedStartArrayToken(); }

    /*
    /**********************************************************
    /* Public API, token state overrides
    /**********************************************************
     */
    
    @Override
    public void clearCurrentToken() {
        delegate.clearCurrentToken();        
    }

    @Override
    public JsonToken getLastClearedToken() {
        return delegate.getLastClearedToken();
    }
    
    @Override
    public void overrideCurrentName(String name) {
        delegate.overrideCurrentName(name);
    }

    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
     */

    @Override
    public String getText() throws IOException, JsonParseException {
        return delegate.getText();
    }

    @Override
    public boolean hasTextCharacters() {
        return delegate.hasTextCharacters();
    }
    
    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        return delegate.getTextCharacters();
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        return delegate.getTextLength();
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException {
        return delegate.getTextOffset();
    }

    public String nextTextValue() throws IOException, JsonParseException {
        return delegate.nextTextValue();
    }

    /*
    /**********************************************************
    /* Public API, access to token information, numeric
    /**********************************************************
     */
    
    @Override
    public BigInteger getBigIntegerValue() throws IOException, JsonParseException {
        return delegate.getBigIntegerValue();
    }

    @Override
    public boolean getBooleanValue() throws IOException, JsonParseException {
        return delegate.getBooleanValue();
    }
    
    @Override
    public byte getByteValue() throws IOException, JsonParseException {
        return delegate.getByteValue();
    }

    @Override
    public short getShortValue() throws IOException, JsonParseException {
        return delegate.getShortValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException, JsonParseException {
        return delegate.getDecimalValue();
    }

    @Override
    public double getDoubleValue() throws IOException, JsonParseException {
        return delegate.getDoubleValue();
    }

    @Override
    public float getFloatValue() throws IOException, JsonParseException {
        return delegate.getFloatValue();
    }

    @Override
    public int getIntValue() throws IOException, JsonParseException {
        return delegate.getIntValue();
    }

    @Override
    public long getLongValue() throws IOException, JsonParseException {
        return delegate.getLongValue();
    }

    @Override
    public NumberType getNumberType() throws IOException, JsonParseException {
        return delegate.getNumberType();
    }

    @Override
    public Number getNumberValue() throws IOException, JsonParseException {
        return delegate.getNumberValue();
    }

    public Boolean nextBooleanValue() throws IOException, JsonParseException {
        return delegate.nextBooleanValue();
    }

    public int nextIntValue(int defaultValue) throws IOException, JsonParseException {
        return delegate.nextIntValue(defaultValue);
    }

    public long nextLongValue(long defaultValue) throws IOException, JsonParseException {
        return delegate.nextLongValue(defaultValue);
    }

    /*
    /**********************************************************
    /* Public API, access to token information, coercion/conversion
    /**********************************************************
     */
    
    @Override
    public int getValueAsInt() throws IOException, JsonParseException {
        return delegate.getValueAsInt();
    }
    
    @Override
    public int getValueAsInt(int defaultValue) throws IOException, JsonParseException {
        return delegate.getValueAsInt(defaultValue);
    }

    @Override
    public long getValueAsLong() throws IOException, JsonParseException {
        return delegate.getValueAsLong();
    }
    
    @Override
    public long getValueAsLong(long defaultValue) throws IOException, JsonParseException {
        return delegate.getValueAsLong(defaultValue);
    }
    
    @Override
    public double getValueAsDouble() throws IOException, JsonParseException {
        return delegate.getValueAsDouble();
    }
    
    @Override
    public double getValueAsDouble(double defaultValue) throws IOException, JsonParseException {
        return delegate.getValueAsDouble(defaultValue);
    }

    @Override
    public boolean getValueAsBoolean() throws IOException, JsonParseException {
        return delegate.getValueAsBoolean();
    }

    @Override
    public boolean getValueAsBoolean(boolean defaultValue) throws IOException, JsonParseException {
        return delegate.getValueAsBoolean(defaultValue);
    }

    @Override
    public String getValueAsString() throws IOException, JsonParseException {
        return delegate.getValueAsString();
    }
    
    @Override
    public String getValueAsString(String defaultValue) throws IOException, JsonParseException {
        return delegate.getValueAsString(defaultValue);
    }

    public <T> T readValueAs(Class<T> valueType) throws IOException, JsonProcessingException {
        return delegate.readValueAs(valueType);
    }

    public <T> T readValueAs(TypeReference<?> valueTypeRef) throws IOException, JsonProcessingException {
        return delegate.readValueAs(valueTypeRef);
    }

    public <T> Iterator<T> readValuesAs(Class<T> valueType) throws IOException, JsonProcessingException {
        return delegate.readValuesAs(valueType);
    }

    public <T> Iterator<T> readValuesAs(TypeReference<?> valueTypeRef) throws IOException, JsonProcessingException {
        return delegate.readValuesAs(valueTypeRef);
    }

    public <T extends TreeNode> T readValueAsTree() throws IOException, JsonProcessingException {
        return delegate.readValueAsTree();
    }

    /*
    /**********************************************************
    /* Public API, access to token values, other
    /**********************************************************
     */

    @Override
    public Object getEmbeddedObject() throws IOException, JsonParseException {
        return delegate.getEmbeddedObject();
    }
    
    @Override
    public byte[] getBinaryValue(Base64Variant b64variant) throws IOException, JsonParseException {
        return delegate.getBinaryValue(b64variant);
    }

    public byte[] getBinaryValue() throws IOException, JsonParseException {
        return delegate.getBinaryValue();
    }

    @Override
    public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException, JsonParseException {
        return delegate.readBinaryValue(b64variant, out);
    }

    public int readBinaryValue(OutputStream out) throws IOException, JsonParseException {
        return delegate.readBinaryValue(out);
    }

    @Override
    public JsonLocation getTokenLocation() {
        return delegate.getTokenLocation();
    }

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {
        return delegate.nextToken();
    }

    @Override
    public JsonToken nextValue() throws IOException, JsonParseException {
        return delegate.nextValue();
    }

    public boolean nextFieldName(SerializableString str) throws IOException, JsonParseException {
        return delegate.nextFieldName(str);
    }

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException {
        delegate.skipChildren();
        // NOTE: must NOT delegate this method to delegate, needs to be self-reference for chaining
        return this;
    }

    public int releaseBuffered(OutputStream out) throws IOException {
        return delegate.releaseBuffered(out);
    }

    public int releaseBuffered(Writer w) throws IOException {
        return delegate.releaseBuffered(w);
    }

    /*
    /**********************************************************
    /* Public API, Native Ids (type, object)
    /**********************************************************
     */

    @Override
    public boolean canReadObjectId() {
        return delegate.canReadObjectId();
    }

    @Override
    public boolean canReadTypeId() {
        return delegate.canReadTypeId();
    }

    @Override
    public Object getObjectId() throws IOException, JsonGenerationException {
        return delegate.getObjectId();
    }

    @Override
    public Object getTypeId() throws IOException, JsonGenerationException {
        return delegate.getTypeId();
    }
}
