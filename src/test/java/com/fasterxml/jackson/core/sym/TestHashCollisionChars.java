package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.*;

/**
 * Some unit tests to try to exercise part of parser code that
 * deals with symbol (table) management.
 *<p>
 * Note that the problem does not necessarily affect code at or
 * after Jackson 2.6, since hash calculation algorithm has been
 * completely changed. It may still be relevant for character-backed
 * sources, however.
 */
public class TestHashCollisionChars
    extends BaseTest
{
    // // // And then a nastier variant; collisions generated using
    // // // CollisionGenerator

    // for 33
    final static String[] MULT_COLLISION_FRAGMENTS = new String[] {
        // Ones generated for 33/65536...
        "9fa", "9g@", ":Ea", ":F@", ";$a", ";%@"
    };

    // for 31
    /*
    final static String[] MULT_COLLISION_FRAGMENTS = new String[] {
        // Ones generated for 31/65536...
        "@~~", "A_~", "A`_", "Aa@", "Ab!", "B@~", // "BA_", "BB@", "BC!", "C!~"
    };
    */

    public void testReaderCollisions() throws Exception
    {
        StringBuilder sb = new StringBuilder();
        List<String> coll = collisions();

        for (String field : coll) {
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

        // First: attempt with exceptions turned on; should catch an exception

        JsonFactory f = JsonFactory.builder()
                .enable(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW)
                .build();
        JsonParser p = f.createParser(sb.toString());

        try {
            while (p.nextToken() != null) {
                ;
            }
            fail("Should have failed");
        } catch (IllegalStateException e) {
            verifyException(e, "hash collision");
        }
        p.close();

        // but then without feature, should pass
        f = JsonFactory.builder()
                .disable(JsonFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW)
                .build();
        p = f.createParser(sb.toString());
        while (p.nextToken() != null) {
            ;
        }
        p.close();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    static List<String> collisions() {
        // we'll get 6^4, which is bit over 1k
        ArrayList<String> result = new ArrayList<String>(36 * 36);

        final String[] FRAGMENTS = MULT_COLLISION_FRAGMENTS;

        for (String str1 : FRAGMENTS) {
            for (String str2 : FRAGMENTS) {
                for (String str3 : FRAGMENTS) {
                    for (String str4 : FRAGMENTS) {
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
    static class CollisionGenerator
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

    }

    public static void main(String[] args) {
        System.out.println("<stuff>");
//        new CollisionGenerator().generate3(1 << 20);
        new CollisionGenerator().generate3(1 << 16);

        System.out.println();
        System.out.println("</stuff>");
    }
}
