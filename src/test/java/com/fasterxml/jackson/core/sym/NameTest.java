package com.fasterxml.jackson.core.sym;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for class {@link Name}.
 *
 * @date 16.07.2017
 * @see Name
 **/
public class NameTest {


    @Test
    public void testEqualsReturningTrue() {

        Name name = Name1.getEmptyName();

        assertTrue(name.equals(name));

    }


    @Test
    public void testEqualsReturningFalse() {

        Name name = new Name2("", (-888), (-2073), (-2073));

        assertFalse(name.equals("on<bLLJ"));

    }


    @Test
    public void testToString() {

        Name name = Name1.getEmptyName();

        assertEquals("", name.toString());

    }


    @Test
    public void testGetName() {

        Name name = Name1.getEmptyName();

        assertEquals("", name.getName());

    }


}