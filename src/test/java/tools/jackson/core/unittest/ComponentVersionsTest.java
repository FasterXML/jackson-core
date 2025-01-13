package tools.jackson.core.unittest;

import java.io.*;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.PackageVersion;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to verify functioning of {@link Version} class.
 */
class ComponentVersionsTest
    extends JacksonCoreTestBase
{
    @Test
    void coreVersions() throws Exception
    {
        final JsonFactory f = new JsonFactory();
        assertVersion(f.version());
        try (JsonParser jp =  f.createParser(ObjectReadContext.empty(),
                new StringReader("true"))) {
            assertVersion(jp.version());
        }
        try (JsonGenerator jg = f.createGenerator(ObjectWriteContext.empty(),
                new ByteArrayOutputStream())) {
            assertVersion(jg.version());
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
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private void assertVersion(Version v)
    {
        assertEquals(PackageVersion.VERSION, v);
    }
}
