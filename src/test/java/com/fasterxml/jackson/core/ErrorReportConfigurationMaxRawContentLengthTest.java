package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.ContentReference;

/**
 * Unit tests for class {@link ErrorReportConfiguration#getMaxRawContentLength()}.
 */
public class ErrorReportConfigurationMaxRawContentLengthTest extends BaseTest 
{
    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */

    public void testBasicToStringErrorConfig()
    {
        final String three = "three";
        // Truncated result
        _verifyToString(three, 4, 
                "[Source: (String)\"thre\"[truncated 1 chars]; line: 1, column: 1]");
        
        // Exact length
        _verifyToString(three, 5,
                "[Source: (String)\"three\"; line: 1, column: 1]");
        
        // Enough length
        _verifyToString(three, 99999,
                "[Source: (String)\"three\"; line: 1, column: 1]");
    }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verifyToString(String rawSrc, int rawContentLength, String expectedMessage)
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
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length(),errorReportConfiguration);
    }

}
