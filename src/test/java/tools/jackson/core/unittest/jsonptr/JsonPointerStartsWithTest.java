package tools.jackson.core.unittest.jsonptr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonPointer;
import tools.jackson.core.JsonToken;
import tools.jackson.core.ObjectReadContext;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.unittest.*;

import static org.junit.jupiter.api.Assertions.*;

// Tests for [core#1637]: `JsonPointer.startsWith(JsonPointer)`
class JsonPointerStartsWithTest extends JacksonCoreTestBase
{
    private final JsonFactory JSON_F = new JsonFactory();

    @Test
    void startsWithEmpty() {
        JsonPointer ptr = JsonPointer.compile("/a/b/c");
        assertTrue(ptr.startsWith(JsonPointer.empty()), "Any pointer should start with the empty pointer");
        assertTrue(JsonPointer.empty().startsWith(JsonPointer.empty()), "Empty pointer should start with empty pointer");
    }

    @Test
    void startsWithNull() {
        JsonPointer ptr = JsonPointer.compile("/a/b/c");
        assertFalse(ptr.startsWith(null), "Should return false for null input");
    }

    @ParameterizedTest
    @CsvSource({
        "/a/b/c, /a",
        "/a/b/c, /a/b",
        "/a/b/c, /a/b/c",
        "/1/2/3, /1",
        "/1/2/3, /1/2",
        "/prop/0/leaf, /prop/0",
        "/~1slash/~0tilde, /~1slash"
    })
    void startsWithValidPrefix(String full, String prefix) {
        JsonPointer fullPtr = JsonPointer.compile(full);
        JsonPointer prefixPtr = JsonPointer.compile(prefix);
        assertTrue(fullPtr.startsWith(prefixPtr),
            String.format("Pointer '%s' should start with '%s'", full, prefix));
    }

    @ParameterizedTest
    @CsvSource({
        "/a/b/c, /b",
        "/a/b/c, /a/c",
        "/a/b/c, /a/b/c/d",
        "/1/2/3, /2",
        "/1/2/3, /1/3",
        "/1/2/3, /1/2/3/4",
        "/prop/0, /prop/1",
        "/a, /b",
        // Matching is by segment, not by raw String prefix:
        "/abc, /ab",
        "/abc/d, /ab",
        "/abc/d, /abc/d/e",
        "/12/3, /1"
    })
    void startsWithInvalidPrefix(String full, String prefix) {
        JsonPointer fullPtr = JsonPointer.compile(full);
        JsonPointer prefixPtr = JsonPointer.compile(prefix);
        assertFalse(fullPtr.startsWith(prefixPtr),
            String.format("Pointer '%s' should NOT start with '%s'", full, prefix));
    }

    @Test
    void startsWithEscaped() {
        JsonPointer fullPtr = JsonPointer.compile("/~1part1/~0part2/end");

        assertTrue(fullPtr.startsWith(JsonPointer.compile("/~1part1")));
        assertTrue(fullPtr.startsWith(JsonPointer.compile("/~1part1/~0part2")));

        // Mismatch in escaping
        assertFalse(fullPtr.startsWith(JsonPointer.compile("/part1")));
        // Escaped slash is part of one segment, not a segment separator
        assertFalse(fullPtr.startsWith(JsonPointer.compile("/")));
    }

    // Matching is on decoded segments, so equal-decoding pointers match even
    // when their String representations (and hence `equals()`) differ
    @Test
    void startsWithNotSameAsEquals() {
        JsonPointer valid = JsonPointer.compile("/a~0b");
        // "~b" is not a valid escape and is decoded as-is; same segment as above
        JsonPointer invalidEsc = JsonPointer.compile("/a~b");

        assertEquals("a~b", valid.getMatchingProperty());
        assertEquals("a~b", invalidEsc.getMatchingProperty());
        assertNotEquals(valid, invalidEsc);

        assertTrue(valid.startsWith(invalidEsc));
        assertTrue(invalidEsc.startsWith(valid));
    }

