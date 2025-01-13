package tools.jackson.core.unittest.read;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [jackson-core#730]
class ParserPrecisionLoss730Test extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = newStreamFactory();

    // [jackson-core#730]
    /**
     * Attempt to pass a BigDecimal value through without losing precision,
     * e.g. for pretty printing a file.
     */
    @Test
    void copyCurrentEventBigDecimal() throws Exception {
        String input = "1E+999";
        StringWriter stringWriter = new StringWriter();
        try (JsonParser parser = JSON_F.createParser(ObjectReadContext.empty(), input)) {
            parser.nextToken();
            try (JsonGenerator generator = JSON_F.createGenerator(ObjectWriteContext.empty(), stringWriter)) {
                generator.copyCurrentEventExact(parser);
            }
        }
        assertEquals(input, stringWriter.toString());
    }
}
