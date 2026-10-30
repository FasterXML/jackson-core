package tools.jackson.core.util;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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
 *  atomic slots, with no lock and no queue node per release (default slot count
 *  being {@link StripedArrayPoolBase#DEFAULT_CAPACITY}).
 * </li>
 * <li>{@link HybridPoolBase} combines a {@link ThreadLocalPoolBase} for platform
 *  threads with shared {@link StripedArrayPoolBase} slots for virtual threads.
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
     * released (see {@link WithPool#releaseToPool}) later after usage ends.
     * Actual acquisition is done by a call to {@link #acquirePooled()}.
     *<p>
     * Default implementation calls {@link #acquirePooled()} followed by
     * a call to {@link WithPool#withPool}.
     *
     * @return Pooled instance for caller to use; caller expected
     *   to call {@link WithPool#releaseToPool} after it is done using instance,
     *   not {@link #releasePooled}: that would leave the instance linked, so
     *   that linking it again on a later acquire fails.
     */
    default P acquireAndLinkPooled() {
        return acquirePooled().withPool(this);
    }

    /**
     * Method for sub-classes to implement for actual acquire logic; called
     * by {@link #acquireAndLinkPooled()}.
     * The instance is not linked to this pool: callers that use this method
     * directly release it with {@link #releasePooled}.
     *
     * @return Instance acquired (pooled or just constructed)
     */
    P acquirePooled();

    /**
     * Method that should be called when previously acquired (see {@link #acquirePooled})
     * pooled value that is no longer needed; this lets pool to take ownership
     * for possible reuse.
     * Instances acquired with {@link #acquireAndLinkPooled} are instead released
     * with {@link WithPool#releaseToPool}, which unlinks them and then calls
     * this method.
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
     * acquirers), and release stores into the first empty slot with
     * compare-and-set. Both scan every slot: a scan starts at a home slot
     * derived from the identity hash of the current thread, and wraps around.
     * Different threads start at different slots, which spreads contention,
     * and a thread tends to get back the instance it released last. But any
     * thread can take any retained instance, so a new thread (such as a new
     * virtual thread) reuses instances that other threads released.
     * Unlike {@link ConcurrentDequePoolBase} no queue node is allocated on
     * release, and unlike {@link BoundedPoolBase} there is no lock on either
     * path.
     *<p>
     * This is a "bounded" pool: it will never hold on to more pooled instances
     * than its slot count (default {@link StripedArrayPoolBase#DEFAULT_CAPACITY},
     * at most {@link StripedArrayPoolBase#MAX_CAPACITY}; requested counts are
     * rounded up to the next power of two, so unlike {@link BoundedPoolBase}
     * it may retain up to almost twice the requested number).
     * A release drops the instance only when its scan finds every slot
     * occupied, and an acquire creates a new one only when its scan finds
     * every slot empty. Each slot is on its own cache line, so such a scan
     * reads {@link #capacity()} cache lines (16 by default, at most 64)
     * before it drops or creates an instance. So when more instances are in
     * use at the same time than there are slots, as when many virtual threads
     * hold one while blocked on I/O, acquires and releases pay for a full
     * scan without recycling, and the pool can be slower than not pooling;
     * {@link ConcurrentDequePoolBase} has no such limit.
     * Special thanks to the Vert.x project for the approach.
     *<p>
     * The slot array is {@code transient} and is not restored on
     * deserialization: concrete subclasses must implement {@code readResolve}
     * to return a newly constructed (or shared) instance, as
     * {@link JsonRecyclerPools.StripedArrayPool} does.
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
         *<p>
         * Note: requested slot counts are rounded up to the next power of two
         * (required by slot indexing), so a pool may retain up to almost twice
         * as many instances as requested; e.g. a request for 33 gives 64
         * slots. Use {@link #capacity()} to see the actual count.
         */
        public final static int DEFAULT_CAPACITY = 16;

        /**
         * Maximum number of slots: larger requested sizes are silently
         * clamped to this.
         * Acquire and release scan every slot, so this bounds a scan that
         * finds no instance, or no empty slot, to 64 slot reads.
         */
        public final static int MAX_CAPACITY = 64;

        /**
         * Spacing between slots in the backing array, as a shift: slots are
         * 32 references apart, 128 bytes with compressed oops (256 bytes
         * without, which is harmless). This matches the JDK's {@code @Contended}
         * padding ({@code -XX:ContendedPaddingWidth}, 128 bytes by default)
         * used by the {@code Striped64} cells behind {@code LongAdder}: it
         * covers 128-byte cache lines (as on Apple silicon) and the x86
         * adjacent-line prefetcher, which fetches 64-byte lines in pairs.
         * So threads working on different slots do not contend on one line.
         * Without the spacing all 16 default slots share a line, and the
         * contended throughput of this pool falls below no pooling.
         */
        private static final int SLOT_SHIFT = 5;

        /**
         * Slot {@code i} is at index {@code (i + 1) << SLOT_SHIFT}: the
         * first stride is padding that keeps slot 0 off the cache line of
         * the array header and length.
         */
        private final transient AtomicReferenceArray<P> _slots;

        /**
         * Bit mask for mapping a scan index onto the slot array; the slot
         * count is always a power of two.
         */
        private final transient int _mask;

        /**
         * Shift that keeps the top {@code log2(capacity)} bits of a hash.
         * At capacity 1 the shift is 32, which Java reduces to 0; the result
         * is still correct because {@link #_mask} is 0.
         */
        private final transient int _hashShift;

        // // // Life-cycle (constructors, factory methods)

        protected StripedArrayPoolBase(int capacityAsId) {
            super(capacityAsId);
            final int capacity = _powerOfTwoCapacity(capacityAsId);
            _slots = new AtomicReferenceArray<>((capacity + 1) << SLOT_SHIFT);
            _mask = capacity - 1;
            _hashShift = Integer.numberOfLeadingZeros(capacity) + 1;
        }

        private static int _powerOfTwoCapacity(int capacityAsId) {
            if (capacityAsId <= 0) {
                return DEFAULT_CAPACITY;
            }
            if (capacityAsId >= MAX_CAPACITY) {
                return MAX_CAPACITY;
            }
            // Round up so any positive requested size works; the mask-based
            // slot indexing requires a power of two.
            int capacity = Integer.highestOneBit(capacityAsId);
            return (capacity == capacityAsId) ? capacity : (capacity << 1);
        }

        /**
         * Slot to start acquire/release scans from, derived from the identity
         * hash of the thread with Fibonacci hashing. Different threads start
         * at different slots, so under contention they mostly touch disjoint
         * slots, and a thread tends to get back the instance (and so the
         * warmed buffers) it released last.
         */
        private int _homeSlot(Thread thread) {
            return (System.identityHashCode(thread) * 0x9E3779B9) >>> _hashShift;
        }

        private int _index(int slot) {
            return ((slot & _mask) + 1) << SLOT_SHIFT;
        }

        private P _acquire(Thread thread) {
            final int home = _homeSlot(thread);
            for (int i = 0; i <= _mask; ++i) {
                final int ix = _index(home + i);
                if (_slots.get(ix) != null) {
                    P pooled = _slots.getAndSet(ix, null);
                    if (pooled != null) {
                        return pooled;
                    }
                }
            }
            return createPooled();
        }

        private void _release(P pooled, Thread thread) {
            final int home = _homeSlot(thread);
            for (int i = 0; i <= _mask; ++i) {
                final int ix = _index(home + i);
                if ((_slots.get(ix) == null) && _slots.compareAndSet(ix, null, pooled)) {
                    return;
                }
            }
            // All slots occupied: drop the instance. This is the retention
            // bound that keeps the pool from growing without limit.
        }

        // // // Actual API implementation

        @Override
        public P acquirePooled() {
            return _acquire(Thread.currentThread());
        }

        @Override
        public void releasePooled(P pooled) {
            _release(pooled, Thread.currentThread());
        }

        @Override
        public int pooledCount() {
            int count = 0;
            for (int i = 0; i <= _mask; ++i) {
                if (_slots.get(_index(i)) != null) {
                    ++count;
                }
            }
            return count;
        }

        @Override
        public boolean clear() {
            for (int i = 0; i <= _mask; ++i) {
                _slots.set(_index(i), null);
            }
            return true;
        }

        // // // Other methods

        public int capacity() {
            return _mask + 1;
        }
    }

    /**
     * {@link RecyclerPool} implementation that combines a
     * {@link ThreadLocalPoolBase} for platform threads with shared
     * {@link StripedArrayPoolBase} slots for virtual threads. Virtual threads
     * are typically created per task, so a per-thread instance would be
     * abandoned after a single use; the shared slots let their instances
     * recycle across threads.
     *<p>
     * Only {@link #acquireAndLinkPooled()} checks the thread type. On a
     * platform thread it returns the leave-in instance of the
     * {@code ThreadLocalPoolBase}: not linked, so releasing it does nothing,
     * and it stays with that thread. On a virtual thread it returns an
     * instance from the shared slots, linked to this pool.
     * {@link #createPooled()} creates only the instances of the shared slots:
     * the leave-ins come from the {@code ThreadLocalPoolBase} given to the
     * constructor, so customizing them takes a {@code ThreadLocalPoolBase}
     * implementation that keeps its own per-thread instances.
     * {@link #acquirePooled()} and {@link #releasePooled} use the shared slots
     * on every thread, so an instance acquired on a virtual thread may be
     * released on a platform thread, and the other way around. Do not pass a
     * leave-in to {@link #releasePooled}: another thread could then acquire it
     * while it is still in use.
     *<p>
     * {@link #capacity()} and {@link #pooledCount()} describe the shared slots
     * only, and leave-ins are not counted; {@link #pooledCount()} is {@code -1}
     * until the shared slots are allocated. {@link #clear()} empties the shared
     * slots and returns {@code false}, because leave-ins cannot be dropped.
     * The shared slots are allocated by the first release into them, so a pool
     * that only platform threads use through {@link #acquireAndLinkPooled()},
     * as Jackson uses it, never allocates them.
     *<p>
     * The virtual-thread check goes through a {@link MethodHandle} resolved
     * once, when this class is initialized, so this class also runs where
     * {@code Thread.isVirtual()} does not exist (JDK before 19, Android),
     * cannot be looked up (such as a native image without reflection
     * metadata for it) or cannot be invoked. Every thread is then a platform
     * thread.
     *<p>
     * Pool state is {@code transient} and not restored on deserialization:
     * concrete subclasses must implement {@code readResolve}, as
     * {@link JsonRecyclerPools.HybridPool} does.
     *
     * @since 3.3
     */
    abstract class HybridPoolBase<P extends WithPool<P>>
        extends StatefulImplBase<P>
    {
        private static final long serialVersionUID = 1L;

        /**
         * {@code Thread#isVirtual()} when the runtime has it, {@code null} when
         * it does not (JDK before 19, Android) or when the lookup fails, for
         * example with the {@link LinkageError} that a native image throws
         * for a method without reflection metadata.
         */
        private static final MethodHandle IS_VIRTUAL = _findIsVirtual();

        private static MethodHandle _findIsVirtual() {
            try {
                return MethodHandles.publicLookup().findVirtual(Thread.class,
                        "isVirtual", MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                return null;
            }
        }

        // Treats the thread as a platform thread if the call fails (such as
        // in a native image that cannot invoke the handle)
        private static boolean _isVirtual(Thread thread) {
            if (IS_VIRTUAL == null) {
                return false;
            }
            try {
                return (boolean) IS_VIRTUAL.invokeExact(thread);
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable t) {
                return false;
            }
        }

        private final transient ThreadLocalPoolBase<P> _platformPool;

        /**
         * Shared slots; {@code null} until the first release into them.
         */
        private transient volatile StripedArrayPoolBase<P> _shared;

        @SuppressWarnings("rawtypes")
        private static final AtomicReferenceFieldUpdater<HybridPoolBase, StripedArrayPoolBase> SHARED_UPDATER
            = AtomicReferenceFieldUpdater.newUpdater(HybridPoolBase.class,
                    StripedArrayPoolBase.class, "_shared");

        // // // Life-cycle (constructors, factory methods)

        /**
         * @param capacityAsId Requested number of shared slots, as with
         *   {@link StripedArrayPoolBase}
         * @param platformPool Pool that gives each platform thread its
         *   leave-in instance
         */
        protected HybridPoolBase(int capacityAsId, ThreadLocalPoolBase<P> platformPool) {
            super(capacityAsId);
            _platformPool = Objects.requireNonNull(platformPool);
        }

        // // // Actual API implementation

        @Override
        public P acquireAndLinkPooled() {
            final Thread thread = Thread.currentThread();
            if (_isVirtual(thread)) {
                return _acquireShared(thread).withPool(this);
            }
            return _platformPool.acquireAndLinkPooled();
        }

        @Override
        public P acquirePooled() {
            return _acquireShared(Thread.currentThread());
        }

        private P _acquireShared(Thread thread) {
            final StripedArrayPoolBase<P> shared = _shared;
            return (shared == null) ? createPooled() : shared._acquire(thread);
        }

        @Override
        public void releasePooled(P pooled) {
            StripedArrayPoolBase<P> shared = _shared;
            if (shared == null) {
                shared = _allocateShared();
            }
            shared._release(pooled, Thread.currentThread());
        }

        private StripedArrayPoolBase<P> _allocateShared() {
            final StripedArrayPoolBase<P> created = new SharedSlots<>(_serialization, this);
            if (SHARED_UPDATER.compareAndSet(this, null, created)) {
                return created;
            }
            return _shared;
        }

        @Override
        public int pooledCount() {
            final StripedArrayPoolBase<P> shared = _shared;
            return (shared == null) ? -1 : shared.pooledCount();
        }

        /**
         * Empties the shared slots, but returns {@code false} because the
         * leave-in instances of platform threads cannot be dropped.
         */
        @Override
        public boolean clear() {
            final StripedArrayPoolBase<P> shared = _shared;
            if (shared != null) {
                shared.clear();
            }
            return false;
        }

        // // // Other methods

        /**
         * @return Number of shared slots: requested count rounded up to a
         *    power of two, at most {@link StripedArrayPoolBase#MAX_CAPACITY}
         */
        public int capacity() {
            return StripedArrayPoolBase._powerOfTwoCapacity(_serialization);
        }

        private static final class SharedSlots<P extends WithPool<P>>
            extends StripedArrayPoolBase<P>
        {
            private static final long serialVersionUID = 1L;

            private final HybridPoolBase<P> _owner;

            SharedSlots(int capacityAsId, HybridPoolBase<P> owner) {
                super(capacityAsId);
                _owner = owner;
            }

            @Override
            public P createPooled() {
                return _owner.createPooled();
            }
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
