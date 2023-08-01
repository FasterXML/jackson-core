package tools.jackson.core.write;

import tools.jackson.core.BaseTest;
import tools.jackson.core.ErrorReportConfiguration;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.io.IOContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.WriterBasedJsonGenerator;
import tools.jackson.core.util.BufferRecycler;

import java.io.StringWriter;

public class WriterBasedJsonGeneratorTest extends BaseTest
{
    public void testNestingDepthWithSmallLimit() throws Exception
    {
        StringWriter sw = new StringWriter();
        IOContext ioc = new IOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.builder().maxNestingDepth(1).build(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference(sw), true,
                JsonEncoding.UTF8);
        try (JsonGenerator gen = new WriterBasedJsonGenerator(ObjectWriteContext.empty(), ioc, 0, 0, sw,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"')) {
            gen.writeStartObject();
            gen.writeName("array");
            gen.writeStartArray();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException sce) {
            String expected = "Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)";
            assertEquals(expected, sce.getMessage());
        }
    }

    public void testNestingDepthWithSmallLimitNestedObject() throws Exception
    {
        StringWriter sw = new StringWriter();
        IOContext ioc = new IOContext(StreamReadConstraints.defaults(),
                StreamWriteConstraints.builder().maxNestingDepth(1).build(),
                ErrorReportConfiguration.defaults(),
                new BufferRecycler(),
                ContentReference.rawReference(sw), true,
                JsonEncoding.UTF8);
        try (JsonGenerator gen = new WriterBasedJsonGenerator(ObjectWriteContext.empty(), ioc, 0, 0, sw,
                JsonFactory.DEFAULT_ROOT_VALUE_SEPARATOR, null, null,
                0, '"')) {
            gen.writeStartObject();
            gen.writeName("object");
            gen.writeStartObject();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException sce) {
            String expected = "Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)";
            assertEquals(expected, sce.getMessage());
        }
    }
}
