package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.io.ContentReference;

/**
 * Unit tests for class {@link ErrorReportConfiguration#getMaxRawContentLength()}.
 */
public class ErrorReportConfigurationMaxRawContentLengthTest extends BaseTest {
    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */

    public void testBasicToStringErrorConfig() throws Exception {
        // Short String
        ContentReference reference = _sourceRefWithErrorReportConfig("abcde", 1);
        String location = new JsonLocation(reference, 10L, 10L, 1, 2).toString();
        assertEquals("[Source: (String)\"a\"[truncated 4 chars]; line: 1, column: 2]", location);

    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private ContentReference _sourceRefWithErrorReportConfig(String rawSrc, int rawContentLength) {
        return _sourceRef(rawSrc, 
                ErrorReportConfiguration.builder().maxRawContentLength(rawContentLength).build());
    }

    private ContentReference _sourceRef(String rawSrc, ErrorReportConfiguration errorReportConfiguration) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length(),errorReportConfiguration);
    }

}
