package tools.jackson.core.json.async;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import tools.jackson.core.JacksonException;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.async.ByteBufferFeeder;
import tools.jackson.core.async.NonBlockingInputFeeder;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.sym.ByteQuadsCanonicalizer;

/**
 * Non-blocking parser implementation for JSON content that takes its input
 * via {@link java.nio.ByteBuffer} instance(s) passed.
 *<p>
 * NOTE: only supports parsing of UTF-8 encoded content (and 7-bit US-ASCII since
 * it is strict subset of UTF-8): other encodings are not supported.
 */
public class NonBlockingByteBufferJsonParser
        extends NonBlockingUtf8JsonParserBase
        implements ByteBufferFeeder
{
    private ByteBuffer _inputBuffer = ByteBuffer.wrap(NO_BYTES);

    public NonBlockingByteBufferJsonParser(ObjectReadContext readCtxt, IOContext ctxt,
            int stdFeatures, int formatReadFeatures, ByteQuadsCanonicalizer sym) {
        super(readCtxt, ctxt, stdFeatures, formatReadFeatures, sym);
    }

    @Override
    public NonBlockingInputFeeder nonBlockingInputFeeder() {
        return this;
    }

    @Override
    public void feedInput(final ByteBuffer byteBuffer) throws JacksonException {
        // Must not have remaining input
        if (_inputPtr < _inputEnd) {
            _reportError("Still have %d undecoded bytes, should not call 'feedInput'", _inputEnd - _inputPtr);
        }

        final int start = byteBuffer.position();
        final int end = byteBuffer.limit();

        if (end < start) {
            _reportError("Input end (%d) may not be before start (%d)", end, start);
        }
        // and shouldn't have been marked as end-of-input
        if (_endOfInput) {
            _reportError("Already closed, can not feed more input");
        }
        // Time to update pointers first
        _currInputProcessed += _origBufferLen;

        // Also need to adjust row start, to work as if it extended into the past wrt new buffer
        _currInputRowStart = start - (_inputEnd - _currInputRowStart);

        // And then update buffer settings
        _currBufferStart = start;
        _inputBuffer = byteBuffer;
        _inputPtr = start;
        _inputEnd = end;
        _origBufferLen = end - start;
    }

    @Override
    public int releaseBuffered(final OutputStream out) throws JacksonException {
        final int avail = _inputEnd - _inputPtr;
        if (avail > 0) {
            final WritableByteChannel channel = Channels.newChannel(out);
            try {
                channel.write(_inputBuffer);
            } catch (IOException e) {
                throw _wrapIOFailure(e);
            }
        }
        return avail;
    }

    @Override
    protected byte getNextSignedByteFromBuffer() {
        return _inputBuffer.get(_inputPtr++);
    }

    @Override
    protected int getNextUnsignedByteFromBuffer() {
        return _inputBuffer.get(_inputPtr++) & 0xFF;
    }

    @Override
    protected byte getByteFromBuffer(final int ptr) {
        return _inputBuffer.get(ptr);
    }
}
