package com.fasterxml.jackson.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for class {@link DefaultIndenter}.
 *
 * @date 2017-07-31
 * @see DefaultIndenter
 **/
class DefaultIndenterTest {

    @Test
    void withLinefeed() {
    DefaultIndenter defaultIndenter = new DefaultIndenter();
    DefaultIndenter defaultIndenterTwo = defaultIndenter.withLinefeed("-XG'#x");
    DefaultIndenter defaultIndenterThree = defaultIndenterTwo.withLinefeed("-XG'#x");

    assertEquals("-XG'#x", defaultIndenterThree.getEol());
    assertNotSame(defaultIndenterThree, defaultIndenter);
    assertSame(defaultIndenterThree, defaultIndenterTwo);
  }

    @Test
    void withIndent() {
    DefaultIndenter defaultIndenter = new DefaultIndenter();
    DefaultIndenter defaultIndenterTwo = defaultIndenter.withIndent("9Qh/6,~n");
    DefaultIndenter defaultIndenterThree = defaultIndenterTwo.withIndent("9Qh/6,~n");

    assertEquals(System.lineSeparator(), defaultIndenterThree.getEol());
    assertNotSame(defaultIndenterThree, defaultIndenter);
    assertSame(defaultIndenterThree, defaultIndenterTwo);
  }

}