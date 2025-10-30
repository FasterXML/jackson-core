package com.fasterxml.jackson.core.json;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.ContentReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link JsonReadContext}.
 */
class JsonReadContextTest extends JUnit5TestBase
{
    static class MyContext extends JsonReadContext {
        public MyContext(JsonReadContext parent, int nestingDepth, DupDetector dups,
                         int type, int lineNr, int colNr) {
            super(parent, nestingDepth, dups, type, lineNr, colNr);
        }
    }

    @Test
    void setCurrentNameTwiceWithSameNameRaisesJsonParseException() throws Exception
    {
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = new JsonReadContext((JsonReadContext) null, 0,
              dupDetector, 2441, 2441, 2441);
      jsonReadContext.setCurrentName("dupField");
      try {
          jsonReadContext.setCurrentName("dupField");
          fail("Should not pass");
      } catch (JsonParseException e) {
          verifyException(e, "Duplicate field 'dupField'");
      }
    }

    @Test
    void setCurrentName() throws Exception
    {
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(0, 0, (DupDetector) null);
      jsonReadContext.setCurrentName("abc");
      assertEquals("abc", jsonReadContext.getCurrentName());
      jsonReadContext.setCurrentName(null);
      assertNull(jsonReadContext.getCurrentName());
    }

    @Test
    void reset()
    {
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(dupDetector);
      final ContentReference bogusSrc = ContentReference.unknown();

      assertTrue(jsonReadContext.inRoot());
      assertEquals("root", jsonReadContext.typeDesc());
      assertEquals(1, jsonReadContext.startLocation(bogusSrc).getLineNr());
      assertEquals(0, jsonReadContext.startLocation(bogusSrc).getColumnNr());

      jsonReadContext.reset(200, 500, 200);

      assertFalse(jsonReadContext.inRoot());
      assertEquals("?", jsonReadContext.typeDesc());
      assertEquals(500, jsonReadContext.startLocation(bogusSrc).getLineNr());
      assertEquals(200, jsonReadContext.startLocation(bogusSrc).getColumnNr());
    }

    // [core#1421]
    @Test
    void testExtension() {
        MyContext context = new MyContext(null, 0, null, 0, 0, 0);
        assertNotNull(context);
    }
}
