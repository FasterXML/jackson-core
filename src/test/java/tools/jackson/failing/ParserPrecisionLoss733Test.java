package tools.jackson.failing;

import tools.jackson.core.BaseTest;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.ObjectWriteContext;
import tools.jackson.core.json.JsonFactory;

import java.io.StringWriter;

public class ParserPrecisionLoss733Test extends BaseTest {
    private final JsonFactory JSON_F = newStreamFactory();

    /**
     * Attempt to pass a BigDecimal value through without losing precision,
     * e.g. for pretty printing a file.
     */
    public void testCopyCurrentEventBigDecimal() throws Exception {
        String input = "1e999";
        JsonParser parser = JSON_F.createParser(ObjectReadContext.empty(), input);
        parser.nextToken();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator generator = JSON_F.createGenerator(ObjectWriteContext.empty(), stringWriter);
        generator.copyCurrentEvent(parser);
        parser.close();
        generator.close();
        String actual = stringWriter.toString();
        assertEquals(input, actual);
    }

    /**
     * Same as {@link #testCopyCurrentEventBigDecimal()} using copyCurrentStructure instead.
     */
    public void testCopyCurrentStructureBigDecimal() throws Exception {
        String input = "[1e999]";
        JsonParser parser = JSON_F.createParser(ObjectReadContext.empty(), input);
        parser.nextToken();
        StringWriter stringWriter = new StringWriter();
        JsonGenerator generator = JSON_F.createGenerator(ObjectWriteContext.empty(), stringWriter);
        generator.copyCurrentStructure(parser);
        parser.close();
        generator.close();
        String actual = stringWriter.toString();
        assertEquals(input, actual);
    }

}
