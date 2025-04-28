package tools.jackson.core.unittest.jsonptr;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonPointer;
import tools.jackson.core.unittest.JacksonCoreTestBase;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPointer1425Test extends JacksonCoreTestBase
{
    // [core#1425]
    @Test
    public void test1425Basic() {
        JsonPointer ptr = JsonPointer.compile("/a/b/0/qwerty");
        JsonPointer head = ptr.head();
        assertEquals("/a/b/0", head.toString());
        head = head.head(); // Exception happens here
        assertEquals("/a/b", head.toString());
        head = head.head();
        assertEquals("/a", head.toString());
        head = head.head();
        assertEquals("", head.toString());
    }

    // [core#1425]
    @Test
    public void test1425Variations() {
        JsonPointer ptr = JsonPointer.compile("/a/b/0/qwerty");
        JsonPointer tail = ptr.tail();

        assertEquals("/b/0/qwerty", tail.toString());
        JsonPointer head = tail.head();
        assertEquals("/b/0", head.toString());
        head = head.head();
        assertEquals("/b", head.toString());
        head = head.head();
        assertEquals("", head.toString());
    }
}
