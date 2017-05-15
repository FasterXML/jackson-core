package com.fasterxml.jackson.core.async;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonToken;

public abstract class AsyncReaderWrapper
{
    protected final JsonParser _streamReader;

    protected AsyncReaderWrapper(JsonParser sr) {
        _streamReader = sr;
    }

    public JsonToken currentToken() throws IOException {
        return _streamReader.currentToken();
    }
    public String currentText() throws IOException {
        return _streamReader.getText();
    }

    public String currentTextViaCharacters() throws IOException
    {
        char[] ch = _streamReader.getTextCharacters();
        int start = _streamReader.getTextOffset();
        int len = _streamReader.getTextLength();
        return new String(ch, start, len);

    }

    public String currentName() throws IOException {
        return _streamReader.getCurrentName();
    }

    public JsonParser parser() { return _streamReader; }

    public abstract JsonToken nextToken() throws IOException;

    public int getIntValue() throws IOException { return _streamReader.getIntValue(); }
    public long getLongValue() throws IOException { return _streamReader.getLongValue(); }
    public float getFloatValue() throws IOException { return _streamReader.getFloatValue(); }
    public double getDoubleValue() throws IOException { return _streamReader.getDoubleValue(); }
    public BigInteger getBigIntegerValue() throws IOException { return _streamReader.getBigIntegerValue(); }
    public BigDecimal getBigDecimalValue() throws IOException { return _streamReader.getDecimalValue(); }
    public byte[] getBinaryValue() throws IOException { return _streamReader.getBinaryValue(); }

    public NumberType getNumberType() throws IOException { return _streamReader.getNumberType(); }

    public void close() throws IOException { _streamReader.close(); }

    public boolean isClosed() {
        return _streamReader.isClosed();
    }
}
