package com.fasterxml.jackson.core.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link JsonWriteContext}.
 */
public class JsonWriteContextTest extends JUnit5TestBase
{
    static class MyContext extends JsonWriteContext {
        public MyContext(int type, JsonWriteContext parent, DupDetector dups, Object currValue) {
            super(type, parent, dups, currValue);
        }
    }

    // [core#1421]
    @Test
    void testExtension() {
        MyContext context = new MyContext(0, null, null, 0);
        assertNotNull(context);
    }
}
