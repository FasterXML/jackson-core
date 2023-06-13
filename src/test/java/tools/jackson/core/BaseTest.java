package tools.jackson.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.testsupport.MockDataInput;
import tools.jackson.core.testsupport.ThrottledInputStream;
import tools.jackson.core.testsupport.ThrottledReader;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.Named;

import junit.framework.TestCase;

@SuppressWarnings("resource")
public abstract class BaseTest
    extends TestCase
{
    protected final static String FIELD_BASENAME = "f";

    protected final static int MODE_INPUT_STREAM = 0;
    protected final static int MODE_INPUT_STREAM_THROTTLED = 1;
    protected final static int MODE_READER = 2;
    protected final static int MODE_READER_THROTTLED = 3;
    protected final static int MODE_DATA_INPUT = 4;

    protected final static int[] ALL_MODES = new int[] {
        MODE_INPUT_STREAM,
        MODE_INPUT_STREAM_THROTTLED,
        MODE_READER,
        MODE_READER_THROTTLED,
        MODE_DATA_INPUT
    };

    protected final static int[] ALL_BINARY_MODES = new int[] {
        MODE_INPUT_STREAM,
        MODE_INPUT_STREAM_THROTTLED,
        MODE_DATA_INPUT
    };

    protected final static int[] ALL_TEXT_MODES = new int[] {
        MODE_READER,
        MODE_READER_THROTTLED
    };

    // DataInput not streaming
    protected final static int[] ALL_STREAMING_MODES = new int[] {
        MODE_INPUT_STREAM,
        MODE_INPUT_STREAM_THROTTLED,
        MODE_READER,
        MODE_READER_THROTTLED
    };

    /*
    /**********************************************************************
    /* Some sample documents
    /**********************************************************************
     */

    protected final static int SAMPLE_SPEC_VALUE_WIDTH = 800;
    protected final static int SAMPLE_SPEC_VALUE_HEIGHT = 600;
    protected final static String SAMPLE_SPEC_VALUE_TITLE = "View from 15th Floor";
    protected final static String SAMPLE_SPEC_VALUE_TN_URL = "http://www.example.com/image/481989943";
    protected final static int SAMPLE_SPEC_VALUE_TN_HEIGHT = 125;
    protected final static String SAMPLE_SPEC_VALUE_TN_WIDTH = "100";
    protected final static int SAMPLE_SPEC_VALUE_TN_ID1 = 116;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID2 = 943;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID3 = 234;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID4 = 38793;

    protected final static String SAMPLE_DOC_JSON_SPEC =
        "{\n"
        +"  \"Image\" : {\n"
        +"    \"Width\" : "+SAMPLE_SPEC_VALUE_WIDTH+",\n"
        +"    \"Height\" : "+SAMPLE_SPEC_VALUE_HEIGHT+","
        +"\"Title\" : \""+SAMPLE_SPEC_VALUE_TITLE+"\",\n"
        +"    \"Thumbnail\" : {\n"
        +"      \"Url\" : \""+SAMPLE_SPEC_VALUE_TN_URL+"\",\n"
        +"\"Height\" : "+SAMPLE_SPEC_VALUE_TN_HEIGHT+",\n"
        +"      \"Width\" : \""+SAMPLE_SPEC_VALUE_TN_WIDTH+"\"\n"
        +"    },\n"
        +"    \"IDs\" : ["+SAMPLE_SPEC_VALUE_TN_ID1+","+SAMPLE_SPEC_VALUE_TN_ID2+","+SAMPLE_SPEC_VALUE_TN_ID3+","+SAMPLE_SPEC_VALUE_TN_ID4+"]\n"
        +"  }"
        +"}"
        ;

    /*
    /**********************************************************************
    /* Helper classes (beans)
    /**********************************************************************
     */

    /**
     * Sample class from Jackson tutorial ("JacksonInFiveMinutes")
     */
    protected static class FiveMinuteUser {
        public enum Gender { MALE, FEMALE };

        public static class Name
        {
          private String _first, _last;

          public Name() { }
          public Name(String f, String l) {
              _first = f;
              _last = l;
          }

          public String getFirst() { return _first; }
          public String getLast() { return _last; }

          public void setFirst(String s) { _first = s; }
          public void setLast(String s) { _last = s; }

          @Override
          public boolean equals(Object o)
          {
              if (o == this) return true;
              if (o == null || o.getClass() != getClass()) return false;
              Name other = (Name) o;
              return _first.equals(other._first) && _last.equals(other._last);
          }
        }

        private Gender _gender;
        private Name _name;
        private boolean _isVerified;
        private byte[] _userImage;

        public FiveMinuteUser() { }

        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data)
        {
            _name = new Name(first, last);
            _isVerified = verified;
            _gender = g;
            _userImage = data;
        }

        public Name getName() { return _name; }
        public boolean isVerified() { return _isVerified; }
        public Gender getGender() { return _gender; }
        public byte[] getUserImage() { return _userImage; }

        public void setName(Name n) { _name = n; }
        public void setVerified(boolean b) { _isVerified = b; }
        public void setGender(Gender g) { _gender = g; }
        public void setUserImage(byte[] b) { _userImage = b; }

        @Override
        public boolean equals(Object o)
        {
            if (o == this) return true;
            if (o == null || o.getClass() != getClass()) return false;
            FiveMinuteUser other = (FiveMinuteUser) o;
            if (_isVerified != other._isVerified) return false;
            if (_gender != other._gender) return false;
            if (!_name.equals(other._name)) return false;
            byte[] otherImage = other._userImage;
            if (otherImage.length != _userImage.length) return false;
            for (int i = 0, len = _userImage.length; i < len; ++i) {
                if (_userImage[i] != otherImage[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    protected final JsonFactory JSON_FACTORY = new JsonFactory();

    /*
    /**********************************************************************
    /* High-level helpers
    /**********************************************************************
     */

    protected void verifyJsonSpecSampleDoc(JsonParser p, boolean verifyContents)
    {
        verifyJsonSpecSampleDoc(p, verifyContents, true);
    }

    protected void verifyJsonSpecSampleDoc(JsonParser p, boolean verifyContents,
            boolean requireNumbers)
    {
        if (!p.hasCurrentToken()) {
            p.nextToken();
        }
        // first basic check, mostly for test coverage
        assertNull(p.getTypeId());
        assertNull(p.getObjectId());

        assertToken(JsonToken.START_OBJECT, p.currentToken()); // main object

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Image'
        if (verifyContents) {
            verifyFieldName(p, "Image");
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken()); // 'image' object

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(p, "Width");
        }

        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_WIDTH);
        }

        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(p, "Height");
        }

        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_HEIGHT);
        }
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Title'
        if (verifyContents) {
            verifyFieldName(p, "Title");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TITLE, getAndVerifyText(p));
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Thumbnail'
        if (verifyContents) {
            verifyFieldName(p, "Thumbnail");
        }

        assertToken(JsonToken.START_OBJECT, p.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Url'
        if (verifyContents) {
            verifyFieldName(p, "Url");
        }
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, getAndVerifyText(p));
        }
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(p, "Height");
        }
        verifyIntToken(p.nextToken(), requireNumbers);
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_HEIGHT);
        }
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(p, "Width");
        }
        // Width value is actually a String in the example
        assertToken(JsonToken.VALUE_STRING, p.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, getAndVerifyText(p));
        }

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.PROPERTY_NAME, p.nextToken()); // 'IDs'
        assertToken(JsonToken.START_ARRAY, p.nextToken()); // 'ids' array
        verifyIntToken(p.nextToken(), requireNumbers); // ids[0]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID1);
        }
        verifyIntToken(p.nextToken(), requireNumbers); // ids[1]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID2);
        }
        verifyIntToken(p.nextToken(), requireNumbers); // ids[2]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID3);
        }
        verifyIntToken(p.nextToken(), requireNumbers); // ids[3]
        if (verifyContents) {
            verifyIntValue(p, SAMPLE_SPEC_VALUE_TN_ID4);
        }
        assertToken(JsonToken.END_ARRAY, p.nextToken()); // 'ids' array

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // 'image' object

        assertToken(JsonToken.END_OBJECT, p.nextToken()); // main object
    }

    private void verifyIntToken(JsonToken t, boolean requireNumbers)
    {
        if (t == JsonToken.VALUE_NUMBER_INT) {
            return;
        }
        if (requireNumbers) { // to get error
            assertToken(JsonToken.VALUE_NUMBER_INT, t);
        }
        // if not number, must be String
        if (t != JsonToken.VALUE_STRING) {
            fail("Expected INT or STRING value, got "+t);
        }
    }

    protected void verifyFieldName(JsonParser p, String expName)
    {
        assertEquals(expName, p.getText());
        assertEquals(expName, p.currentName());
    }

    protected void verifyIntValue(JsonParser p, long expValue)
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), p.getText());
    }

    /*
    /**********************************************************************
    /* Parser construction
    /**********************************************************************
     */

    protected JsonParser createParser(int mode, String doc) {
        return createParser(JSON_FACTORY, mode, doc);
    }

    protected JsonParser createParser(int mode, byte[] doc) {
        return createParser(JSON_FACTORY, mode, doc);
    }

    protected JsonParser createParser(TokenStreamFactory f, int mode, String doc)
    {
        switch (mode) {
        case MODE_INPUT_STREAM:
            return createParserUsingStream(f, doc, "UTF-8");
        case MODE_INPUT_STREAM_THROTTLED:
            return f.createParser(ObjectReadContext.empty(),
                    new ThrottledInputStream(utf8Bytes(doc), 1));
        case MODE_READER:
            return createParserUsingReader(f, doc);
        case MODE_READER_THROTTLED:
            return f.createParser(ObjectReadContext.empty(),
                    new ThrottledReader(doc, 1));
        case MODE_DATA_INPUT:
            return createParserForDataInput(f, new MockDataInput(doc));
        default:
        }
        throw new RuntimeException("internal error");
    }

    protected JsonParser createParser(TokenStreamFactory f, int mode, byte[] doc)
    {
        switch (mode) {
        case MODE_INPUT_STREAM:
            return f.createParser(ObjectReadContext.empty(), new ByteArrayInputStream(doc));
        case MODE_INPUT_STREAM_THROTTLED:
            return f.createParser(ObjectReadContext.empty(),
                    new ThrottledInputStream(doc, 1));
        case MODE_READER:
            return f.createParser(ObjectReadContext.empty(),
                    new StringReader(new String(doc, StandardCharsets.UTF_8)));
        case MODE_READER_THROTTLED:
            return f.createParser(ObjectReadContext.empty(),
                    new ThrottledReader(new String(doc, StandardCharsets.UTF_8), 1));
        case MODE_DATA_INPUT:
            return createParserForDataInput(f, new MockDataInput(doc));
        default:
        }
        throw new RuntimeException("internal error");
    }

    protected JsonParser createParserUsingReader(String input)
    {
        return createParserUsingReader(new JsonFactory(), input);
    }

    protected JsonParser createParserUsingReader(TokenStreamFactory f, String input)
    {
        return f.createParser(ObjectReadContext.empty(), new StringReader(input));
    }

    protected JsonParser createParserUsingStream(String input, String encoding)
    {
        return createParserUsingStream(new JsonFactory(), input, encoding);
    }

    protected JsonParser createParserUsingStream(TokenStreamFactory f,
            String input, String encoding)
    {

        /* 23-Apr-2008, tatus: UTF-32 is not supported by JDK, have to
         *   use our own codec too (which is not optimal since there's
         *   a chance both encoder and decoder might have bugs, but ones
         *   that cancel each other out or such)
         */
        byte[] data;
        if (encoding.equalsIgnoreCase("UTF-32")) {
            data = encodeInUTF32BE(input);
        } else {
            try {
                data = input.getBytes(encoding);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        InputStream is = new ByteArrayInputStream(data);
        return f.createParser(ObjectReadContext.empty(), is);
    }

    protected JsonParser createParserForDataInput(TokenStreamFactory f,
            DataInput input)
    {
        return f.createParser(ObjectReadContext.empty(), input);
    }

    /*
    /**********************************************************************
    /* Generator construction
    /**********************************************************************
     */

    protected JsonGenerator createGenerator(OutputStream out) {
        return createGenerator(JSON_FACTORY, out);
    }

    protected JsonGenerator createGenerator(TokenStreamFactory f, OutputStream out) {
        return f.createGenerator(ObjectWriteContext.empty(), out);
    }

    protected JsonGenerator createGenerator(Writer w) {
        return createGenerator(JSON_FACTORY, w);
    }

    protected JsonGenerator createGenerator(TokenStreamFactory f, Writer w) {
        return f.createGenerator(ObjectWriteContext.empty(), w);
    }

    /*
    /**********************************************************************
    /* Helper read/write methods
    /**********************************************************************
     */

    protected void writeJsonDoc(JsonFactory f, String doc, Writer w)
    {
        writeJsonDoc(f, doc, f.createGenerator(ObjectWriteContext.empty(), w));
    }

    protected void writeJsonDoc(JsonFactory f, String doc, JsonGenerator g)
    {
        JsonParser p = f.createParser(ObjectReadContext.empty(), a2q(doc));

        while (p.nextToken() != null) {
            g.copyCurrentStructure(p);
        }
        p.close();
        g.close();
    }

    /*
    /**********************************************************************
    /* Additional assertion methods
    /**********************************************************************
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    protected void assertToken(JsonToken expToken, JsonParser p)
    {
        assertToken(expToken, p.currentToken());
    }

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

    /**
     * @param e Exception to check
     * @param anyMatches Array of Strings of which AT LEAST ONE ("any") has to be included
     *    in {@code e.getMessage()} -- using case-INSENSITIVE comparison
     */
    protected void verifyException(Throwable e, String... anyMatches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : anyMatches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(anyMatches)+"): got one with message \""+msg+"\"");
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser p)
    {
        // Ok, let's verify other accessors
        int actLen = p.getTextLength();
        char[] ch = p.getTextCharacters();
        String str2 = new String(ch, p.getTextOffset(), actLen);
        String str = p.getText();

        if (str.length() !=  actLen) {
            fail("Internal problem (p.token == "+p.currentToken()+"): p.getText().length() ['"+str+"'] == "+str.length()+"; p.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    /*
    /**********************************************************************
    /* And other helpers
    /**********************************************************************
     */

    protected static String q(String str) {
        return '"'+str+'"';
    }

    protected static String a2q(String json) {
        return json.replace("'", "\"");
    }

    protected byte[] encodeInUTF32BE(String input)
    {
        int len = input.length();
        byte[] result = new byte[len * 4];
        int ptr = 0;
        for (int i = 0; i < len; ++i, ptr += 4) {
            char c = input.charAt(i);
            result[ptr] = result[ptr+1] = (byte) 0;
            result[ptr+2] = (byte) (c >> 8);
            result[ptr+3] = (byte) c;
        }
        return result;
    }

    protected static byte[] utf8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    protected static String utf8String(ByteArrayOutputStream bytes) {
        try {
            return bytes.toString(StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected byte[] readResource(String ref)
    {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       final byte[] buf = new byte[4000];

       InputStream in = getClass().getResourceAsStream(ref);
       if (in != null) {
           try {
               int len;
               while ((len = in.read(buf)) > 0) {
                   bytes.write(buf, 0, len);
               }
               in.close();
           } catch (IOException e) {
               throw new RuntimeException("Failed to read resource '"+ref+"': "+e);
           }
       }
       if (bytes.size() == 0) {
           throw new IllegalArgumentException("Failed to read resource '"+ref+"': empty resource?");
       }
       return bytes.toByteArray();
    }

    protected void fieldNameFor(StringBuilder sb, int index)
    {
        /* let's do something like "f1.1" to exercise different
         * field names (important for byte-based codec)
         * Other name shuffling done mostly just for fun... :)
         */
        sb.append(FIELD_BASENAME);
        sb.append(index);
        if (index > 50) {
            sb.append('.');
            if (index > 200) {
                sb.append(index);
                if (index > 4000) { // and some even longer symbols...
                    sb.append(".").append(index);
                }
            } else {
                sb.append(index >> 3); // divide by 8
            }
        }
    }

    protected JsonFactory sharedStreamFactory() {
        return JSON_FACTORY;
    }

    protected JsonFactory newStreamFactory() {
        return new JsonFactory();
    }

    protected JsonFactoryBuilder streamFactoryBuilder() {
        return JsonFactory.builder();
    }

    protected IOContext ioContextForTests(Object source) {
        return new IOContext(StreamReadConstraints.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference(source), true,
                JsonEncoding.UTF8);
    }

    protected String fieldNameFor(int index)
    {
        StringBuilder sb = new StringBuilder(16);
        fieldNameFor(sb, index);
        return sb.toString();
    }

    protected int[] calcQuads(String word) {
        try {
            return calcQuads(word.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected int[] calcQuads(byte[] wordBytes) {
        int blen = wordBytes.length;
        int[] result = new int[(blen + 3) / 4];
        for (int i = 0; i < blen; ++i) {
            int x = wordBytes[i] & 0xFF;

            if (++i < blen) {
                x = (x << 8) | (wordBytes[i] & 0xFF);
                if (++i < blen) {
                    x = (x << 8) | (wordBytes[i] & 0xFF);
                    if (++i < blen) {
                        x = (x << 8) | (wordBytes[i] & 0xFF);
                    }
                }
            }
            result[i >> 2] = x;
        }
        return result;
    }

    public static List<Named> namedFromStrings(Collection<String> input) {
        ArrayList<Named> result = new ArrayList<>(input.size());
        for (String str : input) {
            result.add(Named.fromString(str));
        }
        return result;
    }

    public static List<Named> namedFromStrings(String... input) {
        return namedFromStrings(Arrays.asList(input));
    }
}
