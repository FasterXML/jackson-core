package com.fasterxml.jackson.core.util;

import java.io.Serializable;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Interface for entity that controls creation and possible reuse of {@link BufferRecycler}
 * instances used for recycling of underlying input/output buffers.
 *
 * @since 2.16
 */
public interface BufferRecyclerPool extends Serializable {

    BufferRecycler acquireBufferRecycler();

    void releaseBufferRecycler(BufferRecycler recycler);

    /**
     * Returns the default BufferRecyclerPool implementation which is the thread local based one.
     */
    static BufferRecyclerPool defaultRecyclerPool() {
        return ThreadLocalRecyclerPool.INSTANCE;
    }

    /**
     * Default {@link BufferRecyclerPool} implementation that uses
     * {@link ThreadLocal} for recycling instances.
     *
     * @since 2.16
     */
    class ThreadLocalRecyclerPool implements BufferRecyclerPool {

        public static final BufferRecyclerPool INSTANCE = new ThreadLocalRecyclerPool();

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
     *
     * @since 2.16
     */
    class NonRecyclingRecyclerPool implements BufferRecyclerPool {

        public static final BufferRecyclerPool INSTANCE = new NonRecyclingRecyclerPool();

        @Override
        public BufferRecycler acquireBufferRecycler() {
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
     *
     * @since 2.16
     */
    class ConcurrentDequePool implements BufferRecyclerPool {

        public static final BufferRecyclerPool INSTANCE = new ConcurrentDequePool();

        private final Deque<BufferRecycler> pool = new ConcurrentLinkedDeque<>();

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
     *
     * @since 2.16
     */
    class LockFreePool implements BufferRecyclerPool {

        public static final BufferRecyclerPool INSTANCE = new LockFreePool();

        private final AtomicReference<LockFreePool.Node> head = new AtomicReference<>();

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

        static class Node implements Serializable {
            final BufferRecycler value;
            LockFreePool.Node next;

            Node(BufferRecycler value) {
                this.value = value;
            }
        }
    }
}
