package tools.jackson.core.unittest.util;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.core.io.ContentReference;
import tools.jackson.core.json.DupDetector;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.SimpleStreamReadContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimpleStreamReadContext}.
 */
class SimpleStreamReadContextTest extends JacksonCoreTestBase
{
    @Test
    void createRootContext()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);

        assertTrue(root.inRoot());
        assertEquals("root", root.typeDesc());
        assertNull(root.getParent());
        assertEquals(0, root.getNestingDepth());
        assertEquals(0, root.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1
        assertNull(root.currentName());
        assertFalse(root.hasCurrentName());
        assertNull(root.currentValue());
    }

    @Test
    void createRootContextWithLineAndColumn()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(5, 10, null);

        assertTrue(root.inRoot());
        assertNull(root.getParent());

        ContentReference srcRef = ContentReference.unknown();
        TokenStreamLocation loc = root.startLocation(srcRef);
        assertEquals(5, loc.getLineNr());
        assertEquals(10, loc.getColumnNr());
        assertEquals(-1L, loc.getByteOffset());
    }

    @Test
    void createRootContextWithDupDetector()
    {
        DupDetector dups = DupDetector.rootDetector((JsonParser) null);
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(1, 0, dups);

        assertNotNull(root.getDupDetector());
        assertSame(dups, root.getDupDetector());
    }

    @Test
    void createChildArrayContext()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        SimpleStreamReadContext array = root.createChildArrayContext(2, 5);

        assertTrue(array.inArray());
        assertEquals("Array", array.typeDesc());
        assertSame(root, array.getParent());
        assertEquals(1, array.getNestingDepth());
        assertEquals(0, array.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1

        ContentReference srcRef = ContentReference.unknown();
        TokenStreamLocation loc = array.startLocation(srcRef);
        assertEquals(2, loc.getLineNr());
        assertEquals(5, loc.getColumnNr());
    }

    @Test
    void createChildObjectContext()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        SimpleStreamReadContext object = root.createChildObjectContext(3, 7);

        assertTrue(object.inObject());
        assertEquals("Object", object.typeDesc());
        assertSame(root, object.getParent());
        assertEquals(1, object.getNestingDepth());
        assertEquals(0, object.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1

        ContentReference srcRef = ContentReference.unknown();
        TokenStreamLocation loc = object.startLocation(srcRef);
        assertEquals(3, loc.getLineNr());
        assertEquals(7, loc.getColumnNr());
    }

    @Test
    void childContextRecycling()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);

        // First child creation
        SimpleStreamReadContext array1 = root.createChildArrayContext(1, 0);
        assertNotNull(array1);

        // Simulate closing the array by going back to parent
        SimpleStreamReadContext parent1 = array1.clearAndGetParent();
        assertSame(root, parent1);

        // Second child creation should reuse the same instance
        SimpleStreamReadContext array2 = root.createChildArrayContext(2, 0);
        assertSame(array1, array2, "Context should be recycled");
        assertTrue(array2.inArray());
        assertEquals(0, array2.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1

        // Verify reset worked correctly
        ContentReference srcRef = ContentReference.unknown();
        TokenStreamLocation loc = array2.startLocation(srcRef);
        assertEquals(2, loc.getLineNr());
    }

    @Test
    void valueRead()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);

        assertEquals(0, root.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1

        int index1 = root.valueRead();
        assertEquals(0, index1);
        assertEquals(0, root.getCurrentIndex());

        int index2 = root.valueRead();
        assertEquals(1, index2);
        assertEquals(1, root.getCurrentIndex());
    }

    @Test
    void setCurrentName() throws Exception
    {
        SimpleStreamReadContext ctx = SimpleStreamReadContext.createRootContext(null);

        assertNull(ctx.currentName());
        assertFalse(ctx.hasCurrentName());

        ctx.setCurrentName("field1");
        assertEquals("field1", ctx.currentName());
        assertTrue(ctx.hasCurrentName());

        ctx.setCurrentName("field2");
        assertEquals("field2", ctx.currentName());

        ctx.setCurrentName(null);
        assertNull(ctx.currentName());
        assertFalse(ctx.hasCurrentName());
    }

    @Test
    void duplicateDetection() throws Exception
    {
        DupDetector dups = DupDetector.rootDetector((JsonParser) null);
        SimpleStreamReadContext ctx = SimpleStreamReadContext.createRootContext(1, 0, dups);

        ctx.setCurrentName("field1");
        assertEquals("field1", ctx.currentName());

        // Setting the same name again should trigger duplicate detection
        try {
            ctx.setCurrentName("field1");
            fail("Should have thrown StreamReadException for duplicate field");
        } catch (StreamReadException e) {
            verifyException(e, "Duplicate Object property");
            verifyException(e, "field1");
        }
    }

    @Test
    void duplicateDetectionInChildContext() throws Exception
    {
        DupDetector dups = DupDetector.rootDetector((JsonParser) null);
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(1, 0, dups);
        SimpleStreamReadContext object = root.createChildObjectContext(2, 0);

        // Child should have its own DupDetector
        assertNotNull(object.getDupDetector());
        assertNotSame(dups, object.getDupDetector());

        object.setCurrentName("prop1");

        try {
            object.setCurrentName("prop1");
            fail("Should have thrown StreamReadException for duplicate property");
        } catch (StreamReadException e) {
            verifyException(e, "Duplicate Object property");
            verifyException(e, "prop1");
        }
    }

    @Test
    void currentValue()
    {
        SimpleStreamReadContext ctx = SimpleStreamReadContext.createRootContext(null);

        assertNull(ctx.currentValue());

        Object value = new Object();
        ctx.assignCurrentValue(value);
        assertSame(value, ctx.currentValue());

        ctx.assignCurrentValue(null);
        assertNull(ctx.currentValue());
    }

    @Test
    void clearAndGetParent()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        SimpleStreamReadContext array = root.createChildArrayContext(1, 0);

        Object value = new Object();
        array.assignCurrentValue(value);
        assertSame(value, array.currentValue());

        SimpleStreamReadContext parent = array.clearAndGetParent();
        assertSame(root, parent);
        assertNull(array.currentValue(), "Value should be cleared");
    }

    @Test
    void nestedContexts()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        assertEquals(0, root.getNestingDepth());

        SimpleStreamReadContext array = root.createChildArrayContext(1, 0);
        assertEquals(1, array.getNestingDepth());
        assertSame(root, array.getParent());

        SimpleStreamReadContext object = array.createChildObjectContext(2, 0);
        assertEquals(2, object.getNestingDepth());
        assertSame(array, object.getParent());

        SimpleStreamReadContext innerArray = object.createChildArrayContext(3, 0);
        assertEquals(3, innerArray.getNestingDepth());
        assertSame(object, innerArray.getParent());
    }

    @Test
    void deprecatedConstructor()
    {
        // Test the deprecated constructor without nestingDepth parameter
        @SuppressWarnings("deprecation")
        SimpleStreamReadContext ctx = new SimpleStreamReadContext(
                SimpleStreamReadContext.TYPE_OBJECT,
                null,
                null,
                1,
                0
        );

        assertTrue(ctx.inObject());
        assertNull(ctx.getParent());
        assertEquals(-1, ctx.getNestingDepth()); // Should be -1 when using deprecated constructor
    }

    @Test
    void contextTypeDescriptions()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        assertEquals("root", root.typeDesc());

        SimpleStreamReadContext array = root.createChildArrayContext(1, 0);
        assertEquals("Array", array.typeDesc());

        SimpleStreamReadContext object = root.createChildObjectContext(1, 0);
        assertEquals("Object", object.typeDesc());
    }

    @Test
    void contextWithoutDupDetector()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        assertNull(root.getDupDetector());

        // Should allow duplicate names when no DupDetector is set
        root.setCurrentName("field");
        root.setCurrentName("field"); // Should not throw
    }

    @Test
    void childContextWithNoDupDetectorInParent()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);
        SimpleStreamReadContext child = root.createChildObjectContext(1, 0);

        assertNull(child.getDupDetector());
    }

    @Test
    void childContextRecyclingResetsState()
    {
        SimpleStreamReadContext root = SimpleStreamReadContext.createRootContext(null);

        SimpleStreamReadContext array1 = root.createChildArrayContext(1, 0);
        array1.setCurrentName("name1");
        array1.assignCurrentValue("value1");
        array1.valueRead();

        assertEquals("name1", array1.currentName());
        assertEquals("value1", array1.currentValue());
        assertEquals(0, array1.getCurrentIndex());

        // Go back to parent
        array1.clearAndGetParent();

        // Create new child (recycling previous one)
        SimpleStreamReadContext array2 = root.createChildArrayContext(2, 0);

        // State should be reset
        assertNull(array2.currentName());
        assertNull(array2.currentValue());
        assertEquals(0, array2.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1
    }
}
