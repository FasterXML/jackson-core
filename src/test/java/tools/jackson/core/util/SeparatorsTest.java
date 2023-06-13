package tools.jackson.core.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link Separators}.
 *
 * @see Separators
 */
public class SeparatorsTest {

  @Test
  public void testWithArrayValueSeparatorWithDigit() {
    Separators separators = new Separators('5', '5', '5');
    Separators separatorsTwo = separators.withArrayElementSeparator('5');

    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('5', separatorsTwo.getObjectNameValueSeparator());
    assertEquals('5', separatorsTwo.getArrayElementSeparator());
    assertSame(separatorsTwo, separators);

    separatorsTwo = separators.withArrayElementSeparator('6');

    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('5', separatorsTwo.getObjectNameValueSeparator());
    assertEquals('6', separatorsTwo.getArrayElementSeparator());
    assertNotSame(separatorsTwo, separators);

  }

  @Test
  public void testWithObjectEntrySeparator() {
    Separators separators = new Separators('5', '5', '5');
    Separators separatorsTwo = separators.withObjectEntrySeparator('!');
    Separators separatorsThree = separatorsTwo.withObjectEntrySeparator('!');

    assertEquals('!', separatorsThree.getObjectEntrySeparator());
    assertEquals('5', separatorsThree.getObjectNameValueSeparator());

    assertSame(separatorsThree, separatorsTwo);
    assertEquals('5', separators.getArrayElementSeparator());

    assertEquals('5', separatorsThree.getArrayElementSeparator());
    assertEquals('5', separators.getObjectNameValueSeparator());
  }

  @Test
  public void testWithObjectNameValueSeparatorWithDigit() {
    Separators separators = new Separators('5', '5', '5');
    Separators separatorsTwo = separators.withObjectNameValueSeparator('5');

    assertEquals('5', separatorsTwo.getArrayElementSeparator());
    assertSame(separatorsTwo, separators);
    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('5', separatorsTwo.getObjectNameValueSeparator());

    separatorsTwo = separators.withObjectNameValueSeparator('6');

    assertEquals('5', separatorsTwo.getArrayElementSeparator());
    assertNotSame(separatorsTwo, separators);
    assertEquals('5', separatorsTwo.getObjectEntrySeparator());
    assertEquals('6', separatorsTwo.getObjectNameValueSeparator());
  }
}
