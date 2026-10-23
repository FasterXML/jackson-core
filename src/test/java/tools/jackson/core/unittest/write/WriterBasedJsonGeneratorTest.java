package tools.jackson.core.unittest.write;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.SerializableString;
import tools.jackson.core.StreamWriteConstraints;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class WriterBasedJsonGeneratorTest extends JacksonCoreTestBase
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

    // [core#1776]: if CharacterEscapes callback throws, already scanned content
    // must remain in the buffer
    @Test
    void customEscapesFailureKeepsScannedPrefix() throws Exception
    {
        final CharacterEscapes failing = new CharacterEscapes() {
            private static final long serialVersionUID = 1L;

            @Override
            public int[] getEscapeCodesForAscii() {
                return standardAsciiEscapesForJSON();
            }

            @Override
            public SerializableString getEscapeSequence(int ch) {
                throw new IllegalStateException("fail on 0x" + Integer.toHexString(ch));
            }
        };
        JsonFactory f = JsonFactory.builder().characterEscapes(failing).build();
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = f.createGenerator(ObjectWriteContext.empty(), sw)) {
            gen.writeStartArray();
            try {
                gen.writeString("abc\u00E9def");
                fail("expected IllegalStateException");
            } catch (IllegalStateException e) {
                assertThat(e.getMessage()).isEqualTo("fail on 0xe9");
            }
            gen.flush();
            assertThat(sw.toString()).isEqualTo("[\"abc");
        }
    }
}
