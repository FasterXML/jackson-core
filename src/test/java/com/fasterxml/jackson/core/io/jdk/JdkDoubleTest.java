package com.fasterxml.jackson.core.io.jdk;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JdkDoubleTest extends DoubleToStringTest {
  @Override
  String f(double f) {
    return Double.toString(f);
  }
}
