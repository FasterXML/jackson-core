package com.fasterxml.jackson.core.json.async;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.async.AsyncTestBase;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.testsupport.AsyncReaderWrapper;

public class AsyncNonStdParsingTest extends AsyncTestBase
{
    public void testLargeUnquotedNames() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .build();
        StringBuilder sb = new StringBuilder(5000);
        sb.append("[\n");
        final int REPS = 1050;
        for (int i = 0; i < REPS; ++i) {
            if (i > 0) {
                sb.append(',');
                if ((i & 7) == 0) {
                    sb.append('\n');
                }
            }
            sb.append("{");
            sb.append("abc").append(i&127).append(':');
            sb.append((i & 1) != 0);
            sb.append("}\n");
        }
        sb.append("]");
        String doc = sb.toString();

        _testLargeUnquoted(f, REPS, doc, 0, 99);
        _testLargeUnquoted(f, REPS, doc, 0, 5);
        _testLargeUnquoted(f, REPS, doc, 0, 3);
        _testLargeUnquoted(f, REPS, doc, 0, 2);
        _testLargeUnquoted(f, REPS, doc, 0, 1);

        _testLargeUnquoted(f, REPS, doc, 1, 99);
        _testLargeUnquoted(f, REPS, doc, 1, 1);
    }

    private void _testLargeUnquoted(JsonFactory f, int reps, String doc,
            int offset, int readSize) throws Exception
    {
        AsyncReaderWrapper p = createParser(f, doc, offset, readSize);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        for (int i = 0; i < reps; ++i) {
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals("abc"+(i&127), p.currentName());
            assertToken(((i&1) != 0) ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, p.nextToken());
            assertToken(JsonToken.END_OBJECT, p.nextToken());
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    public void testSimpleUnquotedNames() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .build();
        _testSimpleUnquoted(f, 0, 99);
        _testSimpleUnquoted(f, 0, 5);
        _testSimpleUnquoted(f, 0, 3);
        _testSimpleUnquoted(f, 0, 2);
        _testSimpleUnquoted(f, 0, 1);

        _testSimpleUnquoted(f, 1, 99);
        _testSimpleUnquoted(f, 1, 3);
        _testSimpleUnquoted(f, 1, 1);
    }

    private void _testSimpleUnquoted(JsonFactory f,
            int offset, int readSize) throws Exception
    {
        String doc = "{ a : 1, _foo:true, $:\"money!\", \" \":null }";
        AsyncReaderWrapper p = createParser(f, doc, offset, readSize);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("_foo", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("$", p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("money!", p.currentText());

        // and then regular quoted one should still work too:
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(" ", p.currentName());

        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();

        // Another thing, as per [jackson-cre#102]: numbers

        p = createParser(f, "{ 123:true,4:false }", offset, readSize);

        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("123", p.currentName());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("4", p.currentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    /**
     * Test to verify that the default parser settings do not
     * accept single-quotes for String values (field names,
     * textual values)
     */
    public void testAposQuotingDisabled() throws Exception
    {
        JsonFactory f = new JsonFactory();
        _testSingleQuotesDefault(f, 0, 99);
        _testSingleQuotesDefault(f, 0, 5);
        _testSingleQuotesDefault(f, 0, 3);
        _testSingleQuotesDefault(f, 0, 1);

        _testSingleQuotesDefault(f, 1, 99);
        _testSingleQuotesDefault(f, 1, 1);
    }

    private void _testSingleQuotesDefault(JsonFactory f,
            int offset, int readSize) throws Exception
    {
        // First, let's see that by default they are not allowed
        String JSON = "[ 'text' ]";
        AsyncReaderWrapper p = createParser(f, JSON, offset, readSize);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        } finally {
            p.close();
        }

        JSON = "{ 'a':1 }";
        p = createParser(f, JSON, offset, readSize);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        try {
            p.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        } finally {
            p.close();
        }
    }

    /**
     * Test to verify optional handling of
     * single quotes, to allow handling invalid (but, alas, common)
     * JSON.
     */
    public void testAposQuotingEnabled() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        _testAposQuotingEnabled(f, 0, 99);
        _testAposQuotingEnabled(f, 0, 5);
        _testAposQuotingEnabled(f, 0, 3);
        _testAposQuotingEnabled(f, 0, 2);
        _testAposQuotingEnabled(f, 0, 1);

        _testAposQuotingEnabled(f, 1, 99);
        _testAposQuotingEnabled(f, 2, 1);
        _testAposQuotingEnabled(f, 1, 1);
    }

    private void _testAposQuotingEnabled(JsonFactory f,
            int offset, int readSize) throws Exception
    {
        String UNINAME = String.format("Uni%c-key-%c", UNICODE_2BYTES, UNICODE_3BYTES);
        String UNIVALUE = String.format("Uni%c-value-%c", UNICODE_3BYTES, UNICODE_2BYTES);
        String JSON = String.format(
                "{ 'a' : 1, \"foobar\": 'b', '_abcde1234':'d', '\"' : '\"\"', '':'', '%s':'%s'}",
                UNINAME, UNIVALUE);
        AsyncReaderWrapper p = createParser(f, JSON, offset, readSize);
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals("1", p.currentText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("foobar", p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("b", p.currentText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("_abcde1234", p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("d", p.currentText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("\"", p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("\"\"", p.currentText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("", p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("", p.currentText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals(UNINAME, p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(UNIVALUE, p.currentText());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();

        JSON = "{'b':1,'array':[{'b':3}],'ob':{'b':4,'x':0,'y':'"+UNICODE_SEGMENT+"','a':false }}";
        p = createParser(f, JSON, offset, readSize);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(3, p.getIntValue());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_ARRAY, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("b", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(4, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("x", p.currentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(0, p.getIntValue());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("y", p.currentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(UNICODE_SEGMENT, p.currentText());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("a", p.currentName());
        assertToken(JsonToken.VALUE_FALSE, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    // test to verify that we implicitly allow escaping of apostrophe
    public void testSingleQuotesEscaped() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
                .build();
        _testSingleQuotesEscaped(f, 0, 99);
        _testSingleQuotesEscaped(f, 0, 5);
        _testSingleQuotesEscaped(f, 0, 3);
        _testSingleQuotesEscaped(f, 0, 1);

        _testSingleQuotesEscaped(f, 1, 99);
        _testSingleQuotesEscaped(f, 1, 1);
    }

    private void _testSingleQuotesEscaped(JsonFactory f,
            int offset, int readSize) throws Exception
    {
        String JSON = "[ '16\\'' ]";
        AsyncReaderWrapper p = createParser(f, JSON, offset, readSize);

        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("16'", p.currentText());
        assertToken(JsonToken.END_ARRAY, p.nextToken());
        p.close();
    }

    public void testNonStandardNameChars() throws Exception
    {
        final JsonFactory f = JsonFactory.builder()
                .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
                .build();
        _testNonStandardNameChars(f, 0, 99);
        _testNonStandardNameChars(f, 0, 6);
        _testNonStandardNameChars(f, 0, 3);
        _testNonStandardNameChars(f, 0, 1);

        _testNonStandardNameChars(f, 1, 99);
        _testNonStandardNameChars(f, 2, 1);
    }

    private void _testNonStandardNameChars(JsonFactory f,
            int offset, int readSize) throws Exception
    {
        String JSON = "{ @type : \"mytype\", #color : 123, *error* : true, "
            +" hyphen-ated : \"yes\", me+my : null"
            +"}";
        AsyncReaderWrapper p = createParser(f, JSON, offset, readSize);

        assertToken(JsonToken.START_OBJECT, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("@type", p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("mytype", p.currentText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("#color", p.currentText());
        assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
        assertEquals(123, p.getIntValue());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("*error*", p.currentText());
        assertToken(JsonToken.VALUE_TRUE, p.nextToken());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("hyphen-ated", p.currentText());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("yes", p.currentText());

        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("me+my", p.currentText());
        assertToken(JsonToken.VALUE_NULL, p.nextToken());

        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    public void testNonStandarBackslashQuotingForValues(int mode) throws Exception
    {
        _testNonStandarBackslashQuoting(0, 99);
        _testNonStandarBackslashQuoting(0, 6);
        _testNonStandarBackslashQuoting(0, 3);
        _testNonStandarBackslashQuoting(0, 1);

        _testNonStandarBackslashQuoting(2, 99);
        _testNonStandarBackslashQuoting(1, 1);
    }

    private void _testNonStandarBackslashQuoting(
            int offset, int readSize) throws Exception
    {
        // first: verify that we get an exception
        JsonFactory f = new JsonFactory();
        final String JSON = q("\\'");
        AsyncReaderWrapper p = createParser(f, JSON, offset, readSize);
        try {
            p.nextToken();
            p.currentText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized character escape");
        } finally {
            p.close();
        }
        // and then verify it's ok...
        f = f.rebuild()
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .build();
        p = createParser(f, JSON, offset, readSize);
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("'", p.currentText());
        p.close();
    }

    private AsyncReaderWrapper createParser(JsonFactory f, String doc,
            int offset, int readSize) throws IOException
    {
        return asyncForBytes(f, readSize, _jsonDoc(doc), offset);
    }
}
