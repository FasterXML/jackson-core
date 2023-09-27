package com.fasterxml.jackson.core.util;

import com.fasterxml.jackson.core.JsonFactory;

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
     * @return Globally shared instance of {@link ThreadLocalPool}; same as calling
     *   {@link ThreadLocalPool#shared()}.
     */
    public static BufferRecyclerPool<BufferRecycler> threadLocalPool() {
        return ThreadLocalPool.shared();
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

        private static final ThreadLocalPool GLOBAL = new ThreadLocalPool();

        private ThreadLocalPool() { }
        
        /**
         * Accessor for the global, shared instance of this pool type:
         * due to its nature it is a Singleton as there can only
         * be a single recycled {@link BufferRecycler} per thread.
         *
         * @return Shared pool instance
         */
        public static ThreadLocalPool shared() {
            return GLOBAL;
        }

        @SuppressWarnings("deprecation")
        @Override
        public BufferRecycler acquireBufferRecycler() {
            return BufferRecyclers.getBufferRecycler();
        }

        // // // JDK serialization support

        protected Object readResolve() { return GLOBAL; }
    }
}
