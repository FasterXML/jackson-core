package tools.jackson.core.unittest.io.xjb;

import tools.jackson.core.io.xjb.XJBWriter;

public class XJBFloatTest extends XJBFloatToStringTest {
  @Override
  String f(float f) {
    return XJBWriter.toString(f);
  }
}
