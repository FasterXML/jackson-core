package perf;

import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;

public class CollisionGenerator
{
    /* JDK uses 31, but Jackson `CharsToNameCanonicalizer.HASH_MULT`,
     * which for 2.3 is 33.
     */
    
    final static int MULT = CharsToNameCanonicalizer.HASH_MULT;

    public void generate3(int h0) {
      int p1 = MULT;
      int p0 = MULT * MULT;

      // Generate chars in printable ASCII range
      
      final char minChar = (char) 32;
//    final char maxChar = (char) 127;
    final char maxChar = (char) 127;

    for (char c0 = minChar; c0 <= maxChar && c0 <= h0 / p0; c0++) {
      int h1 = h0 - c0 * p0;
      for (char c1 = minChar; c1 <= maxChar && c1 <= h1 / p1; c1++) {
        int h2 = h1 - c1 * p1;
        
        if (h2 < minChar || h2 > maxChar) {
            continue;
        }
        char c2 = (char) h2;
        if (h2 != c2) {
            continue;
        }
        printString(new String(new char[] { c0, c1, c2 } ));
      }
    }
  }

  void printString(String s) {
      //System.out.println(s.length() + " " + s.hashCode() + " " + asIntArray(s));
      System.out.println(s.length() + " " + s.hashCode() + " \"" + s + "\"");
  }

  String asIntArray(String s) {
    StringBuilder result = new StringBuilder().append("[");
    for (int c = 0; c < s.length(); c++) {
      if (c > 0) {
        result.append(", ");
      }
      result.append((int) s.charAt(c));
    }
    result.append("]");
    return result.toString();
  }

  public static void main(String[] args) {
      System.out.println("<stuff>");
//      new CollisionGenerator().generate3(1 << 20);
      new CollisionGenerator().generate3(1 << 16);

      System.out.println();
      System.out.println("</stuff>");
  }
}

