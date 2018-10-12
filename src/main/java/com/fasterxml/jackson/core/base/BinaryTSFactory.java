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
public abstract class BinaryTSFactory
    extends DecorableTSFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    protected BinaryTSFactory(int formatPF, int formatGF) {
        super(formatPF, formatGF);
    }

    /**
     * Constructors used by builders for instantiation.
     *
     * @since 3.0
     */
    protected BinaryTSFactory(DecorableTSFBuilder<?,?> baseBuilder)
    {
        super(baseBuilder);
    }

    /**
     * Copy constructor.
     */
    protected BinaryTSFactory(BinaryTSFactory src) {
        super(src);
    }

    /*
    /**********************************************************
    /* Default introspection
    /**********************************************************
     */
    
    @Override
    public boolean canHandleBinaryNatively() {
        // binary formats tend to support native inclusion:
        return true;
    }
    
    /*
    /**********************************************************
    /* Factory methods: parsers
    /**********************************************************
     */

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, File f) throws IOException {
        // true, since we create InputStream from File
        IOContext ioCtxt = _createContext(f, true);
        InputStream in = new FileInputStream(f);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, in));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, URL url) throws IOException {
        // true, since we create InputStream from URL
        IOContext ioCtxt = _createContext(url, true);
        InputStream in = _optimizedStreamFromURL(url);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, in));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, InputStream in) throws IOException {
        IOContext ioCtxt = _createContext(in, false);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, in));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, Reader r) throws IOException {
        return _nonByteSource();
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, 
            byte[] data, int offset, int len) throws IOException {
        IOContext ioCtxt = _createContext(data, true, null);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ioCtxt, data, offset, len);
            if (in != null) {
                return _createParser(readCtxt, ioCtxt, in);
            }
        }
        return _createParser(readCtxt, ioCtxt, data, offset, len);
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, 
            String content) throws IOException {
        return _nonByteSource();
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, 
            char[] content, int offset, int len) throws IOException {
        return _nonByteSource();
    }
    
    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, 
            DataInput in) throws IOException {
        IOContext ioCtxt = _createContext(in, false);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, in));
    }
    
    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ioCtxt, InputStream in) throws IOException;

    protected abstract JsonParser _createParser(ObjectReadContext readCtxt, 
            IOContext ioCtxt, byte[] data, int offset, int len) throws IOException;

    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ioCtxt, DataInput input) throws IOException;

    /*
    /**********************************************************
    /* Factory methods: generators
    /**********************************************************
     */

    /*
    @Override
    public JsonGenerator createGenerator(OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ioCtxt = _createContext(out, false, enc);
        return _createGenerator(EMPTY_WRITE_CONTEXT, ioCtxt, _decorate(ioCtxt, out));
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ioCtxt = _createContext(out, true, enc);
        return _createGenerator(EMPTY_WRITE_CONTEXT, ioCtxt, _decorate(ioCtxt, out));
    }
    */

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ioCtxt = _createContext(out, false, enc);
        return _createGenerator(writeCtxt, ioCtxt, _decorate(ioCtxt, out));
    }

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            Writer w) throws IOException {
        return _nonByteTarget();
    }

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ioCtxt = _createContext(out, true, enc);
        return _createGenerator(writeCtxt, ioCtxt, _decorate(ioCtxt, out));
    }

    /*
    /**********************************************************
    /* Factory methods: abstract, for sub-classes to implement
    /**********************************************************
     */

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
    protected abstract JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ctxt, OutputStream out) throws IOException;

    protected <T> T _nonByteSource() throws IOException {
        throw new UnsupportedOperationException("Can not create parser for character-based (not byte-based) source");
    }

    protected <T> T _nonByteTarget() throws IOException {
        throw new UnsupportedOperationException("Can not create generator for character-based (not byte-based) target");
    }
}
