package com.fasterxml.jackson.core.io;

import com.fasterxml.jackson.core.util.BufferRecycler;

public class SegmentedStringWriterTest
    extends com.fasterxml.jackson.core.BaseTest
{
    public void testSimple() throws Exception
    {
        BufferRecycler br = new BufferRecycler();
        SegmentedStringWriter w = new SegmentedStringWriter(br);

        StringBuilder exp = new StringBuilder();

        for (int i = 0; exp.length() < 100; ++i) {
            String nr = String.valueOf(i);
            exp.append(' ').append(nr);
            w.append(' ');
            switch (i % 4) {
            case 0:
                w.append(nr);
                break;
            case 1:
                {
                    String str = "  "+nr;
                    w.append(str, 2, str.length());
                }
                break;
            case 2:
                w.write(nr.toCharArray());
                break;
            default:
                {
                    char[] ch = (" "+nr+" ").toCharArray();
                    w.write(ch, 1, nr.length());
                }
                break;
            }
        }
        // flush, close are nops but trigger just for fun
        w.flush();
        w.close();

        String act = w.getAndClear();
        assertEquals(exp.toString(), act);
    }
}
