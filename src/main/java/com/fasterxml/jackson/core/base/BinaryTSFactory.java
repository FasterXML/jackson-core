package com.fasterxml.jackson.core.base;

import java.io.*;
import java.net.URL;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;

/**
 * Intermediate {@link TokenStreamFactory} sub-class used as the base for
 * binary (non-textual) data formats.
 */
@SuppressWarnings("resource")
public abstract class BinaryTSFactory extends DecorableTSFactory
{
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    protected BinaryTSFactory() { super(); }

    protected BinaryTSFactory(ObjectCodec codec) { super(codec); }

    /**
     * Constructor used when copy()ing a factory instance.
     */
    protected BinaryTSFactory(BinaryTSFactory src, ObjectCodec codec) {
        super(src, codec);
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
        return _nonByteSource();
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
        return _nonByteSource();
    }

    @Override
    public JsonParser createParser(char[] content, int offset, int len) throws IOException {
        return _nonByteSource();
    }
    
    @Override
    public JsonParser createParser(DataInput in) throws IOException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }
    
    protected abstract JsonParser _createParser(InputStream in, IOContext ctxt) throws IOException;

    protected abstract JsonParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException;

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
        return _createGenerator(_decorate(out, ctxt), ctxt);
    }

    @Override
    public JsonGenerator createGenerator(Writer w) throws IOException {
        return _nonByteTarget();
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ctxt = _createContext(out, true);
        ctxt.setEncoding(enc);
        return _createGenerator(_decorate(out, ctxt), ctxt);
    }

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
    protected abstract JsonGenerator _createGenerator(OutputStream out, IOContext ctxt) throws IOException;

    protected <T> T _nonByteSource() throws IOException {
        throw new UnsupportedOperationException("Can not create parser for character-based (not byte-based) source");
    }

    protected <T> T _nonByteTarget() throws IOException {
        throw new UnsupportedOperationException("Can not create generator for character-based (not byte-based) target");
    }
}
