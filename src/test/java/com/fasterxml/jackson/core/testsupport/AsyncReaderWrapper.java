package com.fasterxml.jackson.core.testsupport;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;

public abstract class AsyncReaderWrapper
    implements AutoCloseable
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

    public String currentTextViaWriter() throws IOException
    {
        StringWriter sw = new StringWriter();
        int len = _streamReader.getText(sw);
        String str = sw.toString();
        if (len != str.length()) {
            throw new IllegalStateException(String.format(
                    "Reader.getText(Writer) returned %d, but wrote %d chars",
                    len, str.length()));
        }
        return str;
    }

    public String currentName() throws IOException {
        return _streamReader.getCurrentName();
    }

    public JsonParser parser() { return _streamReader; }

    public abstract JsonToken nextToken() throws IOException;

    public JsonStreamContext getParsingContext() {
        return _streamReader.getParsingContext();
    }

    public int getIntValue() throws IOException { return _streamReader.getIntValue(); }
    public long getLongValue() throws IOException { return _streamReader.getLongValue(); }
    public float getFloatValue() throws IOException { return _streamReader.getFloatValue(); }
    public double getDoubleValue() throws IOException { return _streamReader.getDoubleValue(); }
    public BigInteger getBigIntegerValue() throws IOException { return _streamReader.getBigIntegerValue(); }
    public BigDecimal getDecimalValue() throws IOException { return _streamReader.getDecimalValue(); }
    public byte[] getBinaryValue() throws IOException { return _streamReader.getBinaryValue(); }

    public Number getNumberValue() throws IOException { return _streamReader.getNumberValue(); }
    public NumberType getNumberType() throws IOException { return _streamReader.getNumberType(); }

    public Object getNumberValueDeferred() throws IOException { return _streamReader.getNumberValueDeferred(); }

    @Override
    public void close() throws IOException { _streamReader.close(); }

    public boolean isClosed() {
        return _streamReader.isClosed();
    }
}
