package com.fasterxml.jackson.core;

import com.fasterxml.jackson.core.util.JacksonFeature;

/**
 * Set of on/off capabilities that a {@link JsonParser} for given format
 * (or in case of buffering, original format) has.
 * Used in some cases to adjust aspects of things like content conversions,
 * coercions and validation by format-agnostic functionality.
 * Specific or expected usage documented by individual capability entry
 * Javadocs.
 *
 * @since 2.12
 */
public enum StreamReadCapability
    implements JacksonFeature // since 2.12
{
    /**
     * Capability that indicates that data format can expose multiple properties
     * with same name ("duplicates") within one Object context.
     * This is usually not enabled, except for formats like {@code xml} that
     * have content model that does not map cleanly to JSON-based token stream.
     *<p>
     * Capability may be used for allowing secondary mapping of such duplicates
     * in case of using Tree Model (see {@link TreeNode}), or "untyped" databinding
     * (mapping content as generic {@link java.lang.Object}).
     * 
     *<p>
     * Capability is typically {@code false}, hence default.
     */
    DUPLICATE_PROPERTIES(false),

    ;

    /**
     * Whether feature is enabled or disabled by default.
     */
    private final boolean _defaultState;

    private final int _mask;

    private StreamReadCapability(boolean defaultState) {
        _defaultState = defaultState;
        _mask = (1 << ordinal());
    }

    @Override
    public boolean enabledByDefault() { return _defaultState; }
    @Override
    public boolean enabledIn(int flags) { return (flags & _mask) != 0; }
    @Override
    public int getMask() { return _mask; }

}
