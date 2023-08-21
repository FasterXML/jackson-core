package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.TokenStreamFactory;

/**
 * Provider for {@link BufferRecycler}s to allow pluggable and optional
 * recycling of underlying input/output buffers.
 *
 * @since 2.16
 */
public abstract class BufferRecyclerProvider
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    public abstract BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory);

    public abstract void releaseBufferRecycler(BufferRecycler r);
}
