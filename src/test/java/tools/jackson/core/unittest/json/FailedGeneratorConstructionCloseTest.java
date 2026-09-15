package tools.jackson.core.unittest.json;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.base.BinaryTSFactory;
import tools.jackson.core.base.DecorableTSFactory;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.OutputDecorator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.JsonGeneratorDecorator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that streams Jackson itself opens for {@link File} /
 * {@link java.nio.file.Path} targets get closed if construction of generator
 * fails after opening.
 */
class FailedGeneratorConstructionCloseTest extends JacksonCoreTestBase
{
    static class CloseTrackingOutputStream extends FilterOutputStream {
        public boolean closed;

        CloseTrackingOutputStream(OutputStream out) { super(out); }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    static class CloseTrackingWriter extends FilterWriter {
        public boolean closed;

        CloseTrackingWriter(Writer out) { super(out); }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    static class TrackingOutputDecorator extends OutputDecorator {
        private static final long serialVersionUID = 1L;

        public final List<CloseTrackingOutputStream> outputs = new ArrayList<>();
        public final List<CloseTrackingWriter> writers = new ArrayList<>();

        @Override
        public OutputStream decorate(IOContext ctxt, OutputStream out) {
            CloseTrackingOutputStream wrapped = new CloseTrackingOutputStream(out);
            outputs.add(wrapped);
            return wrapped;
        }

        @Override
        public Writer decorate(IOContext ctxt, Writer w) {
            CloseTrackingWriter wrapped = new CloseTrackingWriter(w);
            writers.add(wrapped);
            return wrapped;
        }
    }

    /**
     * Factory that fails the way format backends can (say, when schema
     * validation fails), that is, after source/target has been opened.
     */
    static class FailingFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public final List<CloseTrackingOutputStream> outputs = new ArrayList<>();

        public FailingFactory() { }

        public FailingFactory(JsonFactoryBuilder b) {
            super(b);
        }

        @Override
        protected OutputStream _fileOutputStream(File f) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._fileOutputStream(f));
            outputs.add(out);
            return out;
        }

