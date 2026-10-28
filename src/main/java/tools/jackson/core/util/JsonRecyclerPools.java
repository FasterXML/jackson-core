package tools.jackson.core.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;

import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.util.RecyclerPool.BoundedPoolBase;
import tools.jackson.core.util.RecyclerPool.ConcurrentDequePoolBase;

/**
 * Set of {@link RecyclerPool} implementations to be used by the default
 * JSON-backed {@link JsonFactory} for recycling {@link BufferRecycler}
 * containers.
 *<p>
 * Choosing a pool:
 *<ul>
 * <li>{@link #defaultPool()} ({@link ConcurrentDequePool}): shared and unbounded,
 *  with no per-thread state; allocates a queue node per release, and all
 *  threads contend on the ends of one queue.
 *  </li>
 * <li>{@link #newHybridPool(int)} ({@link HybridPool}): when per-thread retention
 *  is acceptable. Platform threads keep one instance each, as with
 *  {@link #threadLocalPool()}; virtual threads share bounded striped slots.
 *  </li>
 * <li>{@link #newStripedArrayPool(int)} ({@link StripedArrayPool}): shared and
 *  bounded, with no per-thread state; replaces the default where per-thread
 *  retention is not wanted (large or changing thread pools, or many
 *  short-lived threads).
 *  </li>
 * <li>{@link #threadLocalPool()} ({@link ThreadLocalPool}): one instance per
 *  thread, held by a {@link SoftReference}, never shared; poor for virtual
 *  threads, which each abandon their instance.
 *  </li>
 * <li>{@link #newBoundedPool(int)} ({@link BoundedPool}): shared, backed by an
 *  {@link ArrayBlockingQueue} of fixed size that drops releases when full
 *  (it never blocks).
 *  </li>
 * <li>{@link #nonRecyclingPool()} ({@link NonRecyclingPool}): no recycling;
 *  every acquire creates a new instance.
 *  </li>
 *</ul>
 */
public final class JsonRecyclerPools
{
    /**
     * Method to call to get the default recycler pool instance:
     * as of Jackson 3.0 and later this is same as calling
     * {@link #newConcurrentDequePool()}.
     *
     * @return the default {@link RecyclerPool} implementation to use
     *   if no specific implementation desired.
     */
    public static RecyclerPool<BufferRecycler> defaultPool() {
        return newConcurrentDequePool();
    }

    /**
     * Accessor for getting the shared/global {@link ThreadLocalPool} instance
     * (due to design only one instance ever needed)
     *
     * @return Globally shared instance of {@link ThreadLocalPool}
     */
    public static RecyclerPool<BufferRecycler> threadLocalPool() {
        return ThreadLocalPool.GLOBAL;
    }

    /**
     * Accessor for getting the shared/global {@link NonRecyclingPool} instance
     * (due to design only one instance ever needed)
     *
     * @return Globally shared instance of {@link NonRecyclingPool}.
     */
    public static RecyclerPool<BufferRecycler> nonRecyclingPool() {
        return NonRecyclingPool.GLOBAL;
    }

    /**
     * Accessor for getting the shared/global {@link ConcurrentDequePool} instance.
     *
     * @return Globally shared instance of {@link NonRecyclingPool}.
     */
    public static RecyclerPool<BufferRecycler> sharedConcurrentDequePool() {
        return ConcurrentDequePool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link ConcurrentDequePool} instance.
     *
     * @return Globally shared instance of {@link NonRecyclingPool}.
     */
    public static RecyclerPool<BufferRecycler> newConcurrentDequePool() {
        return ConcurrentDequePool.construct();
    }

    /**
     * Accessor for getting the shared/global {@link BoundedPool} instance.
     *
     * @return Globally shared instance of {@link BoundedPool}.
     */
    public static RecyclerPool<BufferRecycler> sharedBoundedPool() {
        return BoundedPool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link BoundedPool} instance.
     *
     * @param size Maximum number of values to pool
     *
     * @return Globally shared instance of {@link BoundedPool}.
     */
    public static RecyclerPool<BufferRecycler> newBoundedPool(int size) {
        return BoundedPool.construct(size);
    }

