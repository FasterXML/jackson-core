package perf;

import com.fasterxml.jackson.core.*;

/**
 * Manually run micro-benchmark for checking performance of tokenizing
 * simple tokens (false, true, null).
 */
public class ManualSmallTokenRead extends ManualPerfTestBase
{
    protected final JsonFactory _factory;

    protected final byte[] _jsonBytes;
    protected final char[] _jsonChars;

    private ManualSmallTokenRead(JsonFactory f, String json) throws Exception {
        _factory = f;
        _jsonChars = json.toCharArray();
        _jsonBytes = json.getBytes("UTF-8");
    }

    public static void main(String[] args) throws Exception
    {
        if (args.length != 0) {
            System.err.println("Usage: java ...");
            System.exit(1);
        }
        final JsonFactory f = new JsonFactory();
        final String jsonStr = a2q(
"{'data':[true,false,null,false,null,true],'last':true}"
                );
        new ManualSmallTokenRead(f, jsonStr).test("char[]", "byte[]", jsonStr.length());
    }

    @Override
    protected void testRead1(int reps) throws Exception
    {
        while (--reps >= 0) {
            JsonParser p = _factory.createParser(_jsonChars);
            _stream(p);
            p.close();
        }
    }

    @Override
    protected void testRead2(int reps) throws Exception
    {
        while (--reps >= 0) {
            JsonParser p = _factory.createParser(_jsonBytes);
            _stream(p);
            p.close();
        }
    }

    private final void _stream(JsonParser p) throws Exception
    {
        JsonToken t;

        while ((t = p.nextToken()) != null) {
            // force decoding/reading of scalar values too (booleans are fine, nulls too)
            if (t == JsonToken.VALUE_STRING) {
                p.getText();
            } else if (t == JsonToken.VALUE_NUMBER_INT) {
                p.getLongValue();
            }
        }
    }
}
