package tools.jackson.core;

/**
 * Unit tests for class {@link Version}.
 */
public class VersionTest extends BaseTest
{
  public void testEqualsAndHashCode() {
      Version version1 = new Version(1, 2, 3, "", "", "");
      Version version2 = new Version(1, 2, 3, "", "", "");

      assertEquals(version1, version2);
      assertEquals(version2, version1);

      assertEquals(version1.hashCode(), version2.hashCode());
  }

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

  public void testCompareToSnapshotSame() {
      Version version = new Version(0, 0, 0, "alpha", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, "alpha", "com.fasterxml", "bogus");

      assertEquals(0, version.compareTo(versionTwo));
  }

  public void testCompareToSnapshotDifferent() {
      Version version = new Version(0, 0, 0, "alpha", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, "beta", "com.fasterxml", "bogus");

      assertTrue(version.compareTo(versionTwo) < 0);
      assertTrue(versionTwo.compareTo(version) > 0);
  }

  public void testCompareWhenOnlyFirstHasSnapshot() {
      Version version = new Version(0, 0, 0, "beta", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, null, "com.fasterxml", "bogus");

      assertTrue(version.compareTo(versionTwo) < 0);
      assertTrue(versionTwo.compareTo(version) > 0);
  }

  public void testCompareWhenOnlySecondHasSnapshot() {
      Version version = new Version(0, 0, 0, "", "com.fasterxml", "bogus");
      Version versionTwo = new Version(0, 0, 0, "beta", "com.fasterxml", "bogus");

      assertTrue(version.compareTo(versionTwo) > 0);
      assertTrue(versionTwo.compareTo(version) < 0);
  }
}
