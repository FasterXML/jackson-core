package com.fasterxml.jackson.core.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.util.BufferRecyclerPool.BoundedPoolBase;
import com.fasterxml.jackson.core.util.BufferRecyclerPool.ConcurrentDequePoolBase;
import com.fasterxml.jackson.core.util.BufferRecyclerPool.LockFreePoolBase;

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

    /**
     * Accessor for getting the shared/global {@link ConcurrentDequePool} instance.
     *
     * @return Globally shared instance of {@link NonRecyclingPool}.
     */
    public static BufferRecyclerPool<BufferRecycler> sharedConcurrentDequePool() {
        return ConcurrentDequePool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link ConcurrentDequePool} instance.
     *
     * @return Globally shared instance of {@link NonRecyclingPool}.
     */
    public static BufferRecyclerPool<BufferRecycler> newConcurrentDequePool() {
        return ConcurrentDequePool.construct();
    }

    /**
     * Accessor for getting the shared/global {@link LockFreePool} instance.
     *
     * @return Globally shared instance of {@link LockFreePool}.
     */
    public static BufferRecyclerPool<BufferRecycler> sharedLockFreePool() {
        return LockFreePool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link LockFreePool} instance.
     *
     * @return Globally shared instance of {@link LockFreePool}.
     */
    public static BufferRecyclerPool<BufferRecycler> newLockFreePool() {
        return LockFreePool.construct();
    }

    /**
     * Accessor for getting the shared/global {@link BoundedPool} instance.
     *
     * @return Globally shared instance of {@link BoundedPool}.
     */
    public static BufferRecyclerPool<BufferRecycler> sharedBoundedPool() {
        return BoundedPool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link BoundedPool} instance.
     *
     * @return Globally shared instance of {@link BoundedPool}.
     */
    public static BufferRecyclerPool<BufferRecycler> newBoundedPool(int size) {
        return BoundedPool.construct(size);
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
        public BufferRecycler acquirePooled() {
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
        public BufferRecycler acquirePooled() {
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

        protected static final ConcurrentDequePool GLOBAL = new ConcurrentDequePool(SERIALIZATION_SHARED);

        // // // Life-cycle (constructors, factory methods)

        protected ConcurrentDequePool(int serialization) {
            super(serialization);
        }

        public static ConcurrentDequePool construct() {
            return new ConcurrentDequePool(SERIALIZATION_NON_SHARED);
        }

        @Override
        public BufferRecycler createPooled() {
            return new BufferRecycler();
        }

        // // // JDK serialization support

        // Make sure to re-link to global/shared or non-shared.
        protected Object readResolve() {
            return _resolveToShared(GLOBAL).orElseGet(() -> construct());
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * a lock free linked list for recycling instances.
     *<p>
     * Pool is unbounded: see {@link BufferRecyclerPool} for
     * details on what this means.
     */
    public static class LockFreePool extends LockFreePoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final LockFreePool GLOBAL = new LockFreePool(SERIALIZATION_SHARED);

        // // // Life-cycle (constructors, factory methods)

        protected LockFreePool(int serialization) {
            super(serialization);
        }

        public static LockFreePool construct() {
            return new LockFreePool(SERIALIZATION_NON_SHARED);
        }
        
        @Override
        public BufferRecycler createPooled() {
            return new BufferRecycler();
        }

        // // // JDK serialization support

        // Make sure to re-link to global/shared or non-shared.
        protected Object readResolve() {
            return _resolveToShared(GLOBAL).orElseGet(() -> construct());
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * a bounded queue ({@link ArrayBlockingQueue} for recycling instances.
     * This is "bounded" pool since it will never hold on to more
     * {@link BufferRecycler} instances than its size configuration:
     * the default size is {@link BoundedPoolBase#DEFAULT_CAPACITY}.
     */
    public static class BoundedPool extends BoundedPoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final BoundedPool GLOBAL = new BoundedPool(SERIALIZATION_SHARED);

        // // // Life-cycle (constructors, factory methods)

        protected BoundedPool(int capacityAsId) {
            super(capacityAsId);
        }

        public static BoundedPool construct(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be > 0, was: "+capacity);
            }
            return new BoundedPool(capacity);
        }

        @Override
        public BufferRecycler createPooled() {
            return new BufferRecycler();
        }

        // // // JDK serialization support

        // Make sure to re-link to global/shared or non-shared.
        protected Object readResolve() {
            return _resolveToShared(GLOBAL).orElseGet(() -> construct(_serialization));
        }
    }
}
