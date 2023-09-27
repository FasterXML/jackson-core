package com.fasterxml.jackson.core.util;

import java.util.concurrent.ConcurrentLinkedDeque;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.BufferRecyclerPool.ConcurrentDequePoolBase;

/**
 * Set of {@link BufferRecyclerPool} implementations to be used by the default
 * JSON-backed {@link JsonFactory} for recycling {@link BufferRecycler}
 * containers.
 *
 * @since 2.16
 */
public final class JsonBufferRecyclers
{
    /**
     * @return the default {@link BufferRecyclerPool} implementation
     *   which is the thread local based one:
     *   basically alias to {@link #threadLocalPool()}).
     */
    public static BufferRecyclerPool<BufferRecycler> defaultPool() {
        return threadLocalPool();
    }
    /**
     * Accessor for getting the shared/global {@link ThreadLocalPool} instance
     * (due to design only one instance ever needed)
     *
     * @return Globally shared instance of {@link ThreadLocalPool}
     */
    public static BufferRecyclerPool<BufferRecycler> threadLocalPool() {
        return ThreadLocalPool.GLOBAL;
    }

    /**
     * Accessor for getting the shared/global {@link NonRecyclingPool} instance
     * (due to design only one instance ever needed)
     *
     * @return Globally shared instance of {@link NonRecyclingPool}.
     */
    public static BufferRecyclerPool<BufferRecycler> nonRecyclingPool() {
        return NonRecyclingPool.GLOBAL;
    }

    /*
    /**********************************************************************
    /* Concrete BufferRecyclerPool implementations for recycling BufferRecyclers
    /**********************************************************************
     */

    /**
     * {@link ThreadLocal}-based {@link BufferRecyclerPool} implemenetation used for
     * recycling {@link BufferRecycler} instances:
     * see {@link BufferRecyclerPool.ThreadLocalPoolBase} for full explanation
     * of functioning.
     */
    public static class ThreadLocalPool
        extends BufferRecyclerPool.ThreadLocalPoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final ThreadLocalPool GLOBAL = new ThreadLocalPool();

        private ThreadLocalPool() { }

        @SuppressWarnings("deprecation")
        @Override
        public BufferRecycler acquireBufferRecycler() {
            return BufferRecyclers.getBufferRecycler();
        }

        // // // JDK serialization support

        protected Object readResolve() { return GLOBAL; }
    }

    /**
     * Dummy {@link BufferRecyclerPool} implementation that does not recycle
     * anything but simply creates new instances when asked to acquire items.
     */
    public static class NonRecyclingPool
        extends BufferRecyclerPool.NonRecyclingPoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final NonRecyclingPool GLOBAL = new NonRecyclingPool();

        protected NonRecyclingPool() { }

        @Override
        public BufferRecycler acquireBufferRecycler() {
            return new BufferRecycler();
        }
        
        // // // JDK serialization support

        protected Object readResolve() { return GLOBAL; }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * {@link ConcurrentLinkedDeque} for recycling instances.
     *<p>
     * Pool is unbounded: see {@link BufferRecyclerPool} what this means.
     */
    public static class ConcurrentDequePool extends ConcurrentDequePoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        private static final ConcurrentDequePool GLOBAL = new ConcurrentDequePool(SERIALIZATION_SHARED);

        // // // Life-cycle (constructors, factory methods)

        protected ConcurrentDequePool(int serialization) {
            super(serialization);
        }

        /**
         * Accessor for getting the globally shared singleton instance.
         * Note that if you choose to use this instance,
         * pool may be shared by many other {@code JsonFactory} instances.
         *
         * @return Shared pool instance
         */
        public static ConcurrentDequePool shared() {
            return GLOBAL;
        }

        /**
         * Accessor for creating and returning a new, non-shared pool instance.
         *
         * @return Newly constructed, non-shared pool instance
         */
        public static ConcurrentDequePool nonShared() {
            return new ConcurrentDequePool(SERIALIZATION_NON_SHARED);
        }

        @Override
        public BufferRecycler acquireBufferRecycler() {
            BufferRecycler bufferRecycler = pool.pollFirst();
            if (bufferRecycler == null) {
                bufferRecycler = new BufferRecycler();
            }
            return bufferRecycler;
        }

        // // // JDK serialization support

        /**
         * Make sure to re-link to global/shared or non-shared.
         */
        protected Object readResolve() {
            return _resolveToShared(GLOBAL).orElseGet(() -> nonShared());
        }
    }
}
