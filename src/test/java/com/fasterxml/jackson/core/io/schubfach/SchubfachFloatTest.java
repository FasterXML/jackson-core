package com.fasterxml.jackson.core.io.schubfach;

public class SchubfachFloatTest extends FloatToStringTest {
  @Override
  String f(float f) {
    return FloatToDecimal.toString(f);
  }
}
