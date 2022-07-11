package tools.jackson.core.read;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;

// Tests from [jackson-core#382]
public class UTF32ParseTest extends BaseTest
{
    private final JsonFactory FACTORY = newStreamFactory();

    public void testSimpleEOFs() throws Exception
    {
        // 2 spaces
        byte[] data = { 0x00, 0x00, 0x00, 0x20,
                0x00, 0x00, 0x00, 0x20
        };

        for (int len = 5; len <= 7; ++len) {
            JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), data, 0, len);
            try {
                parser.nextToken();
                fail("Should not pass");
            } catch (JacksonException e) {
                verifyException(e, "Unexpected EOF");
                verifyException(e, "of a 4-byte UTF-32 char");
            }
            parser.close();
        }
    }

    public void testSimpleInvalidUTF32()
    {
        // 2 characters, space, then something beyond valid Unicode set
        byte[] data = {
                0x00,
                0x00,
                0x00,
                0x20,
                (byte) 0xFE,
                (byte) 0xFF,
                0x00,
                0x01
        };

        JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), data);

        try {
            parser.nextToken();
            fail("Should not pass");
        } catch (JacksonException e) {
            verifyException(e, "Invalid UTF-32 character 0xfefe0001");
        }
        parser.close();
    }

    public void testSimpleSevenNullBytes()
    {
        byte[] data = new byte[7];
        JsonParser parser = FACTORY.createParser(ObjectReadContext.empty(), data);
        try {
            parser.nextToken();
            fail("Should not pass");
        } catch (StreamReadException e) {
            verifyException(e, "Illegal character ((CTRL-CHAR, code 0))");
        }
        parser.close();
    }
}
