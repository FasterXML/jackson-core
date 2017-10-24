package com.fasterxml.jackson.core.base;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

/**
 * This base class implements part of API that a JSON generator exposes
 * to applications, adds shared internal methods that sub-classes
 * can use and adds some abstract methods sub-classes must implement.
 */
public abstract class GeneratorBase extends JsonGenerator
{
    public final static int SURR1_FIRST = 0xD800;
    public final static int SURR1_LAST = 0xDBFF;
    public final static int SURR2_FIRST = 0xDC00;
    public final static int SURR2_LAST = 0xDFFF;

    /**
     * Set of feature masks related to features that need updates of other
     * local configuration or state.
     */
    protected final static int DERIVED_FEATURES_MASK =
            Feature.WRITE_NUMBERS_AS_STRINGS.getMask()
            | Feature.ESCAPE_NON_ASCII.getMask()
            | Feature.STRICT_DUPLICATE_DETECTION.getMask()
            ;

    // // // Constants for validation messages (since 2.6)

    protected final static String WRITE_BINARY = "write a binary value";
    protected final static String WRITE_BOOLEAN = "write a boolean value";
    protected final static String WRITE_NULL = "write a null";
    protected final static String WRITE_NUMBER = "write a number";
    protected final static String WRITE_RAW = "write a raw (unencoded) value";
    protected final static String WRITE_STRING = "write a string";

    /**
     * This value is the limit of scale allowed for serializing {@link BigDecimal}
     * in "plain" (non-engineering) notation; intent is to prevent asymmetric
     * attack whereupon simple eng-notation with big scale is used to generate
     * huge "plain" serialization. See [core#315] for details.
     */
    protected final static int MAX_BIG_DECIMAL_SCALE = 9999;
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Context object used both to pass some initial settings and to allow
     * triggering of Object serialization through generator.
     *
     * @since 3.0
     */
    protected final ObjectWriteContext _objectWriteContext;

    /**
     * Bit flag composed of bits that indicate which
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature}s
     * are enabled.
     */
    protected int _features;

    /**
     * Flag set to indicate that implicit conversion from number
     * to JSON String is needed (as per
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#WRITE_NUMBERS_AS_STRINGS}).
     */
    protected boolean _cfgNumbersAsStrings;

    /*
    /**********************************************************
    /* State
    /**********************************************************
     */

    /**
     * Object that keeps track of the current contextual state
     * of the generator.
     */
    protected JsonWriteContext _outputContext;

    /**
     * Flag that indicates whether generator is closed or not. Gets
     * set when it is closed by an explicit call
     * ({@link #close}).
     */
    protected boolean _closed;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected GeneratorBase(ObjectWriteContext writeCtxt, int features) {
        super();
        _objectWriteContext = writeCtxt;
        _features = features;
        DupDetector dups = Feature.STRICT_DUPLICATE_DETECTION.enabledIn(features)
                ? DupDetector.rootDetector(this) : null;
        _outputContext = JsonWriteContext.createRootContext(dups);
        _cfgNumbersAsStrings = Feature.WRITE_NUMBERS_AS_STRINGS.enabledIn(features);
    }

    protected GeneratorBase(ObjectWriteContext writeCtxt, int features, JsonWriteContext ctxt) {
        super();
        _objectWriteContext = writeCtxt;
        _features = features;
        _outputContext = ctxt;
        _cfgNumbersAsStrings = Feature.WRITE_NUMBERS_AS_STRINGS.enabledIn(features);
    }

    /**
     * Implemented with standard version number detection algorithm, typically using
     * a simple generated class, with information extracted from Maven project file
     * during build.
     */
    @Override public Version version() { return PackageVersion.VERSION; }

    @Override
    public Object getCurrentValue() {
        return _outputContext.getCurrentValue();
    }

    @Override
    public void setCurrentValue(Object v) {
        _outputContext.setCurrentValue(v);
    }

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */


    @Override public final boolean isEnabled(Feature f) { return (_features & f.getMask()) != 0; }
    @Override public int getGeneratorFeatures() { return _features; }

    //public JsonGenerator configure(Feature f, boolean state) { }

