package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.Version;

public class TestVersionUtil extends com.fasterxml.jackson.test.BaseTest
{
    public void testVersionPartParsing()
    {
        assertEquals(13, VersionUtil.parseVersionPart("13"));
        assertEquals(27, VersionUtil.parseVersionPart("27.8"));
        assertEquals(0, VersionUtil.parseVersionPart("-3"));
    }

    public void testVersionParsing()
    {
        assertEquals(new Version(1, 2, 15, "foo"), VersionUtil.parseVersion("1.2.15-foo"));
    }

    public void testMavenVersionParsing() {
        assertEquals(new Version(1, 2, 3, "SNAPSHOT", "foo.bar", "foo-bar"),
                VersionUtil.mavenVersionFor(TestVersionUtil.class.getClassLoader(), "foo.bar", "foo-bar"));
    }
}
