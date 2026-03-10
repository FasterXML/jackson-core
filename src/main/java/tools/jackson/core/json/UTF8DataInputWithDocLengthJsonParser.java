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
 * The tracking is achieved by wrapping the source {@link DataInput} in a
 * {@link CountingDataInput} that increments a counter on every
 * {@code readUnsignedByte()} call.  Validation is performed at token
 * boundaries (in {@link #nextToken()} and {@link #nextName()}).
 */
public class UTF8DataInputWithDocLengthJsonParser
    extends UTF8DataInputJsonParser
{
    /**
     * The counting wrapper we passed to the superclass constructor.
     * Held here so we can query {@link CountingDataInput#getBytesRead()}
     * when performing document-length validation.
     */
    private final CountingDataInput _countingInput;

    public UTF8DataInputWithDocLengthJsonParser(ObjectReadContext readCtxt, IOContext ctxt,
            int stdFeatures, int formatFeatures, DataInput inputData,
            ByteQuadsCanonicalizer sym, int firstByte)
    {
        // Wrap the real DataInput so all readUnsignedByte() calls are counted
        super(readCtxt, ctxt, stdFeatures, formatFeatures,
                new CountingDataInput(inputData), sym, firstByte);
        // _inputData has been set to our CountingDataInput by the super constructor
        _countingInput = (CountingDataInput) _inputData;
    }

    @Override
    public JsonToken nextToken() throws JacksonException {
        JsonToken token = super.nextToken();
        _streamReadConstraints.validateDocumentLength(_countingInput.getBytesRead());
        return token;
    }

    @Override
    public String nextName() throws JacksonException {
        String name = super.nextName();
        _streamReadConstraints.validateDocumentLength(_countingInput.getBytesRead());
        return name;
    }

    /*
    /**********************************************************************
    /* Helper class: counting DataInput wrapper
    /**********************************************************************
     */

    /**
     * {@link DataInput} wrapper that counts the number of bytes consumed via
     * {@link #readUnsignedByte()} (the only method used by the JSON parser).
     */
    static final class CountingDataInput implements DataInput
    {
        private final DataInput _wrapped;
        private long _bytesRead;

        CountingDataInput(DataInput wrapped) {
            _wrapped = wrapped;
        }

        long getBytesRead() {
            return _bytesRead;
        }

        @Override
        public int readUnsignedByte() throws IOException {
            ++_bytesRead;
            return _wrapped.readUnsignedByte();
        }

        // ---- Remaining DataInput methods delegate straight through ----

        @Override
        public void readFully(byte[] b) throws IOException {
            _wrapped.readFully(b);
            _bytesRead += b.length;
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            _wrapped.readFully(b, off, len);
            _bytesRead += len;
        }

        @Override
        public int skipBytes(int n) throws IOException {
            int skipped = _wrapped.skipBytes(n);
            _bytesRead += skipped;
            return skipped;
        }

        @Override
        public boolean readBoolean() throws IOException {
            return _wrapped.readBoolean();
        }

        @Override
        public byte readByte() throws IOException {
            return _wrapped.readByte();
        }

        @Override
        public short readShort() throws IOException {
            return _wrapped.readShort();
        }

        @Override
        public int readUnsignedShort() throws IOException {
            return _wrapped.readUnsignedShort();
        }

        @Override
        public char readChar() throws IOException {
            return _wrapped.readChar();
        }

        @Override
        public int readInt() throws IOException {
            return _wrapped.readInt();
        }

        @Override
        public long readLong() throws IOException {
            return _wrapped.readLong();
        }

        @Override
        public float readFloat() throws IOException {
            return _wrapped.readFloat();
        }

        @Override
        public double readDouble() throws IOException {
            return _wrapped.readDouble();
        }

        @Override
        public String readLine() throws IOException {
            return _wrapped.readLine();
        }

        @Override
        public String readUTF() throws IOException {
            return _wrapped.readUTF();
        }
    }
}
