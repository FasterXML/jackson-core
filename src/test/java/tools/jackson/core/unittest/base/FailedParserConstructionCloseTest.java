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
 * Tests to verify that streams Jackson itself opens for {@link File} /
 * {@link Path} sources get closed if construction of parser fails after
 * opening -- either in {@link InputDecorator} (before format backend is
 * entered at all), or in backend's own {@code _createParser()}.
 */
@SuppressWarnings("serial")
class FailedParserConstructionCloseTest extends JacksonCoreTestBase
{
    private final static String DECORATOR_FAIL = "Test-induced decorator failure";

    private final static String CREATE_FAIL = "Test-induced construction failure";

    static class CloseTrackingInputStream extends FilterInputStream {
        public boolean closed;

        CloseTrackingInputStream(InputStream in) { super(in); }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
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

    // // // Textual (JSON) factory

    static class TrackingJsonFactory extends JsonFactory
        implements SourceTracking
    {
        public final List<CloseTrackingInputStream> sources = new ArrayList<>();

        private final boolean _failInCreate;

        TrackingJsonFactory(JsonFactoryBuilder b, boolean failInCreate) {
            super(b);
            _failInCreate = failInCreate;
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
            CloseTrackingInputStream wrapped = new CloseTrackingInputStream(in);
            sources.add(wrapped);
            return wrapped;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt, IOContext ioCtxt,
                InputStream in) {
            if (_failInCreate) {
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
        _verifyFileSourceClosed(_jsonFactory(true, false), DECORATOR_FAIL);
    }

    @Test
    void jsonPathSourceClosedOnDecoratorFailure() throws Exception {
        _verifyPathSourceClosed(_jsonFactory(true, false), DECORATOR_FAIL);
    }

    @Test
    void jsonFileSourceClosedOnCreateFailure() throws Exception {
        _verifyFileSourceClosed(_jsonFactory(false, true), CREATE_FAIL);
    }

    @Test
    void jsonPathSourceClosedOnCreateFailure() throws Exception {
        _verifyPathSourceClosed(_jsonFactory(false, true), CREATE_FAIL);
    }

    /*
    /**********************************************************************
    /* Test methods: binary factory
    /**********************************************************************
     */

    @Test
    void binaryFileSourceClosedOnDecoratorFailure() throws Exception {
        _verifyFileSourceClosed(_binaryFactory(true), DECORATOR_FAIL);
    }

    @Test
    void binaryPathSourceClosedOnDecoratorFailure() throws Exception {
        _verifyPathSourceClosed(_binaryFactory(true), DECORATOR_FAIL);
    }

    @Test
    void binaryFileSourceClosedOnCreateFailure() throws Exception {
        _verifyFileSourceClosed(_binaryFactory(false), CREATE_FAIL);
    }

    @Test
    void binaryPathSourceClosedOnCreateFailure() throws Exception {
        _verifyPathSourceClosed(_binaryFactory(false), CREATE_FAIL);
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private TrackingJsonFactory _jsonFactory(boolean failingDecorator, boolean failInCreate) {
        JsonFactoryBuilder b = JsonFactory.builder();
        if (failingDecorator) {
            b = b.inputDecorator(new FailingInputDecorator());
        }
        return new TrackingJsonFactory(b, failInCreate);
    }

    private ToyBinaryFactory _binaryFactory(boolean failingDecorator) {
        ToyBinaryFactoryBuilder b = new ToyBinaryFactoryBuilder();
        if (failingDecorator) {
            b = b.inputDecorator(new FailingInputDecorator());
        }
        return b.build();
    }

    private <F extends TokenStreamFactory & SourceTracking> void _verifyFileSourceClosed(F f,
            String expFailMsg)
        throws Exception
    {
        final File src = _tempFile();
        Exception e = assertThrows(IllegalStateException.class,
                () -> f.createParser(ObjectReadContext.empty(), src));
        verifyException(e, expFailMsg);
        _verifyClosed(f);
    }

    private <F extends TokenStreamFactory & SourceTracking> void _verifyPathSourceClosed(F f,
            String expFailMsg)
        throws Exception
    {
        final Path src = _tempFile().toPath();
        Exception e = assertThrows(IllegalStateException.class,
                () -> f.createParser(ObjectReadContext.empty(), src));
        verifyException(e, expFailMsg);
        _verifyClosed(f);
    }

    private void _verifyClosed(SourceTracking f) {
        List<CloseTrackingInputStream> sources = f.openedSources();
        assertEquals(1, sources.size(), "Should have opened exactly one source");
        assertTrue(sources.get(0).closed, "InputStream Jackson opened should have been closed");
    }

    private File _tempFile() throws IOException {
        File f = File.createTempFile("jackson-core-test", ".json");
        f.deleteOnExit();
        Files.write(f.toPath(), utf8Bytes("{\"a\":1}"));
        return f;
    }
}
