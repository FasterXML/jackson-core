package com.fasterxml.jackson.core.sym;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.sym.SimpleNameMatcher;
import com.fasterxml.jackson.core.util.Named;

/**
 * Simplified static symbol table used instead of global quad-based canonicalizer
 * when we have smaller set of symbols (like properties of a POJO class).
 *
 * @since 3.0
 */
public final class BinaryNameMatcher
    extends HashedMatcherBase
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    // Limit to 32k entries as well as 32k-int (i.e. 128kb) strings so that both
    // size and offset (in string table) can be encoded in a single int.

    public final static int MAX_ENTRIES = 0x7FFF;

    private final static int MAX_LENGTH_IN_QUADS = 0x7FFF;

    /*
    /**********************************************************
    /* First, main hash area info
    /**********************************************************
     */

    /**
     * Primary hash information area: consists of <code>2 * _hashSize</code>
     * entries of 16 bytes (4 ints), arranged in a cascading lookup
     * structure (details of which may be tweaked depending on expected rates
     * of collisions).
     */
    private int[] _hashArea;

    /**
     * Number of slots for primary entries within {@link #_hashArea}; which is
     * at most <code>1/8</code> of actual size of the underlying array (4-int slots,
     * primary covers only half of the area; plus, additional area for longer
     * symbols after hash area).
     */
    private int _hashSize;

    /**
     * Offset within {@link #_hashArea} where secondary entries start
     */
    private int _secondaryStart;

    /**
     * Offset within {@link #_hashArea} where tertiary entries start
     */
    private int _tertiaryStart;

    /**
     * Constant that determines size of buckets for tertiary entries:
     * <code>1 &lt;&lt; _tertiaryShift</code> is the size, and shift value
     * is also used for translating from primary offset into
     * tertiary bucket (shift right by <code>4 + _tertiaryShift</code>).
     *<p>
     * Default value is 2, for buckets of 4 slots; grows bigger with
     * bigger table sizes.
     */
    private int _tertiaryShift;

    /**
     * Total number of Strings in the symbol table
     */
    private int _count;

    /*
    /**********************************************************
    /* Then information on collisions etc
    /**********************************************************
     */

    /**
     * Pointer to the offset within spill-over area where there is room
     * for more spilled over entries (if any).
     * Spill over area is within fixed-size portion of {@link #_hashArea}.
     */
    private int _spilloverEnd;

    /**
     * Offset within {@link #_hashArea} that follows main slots and contains
     * quads for longer names (13 bytes or longer), and points to the
     * first available int that may be used for appending quads of the next
     * long name.
     * Note that long name area follows immediately after the fixed-size
     * main hash area ({@link #_hashArea}).
     */
    private int _longNameOffset;

    /*
    /**********************************************************
    /* Life-cycle: constructors
    /**********************************************************
     */

    /**
     * Constructor used for creating per-<code>JsonFactory</code> "root"
     * symbol tables: ones used for merging and sharing common symbols
     *
     * @param entryCount Number of Strings to contain
     * @param sz Size of logical hash area
     */
    private BinaryNameMatcher(SimpleNameMatcher matcher, String[] nameLookup, int hashSize)
    {
        super(matcher, nameLookup);
        _count = 0;
        _hashSize = hashSize; // 8x, 4 ints per entry for main area, then sec/ter and spill over
        _hashArea = new int[hashSize << 3];

        _secondaryStart = hashSize << 2; // right after primary area (at 50%)
        _tertiaryStart = _secondaryStart + (_secondaryStart >> 1); // right after secondary
        _tertiaryShift = _calcTertiaryShift(hashSize);
        _spilloverEnd = _hashArea.length - hashSize; // start AND end the same, at 7/8, initially
        _longNameOffset = _hashArea.length; // and start of long name area is at end of initial area (to be expanded)
    }

    static int _calcTertiaryShift(int primarySlots)
    {
        // first: we only get 1/4 of slots of primary, to divide
        int tertSlots = (primarySlots) >> 2;
        // default is for buckets of 4 slots (each 4 ints, i.e. 1 << 4)
        if (tertSlots < 64) return 4;
        // buckets of 8 slots (up to 256 == 32 x 8)
        if (tertSlots <= 256) return 5;
        // buckets of 16 slots (up to 1024 == 64 x 16)
        if (tertSlots <= 1024) return 6;
        // and biggest buckets have 32 slots
        return 7;
    }

    /*
    /**********************************************************
    /* Life-cycle: factory methods
    /**********************************************************
     */

    public static BinaryNameMatcher constructFrom(List<Named> fields,
            boolean alreadyInterned)
    {
        return construct(stringsFromNames(fields, alreadyInterned));
    }

    public static BinaryNameMatcher construct(List<String> symbols)
    {
        // Two-step process: since we need backup string-based lookup (when matching
        // current name, buffered etc etc), start with that
        return _construct(symbols, SimpleNameMatcher.construct(symbols));
    }

    public static BinaryNameMatcher constructCaseInsensitive(List<Named> fields,
            boolean alreadyInterned)
    {
        final List<String> names = FieldNameMatcher.stringsFromNames(fields, alreadyInterned);
        return _construct(names, SimpleNameMatcher.constructCaseInsensitive(names));
    }

    private static BinaryNameMatcher _construct(List<String> symbols,
            SimpleNameMatcher base)
    {
        int sz = _findSize(symbols.size());
        String[] lookup = symbols.toArray(new String[symbols.size()]);
        BinaryNameMatcher matcher = new BinaryNameMatcher(base, lookup, sz);
        for (String name : symbols) {
            matcher.addName(name);
        }
        return matcher;
    }

    /*
    /**********************************************************
    /* API, mutators
    /**********************************************************
     */

    public int addName(String name) {
        byte[] ch = name.getBytes(StandardCharsets.UTF_8);
        int len = ch.length;

        if (len <= 12) {
            if (len <= 4) {
                return addName(name, _decodeLast(ch, 0, len));
            }
            int q1 = _decodeFull(ch, 0);
            if (len <= 8) {
                return addName(name, q1, _decodeLast(ch, 4, len-4));
            }
            return addName(name, q1, _decodeFull(ch, 4), _decodeLast(ch, 8, len-8));
        }
        int[] quads = _quads(name);
        return addName(name, quads, quads.length);
    }

    private int addName(String name, int q1) {
        final int index = _count;
        int offset = _findOffsetForAdd(calcHash(q1));
        _hashArea[offset] = q1;
        _hashArea[offset+3] = _lengthAndIndex(1); // increases _count
        return index;
    }

    private int addName(String name, int q1, int q2) {
        final int index = _count;
        int offset = _findOffsetForAdd(calcHash(q1, q2));
        _hashArea[offset] = q1;
        _hashArea[offset+1] = q2;
        _hashArea[offset+3] = _lengthAndIndex(2); // increases _count
        return index;
    }

    private int addName(String name, int q1, int q2, int q3) {
        final int index = _count;
        int offset = _findOffsetForAdd(calcHash(q1, q2, q3));
        _hashArea[offset] = q1;
        _hashArea[offset+1] = q2;
        _hashArea[offset+2] = q3;
        _hashArea[offset+3] = _lengthAndIndex(3); // increases _count
        return index;
    }

    private int addName(String name, int[] q, int qlen)
    {
        switch (qlen) {
        case 1:
            return addName(name, q[0]);
        case 2:
            return addName(name, q[0], q[1]);
        case 3:
            return addName(name, q[0], q[1], q[2]);
        }
        final int index = _count;
        final int hash = calcHash(q, qlen);
        int offset = _findOffsetForAdd(hash);
        _hashArea[offset] = hash;
        int longStart = _appendLongName(q, qlen);
        _hashArea[offset+1] = longStart;
        _hashArea[offset+3] = _lengthAndIndex(qlen); // increases _count
        return index;
    }

    /**
     * Method called to find the location within hash table to add a new symbol in.
     */
    private int _findOffsetForAdd(int hash)
    {
        // first, check the primary:
        int offset = _calcOffset(hash);
        final int[] hashArea = _hashArea;
        if (hashArea[offset+3] == 0) {
            return offset;
        }
        // then secondary
        int offset2 = _secondaryStart + ((offset >> 3) << 2);
        if (hashArea[offset2+3] == 0) {
            return offset2;
        }
        // if not, tertiary?
        offset2 = _tertiaryStart + ((offset >> (_tertiaryShift + 2)) << _tertiaryShift);
        final int bucketSize = (1 << _tertiaryShift);
        for (int end = offset2 + bucketSize; offset2 < end; offset2 += 4) {
            if (hashArea[offset2+3] == 0) {
                return offset2;
            }
        }

        // and if even tertiary full, append at the end of spill area
        offset = _spilloverEnd;

        // 25-Nov-2017, tatu: One potential problem: we may even fill the overflow area.
        //    Seems very unlikely as instances are created for bounded name sets, but
        //    for correctness need to catch. If we must, we can handle this by resizing
        //    hash areas etc, but let's cross that bridge if we ever get there
        final int end = (_hashSize << 3);
        if (_spilloverEnd >= end) {
            throw new IllegalStateException("Internal error: Overflow with "+_count+" entries (hash size of "+_hashSize+")");
        }
        _spilloverEnd += 4;
        return offset;
    }

    private int _appendLongName(int[] quads, int qlen)
    {
        int start = _longNameOffset;
        // note: at this point we must already be shared. But may not have enough space
        if ((start + qlen) > _hashArea.length) {
            // try to increment in reasonable chunks; at least space that we need
            int toAdd = (start + qlen) - _hashArea.length;
            // but at least 1/8 of regular hash area size or 16kB (whichever smaller)
            int minAdd = Math.min(4096, _hashSize);

            int newSize = _hashArea.length + Math.max(toAdd, minAdd);
            _hashArea = Arrays.copyOf(_hashArea, newSize);
        }
        System.arraycopy(quads, 0, _hashArea, start, qlen);
        _longNameOffset += qlen;
        return start;
    }
    
    /*
    /**********************************************************
    /* API, accessors, mostly for Unit Tests
    /**********************************************************
     */

    public int size() { return _count; }

    public int bucketCount() { return _hashSize; }

    // For tests
    public int primaryQuadCount()
    {
        int count = 0;
        for (int offset = 3, end = _secondaryStart; offset < end; offset += 4) {
            if (_hashArea[offset] != 0) {
                ++count;
            }
        }
        return count;
    }

    // For tests
    public int secondaryQuadCount() {
        int count = 0;
        int offset = _secondaryStart + 3;
        for (int end = _tertiaryStart; offset < end; offset += 4) {
            if (_hashArea[offset] != 0) {
                ++count;
            }
        }
        return count;
    }

    // For tests
    public int tertiaryQuadCount() {
        int count = 0;
        int offset = _tertiaryStart + 3; // to 1.5x, starting point of tertiary
        for (int end = offset + _hashSize; offset < end; offset += 4) {
            if (_hashArea[offset] != 0) {
                ++count;
            }
        }
        return count;
    }

    // For tests
    public int spilloverQuadCount() {
        // difference between spillover end, start, divided by 4 (four ints per slot)
        return (_spilloverEnd - _spilloverStart()) >> 2;
    }

    // For tests
    public int totalCount() {
        int count = 0;
        for (int offset = 3, end = (_hashSize << 3); offset < end; offset += 4) {
            if (_hashArea[offset] != 0) {
                ++count;
            }
        }
        return count;
    }

    /*
    /**********************************************************
    /* Public API, accessing symbols
    /**********************************************************
     */

    @Override
    public int matchByQuad(int q1)
    {
        int offset = _calcOffset(calcHash(q1));

        // first: primary match?
        final int[] hashArea = _hashArea;

        int lenAndIndex = hashArea[offset+3];
        if ((lenAndIndex & 0xFFFF) == 1) {
            if (hashArea[offset] == q1) {
                return lenAndIndex >> 16;
            }
        } else if (lenAndIndex == 0) { // empty slot; unlikely but avoid further lookups if so
            return -1;
        }
        // secondary? single slot shared by N/2 primaries
        int offset2 = _secondaryStart + ((offset >> 3) << 2);
        lenAndIndex = hashArea[offset2+3];
        if ((lenAndIndex & 0xFFFF) == 1) {
            if (hashArea[offset2] == q1) {
                return lenAndIndex >> 16;
            }
        } else if (lenAndIndex == 0) { // empty slot; unlikely but avoid further lookups if so
            return -1;
        }
        // tertiary lookup & spillovers best to offline
        return _findTertiary(offset, q1);
    }

    @Override
    public int matchByQuad(int q1, int q2)
    {
        int offset = _calcOffset(calcHash(q1, q2));

        final int[] hashArea = _hashArea;
        int lenAndIndex = hashArea[offset+3];

        if ((lenAndIndex & 0xFFFF) == 2) {
            if ((q1 == hashArea[offset]) && (q2 == hashArea[offset+1])) {
                return lenAndIndex >> 16;
            }
        } else if (lenAndIndex == 0) { // empty slot; unlikely but avoid further lookups if so
            return -1;
        }
        // secondary?
        int offset2 = _secondaryStart + ((offset >> 3) << 2);
        int lenAndIndex2 = hashArea[offset2+3];
        if ((lenAndIndex2 & 0xFFFF) == 2) {
            if ((q1 == hashArea[offset2]) && (q2 == hashArea[offset2+1])) {
                return lenAndIndex2 >> 16;
            }
        } else if (lenAndIndex2 == 0) { // empty slot? Short-circuit if no more spillovers
            return -1;
        }
        return _findTertiary(offset, q1, q2);
    }

    @Override
    public int matchByQuad(int q1, int q2, int q3)
    {
        int offset = _calcOffset(calcHash(q1, q2, q3));
        final int[] hashArea = _hashArea;
        final int lenAndIndex = hashArea[offset+3];
        if ((lenAndIndex & 0xFFFF) == 3) {
            if ((q1 == hashArea[offset]) && (hashArea[offset+1] == q2) && (hashArea[offset+2] == q3)) {
                return lenAndIndex >> 16;
            }
        } else if (lenAndIndex == 0) { // empty slot; unlikely but avoid further lookups if so
            return -1;
        }

        // secondary?
        int offset2 = _secondaryStart + ((offset >> 3) << 2);
        final int lenAndIndex2 = hashArea[offset2+3];
        if ((lenAndIndex2 & 0xFFFF) == 3) {
            if ((q1 == hashArea[offset2]) && (hashArea[offset2+1] == q2) && (hashArea[offset2+2] == q3)) {
                return lenAndIndex2 >> 16;
            }
        } else if (lenAndIndex2 == 0) { // empty slot? Short-circuit if no more spillovers
            return -1;
        }
        return _findTertiary(offset, q1, q2, q3);
    }

    @Override
    public int matchByQuad(int[] q, int qlen)
    {
        // This version differs significantly, because longer names do not fit within cell.
        // Rather, they contain hash in main slot, and offset+length to extension area
        // that contains actual quads.
        if (qlen < 4) { // another sanity check
            switch (qlen) {
            case 3:
                return matchByQuad(q[0], q[1], q[2]);
            case 2:
                return matchByQuad(q[0], q[1]);
            case 1:
                return matchByQuad(q[0]);
            default: // if 0 ever passed
                return -1;
            }
        }
        final int hash = calcHash(q, qlen);
        int offset = _calcOffset(hash);

        final int[] hashArea = _hashArea;
        final int lenAndIndex = hashArea[offset+3];
        
        if ((hash == hashArea[offset]) && ((lenAndIndex & 0xFFFF) == qlen)) {
            // probable but not guaranteed: verify
            if (_verifyLongName(q, qlen, hashArea[offset+1])) {
                return lenAndIndex >> 16;
            }
        }
        if (lenAndIndex == 0) { // empty slot; unlikely but avoid further lookups if so
            return -1;
        }
        // secondary?
        int offset2 = _secondaryStart + ((offset >> 3) << 2);

        final int lenAndIndex2 = hashArea[offset2+3];
        if ((hash == hashArea[offset2]) && ((lenAndIndex2 & 0xFFFF) == qlen)) {
            if (_verifyLongName(q, qlen, hashArea[offset2+1])) {
                return lenAndIndex2 >> 16;
            }
        }
        return _findTertiary(offset, hash, q, qlen);
    }

    private final int _calcOffset(int hash)
    {
        int ix = hash & (_hashSize-1);
        // keeping in mind we have 4 ints per entry
        return (ix << 2);
    }

    /*
    /**********************************************************
    /* Access from spill-over areas
    /**********************************************************
     */

    private int _findTertiary(int origOffset, int q1)
    {
        // tertiary area division is dynamic. First; its size is N/4 compared to
        // primary hash size; and offsets are for 4 int slots. So to get to logical
        // index would shift by 4. But! Tertiary area is further split into buckets,
        // determined by shift value. And finally, from bucket back into physical offsets
        int offset = _tertiaryStart + ((origOffset >> (_tertiaryShift + 2)) << _tertiaryShift);
        final int[] hashArea = _hashArea;
        final int bucketSize = (1 << _tertiaryShift);
        for (int end = offset + bucketSize; offset < end; offset += 4) {
            int lenAndIndex = hashArea[offset+3];
            if ((q1 == hashArea[offset]) && (1 == (lenAndIndex & 0xFFFF))) {
                return lenAndIndex >> 16;
            }
            if (lenAndIndex == 0) {
                return -1;
            }
        }
        // but if tertiary full, check out spill-over area as last resort
        // shared spillover starts at 7/8 of the main hash area
        // (which is sized at 2 * _hashSize), so:
        for (offset = _spilloverStart(); offset < _spilloverEnd; offset += 4) {
            if (q1 == hashArea[offset]) {
                int lenAndIndex = hashArea[offset+3];
                if (1 == (lenAndIndex & 0xFFFF)) {
                    return lenAndIndex >> 16;
                }
            }
        }
        return -1;
    }

    private int _findTertiary(int origOffset, int q1, int q2)
    {
        int offset = _tertiaryStart + ((origOffset >> (_tertiaryShift + 2)) << _tertiaryShift);
        final int[] hashArea = _hashArea;

        final int bucketSize = (1 << _tertiaryShift);
        for (int end = offset + bucketSize; offset < end; offset += 4) {
            int lenAndIndex = hashArea[offset+3];
            if ((q1 == hashArea[offset]) && (q2 == hashArea[offset+1]) && (2 == (lenAndIndex & 0xFFFF))) {
                return lenAndIndex >> 16;
            }
            if (lenAndIndex == 0) {
                return -1;
            }
        }
        for (offset = _spilloverStart(); offset < _spilloverEnd; offset += 4) {
            if ((q1 == hashArea[offset]) && (q2 == hashArea[offset+1])) {
                int lenAndIndex = hashArea[offset+3];
                if (2 == (lenAndIndex & 0xFFFF)) {
                    return lenAndIndex >> 16;
                }
            }
        }
        return -1;
    }

    private int _findTertiary(int origOffset, int q1, int q2, int q3)
    {
        int offset = _tertiaryStart + ((origOffset >> (_tertiaryShift + 2)) << _tertiaryShift);
        final int[] hashArea = _hashArea;
        final int bucketSize = (1 << _tertiaryShift);
        for (int end = offset + bucketSize; offset < end; offset += 4) {
            int lenAndIndex = hashArea[offset+3];
            if ((q1 == hashArea[offset]) && (q2 == hashArea[offset+1]) && (q3 == hashArea[offset+2])
                    && (3 == (lenAndIndex & 0xFFFF))) {
                return lenAndIndex >> 16;
            }
            if (lenAndIndex == 0) {
                return -1;
            }
        }
        for (offset = _spilloverStart(); offset < _spilloverEnd; offset += 4) {
            if ((q1 == hashArea[offset]) && (q2 == hashArea[offset+1]) && (q3 == hashArea[offset+2])) {
                int lenAndIndex = hashArea[offset+3];
                if (3 == (lenAndIndex & 0xFFFF)) {
                    return lenAndIndex >> 16;
                }
            }
        }
        return -1;
    }

    private int _findTertiary(int origOffset, int hash, int[] q, int qlen)
    {
        int offset = _tertiaryStart + ((origOffset >> (_tertiaryShift + 2)) << _tertiaryShift);
        final int[] hashArea = _hashArea;

        final int bucketSize = (1 << _tertiaryShift);
        for (int end = offset + bucketSize; offset < end; offset += 4) {
            int lenAndIndex = hashArea[offset+3];
            if ((hash == hashArea[offset]) && (qlen == (lenAndIndex & 0xFFFF))) {
                if (_verifyLongName(q, qlen, hashArea[offset+1])) {
                    return lenAndIndex >> 16;
                }
            }
            if (lenAndIndex == 0) {
                return -1;
            }
        }
        for (offset = _spilloverStart(); offset < _spilloverEnd; offset += 4) {
            if (hash == hashArea[offset]) {
                int lenAndIndex = hashArea[offset+3];
                if ((qlen == (lenAndIndex & 0xFFFF))
                        && _verifyLongName(q, qlen, hashArea[offset+1])) {
                    return lenAndIndex >> 16;
                }
            }
        }
        return -1;
    }
    
    private boolean _verifyLongName(int[] q, int qlen, int spillOffset)
    {
        final int[] hashArea = _hashArea;
        // spillOffset assumed to be physical index right into quad string
        int ix = 0;

        switch (qlen) {
        default:
            return _verifyLongName2(q, qlen, spillOffset);
        case 8:
            if (q[ix++] != hashArea[spillOffset++]) return false;
        case 7:
            if (q[ix++] != hashArea[spillOffset++]) return false;
        case 6:
            if (q[ix++] != hashArea[spillOffset++]) return false;
        case 5:
            if (q[ix++] != hashArea[spillOffset++]) return false;
        case 4: // always at least 4
            if (q[ix++] != hashArea[spillOffset++]) return false;
            if (q[ix++] != hashArea[spillOffset++]) return false;
            if (q[ix++] != hashArea[spillOffset++]) return false;
            if (q[ix++] != hashArea[spillOffset++]) return false;
        }
        return true;
    }

    private boolean _verifyLongName2(int[] q, int qlen, int spillOffset)
    {
        int ix = 0;
        do {
            if (q[ix++] != _hashArea[spillOffset++]) {
                return false;
            }
        } while (ix < qlen);
        return true;
    }

    /*
    /**********************************************************
    /* Hash calculation
    /**********************************************************
     */

    // // Copied straight frmo big quads canonicalizer: look comments there
    
    private final static int MULT = 33;
    private final static int MULT2 = 65599;
    private final static int MULT3 = 31;
    
    public int calcHash(int q1)
    {
        int hash = q1 + (q1 >>> 16) ^ (q1 << 3);
        return hash + (hash >>> 11);
    }

    public int calcHash(int q1, int q2)
    {
        int hash = q1 + (q1 >>> 15) ^ (q1 >>> 9);
        hash += (q2 * MULT) ^ (q2 >>> 15);
        hash += (hash >>> 7) + (hash >>> 3);
        return hash;
    }

    public int calcHash(int q1, int q2, int q3)
    {
        int hash = q1 + (q1 >>> 15) ^ (q1 >>> 9);
        hash = (hash * MULT) + q2 ^ (q2 >>> 15) + (q2 >> 7);
        hash = (hash * MULT3) + q3 ^ (q3 >>> 13) + (q3 >> 9);
        hash += (hash >>> 4);
        return hash;
    }

    public int calcHash(int[] q, int qlen)
    {
        if (qlen < 4) {
            throw new IllegalArgumentException();
        }
        // And then change handling again for "multi-quad" case
        int hash = q[0];
        hash += (hash >>> 9);
        hash += q[1];
        hash += (hash >>> 15);
        hash *= MULT;
        hash ^= q[2];
        hash += (hash >>> 4);

        for (int i = 3; i < qlen; ++i) {
            int next = q[i];
            next = next ^ (next >> 21);
            hash += next;
        }
        hash *= MULT2;
        
        // and finally shuffle some more once done
        hash += (hash >>> 19);
        hash ^= (hash << 5);
        return hash;
    }

    @Override
    public String toString() {
        int pri = primaryQuadCount();
        int sec = secondaryQuadCount();
        int tert = tertiaryQuadCount();
        int spill = spilloverQuadCount();
        int total = totalCount();
        return String.format("[%s: size=%d, hashSize=%d, %d/%d/%d/%d pri/sec/ter/spill (=%s), total:%d]",
                getClass().getName(), _count, _hashSize,
                pri, sec, tert, spill, (pri+sec+tert+spill), total);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    public static int[] _quads(String name) {
        final byte[] b = name.getBytes(StandardCharsets.UTF_8);
        final int len = b.length;
        int[] buf = new int[(len + 3) >> 2];

        int in = 0;
        int out = 0;
        int left = len;

        for (; left > 4; left -= 4) {
            buf[out++] = _decodeFull(b, in);
            in += 4;
        }
        buf[out++] = _decodeLast(b, in, left);
        return buf;
    }

    private static int _decodeFull(byte[] b, int offset) {
        return (b[offset] << 24) + ((b[offset+1] & 0xFF) << 16)
                + ((b[offset+2] & 0xFF) << 8) + (b[offset+3] & 0xFF);
    }

    private static int _decodeLast(byte[] b, int offset, int bytes) {
        // 22-Nov-2017, tatu: Padding apparently not used with fully binary field names,
        //     unlike with JSON. May or may not want to change this in future.
        int value = b[offset++] & 0xFF;
        switch (bytes) {
        case 4:
            value = (value << 8) | (b[offset++] & 0xFF);
        case 3:
            value = (value << 8) | (b[offset++] & 0xFF);
        case 2:
            value = (value << 8) | (b[offset++] & 0xFF);
        }
        return value;
    }

    private int _lengthAndIndex(int qlen) {
        if (qlen > MAX_LENGTH_IN_QUADS) {
            throw new IllegalArgumentException("Maximum name length in quads ("+MAX_LENGTH_IN_QUADS+") exceeded: "+qlen);
        }
        // count as Most-Significant-Word (16-bits); length LSB
        if (_count == MAX_ENTRIES) {
            throw new IllegalArgumentException("Maximum entry count ("+MAX_ENTRIES+") reached, can not add more entries");
        }
        int enc = (_count << 16) | qlen;
        ++_count;
        return enc;
    }

    /**
     * Helper method that calculates start of the spillover area
     */
    private int _spilloverStart() {
        // we'll need slot at 1.75x of hashSize, but with 4-ints per slot.
        // So basically multiply by 7 (i.e. shift to multiply by 8 subtract 1)
        int offset = _hashSize;
        return (offset << 3) - offset;
    }
}
