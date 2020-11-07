package com.fasterxml.jackson.failing.filter;

import com.fasterxml.jackson.core.BaseTest;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.filter.FilteringParserDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.fasterxml.jackson.core.filter.TokenFilter.Inclusion;

public class BasicParserFilteringTest extends BaseTest {

  private final JsonFactory JSON_F = new JsonFactory();

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
    JsonParser p0 = JSON_F.createParser(jsonString);
    FilteringParserDelegate p = new FilteringParserDelegate(p0,
        new NoArraysFilter(),
        Inclusion.INCLUDE_NON_NULL,
        true // multipleMatches
    );
    String result = readAndWrite(JSON_F, p);
    assertEquals(aposToQuotes("{'a':123}"), result);
    assertEquals(0, p.getMatchCount());
  }
}
