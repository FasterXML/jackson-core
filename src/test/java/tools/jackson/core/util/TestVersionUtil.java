package tools.jackson.core.util;

import tools.jackson.core.json.PackageVersion;

import tools.jackson.core.Version;
import tools.jackson.core.json.UTF8JsonGenerator;

public class TestVersionUtil extends tools.jackson.core.BaseTest
{
    public void testVersionPartParsing()
    {
        assertEquals(13, VersionUtil.parseVersionPart("13"));
        assertEquals(27, VersionUtil.parseVersionPart("27.8"));
        assertEquals(0, VersionUtil.parseVersionPart("-3"));
    }

    public void testVersionParsing()
    {
        assertEquals(new Version(1, 2, 15, "foo", "group", "artifact"),
                VersionUtil.parseVersion("1.2.15-foo", "group", "artifact"));
    }

    public void testPackageVersionMatches() {
        assertEquals(PackageVersion.VERSION, VersionUtil.versionFor(UTF8JsonGenerator.class));
    }

    // [core#248]: make sure not to return `null` but `Version.unknownVersion()`
    public void testVersionForUnknownVersion() {
        // expecting return version.unknownVersion() instead of null
        assertEquals(Version.unknownVersion(), VersionUtil.versionFor(TestVersionUtil.class));
    }
}
