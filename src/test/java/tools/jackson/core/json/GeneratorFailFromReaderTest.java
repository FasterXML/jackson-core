package tools.jackson.core.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.exc.StreamWriteException;

import static org.junit.jupiter.api.Assertions.fail;

class GeneratorFailFromReaderTest
    extends tools.jackson.core.JUnit5TestBase
{
    private final JsonFactory F = newStreamFactory();

    // [core#177]
    // Also: should not try writing JSON String if field name expected
    // (in future maybe take one as alias... but not yet)
    @Test
    void failOnWritingStringNotFieldNameBytes() throws Exception {
        _testFailOnWritingStringNotFieldName(F, false);
    }

    // [core#177]
    @Test
    void failOnWritingStringNotFieldNameChars() throws Exception {
        _testFailOnWritingStringNotFieldName(F, true);
    }

    @Test
    void failOnWritingStringFromReaderWithTooFewCharacters() throws Exception {
        _testFailOnWritingStringFromReaderWithTooFewCharacters(F, true);
        _testFailOnWritingStringFromReaderWithTooFewCharacters(F, false);
    }

    @Test
    void failOnWritingStringFromNullReader() throws Exception {
        _testFailOnWritingStringFromNullReader(F, true);
        _testFailOnWritingStringFromNullReader(F, false);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */


    private void _testFailOnWritingStringNotFieldName(JsonFactory f, boolean useReader)
        throws IOException
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(ObjectWriteContext.empty(), bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            StringReader reader = new StringReader("a");
            gen.writeString(reader, -1);
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() be used in place of 'writeName()': output = "+json);
        } catch (StreamWriteException e) {
            verifyException(e, "cannot write a String");
        }
        gen.close();
    }

    private void _testFailOnWritingStringFromReaderWithTooFewCharacters(JsonFactory f, boolean useReader)
        throws IOException
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(ObjectWriteContext.empty(), bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            String testStr = "aaaaaaaaa";
            StringReader reader = new StringReader(testStr);
            gen.writeName("a");
            gen.writeString(reader, testStr.length() + 1);
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() ': output = "+json);
        } catch (StreamWriteException e) {
            verifyException(e, "Didn't read enough from reader");
        }
        gen.close();
    }

    private void _testFailOnWritingStringFromNullReader(JsonFactory f, boolean useReader)
        throws IOException
    {
        JsonGenerator gen;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        if (useReader) {
            gen = f.createGenerator(ObjectWriteContext.empty(), new OutputStreamWriter(bout, "UTF-8"));
        } else {
            gen = f.createGenerator(ObjectWriteContext.empty(), bout, JsonEncoding.UTF8);
        }
        gen.writeStartObject();

        try {
            gen.writeName("a");
            gen.writeString(null, -1);
            gen.flush();
            String json = bout.toString("UTF-8");
            fail("Should not have let "+gen.getClass().getName()+".writeString() ': output = "+json);
        } catch (StreamWriteException e) {
            verifyException(e, "null reader");
        }
        gen.close();
    }
}
