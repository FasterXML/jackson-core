package com.fasterxml.jackson.core.util;

public class TestTextBuffer
    extends com.fasterxml.jackson.core.BaseTest
{
    /**
     * Trivially simple basic test to ensure all basic append
     * methods work
     */
    public void testSimple()
    {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.append('a');
        tb.append(new char[] { 'X', 'b' }, 1, 1);
        tb.append("c", 0, 1);
        assertEquals(3, tb.contentsAsArray().length);
        assertEquals("abc", tb.toString());

        assertNotNull(tb.expandCurrentSegment());
    }

    public void testLonger()
    {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        for (int i = 0; i < 2000; ++i) {
            tb.append("abc", 0, 3);
        }
        String str = tb.contentsAsString();
        assertEquals(6000, str.length());
        assertEquals(6000, tb.contentsAsArray().length);

        tb.resetWithShared(new char[] { 'a' }, 0, 1);
        assertEquals(1, tb.toString().length());
    }

      public void testLongAppend()
      {
          final int len = TextBuffer.MAX_SEGMENT_LEN * 3 / 2;
          StringBuilder sb = new StringBuilder(len);
          for (int i = 0; i < len; ++i) {
              sb.append('x');
          }
         final String STR = sb.toString();
         final String EXP = "a" + STR + "c";
 
         // ok: first test with String:
         TextBuffer tb = new TextBuffer(new BufferRecycler());
         tb.append('a');
         tb.append(STR, 0, len);
         tb.append('c');
         assertEquals(len+2, tb.size());
         assertEquals(EXP, tb.contentsAsString());
 
         // then char[]
         tb = new TextBuffer(new BufferRecycler());
         tb.append('a');
         tb.append(STR.toCharArray(), 0, len);
         tb.append('c');
         assertEquals(len+2, tb.size());
         assertEquals(EXP, tb.contentsAsString());
      }

      // [Core#152]
      public void testExpand()
      {
          TextBuffer tb = new TextBuffer(new BufferRecycler());
          char[] buf = tb.getCurrentSegment();

          while (buf.length < 500 * 1000) {
              char[] old = buf;
              buf = tb.expandCurrentSegment();
              if (old.length >= buf.length) {
                  fail("Expected buffer of "+old.length+" to expand, did not, length now "+buf.length);
              }
          }
      }

    // [Core#182]
    public void testEmpty() {
        TextBuffer tb = new TextBuffer(new BufferRecycler());
        tb.resetWithEmpty();

        assertTrue(tb.getTextBuffer().length == 0);
        tb.contentsAsString();
        assertTrue(tb.getTextBuffer().length == 0);
    }
}
