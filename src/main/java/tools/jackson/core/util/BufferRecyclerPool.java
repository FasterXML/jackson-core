package tools.jackson.core.util;

import tools.jackson.core.TokenStreamFactory;

/**
 * Interface for entity that controls creation and possible reuse of {@link BufferRecycler}
 * instances used for recycling of underlying input/output buffers.
 */
public interface BufferRecyclerPool
    extends java.io.Serializable
{
    public BufferRecycler acquireBufferRecycler(TokenStreamFactory forFactory);

    public void releaseBufferRecycler(BufferRecycler recycler);
}
