package com.fasterxml.jackson.core.util;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        DUMMY, BUFFER_RECYCLERS, CONCURRENT_DEQUEUE, LOCK_FREE
    }

    class StrategyHolder {
        private static Strategy strategy = Strategy.LOCK_FREE;

        public static void setStrategy(String name) {
            strategy = Strategy.valueOf(name);
        }
    }

    static <T> ObjectPool<T> newObjectPool(Supplier<T> factory) {
        switch (StrategyHolder.strategy) {
            case DUMMY: return new DummyPool<>(factory);
            case BUFFER_RECYCLERS: return new DummyPool<>(() -> (T) BufferRecyclers.getBufferRecycler());
            case CONCURRENT_DEQUEUE: return new ConcurrentDequePool<>(factory);
            case LOCK_FREE: return new LockFreePool<>(factory);
        }
        throw new UnsupportedOperationException();
    }

    class DummyPool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;


        public DummyPool(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T acquire() {
            return factory.get();
        }

        @Override
        public void release(T t) {

        }

        @Override
        public void close() throws Exception {

        }
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
}
