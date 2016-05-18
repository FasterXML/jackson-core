package com.fasterxml.jackson.core.read;


import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.SerializedString;

import java.io.*;
import java.util.Random;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class UTF8NamesParseTest
    extends BaseTest
{
    final static String[] UTF8_2BYTE_STRINGS = new String[] {
        /* This may look funny, but UTF8 scanner has fairly
         * elaborate decoding machinery, and it is indeed
         * necessary to try out various combinations...
         */
        "b", "A\u00D8", "abc", "c3p0",
        "12345", "......", "Long\u00FAer",
        "Latin1-fully-\u00BE-develop\u00A8d",
        "Some very long name, ridiculously long actually to see that buffer expansion works: \u00BF?"
    };

    final static String[] UTF8_3BYTE_STRINGS = new String[] {
        "\uC823?", "A\u400F", "1\u1234?",
        "Ab123\u4034",
        "Even-longer:\uC023"
    };

    public void testEmptyName() throws Exception
    {
        _testEmptyName(MODE_INPUT_STREAM);
        _testEmptyName(MODE_INPUT_STREAM_THROTTLED);
        _testEmptyName(MODE_DATA_INPUT);
    }

    private void _testEmptyName(int mode) throws Exception
    {
        final String DOC = "{ \"\" : \"\" }";
        JsonParser p = createParser(mode, DOC);
        assertToken(JsonToken.START_OBJECT, p.nextToken());
        assertToken(JsonToken.FIELD_NAME, p.nextToken());
        assertEquals("", p.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals("", p.getText());
        assertToken(JsonToken.END_OBJECT, p.nextToken());
        p.close();
    }

    public void testUtf8Name2Bytes() throws Exception
    {
        _testUtf8Name2Bytes(MODE_INPUT_STREAM);
        _testUtf8Name2Bytes(MODE_INPUT_STREAM_THROTTLED);
        _testUtf8Name2Bytes(MODE_DATA_INPUT);
    }

    private void _testUtf8Name2Bytes(int mode) throws Exception
    {
        final String[] NAMES = UTF8_2BYTE_STRINGS;

        for (int i = 0; i < NAMES.length; ++i) {
            String NAME = NAMES[i];
            String DOC = "{ \""+NAME+"\" : 0 }";
            JsonParser p = createParser(mode, DOC);
            assertToken(JsonToken.START_OBJECT, p.nextToken());

            assertToken(JsonToken.FIELD_NAME, p.nextToken());

            assertTrue(p.hasToken(JsonToken.FIELD_NAME));
            assertTrue(p.hasTokenId(JsonTokenId.ID_FIELD_NAME));
            
            assertEquals(NAME, p.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertTrue(p.hasToken(JsonToken.VALUE_NUMBER_INT));
            assertTrue(p.hasTokenId(JsonTokenId.ID_NUMBER_INT));

            // should retain name during value entry, too
            assertEquals(NAME, p.getCurrentName());
            
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            p.close();
        }
    }

    public void testUtf8Name3Bytes() throws Exception
    {
        _testUtf8Name3Bytes(MODE_INPUT_STREAM);
        _testUtf8Name3Bytes(MODE_DATA_INPUT);
        _testUtf8Name3Bytes(MODE_INPUT_STREAM_THROTTLED);
    }

    public void _testUtf8Name3Bytes(int mode) throws Exception
    {
        final String[] NAMES = UTF8_3BYTE_STRINGS;

        for (int i = 0; i < NAMES.length; ++i) {
            String NAME = NAMES[i];
            String DOC = "{ \""+NAME+"\" : true }";

            JsonParser p = createParser(mode, DOC);
            assertToken(JsonToken.START_OBJECT, p.nextToken());
            
            assertToken(JsonToken.FIELD_NAME, p.nextToken());
            assertEquals(NAME, p.getCurrentName());
            assertToken(JsonToken.VALUE_TRUE, p.nextToken());
            assertEquals(NAME, p.getCurrentName());
            
            assertToken(JsonToken.END_OBJECT, p.nextToken());
            
            p.close();
        }
    }

    // How about tests for Surrogate-Pairs?

    public void testUtf8StringTrivial() throws Exception
    {
        _testUtf8StringTrivial(MODE_INPUT_STREAM);
        _testUtf8StringTrivial(MODE_DATA_INPUT);
        _testUtf8StringTrivial(MODE_INPUT_STREAM_THROTTLED);
    }
    
    public void _testUtf8StringTrivial(int mode) throws Exception
    {
        String[] VALUES = UTF8_2BYTE_STRINGS;
        for (int i = 0; i < VALUES.length; ++i) {
            String VALUE = VALUES[i];
            String DOC = "[ \""+VALUE+"\" ]";
            JsonParser p = createParser(mode, DOC);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            String act = getAndVerifyText(p);
            if (act.length() != VALUE.length()) {
                fail("Failed for value #"+(i+1)+"/"+VALUES.length+": length was "+act.length()+", should be "+VALUE.length());
            }
            assertEquals(VALUE, act);
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }

        VALUES = UTF8_3BYTE_STRINGS;
        for (int i = 0; i < VALUES.length; ++i) {
            String VALUE = VALUES[i];
            String DOC = "[ \""+VALUE+"\" ]";
            JsonParser p = createParser(mode, DOC);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_STRING, p.nextToken());
            assertEquals(VALUE, getAndVerifyText(p));
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            p.close();
        }
    }

    public void testUtf8StringValue() throws Exception
    {
        _testUtf8StringValue(MODE_INPUT_STREAM);
        _testUtf8StringValue(MODE_DATA_INPUT);
        _testUtf8StringValue(MODE_INPUT_STREAM_THROTTLED);
    }

    public void _testUtf8StringValue(int mode) throws Exception
    {
        Random r = new Random(13);
        //int LEN = 72000;
        int LEN = 720;
        StringBuilder sb = new StringBuilder(LEN + 20);
        while (sb.length() < LEN) {
            int c;
            if (r.nextBoolean()) { // ascii
                c = 32 + (r.nextInt() & 0x3F);
                if (c == '"' || c == '\\') {
                    c = ' ';
                }
            } else if (r.nextBoolean()) { // 2-byte
                c = 160 + (r.nextInt() & 0x3FF);
            } else if (r.nextBoolean()) { // 3-byte (non-surrogate)
                c = 8000 + (r.nextInt() & 0x7FFF);
            } else { // surrogates (2 chars)
                int value = r.nextInt() & 0x3FFFF; // 20-bit, ~ 1 million
                sb.append((char) (0xD800 + (value >> 10)));
                c = (0xDC00 + (value & 0x3FF));

            }
            sb.append((char) c);
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(LEN);
        OutputStreamWriter out = new OutputStreamWriter(bout, "UTF-8");
        out.write("[\"");
        String VALUE = sb.toString();
        out.write(VALUE);
        out.write("\"]");
        out.close();

        byte[] data = bout.toByteArray();

        JsonParser p = createParser(mode, data);
        assertToken(JsonToken.START_ARRAY, p.nextToken());
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        String act = p.getText();

        assertEquals(VALUE.length(), act.length());
        assertEquals(VALUE, act);
        p.close();
    }

    public void testNextFieldName() throws IOException
    {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		os.write('{');
		for (int i = 0; i < 3994; i++) {
			os.write(' ');
		}
		os.write("\"id\":2".getBytes("UTF-8"));
		os.write('}');
		byte[] data = os.toByteArray();

		_testNextFieldName(MODE_INPUT_STREAM, data);
          _testNextFieldName(MODE_DATA_INPUT, data);
          _testNextFieldName(MODE_INPUT_STREAM_THROTTLED, data);
    }

    private void _testNextFieldName(int mode, byte[] doc) throws IOException
    {
        SerializedString id = new SerializedString("id");
        JsonParser parser = createParser(mode, doc);
        assertEquals(parser.nextToken(), JsonToken.START_OBJECT);
        assertTrue(parser.nextFieldName(id));
        assertEquals(parser.nextToken(), JsonToken.VALUE_NUMBER_INT);
        assertEquals(parser.nextToken(), JsonToken.END_OBJECT);
        parser.close();
    }
}
