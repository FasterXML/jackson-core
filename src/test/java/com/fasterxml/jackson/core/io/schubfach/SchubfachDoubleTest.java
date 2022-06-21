package com.fasterxml.jackson.core.io.schubfach;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SchubfachDoubleTest extends DoubleToStringTest {
  @Override
  String f(double f) {
    return DoubleToDecimal.toString(f);
  }
}
