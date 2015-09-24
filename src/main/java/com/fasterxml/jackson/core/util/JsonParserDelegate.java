package com.fasterxml.jackson.core.util;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.*;

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

    @Override
    public Object getCurrentValue() {
        return delegate.getCurrentValue();
    }

    @Override
    public void setCurrentValue(Object v) {
        delegate.setCurrentValue(v);
    }

    /*
    /**********************************************************
    /* Public API, configuration
    /**********************************************************
     */

    @Override public void setCodec(ObjectCodec c) { delegate.setCodec(c); }
    @Override public ObjectCodec getCodec() { return delegate.getCodec(); }

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
 
    @Override public boolean isEnabled(Feature f) { return delegate.isEnabled(f); }
    @Override public int getFeatureMask() { return delegate.getFeatureMask(); }

    @Override
    @Deprecated // since 2.7
    public JsonParser setFeatureMask(int mask) {
        delegate.setFeatureMask(mask);
        return this;
    }

    @Override
    public JsonParser overrideStdFeatures(int values, int mask) {
        delegate.overrideStdFeatures(values, mask);
        return this;
    }

    @Override
    public JsonParser overrideFormatFeatures(int values, int mask) {
        delegate.overrideFormatFeatures(values, mask);
        return this;
    }

    @Override public FormatSchema getSchema() { return delegate.getSchema(); }
    @Override public void setSchema(FormatSchema schema) { delegate.setSchema(schema); }
    @Override public boolean canUseSchema(FormatSchema schema) {  return delegate.canUseSchema(schema); }
    @Override public Version version() { return delegate.version(); }
    @Override public Object getInputSource() { return delegate.getInputSource(); }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    @Override public boolean requiresCustomCodec() { return delegate.requiresCustomCodec(); }

    /*
    /**********************************************************
    /* Closeable impl
    /**********************************************************
     */

    @Override public void close() throws IOException { delegate.close(); }
    @Override public boolean isClosed() { return delegate.isClosed(); }

    /*
    /**********************************************************
    /* Public API, token accessors
    /**********************************************************
     */
 
    @Override public JsonToken getCurrentToken() { return delegate.getCurrentToken(); }
    @Override public int getCurrentTokenId() { return delegate.getCurrentTokenId(); }
    @Override public boolean hasCurrentToken() { return delegate.hasCurrentToken(); }
    @Override public boolean hasTokenId(int id) { return delegate.hasTokenId(id); }
    @Override public boolean hasToken(JsonToken t) { return delegate.hasToken(t); }

    @Override public String getCurrentName() throws IOException { return delegate.getCurrentName(); }
    @Override public JsonLocation getCurrentLocation() { return delegate.getCurrentLocation(); }
    @Override public JsonStreamContext getParsingContext() { return delegate.getParsingContext(); }
    @Override public boolean isExpectedStartArrayToken() { return delegate.isExpectedStartArrayToken(); }
    @Override public boolean isExpectedStartObjectToken() { return delegate.isExpectedStartObjectToken(); }

    /*
    /**********************************************************
    /* Public API, token state overrides
    /**********************************************************
     */
    
    @Override public void clearCurrentToken() { delegate.clearCurrentToken(); }
    @Override public JsonToken getLastClearedToken() { return delegate.getLastClearedToken(); }
    @Override public void overrideCurrentName(String name) { delegate.overrideCurrentName(name); }

    /*
    /**********************************************************
    /* Public API, access to token information, text
    /**********************************************************
     */

    @Override public String getText() throws IOException { return delegate.getText();  }
    @Override public boolean hasTextCharacters() { return delegate.hasTextCharacters(); }
    @Override public char[] getTextCharacters() throws IOException { return delegate.getTextCharacters(); }
    @Override public int getTextLength() throws IOException { return delegate.getTextLength(); }
    @Override public int getTextOffset() throws IOException { return delegate.getTextOffset(); }

    /*
    /**********************************************************
    /* Public API, access to token information, numeric
    /**********************************************************
     */
    
    @Override
    public BigInteger getBigIntegerValue() throws IOException { return delegate.getBigIntegerValue(); }

    @Override
    public boolean getBooleanValue() throws IOException { return delegate.getBooleanValue(); }
    
    @Override
    public byte getByteValue() throws IOException { return delegate.getByteValue(); }

    @Override
    public short getShortValue() throws IOException { return delegate.getShortValue(); }

    @Override
    public BigDecimal getDecimalValue() throws IOException { return delegate.getDecimalValue(); }

    @Override
    public double getDoubleValue() throws IOException { return delegate.getDoubleValue(); }

    @Override
    public float getFloatValue() throws IOException { return delegate.getFloatValue(); }

    @Override
    public int getIntValue() throws IOException { return delegate.getIntValue(); }

    @Override
    public long getLongValue() throws IOException { return delegate.getLongValue(); }

    @Override
    public NumberType getNumberType() throws IOException { return delegate.getNumberType(); }

    @Override
    public Number getNumberValue() throws IOException { return delegate.getNumberValue(); }

    /*
    /**********************************************************
    /* Public API, access to token information, coercion/conversion
    /**********************************************************
     */
    
    @Override public int getValueAsInt() throws IOException { return delegate.getValueAsInt(); }
    @Override public int getValueAsInt(int defaultValue) throws IOException { return delegate.getValueAsInt(defaultValue); }
    @Override public long getValueAsLong() throws IOException { return delegate.getValueAsLong(); }
    @Override public long getValueAsLong(long defaultValue) throws IOException { return delegate.getValueAsLong(defaultValue); }
    @Override public double getValueAsDouble() throws IOException { return delegate.getValueAsDouble(); }
    @Override public double getValueAsDouble(double defaultValue) throws IOException { return delegate.getValueAsDouble(defaultValue); }
    @Override public boolean getValueAsBoolean() throws IOException { return delegate.getValueAsBoolean(); }
    @Override public boolean getValueAsBoolean(boolean defaultValue) throws IOException { return delegate.getValueAsBoolean(defaultValue); }
    @Override public String getValueAsString() throws IOException { return delegate.getValueAsString(); }
    @Override public String getValueAsString(String defaultValue) throws IOException { return delegate.getValueAsString(defaultValue); }
    
    /*
    /**********************************************************
    /* Public API, access to token values, other
    /**********************************************************
     */

    @Override public Object getEmbeddedObject() throws IOException { return delegate.getEmbeddedObject(); }
    @Override public byte[] getBinaryValue(Base64Variant b64variant) throws IOException { return delegate.getBinaryValue(b64variant); }
    @Override public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException { return delegate.readBinaryValue(b64variant, out); }
    @Override public JsonLocation getTokenLocation() { return delegate.getTokenLocation(); }
    @Override public JsonToken nextToken() throws IOException { return delegate.nextToken(); }
    @Override public JsonToken nextValue() throws IOException { return delegate.nextValue(); }
    
    @Override
    public JsonParser skipChildren() throws IOException {
        delegate.skipChildren();
        // NOTE: must NOT delegate this method to delegate, needs to be self-reference for chaining
        return this;
    }

    /*
    /**********************************************************
    /* Public API, Native Ids (type, object)
    /**********************************************************
     */

    @Override public boolean canReadObjectId() { return delegate.canReadObjectId(); }
    @Override public boolean canReadTypeId() { return delegate.canReadTypeId(); }
    @Override public Object getObjectId() throws IOException { return delegate.getObjectId(); }
    @Override public Object getTypeId() throws IOException { return delegate.getTypeId(); }
}
