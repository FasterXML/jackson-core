package com.fasterxml.jackson.core.io.schubfach;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SchubfachFloatTest extends FloatToStringTest {
  @Override
  String f(float f) {
    return FloatToDecimal.toString(f);
  }
}
