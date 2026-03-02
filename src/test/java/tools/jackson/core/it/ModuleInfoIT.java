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
 * Integration test for [core#1380]: {@code module-info.class} must be placed
 * at the root of the JAR ({@code /module-info.class}), not under
 * {@code META-INF/versions/}.
 *
 * @see <a href="https://github.com/FasterXML/jackson-core/issues/1380">[core#1380]</a>
 *
 * @since 3.1.1
 */
public class ModuleInfoIT
{
    @Test
    public void moduleInfoAtRootLevel() throws Exception
    {
        File jarFile = findJacksonCoreJar();
        assertNotNull(jarFile, "Could not locate jackson-core JAR file");
        assertTrue(jarFile.exists(), "JAR file does not exist: " + jarFile);

        boolean rootModuleInfo = false;
        List<String> versionedModuleInfos = new ArrayList<>();

        try (InputStream is = new FileInputStream(jarFile);
             JarInputStream jarStream = new JarInputStream(is)) {
            JarEntry entry;
            while ((entry = jarStream.getNextJarEntry()) != null) {
                String name = entry.getName();
                if (name.equals("module-info.class")) {
                    rootModuleInfo = true;
                } else if (name.startsWith("META-INF/versions/") && name.endsWith("/module-info.class")) {
                    versionedModuleInfos.add(name);
                }
            }
        }

        assertTrue(rootModuleInfo,
            "module-info.class must be at root of JAR (not only under META-INF/versions/)");
        assertTrue(versionedModuleInfos.isEmpty(),
            "module-info.class must NOT be under META-INF/versions/; found: " + versionedModuleInfos);
    }

    private File findJacksonCoreJar() {
        File targetDir = new File("target");
        if (!targetDir.exists() || !targetDir.isDirectory()) {
            return null;
        }
        File[] jarFiles = targetDir.listFiles((dir, name) ->
            name.startsWith("jackson-core-") &&
            name.endsWith(".jar") &&
            !name.contains("sources") &&
            !name.contains("javadoc") &&
            !name.contains("tests"));
        if (jarFiles == null || jarFiles.length == 0) {
            return null;
        }
        return jarFiles[0];
    }
}
