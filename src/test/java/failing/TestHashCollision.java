package failing;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.sym.CharsToNameCanonicalizer;
import com.fasterxml.jackson.test.BaseTest;

/**
 * Some unit tests to try to exercise part of parser code that
 * deals with symbol (table) management.
 */
public class TestHashCollision
    extends BaseTest
{
    // // // And then a nastier variant; collisions generated using
    // // // CollisionGenerator

    final static String[] MULT_33_COLLISION_FRAGMENTS = new String[] {
        // Ones generated for 65536...
        "9fa", "9g@", ":Ea", ":F@", ";$a", ";%@"
    };

    public void testReaderCollisions() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        for (String field : collisions()) {
            if (sb.length() == 0) {
                sb.append("{");
            } else {
                sb.append(",\n");
            }
            sb.append('"');
            sb.append(field);
            sb.append("\":3");
        }
        sb.append("}");

        JsonFactory jf = new JsonFactory();
        JsonParser jp = jf.createParser(sb.toString());
        while (jp.nextToken() != null) {
            ;
        }
        // and if we got here, fine
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    static List<String> collisions() {
        // we'll get 6^4, which is bit over 1k
        ArrayList<String> result = new ArrayList<String>(36 * 36);
        for (String str1 : MULT_33_COLLISION_FRAGMENTS) {
            for (String str2 : MULT_33_COLLISION_FRAGMENTS) {
                for (String str3 : MULT_33_COLLISION_FRAGMENTS) {
                    for (String str4 : MULT_33_COLLISION_FRAGMENTS) {
                        result.add(str1+str2+str3+str4);
                    }
                }
            }
        }
        return result;
    }

    /*
    /**********************************************************
    /* Helper class(es)
    /**********************************************************
     */
    
    /**
     * Helper class to use for generating substrings to use for substring
     * substitution collisions.
     */
    public static class CollisionGenerator
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
//        final char maxChar = (char) 127;
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
//          new CollisionGenerator().generate3(1 << 20);
          new CollisionGenerator().generate3(1 << 16);

          System.out.println();
          System.out.println("</stuff>");
      }
    }


}
