package com.fasterxml.jackson.core;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for class {@link JsonFactory#setMaxErrorTokenLength(int)}.
 */
public class MaxErrorTokenLengthTest extends BaseTest {

    final int tokenLength = JsonFactory.DEFAULT_MAX_ERROR_TOKEN_LENGTH;
    
    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */

    public void testSetMaxErrorTokenLength() throws Exception {
        // arrange
        JsonFactory defaultJF = streamFactoryBuilder().build();

        // act & assert
        testWithMaxErrorTokenLength(263, 1 * tokenLength, defaultJF);
        testWithMaxErrorTokenLength(263, 10 * tokenLength, defaultJF);
        testWithMaxErrorTokenLength(263, 100 * tokenLength, defaultJF);
    }

    public void testShorterSetMaxErrorTokenLength() throws Exception {
        // arrange
        int shorterSetting = tokenLength - 200;
        JsonFactory shorterJF = streamFactoryBuilder().build()
                .setMaxErrorTokenLength(shorterSetting);

        // act & assert
        testWithMaxErrorTokenLength(63, 1 * tokenLength, shorterJF);
        testWithMaxErrorTokenLength(63, 10 * tokenLength, shorterJF);
        testWithMaxErrorTokenLength(63, 100 * tokenLength, shorterJF);
    }

    public void testLongerSetMaxErrorTokenLength() throws Exception {
        // arrange
        int longerSetting = tokenLength + 200;
        JsonFactory longerJF = streamFactoryBuilder().build()
                .setMaxErrorTokenLength(longerSetting);

        // act & assert
        testWithMaxErrorTokenLength(263, 1 * tokenLength, longerJF);
        testWithMaxErrorTokenLength(463, 10 * tokenLength, longerJF);
        testWithMaxErrorTokenLength(463, 100 * tokenLength, longerJF);
    }

    public void testZeroSetMaxErrorTokenLength() throws Exception {
        // arrange
        int zeroSetting = 0;
        JsonFactory longerJF = streamFactoryBuilder().build()
                .setMaxErrorTokenLength(zeroSetting);

        // act & assert
        testWithMaxErrorTokenLength(9, 1 * tokenLength, longerJF);
        testWithMaxErrorTokenLength(9, 10 * tokenLength, longerJF);
        testWithMaxErrorTokenLength(9, 100 * tokenLength, longerJF);
    }

    public void testNegativeConfiguration() {
        try {
            streamFactoryBuilder().build()
                    .setMaxErrorTokenLength(-1);
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("maxErrorTokenLength (-1) must be greater than 0");
        }
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void testWithMaxErrorTokenLength(int expectedSize, int tokenLen, JsonFactory factory) throws Exception {
        String inputWithDynamicLength = createBrokenJsonWithLength(tokenLen);
        try (JsonParser parser = factory.createParser(inputWithDynamicLength)) {
            parser.nextToken();
            parser.nextToken();
        } catch (JsonProcessingException e) {
            assertThat(e.getLocation()._totalChars).isEqualTo(expectedSize);
            assertThat(e.getMessage()).contains("Unrecognized token");
        }
    }

    private String createBrokenJsonWithLength(int len) {
        StringBuilder sb = new StringBuilder("{\"key\":");
        for (int i = 0; i < len; i++) {
            sb.append("a");
        }
        sb.append("!}");
        return sb.toString();
    }
}
