package com.fasterxml.jackson.core.sym;

/**
 * Intermediate base class for matchers that use hash-array based approach
 * with Strings.
 */
public abstract class HashedMatcherBase
    extends FieldNameMatcher
{
    private static final long serialVersionUID = 1L;

    /**
     * Mask used to get index from raw hash code, within hash area.
     */
    protected final int _mask;

    // // // Main hash area (ints) along with Strings it maps (sparse)
    
    protected final int[] _offsets;
    protected final String[] _names;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */
    
    protected HashedMatcherBase(String[] names, int[] offsets, int mask,
            FieldNameMatcher backup, String[] nameLookup)
    {
        super(backup, nameLookup);
        _names = names;
        _offsets = offsets;
        _mask = mask;
    }

    protected HashedMatcherBase(HashedMatcherBase base, String[] nameLookup) {
        this(base._names, base._offsets, base._mask, base._backupMatcher, nameLookup);
    }

    protected HashedMatcherBase(HashedMatcherBase base, FieldNameMatcher fallback) {
        this(base._names, base._offsets, base._mask, fallback, base._nameLookup);
    }

    /*
    /**********************************************************************
    /* API: lookup by String
    /**********************************************************************
     */

    @Override
    public final int matchName(String toMatch)
    {
        // Logic here is that it is expected most names to match are intern()ed
        // anyway; as are (typically) contents. So identity check is likely to
        // work, just not guaranteed. So do fast checks for primary, secondary here
        
        int ix = _hash(toMatch.hashCode(), _mask);
        if (_names[ix] == toMatch) {
            return _offsets[ix];
        }
        // check secondary slot
        int ix2 = (_mask + 1) + (ix >> 1);
        if (_names[ix2] == toMatch) {
            return _offsets[ix2];
        }
        return _matchName2(toMatch, ix, ix2);
    }

    private final int _matchName2(String toMatch, int ix, int ix2)
    {
        String name = _names[ix];
        if (toMatch.equals(name)) {
            return _offsets[ix];
        }
        if (name != null) {
            name = _names[ix2];
            if (toMatch.equals(name)) {
                return _offsets[ix2];
            }
            if (name != null) {
                return _matchSpill(toMatch);
            }
        }
        return matchSecondary(toMatch);
    }

    protected int _matchSpill(String toMatch) {
        int ix = (_mask+1);
        ix += (ix>>1);

        for (int end = _names.length; ix < end; ++ix) {
            String name = _names[ix];

            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            if (name == null) {
                break;
            }
        }
        return matchSecondary(toMatch);
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    public int spillCount() {
        int spillStart = (_mask+1) + ((_mask+1) >> 1);
        int count = 0;
        for (int i = spillStart; i < _names.length; ++i) {
            if (_names[i] != null) {
                ++count;
            }
        }
        return count;
    }

    public int secondaryCount() {
        int spillStart = (_mask+1) + ((_mask+1) >> 1);
        int count = 0;
        for (int i = _mask+1; i < spillStart; ++i) {
            if (_names[i] != null) {
                ++count;
            }
        }
        return count;
    }
}
