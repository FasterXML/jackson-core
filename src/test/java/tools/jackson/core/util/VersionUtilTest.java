package tools.jackson.core.util;

import org.junit.jupiter.api.Test;

import tools.jackson.core.Version;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link VersionUtil}.
 *
 * @see VersionUtil
 */
class VersionUtilTest
{
    @Test
    void parseVersionSimple() {
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

}