package tools.jackson.core.testsupport;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.core.JsonParser.NumberType;

public abstract class AsyncReaderWrapper
    implements AutoCloseable
{
    protected final JsonParser _streamReader;

    protected AsyncReaderWrapper(JsonParser sr) {
        _streamReader = sr;
    }

    public JsonToken currentToken() {
        return _streamReader.currentToken();
    }
    public String currentText() {
        return _streamReader.getText();
    }

    public String currentTextViaCharacters()
    {
        char[] ch = _streamReader.getTextCharacters();
        int start = _streamReader.getTextOffset();
        int len = _streamReader.getTextLength();
        return new String(ch, start, len);
    }

    public String currentTextViaWriter()
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

    public String currentName() {
        return _streamReader.currentName();
    }

    public JsonParser parser() { return _streamReader; }

    public abstract JsonToken nextToken();

    public TokenStreamContext getParsingContext() {
        return _streamReader.streamReadContext();
    }

    public int getIntValue() { return _streamReader.getIntValue(); }
    public long getLongValue() { return _streamReader.getLongValue(); }
    public float getFloatValue() { return _streamReader.getFloatValue(); }
    public double getDoubleValue() { return _streamReader.getDoubleValue(); }
    public BigInteger getBigIntegerValue() { return _streamReader.getBigIntegerValue(); }
    public BigDecimal getDecimalValue() { return _streamReader.getDecimalValue(); }
    public byte[] getBinaryValue() { return _streamReader.getBinaryValue(); }

    public Number getNumberValue() { return _streamReader.getNumberValue(); }
    public Object getNumberValueDeferred() { return _streamReader.getNumberValueDeferred(); }
    public NumberType getNumberType() { return _streamReader.getNumberType(); }

    @Override
    public void close() { _streamReader.close(); }

    public boolean isClosed() {
        return _streamReader.isClosed();
    }
}
