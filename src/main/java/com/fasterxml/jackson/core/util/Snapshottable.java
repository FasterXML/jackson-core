package com.fasterxml.jackson.core.util;

/**
 * Interface that defines one method (see {@link #snapshot}) for ensuring that we get
 * an instance that does not allow modifying state of `this` instance. Instance returned
 * may be `this` if (and only if) it is immutable through its API (or, for some limited
 * circumstances, if usage is guaranteed not to modify it after this point -- such usage
 * is discouraged however); or, if that can not be guaranteed, a newly created copy
 * with same configuration and state as `this`.
 *<p>
 * Interface is intended to be used for persisting state for serialization, or to support
 * "re-build" of otherwise immutable objects like factories. Some of the helper objects
 * factories use and rely on have mutable state which can not be shared: instead, to support
 * re-building, a state object may be created to contain copies (snapshots).
 * Intent, therefore, is that caller does not need to know about immutability (or lack thereof)
 * of an entity but can simply call {@link #snapshot}.
 *
 * @since 3.0
 */
public interface Snapshottable<T> {
    /**
     * Method to call to get an instance that may not be modified through any other object,
     * including `this`. That instance may be `this` if (and only if) this instance is effectively
     * immutable (unmodifiable) through its API: if this is not the case, a new copy with same
     * configuration must be created and returned.
     */
    public T snapshot();

    public static <T> T takeSnapshot(Snapshottable<T> src) {
        if (src == null) {
            return null;
        }
        return src.snapshot();
    }
}
