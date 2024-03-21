package tools.jackson.core.io.schubfach;

public class SchubfachDoubleTest extends DoubleToStringTest {
  @Override
  String f(double f) {
    return DoubleToDecimal.toString(f);
  }
}
