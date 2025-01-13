package tools.jackson.core.unittest.io.schubfach;

import tools.jackson.core.io.schubfach.DoubleToDecimal;

public class SchubfachDoubleTest extends DoubleToStringTest {
  @Override
  String f(double f) {
    return DoubleToDecimal.toString(f);
  }
}
