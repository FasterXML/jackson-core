package com.fasterxml.jackson.core.filter;

import org.junit.Test;
import static org.junit.Assert.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.filter.TokenFilter;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Unit tests for class {@link TokenFilter}.
 *
 * @date 16.07.2017
 * @see TokenFilter
 *
 **/
public class TokenFilterTest{


  @Test
  public void testCreatesTokenFilter() {

      TokenFilter tokenFilter = new TokenFilter();

      assertFalse( tokenFilter.toString().equals("TokenFilter.INCLUDE_ALL") );

  }


  @Test
  public void testIncludeEmbeddedValue() {

      TokenFilter tokenFilter = new TokenFilter();

      assertTrue(tokenFilter.includeEmbeddedValue(tokenFilter));

  }


}