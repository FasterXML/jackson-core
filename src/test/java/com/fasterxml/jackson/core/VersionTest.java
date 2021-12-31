package com.fasterxml.jackson.core;

/**
 * Unit tests for class {@link Version}.
 */
public class VersionTest extends BaseTest
{
  public void testCompareToOne() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, -263, -1820, "", "", "");

      assertEquals(263, version.compareTo(versionTwo));
  }

  public void testCompareToReturningZero() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, 0, 0, "", "", "");

      assertEquals(0, version.compareTo(versionTwo));
  }

  public void testCreatesVersionTaking6ArgumentsAndCallsCompareTo() {
      Version version = new Version(0, 0, 0, null, null, "");
      Version versionTwo = new Version(0, 0, 0, "", "", "//0.0.0");

      assertTrue(version.compareTo(versionTwo) < 0);
  }

  public void testCompareToTwo() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(-1, 0, 0, "SNAPSHOT", "groupId", "artifactId");

      int diff = version.compareTo(versionTwo);
      assertTrue("Diff should be negative, was: "+diff, diff < 0);
  }

  public void testCompareToAndCreatesVersionTaking6ArgumentsAndUnknownVersion() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, 0, 0, "SNAPSHOT", "groupId", "artifactId");

      assertTrue(version.compareTo(versionTwo) < 0);
  }
}
