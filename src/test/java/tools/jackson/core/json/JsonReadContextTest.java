package tools.jackson.core.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;

/**
 * Unit tests for class {@link JsonReadContext}.
 */
public class JsonReadContextTest
    extends tools.jackson.core.BaseTest
{
  public void testSetCurrentNameTwiceWithSameName()
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

  public void testSetCurrentName()
  {
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(0, 0, (DupDetector) null);
      jsonReadContext.setCurrentName("abc");
      assertEquals("abc", jsonReadContext.currentName());
      jsonReadContext.setCurrentName(null);
      assertNull(jsonReadContext.currentName());
  }

  public void testReset()
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
}