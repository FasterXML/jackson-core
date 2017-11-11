package com.fasterxml.jackson.core.sym;

import java.util.*;

import com.fasterxml.jackson.core.util.Named;

public class SimpleCSNameMatcher extends FieldNameMatcher
{
    private final HashMap<String,Integer> _fields;

    protected SimpleCSNameMatcher(HashMap<String,Integer> f) {
        _fields = f;
    }

    public static SimpleCSNameMatcher construct(List<Named> fields) {
        HashMap<String,Integer> toMatch = new HashMap<>();
        for (int i = 0; i < fields.size(); ++i) {
            toMatch.put(fields.get(i).getName(), i);
        }
        return new SimpleCSNameMatcher(toMatch);
    }

    @Override
    public int matchName(String name) {
        Object o = _fields.get(name);
        if (o == null) {
            return MATCH_UNKNOWN_NAME;
        }
        return ((Integer) o).intValue();
    }
}
