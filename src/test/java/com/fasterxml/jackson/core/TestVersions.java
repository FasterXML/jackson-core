package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

/**
 * Tests to verify [JACKSON-278]
 */
public class TestVersions extends com.fasterxml.jackson.test.BaseTest
{
    // 18-Nov-2010, tatu: Not a good to do this, but has to do, for now...
    private final static int MAJOR_VERSION = 2;
    private final static int MINOR_VERSION = 0;
    
    private final static String GROUP_ID = "com.fasterxml.jackson.core";
    private final static String ARTIFACT_ID = "jackson-core";
    
    public void testCoreVersions()
    {
        assertVersion(new JsonFactory().version());
        assertVersion(new ReaderBasedJsonParser(getIOContext(), 0, null, null, null).version());
        assertVersion(new WriterBasedJsonGenerator(getIOContext(), 0, null, null).version());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Version v)
    {
        assertFalse("Should find version information (got "+v+")", v.isUknownVersion());
        assertEquals(MAJOR_VERSION, v.getMajorVersion());
        assertEquals(MINOR_VERSION, v.getMinorVersion());
        // Check patch level initially, comment out for maint versions
        assertEquals(0, v.getPatchLevel());

        // also, group & artifact ids should match:
        assertEquals(GROUP_ID, v.getGroupId());
        assertEquals(ARTIFACT_ID, v.getArtifactId());
    }

    private IOContext getIOContext() {
        return new IOContext(new BufferRecycler(), null, false);
    }
}

