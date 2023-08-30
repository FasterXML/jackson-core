package com.fasterxml.jackson.core.util;

import java.io.Serializable;
import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Interface for entity that controls creation and possible reuse of {@link BufferRecycler}
 * instances used for recycling of underlying input/output buffers.
 *<p>
 * Different pool implementations use different strategies on retaining
 * recyclers for reuse. For example we have:
 *<ul>
 * <li>{@link NonRecyclingPool} which does not retain any recyclers and
 * will always simply construct and return new instance when {@code acquireBufferRecycler}
 * is called
 *  </li>
 * <li>{@link ThreadLocalPool} which uses {@link ThreadLocal} to retain at most
 *   1 recycler per {@link Thread}.
 * </li>
 * <li>{@link BoundedPool} is "bounded pool" and retains at most N recyclers (default value being
 *  {@link BoundedPool#DEFAULT_CAPACITY}) at any given time.
 *  </li>
 * <li>Two implementations -- {@link ConcurrentDequePool}, {@link LockFreePool}
 *   -- are "unbounded" and retain any number of recyclers released: in practice
 *   it is at most the highest number of concurrently used {@link BufferRecycler}s.
 *  </li>
 *</ul>
 *
 *<p>
 * Default implementations are also included as nested classes.
 *
 * @since 2.16
 */
public interface BufferRecyclerPool extends Serializable
{
    /**
     * Method called to acquire {@link BufferRecycler}; possibly
     * (but necessarily) a pooled recycler instance (depends on implementation
     * and pool state).
     *
     * @return {@link BufferRecycler} for caller to use; caller expected
     *   to call {@link #releaseBufferRecycler} after it is done using recycler.
     */
    BufferRecycler acquireBufferRecycler();

    /**
     * Method that should be called when previously acquired (see {@link #acquireBufferRecycler})
     * recycler instances is no longer needed; this lets pool to take ownership
     * for possible reuse.
     *
     * @param recycler
     */
    void releaseBufferRecycler(BufferRecycler recycler);

    /**
     * @return the default {@link BufferRecyclerPool} implementation
     *   which is the thread local based one:
     *   basically alias to {@link #threadLocalPool()}).
     */
    static BufferRecyclerPool defaultPool() {
        return threadLocalPool();
    }

    /**
     * @return Globally shared instance of {@link ThreadLocalPool}; same as calling
     *   {@link ThreadLocalPool#shared()}.
     */
    static BufferRecyclerPool threadLocalPool() {
        return ThreadLocalPool.shared();
    }

    /**
     * @return Globally shared instance of {@link NonRecyclingPool}; same as calling
     *   {@link NonRecyclingPool#shared()}.
     */
    static BufferRecyclerPool nonRecyclingPool() {
        return NonRecyclingPool.shared();
    }

    /*
    /**********************************************************************
    /* Default BufferRecyclerPool implementations
    /**********************************************************************
     */

    /**
     * Default {@link BufferRecyclerPool} implementation that uses
     * {@link ThreadLocal} for recycling instances. {@link BufferRecycler}
     * instances are stored using {@link java.lang.ref.SoftReference}s so that
     * they may be Garbage Collected as needed by JVM.
     *<p>
     * Note that this implementation may not work well on platforms where
     * {@link java.lang.ref.SoftReference}s are not well supported (like
     * Android), or on platforms where {@link java.lang.Thread}s are not
     * long-living or reused (like Project Loom).
     */
    class ThreadLocalPool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        private static final BufferRecyclerPool GLOBAL = new ThreadLocalPool();

        /**
         * Accessor for the global, shared instance of {@link ThreadLocal}-based
         * pool: due to its nature it is essentially Singleton as there can only
         * be a single recycled {@link BufferRecycler} per thread.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return GLOBAL;
        }

        // No instances beyond shared one should be constructed
        private ThreadLocalPool() { }

        // // // JDK serialization support

        protected Object readResolve() { return GLOBAL; }

        // // // JDK serialization support

        @SuppressWarnings("deprecation")
        @Override
        public BufferRecycler acquireBufferRecycler() {
            return BufferRecyclers.getBufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, relies on ThreadLocal
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that does not use
     * any pool but simply creates new instances when necessary.
     */
    class NonRecyclingPool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        private static final BufferRecyclerPool GLOBAL = new NonRecyclingPool();

        // No instances beyond shared one should be constructed
        private NonRecyclingPool() { }

        /**
         * Accessor for the shared singleton instance; due to implementation having no state
         * this is preferred over creating instances.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return GLOBAL;
        }

        // // // JDK serialization support

        protected Object readResolve() { return GLOBAL; }

        // // // Actual API implementation
        
        @Override
        public BufferRecycler acquireBufferRecycler() {
            // Could link back to this pool as marker? For now just leave back-ref empty
            return new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, there is no underlying pool
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * {@link ConcurrentLinkedDeque} for recycling instances.
     *<p>
     * Pool is unbounded: see {@link BufferRecyclerPool} what this means.
     */
    class ConcurrentDequePool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        private static final BufferRecyclerPool GLOBAL = new ConcurrentDequePool();

        private final transient Deque<BufferRecycler> pool;

        // // // Life-cycle (constructors, factory methods)

        protected ConcurrentDequePool() {
            pool = new ConcurrentLinkedDeque<>();
        }

        /**
         * Accessor for getting the globally shared singleton instance.
         * Note that if you choose to use this instance,
         * pool may be shared by many other {@code JsonFactory} instances.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return GLOBAL;
        }

        /**
         * Accessor for creating and returning a new, non-shared pool instance.
         *
         * @return Newly constructed, non-shared pool instance
         */
        public static BufferRecyclerPool nonShared() {
            return new ConcurrentDequePool();
        }

        // // // JDK serialization support

        /**
         * To avoid serializing pool contents we made {@code pool} {@code transient};
         * to compensate, need to re-create proper instance using constructor.
         */
        protected Object readResolve() {
            return new ConcurrentDequePool();
        }

        // // // Actual API implementation
        
        @Override
        public BufferRecycler acquireBufferRecycler() {
            return getBufferRecycler().withPool(this);
        }

        private BufferRecycler getBufferRecycler() {
            BufferRecycler bufferRecycler = pool.pollFirst();
            return bufferRecycler != null ? bufferRecycler : new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler bufferRecycler) {
            pool.offerLast(bufferRecycler);
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * a lock free linked list for recycling instances.
     *<p>
     * Pool is unbounded: see {@link BufferRecyclerPool} what this means.
     */
    class LockFreePool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        /**
         * Globally shared pool instance.
         */
        private static final BufferRecyclerPool GLOBAL = new LockFreePool();

        // Needs to be transient to avoid JDK serialization from writing it out
        private final transient AtomicReference<LockFreePool.Node> head;

        // // // Life-cycle (constructors, factory methods)

        private LockFreePool() {
            head = new AtomicReference<>();
        }

        /**
         * Accessor for getting the globally shared singleton instance.
         * Note that if you choose to use this instance,
         * pool may be shared by many other {@code JsonFactory} instances.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return GLOBAL;
        }

        /**
         * Accessor for creating and returning a new, non-shared pool instance.
         *
         * @return Newly constructed, non-shared pool instance
         */
        public static BufferRecyclerPool nonShared() {
            return new LockFreePool();
        }

        // // // JDK serialization support

        /**
         * To avoid serializing pool contents we made {@code head} {@code transient};
         * to compensate, need to re-create proper instance using constructor.
         */
        protected Object readResolve() {
            return new LockFreePool();
        }

        // // // Actual API implementation

        @Override
        public BufferRecycler acquireBufferRecycler() {
            return getBufferRecycler().withPool(this);
        }

        private BufferRecycler getBufferRecycler() {
            // This simple lock free algorithm uses an optimistic compareAndSet strategy to
            // populate the underlying linked list in a thread-safe way. However, under very
            // heavy contention, the compareAndSet could fail multiple times, so it seems a
            // reasonable heuristic to limit the number of retries in this situation.
            for (int i = 0; i < 3; i++) {
                Node currentHead = head.get();
                if (currentHead == null) {
                    return new BufferRecycler();
                }
                if (head.compareAndSet(currentHead, currentHead.next)) {
                    currentHead.next = null;
                    return currentHead.value;
                }
            }
            return new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler bufferRecycler) {
            LockFreePool.Node newHead = new LockFreePool.Node(bufferRecycler);
            for (int i = 0; i < 3; i++) {
                newHead.next = head.get();
                if (head.compareAndSet(newHead.next, newHead)) {
                    return;
                }
            }
        }

        static class Node {
            final BufferRecycler value;
            LockFreePool.Node next;

            Node(BufferRecycler value) {
                this.value = value;
            }
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * a bounded queue ({@link ArrayBlockingQueue} for recycling instances.
     * This is "bounded" pool since it will never hold on to more
     * {@link BufferRecycler} instances than its size configuration:
     * the default size is {@link BoundedPool#DEFAULT_CAPACITY}.
     */
    class BoundedPool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        /**
         * Default capacity which limits number of recyclers that are ever
         * retained for reuse.
         */
        public final static int DEFAULT_CAPACITY = 100;

        private static final BufferRecyclerPool GLOBAL = new BoundedPool(DEFAULT_CAPACITY);

        private final transient Queue<BufferRecycler> pool;

        // // // Life-cycle (constructors, factory methods)

        protected BoundedPool(int capacity) {
            pool = new ArrayBlockingQueue<>(capacity);
        }

        /**
         * Accessor for getting the globally shared singleton instance.
         * Note that if you choose to use this instance,
         * pool may be shared by many other {@code JsonFactory} instances.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return GLOBAL;
        }

        /**
         * Accessor for creating and returning a new, non-shared pool instance.
         *
         * @return Newly constructed, non-shared pool instance
         */
        public static BufferRecyclerPool nonShared(int capacity) {
            return new BoundedPool(capacity);
        }

        // // // JDK serialization support

        /**
         * To avoid serializing pool contents we made {@code pool} {@code transient};
         * to compensate, need to re-create proper instance using constructor.
         */
        protected Object readResolve() {
            return GLOBAL;
        }

        // // // Actual API implementation

        @Override
        public BufferRecycler acquireBufferRecycler() {
            return getBufferRecycler().withPool(this);
        }

        private BufferRecycler getBufferRecycler() {
            BufferRecycler bufferRecycler = pool.poll();
            return bufferRecycler != null ? bufferRecycler : new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler bufferRecycler) {
            pool.offer(bufferRecycler);
        }
    }
}
