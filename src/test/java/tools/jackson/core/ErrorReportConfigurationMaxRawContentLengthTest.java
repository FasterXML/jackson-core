package tools.jackson.core;

import tools.jackson.core.io.ContentReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link ErrorReportConfiguration#getMaxRawContentLength()}.
 */
public class ErrorReportConfigurationMaxRawContentLengthTest
    extends JUnit5TestBase
{
    /*
    /**********************************************************
    /* Unit Tests
    /**********************************************************
     */

    public void testBasicToStringErrorConfig() throws Exception {
        // Truncated result
        _verifyToString("abc", 2,
                "[Source: (String)\"ab\"[truncated 1 chars]; line: 1, column: 1]");
        // Exact length
        _verifyToString("abc", 3,
                "[Source: (String)\"abc\"; line: 1, column: 1]");
        // Enough length
        _verifyToString("abc", 4,
                "[Source: (String)\"abc\"; line: 1, column: 1]");
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    private void _verifyToString(String rawSrc, int rawContentLength, String expectedMessage) {
        ContentReference reference = _sourceRefWithErrorReportConfig(rawSrc, rawContentLength);
        String location = new JsonLocation(reference, 10L, 10L, 1, 1).toString();
        assertEquals(expectedMessage, location);
    }

    private ContentReference _sourceRefWithErrorReportConfig(String rawSrc, int rawContentLength) {
        return _sourceRef(rawSrc,
                ErrorReportConfiguration.builder().maxRawContentLength(rawContentLength).build());
    }

    private ContentReference _sourceRef(String rawSrc, ErrorReportConfiguration errorReportConfiguration) {
        return ContentReference.construct(true, rawSrc, 0, rawSrc.length(),errorReportConfiguration);
    }

}
