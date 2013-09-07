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
    private final static Pattern VERSION_SEPARATOR = Pattern.compile("[-_./;:]");

    private final Version _version;

    /*
    /**********************************************************
    /* Instance life-cycle
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
            System.err.println("ERROR: Failed to load Version information from "+getClass());
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
    @SuppressWarnings("resource")
    public static Version versionFor(Class<?> cls)
    {
        Version packageVersion = packageVersionFor(cls);
        if (packageVersion != null) {
            return packageVersion;
        }
        final InputStream in = cls.getResourceAsStream("VERSION.txt");
        if (in == null) {
            return Version.unknownVersion();
        }
        try {
            InputStreamReader reader = new InputStreamReader(in, "UTF-8");
            return doReadVersion(reader);
        } catch (UnsupportedEncodingException e) {
            return Version.unknownVersion();
        } finally {
            _close(in);
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
        try {
            String versionInfoClassName = cls.getPackage().getName() + ".PackageVersion";
            Class<?> vClass = Class.forName(versionInfoClassName, true, cls.getClassLoader());
            // However, if class exists, it better work correctly, no swallowing exceptions
            try {
                return ((Versioned) vClass.newInstance()).version();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to get Versioned out of "+vClass);
            }
        } catch (Exception e) { // ok to be missing (not good, acceptable)
            return null;
        }
    }

    private static Version doReadVersion(final Reader reader)
    {
        String version = null, group = null, artifact = null;

        final BufferedReader br = new BufferedReader(reader);
        try {
            version = br.readLine();
            if (version != null) {
                group = br.readLine();
                if (group != null) {
                    artifact = br.readLine();
                }
            }
        } catch (IOException ignored) {
        } finally {
            _close(br);
        }
        // We don't trim() version: parseVersion() takes care ot that
        if (group != null) {
            group = group.trim();
        }
        if (artifact != null) {
            artifact = artifact.trim();
        }
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
    @SuppressWarnings("resource")
    public static Version mavenVersionFor(ClassLoader classLoader, String groupId, String artifactId)
    {
        InputStream pomProperties = classLoader.getResourceAsStream("META-INF/maven/"
                + groupId.replaceAll("\\.", "/")+ "/" + artifactId + "/pom.properties");
        if (pomProperties != null) {
            try {
                Properties props = new Properties();
                props.load(pomProperties);
                String versionStr = props.getProperty("version");
                String pomPropertiesArtifactId = props.getProperty("artifactId");
                String pomPropertiesGroupId = props.getProperty("groupId");
                return parseVersion(versionStr, pomPropertiesGroupId, pomPropertiesArtifactId);
            } catch (IOException e) {
                // Ignore
            } finally {
                _close(pomProperties);
            }
        }
        return Version.unknownVersion();
    }

    public static Version parseVersion(String versionStr, String groupId, String artifactId)
    {
        if (versionStr != null && (versionStr = versionStr.trim()).length() > 0) {
            String[] parts = VERSION_SEPARATOR.split(versionStr);
            return new Version(parseVersionPart(parts[0]),
                    (parts.length > 1) ? parseVersionPart(parts[1]) : 0,
                    (parts.length > 2) ? parseVersionPart(parts[2]) : 0,
                    (parts.length > 3) ? parts[3] : null,
                    groupId, artifactId);
        }
        return null;
    }

    protected static int parseVersionPart(String partStr)
    {
        int number = 0;
        for (int i = 0, len = partStr.length(); i < len; ++i) {
            char c = partStr.charAt(i);
            if (c > '9' || c < '0') break;
            number = (number * 10) + (c - '0');
        }
        return number;
    }

    private final static void _close(Closeable c) {
        try {
            c.close();
        } catch (IOException e) { }
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
