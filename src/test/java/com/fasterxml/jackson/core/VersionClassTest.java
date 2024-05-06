package com.fasterxml.jackson.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for class {@link Version}.
 */
class VersionClassTest
        extends JUnit5TestBase
{
    @Test
    void equalsAndHashCode() {
        Version version1 = new Version(1, 2, 3, "", "", "");
        Version version2 = new Version(1, 2, 3, "", "", "");

        assertEquals(version1, version2);
        assertEquals(version2, version1);

        assertEquals(version1.hashCode(), version2.hashCode());
    }

    @Test
    void compareToOne() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, (-263), (-1820), "",
              "", "");

      assertEquals(263, version.compareTo(versionTwo));
  }

    @Test
    void compareToReturningZero() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, 0, 0, "",
              "", "");

      assertEquals(0, version.compareTo(versionTwo));
  }

    @Test
    void createsVersionTaking6ArgumentsAndCallsCompareTo() {
      Version version = new Version(0, 0, 0, null, null, "");
      Version versionTwo = new Version(0, 0, 0, "", "", "//0.0.0");

      assertTrue(version.compareTo(versionTwo) < 0);
  }

    @Test
    void compareToTwo() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version((-1), 0, 0, "0.0.0",
              "", "");

      assertTrue(version.compareTo(versionTwo) > 0);
  }

    @Test
    void compareToAndCreatesVersionTaking6ArgumentsAndUnknownVersion() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, 0, 0, "//0.0.0", "//0.0.0", "");

      assertTrue(version.compareTo(versionTwo) < 0);
  }

    @Test
    void compareToSnapshotSame() {
      Version version = new Version(0, 0, 0, "alpha", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, "alpha", "com.fasterxml", "bogus");

      assertEquals(0, version.compareTo(versionTwo));
  }

    @Test
    void compareToSnapshotDifferent() {
      Version version = new Version(0, 0, 0, "alpha", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, "beta", "com.fasterxml", "bogus");

      assertTrue(version.compareTo(versionTwo) < 0);
      assertTrue(versionTwo.compareTo(version) > 0);
  }

    @Test
    void compareWhenOnlyFirstHasSnapshot() {
      Version version = new Version(0, 0, 0, "beta", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, null, "com.fasterxml", "bogus");

      assertTrue(version.compareTo(versionTwo) < 0);
      assertTrue(versionTwo.compareTo(version) > 0);
  }

    @Test
    void compareWhenOnlySecondHasSnapshot() {
      Version version = new Version(0, 0, 0, "", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, "beta", "com.fasterxml", "bogus");

      assertTrue(version.compareTo(versionTwo) > 0);
      assertTrue(versionTwo.compareTo(version) < 0);
  }
}
