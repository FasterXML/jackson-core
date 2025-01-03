package tools.jackson.core;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.testsupport.MockDataInput;
import tools.jackson.core.testsupport.TestSupport;
import tools.jackson.core.testsupport.ThrottledInputStream;
import tools.jackson.core.testsupport.ThrottledReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Replacement of JUnit4-based {@code BaseTest}
 */
public class JUnit5TestBase
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
    /**********************************************************
    /* Some sample documents:
    /**********************************************************
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
    /**********************************************************
    /* Helper classes (beans)
    /**********************************************************
     */

    protected final static JsonFactory JSON_FACTORY = new JsonFactory();




    /*
    /**********************************************************************
    /* Factory methods
    /**********************************************************************
     */


    public static JsonFactory sharedStreamFactory() {
        return JSON_FACTORY;
    }

    protected JsonFactory newStreamFactory() {
        return new JsonFactory();
    }

    protected JsonFactoryBuilder streamFactoryBuilder() {
        return (JsonFactoryBuilder) JsonFactory.builder();
    }

    /*
    /**********************************************************
    /* Parser construction
    /**********************************************************
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
            return f.createParser(testObjectReadContext(),
                    new ThrottledInputStream(utf8Bytes(doc), 1));
        case MODE_READER:
            return createParserUsingReader(f, doc);
        case MODE_READER_THROTTLED:
            return f.createParser(testObjectReadContext(),
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
            return f.createParser(testObjectReadContext(),
                    new ByteArrayInputStream(doc));
        case MODE_INPUT_STREAM_THROTTLED:
            return f.createParser(testObjectReadContext(),
                    new ThrottledInputStream(doc, 1));
        case MODE_READER:
            return f.createParser(testObjectReadContext(),
                    new StringReader(new String(doc, StandardCharsets.UTF_8)));
        case MODE_READER_THROTTLED:
            return f.createParser(testObjectReadContext(),
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
        return f.createParser(testObjectReadContext(), new StringReader(input));
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
        return f.createParser(testObjectReadContext(), is);
    }

    protected JsonParser createParserForDataInput(TokenStreamFactory f,
            DataInput input)
    {
        return f.createParser(testObjectReadContext(), input);
    }

    /*
    /**********************************************************
    /* Generator construction
    /**********************************************************
     */

    public static JsonGenerator createGenerator(OutputStream out) throws IOException {
        return createGenerator(JSON_FACTORY, out);
    }

    public static JsonGenerator createGenerator(TokenStreamFactory f, OutputStream out) throws IOException {
        return f.createGenerator(ObjectWriteContext.empty(), out);
    }

    public static JsonGenerator createGenerator(Writer w) throws IOException {
        return createGenerator(JSON_FACTORY, w);
    }

    public static JsonGenerator createGenerator(TokenStreamFactory f, Writer w) throws IOException {
        return f.createGenerator(ObjectWriteContext.empty(), w);
    }

    /*
    /**********************************************************************
    /* Helper type construction
    /**********************************************************************
     */

    public static IOContext testIOContext() {
        return TestSupport.testIOContext();
    }

    protected void writeJsonDoc(JsonFactory f, String doc, JsonGenerator g) throws IOException
    {
        try (JsonParser p = f.createParser(ObjectReadContext.empty(), a2q(doc))) {
            while (p.nextToken() != null) {
                g.copyCurrentStructure(p);
            }
            g.close();
        }
    }

    /*
    /**********************************************************************
    /* Assertions
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

    /**
     * @param e Exception to check
     * @param anyMatches Array of Strings of which AT LEAST ONE ("any") has to be included
     *    in {@code e.getMessage()} -- using case-INSENSITIVE comparison
     */
    public static void verifyException(Throwable e, String... anyMatches)
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
    public static String getAndVerifyText(JsonParser p)
    {
        // Ok, let's verify other accessors
        int actLen = p.getStringLength();
        char[] ch = p.getStringCharacters();
        String str2 = new String(ch, p.getStringOffset(), actLen);
        String str = p.getString();

        if (str.length() !=  actLen) {
            fail("Internal problem (p.token == "+p.currentToken()+"): p.getText().length() ['"+str+"'] == "+str.length()+"; p.getTextLength() == "+actLen);
        }
        assertEquals(str, str2, "String access via getText(), getTextXxx() must be the same");

        return str;
    }

    /*
    /**********************************************************************
    /* Escaping/quoting
    /**********************************************************************
     */

    protected String q(String str) {
        return '"'+str+'"';
    }

    public static String a2q(String json) {
        return json.replace('\'', '"');
    }

    public static byte[] encodeInUTF32BE(String input)
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

    /*
    /**********************************************************************
    /* Misc other
    /**********************************************************************
     */

    protected ObjectReadContext testObjectReadContext() {
        return ObjectReadContext.empty();
    }

    protected static byte[] utf8Bytes(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    protected String utf8String(ByteArrayOutputStream bytes) {
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    public static String fieldNameFor(int index)
    {
        StringBuilder sb = new StringBuilder(16);
        fieldNameFor(sb, index);
        return sb.toString();
    }

    private static void fieldNameFor(StringBuilder sb, int index)
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

    protected int[] calcQuads(String word) {
        return calcQuads(utf8Bytes(word));
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

    /*
    /**********************************************************************
    /* Content reading, serialization
    /**********************************************************************
     */

    public static byte[] readResource(String ref)
    {
       ByteArrayOutputStream bytes = new ByteArrayOutputStream();
       final byte[] buf = new byte[4000];

       InputStream in = JUnit5TestBase.class.getResourceAsStream(ref);
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

    public static byte[] jdkSerialize(Object o) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(1000);
        ObjectOutputStream obOut = new ObjectOutputStream(bytes);
        obOut.writeObject(o);
        obOut.close();
        return bytes.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static <T> T jdkDeserialize(byte[] raw) throws IOException
    {
        ObjectInputStream objIn = new ObjectInputStream(new ByteArrayInputStream(raw));
        try {
            return (T) objIn.readObject();
        } catch (ClassNotFoundException e) {
            fail("Missing class: "+e.getMessage());
            return null;
        } finally {
            objIn.close();
        }
    }

}
