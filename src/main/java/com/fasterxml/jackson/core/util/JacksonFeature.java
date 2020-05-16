package com.fasterxml.jackson.core.util;

/**
 * Basic API implemented by Enums used for simple Jackson "features": on/off
 * settings and capabilities exposed as something that can be internally
 * represented as bit sets.
 * Designed to be used with {@link JacksonFeatureSet}.
 *
 * @since 2.12
 */
public interface JacksonFeature
{
    /**
     * Accessor for checking whether this feature is enabled by default.
     */
    public boolean enabledByDefault();
    
    /**
     * Returns bit mask for this feature instance; must be a single bit,
     * that is of form {@code 1 << N}
     */
    public int getMask();

    /**
     * Convenience method for checking whether feature is enabled in given bitmask
     */
    public boolean enabledIn(int flags);
}
