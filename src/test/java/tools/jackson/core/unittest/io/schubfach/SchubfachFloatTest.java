package tools.jackson.core.unittest.io.schubfach;

import tools.jackson.core.io.schubfach.FloatToDecimal;

public class SchubfachFloatTest extends FloatToStringTest {
  @Override
  String f(float f) {
    return FloatToDecimal.toString(f);
  }
}
