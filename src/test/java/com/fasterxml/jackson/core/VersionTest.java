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
}
