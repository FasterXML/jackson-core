package tools.jackson.core.unittest.base;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.base.BinaryTSFactory;
import tools.jackson.core.base.DecorableTSFactory;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.io.InputDecorator;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonFactoryBuilder;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that streams Jackson itself opens -- for {@link File} /
 * {@link Path} sources, or via {@link InputDecorator} -- get closed, exactly
 * once, if construction of parser fails after opening. Failure may occur in
 * decorator (before format backend is entered at all), or in backend's own
 * {@code _createParser()}.
 */
@SuppressWarnings("serial")
class FailedParserConstructionCloseTest extends JacksonCoreTestBase
{
    private final static String DECORATOR_FAIL = "Test-induced decorator failure";

    private final static String CREATE_FAIL = "Test-induced construction failure";

    private final static String READ_FAIL = "Will not read, ever!";

    /**
     * Where construction is to fail, for factories that open source themselves.
     */
    enum Failure {
        /** Fail before backend sees source at all (decorator failure) */
        IN_DECORATOR,
        /** Fail in backend's {@code _createParser()}, the way schema validation can */
        IN_CREATE,
        /** Fail deeper, on first actual read: exercises real backend code path */
        ON_READ
    }

    static class CloseTrackingInputStream extends FilterInputStream {
        public int closeCount;

        CloseTrackingInputStream(InputStream in) { super(in); }

        @Override
        public void close() throws IOException {
            ++closeCount;
            super.close();
        }
    }

    static class UnreadableInputStream extends InputStream {
        @Override
        public int read() throws IOException { throw new IOException(READ_FAIL); }
    }

    /**
     * Implemented by test factories that track streams Jackson opened for
     * {@link File} / {@link Path} sources.
     */
    interface SourceTracking {
        List<CloseTrackingInputStream> openedSources();
    }

    /**
     * Decorator that fails the way a user-provided one can, that is, after
     * source has been opened but before backend gets to see it.
     */
    static class FailingInputDecorator extends InputDecorator {
        @Override
        public InputStream decorate(IOContext ctxt, InputStream in) {
            throw new IllegalStateException(DECORATOR_FAIL);
        }

        @Override
        public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length) {
            throw new IllegalStateException(DECORATOR_FAIL);
        }

