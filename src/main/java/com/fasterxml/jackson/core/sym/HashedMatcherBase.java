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

    final int BOGUS_PADDING = 0; // just for aligning

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
    public final int matchAnyName(String toMatch) {
        int ix = _hash(toMatch.hashCode(), _mask);
        String name = _names[ix];
        if (toMatch == name) {
            return _offsets[ix];
        }
        if (name != null) {
            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            // check secondary slot
            ix = (_mask + 1) + (ix >> 1);
            name = _names[ix];
            if (toMatch.equals(name)) {
                return _offsets[ix];
            }
            // or spill-over if need be
            if (name != null) {
                return _matchAnySpill(toMatch);
            }
        }
        return matchSecondary(toMatch);
    }

    private final int _matchAnySpill(String toMatch) {
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

    /**
     * Lookup method when caller guarantees that name to match has been
     * {@link String#intern}ed
     */
    @Override
    public final int matchInternedName(String toMatch) {
        int ix = _hash(toMatch.hashCode(), _mask);
        String name = _names[ix];
        if (name == toMatch) {
            return _offsets[ix];
        }
        if (name != null) {
            // check secondary slot
            ix = (_mask + 1) + (ix >> 1);
            name = _names[ix];
            if (name == toMatch) {
                return _offsets[ix];
            }
            // or spill-over if need be
            if (name != null) {
                return _matchInternedSpill(toMatch);
            }
        }
        return matchSecondary(toMatch);
    }

    private final int _matchInternedSpill(String toMatch) {
        int ix = (_mask+1);
        ix += (ix>>1);

        for (int end = _names.length; ix < end; ++ix) {
            String name = _names[ix];
            if (name == toMatch) {
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
