package com.fasterxml.jackson.core.util;

/**
 * Interface for entity that controls creation and possible reuse of {@link BufferRecycler}
 * instances used for recycling of underlying input/output buffers.
 *
 * @since 2.16
 */
public interface BufferRecyclerPool
    extends java.io.Serializable
{
    public BufferRecycler acquireBufferRecycler();

    public void releaseBufferRecycler(BufferRecycler recycler);
}
