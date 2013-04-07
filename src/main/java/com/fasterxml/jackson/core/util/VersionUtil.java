package com.fasterxml.jackson.core.util;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;

/**
 * Functionality for supporting exposing of component {@link Version}s.
 * Also contains other misc methods that have no other place to live in.
 *<p>
 * Note that this class can be used in two roles: first, as a static
 * utility class for loading purposes, and second, as a singleton
 * loader of per-module version information.
 *<p>
 * Note that method for accessing version information changed between versions
 * 2.1 and 2.2; earlier code used file named "VERSION.txt"; but this has serious
 * performance issues on some platforms (Android), so a replacement system
 * was implemented to use class generation and dynamic class loading.
 */
public class VersionUtil
{
    /**
     * @deprecated Since 2.2, use of version file is deprecated, and generated
     *    class should be used instead.
     */
    @Deprecated
    public final static String VERSION_FILE = "VERSION.txt";
    public final static String PACKAGE_VERSION_CLASS_NAME = "PackageVersion";
//    public final static String PACKAGE_VERSION_FIELD = "VERSION";

    private final static Pattern VERSION_SEPARATOR = Pattern.compile("[-_./;:]");

    private final Version _version;

    /*
    /**********************************************************
    /* Instance life-cycle, accesso
    /**********************************************************
     */
    
    protected VersionUtil()
    {
        Version v = null;
        try {
            /* Class we pass only matters for resource-loading: can't use this Class
             * (as it's just being loaded at this point), nor anything that depends on it.
             */
            v = VersionUtil.versionFor(getClass());
        } catch (Exception e) { // not good to dump to stderr; but that's all we have at this low level
            System.err.println("ERROR: Failed to load Version information for bundle (via "+getClass().getName()+").");
        }
        if (v == null) {
            v = Version.unknownVersion();
        }
        _version = v;
    }

    public Version version() { return _version; }
    
    /*
    /**********************************************************
    /* Static load methods
    /**********************************************************
     */
    
    /**
     * Helper method that will try to load version information for specified
     * class. Implementation is as follows:
     *
     * First, tries to load version info from a class named
     * "PackageVersion" in the same package as the class.
     *
     * Next, if that fails, class loader that loaded specified class is
     * asked to load resource with name "VERSION" from same location
     * (package) as class itself had.
     *
     * If no version information is found, {@link Version#unknownVersion()} is returned.
     */
    public static Version versionFor(Class<?> cls)
    {
        Version packageVersion = packageVersionFor(cls);
        if (packageVersion != null) {
            return packageVersion;
        }

        final InputStream in = cls.getResourceAsStream(VERSION_FILE);

        if (in == null)
            return Version.unknownVersion();

        try {
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            try {
                return doReadVersion(reader);
            } finally {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        } catch (UnsupportedEncodingException e) {
            return Version.unknownVersion();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Loads version information by introspecting a class named
     * "PackageVersion" in the same package as the given class.
     *
     * If the class could not be found or does not have a public
     * static Version field named "VERSION", returns null.
     */
    public static Version packageVersionFor(Class<?> cls)
    {
        Class<?> versionInfoClass = null;
        try {
            Package p = cls.getPackage();
            String versionInfoClassName =
                new StringBuilder(p.getName())
                    .append(".")
                    .append(PACKAGE_VERSION_CLASS_NAME)
                    .toString();
            versionInfoClass = Class.forName(versionInfoClassName, true, cls.getClassLoader());
        } catch (Exception e) { // ok to be missing (not good, acceptable)
            return null;
        }
        if (versionInfoClass == null) {
            return null;
        }
        // However, if class exists, it better work correctly, no swallowing exceptions
        Object v;
        try {
            v = versionInfoClass.newInstance();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate "+versionInfoClass.getName()
                    +" to find version information, problem: "+e.getMessage(), e);
        }
        if (!(v instanceof Versioned)) {
            throw new IllegalArgumentException("Bad version class "+versionInfoClass.getName()
        			+": does not implement "+Versioned.class.getName());
        }
        return ((Versioned) v).version();
    }

    private static Version doReadVersion(final Reader reader)
    {
        String version = null, group = null, artifact = null;

        final BufferedReader br = new BufferedReader(reader);
        try {
            version = br.readLine();
            if (version != null) {
                group = br.readLine();
                if (group != null)
                    artifact = br.readLine();
            }
        } catch (IOException ignored) {
        } finally {
            try {
                br.close();
            } catch (IOException ignored) {
            }
        }

        // We don't trim() version: parseVersion() takes care ot that
        if (group != null)
            group = group.trim();
        if (artifact != null)
            artifact = artifact.trim();
        return parseVersion(version, group, artifact);
    }

    /**
     * Will attempt to load the maven version for the given groupId and
     * artifactId.  Maven puts a pom.properties file in
     * META-INF/maven/groupId/artifactId, containing the groupId,
     * artifactId and version of the library.
     *
     * @param classLoader the ClassLoader to load the pom.properties file from
     * @param groupId the groupId of the library
     * @param artifactId the artifactId of the library
     * @return The version
     */
    public static Version mavenVersionFor(ClassLoader classLoader, String groupId, String artifactId) {
        InputStream pomPoperties = classLoader.getResourceAsStream("META-INF/maven/" + groupId.replaceAll("\\.", "/")
                + "/" + artifactId + "/pom.properties");
        if (pomPoperties != null) {
            try {
                Properties props = new Properties();
                props.load(pomPoperties);
                String versionStr = props.getProperty("version");
                String pomPropertiesArtifactId = props.getProperty("artifactId");
                String pomPropertiesGroupId = props.getProperty("groupId");
                return parseVersion(versionStr, pomPropertiesGroupId, pomPropertiesArtifactId);
            } catch (IOException e) {
                // Ignore
            } finally {
                try {
                    pomPoperties.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return Version.unknownVersion();
    }

    /**
     * Use variant that takes three arguments instead
     * 
     * @deprecated
     */
    @Deprecated
    public static Version parseVersion(String versionStr) {
        return parseVersion(versionStr, null, null);
    }

    public static Version parseVersion(String versionStr, String groupId, String artifactId)
    {
        if (versionStr == null) {
            return null;
        }
        versionStr = versionStr.trim();
        if (versionStr.length() == 0) {
            return null;
        }
        String[] parts = VERSION_SEPARATOR.split(versionStr);
        int major = parseVersionPart(parts[0]);
        int minor = (parts.length > 1) ? parseVersionPart(parts[1]) : 0;
        int patch = (parts.length > 2) ? parseVersionPart(parts[2]) : 0;
        String snapshot = (parts.length > 3) ? parts[3] : null;

        return new Version(major, minor, patch, snapshot,
                groupId, artifactId);
    }

    protected static int parseVersionPart(String partStr)
    {
        partStr = partStr.toString();
        int len = partStr.length();
        int number = 0;
        for (int i = 0; i < len; ++i) {
            char c = partStr.charAt(i);
            if (c > '9' || c < '0') break;
            number = (number * 10) + (c - '0');
        }
        return number;
    }

    /*
    /**********************************************************
    /* Orphan utility methods
    /**********************************************************
     */

    public final static void throwInternal() {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }
}
