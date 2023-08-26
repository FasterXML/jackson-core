package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;

/**
 * Tests to verify functioning of {@link Version} class.
 */
public class TestVersions extends com.fasterxml.jackson.core.BaseTest
{
    public void testCoreVersions() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        assertVersion(f.version());
        try (ReaderBasedJsonParser p = new ReaderBasedJsonParser(testIOContext(), 0, null, null,
                CharsToNameCanonicalizer.createRoot(f))) {
            assertVersion(p.version());
        }
        try (JsonGenerator g = new WriterBasedJsonGenerator(testIOContext(), 0, null, null, '"')) {
            assertVersion(g.version());
        }
    }

    public void testMisc() {
        Version unk = Version.unknownVersion();
        assertEquals("0.0.0", unk.toString());
        assertEquals("//0.0.0", unk.toFullString());
        assertTrue(unk.equals(unk));

        Version other = new Version(2, 8, 4, "",
                "groupId", "artifactId");
        assertEquals("2.8.4", other.toString());
        assertEquals("groupId/artifactId/2.8.4", other.toFullString());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void assertVersion(Version v)
    {
        assertEquals(PackageVersion.VERSION, v);
    }
}
