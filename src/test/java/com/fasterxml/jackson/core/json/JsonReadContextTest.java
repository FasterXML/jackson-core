package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.io.ContentReference;

/**
 * Unit tests for class {@link JsonReadContext}.
 */
public class JsonReadContextTest extends BaseTest
{
  public void testSetCurrentNameTwiceWithSameNameRaisesJsonParseException() throws Exception
  {
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = new JsonReadContext((JsonReadContext) null, dupDetector, 2441, 2441, 2441);
      jsonReadContext.setCurrentName("dupField");
      try {
          jsonReadContext.setCurrentName("dupField");
          fail("Should not pass");
      } catch (JsonParseException e) {
          verifyException(e, "Duplicate field 'dupField'");
      }
  }

  public void testSetCurrentName() throws Exception
  {
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(0, 0, (DupDetector) null);
      jsonReadContext.setCurrentName("abc");
      assertEquals("abc", jsonReadContext.getCurrentName());
      jsonReadContext.setCurrentName(null);
      assertNull(jsonReadContext.getCurrentName());
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