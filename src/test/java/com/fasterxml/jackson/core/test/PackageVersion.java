package com.fasterxml.jackson.core.test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.util.VersionUtil;

public final class PackageVersion {
    public final static Version VERSION = VersionUtil.parseVersion(
        "23.42.64738-foobar", "foobar-group", "foobar-artifact");
}
