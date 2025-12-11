package tools.jackson.core.unittest.util;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.json.DupDetector;
import tools.jackson.core.unittest.JacksonCoreTestBase;
import tools.jackson.core.util.SimpleStreamWriteContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SimpleStreamWriteContext}.
 */
class SimpleStreamWriteContextTest extends JacksonCoreTestBase
{
    @Test
    void createRootContext()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);

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
    void createRootContextWithDupDetector()
    {
        DupDetector dups = DupDetector.rootDetector((JsonGenerator) null);
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(dups);

        assertNotNull(root.getDupDetector());
        assertSame(dups, root.getDupDetector());
    }

    @Test
    void withDupDetector()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        assertNull(root.getDupDetector());

        DupDetector dups = DupDetector.rootDetector((JsonGenerator) null);
        SimpleStreamWriteContext result = root.withDupDetector(dups);

        assertSame(root, result, "Should return same instance");
        assertSame(dups, root.getDupDetector());
    }

    @Test
    void createChildArrayContext()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        Object arrayValue = new Object();
        SimpleStreamWriteContext array = root.createChildArrayContext(arrayValue);

        assertTrue(array.inArray());
        assertEquals("Array", array.typeDesc());
        assertSame(root, array.getParent());
        assertEquals(1, array.getNestingDepth());
        assertEquals(0, array.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1
        assertSame(arrayValue, array.currentValue());
    }

    @Test
    void createChildObjectContext()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        Object objectValue = new Object();
        SimpleStreamWriteContext object = root.createChildObjectContext(objectValue);

        assertTrue(object.inObject());
        assertEquals("Object", object.typeDesc());
        assertSame(root, object.getParent());
        assertEquals(1, object.getNestingDepth());
        assertEquals(0, object.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1
        assertSame(objectValue, object.currentValue());
    }

    @Test
    void childContextRecycling()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);

        // First child creation
        SimpleStreamWriteContext array1 = root.createChildArrayContext("value1");
        assertNotNull(array1);
        assertEquals("value1", array1.currentValue());

        // Simulate closing the array by going back to parent
        SimpleStreamWriteContext parent1 = array1.clearAndGetParent();
        assertSame(root, parent1);

        // Second child creation should reuse the same instance
        SimpleStreamWriteContext array2 = root.createChildArrayContext("value2");
        assertSame(array1, array2, "Context should be recycled");
        assertTrue(array2.inArray());
        assertEquals(0, array2.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1
        assertEquals("value2", array2.currentValue());
    }

    @Test
    void writeNameInObjectContext() throws Exception
    {
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(null)
                .createChildObjectContext(null);

        assertTrue(object.writeName("field1"));
        assertEquals("field1", object.currentName());
        assertTrue(object.hasCurrentName());
    }

    @Test
    void writeNameNotAllowedInArrayContext() throws Exception
    {
        SimpleStreamWriteContext array = SimpleStreamWriteContext.createRootContext(null)
                .createChildArrayContext(null);

        assertFalse(array.writeName("field1"), "writeName should return false in array context");
        assertNull(array.currentName());
        assertFalse(array.hasCurrentName());
    }

    @Test
    void writeNameNotAllowedInRootContext() throws Exception
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);

        assertFalse(root.writeName("field1"), "writeName should return false in root context");
    }

    @Test
    void writeNameNotAllowedWhenAlreadyHavePropertyId() throws Exception
    {
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(null)
                .createChildObjectContext(null);

        assertTrue(object.writeName("field1"));
        assertFalse(object.writeName("field2"), "writeName should return false when property name already set");
        assertEquals("field1", object.currentName(), "Name should not change");
    }

    @Test
    void writeValue() throws Exception
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);

        assertEquals(0, root.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1

        assertTrue(root.writeValue());
        assertEquals(0, root.getCurrentIndex());

        assertTrue(root.writeValue());
        assertEquals(1, root.getCurrentIndex());
    }

    @Test
    void writeValueInArrayContext() throws Exception
    {
        SimpleStreamWriteContext array = SimpleStreamWriteContext.createRootContext(null)
                .createChildArrayContext(null);

        assertTrue(array.writeValue());
        assertEquals(0, array.getCurrentIndex());

        assertTrue(array.writeValue());
        assertEquals(1, array.getCurrentIndex());
    }

    @Test
    void writeValueInObjectContext() throws Exception
    {
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(null)
                .createChildObjectContext(null);

        // Cannot write value without property name first
        assertFalse(object.writeValue(), "writeValue should return false without property name");
        assertEquals(0, object.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1

        // Write property name
        assertTrue(object.writeName("field1"));

        // Now can write value
        assertTrue(object.writeValue());
        assertEquals(0, object.getCurrentIndex());
        assertFalse(object.hasCurrentName(), "Property ID flag should be cleared after value");

        // Need another property name before next value
        assertFalse(object.writeValue());
        assertEquals(0, object.getCurrentIndex(), "Index should not change");

        assertTrue(object.writeName("field2"));
        assertTrue(object.writeValue());
        assertEquals(1, object.getCurrentIndex());
    }

    @Test
    void duplicateDetection() throws Exception
    {
        DupDetector dups = DupDetector.rootDetector((JsonGenerator) null);
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(dups)
                .createChildObjectContext(null);

        object.writeName("field1");
        object.writeValue();

        // Try to write the same property name again
        try {
            object.writeName("field1");
            fail("Should have thrown StreamWriteException for duplicate field");
        } catch (StreamWriteException e) {
            verifyException(e, "Duplicate Object property");
            verifyException(e, "field1");
        }
    }

    @Test
    void currentValue()
    {
        SimpleStreamWriteContext ctx = SimpleStreamWriteContext.createRootContext(null);

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
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        SimpleStreamWriteContext array = root.createChildArrayContext("arrayValue");

        assertEquals("arrayValue", array.currentValue());

        SimpleStreamWriteContext parent = array.clearAndGetParent();
        assertSame(root, parent);
        assertNull(array.currentValue(), "Value should be cleared");
    }

    @Test
    void nestedContexts()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        assertEquals(0, root.getNestingDepth());

        SimpleStreamWriteContext array = root.createChildArrayContext(null);
        assertEquals(1, array.getNestingDepth());
        assertSame(root, array.getParent());

        SimpleStreamWriteContext object = array.createChildObjectContext(null);
        assertEquals(2, object.getNestingDepth());
        assertSame(array, object.getParent());

        SimpleStreamWriteContext innerArray = object.createChildArrayContext(null);
        assertEquals(3, innerArray.getNestingDepth());
        assertSame(object, innerArray.getParent());
    }

    @Test
    void contextTypeDescriptions()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        assertEquals("root", root.typeDesc());

        SimpleStreamWriteContext array = root.createChildArrayContext(null);
        assertEquals("Array", array.typeDesc());

        SimpleStreamWriteContext object = root.createChildObjectContext(null);
        assertEquals("Object", object.typeDesc());
    }

    @Test
    void contextWithoutDupDetector() throws Exception
    {
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(null)
                .createChildObjectContext(null);
        assertNull(object.getDupDetector());

        // Should allow duplicate names when no DupDetector is set
        object.writeName("field");
        object.writeValue();
        object.writeName("field"); // Should not throw
        object.writeValue();
    }

    @Test
    void childContextWithNoDupDetectorInParent()
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);
        SimpleStreamWriteContext child = root.createChildObjectContext(null);

        assertNull(child.getDupDetector());
    }

    @Test
    void childContextWithDupDetectorInParent()
    {
        DupDetector dups = DupDetector.rootDetector((JsonGenerator) null);
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(dups);
        SimpleStreamWriteContext child = root.createChildObjectContext(null);

        assertNotNull(child.getDupDetector());
        assertNotSame(dups, child.getDupDetector());
    }

    @Test
    void childContextRecyclingResetsState() throws Exception
    {
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(null);

        SimpleStreamWriteContext object1 = root.createChildObjectContext("value1");
        object1.writeName("name1");
        object1.writeValue();

        assertEquals("name1", object1.currentName());
        assertEquals("value1", object1.currentValue());
        assertFalse(object1.hasCurrentName()); // hasCurrentName is false after writeValue()
        assertEquals(0, object1.getCurrentIndex());

        // Go back to parent
        object1.clearAndGetParent();

        // Create new child (recycling previous one)
        SimpleStreamWriteContext object2 = root.createChildObjectContext("value2");

        // State should be reset
        assertNull(object2.currentName());
        assertEquals("value2", object2.currentValue());
        assertFalse(object2.hasCurrentName());
        assertEquals(0, object2.getCurrentIndex()); // getCurrentIndex() returns 0 when _index is -1
    }

    @Test
    void hasCurrentNameBehavior() throws Exception
    {
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(null)
                .createChildObjectContext(null);

        assertFalse(object.hasCurrentName());

        object.writeName("field1");
        assertTrue(object.hasCurrentName());

        object.writeValue();
        assertFalse(object.hasCurrentName(), "hasCurrentName should be false after writing value");

        // Note: currentName() still returns the name even after writeValue()
        assertEquals("field1", object.currentName());
    }

    @Test
    void currentNameAccessibleAfterNewScope() throws Exception
    {
        SimpleStreamWriteContext object = SimpleStreamWriteContext.createRootContext(null)
                .createChildObjectContext(null);

        object.writeName("outerField");
        assertEquals("outerField", object.currentName());

        // Start a nested array
        SimpleStreamWriteContext array = object.createChildArrayContext(null);

        // Parent's current name should still be accessible from parent context
        assertEquals("outerField", object.currentName());
    }

    @Test
    void dupDetectorResetOnRecycle() throws Exception
    {
        DupDetector dups = DupDetector.rootDetector((JsonGenerator) null);
        SimpleStreamWriteContext root = SimpleStreamWriteContext.createRootContext(dups);

        SimpleStreamWriteContext object1 = root.createChildObjectContext(null);
        object1.writeName("field1");
        object1.writeValue();

        // Return to parent
        object1.clearAndGetParent();

        // Create new child (recycling)
        SimpleStreamWriteContext object2 = root.createChildObjectContext(null);

        // Should be able to write "field1" again because DupDetector was reset
        assertTrue(object2.writeName("field1"));
        object2.writeValue();
    }
}
