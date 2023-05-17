package tools.jackson.core.util;

import java.util.regex.Pattern;

import tools.jackson.core.Version;
import tools.jackson.core.Versioned;

/**
 * Functionality for supporting exposing of component {@link Version}s.
 * Also contains other misc methods that have no other place to live in.
 *<p>
 * Note that this class can be used in two roles: first, as a static
 * utility class for loading purposes, and second, as a singleton
 * loader of per-module version information.
 */
public class VersionUtil
{
    private final static Pattern V_SEP = Pattern.compile("[-_./;:]");

    /*
    /**********************************************************************
    /* Instance life-cycle
    /**********************************************************************
     */

    protected VersionUtil() { }

    /*
    /**********************************************************************
    /* Static load methods
    /**********************************************************************
     */

    /**
     * Loads version information by introspecting a class named
     * "PackageVersion" in the same package as the given class.
     *<p>
     * If the class could not be found or does not have a public
     * static Version field named "VERSION", returns "empty" {@link Version}
     * returned by {@link Version#unknownVersion()}.
     *
     * @param cls Class for which to look version information
     *
     * @return Version information discovered if any;
     *  {@link Version#unknownVersion()} if none
     */
    public static Version versionFor(Class<?> cls)
    {
        Version v = null;
        try {
            String versionInfoClassName = cls.getPackage().getName() + ".PackageVersion";
            Class<?> vClass = Class.forName(versionInfoClassName, true, cls.getClassLoader());
            // However, if class exists, it better work correctly, no swallowing exceptions
            try {
                v = ((Versioned) vClass.getDeclaredConstructor().newInstance()).version();
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to get Versioned out of "+vClass);
            }
        } catch (Exception e) { // ok to be missing (not good but acceptable)
            ;
        }
        return (v == null) ? Version.unknownVersion() : v;
    }

    /**
     * Method used by <code>PackageVersion</code> classes to decode version injected by Maven build.
     *
     * @param s Version String to parse
     * @param groupId Maven group id to include with version
     * @param artifactId Maven artifact id to include with version
     *
     * @return Version instance constructed from parsed components, if successful;
     *    {@link Version#unknownVersion()} if parsing of components fail
     */
    public static Version parseVersion(String s, String groupId, String artifactId)
    {
        if (s != null && (s = s.trim()).length() > 0) {
            String[] parts = V_SEP.split(s);
            return new Version(parseVersionPart(parts[0]),
                    (parts.length > 1) ? parseVersionPart(parts[1]) : 0,
                    (parts.length > 2) ? parseVersionPart(parts[2]) : 0,
                    (parts.length > 3) ? parts[3] : null,
                    groupId, artifactId);
        }
        return Version.unknownVersion();
    }

    protected static int parseVersionPart(String s) {
        int number = 0;
        for (int i = 0, len = s.length(); i < len; ++i) {
            char c = s.charAt(i);
            if (c > '9' || c < '0') break;
            number = (number * 10) + (c - '0');
        }
        return number;
    }

    /*
    /**********************************************************************
    /* Orphan utility methods
    /**********************************************************************
     */

    public final static void throwInternal() {
        throw new RuntimeException("Internal error: this code path should never get executed");
    }
}
