package com.fasterxml.jackson.core.testsupport;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;

/**
 * Helper class used with async parser
 */
public class AsyncReaderWrapperForByteArray extends AsyncReaderWrapper
{
    private final byte[] _doc;
    private final int _bytesPerFeed;
    private final int _padding;

    private int _offset;
    private int _end;

    public AsyncReaderWrapperForByteArray(JsonParser sr, int bytesPerCall,
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
    public JsonToken nextToken() throws IOException
    {
        JsonToken token;

        while ((token = _streamReader.nextToken()) == JsonToken.NOT_AVAILABLE) {
            ByteArrayFeeder feeder = (ByteArrayFeeder) _streamReader.getNonBlockingInputFeeder();
            if (!feeder.needMoreInput()) {
                throw new IOException("Got NOT_AVAILABLE, could not feed more input");
            }
            int amount = Math.min(_bytesPerFeed, _end - _offset);
            if (amount < 1) { // end-of-input?
                feeder.endOfInput();
            } else {
                // padding?
                if (_padding == 0) {
                    feeder.feedInput(_doc, _offset, _offset+amount);
                } else {
                    byte[] tmp = new byte[amount + _padding + _padding];
                    System.arraycopy(_doc, _offset, tmp, _padding, amount);
                    feeder.feedInput(tmp, _padding, _padding+amount);
                }
                _offset += amount;
            }
        }
        return token;
    }
}