    @Override
    public JsonGenerator enable(Feature f) {
        final int mask = f.getMask();
        _features |= mask;
        if ((mask & DERIVED_FEATURES_MASK) != 0) {
            // why not switch? Requires addition of a generated class, alas
            if (f == Feature.WRITE_NUMBERS_AS_STRINGS) {
                _cfgNumbersAsStrings = true;
            } else if (f == Feature.ESCAPE_NON_ASCII) {
                setHighestNonEscapedChar(127);
            } else if (f == Feature.STRICT_DUPLICATE_DETECTION) {
                if (_outputContext.getDupDetector() == null) { // but only if disabled currently
                    _outputContext = _outputContext.withDupDetector(DupDetector.rootDetector(this));
                }
            }
        }
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        final int mask = f.getMask();
        _features &= ~mask;
        if ((mask & DERIVED_FEATURES_MASK) != 0) {
            if (f == Feature.WRITE_NUMBERS_AS_STRINGS) {
                _cfgNumbersAsStrings = false;
            } else if (f == Feature.ESCAPE_NON_ASCII) {
                setHighestNonEscapedChar(0);
            } else if (f == Feature.STRICT_DUPLICATE_DETECTION) {
                _outputContext = _outputContext.withDupDetector(null);
            }
        }
        return this;
    }

    /**
     * Helper method called to verify changes to standard features.
     *
     * @param newFeatureFlags Bitflag of standard features after they were changed
     * @param changedFeatures Bitflag of standard features for which setting
     *    did change
     *
     * @since 2.7
     */
    protected void _checkStdFeatureChanges(int newFeatureFlags, int changedFeatures)
    {
        if ((changedFeatures & DERIVED_FEATURES_MASK) == 0) {
            return;
        }
        _cfgNumbersAsStrings = Feature.WRITE_NUMBERS_AS_STRINGS.enabledIn(newFeatureFlags);
        if (Feature.ESCAPE_NON_ASCII.enabledIn(changedFeatures)) {
            if (Feature.ESCAPE_NON_ASCII.enabledIn(newFeatureFlags)) {
                setHighestNonEscapedChar(127);
            } else {
                setHighestNonEscapedChar(0);
            }
        }
        if (Feature.STRICT_DUPLICATE_DETECTION.enabledIn(changedFeatures)) {
            if (Feature.STRICT_DUPLICATE_DETECTION.enabledIn(newFeatureFlags)) { // enabling
                if (_outputContext.getDupDetector() == null) { // but only if disabled currently
                    _outputContext = _outputContext.withDupDetector(DupDetector.rootDetector(this));
                }
            } else { // disabling
                _outputContext = _outputContext.withDupDetector(null);
            }
        }
    }

    @Override public JsonGenerator useDefaultPrettyPrinter() {
        // Should not override a pretty printer if one already assigned.
        if (getPrettyPrinter() != null) {
            return this;
        }
        return setPrettyPrinter(_constructDefaultPrettyPrinter());
    }

    /*
    /**********************************************************
    /* Public API, accessors
    /**********************************************************
     */

    @Override public TokenStreamContext getOutputContext() { return _outputContext; }
    @Override public ObjectWriteContext getObjectWriteContext() { return _objectWriteContext; }

    /*
    /**********************************************************
    /* Public API, write methods, structural
    /**********************************************************
     */

    //public void writeStartArray() throws IOException
    //public void writeEndArray() throws IOException
    //public void writeStartObject() throws IOException
    //public void writeEndObject() throws IOException

    @Override
    public void writeStartArray(Object forValue, int size) throws IOException {
        writeStartArray(size);
        if ((_outputContext != null) && (forValue != null)) {
            _outputContext.setCurrentValue(forValue);
        }
    }

    @Override
    public void writeStartObject(Object forValue) throws IOException
    {
        writeStartObject();
        if ((_outputContext != null) && (forValue != null)) {
            _outputContext.setCurrentValue(forValue);
        }
    }

/*
    /**********************************************************
    /* Public API, write methods, textual
    /**********************************************************
     */

    @Override public void writeFieldName(SerializableString name) throws IOException {
        writeFieldName(name.getValue());
    }

    //public abstract void writeString(String text) throws IOException;

    //public abstract void writeString(char[] text, int offset, int len) throws IOException;

    //public abstract void writeString(Reader reader, int len) throws IOException;

    //public abstract void writeRaw(String text) throws IOException,;

    //public abstract void writeRaw(char[] text, int offset, int len) throws IOException;

    @Override
    public void writeString(SerializableString text) throws IOException {
        writeString(text.getValue());
    }

    @Override public void writeRawValue(String text) throws IOException {
        _verifyValueWrite("write raw value");
        writeRaw(text);
    }

    @Override public void writeRawValue(String text, int offset, int len) throws IOException {
        _verifyValueWrite("write raw value");
        writeRaw(text, offset, len);
    }

