package com.fasterxml.jackson.core.base;

import java.io.*;
import java.net.URL;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.*;

/**
 * Intermediate base implementation for factories that allow decorators
 * for input and output.
 *
 * @since 3.0
 */
@SuppressWarnings("resource")
public abstract class DecoratableTSFactory
    extends TokenStreamFactory
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Object that implements conversion functionality between
     * Java objects and JSON content. For base JsonFactory implementation
     * usually not set by default, but can be explicitly set.
     * Sub-classes (like @link org.codehaus.jackson.map.MappingJsonFactory}
     * usually provide an implementation.
     */
    protected ObjectCodec _objectCodec;

    /**
     * Optional helper object that may decorate input sources, to do
     * additional processing on input during parsing.
     */
    protected InputDecorator _inputDecorator;

    /**
     * Optional helper object that may decorate output object, to do
     * additional processing on output during content generation.
     */
    protected OutputDecorator _outputDecorator;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    protected DecoratableTSFactory() { this(null); }

    protected DecoratableTSFactory(ObjectCodec oc) {
        super();
        _objectCodec = oc;
    }

    /**
     * Constructor used when copy()ing a factory instance.
     */
    protected DecoratableTSFactory(DecoratableTSFactory src, ObjectCodec codec) {
        super(src);
        _objectCodec = codec;
    }

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Method for associating a {@link ObjectCodec} (typically
     * a <code>com.fasterxml.jackson.databind.ObjectMapper</code>)
     * with this factory (and more importantly, parsers and generators
     * it constructs). This is needed to use data-binding methods
     * of {@link JsonParser} and {@link JsonGenerator} instances.
     */
    @Override
    public DecoratableTSFactory setCodec(ObjectCodec oc) {
        _objectCodec = oc;
        return this;
    }

    @Override
    public ObjectCodec getCodec() { return _objectCodec; }

    /**
     * Method for getting currently configured output decorator (if any;
     * there is no default decorator).
     */
    @Override
    public OutputDecorator getOutputDecorator() {
        return _outputDecorator;
    }

    /**
     * Method for overriding currently configured output decorator
     */
    @Override
    public DecoratableTSFactory setOutputDecorator(OutputDecorator d) {
        _outputDecorator = d;
        return this;
    }

    /**
     * Method for getting currently configured input decorator (if any;
     * there is no default decorator).
     */
    @Override
    public InputDecorator getInputDecorator() {
        return _inputDecorator;
    }

    /**
     * Method for overriding currently configured input decorator
     */
    @Override
    public DecoratableTSFactory setInputDecorator(InputDecorator d) {
        _inputDecorator = d;
        return this;
    }

    /*
    /**********************************************************
    /* Factory methods: parsers
    /**********************************************************
     */

    @Override
    public JsonParser createParser(File f) throws IOException {
        // true, since we create InputStream from File
        IOContext ctxt = _createContext(f, true);
        InputStream in = new FileInputStream(f);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(URL url) throws IOException {
        // true, since we create InputStream from URL
        IOContext ctxt = _createContext(url, true);
        InputStream in = _optimizedStreamFromURL(url);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(InputStream in) throws IOException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(Reader r) throws IOException {
        // false -> we do NOT own Reader (did not create it)
        IOContext ctxt = _createContext(r, false);
        return _createParser(_decorate(r, ctxt), ctxt);
    }

    @Override
    public JsonParser createParser(byte[] data) throws IOException {
        IOContext ctxt = _createContext(data, true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, 0, data.length);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, 0, data.length, ctxt);
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        IOContext ctxt = _createContext(data, true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ctxt, data, offset, len);
            if (in != null) {
                return _createParser(in, ctxt);
            }
        }
        return _createParser(data, offset, len, ctxt);
    }

    @Override
    public JsonParser createParser(String content) throws IOException {
        final int strLen = content.length();
        // Actually, let's use this for medium-sized content, up to 64kB chunk (32kb char)
        if ((_inputDecorator != null) || (strLen > 0x8000) || !canUseCharArrays()) {
            // easier to just wrap in a Reader than extend InputDecorator; or, if content
            // is too long for us to copy it over
            return createParser(new StringReader(content));
        }
        IOContext ctxt = _createContext(content, true);
        char[] buf = ctxt.allocTokenBuffer(strLen);
        content.getChars(0, strLen, buf, 0);
        return _createParser(buf, 0, strLen, ctxt, true);
    }

    @Override
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        if (_inputDecorator != null) { // easier to just wrap in a Reader than extend InputDecorator
            return createParser(new CharArrayReader(content, offset, len));
        }
        return _createParser(content, offset, len, _createContext(content, true),
                // important: buffer is NOT recyclable, as it's from caller
                false);
    }
    
    @Override
    public JsonParser createParser(DataInput in) throws IOException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }
    
    protected abstract JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException;

    protected abstract JsonParser _createParser(Reader r, IOContext ctxt) throws IOException;

    protected abstract JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException;

    protected abstract JsonParser _createParser(char[] data, int offset, int len, IOContext ctxt,
        boolean recyclable) throws IOException;

    protected abstract JsonParser _createParser(DataInput input, IOContext ctxt) throws IOException;

    /*
    /**********************************************************
    /* Factory methods: generators
    /**********************************************************
     */

    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ctxt = _createContext(out, false);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(_decorate(out, ctxt), ctxt);
        }
        Writer w = _createWriter(out, enc, ctxt);
        return _createGenerator(_decorate(w, ctxt), ctxt);
    }

    @Override
    public JsonGenerator createGenerator(Writer w) throws IOException {
        IOContext ctxt = _createContext(w, false);
        return _createGenerator(_decorate(w, ctxt), ctxt);
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(_decorate(out, ctxt), ctxt);
        }
        Writer w = _createWriter(out, enc, ctxt);
        return _createGenerator(_decorate(w, ctxt), ctxt);
    }    

    /**
     * Overridable factory method that actually instantiates generator for
     * given {@link Writer} and context object.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     */
    protected abstract JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException;

    /**
     * Overridable factory method that actually instantiates generator for
     * given {@link OutputStream} and context object, using UTF-8 encoding.
     *<p>
     * This method is specifically designed to remain
     * compatible between minor versions so that sub-classes can count
     * on it being called as expected. That is, it is part of official
     * interface from sub-class perspective, although not a public
     * method available to users of factory implementations.
     */
    protected abstract JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException;

    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException
    {
        // note: this should not get called any more (caller checks, dispatches)
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
            return new UTF8Writer(ctxt, out);
        }
        // not optimal, but should do unless we really care about UTF-16/32 encoding speed
        return new OutputStreamWriter(out, enc.getJavaName());
    }

    /*
    /**********************************************************
    /* Decorators, input
    /**********************************************************
     */

    protected InputStream _decorate(InputStream in, IOContext ctxt) throws IOException
    {
        if (_inputDecorator != null) {
            InputStream in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected Reader _decorate(Reader in, IOContext ctxt) throws IOException
    {
        if (_inputDecorator != null) {
            Reader in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected DataInput _decorate(DataInput in, IOContext ctxt) throws IOException
    {
        if (_inputDecorator != null) {
            DataInput in2 = _inputDecorator.decorate(ctxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    /*
    /**********************************************************
    /* Decorators, output
    /**********************************************************
     */

    protected OutputStream _decorate(OutputStream out, IOContext ctxt) throws IOException
    {
        if (_outputDecorator != null) {
            OutputStream out2 = _outputDecorator.decorate(ctxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }

    protected Writer _decorate(Writer out, IOContext ctxt) throws IOException
    {
        if (_outputDecorator != null) {
            Writer out2 = _outputDecorator.decorate(ctxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }

}
