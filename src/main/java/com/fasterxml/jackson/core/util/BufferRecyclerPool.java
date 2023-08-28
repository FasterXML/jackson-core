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
        return PoolStrategy.THREAD_LOCAL.getPool();
    }

    enum PoolStrategy {
        NO_OP(new NonRecyclingRecyclerPool()),
        THREAD_LOCAL(new ThreadLocalRecyclerPool()), 
        CONCURRENT_DEQUEUE(new ConcurrentDequePool()),
        LOCK_FREE(new LockFreePool());

        private final BufferRecyclerPool pool;

        PoolStrategy(BufferRecyclerPool pool) {
            this.pool = pool;
        }

        public BufferRecyclerPool getPool() {
            return pool;
        }
    }

    /**
     * Default {@link BufferRecyclerPool} implementation that uses
     * {@link ThreadLocal} for recycling instances.
     *
     * @since 2.16
     */
    class ThreadLocalRecyclerPool implements BufferRecyclerPool {

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

        @Override
        public BufferRecycler acquireBufferRecycler() {
            return new BufferRecycler();
        }

        @Override
        public void releaseBufferRecycler(BufferRecycler recycler) {
            ; // nothing to do, relies on ThreadLocal
        }
    }

    /**
     * {@link BufferRecyclerPool} implementation that uses
     * {@link ConcurrentLinkedDeque} for recycling instances.
     *
     * @since 2.16
     */
    class ConcurrentDequePool implements BufferRecyclerPool {
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
        private final AtomicReference<LockFreePool.Node> head = new AtomicReference<>();

        @Override
        public BufferRecycler acquireBufferRecycler() {
            return getBufferRecycler().withPool(this);
        }

        private BufferRecycler getBufferRecycler() {
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
