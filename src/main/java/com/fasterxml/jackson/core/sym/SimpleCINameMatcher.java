package com.fasterxml.jackson.core.sym;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.core.util.Named;

public class SimpleCINameMatcher extends FieldNameMatcher
{
    private final HashMap<String,Integer> _fields;

    protected SimpleCINameMatcher(HashMap<String,Integer> f) {
        _fields = f;
    }

    public static SimpleCINameMatcher construct(List<Named> fields) {
        HashMap<String,Integer> toMatch = new HashMap<>();
        for (int i = 0; i < fields.size(); ++i) {
            String key = fields.get(i).getName().toLowerCase();
            toMatch.put(key, i);
        }
        return new SimpleCINameMatcher(toMatch);
    }

    @Override
    public int matchName(String name) {
        name = name.toLowerCase();
        Object o = _fields.get(name);
        if (o == null) {
            return MATCH_UNKNOWN_NAME;
        }
        return ((Integer) o).intValue();
    }
}
