package com.fasterxml.jackson.core;

import java.io.*;
import java.net.URL;

import com.fasterxml.jackson.core.format.InputAccessor;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.io.DataOutputAsStream;
import com.fasterxml.jackson.core.io.InputDecorator;
import com.fasterxml.jackson.core.io.OutputDecorator;

/**
 * Intermediate base class for actual format-specific factories for constructing
 * parsers (reading) and generators (writing). Although full power will only be
 * available with Jackson 3, skeletal implementation added in 2.10 to help conversion
 * of code for 2.x to 3.x migration of projects depending on Jackson
 *
 * @since 2.10
 */
public abstract class TokenStreamFactory
    implements Versioned,
    java.io.Serializable
{
    private static final long serialVersionUID = 2;

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    public abstract boolean requiresPropertyOrdering();

    public abstract boolean canHandleBinaryNatively();

    public abstract boolean canUseCharArrays();

    public abstract boolean canParseAsync();

    public abstract Class<? extends FormatFeature> getFormatReadFeatureType();

    public abstract Class<? extends FormatFeature> getFormatWriteFeatureType();

    /*
    /**********************************************************
    /* Format detection functionality
    /**********************************************************
     */

    public abstract boolean canUseSchema(FormatSchema schema);

    public abstract String getFormatName();

    public abstract MatchStrength hasFormat(InputAccessor acc) throws IOException;

    public abstract boolean requiresCustomCodec();

    /*
    /**********************************************************
    /* Configuration access
    /**********************************************************
     */

    public abstract boolean isEnabled(JsonParser.Feature f);
    public abstract boolean isEnabled(JsonGenerator.Feature f);

    public abstract int getParserFeatures();
    public abstract int getGeneratorFeatures();

    public abstract int getFormatParserFeatures();
    public abstract int getFormatGeneratorFeatures();
    
    public abstract InputDecorator getInputDecorator();
    public abstract OutputDecorator getOutputDecorator();
    public abstract String getRootValueSeparator();

    /*
    /**********************************************************
    /* Factory methods, parsers
    /**********************************************************
     */

    public abstract JsonParser createParser(File f) throws IOException;
    public abstract JsonParser createParser(URL url) throws IOException;
    public abstract JsonParser createParser(InputStream in) throws IOException;
    public abstract JsonParser createParser(Reader r) throws IOException;
    public abstract JsonParser createParser(byte[] data) throws IOException;
    public abstract JsonParser createParser(byte[] data, int offset, int len) throws IOException;
    public abstract JsonParser createParser(String content) throws IOException;
    public abstract JsonParser createParser(char[] content) throws IOException;
    public abstract JsonParser createParser(char[] content, int offset, int len) throws IOException;
    public abstract JsonParser createParser(DataInput in) throws IOException;

    public abstract JsonParser createNonBlockingByteArrayParser() throws IOException;
    
    /*
    /**********************************************************
    /* Factory methods, generators
    /**********************************************************
     */

    public abstract JsonGenerator createGenerator(OutputStream out, JsonEncoding enc) throws IOException;
    public abstract JsonGenerator createGenerator(OutputStream out) throws IOException;
    public abstract JsonGenerator createGenerator(Writer w) throws IOException;
    public abstract JsonGenerator createGenerator(File f, JsonEncoding enc) throws IOException;
    public abstract JsonGenerator createGenerator(DataOutput out, JsonEncoding enc) throws IOException;
    public abstract JsonGenerator createGenerator(DataOutput out) throws IOException;

    /*
    /**********************************************************
    /* Deprecated parser factory methods: to be removed from 3.x
    /**********************************************************
     */

    /**
     * Method for constructing JSON parser instance to parse
     * contents of specified file.
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param f File that contains JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(File)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(File f) throws IOException, JsonParseException {
        return createParser(f);
    }

    /**
     * Method for constructing JSON parser instance to parse
     * contents of resource reference by given URL.
     *
     *<p>
     * Encoding is auto-detected from contents according to JSON
     * specification recommended mechanism. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     *<p>
     * Underlying input stream (needed for reading contents)
     * will be <b>owned</b> (and managed, i.e. closed as need be) by
     * the parser, since caller has no access to it.
     *
     * @param url URL pointing to resource that contains JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(URL)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(URL url) throws IOException, JsonParseException {
        return createParser(url);
    }

    /**
     * Method for constructing JSON parser instance to parse
     * the contents accessed via specified input stream.
     *<p>
     * The input stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *<p>
     *
     * Note: no encoding argument is taken since it can always be
     * auto-detected as suggested by JSON RFC. Json specification
     * supports only UTF-8, UTF-16 and UTF-32 as valid encodings,
     * so auto-detection implemented only for this charsets.
     * For other charsets use {@link #createParser(java.io.Reader)}.
     *
     * @param in InputStream to use for reading JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(InputStream)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(InputStream in) throws IOException, JsonParseException {
        return createParser(in);
    }

    /**
     * Method for constructing parser for parsing
     * the contents accessed via specified Reader.
     <p>
     * The read stream will <b>not be owned</b> by
     * the parser, it will still be managed (i.e. closed if
     * end-of-stream is reacher, or parser close method called)
     * if (and only if) {@link com.fasterxml.jackson.core.JsonParser.Feature#AUTO_CLOSE_SOURCE}
     * is enabled.
     *
     * @param r Reader to use for reading JSON content to parse
     * 
     * @deprecated Since 2.2, use {@link #createParser(Reader)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(Reader r) throws IOException, JsonParseException {
        return createParser(r);
    }

    /**
     * Method for constructing parser for parsing the contents of given byte array.
     * 
     * @deprecated Since 2.2, use {@link #createParser(byte[])} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(byte[] data) throws IOException, JsonParseException {
        return createParser(data);
    }

    /**
     * Method for constructing parser for parsing
     * the contents of given byte array.
     * 
     * @param data Buffer that contains data to parse
     * @param offset Offset of the first data byte within buffer
     * @param len Length of contents to parse within buffer
     * 
     * @deprecated Since 2.2, use {@link #createParser(byte[],int,int)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(byte[] data, int offset, int len) throws IOException, JsonParseException {
        return createParser(data, offset, len);
    }

    /**
     * Method for constructing parser for parsing
     * contents of given String.
     * 
     * @deprecated Since 2.2, use {@link #createParser(String)} instead.
     */
    @Deprecated
    public JsonParser createJsonParser(String content) throws IOException, JsonParseException {
        return createParser(content);
    }

    /*
    /**********************************************************
    /* Deprecated generator factory methods: to be removed from 3.x
    /**********************************************************
     */

    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified output stream.
     * Encoding to use must be specified, and needs to be one of available
     * types (as per JSON specification).
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the output stream when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#AUTO_CLOSE_TARGET}
     * is enabled).
     * Using application needs to close it explicitly if this is the case.
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats)
     * and that ignore passed in encoding.
     *
     * @param out OutputStream to use for writing JSON content 
     * @param enc Character encoding to use
     *
     * @deprecated Since 2.2, use {@link #createGenerator(OutputStream, JsonEncoding)} instead.
     */
    @Deprecated
    public JsonGenerator createJsonGenerator(OutputStream out, JsonEncoding enc) throws IOException {
        return createGenerator(out, enc);
    }

    /**
     * Method for constructing JSON generator for writing JSON content
     * using specified Writer.
     *<p>
     * Underlying stream <b>is NOT owned</b> by the generator constructed,
     * so that generator will NOT close the Reader when
     * {@link JsonGenerator#close} is called (unless auto-closing
     * feature,
     * {@link com.fasterxml.jackson.core.JsonGenerator.Feature#AUTO_CLOSE_TARGET} is enabled).
     * Using application needs to close it explicitly.
     *
     * @param out Writer to use for writing JSON content 
     * 
     * @deprecated Since 2.2, use {@link #createGenerator(Writer)} instead.
     */
    @Deprecated
    public JsonGenerator createJsonGenerator(Writer out) throws IOException {
        return createGenerator(out);
    }

    /**
     * Convenience method for constructing generator that uses default
     * encoding of the format (UTF-8 for JSON and most other data formats).
     *<p>
     * Note: there are formats that use fixed encoding (like most binary data formats).
     * 
     * @deprecated Since 2.2, use {@link #createGenerator(OutputStream)} instead.
     */
    @Deprecated
    public JsonGenerator createJsonGenerator(OutputStream out) throws IOException {
        return createGenerator(out, JsonEncoding.UTF8);
    }

    /*
    /**********************************************************
    /* Internal factory methods, other
    /**********************************************************
     */

    protected OutputStream _createDataOutputWrapper(DataOutput out) {
        return new DataOutputAsStream(out);
    }
    /**
     * Helper methods used for constructing an optimal stream for
     * parsers to use, when input is to be read from an URL.
     * This helps when reading file content via URL.
     */
    protected InputStream _optimizedStreamFromURL(URL url) throws IOException {
        if ("file".equals(url.getProtocol())) {
            /* Can not do this if the path refers
             * to a network drive on windows. This fixes the problem;
             * might not be needed on all platforms (NFS?), but should not
             * matter a lot: performance penalty of extra wrapping is more
             * relevant when accessing local file system.
             */
            String host = url.getHost();
            if (host == null || host.length() == 0) {
                // [core#48]: Let's try to avoid probs with URL encoded stuff
                String path = url.getPath();
                if (path.indexOf('%') < 0) {
                    return new FileInputStream(url.getPath());

                }
                // otherwise, let's fall through and let URL decoder do its magic
            }
        }
        return url.openStream();
    }
}
