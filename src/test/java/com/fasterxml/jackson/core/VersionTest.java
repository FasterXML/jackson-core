package com.fasterxml.jackson.core;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for class {@link Version}.
 *
 **/
public class VersionTest{

  @Test
  public void testCompareToOne() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, (-263), (-1820), "",
              "", "");

      assertEquals(263, version.compareTo(versionTwo));
  }

  @Test
  public void testCompareToReturningZero() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, 0, 0, "",
              "", "");

      assertEquals(0, version.compareTo(versionTwo));
  }

  @Test
  public void testCreatesVersionTaking6ArgumentsAndCallsCompareTo() {
      Version version = new Version(0, 0, 0, null, null, "");
      Version versionTwo = new Version(0, 0, 0, "", "", "//0.0.0");

      assertTrue(version.compareTo(versionTwo) < 0);
  }

  @Test
  public void testCompareToTwo() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version((-1), 0, 0, "0.0.0",
              "", "");

      assertTrue(version.compareTo(versionTwo) > 0);
  }

  @Test
  public void testCompareToAndCreatesVersionTaking6ArgumentsAndUnknownVersion() {
      Version version = Version.unknownVersion();
      Version versionTwo = new Version(0, 0, 0, "//0.0.0", "//0.0.0", "");

      assertTrue(version.compareTo(versionTwo) < 0);
  }

  @Test
  public void testCompareToSnapshotSame() {
      Version version = new Version(0, 0, 0, "alpha");
      Version versionTwo = new Version(0, 0, 0, "alpha");

      assertEquals(0, version.compareTo(versionTwo));
  }

  @Test
  public void testCompareToSnapshotDifferent() {
      Version version = new Version(0, 0, 0, "alpha");
      Version versionTwo = new Version(0, 0, 0, "beta");

      assertTrue(version.compareTo(versionTwo) < 0);
  }

  @Test
  public void testCompareWhenOnlyFirstHasSnapshot() {
      Version version = new Version(0, 0, 0, "beta");
      Version versionTwo = new Version(0, 0, 0, null);

      assertEquals(-1, version.compareTo(versionTwo));
  }

  @Test
  public void testCompareWhenOnlySecondHasSnapshot() {
      Version version = new Version(0, 0, 0, "");
      Version versionTwo = new Version(0, 0, 0, "beta");

      assertEquals(1, version.compareTo(versionTwo));
  }
}
