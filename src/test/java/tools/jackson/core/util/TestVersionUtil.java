package tools.jackson.core.util;

import tools.jackson.core.json.PackageVersion;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;
import tools.jackson.core.json.UTF8JsonGenerator;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestVersionUtil extends tools.jackson.core.JUnit5TestBase
{
    @Test
    void versionPartParsing()
    {
        assertEquals(13, VersionUtil.parseVersionPart("13"));
        assertEquals(27, VersionUtil.parseVersionPart("27.8"));
        assertEquals(0, VersionUtil.parseVersionPart("-3"));
    }

    @Test
    void versionParsing()
    {
        assertEquals(new Version(1, 2, 15, "foo", "group", "artifact"),
                VersionUtil.parseVersion("1.2.15-foo", "group", "artifact"));
    }

    @Test
    void packageVersionMatches() {
        assertEquals(PackageVersion.VERSION, VersionUtil.versionFor(UTF8JsonGenerator.class));
    }

    // [core#248]: make sure not to return `null` but `Version.unknownVersion()`
    @Test
    void versionForUnknownVersion() {
        // expecting return version.unknownVersion() instead of null
        assertEquals(Version.unknownVersion(), VersionUtil.versionFor(TestVersionUtil.class));
    }
}