        @Override
        protected OutputStream _pathOutputStream(Path p) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._pathOutputStream(p));
            outputs.add(out);
            return out;
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

    /**
     * Factory that fails after the target has been opened but before the normal
     * generator construction failure guard is entered.
     */
    static class ContextFailingFactory extends FailingFactory {
        private static final long serialVersionUID = 1L;

        @Override
        protected IOContext _createContext(ContentReference contentRef,
                boolean resourceManaged, JsonEncoding enc) {
            throw new IllegalStateException("Test-induced context failure");
        }
    }

    static class TrackingFactory extends JsonFactory {
        private static final long serialVersionUID = 1L;

        public final List<CloseTrackingOutputStream> outputs = new ArrayList<>();

        public TrackingFactory(JsonFactoryBuilder b) {
            super(b);
        }

        @Override
        protected OutputStream _fileOutputStream(File f) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._fileOutputStream(f));
            outputs.add(out);
            return out;
        }

        @Override
        protected OutputStream _pathOutputStream(Path p) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._pathOutputStream(p));
            outputs.add(out);
            return out;
        }
    }

    static class FailingGeneratorDecorator implements JsonGeneratorDecorator {
        @Override
        public JsonGenerator decorate(TokenStreamFactory factory, JsonGenerator generator) {
            throw new IllegalStateException("Test-induced generator decorator failure");
        }
    }

    static class FailingBinaryFactory extends BinaryTSFactory {
        private static final long serialVersionUID = 1L;

        public final List<CloseTrackingOutputStream> outputs = new ArrayList<>();

        private final boolean _failContextCreation;

        FailingBinaryFactory(FailingBinaryFactoryBuilder b, boolean failContextCreation) {
            super(b);
            _failContextCreation = failContextCreation;
        }

        @Override
        protected OutputStream _fileOutputStream(File f) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._fileOutputStream(f));
            outputs.add(out);
            return out;
        }

        @Override
        protected OutputStream _pathOutputStream(Path p) throws JacksonException {
            CloseTrackingOutputStream out = new CloseTrackingOutputStream(super._pathOutputStream(p));
            outputs.add(out);
            return out;
        }

        @Override
        protected IOContext _createContext(ContentReference contentRef,
                boolean resourceManaged, JsonEncoding enc) {
            if (_failContextCreation) {
                throw new IllegalStateException("Test-induced context failure");
            }
            return super._createContext(contentRef, resourceManaged, enc);
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ioCtxt, InputStream in) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ioCtxt, byte[] data, int offset, int len) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ioCtxt, DataInput input) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, OutputStream out) {
            throw new IllegalStateException("Test-induced construction failure");
        }

        @Override
        public TokenStreamFactory copy() { return this; }
        @Override
        public TokenStreamFactory snapshot() { return this; }
        @Override
        public TSFBuilder<?,?> rebuild() { return null; }

        @Override
        public boolean canParseAsync() { return false; }
        @Override
        public boolean canUseSchema(FormatSchema schema) { return false; }

        @Override
        public String getFormatName() { return "test-binary"; }

        @Override
        public Version version() { return Version.unknownVersion(); }
    }

    static class FailingBinaryFactoryBuilder
        extends DecorableTSFactory.DecorableTSFBuilder<FailingBinaryFactory, FailingBinaryFactoryBuilder>
    {
        FailingBinaryFactoryBuilder() {
            super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                    ErrorReportConfiguration.defaults(), 0, 0);
        }

        @Override
        public FailingBinaryFactory build() {
            return build(false);
        }

        public FailingBinaryFactory build(boolean failContextCreation) {
            return new FailingBinaryFactory(this, failContextCreation);
        }
    }

    private File _tempFile() throws IOException {
        File f = File.createTempFile("jackson-core-test", ".json");
        f.deleteOnExit();
        Files.write(f.toPath(), utf8Bytes("{\"a\":1}"));
        return f;
    }

    @Test
    void closesFileOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        for (JsonEncoding enc : new JsonEncoding[] { JsonEncoding.UTF8, JsonEncoding.UTF16_BE }) {
            FailingFactory f = new FailingFactory();
            File dst = _tempFile();
            assertThrows(IllegalStateException.class,
                    () -> f.createGenerator(ObjectWriteContext.empty(), dst, enc));
            assertEquals(1, f.outputs.size(), enc.toString());
            assertTrue(f.outputs.get(0).closed,
                    "OutputStream Jackson opened should have been closed, encoding "+enc);
        }
    }

    @Test
    void closesFileOutputStreamOnContextCreationFailure() throws Exception
    {
        ContextFailingFactory f = new ContextFailingFactory();
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoLeakedOutput(f.outputs);
    }

    @Test
    void closesPathOutputStreamOnContextCreationFailure() throws Exception
    {
        ContextFailingFactory f = new ContextFailingFactory();
        Path dst = _tempFile().toPath();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoLeakedOutput(f.outputs);
    }

    @Test
    void closesDecoratedFileOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        TrackingOutputDecorator dec = new TrackingOutputDecorator();
        FailingFactory f = new FailingFactory(JsonFactory.builder()
                .outputDecorator(dec));
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        assertEquals(1, dec.outputs.size());
        assertTrue(dec.outputs.get(0).closed,
                "Decorated OutputStream should have been closed");
    }

    @Test
    void closesDecoratedFileWriterOnFailedGeneratorConstruction() throws Exception
    {
        TrackingOutputDecorator dec = new TrackingOutputDecorator();
        FailingFactory f = new FailingFactory(JsonFactory.builder()
                .outputDecorator(dec));
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF16_BE));
        assertEquals(1, dec.writers.size());
        assertTrue(dec.writers.get(0).closed,
                "Decorated Writer should have been closed");
    }

    @Test
    void closesFileOutputStreamOnGeneratorDecorationFailureWithAutoCloseTargetDisabled() throws Exception
    {
        TrackingFactory f = new TrackingFactory(JsonFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, false)
                .addDecorator(new FailingGeneratorDecorator()));
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoLeakedOutput(f.outputs);
    }

    @Test
    void closesPathOutputStreamOnGeneratorDecorationFailureWithAutoCloseTargetDisabled() throws Exception
    {
        TrackingFactory f = new TrackingFactory(JsonFactory.builder()
                .configure(StreamWriteFeature.AUTO_CLOSE_TARGET, false)
                .addDecorator(new FailingGeneratorDecorator()));
        Path dst = _tempFile().toPath();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF16_BE));
        _verifyNoLeakedOutput(f.outputs);
    }

    @Test
    void binaryClosesFileOutputStreamOnContextCreationFailure() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder().build(true);
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoLeakedOutput(f.outputs);
    }

    @Test
    void binaryClosesPathOutputStreamOnContextCreationFailure() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder().build(true);
        Path dst = _tempFile().toPath();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoLeakedOutput(f.outputs);
    }

    @Test
    void binaryClosesDecoratedFileOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        TrackingOutputDecorator dec = new TrackingOutputDecorator();
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder()
                .outputDecorator(dec)
                .build();
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        assertEquals(1, dec.outputs.size());
        assertTrue(dec.outputs.get(0).closed,
                "Decorated OutputStream should have been closed");
    }

    private void _verifyNoLeakedOutput(List<CloseTrackingOutputStream> outputs) {
        assertTrue(outputs.size() <= 1, "Should have opened at most one output");
        if (!outputs.isEmpty()) {
            assertTrue(outputs.get(0).closed,
                    "OutputStream Jackson opened should have been closed");
        }
    }
}
