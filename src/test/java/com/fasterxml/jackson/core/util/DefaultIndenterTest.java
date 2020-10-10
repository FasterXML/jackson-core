package com.fasterxml.jackson.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link DefaultIndenter}.
 *
 * @date 2017-07-31
 * @see DefaultIndenter
 **/
public class DefaultIndenterTest {

  @Test
  public void testWithLinefeed() {
    DefaultIndenter defaultIndenter = new DefaultIndenter();
    DefaultIndenter defaultIndenterTwo = defaultIndenter.withLinefeed("-XG'#x");
    DefaultIndenter defaultIndenterThree = defaultIndenterTwo.withLinefeed("-XG'#x");

    assertEquals("-XG'#x", defaultIndenterThree.getEol());
    assertNotSame(defaultIndenterThree, defaultIndenter);
    assertSame(defaultIndenterThree, defaultIndenterTwo);
  }

  @Test
  public void testWithIndent() {
    DefaultIndenter defaultIndenter = new DefaultIndenter();
    DefaultIndenter defaultIndenterTwo = defaultIndenter.withIndent("9Qh/6,~n");
    DefaultIndenter defaultIndenterThree = defaultIndenterTwo.withIndent("9Qh/6,~n");

    assertEquals(System.lineSeparator(), defaultIndenterThree.getEol());
    assertNotSame(defaultIndenterThree, defaultIndenter);
    assertSame(defaultIndenterThree, defaultIndenterTwo);
  }

}