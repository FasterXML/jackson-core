package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.util.VersionUtil;

public final class PackageVersion implements Versioned {
    public final static Version VERSION = VersionUtil.parseVersion(
            "@projectversion@", "@projectgroupid@", "@projectartifactid@");

    @Override
    public Version version() {
        return VERSION;
    }
}