    /**
     * Accessor for getting the shared/global {@link StripedArrayPool} instance.
     *
     * @return Globally shared instance of {@link StripedArrayPool}
     *
     * @since 3.3
     */
    public static RecyclerPool<BufferRecycler> sharedStripedArrayPool() {
        return StripedArrayPool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link StripedArrayPool}
     * instance.
     *
     * @param size Requested number of slots; rounded up to the next power of
     *   two (so up to almost twice as many values may be pooled; 513 gives
     *   1024 slots), and clamped to
     *   {@link RecyclerPool.StripedArrayPoolBase#MAX_CAPACITY}
     *
     * @return New instance of {@link StripedArrayPool}
     *
     * @since 3.3
     */
    public static RecyclerPool<BufferRecycler> newStripedArrayPool(int size) {
        return StripedArrayPool.construct(size);
    }

    /**
     * Accessor for getting the shared/global {@link HybridPool} instance.
     *
     * @return Globally shared instance of {@link HybridPool}
     *
     * @since 3.3
     */
    public static RecyclerPool<BufferRecycler> sharedHybridPool() {
        return HybridPool.GLOBAL;
    }

    /**
     * Accessor for constructing a new, non-shared {@link HybridPool} instance.
     *
     * @param size Requested number of shared slots; rounded up
     *   to the next power of two (so up to almost twice as many values may be
     *   pooled; 513 gives 1024 slots), and clamped to
     *   {@link RecyclerPool.StripedArrayPoolBase#MAX_CAPACITY}
     *
     * @return New instance of {@link HybridPool}
     *
     * @since 3.3
     */
    public static RecyclerPool<BufferRecycler> newHybridPool(int size) {
        return HybridPool.construct(size);
    }

    /*
    /**********************************************************************
    /* Concrete RecyclerPool implementations for recycling BufferRecyclers
    /**********************************************************************
     */

    /**
     * {@link ThreadLocal}-based {@link RecyclerPool} implementation used for
     * recycling {@link BufferRecycler} instances:
     * see {@link RecyclerPool.ThreadLocalPoolBase} for full explanation
     * of functioning.
     */
    public static class ThreadLocalPool
        extends RecyclerPool.ThreadLocalPoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final ThreadLocalPool GLOBAL = new ThreadLocalPool();

        /**
         * This <code>ThreadLocal</code> contains a {@link java.lang.ref.SoftReference}
         * to a {@link BufferRecycler} used to provide a low-cost
         * buffer recycling between reader and writer instances.
         */
        protected final static ThreadLocal<SoftReference<BufferRecycler>> _recyclerRef
            = new ThreadLocal<SoftReference<BufferRecycler>>();
        
        private ThreadLocalPool() { }

        @Override
        public BufferRecycler acquirePooled() {
            SoftReference<BufferRecycler> ref = _recyclerRef.get();
            BufferRecycler br = (ref == null) ? null : ref.get();

            if (br == null) {
                br = new BufferRecycler();
                ref = new SoftReference<BufferRecycler>(br);
                _recyclerRef.set(ref);
            }
            return br;
        }

        // // // JDK serialization support

        protected Object readResolve() { return GLOBAL; }
    }

    /**
     * Dummy {@link RecyclerPool} implementation that does not recycle
     * anything but simply creates new instances when asked to acquire items.
     */
    public static class NonRecyclingPool
        extends RecyclerPool.NonRecyclingPoolBase<BufferRecycler>
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
     * {@link RecyclerPool} implementation that uses
     * {@link ConcurrentLinkedDeque} for recycling instances.
     *<p>
     * Pool is unbounded: see {@link RecyclerPool} what this means.
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
     * {@link RecyclerPool} implementation that uses
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

    /**
     * {@link RecyclerPool} implementation that uses a fixed array of atomic
     * slots for recycling {@link BufferRecycler} instances:
     * see {@link RecyclerPool.StripedArrayPoolBase} for full explanation
     * of functioning.
     *<p>
     * Choose this pool over {@link HybridPool} when instances must not be
     * retained per thread: its slot count bounds all retention.
     *
     * @since 3.3
     */
    public static class StripedArrayPool extends RecyclerPool.StripedArrayPoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final StripedArrayPool GLOBAL = new StripedArrayPool(SERIALIZATION_SHARED);

        // // // Life-cycle (constructors, factory methods)

        protected StripedArrayPool(int capacityAsId) {
            super(capacityAsId);
        }

        public static StripedArrayPool construct(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be > 0, was: "+capacity);
            }
            return new StripedArrayPool(capacity);
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

    /**
     * {@link RecyclerPool} implementation that recycles
     * {@link BufferRecycler} instances through {@link ThreadLocalPool} for
     * platform threads (so a platform thread uses the same instance as with
     * {@link #threadLocalPool()}) and shared atomic slots for virtual threads:
     * see {@link RecyclerPool.HybridPoolBase} for full explanation
     * of functioning.
     *<p>
     * Choose this pool over {@link StripedArrayPool} when retaining one
     * instance per platform thread is acceptable: platform threads then
     * acquire without touching shared state.
     *
     * @since 3.3
     */
    public static class HybridPool extends RecyclerPool.HybridPoolBase<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        protected static final HybridPool GLOBAL = new HybridPool(SERIALIZATION_SHARED);

        // // // Life-cycle (constructors, factory methods)

        protected HybridPool(int capacityAsId) {
            super(capacityAsId, ThreadLocalPool.GLOBAL);
        }

        public static HybridPool construct(int capacity) {
            if (capacity <= 0) {
                throw new IllegalArgumentException("capacity must be > 0, was: "+capacity);
            }
            return new HybridPool(capacity);
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
