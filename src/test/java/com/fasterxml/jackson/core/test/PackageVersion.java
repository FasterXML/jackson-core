package com.fasterxml.jackson.core.test;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.Versioned;
import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * Helper class used for verifying that auto-generated <code>PackageVersion</code>
 * classes can be used for verification.
 */
public final class PackageVersion implements Versioned {
	@Override
	public Version version() {
		return VersionUtil.parseVersion(
	            "23.42.64738-foobar", "foobar-group", "foobar-artifact");
	}
}
