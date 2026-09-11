package tools.jackson.core.unittest.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.BufferRecycler;
import tools.jackson.core.util.JsonRecyclerPools;
import tools.jackson.core.util.RecyclerPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that {@link BufferRecycler} leased by {@link IOContext} gets
 * returned to the pool even if no parser or generator ends up being constructed:
 * context is otherwise only released when parser/generator that owns it is closed.
 */
class FailedConstructionRecyclerTest extends JacksonCoreTestBase
{
    /**
     * Factory that fails the way format backends can (say, when schema
     * validation fails), that is, after {@link IOContext} has been created.
     */
    static class FailingFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public FailingFactory(JsonFactoryBuilder b) { super(b); }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                InputStream in) throws JacksonException {
            throw new IllegalStateException("Test-induced construction failure");
        }

        @Override
        protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, Writer out) throws JacksonException {
            throw new IllegalStateException("Test-induced construction failure");
        }

        @Override
        protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, OutputStream out) throws JacksonException {
            throw new IllegalStateException("Test-induced construction failure");
        }
    }

    // Constraint violation trips before parser gets constructed
    @Test
    void releasesRecyclerOnFailedParserConstruction() throws Exception
    {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(5);
        JsonFactory f = JsonFactory.builder()
                .recyclerPool(pool)
                .streamReadConstraints(StreamReadConstraints.builder().maxDocumentLength(2).build())
                .build();
        assertEquals(0, pool.pooledCount());

        byte[] doc = utf8Bytes("{\"a\":1}");
        assertThrows(StreamConstraintsException.class,
                () -> f.createParser(ObjectReadContext.empty(), doc));
        assertEquals(1, pool.pooledCount());

        char[] chars = "{\"a\":1}".toCharArray();
        assertThrows(StreamConstraintsException.class,
                () -> f.createParser(ObjectReadContext.empty(), chars, 0, chars.length));
        assertEquals(1, pool.pooledCount());
    }

    // Caller-supplied bounds checked before parser gets constructed
    @Test
    void releasesRecyclerOnInvalidBounds() throws Exception
    {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(5);
        JsonFactory f = JsonFactory.builder().recyclerPool(pool).build();
        assertEquals(0, pool.pooledCount());

        byte[] doc = utf8Bytes("{\"a\":1}");
        assertThrows(StreamReadException.class,
                () -> f.createParser(ObjectReadContext.empty(), doc, 5, 100));
        assertEquals(1, pool.pooledCount());
    }

    @Test
    void releasesRecyclerOnFailedParserConstructionFromStream() throws Exception
    {
        RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(5);
        FailingFactory f = new FailingFactory(JsonFactory.builder().recyclerPool(pool));
        assertEquals(0, pool.pooledCount());

        assertThrows(IllegalStateException.class,
                () -> f.createParser(ObjectReadContext.empty(),
                        new ByteArrayInputStream(utf8Bytes("{\"a\":1}"))));
        assertEquals(1, pool.pooledCount());
    }

    @Test
    void releasesRecyclerOnFailedGeneratorConstruction() throws Exception
    {
        for (JsonEncoding enc : new JsonEncoding[] { JsonEncoding.UTF8, JsonEncoding.UTF16_BE }) {
            RecyclerPool<BufferRecycler> pool = JsonRecyclerPools.newBoundedPool(5);
            FailingFactory f = new FailingFactory(JsonFactory.builder().recyclerPool(pool));
            assertEquals(0, pool.pooledCount(), enc.toString());

            assertThrows(IllegalStateException.class,
                    () -> f.createGenerator(ObjectWriteContext.empty(),
                            new ByteArrayOutputStream(), enc));
            assertEquals(1, pool.pooledCount(), enc.toString());
        }
    }
}