        @Override
        public Reader decorate(IOContext ctxt, Reader r) {
            throw new IllegalStateException(DECORATOR_FAIL);
        }
    }

    /**
     * Decorator that creates {@link InputStream} for {@code byte[]} source: stream
     * is created by decorator, not caller, so factory must close it on failure.
     */
    static class ByteArraySourceDecorator extends InputDecorator {
        public final List<CloseTrackingInputStream> sources = new ArrayList<>();

        @Override
        public InputStream decorate(IOContext ctxt, InputStream in) { return in; }

        @Override
        public InputStream decorate(IOContext ctxt, byte[] src, int offset, int length) {
            CloseTrackingInputStream wrapped = new CloseTrackingInputStream(
                    new ByteArrayInputStream(src, offset, length));
            sources.add(wrapped);
            return wrapped;
        }

        @Override
        public Reader decorate(IOContext ctxt, Reader r) { return r; }
    }

    // // // Textual (JSON) factory

    static class TrackingJsonFactory extends JsonFactory
        implements SourceTracking
    {
        public final List<CloseTrackingInputStream> sources = new ArrayList<>();

        private final Failure _failure;

        TrackingJsonFactory(JsonFactoryBuilder b, Failure failure) {
            super(b);
            _failure = failure;
        }

        @Override
        public List<CloseTrackingInputStream> openedSources() { return sources; }

        @Override
        protected InputStream _fileInputStream(File f) {
            return _track(super._fileInputStream(f));
        }

        @Override
        protected InputStream _pathInputStream(Path p) {
            return _track(super._pathInputStream(p));
        }

        private InputStream _track(InputStream in) {
            if (_failure == Failure.ON_READ) {
                in = new UnreadableInputStream();
            }
            CloseTrackingInputStream wrapped = new CloseTrackingInputStream(in);
            sources.add(wrapped);
            return wrapped;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                InputStream in) {
            if (_failure == Failure.IN_CREATE) {
                throw new IllegalStateException(CREATE_FAIL);
            }
            return super._createParser(readCtxt, ioCtxt, in);
        }
    }

    // // // Binary factory: minimal impl, fails the way format backends can
    // // // (say, when schema validation fails)

    static class ToyBinaryFactory extends BinaryTSFactory
        implements SourceTracking
    {
        public final List<CloseTrackingInputStream> sources = new ArrayList<>();

        ToyBinaryFactory(ToyBinaryFactoryBuilder b) { super(b); }

        @Override
        public List<CloseTrackingInputStream> openedSources() { return sources; }

        @Override
        protected InputStream _fileInputStream(File f) {
            return _track(super._fileInputStream(f));
        }

        @Override
        protected InputStream _pathInputStream(Path p) {
            return _track(super._pathInputStream(p));
        }

        private InputStream _track(InputStream in) {
            CloseTrackingInputStream wrapped = new CloseTrackingInputStream(in);
            sources.add(wrapped);
            return wrapped;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                InputStream in) {
            throw new IllegalStateException(CREATE_FAIL);
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                byte[] data, int offset, int len) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                DataInput input) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, OutputStream out) {
            throw new UnsupportedOperationException();
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
        public String getFormatName() { return "toy-binary"; }

        @Override
        public Version version() { return Version.unknownVersion(); }
    }

    static class ToyBinaryFactoryBuilder
        extends DecorableTSFactory.DecorableTSFBuilder<ToyBinaryFactory, ToyBinaryFactoryBuilder>
    {
        ToyBinaryFactoryBuilder() {
            super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                    ErrorReportConfiguration.defaults(), 0, 0);
        }

        @Override
        public ToyBinaryFactory build() { return new ToyBinaryFactory(this); }
    }

    /*
    /**********************************************************************
    /* Test methods: textual (JSON) factory
    /**********************************************************************
     */

    @Test
    void jsonFileSourceClosedOnDecoratorFailure() throws Exception {
        _verifyFileSourceClosed(_jsonFactory(Failure.IN_DECORATOR),
                IllegalStateException.class, DECORATOR_FAIL);
    }

    @Test
    void jsonPathSourceClosedOnDecoratorFailure() throws Exception {
        _verifyPathSourceClosed(_jsonFactory(Failure.IN_DECORATOR),
                IllegalStateException.class, DECORATOR_FAIL);
    }

    @Test
    void jsonFileSourceClosedOnCreateFailure() throws Exception {
        _verifyFileSourceClosed(_jsonFactory(Failure.IN_CREATE),
                IllegalStateException.class, CREATE_FAIL);
    }

    @Test
    void jsonPathSourceClosedOnCreateFailure() throws Exception {
        _verifyPathSourceClosed(_jsonFactory(Failure.IN_CREATE),
                IllegalStateException.class, CREATE_FAIL);
    }

    // [core#763]: failure inside real backend, after hand-off
    @Test
    void jsonFileSourceClosedOnReadFailure() throws Exception {
        _verifyFileSourceClosed(_jsonFactory(Failure.ON_READ),
                JacksonException.class, READ_FAIL);
    }

    @Test
    void jsonPathSourceClosedOnReadFailure() throws Exception {
        _verifyPathSourceClosed(_jsonFactory(Failure.ON_READ),
                JacksonException.class, READ_FAIL);
    }

    // [core#763]: stream decorator creates from `byte[]` source is ours to close too
    @Test
    void jsonByteArraySourceClosedOnCreateFailure() throws Exception {
        ByteArraySourceDecorator dec = new ByteArraySourceDecorator();
        TrackingJsonFactory f = new TrackingJsonFactory(
                JsonFactory.builder().inputDecorator(dec), Failure.IN_CREATE);
        _verifyByteArraySourceClosed(f, dec);
    }

    /*
    /**********************************************************************
    /* Test methods: binary factory
    /**********************************************************************
     */

    @Test
    void binaryFileSourceClosedOnDecoratorFailure() throws Exception {
        _verifyFileSourceClosed(_binaryFactory(true),
                IllegalStateException.class, DECORATOR_FAIL);
    }

    @Test
    void binaryPathSourceClosedOnDecoratorFailure() throws Exception {
        _verifyPathSourceClosed(_binaryFactory(true),
                IllegalStateException.class, DECORATOR_FAIL);
    }

    @Test
    void binaryFileSourceClosedOnCreateFailure() throws Exception {
        _verifyFileSourceClosed(_binaryFactory(false),
                IllegalStateException.class, CREATE_FAIL);
    }

    @Test
    void binaryPathSourceClosedOnCreateFailure() throws Exception {
        _verifyPathSourceClosed(_binaryFactory(false),
                IllegalStateException.class, CREATE_FAIL);
    }

    // [core#763]: stream decorator creates from `byte[]` source is ours to close too
    @Test
    void binaryByteArraySourceClosedOnCreateFailure() throws Exception {
        ByteArraySourceDecorator dec = new ByteArraySourceDecorator();
        ToyBinaryFactory f = new ToyBinaryFactoryBuilder().inputDecorator(dec).build();
        _verifyByteArraySourceClosed(f, dec);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private TrackingJsonFactory _jsonFactory(Failure failure) {
        JsonFactoryBuilder b = JsonFactory.builder();
        if (failure == Failure.IN_DECORATOR) {
            b = b.inputDecorator(new FailingInputDecorator());
        }
        return new TrackingJsonFactory(b, failure);
    }

    private ToyBinaryFactory _binaryFactory(boolean failingDecorator) {
        ToyBinaryFactoryBuilder b = new ToyBinaryFactoryBuilder();
        if (failingDecorator) {
            b = b.inputDecorator(new FailingInputDecorator());
        }
        return b.build();
    }

    private <F extends TokenStreamFactory & SourceTracking> void _verifyFileSourceClosed(F f,
            Class<? extends Exception> expType, String expFailMsg)
        throws Exception
    {
        final File src = _tempFile();
        Exception e = assertThrows(expType,
                () -> f.createParser(ObjectReadContext.empty(), src));
        verifyException(e, expFailMsg);
        _verifyClosed(f.openedSources());
    }

    private <F extends TokenStreamFactory & SourceTracking> void _verifyPathSourceClosed(F f,
            Class<? extends Exception> expType, String expFailMsg)
        throws Exception
    {
        final Path src = _tempFile().toPath();
        Exception e = assertThrows(expType,
                () -> f.createParser(ObjectReadContext.empty(), src));
        verifyException(e, expFailMsg);
        _verifyClosed(f.openedSources());
    }

    private void _verifyByteArraySourceClosed(TokenStreamFactory f,
            ByteArraySourceDecorator dec)
    {
        final byte[] src = utf8Bytes("{\"a\":1}");
        Exception e = assertThrows(IllegalStateException.class,
                () -> f.createParser(ObjectReadContext.empty(), src));
        verifyException(e, CREATE_FAIL);
        _verifyClosed(dec.sources);
    }

    private void _verifyClosed(List<CloseTrackingInputStream> sources) {
        assertEquals(1, sources.size(), "Should have opened exactly one source");
        // Exactly once: not closing leaks, closing twice may fail for non-idempotent
        // streams (and add bogus suppressed exceptions)
        assertEquals(1, sources.get(0).closeCount,
                "InputStream Jackson opened should have been closed exactly once");
    }

    private File _tempFile() throws IOException {
        File f = File.createTempFile("jackson-core-test", ".json");
        f.deleteOnExit();
        Files.write(f.toPath(), utf8Bytes("{\"a\":1}"));
        return f;
    }
}
