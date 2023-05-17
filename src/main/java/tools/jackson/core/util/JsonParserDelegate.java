package tools.jackson.core.util;

import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.*;
import tools.jackson.core.async.NonBlockingInputFeeder;
import tools.jackson.core.exc.InputCoercionException;
import tools.jackson.core.sym.PropertyNameMatcher;
import tools.jackson.core.type.ResolvedType;
import tools.jackson.core.type.TypeReference;

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

    @Override public Version version() { return delegate.version(); }

    // // // Public API: basic context access

    @Override
    public TokenStreamContext streamReadContext() { return delegate.streamReadContext(); }

    @Override
    public ObjectReadContext objectReadContext() { return delegate.objectReadContext(); }

    // // // Public API, input source, location access

    @Override public JsonLocation currentTokenLocation() { return delegate.currentTokenLocation(); }
    @Override public JsonLocation currentLocation() { return delegate.currentLocation(); }
    @Override public Object streamReadInputSource() { return delegate.streamReadInputSource(); }

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
    /* Public API, configuration
    /**********************************************************************
     */

    /*
    @Override
    public JsonParser enable(StreamReadFeature f) {
        delegate.enable(f);
        return this;
    }

    @Override
    public JsonParser disable(StreamReadFeature f) {
        delegate.disable(f);
        return this;
    }
    */

    @Override public boolean isEnabled(StreamReadFeature f) { return delegate.isEnabled(f); }
    @Override public int streamReadFeatures() { return delegate.streamReadFeatures(); }

    @Override public FormatSchema getSchema() { return delegate.getSchema(); }

    /*
    /**********************************************************************
    /* Capability introspection
    /**********************************************************************
     */

    @Override public boolean canParseAsync() { return delegate.canParseAsync(); }

    @Override public NonBlockingInputFeeder nonBlockingInputFeeder() { return delegate.nonBlockingInputFeeder(); }

    @Override public JacksonFeatureSet<StreamReadCapability> streamReadCapabilities() { return delegate.streamReadCapabilities(); }

    @Override public StreamReadConstraints streamReadConstraints() { return delegate.streamReadConstraints(); }

    /*
    /**********************************************************************
    /* Closeable impl
    /**********************************************************************
     */

    @Override public void close() { delegate.close(); }
    @Override public boolean isClosed() { return delegate.isClosed(); }

    /*
    /**********************************************************************
    /* Public API, token accessors
    /**********************************************************************
     */

    @Override public JsonToken currentToken() { return delegate.currentToken(); }
    @Override public int currentTokenId() { return delegate.currentTokenId(); }
    @Override public String currentName() { return delegate.currentName(); }

    @Override public boolean hasCurrentToken() { return delegate.hasCurrentToken(); }
    @Override public boolean hasTokenId(int id) { return delegate.hasTokenId(id); }
    @Override public boolean hasToken(JsonToken t) { return delegate.hasToken(t); }

    @Override public boolean isExpectedStartArrayToken() { return delegate.isExpectedStartArrayToken(); }
    @Override public boolean isExpectedStartObjectToken() { return delegate.isExpectedStartObjectToken(); }
    @Override public boolean isExpectedNumberIntToken() { return delegate.isExpectedNumberIntToken(); }

    @Override public boolean isNaN() { return delegate.isNaN(); }

    /*
    /**********************************************************************
    /* Public API, token state overrides
    /**********************************************************************
     */

    @Override public void clearCurrentToken() { delegate.clearCurrentToken(); }
    @Override public JsonToken getLastClearedToken() { return delegate.getLastClearedToken(); }
    /*
    @Override public void overrideCurrentName(String name) {
        delegate.overrideCurrentName(name);
    }
    */

    /*
    /**********************************************************************
    /* Public API, iteration over token stream
    /**********************************************************************
     */

    @Override public JsonToken nextToken() throws JacksonException { return delegate.nextToken(); }
    @Override public JsonToken nextValue() throws JacksonException { return delegate.nextValue(); }
    @Override public void finishToken() throws JacksonException { delegate.finishToken(); }

    @Override
    public JsonParser skipChildren() throws JacksonException {
        delegate.skipChildren();
        // NOTE: must NOT delegate this method to delegate, needs to be self-reference for chaining
        return this;
    }

    // 12-Nov-2017, tatu: These DO work as long as `JsonParserSequence` further overrides
    //     handling

    @Override public String nextName() throws JacksonException { return delegate.nextName(); }
    @Override public boolean nextName(SerializableString str) throws JacksonException { return delegate.nextName(str); }
    @Override public int nextNameMatch(PropertyNameMatcher matcher) throws JacksonException { return delegate.nextNameMatch(matcher); }

    // NOTE: fine without overrides since it does NOT change state
    @Override public int currentNameMatch(PropertyNameMatcher matcher) { return delegate.currentNameMatch(matcher); }

    /*
    /**********************************************************************
    /* Public API, access to token information, text
    /**********************************************************************
     */

    @Override public String getText() throws JacksonException { return delegate.getText();  }
    @Override public boolean hasTextCharacters() { return delegate.hasTextCharacters(); }
    @Override public char[] getTextCharacters() throws JacksonException { return delegate.getTextCharacters(); }
    @Override public int getTextLength() throws JacksonException { return delegate.getTextLength(); }
    @Override public int getTextOffset() throws JacksonException { return delegate.getTextOffset(); }
    @Override public int getText(Writer writer) throws JacksonException { return delegate.getText(writer);  }

    /*
    /**********************************************************************
    /* Public API, access to token information, numeric
    /**********************************************************************
     */

    @Override
    public BigInteger getBigIntegerValue() { return delegate.getBigIntegerValue(); }

    @Override
    public boolean getBooleanValue() throws InputCoercionException { return delegate.getBooleanValue(); }

    @Override
    public byte getByteValue() throws InputCoercionException { return delegate.getByteValue(); }

    @Override
    public short getShortValue() throws InputCoercionException { return delegate.getShortValue(); }

    @Override
    public BigDecimal getDecimalValue() throws InputCoercionException { return delegate.getDecimalValue(); }

    @Override
    public double getDoubleValue() throws InputCoercionException { return delegate.getDoubleValue(); }

    @Override
    public float getFloatValue() throws InputCoercionException { return delegate.getFloatValue(); }

    @Override
    public int getIntValue() throws InputCoercionException { return delegate.getIntValue(); }

    @Override
    public long getLongValue() throws InputCoercionException { return delegate.getLongValue(); }

    @Override
    public NumberType getNumberType() { return delegate.getNumberType(); }

    @Override
    public Number getNumberValue() throws InputCoercionException { return delegate.getNumberValue(); }

    @Override
    public Number getNumberValueExact() throws InputCoercionException { return delegate.getNumberValueExact(); }

    @Override
    public Object getNumberValueDeferred() throws InputCoercionException { return delegate.getNumberValueDeferred(); }

    /*
    /**********************************************************************
    /* Public API, access to token information, coercion/conversion
    /**********************************************************************
     */

    @Override public int getValueAsInt() throws InputCoercionException { return delegate.getValueAsInt(); }
    @Override public int getValueAsInt(int defaultValue) throws InputCoercionException { return delegate.getValueAsInt(defaultValue); }
    @Override public long getValueAsLong() throws InputCoercionException { return delegate.getValueAsLong(); }
    @Override public long getValueAsLong(long defaultValue) throws InputCoercionException { return delegate.getValueAsLong(defaultValue); }
    @Override public double getValueAsDouble() throws InputCoercionException { return delegate.getValueAsDouble(); }
    @Override public double getValueAsDouble(double defaultValue) throws InputCoercionException { return delegate.getValueAsDouble(defaultValue); }
    @Override public boolean getValueAsBoolean() { return delegate.getValueAsBoolean(); }
    @Override public boolean getValueAsBoolean(boolean defaultValue) { return delegate.getValueAsBoolean(defaultValue); }
    @Override public String getValueAsString(){ return delegate.getValueAsString(); }
    @Override public String getValueAsString(String defaultValue) { return delegate.getValueAsString(defaultValue); }

    /*
    /**********************************************************************
    /* Public API, access to token values, other
    /**********************************************************************
     */

    @Override public Object getEmbeddedObject() { return delegate.getEmbeddedObject(); }
    @Override public byte[] getBinaryValue(Base64Variant b64variant) throws JacksonException { return delegate.getBinaryValue(b64variant); }
    @Override public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws JacksonException { return delegate.readBinaryValue(b64variant, out); }

    /*
    /**********************************************************************
    /* Public API, databind callbacks via `ObjectReadContext`
    /**********************************************************************
     */

    @Override
    public <T> T readValueAs(Class<T> valueType) throws JacksonException {
        return delegate.readValueAs(valueType);
    }

    @Override
    public <T> T readValueAs(TypeReference<T> valueTypeRef) throws JacksonException {
        return delegate.readValueAs(valueTypeRef);
    }

    @Override
    public <T> T readValueAs(ResolvedType type) throws JacksonException {
        return delegate.readValueAs(type);
    }

    @Override
    public <T extends TreeNode> T readValueAsTree() throws JacksonException {
        return delegate.readValueAsTree();
    }

    /*
    /**********************************************************************
    /* Public API, Native Ids (type, object)
    /**********************************************************************
     */

    @Override public boolean canReadObjectId() { return delegate.canReadObjectId(); }
    @Override public boolean canReadTypeId() { return delegate.canReadTypeId(); }
    @Override public Object getObjectId() { return delegate.getObjectId(); }
    @Override public Object getTypeId() { return delegate.getTypeId(); }

    /*
    /**********************************************************************
    /* Extended API
    /**********************************************************************
     */

    /**
     * Accessor for getting the immediate {@link JsonParser} this parser delegates calls to.
     *
     * @return Underlying parser calls are delegated to
     */
    public JsonParser delegate() { return delegate; }
}
