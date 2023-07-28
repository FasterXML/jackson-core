package com.fasterxml.jackson.core.util;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This is a utility class, whose main functionality is pooling object
 * with a huge memory footprint and that are costly to be recreated at
 * every usage like the {@link BufferRecycler}. It is intended for
 * internal use only.
 *
 * @since 2.16
 */
public interface ObjectPool<T> extends AutoCloseable {

    T acquire();
    void release(T t);

    default void withPooledObject(Consumer<T> objectConsumer) {
        T t = acquire();
        try {
            objectConsumer.accept(t);
        } finally {
            release(t);
        }
    }

    enum Strategy {
        CONCURRENT_DEQUEUE(ConcurrentDequePool::new, false), LOCK_FREE(LockFreePool::new, false),
        DEBUG_CONCURRENT_DEQUEUE(ConcurrentDequePool::new, true), DEBUG_LOCK_FREE(LockFreePool::new, true);

        private final Function<Supplier, ObjectPool> constructor;

        private final boolean debug;

        Strategy(Function<Supplier, ObjectPool> constructor, boolean debug) {
            this.constructor = constructor;
            this.debug = debug;
        }

        <T> ObjectPool<T> newObjectPool(Supplier<T> factory) {
            ObjectPool<T> pool = constructor.apply(factory);
            return debug ? new DebugPoolDecorator<>(pool) : pool;
        }
    }

    class StrategyHolder {
        private static Strategy strategy = Strategy.LOCK_FREE;

        public static void setStrategy(String name) {
            strategy = Strategy.valueOf(name);
        }
    }

    static <T> ObjectPool<T> newObjectPool(Supplier<T> factory) {
        return StrategyHolder.strategy.newObjectPool(factory);
    }

    class ConcurrentDequePool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;
        private final Consumer<T> destroyer;

        private final Deque<T> pool = new ConcurrentLinkedDeque<>();

        public ConcurrentDequePool(Supplier<T> factory) {
            this(factory, null);
        }

        public ConcurrentDequePool(Supplier<T> factory, Consumer<T> destroyer) {
            this.factory = factory;
            this.destroyer = destroyer;
        }

        @Override
        public T acquire() {
            T t = pool.pollFirst();
            return t != null ? t : factory.get();
        }

        @Override
        public void release(T t) {
            pool.offerLast(t);
        }

        @Override
        public void close() throws Exception {
            if (destroyer != null) {
                pool.forEach(destroyer);
            }
        }
    }

    class LockFreePool<T> implements ObjectPool<T> {
        private final AtomicReference<Node<T>> head = new AtomicReference<>();

        private final Supplier<T> factory;

        public LockFreePool(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T acquire() {
            for (int i = 0; i < 3; i++) {
                Node<T> currentHead = head.get();
                if (currentHead == null) {
                    return factory.get();
                }
                if (head.compareAndSet(currentHead, currentHead.next)) {
                    currentHead.next = null;
                    return currentHead.value;
                }
            }
            return factory.get();
        }

        @Override
        public void release(T object) {
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

        static class Node<T> {
            final T value;
            Node<T> next;

            Node(T value) {
                this.value = value;
            }
        }
    }

    class DebugPoolDecorator<T> implements ObjectPool<T> {

        private final ObjectPool<T> pool;

        private final LongAdder acquireCounter = new LongAdder();
        private final LongAdder releaseCounter = new LongAdder();

        public DebugPoolDecorator(ObjectPool<T> pool) {
            this.pool = pool;
        }

        @Override
        public T acquire() {
            acquireCounter.increment();
            return pool.acquire();
        }

        @Override
        public void release(T t) {
            releaseCounter.increment();
            pool.release(t);
        }

        @Override
        public void close() throws Exception {
            System.out.println("Closing " + this);
            pool.close();
        }

        @Override
        public String toString() {
            return "DebugPoolDecorator{" +
                    "acquires = " + acquireCounter.sum() +
                    ", releases = " + releaseCounter.sum() +
                    '}';
        }
    }
}
