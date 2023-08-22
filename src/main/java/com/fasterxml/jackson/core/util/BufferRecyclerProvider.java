package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.TokenStreamFactory;

/**
 * Provider for {@link BufferRecycler}s to allow pluggable and optional
 * recycling of underlying input/output buffers.
 *
 * @since 2.16
 */
public interface BufferRecyclerProvider
    extends java.io.Serializable
{
    public abstract BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory);
}
