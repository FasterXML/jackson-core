package tools.jackson.core.unittest.write;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Set of basic unit tests for verifying that copy-through methods
 * of {@link JsonGenerator} work as expected.
 */
class GeneratorCopyTest
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = sharedStreamFactory();

    @Test
    void copyRootTokens()
            throws IOException
    {
        JsonFactory jf = JSON_F;
        final String DOC = "\"text\\non two lines\" true false 2.0 null 1234567890123 ";
        JsonParser jp = jf.createParser(ObjectReadContext.empty(), new StringReader(DOC));
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createGenerator(ObjectWriteContext.empty(), sw);

        JsonToken t;

        while ((t = jp.nextToken()) != null) {
            gen.copyCurrentEvent(jp);
            // should not change parser state:
            assertToken(t, jp.currentToken());
        }
        jp.close();
        gen.close();

        assertEquals("\"text\\non two lines\" true false 2.0 null 1234567890123", sw.toString());
    }

    @Test
    void copyArrayTokens()
            throws IOException
    {
        JsonFactory jf = JSON_F;
        final String DOC = "123 [ 1, null, [ false, 1234567890124 ] ]";
        JsonParser jp = jf.createParser(ObjectReadContext.empty(), new StringReader(DOC));
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createGenerator(ObjectWriteContext.empty(), sw);

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        gen.copyCurrentEvent(jp);
        // should not change parser state:
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.currentToken());
        assertEquals(123, jp.getIntValue());

        // And then let's copy the array
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        gen.copyCurrentStructure(jp);
        // which will advance parser to matching close Array
        assertToken(JsonToken.END_ARRAY, jp.currentToken());
        jp.close();
        gen.close();

        assertEquals("123 [1,null,[false,1234567890124]]", sw.toString());
    }

    @Test
    void copyObjectTokens()
            throws IOException
    {
        JsonFactory jf = JSON_F;
        final String DOC = "{ \"a\":1, \"b\":[{ \"c\" : null, \"d\" : 0.25 }] }";
        JsonParser jp = jf.createParser(ObjectReadContext.empty(), new StringReader(DOC));
        StringWriter sw = new StringWriter();
        JsonGenerator gen = jf.createGenerator(ObjectWriteContext.empty(), sw);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        gen.copyCurrentStructure(jp);
        // which will advance parser to matching end Object
        assertToken(JsonToken.END_OBJECT, jp.currentToken());
        jp.close();
        gen.close();
        assertEquals("{\"a\":1,\"b\":[{\"c\":null,\"d\":0.25}]}", sw.toString());

        // but also sort of special case of field name plus value
        jp = jf.createParser(ObjectReadContext.empty(), new StringReader("{\"a\":1,\"b\":null}"));
        sw = new StringWriter();
        gen = jf.createGenerator(ObjectWriteContext.empty(), sw);
        gen.writeStartObject();
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.PROPERTY_NAME, jp.nextToken());
        gen.copyCurrentStructure(jp);
        gen.writeEndObject();

        jp.close();
        gen.close();
        assertEquals("{\"a\":1}", sw.toString());
    }
}
