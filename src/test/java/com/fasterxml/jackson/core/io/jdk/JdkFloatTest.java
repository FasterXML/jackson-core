package com.fasterxml.jackson.core.io.jdk;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JdkFloatTest extends FloatToStringTest {
  @Override
  String f(float f) {
    return Float.toString(f);
  }
}
