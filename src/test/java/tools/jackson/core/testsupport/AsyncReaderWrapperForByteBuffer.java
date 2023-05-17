package tools.jackson.core.testsupport;

import java.nio.ByteBuffer;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.async.ByteBufferFeeder;
import tools.jackson.core.exc.StreamReadException;

/**
 * Helper class used with async parser
 */
public class AsyncReaderWrapperForByteBuffer extends AsyncReaderWrapper
{
    private final byte[] _doc;
    private final int _bytesPerFeed;
    private final int _padding;

    private int _offset;
    private int _end;

    public AsyncReaderWrapperForByteBuffer(JsonParser sr, int bytesPerCall,
                                           byte[] doc, int padding)
    {
        super(sr);
        _bytesPerFeed = bytesPerCall;
        _doc = doc;
        _offset = 0;
        _end = doc.length;
        _padding = padding;
    }

    @Override
    public JsonToken nextToken() throws JacksonException
    {
        JsonToken token;

        while ((token = _streamReader.nextToken()) == JsonToken.NOT_AVAILABLE) {
            ByteBufferFeeder feeder = (ByteBufferFeeder) _streamReader.nonBlockingInputFeeder();
            if (!feeder.needMoreInput()) {
                throw new StreamReadException(null, "Got NOT_AVAILABLE, could not feed more input");
            }
            int amount = Math.min(_bytesPerFeed, _end - _offset);
            if (amount < 1) { // end-of-input?
                feeder.endOfInput();
            } else {
                // padding?
                if (_padding == 0) {
                    feeder.feedInput(ByteBuffer.wrap(_doc, _offset, amount));
                } else {
                    byte[] tmp = new byte[amount + _padding + _padding];
                    System.arraycopy(_doc, _offset, tmp, _padding, amount);
                    feeder.feedInput(ByteBuffer.wrap(tmp, _padding, amount));
                }
                _offset += amount;
            }
        }
        return token;
    }
}
