package com.fasterxml.jackson.core.base64;

import java.util.Arrays;

import org.junit.Assert;

import com.fasterxml.jackson.core.*;

public class Base64CodecTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testVariantAccess()
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

    public void testProps()
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

    public void testCharEncoding() throws Exception
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
        Assert.assertArrayEquals(exp, act);
    }

    public void testConvenienceMethods() throws Exception
    {
        final Base64Variant std = Base64Variants.MIME;

        byte[] input = new byte[] { 1, 2, 34, 127, -1 };
        String encoded = std.encode(input, false);
        byte[] decoded = std.decode(encoded);
        Assert.assertArrayEquals(input, decoded);

        assertEquals(q(encoded), std.encode(input, true));

        // [core#414]: check white-space allow too
        decoded = std.decode("\n"+encoded);
        Assert.assertArrayEquals(input, decoded);
        decoded = std.decode("   "+encoded);
        Assert.assertArrayEquals(input, decoded);
        decoded = std.decode(encoded + "   ");
        Assert.assertArrayEquals(input, decoded);
        decoded = std.decode(encoded + "\n");
        Assert.assertArrayEquals(input, decoded);
    }

    public void testConvenienceMethodWithLFs() throws Exception
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

    @SuppressWarnings("unused")
    public void testErrors() throws Exception
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

    public void testPaddingReadBehaviour() throws Exception {

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