    @Test
    void startsWithTypeSafety() {
        // "/0" is index 0, "/00" is property name "00"
        JsonPointer indexPtr = JsonPointer.compile("/0/next");
        JsonPointer propPtr = JsonPointer.compile("/00/next");

        assertTrue(indexPtr.startsWith(JsonPointer.compile("/0")));
        assertFalse(indexPtr.startsWith(JsonPointer.compile("/00")));

        assertTrue(propPtr.startsWith(JsonPointer.compile("/00")));
        assertFalse(propPtr.startsWith(JsonPointer.compile("/0")));
    }

    @Test
    void startsWithLongerPrefix() {
        JsonPointer ptr = JsonPointer.compile("/a/b");
        JsonPointer longer = JsonPointer.compile("/a/b/c");
        assertFalse(ptr.startsWith(longer), "Pointer should not start with a longer pointer");
    }

    // [core#788]: empty String ("") is a valid property name, distinct from EMPTY pointer
    @Test
    void startsWithEmptyStringProperty() {
        // "/" is a single segment matching property with empty-String name
        JsonPointer emptyProp = JsonPointer.compile("/");
        JsonPointer emptyPropChild = JsonPointer.compile("//leaf");

        assertTrue(emptyProp.startsWith(JsonPointer.compile("/")));
        assertTrue(emptyPropChild.startsWith(JsonPointer.compile("/")));
        assertTrue(emptyPropChild.startsWith(JsonPointer.compile("//leaf")));

        // empty-String property "/" is NOT the same as the EMPTY (root) pointer as a prefix source:
        assertFalse(JsonPointer.empty().startsWith(emptyProp));
        // ...but every pointer (incl. "/") starts with the EMPTY pointer
        assertTrue(emptyProp.startsWith(JsonPointer.empty()));
    }

    // Pointers constructed by mutant factories build fresh segment chains: verify
    // those work as both receiver and argument
    @Test
    void startsWithDerivedPointers() {
        final JsonPointer full = JsonPointer.compile("/a/b/c/d");

        JsonPointer head = full.head(); // "/a/b/c"
        assertEquals("/a/b/c", head.toString());
        assertTrue(full.startsWith(head));
        assertTrue(head.startsWith(head.head()));
        assertFalse(head.startsWith(full));

        JsonPointer tail = full.tail(); // "/b/c/d"
        assertTrue(tail.startsWith(JsonPointer.compile("/b/c")));
        assertFalse(tail.startsWith(JsonPointer.compile("/a")));

        JsonPointer appended = JsonPointer.compile("/a/b").append(JsonPointer.compile("/c"));
        assertTrue(appended.startsWith(JsonPointer.compile("/a/b")));
        assertTrue(full.startsWith(appended));

        JsonPointer built = JsonPointer.compile("/a").appendIndex(3).appendProperty("x/y");
        assertTrue(built.startsWith(JsonPointer.compile("/a/3")));
        assertTrue(built.startsWith(JsonPointer.compile("/a").appendIndex(3)));
        assertTrue(built.startsWith(built));
        // "/03" is a property name, not index 3
        assertFalse(built.startsWith(JsonPointer.compile("/a/03")));
    }

    // Pointers from parsing context are built via `JsonPointer.forPath()`, another
    // separate construction path
    @Test
    void startsWithPointerFromContext() throws Exception
    {
        final String DOC = a2q("{'ob':{'array':[1,{'leaf':true}]}}");

        try (JsonParser p = JSON_F.createParser(ObjectReadContext.empty(), DOC)) {
            while (p.nextToken() != null) {
                if (p.currentToken() == JsonToken.VALUE_TRUE) {
                    break;
                }
            }
            JsonPointer ptr = p.streamReadContext().pathAsPointer();
            assertEquals("/ob/array/1/leaf", ptr.toString());

            assertTrue(ptr.startsWith(JsonPointer.empty()));
            assertTrue(ptr.startsWith(JsonPointer.compile("/ob")));
            assertTrue(ptr.startsWith(JsonPointer.compile("/ob/array/1")));
            assertTrue(ptr.startsWith(ptr));
            assertFalse(ptr.startsWith(JsonPointer.compile("/ob/array/0")));
            // and works as prefix argument as well:
            assertTrue(JsonPointer.compile("/ob/array/1/leaf/deeper").startsWith(ptr));
        }
    }
}
