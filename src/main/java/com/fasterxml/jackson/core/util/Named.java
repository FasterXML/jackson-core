package com.fasterxml.jackson.core.util;

/**
 * Simple tag interface used primarily to allow databind to pass entities with
 * name without needing to expose more details of implementation.
 *<p>
 * NOTE: in Jackson 2.x, was part of `jackson-databind`: demoted here for 3.0.
 *
 * @since 3.0
 */
public interface Named {
    public String getName();
}
