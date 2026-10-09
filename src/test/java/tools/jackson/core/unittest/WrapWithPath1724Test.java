package tools.jackson.core.unittest;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.exc.StreamReadException;

import static org.junit.jupiter.api.Assertions.*;

class WrapWithPath1724Test
{
    @Test
    void usesOriginalMessageOfTargetJacksonException()
    {
        StreamReadException cause = new StreamReadException(null, "Bad value",
                new TokenStreamLocation(null, -1L, 2, 3));
        cause.prependPath(this, "inner");
        InvocationTargetException src = new InvocationTargetException(cause);

        JacksonException wrapped = JacksonException.wrapWithPath(src, this, "value");

        assertEquals("Bad value", wrapped.getOriginalMessage());
        assertSame(src, wrapped.getCause());
        assertSame(cause, wrapped.getCause().getCause());
        assertEquals(1, wrapped.getPath().size());
        assertEquals("value", wrapped.getPath().get(0).getPropertyName());
        assertEquals(1, cause.getPath().size());
        assertEquals("inner", cause.getPath().get(0).getPropertyName());
    }

    @Test
    void usesTargetTypeWhenTargetMessageIsEmpty()
    {
        for (String message : new String[] { null, "" }) {
            InvocationTargetException src = new InvocationTargetException(
                    new IllegalStateException(message));

            JacksonException wrapped = JacksonException.wrapWithPath(src, this, "value");

            assertEquals("(was java.lang.IllegalStateException)", wrapped.getOriginalMessage());
            assertSame(src, wrapped.getCause());
        }
    }

    @Test
    void retainsTargetMessageAndOriginalCause()
    {
        InvocationTargetException src = new InvocationTargetException(
                new IllegalArgumentException("Bad value"), "Wrapper message");

        JacksonException wrapped = JacksonException.wrapWithPath(src, this, 3);

        assertEquals("Bad value", wrapped.getOriginalMessage());
        assertSame(src, wrapped.getCause());
        assertEquals(3, wrapped.getPath().get(0).getIndex());
    }

    @Test
    void handlesInvocationTargetExceptionWithoutCause()
    {
        InvocationTargetException src = new InvocationTargetException(null, "Wrapper message");
        JacksonException wrapped = JacksonException.wrapWithPath(src, this, "value");
        assertEquals("Wrapper message", wrapped.getOriginalMessage());
        assertSame(src, wrapped.getCause());

        src = new InvocationTargetException(null);
        wrapped = JacksonException.wrapWithPath(src, this, "value");
        assertEquals("(was java.lang.reflect.InvocationTargetException)", wrapped.getOriginalMessage());
        assertSame(src, wrapped.getCause());
    }

    @Test
    void prependsPathToExistingJacksonException()
    {
        JacksonException src = new StreamReadException("Bad value");
        src.prependPath(this, "inner");

        JacksonException wrapped = JacksonException.wrapWithPath(src, this, "outer");

        assertSame(src, wrapped);
        assertEquals("Bad value", wrapped.getOriginalMessage());
        assertEquals(2, wrapped.getPath().size());
        assertEquals("outer", wrapped.getPath().get(0).getPropertyName());
        assertEquals("inner", wrapped.getPath().get(1).getPropertyName());
    }
}
