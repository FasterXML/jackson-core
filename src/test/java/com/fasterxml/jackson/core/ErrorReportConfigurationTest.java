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
    public void testNormalBuild()
    {
        ErrorReportConfiguration config = ErrorReportConfiguration.builder()
                .maxErrorTokenLength(1004)
                .maxRawContentLength(2008)
                .build();

        assertEquals(1004, config.getMaxErrorTokenLength());
        assertEquals(2008, config.getMaxRawContentLength());
    }

    @Test
    public void testZeroLengths()
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
    public void testInvalidMaxErrorTokenLength()
    {
        ErrorReportConfiguration.Builder builder = ErrorReportConfiguration.builder();
        
        try {
            builder.maxErrorTokenLength(-1);
            fail("Should not reach here as exception is expected");
        } catch (IllegalArgumentException ex) {
            // expected
        }
        
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
        // (1) override with null, will be no change
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(null);
        
        ErrorReportConfiguration nullDefaults = ErrorReportConfiguration.defaults();
        
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH, nullDefaults.getMaxErrorTokenLength());
        assertEquals(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH, nullDefaults.getMaxRawContentLength());
        
        // (2) override wiht other value that actually changes default values
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(ErrorReportConfiguration.builder()
                .maxErrorTokenLength(10101)
                .maxRawContentLength(20202)
                .build());
        
        ErrorReportConfiguration overrideDefaults = ErrorReportConfiguration.defaults();
        
        assertEquals(10101, overrideDefaults.getMaxErrorTokenLength());
        assertEquals(20202, overrideDefaults.getMaxRawContentLength());
        
        // (3) revert back to default values
        // IMPORTANT : make sure to revert back, otherwise other tests will be affected
        ErrorReportConfiguration.overrideDefaultErrorReportConfiguration(ErrorReportConfiguration.builder()
                .maxErrorTokenLength(ErrorReportConfiguration.DEFAULT_MAX_ERROR_TOKEN_LENGTH)
                .maxRawContentLength(ErrorReportConfiguration.DEFAULT_MAX_RAW_CONTENT_LENGTH)
                .build());
    }

    @Test
    public void testRebuild()
    {
        ErrorReportConfiguration config = ErrorReportConfiguration.builder().build();
        
        ErrorReportConfiguration rebuiltConfig = config.rebuild().build();
        
        assertEquals(config.getMaxErrorTokenLength(), rebuiltConfig.getMaxErrorTokenLength());
        assertEquals(config.getMaxRawContentLength(), rebuiltConfig.getMaxRawContentLength());
    }


    @Test
    public void testBuilderConstructorWithTwoParams()
    {
        ErrorReportConfiguration config = new ErrorReportConfiguration.Builder(1313, 2424)
                .build();
        
        assertEquals(1313, config.getMaxErrorTokenLength());
        assertEquals(2424, config.getMaxRawContentLength());
    }

    @Test
    public void testBuilderConstructorWithErrorReportConfiguration()
    {
        ErrorReportConfiguration configA = ErrorReportConfiguration.builder()
                .maxErrorTokenLength(1234)
                .maxRawContentLength(5678)
                .build();
        
        ErrorReportConfiguration configB = new ErrorReportConfiguration.Builder(configA).build();
        
        assertEquals(configA.getMaxErrorTokenLength(), configB.getMaxErrorTokenLength());
        assertEquals(configA.getMaxRawContentLength(), configB.getMaxRawContentLength());
    }
}
