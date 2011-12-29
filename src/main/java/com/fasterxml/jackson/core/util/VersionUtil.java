package com.fasterxml.jackson.core.util;

import java.io.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.Version;

/**
 * Functionality for supporting exposing of component {@link Version}s.
 */
public class VersionUtil
{
    public final static String VERSION_FILE = "VERSION.txt";

    private final static Pattern VERSION_SEPARATOR = Pattern.compile("[-_./;:]");
    
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
