package tools.jackson.core.unittest.base64;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Base64Variant;
import tools.jackson.core.Base64Variants;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.*;

class Base64CodecTest
    extends JacksonCoreTestBase
{
    @Test
    void variantAccess()
    {
        for (Base64Variant var : new Base64Variant[] {
                Base64Variants.MIME,
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.PEM
        }) {
            assertSame(var, Base64Variants.valueOf(var.getName()));
        }

        try {
            Base64Variants.valueOf("foobar");
            fail("Should not pass");
        } catch (IllegalArgumentException e) {
            verifyException(e, "No Base64Variant with name 'foobar'");
        }
    }

    @Test
    void props()
    {
        Base64Variant std = Base64Variants.MIME;
        // let's verify basic props of std cocec
        assertEquals("MIME", std.getName());
        assertEquals("MIME", std.toString());
        assertTrue(std.usesPadding());
        assertFalse(std.usesPaddingChar('X'));
        assertEquals('=', std.getPaddingChar());
        assertTrue(std.usesPaddingChar('='));
        assertEquals((byte) '=', std.getPaddingByte());
        assertEquals(76, std.getMaxLineLength());
    }

    @Test
    void charEncoding() throws Exception
    {
        Base64Variant std = Base64Variants.MIME;
        assertEquals(Base64Variant.BASE64_VALUE_INVALID, std.decodeBase64Char('?'));
        assertEquals(Base64Variant.BASE64_VALUE_INVALID, std.decodeBase64Char((int) '?'));
        assertEquals(Base64Variant.BASE64_VALUE_INVALID, std.decodeBase64Char((char) 0xA0));
        assertEquals(Base64Variant.BASE64_VALUE_INVALID, std.decodeBase64Char(0xA0));

        assertEquals(Base64Variant.BASE64_VALUE_INVALID, std.decodeBase64Byte((byte) '?'));
        assertEquals(Base64Variant.BASE64_VALUE_INVALID, std.decodeBase64Byte((byte) 0xA0));

        assertEquals(0, std.decodeBase64Char('A'));
        assertEquals(1, std.decodeBase64Char((int) 'B'));
        assertEquals(2, std.decodeBase64Char((byte)'C'));

        assertEquals(0, std.decodeBase64Byte((byte) 'A'));
        assertEquals(1, std.decodeBase64Byte((byte) 'B'));
        assertEquals(2, std.decodeBase64Byte((byte)'C'));

        assertEquals('/', std.encodeBase64BitsAsChar(63));
        assertEquals((byte) 'b', std.encodeBase64BitsAsByte(27));

        String EXP_STR = "HwdJ";
        int TRIPLET = 0x1F0749;
        StringBuilder sb = new StringBuilder();
        std.encodeBase64Chunk(sb, TRIPLET);
        assertEquals(EXP_STR, sb.toString());

        byte[] exp = EXP_STR.getBytes("UTF-8");
        byte[] act = new byte[exp.length];
        std.encodeBase64Chunk(TRIPLET, act, 0);
        assertArrayEquals(exp, act);
    }

    @Test
    void convenienceMethods() throws Exception
    {
        final Base64Variant std = Base64Variants.MIME;

        byte[] input = new byte[] { 1, 2, 34, 127, -1 };
        String encoded = std.encode(input, false);
        byte[] decoded = std.decode(encoded);
        assertArrayEquals(input, decoded);

        assertEquals(q(encoded), std.encode(input, true));

        // [core#414]: check white-space allow too
        decoded = std.decode("\n"+encoded);
        assertArrayEquals(input, decoded);
        decoded = std.decode("   "+encoded);
        assertArrayEquals(input, decoded);
        decoded = std.decode(encoded + "   ");
        assertArrayEquals(input, decoded);
        decoded = std.decode(encoded + "\n");
        assertArrayEquals(input, decoded);
    }

    @Test
    void convenienceMethodWithLFs() throws Exception
    {
        final Base64Variant std = Base64Variants.MIME;

        final int length = 100;
        final byte[] data = new byte[length];
        Arrays.fill(data, (byte) 1);

        final StringBuilder sb = new StringBuilder(140);
        for (int i = 0; i < 100/3; ++i) {
            sb.append("AQEB");
            if (sb.length() == 76) {
                sb.append("##");
            }
        }
        sb.append("AQ==");
        final String exp = sb.toString();

        // first, JSON standard
        assertEquals(exp.replace("##", "\\n"), std.encode(data, false));

        // then with custom linefeed

        assertEquals(exp.replace("##", "<%>"), std.encode(data, false, "<%>"));
    }

    @Test
    void convenienceMethodShortInputs() throws Exception
    {
        final Base64Variant std = Base64Variants.MIME;
        assertEquals("", std.encode(new byte[0], false));
        assertEquals("\"\"", std.encode(new byte[0], true));
        assertEquals("AQ==", std.encode(new byte[] { 1 }, false));
        assertEquals("AQE=", std.encode(new byte[] { 1, 1 }, false));
        assertEquals("AQEB", std.encode(new byte[] { 1, 1, 1 }, false));

        // Without padding, trailing partial chunk is 2 or 3 chars instead of 4
        final Base64Variant url = Base64Variants.MODIFIED_FOR_URL;
        assertEquals("AQ", url.encode(new byte[] { 1 }, false));
        assertEquals("AQE", url.encode(new byte[] { 1, 1 }, false));
    }

    @Test
    void convenienceMethodAtLineBoundary() throws Exception
    {
        // 48 bytes == 16 chunks == exactly one 64-char PEM line, so content ends
        // with a linefeed and quotes (if any) follow it
        final byte[] data = new byte[48];
        Arrays.fill(data, (byte) 1);
        final String line = "AQEB".repeat(16);

        assertEquals(line + "\\n", Base64Variants.PEM.encode(data, false));
        assertEquals("\"" + line + "\\n\"", Base64Variants.PEM.encode(data, true));
        assertEquals(line, Base64Variants.PEM.encode(data, false, ""));

        // MIME uses 76-char lines, so no linefeed at all for this input
        assertEquals(line, Base64Variants.MIME.encode(data, false));
        assertEquals(line, Base64Variants.MIME_NO_LINEFEEDS.encode(data, false));
    }

    @Test
    void convenienceMethodWithShortLineLength() throws Exception
    {
        // Constructor does not reject line lengths below one 4-char chunk;
        // encoder emits a linefeed after every chunk for those
        final byte[] data = new byte[9];
        Arrays.fill(data, (byte) 1);
        final String exp = "AQEB\\nAQEB\\nAQEB\\n";

        for (int maxLineLength : new int[] { 0, 1, 4 }) {
            Base64Variant v = new Base64Variant(Base64Variants.MIME,
                    "test-"+maxLineLength, maxLineLength);
            assertEquals(exp, v.encode(data, false), "maxLineLength="+maxLineLength);
        }
    }

    @SuppressWarnings("unused")
    @Test
    void errors() throws Exception
    {
        try {
            Base64Variant b = new Base64Variant("foobar", "xyz", false, '!', 24);
            fail("Should not pass");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "length must be exactly");
        }
        try {
            Base64Variants.MIME.decode("!@##@%$#%&*^(&)(*");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "Illegal character");
        }

        // also, for [jackson-core#335]
        final String BASE64_HELLO = "aGVsbG8=!";
        try {
            Base64Variants.MIME.decode(BASE64_HELLO);
            fail("Should not pass");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "Illegal character");
        }
    }

    @Test
    void paddingReadBehaviour() throws Exception {

        for (Base64Variant variant: Arrays.asList(Base64Variants.MIME, Base64Variants.MIME_NO_LINEFEEDS, Base64Variants.PEM)) {

            final String BASE64_HELLO = "aGVsbG8=";
            try {
                variant.withPaddingForbidden().decode(BASE64_HELLO);
                fail("Should not pass");
            } catch (IllegalArgumentException iae) {
                    verifyException(iae, "no padding");
            }

            variant.withPaddingAllowed().decode(BASE64_HELLO);
            variant.withPaddingRequired().decode(BASE64_HELLO);

            final String BASE64_HELLO_WITHOUT_PADDING = "aGVsbG8";
            try {
                variant.withPaddingRequired().decode(BASE64_HELLO_WITHOUT_PADDING);
                fail("Should not pass");
            } catch (IllegalArgumentException iae) {
                verifyException(iae, "expects padding");
            }
            variant.withPaddingAllowed().decode(BASE64_HELLO_WITHOUT_PADDING);
            variant.withPaddingForbidden().decode(BASE64_HELLO_WITHOUT_PADDING);
        }

        //testing for MODIFIED_FOR_URL

        final String BASE64_HELLO = "aGVsbG8=";
        try {
            Base64Variants.MODIFIED_FOR_URL.withPaddingForbidden().decode(BASE64_HELLO);
            fail("Should not pass");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "illegal character");
        }

        try {
            Base64Variants.MODIFIED_FOR_URL.withPaddingAllowed().decode(BASE64_HELLO);
            fail("Should not pass");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "illegal character");
        }

        try {
            Base64Variants.MODIFIED_FOR_URL.withPaddingRequired().decode(BASE64_HELLO);
            fail("Should not pass");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "illegal character");
        }

        final String BASE64_HELLO_WITHOUT_PADDING = "aGVsbG8";
        try {
            Base64Variants.MODIFIED_FOR_URL.withPaddingRequired().decode(BASE64_HELLO_WITHOUT_PADDING);
            fail("Should not pass");
        } catch (IllegalArgumentException iae) {
            verifyException(iae, "expects padding");
        }

        Base64Variants.MODIFIED_FOR_URL.withPaddingAllowed().decode(BASE64_HELLO_WITHOUT_PADDING);
        Base64Variants.MODIFIED_FOR_URL.withPaddingForbidden().decode(BASE64_HELLO_WITHOUT_PADDING);
    }
}
