package com.fasterxml.jackson.core.base;

import java.io.*;
import java.net.URL;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.io.UTF8Writer;

/**
 * Intermediate {@link TokenStreamFactory} sub-class used as the base for
 * textual data formats.
 */
@SuppressWarnings("resource")
public abstract class TextualTSFactory extends DecorableTSFactory
{
    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    protected TextualTSFactory() { super(); }

    protected TextualTSFactory(ObjectCodec codec) { super(codec); }

    /**
     * Constructor used when copy()ing a factory instance.
     */
    protected TextualTSFactory(TextualTSFactory src, ObjectCodec codec) {
        super(src, codec);
    }

    /*
    /**********************************************************
    /* Default introspection
    /**********************************************************
     */
    
    @Override
    public boolean canHandleBinaryNatively() {
        // typically textual formats need escaping like Base64 so:
        return false;
    }

    /*
    /**********************************************************
    /* Extended capabilities for textual formats (only)
    /**********************************************************
     */
    
    /**
     * Introspection method that can be used by base factory to check
     * whether access using <code>char[]</code> is something that actual
     * parser implementations can take advantage of, over having to
     * use {@link java.io.Reader}. Sub-types are expected to override
     * definition; default implementation (suitable for JSON) alleges
     * that optimization are possible; and thereby is likely to try
     * to access {@link java.lang.String} content by first copying it into
     * recyclable intermediate buffer.
     */
    public boolean canUseCharArrays() { return true; }

    /*
    /**********************************************************
    /* Factory methods: parsers, with context
    /**********************************************************
     */

    @Override
    public JsonParser createParser(File f) throws IOException {
        // true, since we create InputStream from File
        IOContext ioCtxt = _createContext(f, true);
        return _createParser(_decorate(ioCtxt, new FileInputStream(f)), ioCtxt);
    }

    @Override
    public JsonParser createParser(URL url) throws IOException {
        // true, since we create InputStream from URL
        IOContext ioCtxt = _createContext(url, true);
        return _createParser(_decorate(ioCtxt, _optimizedStreamFromURL(url)), ioCtxt);
    }

    @Override
    public JsonParser createParser(InputStream in) throws IOException {
        IOContext ioCtxt = _createContext(in, false);
        return _createParser(_decorate(ioCtxt, in), ioCtxt);
    }

    @Override
    public JsonParser createParser(Reader r) throws IOException {
        // false -> we do NOT own Reader (did not create it)
        IOContext ioCtxt = _createContext(r, false);
        return _createParser(_decorate(ioCtxt, r), ioCtxt);
    }

    @Override
    public JsonParser createParser(byte[] data, int offset, int len) throws IOException {
        IOContext ioCtxt = _createContext(data, true);
        if (_inputDecorator != null) {
            InputStream in = _inputDecorator.decorate(ioCtxt, data, offset, len);
            if (in != null) {
                return _createParser(in, ioCtxt);
            }
        }
        return _createParser(data, offset, len, ioCtxt);
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
        IOContext ioCtxt = _createContext(content, true);
        char[] buf = ioCtxt.allocTokenBuffer(strLen);
        content.getChars(0, strLen, buf, 0);
        return _createParser(buf, 0, strLen, ioCtxt, true);
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
        IOContext ioCtxt = _createContext(in, false);
        return _createParser(_decorate(ioCtxt, in), ioCtxt);
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
        IOContext ioCtxt = _createContext(out, false).setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(EMPTY_WRITE_CONTEXT, ioCtxt,
                    _decorate(ioCtxt, out));
        }
        Writer w = _createWriter(ioCtxt, out, enc);
        return _createGenerator(EMPTY_WRITE_CONTEXT, ioCtxt,
                _decorate(ioCtxt, w));
    }

    @Override
    public JsonGenerator createGenerator(Writer w) throws IOException {
        IOContext ioCtxt = _createContext(w, false);
        return _createGenerator(EMPTY_WRITE_CONTEXT, ioCtxt,
                _decorate(ioCtxt, w));
    }

    @Override
    public JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        // true -> yes, we have to manage the stream since we created it
        IOContext ioCtxt = _createContext(f, true).setEncoding(enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(EMPTY_WRITE_CONTEXT, ioCtxt,
                    _decorate(ioCtxt, out));
        }
        return _createGenerator(EMPTY_WRITE_CONTEXT, ioCtxt,
                _decorate(ioCtxt, _createWriter(ioCtxt, out, enc)));
    }

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // false -> we won't manage the stream unless explicitly directed to
        IOContext ioCtxt = _createContext(out, false, enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(writeCtxt, ioCtxt, _decorate(ioCtxt, out));
        }
        return _createGenerator(writeCtxt, ioCtxt,
                _decorate(ioCtxt, _createWriter(ioCtxt, out, enc)));
    }

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt, Writer w)
        throws IOException
    {
        IOContext ioCtxt = _createContext(w, false);
        return _createGenerator(writeCtxt, ioCtxt, _decorate(ioCtxt, w));
    }

    @Override
    public JsonGenerator createGenerator(ObjectWriteContext writeCtxt,
            File f, JsonEncoding enc)
        throws IOException
    {
        OutputStream out = new FileOutputStream(f);
        IOContext ioCtxt = _createContext(f, true, enc);
        if (enc == JsonEncoding.UTF8) {
            return _createUTF8Generator(writeCtxt, ioCtxt, _decorate(ioCtxt, out));
        }
        return _createGenerator(writeCtxt, ioCtxt,
                _decorate(ioCtxt, _createWriter(ioCtxt, out, enc)));
    }

    /*
    /**********************************************************
    /* Factory methods: abstract, for sub-classes to implement
    /**********************************************************
     */
    
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
    protected abstract JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, Writer out) throws IOException;

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
    protected abstract JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
            IOContext ioCtxt, OutputStream out) throws IOException;

    protected Writer _createWriter(IOContext ioCtxt, OutputStream out, JsonEncoding enc)
        throws IOException
    {
        // note: this should not get called any more (caller checks, dispatches)
        if (enc == JsonEncoding.UTF8) { // We have optimized writer for UTF-8
            return new UTF8Writer(ioCtxt, out);
        }
        // not optimal, but should do unless we really care about UTF-16/32 encoding speed
        return new OutputStreamWriter(out, enc.getJavaName());
    }
}
