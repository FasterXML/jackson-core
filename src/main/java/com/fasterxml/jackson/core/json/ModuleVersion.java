package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.util.VersionUtil;

/**
 * Helper class used for finding and caching version information
 * for the core bundle.
 */
public class ModuleVersion extends VersionUtil
{
    public final static ModuleVersion instance = new ModuleVersion();
}
