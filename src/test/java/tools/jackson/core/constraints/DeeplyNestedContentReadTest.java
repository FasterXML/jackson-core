package tools.jackson.core.constraints;

import tools.jackson.core.*;
import tools.jackson.core.exc.StreamConstraintsException;
import tools.jackson.core.json.JsonFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test(s) for verifying handling of new (in 2.15) StreamReadConstraints
 * wrt maximum nesting depth.
 */
public class DeeplyNestedContentReadTest
    extends BaseTest
{
    private final JsonFactory JSON_F = newStreamFactory();

    private final int MAX_NESTING = StreamReadConstraints.DEFAULT_MAX_DEPTH;

    public void testDeepNestingStreaming() throws Exception
    {
        // only needs to be one more
        final String DOC = createDeepNestedDoc(MAX_NESTING + 1);
        for (int mode : ALL_STREAMING_MODES) {
            try (JsonParser p = createParser(JSON_F, mode, DOC)) {
                _testDeepNesting(p);
            }
        }
    }

    private void _testDeepNesting(JsonParser p) throws Exception
    {
        try {
            while (p.nextToken() != null) { }
            fail("expected StreamConstraintsException");
        } catch (StreamConstraintsException e) {
            assertThat(e.getMessage())
                .startsWith("Document nesting depth ("+
                        (MAX_NESTING+1)+") exceeds the maximum allowed ("+
                        MAX_NESTING+", from `StreamReadConstraints.getMaxNestingDepth()`)");
        }
    }

    private String createDeepNestedDoc(final int depth) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < depth; i++) {
            sb.append("{ \"a\": [");
        }
        sb.append(" \"val\" ");
        for (int i = 0; i < depth; i++) {
            sb.append("]}");
        }
        sb.append("]");
        return sb.toString();
    }
}
