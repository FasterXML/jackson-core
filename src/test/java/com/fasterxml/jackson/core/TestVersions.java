package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.util.BufferRecycler;

/**
 * Tests to verify [JACKSON-278]
 */
public class TestVersions extends com.fasterxml.jackson.test.BaseTest
{
    /**
     * 18-Nov-2010, tatu: Not a good to do this, but has to do, for now...
     */
    private final static int MAJOR_VERSION = 2;
    private final static int MINOR_VERSION = 0;
    
    public void testCoreVersions()
    {
        /* 01-Sep-2010, tatu: Somewhat of a dirty hack; let's only run when specific system
         *    property is set; and set that flag from Ant unit test. Why? To prevent running
         *    from Eclipse, where this would just fail
         */
        if (runsFromAnt()) {
            System.out.println("Note: running version tests (FROM_ANT=true)");
            assertVersion(new JsonFactory().version(), MAJOR_VERSION, MINOR_VERSION);
            assertVersion(new ReaderBasedJsonParser(getIOContext(), 0, null, null, null).version(),
                    MAJOR_VERSION, MINOR_VERSION);
            assertVersion(new WriterBasedJsonGenerator(getIOContext(), 0, null, null).version(),
                    MAJOR_VERSION, MINOR_VERSION);
        } else {
            System.out.println("Skipping version test (FROM_ANT=false)");
        }
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private void assertVersion(Version v, int major, int minor)
    {
        assertFalse("Should find version information (got "+v+")", v.isUknownVersion());
        assertEquals(major, v.getMajorVersion());
        assertEquals(minor, v.getMinorVersion());
        // 07-Jan-2011, tatus: Check patch level initially, comment out for maint versions

        //assertEquals(0, v.getPatchLevel());
    }

    private IOContext getIOContext() {
        return new IOContext(new BufferRecycler(), null, false);
    }
}

