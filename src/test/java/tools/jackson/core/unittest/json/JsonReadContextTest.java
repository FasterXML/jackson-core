package tools.jackson.core.unittest.json;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.json.DupDetector;
import tools.jackson.core.json.JsonReadContext;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link JsonReadContext}.
 */
class JsonReadContextTest extends JacksonCoreTestBase
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
      final String PROP_NAME = "dupField";
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = new JsonReadContext((JsonReadContext) null, 0, dupDetector, 2441, 2441, 2441);
      jsonReadContext.setCurrentName(PROP_NAME);
      try {
          jsonReadContext.setCurrentName(PROP_NAME);
      } catch (StreamReadException e) {
          verifyException(e, "Duplicate Object property \""+PROP_NAME+"\"");
          verifyException(e, PROP_NAME);
      }
    }

    @Test
    void setCurrentName() throws Exception
    {
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(0, 0, (DupDetector) null);
      jsonReadContext.setCurrentName("abc");
      assertEquals("abc", jsonReadContext.currentName());
      jsonReadContext.setCurrentName(null);
      assertNull(jsonReadContext.currentName());
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
