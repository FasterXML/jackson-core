package tools.jackson.core.util;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;
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

    static <T> ObjectPool<T> newLockFreePool(Supplier<T> factory) {
        return new LockFreeObjectPool<>(factory);
    }

    static <T> ObjectPool<T> newLockFreePool(Supplier<T> factory, Consumer<T> destroyer) {
        return new LockFreeObjectPool<>(factory, destroyer);
    }

    class LockFreeObjectPool<T> implements ObjectPool<T> {
        private final Supplier<T> factory;
        private final Consumer<T> destroyer;

        private final Queue<T> pool = new LinkedTransferQueue<>();

        public LockFreeObjectPool(Supplier<T> factory) {
            this(factory, null);
        }

        public LockFreeObjectPool(Supplier<T> factory, Consumer<T> destroyer) {
            this.factory = factory;
            this.destroyer = destroyer;
        }

        @Override
        public T borrow() {
//            System.out.println("Before borrow size: " + pool.size());
            T t = pool.poll();
            return t != null ? t : factory.get();
        }

        @Override
        public void offer(T t) {
            pool.offer(t);
//            System.out.println("After offer size: " + pool.size());
        }

        @Override
        public void close() throws Exception {
            if (destroyer != null) {
                pool.forEach(destroyer);
            }
        }
    }
}
