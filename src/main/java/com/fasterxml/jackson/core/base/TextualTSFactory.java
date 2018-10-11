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
public abstract class TextualTSFactory
    extends DecorableTSFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    protected TextualTSFactory(int formatPF, int formatGF) {
        super(formatPF, formatGF);
    }

    /**
     * Constructors used by builders for instantiation.
     *
     * @since 3.0
     */
    protected TextualTSFactory(DecorableTSFBuilder<?,?> baseBuilder)
    {
        super(baseBuilder);
    }

    /**
     * Copy constructor.
     */
    protected TextualTSFactory(TextualTSFactory src) {
        super(src);
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
    public JsonParser createParser(ObjectReadContext readCtxt,
            File f) throws IOException {
        // true, since we create InputStream from File
        IOContext ioCtxt = _createContext(f, true);
        return _createParser(readCtxt, ioCtxt,
                _decorate(ioCtxt, new FileInputStream(f)));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            URL url) throws IOException {
        // true, since we create InputStream from URL
        IOContext ioCtxt = _createContext(url, true);
        return _createParser(readCtxt, ioCtxt,
                _decorate(ioCtxt, _optimizedStreamFromURL(url)));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            InputStream in) throws IOException {
        IOContext ioCtxt = _createContext(in, false);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, in));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            Reader r) throws IOException {
        // false -> we do NOT own Reader (did not create it)
        IOContext ioCtxt = _createContext(r, false);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, r));
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            byte[] data, int offset, int len) throws IOException {
        IOContext ioCtxt = _createContext(data, true);
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
            String content) throws IOException
    {
        final int strLen = content.length();
        // Actually, let's use this for medium-sized content, up to 64kB chunk (32kb char)
        if ((_inputDecorator != null) || (strLen > 0x8000) || !canUseCharArrays()) {
            // easier to just wrap in a Reader than extend InputDecorator; or, if content
            // is too long for us to copy it over
            return createParser(readCtxt, new StringReader(content));
        }
        IOContext ioCtxt = _createContext(content, true);
        char[] buf = ioCtxt.allocTokenBuffer(strLen);
        content.getChars(0, strLen, buf, 0);
        return _createParser(readCtxt, ioCtxt, buf, 0, strLen, true);
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt,
            char[] content, int offset, int len) throws IOException {
        if (_inputDecorator != null) { // easier to just wrap in a Reader than extend InputDecorator
            return createParser(readCtxt, new CharArrayReader(content, offset, len));
        }
        return _createParser(readCtxt, _createContext(content, true),
                content, offset, len,
                // important: buffer is NOT recyclable, as it's from caller
                false);
    }

    @Override
    public JsonParser createParser(ObjectReadContext readCtxt, 
            DataInput in) throws IOException
    {
        IOContext ioCtxt = _createContext(in, false);
        return _createParser(readCtxt, ioCtxt, _decorate(ioCtxt, in));
    }
    
    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ctxt, InputStream in) throws IOException;

    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ctxt, Reader r) throws IOException;

    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ctxt,
            byte[] data, int offset, int len) throws IOException;

    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ctxt, char[] data, int offset, int len, boolean recyclable)
                    throws IOException;

    protected abstract JsonParser _createParser(ObjectReadContext readCtxt,
            IOContext ctxt, DataInput input) throws IOException;

    /*
    /**********************************************************
    /* Factory methods: generators
    /**********************************************************
     */

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
