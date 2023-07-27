package com.fasterxml.jackson.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link ErrorReportConfiguration}.
 * 
 * @since 2.16
 */
public class ErrorReportConfigurationTest
{
    private final ErrorReportConfiguration DEFAULTS = ErrorReportConfiguration.defaults();

    @Test
    public void testZeroLengths()
    {
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder();
        builder.maxErrorTokenLength(0);
        builder.maxRawContentLength(0);
        ErrorReportConfiguration config = builder.build();
        assertEquals(0, config.getMaxErrorTokenLength());
        assertEquals(0, config.getMaxRawContentLength());
    }

    @Test
    public void testInvalidMaxErrorTokenLength()
    {
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder();
        try {
            builder.maxErrorTokenLength(-1);
            fail("Should not reach here as exception is expected");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testMaxRawContentLength()
    {
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder();
        int maxLength = 256;
        builder.maxRawContentLength(maxLength);
        ErrorReportConfiguration config = builder.build();
        assertEquals(maxLength, config.getMaxRawContentLength());
    }

    @Test
    public void testInvalidMaxRawContentLength()
    {
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder();
        try {
            builder.maxRawContentLength(-1);
            fail("Should not reach here as exception is expected");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testDefaults()
    {
        // default value
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH, DEFAULTS.getMaxErrorTokenLength());
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH, DEFAULTS.getMaxRawContentLength());

        // equals
        assertEquals(ErrorReportConfiguration.defaults(), ErrorReportConfiguration.defaults());
    }

    @Test
    public void testOverrideDefaultErrorReportConfiguration()
    {
        // try with null, no change
        ErrorReportConfiguration nullDefaults = ErrorReportConfiguration.defaults();
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(null);
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH, nullDefaults.getMaxErrorTokenLength());
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH, nullDefaults.getMaxRawContentLength());
        
        // try override defaults
        ErrorReportConfiguration overrideDefaults = new ErrorReportConfiguration.Builder()
                .maxErrorTokenLength(128)
                .maxRawContentLength(256)
                .build();
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(overrideDefaults);
        assertEquals(128, overrideDefaults.getMaxErrorTokenLength());
        assertEquals(256, overrideDefaults.getMaxRawContentLength());
        
        // IMPORTANT : make sure to revert back, otherwise other tests will be affected
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(new ErrorReportConfiguration.Builder()
                .maxErrorTokenLength(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH)
                .maxRawContentLength(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH)
                .build());
    }

    @Test
    public void testRebuild()
    {
        ErrorReportConfiguration config = new ErrorReportConfiguration.Builder().build();
        ErrorReportConfiguration rebuiltConfig = config.rebuild().build();
        assertEquals(config.getMaxErrorTokenLength(), rebuiltConfig.getMaxErrorTokenLength());
        assertEquals(config.getMaxRawContentLength(), rebuiltConfig.getMaxRawContentLength());
    }


    @Test
    public void testBuilderConstructorWithTwoParams()
    {
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder(128, 256);
        ErrorReportConfiguration config = builder.build();
        assertEquals(128, config.getMaxErrorTokenLength());
        assertEquals(256, config.getMaxRawContentLength());
    }

    @Test
    public void testBuilderConstructorWithErrorReportConfiguration()
    {
        ErrorReportConfiguration newConfig = new ErrorReportConfiguration.Builder()
                .maxErrorTokenLength(128)
                .maxRawContentLength(256)
                .build();
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder(newConfig);
        ErrorReportConfiguration config = builder.build();
        assertEquals(newConfig.getMaxErrorTokenLength(), config.getMaxErrorTokenLength());
        assertEquals(newConfig.getMaxRawContentLength(), config.getMaxRawContentLength());
    }
}
