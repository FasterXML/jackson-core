package com.fasterxml.jackson.core;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.core.io.ContentReference;

/**
 * Unit tests for class {@link ErrorReportConfiguration#getMaxRawContentLength()}.
 */
public class ErrorReportConfigurationMaxRawContentLengthTest extends BaseTest
{
    // Should be 256
    public final static int DEFAULT_LENGTH = ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH;
    
    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */

    public void testWithJsonLocation() throws Exception
    {
        // Truncated result
        _verifyJsonLocationToString("abc", 2,
                "[Source: (String)\"ab\"[truncated 1 chars]; line: 1, column: 1]");
        // Exact length
        _verifyJsonLocationToString("abc", 3,
                "[Source: (String)\"abc\"; line: 1, column: 1]");
        // Enough length
        _verifyJsonLocationToString("abc", 4,
                "[Source: (String)\"abc\"; line: 1, column: 1]");
    }

    public void testWithJsonFactory() throws Exception
    {
        // default
        _verifyJsonProcessingExceptionSourceLength(500,
                ErrorReportConfiguration.builder().build());

        // default
        _verifyJsonProcessingExceptionSourceLength(500,
                ErrorReportConfiguration.defaults());

        // shorter
        _verifyJsonProcessingExceptionSourceLength(499,
                ErrorReportConfiguration.builder()
                        .maxRawContentLength(DEFAULT_LENGTH - 1).build());

        // longer 
        _verifyJsonProcessingExceptionSourceLength(501,
                ErrorReportConfiguration.builder()
                        .maxRawContentLength(DEFAULT_LENGTH + 1).build());

        // zero
        _verifyJsonProcessingExceptionSourceLength(0,
                ErrorReportConfiguration.builder()
                        .maxRawContentLength(0).build());
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verifyJsonLocationToString(String rawSrc, int rawContentLength, String expectedMessage)
    {
        ContentReference reference = _sourceRefWithErrorReportConfig(rawSrc, rawContentLength);
        String location = new JsonLocation(reference, 10L, 10L, 1, 1).toString();
        assertEquals(expectedMessage, location);
    }

    private ContentReference _sourceRefWithErrorReportConfig(String rawSrc, int rawContentLength)
    {
        return _sourceRef(rawSrc,
                ErrorReportConfiguration.builder().maxRawContentLength(rawContentLength).build());
    }

    private ContentReference _sourceRef(String rawSrc, ErrorReportConfiguration errorReportConfiguration)
    {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length(), errorReportConfiguration);
    }

    private void _verifyJsonProcessingExceptionSourceLength(int expectedRawContentLength, ErrorReportConfiguration errorReportConfiguration)
            throws Exception
    {
        // Arrange
        JsonFactory factory = streamFactoryBuilder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .errorReportConfiguration(errorReportConfiguration)
                .build();
        // Make JSON input too long so it can be cutoff
        int tooLongContent = 50 * DEFAULT_LENGTH;
        String inputWithDynamicLength = _buildBrokenJsonOfLength(tooLongContent);

        // Act
        try (JsonParser parser = factory.createParser(inputWithDynamicLength)) {
            parser.nextToken();
            parser.nextToken();
            fail("Should not reach");
        } catch (JsonProcessingException e) {

            // Assert
            String prefix = "(String)\"";
            String suffix = "\"[truncated 12309 chars]";

            // The length of the source description should be [ prefix + expected length + suffix ]
            int expectedLength = prefix.length() + expectedRawContentLength + suffix.length();
            int actualLength = e.getLocation().sourceDescription().length();

            assertEquals(expectedLength, actualLength);
            assertThat(e.getMessage())
                    .contains("Unrecognized token '")
                    .contains("was expecting (JSON");
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
