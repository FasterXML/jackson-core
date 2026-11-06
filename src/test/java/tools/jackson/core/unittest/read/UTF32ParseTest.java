package tools.jackson.core.unittest.read;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;
import tools.jackson.core.unittest.testutil.ThrottledInputStream;

import static org.junit.jupiter.api.Assertions.fail;

// Tests from [jackson-core#382]
class UTF32ParseTest extends JacksonCoreTestBase
{
    private final JsonFactory FACTORY = newStreamFactory();

    @Test
    void simpleEOFs() throws Exception
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

    @Test
    void simpleInvalidUTF32() throws Exception
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
            verifyException(e, "Invalid UTF-32 character 0xfeff0001");
            verifyException(e, "byte #4");
        }
        parser.close();
    }

    // Surrogate code points are not valid UTF-32, same as with UTF-8
    @Test
    void surrogateCodeUnitsInUTF32() throws Exception
    {
        // String value with D83D, DE00 as separate UTF-32BE code units
        byte[] data = {
                0x00, 0x00, 0x00, '"',
                0x00, 0x00, (byte) 0xD8, 0x3D,
                0x00, 0x00, (byte) 0xDE, 0x00,
                0x00, 0x00, 0x00, '"'
        };
        _verifySurrogateFail(data, "Invalid UTF-32 character 0xd83d", "byte #4");

        // and lone low surrogate in UTF-32LE
        data = new byte[] {
                '"', 0x00, 0x00, 0x00,
                0x00, (byte) 0xDC, 0x00, 0x00,
                '"', 0x00, 0x00, 0x00
        };
        _verifySurrogateFail(data, "Invalid UTF-32 character 0xdc00", "byte #4");
    }

    @Test
    void surrogateInPropertyName() throws Exception
    {
        // {"<D800>":1} in UTF-32BE
        byte[] data = utf32BE("{\"", 0xD800, "\":1}");
        _verifySurrogateFail(data, "Invalid UTF-32 character 0xd800", "byte #8");
    }

    // Surrogate after buffer refill(s)
    @Test
    void surrogateAfterLongPrefix() throws Exception
    {
        final int prefixLen = 5000;
        byte[] data = utf32BE("\"" + "a".repeat(prefixLen), 0xDFFF, "\"");
        _verifySurrogateFail(data, "Invalid UTF-32 character 0xdfff",
                "byte #" + (4 * (prefixLen + 1)));
    }

    private void _verifySurrogateFail(byte[] data, String... msgs) throws Exception
    {
        _verifySurrogateFail(FACTORY.createParser(ObjectReadContext.empty(), data), msgs);
        _verifySurrogateFail(FACTORY.createParser(ObjectReadContext.empty(),
                new ByteArrayInputStream(data)), msgs);
        _verifySurrogateFail(FACTORY.createParser(ObjectReadContext.empty(),
                new ThrottledInputStream(data, 1)), msgs);
        _verifySurrogateFail(FACTORY.createParser(ObjectReadContext.empty(),
                new ThrottledInputStream(data, 3)), msgs);
    }

    private void _verifySurrogateFail(JsonParser p, String... msgs) throws Exception
    {
        try (JsonParser parser = p) {
            while (parser.nextToken() != null) {
                parser.getString();
            }
            fail("Should not pass");
        } catch (JacksonException e) {
            for (String msg : msgs) {
                verifyException(e, msg);
            }
        }
    }

    private static byte[] utf32BE(String prefix, int codeUnit, String suffix)
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (char c : prefix.toCharArray()) {
            _writeBE(out, c);
        }
        _writeBE(out, codeUnit);
        for (char c : suffix.toCharArray()) {
            _writeBE(out, c);
        }
        return out.toByteArray();
    }

    private static void _writeBE(ByteArrayOutputStream out, int ch) {
        out.write(ch >> 24);
        out.write(ch >> 16);
        out.write(ch >> 8);
        out.write(ch);
    }

    @Test
    void simpleSevenNullBytes() throws Exception {
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
