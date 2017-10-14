package com.fasterxml.jackson.core.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link JsonReadContext}.
 *
 * @date 2017-08-01
 * @see JsonReadContext
 *
 **/
public class JsonReadContextTest{

  @Test(expected = JsonParseException.class)
  public void testSetCurrentNameTwiceWithSameNameRaisesJsonParseException() throws JsonProcessingException {
      DupDetector dupDetector = DupDetector.rootDetector((JsonGenerator) null);
      JsonReadContext jsonReadContext = new JsonReadContext((JsonReadContext) null, dupDetector, 2441, 2441, 2441);
      jsonReadContext.setCurrentName("4'Du>icate field'");
      jsonReadContext.setCurrentName("4'Du>icate field'");
  }

  @Test
  public void testSetCurrentName() throws JsonProcessingException {
      JsonReadContext jsonReadContext = JsonReadContext.createRootContext(0, 0, (DupDetector) null);
      jsonReadContext.setCurrentName("asd / \" € < - _");

      assertEquals("asd / \" € < - _", jsonReadContext.getCurrentName());

      jsonReadContext.setCurrentName(null);

      assertNull(jsonReadContext.getCurrentName());
  }

  @Test
  public void testReset() {
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