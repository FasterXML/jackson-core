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
import tools.jackson.core.util.JsonGeneratorDelegate;

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
        public int closeCount;

        CloseTrackingOutputStream(OutputStream out) { super(out); }

        @Override
        public void close() throws IOException {
            closed = true;
            ++closeCount;
            super.close();
        }
    }

    static class CloseTrackingWriter extends FilterWriter {
        public boolean closed;
        public int closeCount;

        CloseTrackingWriter(Writer out) { super(out); }

        @Override
        public void close() throws IOException {
            closed = true;
            ++closeCount;
            super.close();
        }
    }

    static class FailingCloseOutputStream extends FilterOutputStream {
        public boolean closeAttempted;

        FailingCloseOutputStream(OutputStream out) { super(out); }

        @Override
        public void close() throws IOException {
            closeAttempted = true;
            throw new IOException("Test-induced output close failure");
        }
    }

    static class FailingCloseWriter extends FilterWriter {
        public boolean closeAttempted;

        FailingCloseWriter(Writer out) { super(out); }

        @Override
        public void close() throws IOException {
            closeAttempted = true;
            throw new IOException("Test-induced writer close failure");
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

    static class FailingCloseOutputDecorator extends OutputDecorator {
        private static final long serialVersionUID = 1L;

        public final List<FailingCloseOutputStream> outputs = new ArrayList<>();
        public final List<FailingCloseWriter> writers = new ArrayList<>();

        @Override
        public OutputStream decorate(IOContext ctxt, OutputStream out) {
            FailingCloseOutputStream wrapped = new FailingCloseOutputStream(out);
            outputs.add(wrapped);
            return wrapped;
        }

        @Override
        public Writer decorate(IOContext ctxt, Writer w) {
            FailingCloseWriter wrapped = new FailingCloseWriter(w);
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
     * Factory that fails while creating the context, before generator
     * construction itself starts.
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
        private final boolean _writeHeaderGenerator;

        FailingBinaryFactory(FailingBinaryFactoryBuilder b,
                boolean failContextCreation, boolean writeHeaderGenerator) {
            super(b);
            _failContextCreation = failContextCreation;
            _writeHeaderGenerator = writeHeaderGenerator;
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
                IOContext ioCtxt, OutputStream out) throws JacksonException {
            if (_writeHeaderGenerator) {
                return new HeaderCloseSideEffectGenerator(out);
            }
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
            return new FailingBinaryFactory(this, failContextCreation, false);
        }

        public FailingBinaryFactory buildWithHeaderGenerator() {
            return new FailingBinaryFactory(this, false, true);
        }
    }

    static class HeaderCloseSideEffectGenerator extends JsonGeneratorDelegate {
        private final OutputStream _out;

        HeaderCloseSideEffectGenerator(OutputStream out) throws JacksonException {
            super(new JsonFactory().createGenerator(ObjectWriteContext.empty(),
                    new ByteArrayOutputStream()));
            _out = out;
            _writeByte('H');
        }

        @Override
        public void close() {
            _writeByte('C');
            super.close();
        }

        private void _writeByte(int b) {
            try {
                _out.write(b);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
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
            _verifyOneClosedOutput(f.outputs, enc.toString());
        }
    }

    @Test
    void closesPathOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        for (JsonEncoding enc : new JsonEncoding[] { JsonEncoding.UTF8, JsonEncoding.UTF16_BE }) {
            FailingFactory f = new FailingFactory();
            Path dst = _tempFile().toPath();
            assertThrows(IllegalStateException.class,
                    () -> f.createGenerator(ObjectWriteContext.empty(), dst, enc));
            _verifyOneClosedOutput(f.outputs, enc.toString());
        }
    }

    @Test
    void doesNotOpenFileOutputStreamOnContextCreationFailure() throws Exception
    {
        ContextFailingFactory f = new ContextFailingFactory();
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoOutputOpened(f.outputs);
    }

    @Test
    void doesNotOpenPathOutputStreamOnContextCreationFailure() throws Exception
    {
        ContextFailingFactory f = new ContextFailingFactory();
        Path dst = _tempFile().toPath();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoOutputOpened(f.outputs);
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
        _verifyOneClosedOutput(f.outputs);
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
        _verifyOneClosedOutput(f.outputs);
        assertEquals(1, dec.writers.size());
        assertTrue(dec.writers.get(0).closed,
                "Decorated Writer should have been closed");
    }

    @Test
    void closesRawFileOutputStreamWhenDecoratedOutputCloseFails() throws Exception
    {
        FailingCloseOutputDecorator dec = new FailingCloseOutputDecorator();
        FailingFactory f = new FailingFactory(JsonFactory.builder()
                .outputDecorator(dec));
        File dst = _tempFile();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyOneClosedOutput(f.outputs);
        assertEquals(1, dec.outputs.size());
        assertTrue(dec.outputs.get(0).closeAttempted,
                "Decorated OutputStream close should have been attempted");
        _verifySuppressed(e, "Test-induced output close failure");
    }

    @Test
    void closesRawFileOutputStreamWhenDecoratedWriterCloseFails() throws Exception
    {
        FailingCloseOutputDecorator dec = new FailingCloseOutputDecorator();
        FailingFactory f = new FailingFactory(JsonFactory.builder()
                .outputDecorator(dec));
        File dst = _tempFile();

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF16_BE));
        _verifyOneClosedOutput(f.outputs);
        assertEquals(1, dec.writers.size());
        assertTrue(dec.writers.get(0).closeAttempted,
                "Decorated Writer close should have been attempted");
        _verifySuppressed(e, "Test-induced writer close failure");
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
        _verifyOneClosedOutput(f.outputs);
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
        _verifyOneClosedOutput(f.outputs);
    }

    @Test
    void binaryClosesFileOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder().build();
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyOneClosedOutput(f.outputs);
    }

    @Test
    void binaryClosesPathOutputStreamOnFailedGeneratorConstruction() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder().build();
        Path dst = _tempFile().toPath();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyOneClosedOutput(f.outputs);
    }

    @Test
    void binaryClosesFileOutputStreamOnContextCreationFailure() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder().build(true);
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyOneClosedOutput(f.outputs);
    }

    @Test
    void binaryDoesNotOpenPathOutputStreamOnContextCreationFailure() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder().build(true);
        Path dst = _tempFile().toPath();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyNoOutputOpened(f.outputs);
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
        _verifyOneClosedOutput(f.outputs);
        assertEquals(1, dec.outputs.size());
        assertTrue(dec.outputs.get(0).closed,
                "Decorated OutputStream should have been closed");
    }

    @Test
    void binaryGeneratorDecoratorFailureDoesNotClosePartiallyConstructedGenerator() throws Exception
    {
        FailingBinaryFactory f = new FailingBinaryFactoryBuilder()
                .addDecorator(new FailingGeneratorDecorator())
                .buildWithHeaderGenerator();
        File dst = _tempFile();

        assertThrows(IllegalStateException.class,
                () -> f.createGenerator(ObjectWriteContext.empty(), dst, JsonEncoding.UTF8));
        _verifyOneClosedOutput(f.outputs);
        assertArrayEquals(new byte[] { 'H' }, Files.readAllBytes(dst.toPath()));
    }

    private void _verifyOneClosedOutput(List<CloseTrackingOutputStream> outputs) {
        _verifyOneClosedOutput(outputs, "Should have opened exactly one output");
    }

    private void _verifyOneClosedOutput(List<CloseTrackingOutputStream> outputs, String msg) {
        assertEquals(1, outputs.size(), msg);
        assertTrue(outputs.get(0).closed,
                "OutputStream Jackson opened should have been closed");
        // 16-Sep-2026, tatu: [core#1711] Closing decorated resource closes what it wraps,
        //   so target Jackson opened must not be closed a second time
        assertEquals(1, outputs.get(0).closeCount,
                "OutputStream Jackson opened should have been closed exactly once");
    }

    private void _verifyNoOutputOpened(List<CloseTrackingOutputStream> outputs) {
        assertEquals(0, outputs.size(), "Should not have opened output");
    }

    private void _verifySuppressed(Throwable failure, String msg) {
        for (Throwable suppressed : failure.getSuppressed()) {
            if (suppressed.getMessage().contains(msg)) {
                return;
            }
        }
        fail("Expected suppressed exception containing: "+msg);
    }
}
