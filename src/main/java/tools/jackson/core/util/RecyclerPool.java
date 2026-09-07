package tools.jackson.core.util;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * API for object pools that control creation and possible reuse of
 * objects that are costly to create (often things like encoding/decoding buffers).
 *<p>
 * Also contains partial (base) implementations for pools that use different
 * strategies on retaining objects for reuse.
 * Following implementations are included:
 *<ul>
 * <li>{@link NonRecyclingPoolBase} which does not retain or recycle anything and
 * will always simply construct and return new instance when
 * {@code acquireBufferRecycler} is called
 *  </li>
 * <li>{@link ThreadLocalPoolBase} which uses {@link ThreadLocal} to retain at most
 *   1 object per {@link Thread}.
 * </li>
 * <li>{@link BoundedPoolBase} is "bounded pool" and retains at most N objects (default value being
 *  {@link BoundedPoolBase#DEFAULT_CAPACITY}) at any given time.
 *  </li>
 * <li>{@link StripedArrayPoolBase} retains at most N objects in a fixed array of
 *  atomic slots, allocation-free on both acquire and release (default slot count
 *  being {@link StripedArrayPoolBase#DEFAULT_CAPACITY}).
 * </li>
 * <li>{@link HybridPoolBase} gives platform threads the {@link ThreadLocalPoolBase}
 *  behavior and routes virtual threads to shared {@link StripedArrayPoolBase} slots.
 * </li>
 *</ul>
 *
 *<p>
 * Default implementations are also included as nested classes.
 *
 * @param <P> Type of Objects pool recycles
 */
public interface RecyclerPool<P extends RecyclerPool.WithPool<P>> extends Serializable
{
    /**
     * Simple add-on interface that poolable entities must implement.
     *
     * @param <P> Self type
     */
    public interface WithPool<P extends WithPool<P>> {
        /**
         * Method to call to add link from pooled item back to pool
         * that handles it
         * 
         * @param pool Pool that "owns" pooled item
         *
         * @return This item (for call chaining)
         */
        P withPool(RecyclerPool<P> pool);

        /**
         * Method called when this item is to be released back to the
         * pool that owns it (if any)
         */
        void releaseToPool();
    }

    /**
     * Method called to acquire a Pooled value from this pool
     * AND make sure it is linked back to this
     * {@link RecyclerPool} as necessary for it to be
     * released (see {@link #releasePooled}) later after usage ends.
     * Actual acquisition is done by a call to {@link #acquirePooled()}.
     *<p>
     * Default implementation calls {@link #acquirePooled()} followed by
     * a call to {@link WithPool#withPool}.
     *
     * @return Pooled instance for caller to use; caller expected
     *   to call {@link #releasePooled} after it is done using instance.
     */
    default P acquireAndLinkPooled() {
        return acquirePooled().withPool(this);
    }

    /**
     * Method for sub-classes to implement for actual acquire logic; called
     * by {@link #acquireAndLinkPooled()}.
     *
     * @return Instance acquired (pooled or just constructed)
     */
    P acquirePooled();

    /**
     * Method that should be called when previously acquired (see {@link #acquireAndLinkPooled})
     * pooled value that is no longer needed; this lets pool to take ownership
     * for possible reuse.
     *
     * @param pooled Pooled instance to release back to pool
     */
    void releasePooled(P pooled);

    /**
     * Optional method that may allow dropping of all pooled Objects; mostly
     * useful for unbounded pool implementations that may retain significant
     * memory and that may then be cleared regularly.
     *
     * @since 2.17
     *
     * @return {@code true} If pool supports operation and dropped all pooled
     *    Objects; {@code false} otherwise.
     */
    default boolean clear() {
        return false;
    }

    /**
     * Diagnostic method for obtaining an estimate of number of pooled items
     * this pool contains, available for recycling.
     * Note that in addition to this information possibly not being available
     * (denoted by return value of {@code -1}) even when available this may be
     * just an approximation.
     *<p>
     * Default method implementation simply returns {@code -1} and is meant to be
     * overridden by concrete sub-classes.
     *
     * @return Number of pooled entries available from this pool, if available;
     *    {@code -1} if not.
     *
     * @since 2.18
     */
    default int pooledCount() {
        return -1;
    }

    /*
    /**********************************************************************
    /* Partial/base RecyclerPool implementations
    /**********************************************************************
     */

    /**
     * Default {@link RecyclerPool} implementation that uses
     * {@link ThreadLocal} for recycling instances. 
     * Instances are stored using {@link java.lang.ref.SoftReference}s so that
     * they may be Garbage Collected as needed by JVM.
     *<p>
     * Note that this implementation may not work well on platforms where
     * {@link java.lang.ref.SoftReference}s are not well supported (like
     * Android), or on platforms where {@link java.lang.Thread}s are not
     * long-living or reused (like Project Loom).
     */
    abstract class ThreadLocalPoolBase<P extends WithPool<P>> implements RecyclerPool<P>
    {
        private static final long serialVersionUID = 1L;
        protected ThreadLocalPoolBase() { }

        // // // Actual API implementation

        @Override
        public P acquireAndLinkPooled() {
            // since this pool doesn't do anything on release it doesn't need to be registered on the BufferRecycler
            return acquirePooled();
        }

        @Override
        public abstract P acquirePooled();

        @Override
        public void releasePooled(P pooled) {
             // nothing to do, relies on ThreadLocal
        }

        // No way to actually even estimate...
        @Override
        public int pooledCount() {
            return -1;
        }

        // Due to use of ThreadLocal no tracking available; cannot clear
        @Override
        public boolean clear() {
            return false;
        }
    }

    /**
     * {@link RecyclerPool} implementation that does not use
     * any pool but simply creates new instances when necessary.
     */
    abstract class NonRecyclingPoolBase<P extends WithPool<P>> implements RecyclerPool<P>
    {
        private static final long serialVersionUID = 1L;

        // // // Actual API implementation

        @Override
        public P acquireAndLinkPooled() {
            // since this pool doesn't do anything on release it doesn't need to be registered on the BufferRecycler
            return acquirePooled();
        }

        @Override
        public abstract P acquirePooled();

        @Override
        public void releasePooled(P pooled) {
             // nothing to do, there is no underlying pool
        }

        @Override
        public int pooledCount() {
            return 0;
        }

        /**
         * Although no pooling occurs, we consider clearing to succeed,
         * so returns always {@code true}.
         *
         * @return Always returns {@code true}
         */
        @Override
        public boolean clear() {
            return true;
        }
    }

    /**
     * Intermediate base class for instances that are stateful and require
     * special handling with respect to JDK serialization, to retain
     * "global" reference distinct from non-shared ones.
     */
    abstract class StatefulImplBase<P extends WithPool<P>>
        implements RecyclerPool<P>
    {
        private static final long serialVersionUID = 1L;

        public final static int SERIALIZATION_SHARED = -1;

        public final static int SERIALIZATION_NON_SHARED = 1;

        /**
         * Value that indicates basic aspects of pool for JDK serialization;
         * either marker for shared/non-shared, or possibly bounded size;
         * depends on sub-class.
         */
        protected final int _serialization;

        protected StatefulImplBase(int serialization) {
            _serialization = serialization;
        }

        protected Optional<StatefulImplBase<P>> _resolveToShared(StatefulImplBase<P> shared) {
            if (_serialization == SERIALIZATION_SHARED) {
                return Optional.of(shared);
            }
            return Optional.empty();
        }

        public abstract P createPooled();
    }

    /**
     * {@link RecyclerPool} implementation that uses
     * {@link ConcurrentLinkedDeque} for recycling instances.
     *<p>
     * Pool is unbounded: see {@link RecyclerPool} what this means.
     */
    abstract class ConcurrentDequePoolBase<P extends WithPool<P>>
        extends StatefulImplBase<P>
    {
        private static final long serialVersionUID = 1L;

        protected final transient Deque<P> pool;

        protected ConcurrentDequePoolBase(int serialization) {
            super(serialization);
            pool = new ConcurrentLinkedDeque<>();
        }

        // // // Actual API implementation

        @Override
        public P acquirePooled() {
            P pooled = pool.pollFirst();
            if (pooled == null) {
                pooled = createPooled();
            }
            return pooled;
        }

        @Override
        public void releasePooled(P pooled) {
            pool.offerLast(pooled);
        }

        @Override
        public int pooledCount() {
            return pool.size();
        }

        @Override
        public boolean clear() {
            pool.clear();
            return true;
        }
    }

    /**
     * {@link RecyclerPool} implementation that uses a fixed-size array of
     * atomic slots for recycling instances: acquire takes a slot with an
     * atomic get-and-clear (so an instance can never be handed to two
     * acquirers), release stores into the first empty slot with compare-and-set,
     * and both scans start from a slot indexed off the current thread id, which
     * keeps contention and buffer reuse thread-local. Unlike
     * {@link ConcurrentDequePoolBase} no queue node is allocated on release,
     * and unlike {@link BoundedPoolBase} there is no lock on either path.
     *<p>
     * This is a "bounded" pool: it will never hold on to more pooled instances
     * than its slot count (default {@link StripedArrayPoolBase#DEFAULT_CAPACITY});
     * an instance released when every slot is occupied is dropped.
     *
     * @since 3.3
     */
    abstract class StripedArrayPoolBase<P extends WithPool<P>>
        extends StatefulImplBase<P>
    {
        private static final long serialVersionUID = 1L;

        /**
         * Default number of slots, and so the maximum number of instances
         * ever retained for reuse.
         */
        public final static int DEFAULT_CAPACITY = 16;

        private final transient AtomicReferenceArray<P> _slots;

        /**
         * Bit mask for mapping a scan index onto the slot array; the slot
         * count is always a power of two.
         */
        private final transient int _mask;

        // // // Life-cycle (constructors, factory methods)

        protected StripedArrayPoolBase(int capacityAsId) {
            super(capacityAsId);
            final int capacity = _powerOfTwoCapacity(capacityAsId);
            _slots = new AtomicReferenceArray<>(capacity);
            _mask = capacity - 1;
        }

        private static int _powerOfTwoCapacity(int capacityAsId) {
            if (capacityAsId <= 0) {
                return DEFAULT_CAPACITY;
            }
            // Round up so any positive requested size works; the mask-based
            // slot indexing requires a power of two.
            int capacity = Integer.highestOneBit(capacityAsId);
            return (capacity == capacityAsId) ? capacity : (capacity << 1);
        }

        /**
         * Slot index to start acquire/release scans from, derived from the
         * current thread id. Different threads start at different slots, so
         * under contention they mostly touch disjoint slots, and a thread
         * tends to get back the instance (and so the warmed buffers) it
         * released last.
         */
        private int _startingSlot() {
            final long id = Thread.currentThread().getId();
            final int h = (int) (id * 0x9E3779B97F4A7C15L >>> 32);
            return h & _mask;
        }

        // // // Actual API implementation

        @Override
        public P acquirePooled() {
            final int end = _mask;
            final int start = _startingSlot();
            for (int i = 0; i <= end; ++i) {
                final int slot = (start + i) & _mask;
                if (_slots.get(slot) != null) {
                    P pooled = _slots.getAndSet(slot, null);
                    if (pooled != null) {
                        return pooled;
                    }
                }
            }
            return createPooled();
        }

        @Override
        public void releasePooled(P pooled) {
            final int end = _mask;
            final int start = _startingSlot();
            for (int i = 0; i <= end; ++i) {
                final int slot = (start + i) & _mask;
                if ((_slots.get(slot) == null)
                        && _slots.compareAndSet(slot, null, pooled)) {
                    return;
                }
            }
            // All slots occupied: drop the instance. This is the retention
            // bound that keeps the pool from growing without limit.
        }

        @Override
        public int pooledCount() {
            int count = 0;
            for (int i = 0; i <= _mask; ++i) {
                if (_slots.get(i) != null) {
                    ++count;
                }
            }
            return count;
        }

        @Override
        public boolean clear() {
            for (int i = 0; i <= _mask; ++i) {
                _slots.set(i, null);
            }
            return true;
        }

        // // // Other methods

        public int capacity() {
            return _mask + 1;
        }
    }

    /**
     * {@link RecyclerPool} implementation that gives platform threads the
     * {@link ThreadLocalPoolBase} behavior (one leave-in instance per thread,
     * held through a {@link java.lang.ref.SoftReference}, released by doing
     * nothing) and routes virtual threads to the {@link StripedArrayPoolBase}
     * slots it extends. Virtual threads are typically created per task, so a
     * per-thread instance would be abandoned after a single use; the shared
     * slots let their instances recycle across threads.
     *<p>
     * On runtimes without virtual threads (JDK before 21, Android) every
     * thread takes the platform path, which makes this pool behave exactly
     * like {@link ThreadLocalPoolBase}. The virtual-thread check goes through
     * a {@link MethodHandle} resolved once per JVM, so it does not require
     * {@code Thread.isVirtual()} to exist at run time (and this class does
     * not reference it at compile time).
     *
     * @since 3.3
     */
    abstract class HybridPoolBase<P extends WithPool<P>>
        extends StripedArrayPoolBase<P>
    {
        private static final long serialVersionUID = 1L;

        // 07-Sep-2026, steven: [core#1687] every java.lang.invoke reference
        //   lives in this nested holder, and the one call site catches
        //   Throwable, so a runtime without method-handle support (Android
        //   before API 26) degrades to the platform path instead of failing:
        //   loading, initializing, or executing the holder can throw
        //   LinkageError there, and all of it lands in the same handler.
        private static final class VirtualProbe {
            /**
             * {@code Thread#isVirtual()} when the runtime has it,
             * {@code null} when it does not (JDK before 21, Android).
             */
            static final MethodHandle IS_VIRTUAL = _findIsVirtual();

            private VirtualProbe() {}

            private static MethodHandle _findIsVirtual() {
                try {
                    return MethodHandles.publicLookup().findVirtual(Thread.class,
                            "isVirtual", MethodType.methodType(boolean.class));
                } catch (Throwable t) {
                    return null;
                }
            }

            static boolean isVirtual(Thread thread) throws Throwable {
                return (IS_VIRTUAL != null) && (boolean) IS_VIRTUAL.invokeExact(thread);
            }
        }

        protected static boolean _isVirtual(Thread thread) {
            try {
                return VirtualProbe.isVirtual(thread);
            } catch (Throwable t) {
                // No method-handle support (or, in principle, an exact-handle
                // invocation failure): treat as a platform thread.
                return false;
            }
        }

        /**
         * Per-platform-thread leave-in instance, exactly as
         * {@link ThreadLocalPoolBase} keeps it. Rebuilt via
         * {@code readResolve} on deserialization, like the slot array.
         */
        private final transient ThreadLocal<SoftReference<P>> _perThread = new ThreadLocal<>();

        protected HybridPoolBase(int capacityAsId) {
            super(capacityAsId);
        }

        // // // Actual API implementation

        @Override
        public P acquireAndLinkPooled() {
            if (_isVirtual(Thread.currentThread())) {
                return acquirePooled().withPool(this);
            }
            SoftReference<P> ref = _perThread.get();
            P pooled = (ref == null) ? null : ref.get();
            if (pooled == null) {
                pooled = createPooled();
                _perThread.set(new SoftReference<>(pooled));
            }
            // Not linked: releaseToPool() is then a no-op, the instance stays
            // in the ThreadLocal, and no other thread ever sees it.
            return pooled;
        }

        /**
         * Counts the shared (virtual-thread) slots only; per-thread leave-in
         * instances are not tracked, as with {@link ThreadLocalPoolBase}.
         */
        @Override
        public int pooledCount() {
            return super.pooledCount();
        }

        /**
         * Drops the contents of the shared (virtual-thread) slots, but
         * returns {@code false} because the per-thread leave-in instances are
         * not tracked and cannot be dropped, as with
         * {@link ThreadLocalPoolBase}.
         */
        @Override
        public boolean clear() {
            super.clear();
            return false;
        }
    }

    /**
     * {@link RecyclerPool} implementation that uses
     * a bounded queue ({@link ArrayBlockingQueue} for recycling instances.
     * This is "bounded" pool since it will never hold on to more
     * pooled instances than its size configuration:
     * the default size is {@link BoundedPoolBase#DEFAULT_CAPACITY}.
     */
    abstract class BoundedPoolBase<P extends WithPool<P>>
        extends StatefulImplBase<P>
    {
        private static final long serialVersionUID = 1L;

        /**
         * Default capacity which limits number of items that are ever
         * retained for reuse.
         */
        public final static int DEFAULT_CAPACITY = 100;

        private final transient ArrayBlockingQueue<P> pool;

        private final transient int capacity;

        // // // Life-cycle (constructors, factory methods)

        protected BoundedPoolBase(int capacityAsId) {
            super(capacityAsId);
            capacity = (capacityAsId <= 0) ? DEFAULT_CAPACITY : capacityAsId;
            pool = new ArrayBlockingQueue<>(capacity);
        }

        // // // Actual API implementation

        @Override
        public P acquirePooled() {
            P pooled = pool.poll();
            if (pooled == null) {
                pooled = createPooled();
            }
            return pooled;
        }

        @Override
        public void releasePooled(P pooled) {
            pool.offer(pooled);
        }

        @Override
        public int pooledCount() {
            return pool.size();
        }

        @Override
        public boolean clear() {
            pool.clear();
            return true;
        }

        // // // Other methods

        public int capacity() {
            return capacity;
        }
    }
}
