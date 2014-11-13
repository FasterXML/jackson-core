package com.fasterxml.jackson.core.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * Default linefeed-based indenter uses system-specific linefeeds and
 * 2 spaces for indentation per level.
 */
public class DefaultIndenter
    extends DefaultPrettyPrinter.NopIndenter
{
    public final static String SYS_LF;
    static {
        String lf;
        try {
            lf = System.getProperty("line.separator");
        } catch (Throwable t) {
            lf = "\n"; // fallback when security manager denies access
        }
        SYS_LF = lf;
    }

    public static final DefaultIndenter SYSTEM_LINEFEED_INSTANCE = new DefaultIndenter("  ", SYS_LF);

    private final static int INDENT_LEVELS = 64;
    private final char[] indents;
    private final int charsPerLevel;
    private final String eol;

    /** Create an indenter which uses the <code>indent</code> string to indent one level
     *  and the <code>eol</code> string to separate lines. */
    public DefaultIndenter(String indent, String eol)
    {
        charsPerLevel = indent.length();

        this.indents = new char[indent.length() * INDENT_LEVELS];
        int offset = 0;
        for (int i=0; i<INDENT_LEVELS; i++) {
            indent.getChars(0, indent.length(), this.indents, offset);
            offset += indent.length();
        }

        this.eol = eol;
    }

    @Override
    public boolean isInline() { return false; }

    @Override
    public void writeIndentation(JsonGenerator jg, int level)
        throws IOException, JsonGenerationException
    {
        jg.writeRaw(eol);
        if (level > 0) { // should we err on negative values (as there's some flaw?)
            level *= charsPerLevel;
            while (level > indents.length) { // should never happen but...
                jg.writeRaw(indents, 0, indents.length); 
                level -= indents.length;
            }
            jg.writeRaw(indents, 0, level);
        }
    }
}