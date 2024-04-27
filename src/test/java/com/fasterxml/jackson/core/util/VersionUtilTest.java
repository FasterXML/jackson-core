package com.fasterxml.jackson.core.util;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.core.json.UTF8JsonGenerator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link VersionUtil}.
 *
 * @see VersionUtil
 */
class VersionUtilTest
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
        Version v = VersionUtil.parseVersion("1.2.3-SNAPSHOT", "group", "artifact");
        assertEquals("group/artifact/1.2.3-SNAPSHOT", v.toFullString());
    }

    @Test
    void parseVersionPartReturningPositive() {
        assertEquals(66, VersionUtil.parseVersionPart("66R"));
    }

    @Test
    void parseVersionReturningVersionWhereGetMajorVersionIsZero() {
        Version version = VersionUtil.parseVersion("#M&+m@569P", "#M&+m@569P", "com.fasterxml.jackson.core.util.VersionUtil");

        assertEquals(0, version.getMinorVersion());
        assertEquals(0, version.getPatchLevel());
        assertEquals(0, version.getMajorVersion());
        assertFalse(version.isSnapshot());
        assertFalse(version.isUnknownVersion());
    }

    @Test
    void parseVersionWithEmptyStringAndEmptyString() {
        Version version = VersionUtil.parseVersion("", "", "\"g2AT");
        assertTrue(version.isUnknownVersion());
    }

    @Test
    void parseVersionWithNullAndEmptyString() {
        Version version = VersionUtil.parseVersion(null, "/nUmRN)3", "");

        assertFalse(version.isSnapshot());
    }

    @Test
    void packageVersionMatches() {
        assertEquals(PackageVersion.VERSION, VersionUtil.versionFor(UTF8JsonGenerator.class));
    }

    // [core#248]: make sure not to return `null` but `Version.unknownVersion()`
    @Test
    void versionForUnknownVersion() {
        // expecting return version.unknownVersion() instead of null
        assertEquals(Version.unknownVersion(), VersionUtil.versionFor(VersionUtilTest.class));
    }

    // // // Deprecated functionality

    @SuppressWarnings("deprecation")
    @Test
    void mavenVersionParsing() {
        assertEquals(new Version(1, 2, 3, "SNAPSHOT", "foo.bar", "foo-bar"),
                VersionUtil.mavenVersionFor(VersionUtilTest.class.getClassLoader(), "foo.bar", "foo-bar"));
    }
}
