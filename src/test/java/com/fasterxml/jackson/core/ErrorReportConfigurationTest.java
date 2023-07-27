package com.fasterxml.jackson.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

public class ErrorReportConfigurationTest
{
    @Test
    public void testMaxErrorTokenLength()
    {
        ErrorReportConfiguration.Builder builder = new ErrorReportConfiguration.Builder();
        int maxLength = 128;
        builder.maxErrorTokenLength(maxLength);
        ErrorReportConfiguration config = builder.build();
        assertEquals(maxLength, config.getMaxErrorTokenLength());
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
        ErrorReportConfiguration config = ErrorReportConfiguration.defaults();
        
        // default value
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH, config.getMaxErrorTokenLength());
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH, config.getMaxRawContentLength());

        // equals
        assertEquals(ErrorReportConfiguration.defaults(), ErrorReportConfiguration.defaults());
    }

    @Test
    public void testOverrideDefaultErrorReportConfiguration()
    {
        // try override static
        ErrorReportConfiguration newConfig = new ErrorReportConfiguration.Builder()
                .maxErrorTokenLength(128)
                .maxRawContentLength(256)
                .build();
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(newConfig);
        
        ErrorReportConfiguration config = ErrorReportConfiguration.defaults();
        assertEquals(128, config.getMaxErrorTokenLength());
        assertEquals(256, config.getMaxRawContentLength());
        
        // IMPORTANT : make sure to revert back, otherwise other tests will be affected
        ErrorReportConfiguration defaultConfig = new ErrorReportConfiguration.Builder()
                .maxErrorTokenLength(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH)
                .maxRawContentLength(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH)
                .build();
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(defaultConfig);
    }

    @Test
    public void testRebuild()
    {
        ErrorReportConfiguration config = new ErrorReportConfiguration.Builder().build();
        ErrorReportConfiguration rebuiltConfig = config.rebuild().build();
        assertEquals(config.getMaxErrorTokenLength(), rebuiltConfig.getMaxErrorTokenLength());
        assertEquals(config.getMaxRawContentLength(), rebuiltConfig.getMaxRawContentLength());
    }
}
