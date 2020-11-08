package com.fasterxml.jackson.failing.filter;

import java.io.IOException;
import java.io.StringWriter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectReadContext;
import com.fasterxml.jackson.core.ObjectWriteContext;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;
import com.fasterxml.jackson.core.json.JsonFactory;

public class BasicParserFilteringTest extends BaseTest {

  private final JsonFactory JSON_F = newStreamFactory();

  static class NoArraysFilter extends TokenFilter
  {
    @Override
    public TokenFilter filterStartArray() {
      return null;
    }
  }

  // for [core#649]
  public void testValueOmitsFieldName1() throws Exception
  {
    String jsonString = aposToQuotes("{'a':123,'array':[1,2]}");
    JsonParser p0 = JSON_F.createParser(ObjectReadContext.empty(), jsonString);
    FilteringParserDelegate p = new FilteringParserDelegate(p0,
        new NoArraysFilter(),
        Inclusion.INCLUDE_NON_NULL,
        true // multipleMatches
    );
    String result = readAndWrite(JSON_F, p);
    assertEquals(aposToQuotes("{'a':123}"), result);
    assertEquals(0, p.getMatchCount());
  }

  private String readAndWrite(JsonFactory f, JsonParser p) throws IOException
  {
      StringWriter sw = new StringWriter(100);
      JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), sw);
      //g.disable(StreamWriteFeature.AUTO_CLOSE_CONTENT);
      try {
          while (p.nextToken() != null) {
              g.copyCurrentEvent(p);
          }
      } catch (IOException e) {
          g.flush();
          fail("Unexpected problem during `readAndWrite`. Output so far: '"+sw+"'; problem: "+e);
      }
      p.close();
      g.close();
      return sw.toString();
  }
}
