package com.fasterxml.jackson.core;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for class {@link ErrorTokenConfiguration}.
 */
public class ErrorTokenConfigurationTest extends BaseTest 
{

    // Should be 256
    public final static int DEFAULT_LENGTH = ErrorTokenConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH;

    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */
    public void testExpectedTokenLengthWithConfigurations() 
            throws Exception 
    {
        // default
        _verifyErrorTokenLength(263,
                ErrorTokenConfiguration.builder().build());

        // default
        _verifyErrorTokenLength(263,
                ErrorTokenConfiguration.defaults());

        // null -> should be default
        _verifyErrorTokenLength(263,
                null);

        // shorter
        _verifyErrorTokenLength(63,
                ErrorTokenConfiguration.builder()
                        .maxErrorTokenLength(DEFAULT_LENGTH - 200).build());

        // longer 
        _verifyErrorTokenLength(463,
                ErrorTokenConfiguration.builder()
                        .maxErrorTokenLength(DEFAULT_LENGTH + 200).build());

        // zero
        _verifyErrorTokenLength(9,
                ErrorTokenConfiguration.builder()
                        .maxErrorTokenLength(0).build());
        
        // negative value fails
        try {
            _verifyErrorTokenLength(9,
                    ErrorTokenConfiguration.builder()
                            .maxErrorTokenLength(-1).build());   
        } catch (IllegalArgumentException e) {
            _verifyExceptionMessage(e.getMessage());
        }
    }

    public void testNegativeBuilderConfiguration() 
    {
        // Zero should be ok
        ErrorTokenConfiguration.builder().maxErrorTokenLength(0).build();
        
        // But not -1
        try {
            ErrorTokenConfiguration.builder().maxErrorTokenLength(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            _verifyExceptionMessage(e.getMessage());
        }
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verifyExceptionMessage(String message) 
    {
        assertThat(message)
                .contains("Value of maxErrorTokenLength")
                .contains("cannot be negative");
    }

    private void _verifyErrorTokenLength(int expectedTokenLen, ErrorTokenConfiguration errorTokenConfiguration) 
            throws Exception 
    {
        // for Jackson 2.x
        JsonFactory jf2 = new JsonFactory().setErrorTokenConfiguration(errorTokenConfiguration);
        _testWithMaxErrorTokenLength(expectedTokenLen,
                // creating arbitrary number so that token reaches max len, but not over-do it
                50 * DEFAULT_LENGTH, jf2);

        // for Jackson 3.x
        JsonFactory jf3 = streamFactoryBuilder().errorTokenConfiguration(errorTokenConfiguration)
                .build();
        _testWithMaxErrorTokenLength(expectedTokenLen,
                // creating arbitrary number so that token reaches max len, but not over-do it
                50 * DEFAULT_LENGTH, jf3);
        
    }

    private void _testWithMaxErrorTokenLength(int expectedSize, int tokenLen, JsonFactory factory) 
            throws Exception 
    {
        String inputWithDynamicLength = _buildBrokenJsonOfLength(tokenLen);
        try (JsonParser parser = factory.createParser(inputWithDynamicLength)) {
            parser.nextToken();
            parser.nextToken();
        } catch (JsonProcessingException e) {
            assertThat(e.getLocation()._totalChars).isEqualTo(expectedSize);
            assertThat(e.getMessage()).contains("Unrecognized token");
        }
    }

    private String _buildBrokenJsonOfLength(int len) 
    {
        StringBuilder sb = new StringBuilder("{\"key\":");
        for (int i = 0; i < len; i++) {
            sb.append("a");
        }
        sb.append("!}");
        return sb.toString();
    }
}
