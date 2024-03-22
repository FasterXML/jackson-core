package com.fasterxml.jackson.core;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.json.*;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.core.testsupport.TestSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to verify functioning of {@link Version} class.
 */
class ComponentVersionsTest
        extends JUnit5TestBase
{
    @Test
    void coreVersions() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        assertVersion(f.version());
        try (ReaderBasedJsonParser p = new ReaderBasedJsonParser(TestSupport.testIOContext(), 0, null, null,
                CharsToNameCanonicalizer.createRoot(f))) {
            assertVersion(p.version());
        }
        try (JsonGenerator g = new WriterBasedJsonGenerator(TestSupport.testIOContext(), 0, null, null, '"')) {
            assertVersion(g.version());
        }
    }

    @Test
    void equality() {
        Version unk = Version.unknownVersion();
        assertEquals("0.0.0", unk.toString());
        assertEquals("//0.0.0", unk.toFullString());
        assertEquals(unk, unk);

        Version other = new Version(2, 8, 4, "",
                "groupId", "artifactId");
        assertEquals("2.8.4", other.toString());
        assertEquals("groupId/artifactId/2.8.4", other.toFullString());

        // [core#1141]: Avoid NPE for snapshot-info
        Version unk2 = new Version(0, 0, 0, null, null, null);
        assertEquals(unk, unk2);
    }

    @Test
    void misc() {
        Version unk = Version.unknownVersion();
        int hash = unk.hashCode();
        // Happens to be 0 at this point (Jackson 2.16)
        assertEquals(0, hash);
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
