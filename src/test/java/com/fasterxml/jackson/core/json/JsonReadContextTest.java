package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.exc.StreamReadException;

import org.junit.Test;

/**
 * Unit tests for class {@link JsonReadContext}.
 */
public class JsonReadContextTest
    extends com.fasterxml.jackson.core.BaseTest
{
  public void testSetCurrentNameTwiceWithSameName()
  {
      final String PROP_NAME = "4'Du>icate field'";
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = new JsonReadContext((JsonReadContext) null, dupDetector, 2441, 2441, 2441);
      jsonReadContext.setCurrentName(PROP_NAME);
      try {
          jsonReadContext.setCurrentName(PROP_NAME);
      } catch (StreamReadException e) {
          verifyException(e, "Duplicate field");
          verifyException(e, PROP_NAME);
      }
  }

  @Test
  public void testSetCurrentName()
  {
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(0, 0, (DupDetector) null);
      jsonReadContext.setCurrentName("asd / \" € < - _");

      assertEquals("asd / \" € < - _", jsonReadContext.currentName());

      jsonReadContext.setCurrentName(null);

      assertNull(jsonReadContext.currentName());
  }

  public void testReset()
  {
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(dupDetector);

      assertTrue(jsonReadContext.inRoot());
      assertEquals("root", jsonReadContext.typeDesc());
      assertEquals(1, jsonReadContext.getStartLocation(jsonReadContext).getLineNr());
      assertEquals(0, jsonReadContext.getStartLocation(jsonReadContext).getColumnNr());

      jsonReadContext.reset(200, 500, 200);

      assertFalse(jsonReadContext.inRoot());
      assertEquals("?", jsonReadContext.typeDesc());
      assertEquals(500, jsonReadContext.getStartLocation(jsonReadContext).getLineNr());
      assertEquals(200, jsonReadContext.getStartLocation(jsonReadContext).getColumnNr());
  }
}