package tools.jackson.core.util;

import tools.jackson.core.TokenStreamFactory;

/**
 * Provider for {@link BufferRecycler}s to allow pluggable and optional
 * recycling of underlying input/output buffers.
 */
public interface BufferRecyclerProvider
    extends java.io.Serializable
{
    public abstract BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory);
}
