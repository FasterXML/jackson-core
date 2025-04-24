package tools.jackson.core.unittest.json;

import org.junit.jupiter.api.Test;
import tools.jackson.core.json.DupDetector;
import tools.jackson.core.json.JsonWriteContext;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link JsonWriteContext}.
 */
class JsonWriteContextTest extends JacksonCoreTestBase
{
    static class MyContext extends JsonWriteContext {
        public MyContext(int type, JsonWriteContext parent, DupDetector dups, Object currValue) {
            super(type, parent, dups, currValue);
        }
    }

    @Test
    void testExtension() {
        MyContext context = new MyContext(0, null, null, 0);
        assertNotNull(context);
    }
}