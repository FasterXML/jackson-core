package tools.jackson.core.unittest.base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.core.Base64Variant;
import tools.jackson.core.Base64Variants;

import static org.junit.jupiter.api.Assertions.*;

class Base64DecodeInitialBuffer1844Test
{
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 16, 32, 64, 128, 256,
            498, 499, 500, 501, 502, 503, 1024, 65536, 1048576})
    void decodeAcrossInitialBufferBoundary(int size)
    {
        byte[] expected = new byte[size];
        for (int i = 0; i < size; ++i) {
            expected[i] = (byte) (i * 31);
        }
        for (Base64Variant variant : new Base64Variant[] {
                Base64Variants.MIME_NO_LINEFEEDS,
                Base64Variants.MODIFIED_FOR_URL,
                Base64Variants.MIME_NO_LINEFEEDS.withPaddingAllowed().withWritePadding(false)
        }) {
            assertArrayEquals(expected, variant.decode(variant.encode(expected)), variant.getName());
        }
    }

    @Test
    void decodeEmptyAndWhitespaceOnlyInput()
    {
        Base64Variant variant = Base64Variants.getDefaultVariant();
        assertArrayEquals(new byte[0], variant.decode(""));
        assertArrayEquals(new byte[0], variant.decode(" \t\r\n"));
        assertArrayEquals(new byte[0], variant.decode(" ".repeat(4096)));
    }

    @Test
    void decodeWhitespaceBetweenUnits()
    {
        assertArrayEquals(new byte[] {1, 2, 3, 4, 5},
                Base64Variants.getDefaultVariant().decode(" \t\r\nAQID \r\nBAU= \n"));
        // Padding does not terminate the input in the existing decoder.
        assertArrayEquals(new byte[] {1, 1},
                Base64Variants.getDefaultVariant().decode("AQ== AQ=="));
    }

    @Test
    void decodeWithCustomPadding()
    {
        Base64Variant variant = new Base64Variant("custom",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/",
                true, '*', Integer.MAX_VALUE);
        assertArrayEquals(new byte[] {1}, variant.decode("AQ**"));
        assertArrayEquals(new byte[] {1, 2}, variant.decode("AQI*"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "=", "AA=", "A=AA", "AA A", "AA\u0100A", "AA==!"})
    void rejectInvalidInput(String input)
    {
        assertThrows(IllegalArgumentException.class, () -> Base64Variants.getDefaultVariant().decode(input));
    }
}
