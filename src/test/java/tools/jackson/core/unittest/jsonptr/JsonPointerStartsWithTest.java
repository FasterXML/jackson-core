package tools.jackson.core.unittest.jsonptr;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import tools.jackson.core.JsonPointer;

import static org.junit.jupiter.api.Assertions.*;

public class JsonPointerStartsWithTest {

    @Test
    @DisplayName("Should return true when comparing against the EMPTY pointer")
    public void testStartsWithEmpty() {
        JsonPointer ptr = JsonPointer.compile("/a/b/c");
        assertTrue(ptr.startsWith(JsonPointer.empty()), "Any pointer should start with the empty pointer");
        assertTrue(JsonPointer.empty().startsWith(JsonPointer.empty()), "Empty pointer should start with empty pointer");
    }

    @Test
    @DisplayName("Should return false when the other pointer is null")
    public void testStartsWithNull() {
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
    @DisplayName("Should return true for valid prefixes")
    public void testStartsWithValidPrefix(String full, String prefix) {
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
        "/a, /b"
    })
    @DisplayName("Should return false for invalid prefixes")
    public void testStartsWithInvalidPrefix(String full, String prefix) {
        JsonPointer fullPtr = JsonPointer.compile(full);
        JsonPointer prefixPtr = JsonPointer.compile(prefix);
        assertFalse(fullPtr.startsWith(prefixPtr),
            String.format("Pointer '%s' should NOT start with '%s'", full, prefix));
    }

    @Test
    @DisplayName("Should handle complex escaped characters correctly")
    public void testStartsWithEscaped() {
        JsonPointer fullPtr = JsonPointer.compile("/~1part1/~0part2/end");

        assertTrue(fullPtr.startsWith(JsonPointer.compile("/~1part1")));
        assertTrue(fullPtr.startsWith(JsonPointer.compile("/~1part1/~0part2")));

        // Mismatch in escaping
        assertFalse(fullPtr.startsWith(JsonPointer.compile("/part1")));
    }

    @Test
    @DisplayName("Should distinguish between property names and array indices")
    public void testStartsWithTypeSafety() {
        // "/0" is index 0, "/00" is property name "00"
        JsonPointer indexPtr = JsonPointer.compile("/0/next");
        JsonPointer propPtr = JsonPointer.compile("/00/next");

        assertTrue(indexPtr.startsWith(JsonPointer.compile("/0")));
        assertFalse(indexPtr.startsWith(JsonPointer.compile("/00")));

        assertTrue(propPtr.startsWith(JsonPointer.compile("/00")));
        assertFalse(propPtr.startsWith(JsonPointer.compile("/0")));
    }

    @Test
    @DisplayName("Should return false if prefix is longer than the pointer")
    public void testStartsWithLongerPrefix() {
        JsonPointer ptr = JsonPointer.compile("/a/b");
        JsonPointer longer = JsonPointer.compile("/a/b/c");
        assertFalse(ptr.startsWith(longer), "Pointer should not start with a longer pointer");
    }
}
