package com.fasterxml.jackson.core.util;

import org.jctools.queues.MpmcArrayQueue;

import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ObjectPool<T> extends AutoCloseable {

    T borrow();
    void offer(T t);

    default void withPooledObject(Consumer<T> objectConsumer) {
        T t = borrow();
        try {
            objectConsumer.accept(t);
        } finally {
            offer(t);
        }
    }

    enum Strategy {
        DUMMY, BUFFER_RECYCLERS, LINKED_QUEUE,
        ATOMIC_REF_ARRAY_16, ATOMIC_REF_ARRAY_64,
        SYNCED_ARRAY_16, SYNCED_ARRAY_64,
        SHARDED_SYNCED_ARRAY_4_64, SHARDED_SYNCED_ARRAY_16_16,
        LOCK_FREE, LOCK_FREE_ASYNC_OFFER,
        JCTOOLS_16, JCTOOLS_64
    }

    class StrategyHolder {
        private static Strategy strategy = Strategy.SYNCED_ARRAY_16;

        public static void setStrategy(String name) {
            strategy = Strategy.valueOf(name);
        }
    }

    static <T> ObjectPool<T> newLockFreePool(Supplier<T> factory) {
        switch (StrategyHolder.strategy) {
            case DUMMY: return new DummyPool<>(factory);
            case BUFFER_RECYCLERS: return new DummyPool<>(() -> (T) BufferRecyclers.getBufferRecycler());
            case LINKED_QUEUE: return new LinkedQueuePool<>(factory);
            case ATOMIC_REF_ARRAY_16: return new AtomicReferenceArrayPool<>(factory, 16);
            case ATOMIC_REF_ARRAY_64: return new AtomicReferenceArrayPool<>(factory, 64);
            case SYNCED_ARRAY_16: return new SyncedArrayPool<>(factory, 16);
            case SYNCED_ARRAY_64: return new SyncedArrayPool<>(factory, 64);
            case SHARDED_SYNCED_ARRAY_4_64: return new ShardedSyncedArrayPool<>(factory, 4, 64);
            case SHARDED_SYNCED_ARRAY_16_16: return new ShardedSyncedArrayPool<>(factory, 16, 16);
            case LOCK_FREE: return new LockFreePool<>(factory);
            case LOCK_FREE_ASYNC_OFFER: return new LockFreeAsyncOfferPool<>(factory);
            case JCTOOLS_16: return new JCToolsPool<>(factory, 16);
            case JCTOOLS_64: return new JCToolsPool<>(factory, 64);
        }
        throw new UnsupportedOperationException();
    }

    class LinkedQueuePool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;
        private final Consumer<T> destroyer;

        private final Queue<T> pool = new LinkedTransferQueue<>();

        private final AtomicInteger counter = new AtomicInteger(0);

        public LinkedQueuePool(Supplier<T> factory) {
            this(factory, null);
        }

        public LinkedQueuePool(Supplier<T> factory, Consumer<T> destroyer) {
            this.factory = factory;
            this.destroyer = destroyer;
        }

        @Override
        public T borrow() {
//            System.out.println("Borrow: " + counter.incrementAndGet());
            T t = pool.poll();
            return t != null ? t : factory.get();
        }

        @Override
        public void offer(T t) {
            pool.offer(t);
//            System.out.println("Offer: " + counter.decrementAndGet());
        }

        @Override
        public void close() throws Exception {
            if (destroyer != null) {
                pool.forEach(destroyer);
            }
        }
    }

    class AtomicReferenceArrayPool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;

        private final AtomicInteger counter = new AtomicInteger(0);

        private final int size;

        private final AtomicReferenceArray<T> pool;

        public AtomicReferenceArrayPool(Supplier<T> factory, int size) {
            this.factory = factory;
            this.size = size;
            this.pool = new AtomicReferenceArray<>(size);
        }

        @Override
        public T borrow() {
            int pos = counter.getAndUpdate(i -> i > 0 ? i-1 : i);
            if (pos <= 0) {
                return factory.get();
            }
            T t = pool.getAndSet(pos-1, null);
            return t == null ? borrow() : t;
        }

        @Override
        public void offer(T t) {
            int pos = counter.getAndUpdate(i -> i < 0 ? 1 : i+1);
            if (pos < 0) {
                pool.set(0, t);
            } else if (pos < size) {
                pool.set(pos, t);
            }
        }

        @Override
        public void close() throws Exception {

        }
    }

    class SyncedArrayPool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;

        private final int size;

        private final Object[] pool;

        private final ReentrantLock lock = new ReentrantLock();

        private volatile int counter = 0;

        public SyncedArrayPool(Supplier<T> factory, int size) {
            this.factory = factory;
            this.size = size;
            this.pool = new Object[size];
        }

        @Override
        public T borrow() {
            lock.lock();
            try {
                if (counter == 0) {
                    return factory.get();
                }
                T t = (T) pool[--counter];
                pool[counter] = null;
                return t;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void offer(T t) {
            lock.lock();
            try {
                if (counter < size) {
                    pool[counter++] = t;
                }
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void close() throws Exception {

        }
    }

    class ShardedSyncedArrayPool<T> implements ObjectPool<T> {
        private final SyncedArrayPool<T>[] pools;

        public ShardedSyncedArrayPool(Supplier<T> factory, int shardsNr, int shardSize) {
            this.pools = new SyncedArrayPool[shardsNr];
            for (int i = 0; i < pools.length; i++) {
                pools[i] = new SyncedArrayPool(factory, shardSize);
            }
        }

        @Override
        public T borrow() {
            return pools[Thread.currentThread().hashCode() % pools.length].borrow();
        }

        @Override
        public void offer(T t) {
            pools[Thread.currentThread().hashCode() % pools.length].offer(t);
        }

        @Override
        public void close() throws Exception {

        }
    }

    class DummyPool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;


        public DummyPool(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T borrow() {
            return factory.get();
        }

        @Override
        public void offer(T t) {

        }

        @Override
        public void close() throws Exception {

        }
    }

    class JCToolsPool<T> implements ObjectPool<T> {
        private final MpmcArrayQueue<T> queue;

        private final Supplier<T> factory;

        public JCToolsPool(Supplier<T> factory, int size) {
            this.factory = factory;
            this.queue = new MpmcArrayQueue<>(size);
        }

        @Override
        public T borrow() {
            T t = queue.poll();
            return t == null ? factory.get() : t;
        }

        @Override
        public void offer(T t) {
            queue.offer(t);
        }

        @Override
        public void close() throws Exception {

        }
    }

    class LockFreePool<T> implements ObjectPool<T> {
        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        private final Supplier<T> factory;

        public LockFreePool(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T borrow() {
            for (int i = 0; i < 3; i++) {
                Node<T> currentHead = head.get();
                if (currentHead == null) {
                    return factory.get();
                }
                if (head.compareAndSet(currentHead, currentHead.next)) {
                    return currentHead.value;
                }
            }
            return factory.get();
        }

        @Override
        public void offer(T object) {
            Node<T> newHead = new Node<>(object);
            for (int i = 0; i < 3; i++) {
                newHead.next = head.get();
                if (head.compareAndSet(newHead.next, newHead)) {
                    return;
                }
            }
        }

        @Override
        public void close() throws Exception {

        }
    }

    class LockFreeAsyncOfferPool<T> extends LockFreePool<T> {

        private static final Executor asyncOffer = Executors.newSingleThreadExecutor();

        public LockFreeAsyncOfferPool(Supplier<T> factory) {
            super(factory);
        }

        @Override
        public void offer(T object) {
            asyncOffer.execute( () -> super.offer(object) );
        }
    }

    class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }
}
