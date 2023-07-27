package com.fasterxml.jackson.core;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for class {@link ErrorReportConfiguration#getMaxErrorTokenLength()}.
 */
public class ErrorReportConfigurationMaxErrorTokenLengthTest extends BaseTest 
{

    // Should be 256
    public final static int DEFAULT_LENGTH = ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH;

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
                ErrorReportConfiguration.builder().build());

        // default
        _verifyErrorTokenLength(263,
                ErrorReportConfiguration.defaults());

        // shorter
        _verifyErrorTokenLength(63,
                ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(DEFAULT_LENGTH - 200).build());

        // longer 
        _verifyErrorTokenLength(463,
                ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(DEFAULT_LENGTH + 200).build());

        // zero
        _verifyErrorTokenLength(9,
                ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(0).build());
        
        // negative value fails
        try {
            _verifyErrorTokenLength(9,
                    ErrorReportConfiguration.builder()
                            .maxErrorTokenLength(-1).build());   
        } catch (IllegalArgumentException e) {
            _verifyIllegalArgumentExceptionMessage(e.getMessage());
        }

        // null is not allowed
        try {
            _verifyErrorTokenLength(263,
                null);
        } catch (NullPointerException e) {
            // no-op
        }
    }

    public void testNegativeBuilderConfiguration() 
    {
        // Zero should be ok
        ErrorReportConfiguration.builder().maxErrorTokenLength(0).build();
        
        // But not -1
        try {
            ErrorReportConfiguration.builder().maxErrorTokenLength(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            _verifyIllegalArgumentExceptionMessage(e.getMessage());
        }
    }
    
    public void testNullSetterThrowsException() {
        try {
            newStreamFactory().setErrorReportConfiguration(null);
            fail();
        } catch (NullPointerException npe) {
            assertThat(npe).hasMessage("Cannot pass null ErrorReportConfiguration");
        }
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verifyIllegalArgumentExceptionMessage(String message) {
        assertThat(message)
                .contains("Value of maxErrorTokenLength")
                .contains("cannot be negative");
    }

    private void _verifyErrorTokenLength(int expectedTokenLen, ErrorReportConfiguration errorReportConfiguration) 
            throws Exception 
    {
        JsonFactory jf3 = streamFactoryBuilder()
                .errorReportConfiguration(errorReportConfiguration)
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
