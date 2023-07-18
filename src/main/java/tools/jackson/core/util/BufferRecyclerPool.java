package tools.jackson.core.util;

public class BufferRecyclerPool {

    private static final ObjectPool<BufferRecycler> pool = ObjectPool.newLockFreePool(BufferRecycler::new);

    public static BufferRecycler borrowBufferRecycler() {
        return pool.borrow();
    }
    public static void offerBufferRecycler(BufferRecycler bufferRecycler) {
        pool.offer(bufferRecycler);
    }
}
