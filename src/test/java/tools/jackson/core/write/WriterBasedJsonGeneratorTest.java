package tools.jackson.core.write;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class WriterBasedJsonGeneratorTest extends JUnit5TestBase
{
    private final JsonFactory JSON_MAX_NESTING_1 = JsonFactory.builder()
            .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(1).build())
            .build();

    @Test
    void nestingDepthWithSmallLimit() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = JSON_MAX_NESTING_1.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartObject();
            gen.writeName("array");
            gen.writeStartArray();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)");
        }
    }

    @Test
    void nestingDepthWithSmallLimitNestedObject() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = JSON_MAX_NESTING_1.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartObject();
            gen.writeName("object");
            gen.writeStartObject();
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth (2) exceeds the maximum allowed (1, from `StreamWriteConstraints.getMaxNestingDepth()`)");
        }
    }
}
