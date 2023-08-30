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
 *
 * @since 2.16
 */
public interface BufferRecyclerPool extends Serializable
{
    BufferRecycler acquireBufferRecycler();

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
     * @return Shared instance of {@link ThreadLocalPool}; same as calling
     *   {@link ThreadLocalPool#shared()}.
     */
    static BufferRecyclerPool threadLocalPool() {
        return ThreadLocalPool.shared();
    }

    /**
     * @return Shared instance of {@link NonRecyclingPool}; same as calling
     *   {@link NonRecyclingPool#shared()}.
     */
    static BufferRecyclerPool nonRecyclingPool() {
        return NonRecyclingPool.shared();
    }
    
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

        private static final BufferRecyclerPool SHARED = new ThreadLocalPool();

        /**
         * Accessor for the global, shared instance of {@link ThreadLocal}-based
         * pool: due to its nature it is essentially Singleton as there can only
         * be a single recycled {@link BufferRecycler} per thread.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return SHARED;
        }

        // No instances beyond shared one should be constructed
        private ThreadLocalPool() { }

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

        private static final BufferRecyclerPool SHARED = new NonRecyclingPool();

        // No instances beyond shared one should be constructed
        private NonRecyclingPool() { }

        /**
         * Accessor for the shared singleton instance; due to implementation having no state
         * this is preferred over creating instances.
         *
         * @return Shared pool instance
         */
        public static BufferRecyclerPool shared() {
            return SHARED;
        }

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
     */
    class ConcurrentDequePool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        private static final BufferRecyclerPool SHARED = new ConcurrentDequePool();

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
            return SHARED;
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
     */
    class LockFreePool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        /**
         * Globally shared pool instance.
         */
        private static final BufferRecyclerPool SHARED = new LockFreePool();

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
            return SHARED;
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
     * a bounded queue for recycling instances.
     */
    class BoundedPool implements BufferRecyclerPool
    {
        private static final long serialVersionUID = 1L;

        private static final BufferRecyclerPool SHARED = new BoundedPool(16);

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
            return SHARED;
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
            return SHARED;
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
