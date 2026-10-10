package tools.jackson.core.unittest.io.xjb;

import tools.jackson.core.io.xjb.XJBWriter;

public class XJBDoubleTest extends XJBDoubleToStringTest {
  @Override
  String f(double f) {
    return XJBWriter.toString(f);
  }
}
