package com.fasterxml.jackson.core.base;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.json.DupDetector;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * This base class implements part of API that a JSON generator exposes
 * to applications, adds shared internal methods that sub-classes
 * can use and adds some abstract methods sub-classes must implement.
 */
public abstract class GeneratorBase extends JsonGenerator
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    protected ObjectCodec _objectCodec;

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
    protected JsonWriteContext _writeContext;

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

    protected GeneratorBase(int features, ObjectCodec codec) {
        super();
        _features = features;
        DupDetector dups = Feature.STRICT_DUPLICATE_DETECTION.enabledIn(features)
                ? DupDetector.rootDetector(this) : null;
        _writeContext = JsonWriteContext.createRootContext(dups);
        _objectCodec = codec;
        _cfgNumbersAsStrings = Feature.WRITE_NUMBERS_AS_STRINGS.enabledIn(features);
    }

    /**
     * Implemented with detection that tries to find "VERSION.txt" in same
     * package as the implementation class.
     */
    @Override public Version version() { return VersionUtil.versionFor(getClass()); }
    
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    @Override
    public JsonGenerator enable(Feature f) {
        _features |= f.getMask();
        if (f == Feature.WRITE_NUMBERS_AS_STRINGS) {
            _cfgNumbersAsStrings = true;
        } else if (f == Feature.ESCAPE_NON_ASCII) {
            setHighestNonEscapedChar(127);
        }
        return this;
    }

    @Override
    public JsonGenerator disable(Feature f) {
        _features &= ~f.getMask();
        if (f == Feature.WRITE_NUMBERS_AS_STRINGS) {
            _cfgNumbersAsStrings = false;
        } else if (f == Feature.ESCAPE_NON_ASCII) {
            setHighestNonEscapedChar(0);
        }
        return this;
    }

    //public JsonGenerator configure(Feature f, boolean state) { }

    @Override public final boolean isEnabled(Feature f) { return (_features & f.getMask()) != 0; }
    @Override public int getFeatureMask() { return _features; }

    @Override public JsonGenerator setFeatureMask(int mask) {
        _features = mask;
        return this;
    }
    
    @Override public JsonGenerator useDefaultPrettyPrinter() {
        /* 28-Sep-2012, tatu: As per [Issue#84], should not override a
         *  pretty printer if one already assigned.
         */
        if (getPrettyPrinter() != null) {
            return this;
        }
        return setPrettyPrinter(new DefaultPrettyPrinter());
    }
    
    @Override public JsonGenerator setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    @Override public final ObjectCodec getCodec() { return _objectCodec; }

    /*
    /**********************************************************
    /* Public API, accessors
    /**********************************************************
     */

    /**
     * Note: co-variant return type.
     */
    @Override public final JsonWriteContext getOutputContext() { return _writeContext; }

    /*
    /**********************************************************
    /* Public API, write methods, structural
    /**********************************************************
     */

    //public void writeStartArray() throws IOException
    //public void writeEndArray() throws IOException
    //public void writeStartObject() throws IOException
    //public void writeEndObject() throws IOException

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
            /* 02-Mar-2009, tatu: we are NOT to call _verifyValueWrite here,
             *   because that will be done when codec actually serializes
             *   contained POJO. If we did call it it would advance state
             *   causing exception later on
             */
            if (_objectCodec != null) {
                _objectCodec.writeValue(this, value);
                return;
            }
            _writeSimpleObject(value);
        }
    }

    @Override
    public void writeTree(TreeNode rootNode) throws IOException {
        // As with 'writeObject()', we are not check if write would work
        if (rootNode == null) {
            writeNull();
        } else {
            if (_objectCodec == null) {
                throw new IllegalStateException("No ObjectCodec defined");
            }
            _objectCodec.writeValue(this, rootNode);
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
}
