package com.fasterxml.jackson.core.util;

import java.io.*;
import java.util.Properties;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.Version;

/**
 * Functionality for supporting exposing of component {@link Version}s.
 *<p>
 * Note that this class can be used in two roles: first, as a static
 * utility class for loading purposes, and second, as a singleton
 * loader of per-module version information.
 * In latter case one must sub-class to get proper per-module instance;
 * and sub-class must reside in same Java package as matching "VERSION.txt"
 * file.
 */
public class VersionUtil
{
    public final static String VERSION_FILE = "VERSION.txt";

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
     * class. Implementation is simple: class loader that loaded specified
     * class is asked to load resource with name "VERSION" from same
     * location (package) as class itself had.
     * If no version information is found, {@link Version#unknownVersion()} is
     * returned.
     */
    public static Version versionFor(Class<?> cls)
    {
        InputStream in;
        Version version = null;

        try {
            in = cls.getResourceAsStream(VERSION_FILE);
            if (in != null) {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                    String groupStr = null, artifactStr = null;
                    String versionStr = br.readLine();
                    if (versionStr != null) {
                        groupStr = br.readLine();
                        if (groupStr != null) {
                            groupStr = groupStr.trim();
                            artifactStr = br.readLine();
                            if (artifactStr != null) {
                                artifactStr = artifactStr.trim();
                            }
                        }
                    }
                    version = parseVersion(versionStr, groupStr, artifactStr);
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } catch (IOException e) { }
        return (version == null) ? Version.unknownVersion() : version;
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
}
