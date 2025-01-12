package tools.jackson.core.read;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;

import static org.junit.jupiter.api.Assertions.*;

class ParserDupHandlingTest
    extends JacksonCoreTestBase
{
    private final String[] DUP_DOCS = new String[] {
            "{ 'a':1, 'a':2 }",
            "[{ 'a':1, 'a':2 }]",
            "{ 'a':1, 'b':2, 'c':3,'a':true,'e':false }",
            "{ 'foo': { 'bar': [ [ { 'x':3, 'a':1 } ]], 'x':0, 'a':'y', 'b':3,'a':13 } }",
            "[{'b':1},{'b\":3},[{'a':3}], {'a':1,'a':2}]",
            "{'b':1,'array':[{'b':3}],'ob':{'b':4,'x':0,'y':3,'a':true,'a':false }}",
    };
    {
        for (int i = 0; i < DUP_DOCS.length; ++i) {
            DUP_DOCS[i] = a2q(DUP_DOCS[i]);
        }
    }

    @Test
    void simpleDupCheckDisabled() throws Exception
    {
        // first: verify no problems if detection NOT enabled
        final JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(StreamReadFeature.STRICT_DUPLICATE_DETECTION));
        for (String doc : DUP_DOCS) {
            _testSimpleDupsOk(doc, f, MODE_INPUT_STREAM);
            _testSimpleDupsOk(doc, f, MODE_INPUT_STREAM_THROTTLED);
            _testSimpleDupsOk(doc, f, MODE_READER);
            _testSimpleDupsOk(doc, f, MODE_DATA_INPUT);
        }
    }

    @Test
    void simpleDupsBytes() throws Exception
    {
        JsonFactory dupF = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        for (String doc : DUP_DOCS) {
            _testSimpleDupsFail(doc, dupF, MODE_INPUT_STREAM, "a");

            _testSimpleDupsFail(doc, dupF, MODE_INPUT_STREAM_THROTTLED, "a");
        }
    }

    @Test
    void simpleDupsDataInput() throws Exception
    {
        JsonFactory dupF = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        for (String doc : DUP_DOCS) {
            _testSimpleDupsFail(doc, dupF, MODE_DATA_INPUT, "a");
        }
    }

    @Test
    void simpleDupsChars() throws Exception
    {
        JsonFactory dupF = JsonFactory.builder()
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        for (String doc : DUP_DOCS) {
            _testSimpleDupsFail(doc, dupF, MODE_READER, "a");
        }
    }

    private void _testSimpleDupsOk(final String doc, JsonFactory f,
            int mode) throws Exception
    {
        JsonParser p = createParser(f, mode, doc);
        JsonToken t = p.nextToken();
        assertNotNull(t);
        assertTrue(t.isStructStart());

        int depth = 1;

        while (depth > 0) {
            switch (p.nextToken()) {
            case START_ARRAY:
            case START_OBJECT:
                ++depth;
                break;
            case END_ARRAY:
            case END_OBJECT:
                --depth;
                break;
            default:
            }
        }
        if (mode != MODE_DATA_INPUT) {
            assertNull(p.nextToken());
        }
        p.close();
    }

    private void _testSimpleDupsFail(final String doc, JsonFactory f,
            int mode, String name)
    {
        JsonParser p = createParser(f, mode, doc);
        JsonToken t = p.nextToken();
        assertNotNull(t);
        assertTrue(t.isStructStart());

        int depth = 1;
        StreamReadException e = null;

        while (depth > 0) {
            try {
                switch (p.nextToken()) {
                case START_ARRAY:
                case START_OBJECT:
                    ++depth;
                    break;
                case END_ARRAY:
                case END_OBJECT:
                    --depth;
                    break;
                default:
                }
            } catch (StreamReadException e0) {
                e = e0;
                break;
            }
        }
        p.close();

        if (e == null) {
            fail("Should have caught exception for dup");
        }
        verifyException(e, "duplicate Object Property \""+name+"\"");
    }
}
