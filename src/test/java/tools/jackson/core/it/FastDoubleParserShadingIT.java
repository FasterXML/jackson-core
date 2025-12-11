package tools.jackson.core.it;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for [core#1498]: FastDoubleParser classes must be properly
 * shaded in the Jackson JAR to prevent {@code NoClassDefFoundError} when
 * Jackson's JAR loads before the FastDoubleParser JAR on the classpath.
 * <p>
 * This test verifies the packaged JAR to ensure that:
 * <ul>
 * <li>No unshaded {@code ch.randelshofer.fastdoubleparser} classes exist in base package</li>
 * <li>Multi-version JAR entries (JDK 17, 21, 22, 23+) have properly shaded paths</li>
 * <li>All FastDoubleParser classes are relocated to {@code tools.jackson.core.internal.shaded.fdp}</li>
 * </ul>
 *
 * @see <a href="https://github.com/FasterXML/jackson-core/issues/1498">[core#1498]</a>
 */
public class FastDoubleParserShadingIT
{
    private static final String UNSHADED_PACKAGE_PREFIX = "ch/randelshofer/fastdoubleparser/";
    private static final String SHADED_PACKAGE_PREFIX = "tools/jackson/core/internal/shaded/fdp/";

    /**
     * Test that verifies no unshaded FastDoubleParser classes exist in the
     * Jackson JAR, including in multi-version JAR directories.
     */
    @Test
    public void verifyNoUnshadedFDPClasses() throws Exception
    {
        File jarFile = findJacksonCoreJar();

        assertNotNull(jarFile, "Could not locate jackson-core JAR file");
        assertTrue(jarFile.exists(), "JAR file does not exist: " + jarFile);
        assertTrue(jarFile.isFile(), "Not a file: " + jarFile);

        // Parse the JAR to check for unshaded classes
        List<String> unshadedClasses = new ArrayList<>();
        List<String> shadedClasses = new ArrayList<>();
        List<String> multiVersionUnshadedClasses = new ArrayList<>();
        List<String> multiVersionShadedClasses = new ArrayList<>();

        try (InputStream is = new FileInputStream(jarFile);
             JarInputStream jarStream = new JarInputStream(is)) {

            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                String entryName = entry.getName();

                // Check for unshaded classes in base location
                if (entryName.startsWith(UNSHADED_PACKAGE_PREFIX) && entryName.endsWith(".class")) {
                    unshadedClasses.add(entryName);
                }

                // Check for unshaded classes in multi-version JAR directories
                // Pattern: META-INF/versions/{version}/ch/randelshofer/fastdoubleparser/...
                if (entryName.startsWith("META-INF/versions/") &&
                    entryName.contains("/" + UNSHADED_PACKAGE_PREFIX) &&
                    entryName.endsWith(".class")) {
                    multiVersionUnshadedClasses.add(entryName);
                }

                // Track properly shaded classes for verification
                if (entryName.startsWith(SHADED_PACKAGE_PREFIX) && entryName.endsWith(".class")) {
                    shadedClasses.add(entryName);
                }

                // Also check multi-version shaded classes
                if (entryName.startsWith("META-INF/versions/") &&
                    entryName.contains("/" + SHADED_PACKAGE_PREFIX) &&
                    entryName.endsWith(".class")) {
                    multiVersionShadedClasses.add(entryName);
                }
            }
        }

        // Report findings for debugging
        System.out.println("=== FastDoubleParser Shading Verification Report ===");
        System.out.println("JAR: " + jarFile.getName());
        System.out.println("Shaded classes found: " + shadedClasses.size());
        System.out.println("Multi-version shaded classes found: " + multiVersionShadedClasses.size());

        if (!shadedClasses.isEmpty()) {
            System.out.println("\nShaded classes (sample):");
            shadedClasses.stream().limit(5).forEach(name -> System.out.println("  ✓ " + name));
        }

        if (!multiVersionShadedClasses.isEmpty()) {
            System.out.println("\nMulti-version shaded classes:");
            multiVersionShadedClasses.forEach(name -> System.out.println("  ✓ " + name));
        }

        // Verify no unshaded classes exist in base location
        if (!unshadedClasses.isEmpty()) {
            System.err.println("\n✗ FAILED: Found unshaded FastDoubleParser classes in base location:");
            unshadedClasses.forEach(name -> System.err.println("  ✗ " + name));
            fail("Found " + unshadedClasses.size() + " unshaded FastDoubleParser classes in base location. " +
                 "These should be shaded to '" + SHADED_PACKAGE_PREFIX + "'");
        }

        // Verify no unshaded classes exist in multi-version directories
        if (!multiVersionUnshadedClasses.isEmpty()) {
            System.err.println("\n✗ FAILED: Found unshaded FastDoubleParser classes in multi-version JAR:");
            multiVersionUnshadedClasses.forEach(name -> System.err.println("  ✗ " + name));
            fail("Found " + multiVersionUnshadedClasses.size() + " unshaded FastDoubleParser classes " +
                 "in multi-version JAR directories. These should be shaded to '" + SHADED_PACKAGE_PREFIX + "'");
        }

        // Verify that shaded classes DO exist (sanity check that shading happened)
        assertFalse(shadedClasses.isEmpty(),
            "No shaded FastDoubleParser classes found. Expected classes under '" +
            SHADED_PACKAGE_PREFIX + "'. Shading may have failed.");

        // Additional verification: check that expected core classes are present
        assertTrue(shadedClasses.stream()
            .anyMatch(name -> name.contains("FastIntegerMath") || name.contains("FastDoubleMath")),
            "Expected to find core FastDoubleParser classes like FastIntegerMath or FastDoubleMath");

        System.out.println("\n✓ All FastDoubleParser classes are properly shaded!");
        System.out.println("=====================================================");
    }

    /**
     * Finds the jackson-core JAR file in the target directory.
     */
    private File findJacksonCoreJar() {
        // Look in target directory for the built JAR
        File targetDir = new File("target");
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return null;
        }

        // Find JAR files matching jackson-core pattern
        File[] jarFiles = targetDir.listFiles((dir, name) ->
            name.startsWith("jackson-core-") &&
            name.endsWith(".jar") &&
            !name.contains("sources") &&
            !name.contains("javadoc") &&
            !name.contains("tests"));

        if (jarFiles == null || jarFiles.length == 0) {
            return null;
        }

        // Return the first matching JAR (there should only be one)
        return jarFiles[0];
    }
}
