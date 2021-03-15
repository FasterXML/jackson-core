package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.io.InputSourceReference;

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
          verifyException(e, "Duplicate Object property \"");
          verifyException(e, PROP_NAME);
      }
  }

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
      final InputSourceReference bogusSrc = InputSourceReference.unknown();

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