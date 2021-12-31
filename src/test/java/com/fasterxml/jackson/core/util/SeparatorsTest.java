package com.fasterxml.jackson.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link Separators}.
 *
 * @date 2017-07-31
 * @see Separators
 **/
public class SeparatorsTest {

  @Test
  public void testWithArrayValueSeparatorWithDigit() {
    Separators separators = new Separators('5', '5', '5');
    Separators separatorsTwo = separators.withArrayValueSeparator('5');

    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('5', separatorsTwo.getObjectFieldValueSeparator());
    assertEquals('5', separatorsTwo.getArrayValueSeparator());
    assertSame(separatorsTwo, separators);

    separatorsTwo = separators.withArrayValueSeparator('6');

    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('5', separatorsTwo.getObjectFieldValueSeparator());
    assertEquals('6', separatorsTwo.getArrayValueSeparator());
    assertNotSame(separatorsTwo, separators);

  }

  @Test
  public void testWithObjectEntrySeparator() {
    Separators separators = new Separators('5', '5', '5');
    Separators separatorsTwo = separators.withObjectEntrySeparator('!');
    Separators separatorsThree = separatorsTwo.withObjectEntrySeparator('!');

    assertEquals('!', separatorsThree.getObjectEntrySeparator());
    assertEquals('5', separatorsThree.getObjectFieldValueSeparator());

    assertSame(separatorsThree, separatorsTwo);
    assertEquals('5', separators.getArrayValueSeparator());

    assertEquals('5', separatorsThree.getArrayValueSeparator());
    assertEquals('5', separators.getObjectFieldValueSeparator());
  }

  @Test
  public void testWithObjectFieldValueSeparatorWithDigit() {
    Separators separators = new Separators('5', '5', '5');
    Separators separatorsTwo = separators.withObjectFieldValueSeparator('5');

    assertEquals('5', separatorsTwo.getArrayValueSeparator());
    assertSame(separatorsTwo, separators);
    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('5', separatorsTwo.getObjectFieldValueSeparator());

    separatorsTwo = separators.withObjectFieldValueSeparator('6');

    assertEquals('5', separatorsTwo.getArrayValueSeparator());
    assertNotSame(separatorsTwo, separators);
    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('6', separatorsTwo.getObjectFieldValueSeparator());

  }

}