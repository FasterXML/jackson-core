package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.Version;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link VersionUtil}.
 *
 * @date 2017-07-31
 * @see VersionUtil
 **/
public class VersionUtilTest {

  @Test
  public void testParseVersionPartReturningPositive() {
    assertEquals(66, VersionUtil.parseVersionPart("66R"));
  }

  @Test
  public void testParseVersionReturningVersionWhereGetMajorVersionIsZero() {
    Version version = VersionUtil.parseVersion("#M&+m@569P", "#M&+m@569P", "com.fasterxml.jackson.core.util.VersionUtil");

    assertEquals(0, version.getMinorVersion());
    assertEquals(0, version.getPatchLevel());
    assertEquals(0, version.getMajorVersion());
    assertFalse(version.isSnapshot());
    assertFalse(version.isUnknownVersion());
  }

  @Test
  public void testParseVersionWithEmptyStringAndEmptyString() {
    Version version = VersionUtil.parseVersion("", "", "\"g2AT");

    assertTrue(version.isUnknownVersion());
  }

  @Test
  public void testParseVersionWithNullAndEmptyString() {
    Version version = VersionUtil.parseVersion(null, "/nUmRN)3", "");

    assertFalse(version.isSnapshot());
  }

}