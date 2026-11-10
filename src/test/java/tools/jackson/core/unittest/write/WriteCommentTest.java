package tools.jackson.core.unittest.write;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.JsonGeneratorDelegate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link JsonGenerator#writeComment(String)} and
 * {@link JsonGenerator#canWriteComments()} default implementations
 * and delegation.
 */
class WriteCommentTest
    extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = sharedStreamFactory();

    // Verify that JSON generator does not support comments by default
    @Test
    void noCommentSupportForJson() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw)) {
            assertFalse(gen.canWriteComments());
            try {
                gen.writeComment("a comment");
                fail("Should not pass");
            } catch (UnsupportedOperationException e) {
                verifyException(e, "does not support writing Comments");
            }
        }
    }

    // Verify null comment also throws for non-supporting format
    @Test
    void noCommentSupportNullComment() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator gen = JSON_F.createGenerator(ObjectWriteContext.empty(), sw)) {
            try {
                gen.writeComment(null);
                fail("Should not pass");
            } catch (UnsupportedOperationException e) {
                verifyException(e, "does not support writing Comments");
            }
        }
    }

    // Verify that delegate forwards canWriteComments() to underlying generator
    @Test
    void delegateCanWriteComments() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g0 = JSON_F.createGenerator(ObjectWriteContext.empty(), sw)) {
            JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);
            assertFalse(del.canWriteComments());
            // Should match the underlying generator
            assertEquals(g0.canWriteComments(), del.canWriteComments());
            del.close();
        }
    }

    // Verify that delegate forwards writeComment() to underlying generator
    @Test
    void delegateWriteComment() throws Exception
    {
        StringWriter sw = new StringWriter();
        try (JsonGenerator g0 = JSON_F.createGenerator(ObjectWriteContext.empty(), sw)) {
            JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0);
            try {
                del.writeComment("test");
                fail("Should not pass");
            } catch (UnsupportedOperationException e) {
                verifyException(e, "does not support writing Comments");
            }
            del.close();
        }
    }

    // Verify writeComment returns delegate itself (not the underlying generator)
    @Test
    void delegateWriteCommentReturnValue() throws Exception
    {
        // Use a custom delegate that overrides to succeed
        StringWriter sw = new StringWriter();
        try (JsonGenerator g0 = JSON_F.createGenerator(ObjectWriteContext.empty(), sw)) {
            JsonGeneratorDelegate del = new JsonGeneratorDelegate(g0) {
                @Override
                public JsonGenerator writeComment(String comment) {
                    // no-op: just test return value
                    return this;
                }
            };
            assertSame(del, del.writeComment("test"));
            del.close();
        }
    }
}
