package tools.jackson.core.base;

import java.io.DataInput;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import tools.jackson.core.*;
import tools.jackson.core.io.IOContext;

// Bit different "test" class, used to check that intermediate base types
// (textual, binary format bases) are complete enough. This is not done
// via test methods but just by having minimal definitions present.
//
// In future might add some actual tests too
@SuppressWarnings("serial")
public class FactoryBaseImplsTest extends BaseTest
{
    static class ToyBinaryFormatFactory
        extends BinaryTSFactory
    {
        public ToyBinaryFormatFactory() { super(null, 0, 0); }

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
        public ToyTextualFormatFactory() { super(StreamReadConstraints.defaults(), 0, 0); }

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
