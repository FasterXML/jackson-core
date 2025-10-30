package com.fasterxml.jackson.core.jsonptr;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JUnit5TestBase;
import com.fasterxml.jackson.core.JsonPointer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonPointer1425Test extends JUnit5TestBase
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
