package tools.jackson.core.unittest.base;

import java.io.Closeable;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.base.DecorableTSFactory;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DecorableTSFactory} failed-construction cleanup helpers, called
 * from a format backend (that is: a sub-class in another package, the way backends
 * that extend {@code DecorableTSFactory} directly -- like {@code IonFactory} -- have
 * to, since they implement their own {@code File} / {@code Path} create methods).
 */
@SuppressWarnings("serial")
class FailedConstructionHelpersTest extends JacksonCoreTestBase
{
    /**
     * Stand-in for a format backend: only exposes the {@code protected} helpers so
     * that they can be exercised directly.
     */
    static class BackendFactory extends JsonFactory {
        public BackendFactory(RecyclerPool<BufferRecycler> pool) {
            super(JsonFactory.builder().recyclerPool(pool));
        }

        public IOContext newContext() {
            return _createContext(_createContentReference(this), true);
        }

        public void release(IOContext ioCtxt, RuntimeException failure) {
            _releaseOnFailedConstruction(ioCtxt, failure);
        }

        public void close(Closeable toClose, Closeable rawFallback, RuntimeException failure) {
            _closeOnFailedConstruction(toClose, rawFallback, failure);
        }
    }

    static class TrackedCloseable implements Closeable {
        private final boolean _failOnClose;

        public int closeCount;

        public TrackedCloseable(boolean failOnClose) { _failOnClose = failOnClose; }

        @Override
        public void close() throws IOException {
            ++closeCount;
            if (_failOnClose) {
                throw new IOException("Test-induced close failure");
            }
        }
    }

    private BackendFactory factoryWith(RecyclerPool<BufferRecycler> pool) {
        return new BackendFactory(pool);
    }

    // Context is released, returning `BufferRecycler` lease to the pool
    @Test
    void releaseReturnsRecyclerToPool() {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(5);
        BackendFactory f = factoryWith(pool);
        IOContext ioCtxt = f.newContext();
        assertEquals(0, pool.pooledCount());

        RuntimeException failure = new IllegalStateException("Construction failed");
        f.release(ioCtxt, failure);

        assertEquals(1, pool.pooledCount());
        assertEquals(0, failure.getSuppressed().length);
    }

    // Backends create the context within their `try` block, so handler may be
    // reached before there is anything to release
    @Test
    void releaseAllowsNullContext() {
        BackendFactory f = factoryWith(JsonRecyclerPools.newBoundedPool(5));
        RuntimeException failure = new IllegalStateException("Construction failed");

        assertDoesNotThrow(() -> f.release(null, failure));
        assertEquals(0, failure.getSuppressed().length);
    }

    // Closing wrapper is expected to close what it wraps: no double close
    @Test
    void closeOnlyClosesWrapperIfItSucceeds() {
        BackendFactory f = factoryWith(JsonRecyclerPools.newBoundedPool(5));
        TrackedCloseable raw = new TrackedCloseable(false);
        TrackedCloseable wrapper = new TrackedCloseable(false);
        RuntimeException failure = new IllegalStateException("Construction failed");

        f.close(wrapper, raw, failure);

        assertEquals(1, wrapper.closeCount);
        assertEquals(0, raw.closeCount);
        assertEquals(0, failure.getSuppressed().length);
    }

    // ... but if it fails, source/target Jackson opened must not be left leaked
    @Test
    void closeFallsBackToRawIfWrapperCloseFails() {
        BackendFactory f = factoryWith(JsonRecyclerPools.newBoundedPool(5));
        TrackedCloseable raw = new TrackedCloseable(false);
        TrackedCloseable wrapper = new TrackedCloseable(true);
        RuntimeException failure = new IllegalStateException("Construction failed");

        f.close(wrapper, raw, failure);

        assertEquals(1, wrapper.closeCount);
        assertEquals(1, raw.closeCount);
        assertEquals(1, failure.getSuppressed().length);
    }

    // Same if wrapper was never created (failure before decoration)
    @Test
    void closeUsesRawIfWrapperNotCreated() {
        BackendFactory f = factoryWith(JsonRecyclerPools.newBoundedPool(5));
        TrackedCloseable raw = new TrackedCloseable(false);
        RuntimeException failure = new IllegalStateException("Construction failed");

        f.close(null, raw, failure);

        assertEquals(1, raw.closeCount);
        assertEquals(0, failure.getSuppressed().length);
    }

    // And nothing to close is fine too
    @Test
    void closeAllowsNothingToClose() {
        BackendFactory f = factoryWith(JsonRecyclerPools.newBoundedPool(5));
        RuntimeException failure = new IllegalStateException("Construction failed");

        assertDoesNotThrow(() -> f.close(null, null, failure));
        assertEquals(0, failure.getSuppressed().length);
    }
}
