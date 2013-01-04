package com.fasterxml.jackson.core.test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;

public class TestPackageVersion extends com.fasterxml.jackson.test.BaseTest
{
    public void testPackageVersion()
    {
        Version expected = new Version(23, 42, 64738, "foobar", "foobar-group", "foobar-artifact");
        assertEquals(expected, VersionUtil.packageVersionFor(this.getClass()));
        assertEquals(expected, VersionUtil.versionFor(this.getClass()));
    }
}
