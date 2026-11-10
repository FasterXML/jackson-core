package tools.jackson.core.json;

import java.io.*;

import tools.jackson.core.*;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;

/**
 * Specialization of {@link UTF8DataInputJsonParser} that tracks the number of
 * bytes read and enforces the
 * {@link tools.jackson.core.StreamReadConstraints#getMaxDocumentLength()} limit.
 *<p>
 * Byte tracking is achieved by overriding {@link #readUnsignedByte()}: every
 * call increments {@link #_bytesRead} so that validation can be performed at
 * token boundaries in {@link #nextToken()}.
 *
 * @since 3.2
 */
public class UTF8DataInputWithDocLengthJsonParser
    extends UTF8DataInputJsonParser
{
    /**
     * Running total of bytes read from {@link #_inputData}.
     */
    protected long _bytesRead;

    public UTF8DataInputWithDocLengthJsonParser(ObjectReadContext readCtxt, IOContext ctxt,
            int stdFeatures, int formatFeatures, DataInput inputData,
            ByteQuadsCanonicalizer sym, int firstByte)
    {
        super(readCtxt, ctxt, stdFeatures, formatFeatures, inputData, sym, firstByte);
        // NOTE: bytes consumed by ByteSourceJsonBootstrapper.skipUTF8BOM() before this
        // constructor (1 without BOM, 4 with BOM) are not tracked — the undercount of
        // a few bytes is negligible for document length constraint enforcement.
    }

    @Override
    protected int readUnsignedByte() throws IOException {
        ++_bytesRead;
        return _inputData.readUnsignedByte();
    }

    @Override
    public JsonToken nextToken() throws JacksonException {
        JsonToken token = super.nextToken();
        _streamReadConstraints.validateDocumentLength(_bytesRead);
        return token;
    }

}