    @Override public void writeRawValue(char[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write raw value");
        writeRaw(text, offset, len);
    }

    @Override public void writeRawValue(SerializableString text) throws IOException {
        _verifyValueWrite("write raw value");
        writeRaw(text);
    }

    @Override
    public int writeBinary(Base64Variant b64variant, InputStream data, int dataLength) throws IOException {
        // Let's implement this as "unsupported" to make it easier to add new parser impls
        _reportUnsupportedOperation();
        return 0;
    }

    /*
    /**********************************************************
    /* Public API, write methods, primitive
    /**********************************************************
     */

    // Not implemented at this level, added as placeholders

     /*
    public abstract void writeNumber(int i)
    public abstract void writeNumber(long l)
    public abstract void writeNumber(double d)
    public abstract void writeNumber(float f)
    public abstract void writeNumber(BigDecimal dec)
    public abstract void writeBoolean(boolean state)
    public abstract void writeNull()
    */

    /*
    /**********************************************************
    /* Public API, write methods, POJOs, trees
    /**********************************************************
     */

    @Override
    public void writeObject(Object value) throws IOException {
        if (value == null) {
            // important: call method that does check value write:
            writeNull();
        } else {
            // We are NOT to call _verifyValueWrite here, because that will be
            // done when actual serialization of POJO occurs. If we did call it,
            // state would advance causing exception later on
            _objectWriteContext.writeValue(this, value);
        }
    }

    @Override
    public void writeTree(TreeNode rootNode) throws IOException {
        // As with 'writeObject()', we are not to check if write would work
        if (rootNode == null) {
            writeNull();
        } else {
            _objectWriteContext.writeTree(this, rootNode);
        }
    }

    /*
    /**********************************************************
    /* Public API, low-level output handling
    /**********************************************************
     */

    @Override public abstract void flush() throws IOException;
    @Override public void close() throws IOException { _closed = true; }
    @Override public boolean isClosed() { return _closed; }

    /*
    /**********************************************************
    /* Package methods for this, sub-classes
    /**********************************************************
     */

    /**
     * Method called to release any buffers generator may be holding,
     * once generator is being closed.
     */
    protected abstract void _releaseBuffers();

    /**
     * Method called before trying to write a value (scalar or structured),
     * to verify that this is legal in current output state, as well as to
     * output separators if and as necessary.
     * 
     * @param typeMsg Additional message used for generating exception message
     *   if value output is NOT legal in current generator output state.
     */
    protected abstract void _verifyValueWrite(String typeMsg) throws IOException;

    /**
     * Overridable factory method called to instantiate an appropriate {@link PrettyPrinter}
     * for case of "just use the default one", when {@link #useDefaultPrettyPrinter()} is called.
     *
     * @since 2.6
     */
    protected PrettyPrinter _constructDefaultPrettyPrinter() {
        return new DefaultPrettyPrinter();
    }

    /**
     * Helper method used to serialize a {@link java.math.BigDecimal} as a String,
     * for serialization, taking into account configuration settings
     *
     * @since 2.7.7
     */
    protected String _asString(BigDecimal value) throws IOException {
        if (Feature.WRITE_BIGDECIMAL_AS_PLAIN.enabledIn(_features)) {
            // 24-Aug-2016, tatu: [core#315] prevent possible DoS vector
            int scale = value.scale();
            if ((scale < -MAX_BIG_DECIMAL_SCALE) || (scale > MAX_BIG_DECIMAL_SCALE)) {
                _reportError(String.format(
"Attempt to write plain `java.math.BigDecimal` (see JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN) with illegal scale (%d): needs to be between [-%d, %d]",
scale, MAX_BIG_DECIMAL_SCALE, MAX_BIG_DECIMAL_SCALE));
            }
            return value.toPlainString();
        }
        return value.toString();
    }

    /*
    /**********************************************************
    /* UTF-8 related helper method(s)
    /**********************************************************
     */

    protected final int _decodeSurrogate(int surr1, int surr2) throws IOException
    {
        // First is known to be valid, but how about the other?
        if (surr2 < SURR2_FIRST || surr2 > SURR2_LAST) {
            String msg = "Incomplete surrogate pair: first char 0x"+Integer.toHexString(surr1)+", second 0x"+Integer.toHexString(surr2);
            _reportError(msg);
        }
        int c = 0x10000 + ((surr1 - SURR1_FIRST) << 10) + (surr2 - SURR2_FIRST);
        return c;
    }
}
