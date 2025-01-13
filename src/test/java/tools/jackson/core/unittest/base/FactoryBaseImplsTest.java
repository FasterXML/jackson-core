package tools.jackson.core.unittest.base;

import java.io.*;

import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.FormatSchema;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.TSFBuilder;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.core.Version;
import tools.jackson.core.base.BinaryTSFactory;
import tools.jackson.core.base.TextualTSFactory;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// Bit different "test" class, used to check that intermediate base types
// (textual, binary format bases) are complete enough. This is not done
// via test methods but just by having minimal definitions present.
//
// In future might add some actual tests too
@SuppressWarnings("serial")
class FactoryBaseImplsTest extends JacksonCoreTestBase
{
    static class ToyBinaryFormatFactory
        extends BinaryTSFactory
    {
        public ToyBinaryFormatFactory() {
            super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                ErrorReportConfiguration.defaults(), 0, 0);
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ioCtxt, InputStream in) throws JacksonException {
            return null;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ioCtxt, byte[] data, int offset, int len) throws JacksonException {
            return null;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ioCtxt, DataInput input) throws JacksonException {
            return null;
        }

        @Override
        protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, OutputStream out) throws JacksonException {
            return null;
        }

        @Override
        public TokenStreamFactory copy() { return this; }
        @Override
        public TokenStreamFactory snapshot() { return this; }
        @Override
        public TSFBuilder<?, ?> rebuild() { return null; }

        @Override
        public boolean canParseAsync() { return false; }
        @Override
        public boolean canUseSchema(FormatSchema schema) { return false; }

        @Override
        public String getFormatName() { return null; }

        @Override
        public Version version() { return Version.unknownVersion(); }
    }

    static class ToyTextualFormatFactory
        extends TextualTSFactory
    {
        public ToyTextualFormatFactory() {
            super(StreamReadConstraints.defaults(), StreamWriteConstraints.defaults(),
                    ErrorReportConfiguration.defaults(), 0, 0);
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ctxt, InputStream in) throws JacksonException {
            return null;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ctxt, Reader r) throws JacksonException {
            return null;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ctxt,
                byte[] data, int offset, int len) throws JacksonException {
            return null;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ctxt, char[] data, int offset, int len, boolean recyclable)
                        throws JacksonException {
            return null;
        }

        @Override
        protected JsonParser _createParser(ObjectReadContext readCtxt,
                IOContext ctxt, DataInput input) throws JacksonException {
            return null;
        }

        @Override
        protected JsonGenerator _createGenerator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, Writer out) throws JacksonException {
            return null;
        }

        @Override
        protected JsonGenerator _createUTF8Generator(ObjectWriteContext writeCtxt,
                IOContext ioCtxt, OutputStream out) throws JacksonException {
            return null;
        }

        @Override
        public TokenStreamFactory copy() { return this; }
        @Override
        public TokenStreamFactory snapshot() { return this; }
        @Override
        public TSFBuilder<?, ?> rebuild() { return null; }

        @Override
        public boolean canParseAsync() { return false; }
        @Override
        public boolean canUseSchema(FormatSchema schema) { return false; }

        @Override
        public String getFormatName() { return null; }

        @Override
        public Version version() { return Version.unknownVersion(); }
}

    public void testBogus() {
        // no real tests but need one "test" method to avoid junit fail

        assertNotNull(new ToyBinaryFormatFactory());
        assertNotNull(new ToyTextualFormatFactory());
    }
}
