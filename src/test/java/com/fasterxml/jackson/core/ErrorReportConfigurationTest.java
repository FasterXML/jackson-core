package com.fasterxml.jackson.core;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.io.ContentReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Unit tests for class {@link ErrorReportConfiguration}.
 * 
 * @since 2.16
 */
class ErrorReportConfigurationTest
        extends JUnit5TestBase
{
    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */

    private final int DEFAULT_CONTENT_LENGTH = ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH;

    private final int DEFAULT_ERROR_LENGTH = ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH;

    private final ErrorReportConfiguration DEFAULTS = ErrorReportConfiguration.defaults();

    @Test
    void normalBuild()
    {
        ErrorReportConfiguration config = ErrorReportConfiguration.builder()
                .maxErrorTokenLength(1004)
                .maxRawContentLength(2008)
                .build();

        assertEquals(1004, config.getMaxErrorTokenLength());
        assertEquals(2008, config.getMaxRawContentLength());
    }

    @Test
    void zeroLengths()
    {
        // boundary tests, because we throw error on negative values
        ErrorReportConfiguration config = ErrorReportConfiguration.builder()
                .maxErrorTokenLength(0)
                .maxRawContentLength(0)
                .build();

        assertEquals(0, config.getMaxErrorTokenLength());
        assertEquals(0, config.getMaxRawContentLength());
    }

    @Test
    void invalidMaxErrorTokenLength()
    {
        ErrorReportConfiguration.Builder builder = ErrorReportConfiguration.builder();
        try {
            builder.maxErrorTokenLength(-1);
            fail("Should not reach here as exception is expected");
        } catch (IllegalArgumentException ex) {
            verifyException(ex, "Value of maxErrorTokenLength");
            verifyException(ex, "cannot be negative");
        }
        try {
            builder.maxRawContentLength(-1);
            fail("Should not reach here as exception is expected");
        } catch (IllegalArgumentException ex) {
            verifyException(ex, "Value of maxRawContentLength");
            verifyException(ex, "cannot be negative");
        }
    }

    @Test
    void defaults()
    {
        // default value
        assertEquals(DEFAULT_ERROR_LENGTH, DEFAULTS.getMaxErrorTokenLength());
        assertEquals(DEFAULT_CONTENT_LENGTH, DEFAULTS.getMaxRawContentLength());

        // equals
        assertEquals(ErrorReportConfiguration.defaults(), ErrorReportConfiguration.defaults());
    }

    @Test
    void overrideDefaultErrorReportConfiguration()
    {
        // (1) override with null, will be no change
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(null);
        try {
            ErrorReportConfiguration nullDefaults = ErrorReportConfiguration.defaults();

            assertEquals(DEFAULT_ERROR_LENGTH, nullDefaults.getMaxErrorTokenLength());
            assertEquals(DEFAULT_CONTENT_LENGTH, nullDefaults.getMaxRawContentLength());

            // (2) override with other value that actually changes default values
            ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(ErrorReportConfiguration.builder()
                    .maxErrorTokenLength(10101)
                    .maxRawContentLength(20202)
                    .build());

            ErrorReportConfiguration overrideDefaults = ErrorReportConfiguration.defaults();

            assertEquals(10101, overrideDefaults.getMaxErrorTokenLength());
            assertEquals(20202, overrideDefaults.getMaxRawContentLength());
        } finally {
            // (3) revert back to default values
            // IMPORTANT : make sure to revert back, otherwise other tests will be affected
            ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(ErrorReportConfiguration.builder()
                    .maxErrorTokenLength(DEFAULT_ERROR_LENGTH)
                    .maxRawContentLength(DEFAULT_CONTENT_LENGTH)
                    .build());
        }
    }

    @Test
    void rebuild()
    {
        ErrorReportConfiguration config = ErrorReportConfiguration.builder().build();
        ErrorReportConfiguration rebuiltConfig = config.rebuild().build();

        assertEquals(config.getMaxErrorTokenLength(), rebuiltConfig.getMaxErrorTokenLength());
        assertEquals(config.getMaxRawContentLength(), rebuiltConfig.getMaxRawContentLength());
    }

    @Test
    void builderConstructorWithErrorReportConfiguration()
    {
        ErrorReportConfiguration configA = ErrorReportConfiguration.builder()
                .maxErrorTokenLength(1234)
                .maxRawContentLength(5678)
                .build();

        ErrorReportConfiguration configB = configA.rebuild().build();

        assertEquals(configA.getMaxErrorTokenLength(), configB.getMaxErrorTokenLength());
        assertEquals(configA.getMaxRawContentLength(), configB.getMaxRawContentLength());
    }

    @Test
    void withJsonLocation() throws Exception
    {
        // Truncated result
        _verifyJsonLocationToString("abc", 2, "\"ab\"[truncated 1 chars]");
        // Exact length
        _verifyJsonLocationToString("abc", 3, "\"abc\"");
        // Enough length
        _verifyJsonLocationToString("abc", 4, "\"abc\"");
    }

    @Test
    void withJsonFactory() throws Exception
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
                        .maxRawContentLength(DEFAULT_CONTENT_LENGTH - 1).build());
        // longer 
        _verifyJsonProcessingExceptionSourceLength(501,
                ErrorReportConfiguration.builder()
                        .maxRawContentLength(DEFAULT_CONTENT_LENGTH + 1).build());
        // zero
        _verifyJsonProcessingExceptionSourceLength(0,
                ErrorReportConfiguration.builder()
                        .maxRawContentLength(0).build());
    }

    @Test
    void expectedTokenLengthWithConfigurations()
            throws Exception
    {
        // [core#1180]: After fix, error location points to token start (position 7 in "{\"key\":aaa...")
        // not to the end of consumed token, so all cases now report same offset
        final int tokenStartOffset = 7;

        // default
        _verifyErrorTokenLength(tokenStartOffset,
                ErrorReportConfiguration.builder().build());
        // default
        _verifyErrorTokenLength(tokenStartOffset,
                ErrorReportConfiguration.defaults());
        // shorter
        _verifyErrorTokenLength(tokenStartOffset,
                ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(DEFAULT_ERROR_LENGTH - 200).build());
        // longer
        _verifyErrorTokenLength(tokenStartOffset,
                ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(DEFAULT_ERROR_LENGTH + 200).build());
        // zero
        _verifyErrorTokenLength(tokenStartOffset,
                ErrorReportConfiguration.builder()
                        .maxErrorTokenLength(0).build());

        // negative value fails
        try {
            _verifyErrorTokenLength(tokenStartOffset,
                    ErrorReportConfiguration.builder()
                            .maxErrorTokenLength(-1).build());
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .contains("Value of maxErrorTokenLength")
                    .contains("cannot be negative");
        }
        // null is not allowed, throws NPE
        try {
            _verifyErrorTokenLength(tokenStartOffset,
                    null);
            fail("Should not reach here as exception is expected");
        } catch (NullPointerException e) {
            // no-op
        }
    }

    @Test
    void nonPositiveErrorTokenConfig()
    {
        // Zero should be ok
        ErrorReportConfiguration.builder().maxErrorTokenLength(0).build();

        // But not -1
        try {
            ErrorReportConfiguration.builder().maxErrorTokenLength(-1).build();
            fail();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage())
                    .contains("Value of maxErrorTokenLength")
                    .contains("cannot be negative");
        }
    }

    @Test
    void nullSetterThrowsException() {
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

    private void _verifyJsonProcessingExceptionSourceLength(int expectedRawContentLength, ErrorReportConfiguration erc)
            throws Exception
    {
        // Arrange
        JsonFactory factory = streamFactoryBuilder()
                .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
                .errorReportConfiguration(erc)
                .build();
        // Make JSON input too long so it can be cutoff
        int tooLongContent = 50 * DEFAULT_CONTENT_LENGTH;
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

    private void _verifyJsonLocationToString(String rawSrc, int rawContentLength, String expectedMessage)
    {
        ErrorReportConfiguration erc = ErrorReportConfiguration.builder()
                .maxRawContentLength(rawContentLength)
                .build();
        ContentReference reference = ContentReference.construct(true, rawSrc, 0, rawSrc.length(), erc);
        assertEquals(
                "[Source: (String)" + expectedMessage + "; line: 1, column: 1]",
                new JsonLocation(reference, 10L, 10L, 1, 1).toString());
    }

    private void _verifyErrorTokenLength(int expectedTokenLen, ErrorReportConfiguration errorReportConfiguration)
            throws Exception
    {
        JsonFactory jf3 = streamFactoryBuilder()
                .errorReportConfiguration(errorReportConfiguration)
                .build();
        _testWithMaxErrorTokenLength(expectedTokenLen,
                // creating arbitrary number so that token reaches max len, but not over-do it
                50 * DEFAULT_ERROR_LENGTH, jf3);
    }

    private void _testWithMaxErrorTokenLength(int expectedSize, int tokenLen, JsonFactory factory)
            throws Exception
    {
        String inputWithDynamicLength = _buildBrokenJsonOfLength(tokenLen);
        try (JsonParser parser = factory.createParser(inputWithDynamicLength)) {
            parser.nextToken();
            parser.nextToken();
        } catch (JsonProcessingException e) {
            assertThat(e.getLocation().getCharOffset()).isEqualTo(expectedSize);
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
