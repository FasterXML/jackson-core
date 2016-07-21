/* Jackson JSON-processor.
 *
 * Copyright (c) 2007- Tatu Saloranta, tatu.saloranta@iki.fi
 */
package com.fasterxml.jackson.core;

import java.util.Arrays;

import com.fasterxml.jackson.core.util.ByteArrayBuilder;

/**
 * Abstract base class used to define specific details of which
 * variant of Base64 encoding/decoding is to be used. Although there is
 * somewhat standard basic version (so-called "MIME Base64"), other variants
 * exists, see <a href="http://en.wikipedia.org/wiki/Base64">Base64 Wikipedia entry</a> for details.
 * 
 * @author Tatu Saloranta
 */
public final class Base64Variant
    implements java.io.Serializable
{
    private final static int INT_SPACE = 0x20;
    
    // We'll only serialize name
    private static final long serialVersionUID = 1L;

    /**
     * Placeholder used by "no padding" variant, to be used when a character
     * value is needed.
     */
    final static char PADDING_CHAR_NONE = '\0';

    /**
     * Marker used to denote ascii characters that do not correspond
     * to a 6-bit value (in this variant), and is not used as a padding
     * character.
     */
    public final static int BASE64_VALUE_INVALID = -1;

    /**
     * Marker used to denote ascii character (in decoding table) that
     * is the padding character using this variant (if any).
     */
    public final static int BASE64_VALUE_PADDING = -2;

    /*
    /**********************************************************
    /* Encoding/decoding tables
    /**********************************************************
     */

    /**
     * Decoding table used for base 64 decoding.
     */
    private final transient int[] _asciiToBase64 = new int[128];

    /**
     * Encoding table used for base 64 decoding when output is done
     * as characters.
     */
    private final transient char[] _base64ToAsciiC = new char[64];

    /**
     * Alternative encoding table used for base 64 decoding when output is done
     * as ascii bytes.
     */
    private final transient byte[] _base64ToAsciiB = new byte[64];

    /*
    /**********************************************************
    /* Other configuration
    /**********************************************************
     */

    /**
     * Symbolic name of variant; used for diagnostics/debugging.
     *<p>
     * Note that this is the only non-transient field; used when reading
     * back from serialized state.
     *<p>
     * Also: must not be private, accessed from `BaseVariants`
     */
    final String _name;

    /**
     * Whether this variant uses padding or not.
     */
    private final transient boolean _usesPadding;

    /**
     * Characted used for padding, if any ({@link #PADDING_CHAR_NONE} if not).
     */
    private final transient char _paddingChar;
    
    /**
     * Maximum number of encoded base64 characters to output during encoding
     * before adding a linefeed, if line length is to be limited
     * ({@link java.lang.Integer#MAX_VALUE} if not limited).
     *<p>
     * Note: for some output modes (when writing attributes) linefeeds may
     * need to be avoided, and this value ignored.
     */
    private final transient int _maxLineLength;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    public Base64Variant(String name, String base64Alphabet, boolean usesPadding, char paddingChar, int maxLineLength)
    {
        _name = name;
        _usesPadding = usesPadding;
        _paddingChar = paddingChar;
        _maxLineLength = maxLineLength;

        // Ok and then we need to create codec tables.

        // First the main encoding table:
        int alphaLen = base64Alphabet.length();
        if (alphaLen != 64) {
            throw new IllegalArgumentException("Base64Alphabet length must be exactly 64 (was "+alphaLen+")");
        }

        // And then secondary encoding table and decoding table:
        base64Alphabet.getChars(0, alphaLen, _base64ToAsciiC, 0);
        Arrays.fill(_asciiToBase64, BASE64_VALUE_INVALID);
        for (int i = 0; i < alphaLen; ++i) {
            char alpha = _base64ToAsciiC[i];
            _base64ToAsciiB[i] = (byte) alpha;
            _asciiToBase64[alpha] = i;
        }

        // Plus if we use padding, add that in too
        if (usesPadding) {
            _asciiToBase64[(int) paddingChar] = BASE64_VALUE_PADDING;
        }
    }

    /**
     * "Copy constructor" that can be used when the base alphabet is identical
     * to one used by another variant except for the maximum line length
     * (and obviously, name).
     */
    public Base64Variant(Base64Variant base, String name, int maxLineLength)
    {
        this(base, name, base._usesPadding, base._paddingChar, maxLineLength);
    }

    /**
     * "Copy constructor" that can be used when the base alphabet is identical
     * to one used by another variant, but other details (padding, maximum
     * line length) differ
     */
    public Base64Variant(Base64Variant base, String name, boolean usesPadding, char paddingChar, int maxLineLength)
    {
        _name = name;
        byte[] srcB = base._base64ToAsciiB;
        System.arraycopy(srcB, 0, this._base64ToAsciiB, 0, srcB.length);
        char[] srcC = base._base64ToAsciiC;
        System.arraycopy(srcC, 0, this._base64ToAsciiC, 0, srcC.length);
        int[] srcV = base._asciiToBase64;
        System.arraycopy(srcV, 0, this._asciiToBase64, 0, srcV.length);

        _usesPadding = usesPadding;
        _paddingChar = paddingChar;
        _maxLineLength = maxLineLength;
    }

    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method used to "demote" deserialized instances back to 
     * canonical ones
     */
    protected Object readResolve() {
        return Base64Variants.valueOf(_name);
    }
    
    /*
    /**********************************************************
    /* Public accessors
    /**********************************************************
     */

    public String getName() { return _name; }

    public boolean usesPadding() { return _usesPadding; }
    public boolean usesPaddingChar(char c) { return c == _paddingChar; }
    public boolean usesPaddingChar(int ch) { return ch == (int) _paddingChar; }
    public char getPaddingChar() { return _paddingChar; }
    public byte getPaddingByte() { return (byte)_paddingChar; }

    public int getMaxLineLength() { return _maxLineLength; }

    /*
    /**********************************************************
    /* Decoding support
    /**********************************************************
     */

    /**
     * @return 6-bit decoded value, if valid character; 
     */
    public int decodeBase64Char(char c)
    {
        int ch = (int) c;
        return (ch <= 127) ? _asciiToBase64[ch] : BASE64_VALUE_INVALID;
    }

    public int decodeBase64Char(int ch)
    {
        return (ch <= 127) ? _asciiToBase64[ch] : BASE64_VALUE_INVALID;
    }

    public int decodeBase64Byte(byte b)
    {
        int ch = (int) b;
        // note: cast retains sign, so it's from -128 to +127
        if (ch < 0) {
            return BASE64_VALUE_INVALID;
        }
        return _asciiToBase64[ch];
    }

    /*
    /**********************************************************
    /* Encoding support
    /**********************************************************
     */

    public char encodeBase64BitsAsChar(int value)
    {
        /* Let's assume caller has done necessary checks; this
         * method must be fast and inlinable
         */
        return _base64ToAsciiC[value];
    }

    /**
     * Method that encodes given right-aligned (LSB) 24-bit value
     * into 4 base64 characters, stored in given result buffer.
     */
    public int encodeBase64Chunk(int b24, char[] buffer, int ptr)
    {
        buffer[ptr++] = _base64ToAsciiC[(b24 >> 18) & 0x3F];
        buffer[ptr++] = _base64ToAsciiC[(b24 >> 12) & 0x3F];
        buffer[ptr++] = _base64ToAsciiC[(b24 >> 6) & 0x3F];
        buffer[ptr++] = _base64ToAsciiC[b24 & 0x3F];
        return ptr;
    }

    public void encodeBase64Chunk(StringBuilder sb, int b24)
    {
        sb.append(_base64ToAsciiC[(b24 >> 18) & 0x3F]);
        sb.append(_base64ToAsciiC[(b24 >> 12) & 0x3F]);
        sb.append(_base64ToAsciiC[(b24 >> 6) & 0x3F]);
        sb.append(_base64ToAsciiC[b24 & 0x3F]);
    }

    /**
     * Method that outputs partial chunk (which only encodes one
     * or two bytes of data). Data given is still aligned same as if
     * it as full data; that is, missing data is at the "right end"
     * (LSB) of int.
     *
     * @param outputBytes Number of encoded bytes included (either 1 or 2)
     */
    public int encodeBase64Partial(int bits, int outputBytes, char[] buffer, int outPtr)
    {
        buffer[outPtr++] = _base64ToAsciiC[(bits >> 18) & 0x3F];
        buffer[outPtr++] = _base64ToAsciiC[(bits >> 12) & 0x3F];
        if (_usesPadding) {
            buffer[outPtr++] = (outputBytes == 2) ?
                _base64ToAsciiC[(bits >> 6) & 0x3F] : _paddingChar;
            buffer[outPtr++] = _paddingChar;
        } else {
            if (outputBytes == 2) {
                buffer[outPtr++] = _base64ToAsciiC[(bits >> 6) & 0x3F];
            }
        }
        return outPtr;
    }

    public void encodeBase64Partial(StringBuilder sb, int bits, int outputBytes)
    {
        sb.append(_base64ToAsciiC[(bits >> 18) & 0x3F]);
        sb.append(_base64ToAsciiC[(bits >> 12) & 0x3F]);
        if (_usesPadding) {
            sb.append((outputBytes == 2) ?
                      _base64ToAsciiC[(bits >> 6) & 0x3F] : _paddingChar);
            sb.append(_paddingChar);
        } else {
            if (outputBytes == 2) {
                sb.append(_base64ToAsciiC[(bits >> 6) & 0x3F]);
            }
        }
    }

    public byte encodeBase64BitsAsByte(int value)
    {
        // As with above, assuming it is 6-bit value
        return _base64ToAsciiB[value];
    }

    /**
     * Method that encodes given right-aligned (LSB) 24-bit value
     * into 4 base64 bytes (ascii), stored in given result buffer.
     */
    public int encodeBase64Chunk(int b24, byte[] buffer, int ptr)
    {
        buffer[ptr++] = _base64ToAsciiB[(b24 >> 18) & 0x3F];
        buffer[ptr++] = _base64ToAsciiB[(b24 >> 12) & 0x3F];
        buffer[ptr++] = _base64ToAsciiB[(b24 >> 6) & 0x3F];
        buffer[ptr++] = _base64ToAsciiB[b24 & 0x3F];
        return ptr;
    }

    /**
     * Method that outputs partial chunk (which only encodes one
     * or two bytes of data). Data given is still aligned same as if
     * it as full data; that is, missing data is at the "right end"
     * (LSB) of int.
     *
     * @param outputBytes Number of encoded bytes included (either 1 or 2)
     */
    public int encodeBase64Partial(int bits, int outputBytes, byte[] buffer, int outPtr)
    {
        buffer[outPtr++] = _base64ToAsciiB[(bits >> 18) & 0x3F];
        buffer[outPtr++] = _base64ToAsciiB[(bits >> 12) & 0x3F];
        if (_usesPadding) {
            byte pb = (byte) _paddingChar;
            buffer[outPtr++] = (outputBytes == 2) ?
                _base64ToAsciiB[(bits >> 6) & 0x3F] : pb;
            buffer[outPtr++] = pb;
        } else {
            if (outputBytes == 2) {
                buffer[outPtr++] = _base64ToAsciiB[(bits >> 6) & 0x3F];
            }
        }
        return outPtr;
    }

    /*
    /**********************************************************
    /* Convenience conversion methods for String to/from bytes
    /* use case.
    /**********************************************************
     */
    
    /**
     * Convenience method for converting given byte array as base64 encoded
     * String using this variant's settings.
     * Resulting value is "raw", that is, not enclosed in double-quotes.
     * 
     * @param input Byte array to encode
     */
    public String encode(byte[] input)
    {
        return encode(input, false);
    }

    /**
     * Convenience method for converting given byte array as base64 encoded String
     * using this variant's settings,
     * optionally enclosed in double-quotes.
     * 
     * @param input Byte array to encode
     * @param addQuotes Whether to surround resulting value in double quotes or not
     */
    public String encode(byte[] input, boolean addQuotes)
    {
        int inputEnd = input.length;
        StringBuilder sb;
        {
            // let's approximate... 33% overhead, ~= 3/8 (0.375)
            int outputLen = inputEnd + (inputEnd >> 2) + (inputEnd >> 3);
            sb = new StringBuilder(outputLen);
        }
        if (addQuotes) {
            sb.append('"');
        }

        int chunksBeforeLF = getMaxLineLength() >> 2;

        // Ok, first we loop through all full triplets of data:
        int inputPtr = 0;
        int safeInputEnd = inputEnd-3; // to get only full triplets

        while (inputPtr <= safeInputEnd) {
            // First, mash 3 bytes into lsb of 32-bit int
            int b24 = ((int) input[inputPtr++]) << 8;
            b24 |= ((int) input[inputPtr++]) & 0xFF;
            b24 = (b24 << 8) | (((int) input[inputPtr++]) & 0xFF);
            encodeBase64Chunk(sb, b24);
            if (--chunksBeforeLF <= 0) {
                // note: must quote in JSON value, so not really useful...
                sb.append('\\');
                sb.append('n');
                chunksBeforeLF = getMaxLineLength() >> 2;
            }
        }

        // And then we may have 1 or 2 leftover bytes to encode
        int inputLeft = inputEnd - inputPtr; // 0, 1 or 2
        if (inputLeft > 0) { // yes, but do we have room for output?
            int b24 = ((int) input[inputPtr++]) << 16;
            if (inputLeft == 2) {
                b24 |= (((int) input[inputPtr++]) & 0xFF) << 8;
            }
            encodeBase64Partial(sb, b24, inputLeft);
        }

        if (addQuotes) {
            sb.append('"');
        }
        return sb.toString();
    }

    /**
     * Convenience method for decoding contents of a Base64-encoded String,
     * using this variant's settings.
     * 
     * @param input
     * 
     * @since 2.2.3
     *
     * @throws IllegalArgumentException if input is not valid base64 encoded data
     */
    @SuppressWarnings("resource")
    public byte[] decode(String input) throws IllegalArgumentException
    {
        ByteArrayBuilder b = new ByteArrayBuilder();
        decode(input, b);
        return b.toByteArray();
    }

    /**
     * Convenience method for decoding contents of a Base64-encoded String,
     * using this variant's settings
     * and appending decoded binary data using provided {@link ByteArrayBuilder}.
     *<p>
     * NOTE: builder will NOT be reset before decoding (nor cleared afterwards);
     * assumption is that caller will ensure it is given in proper state, and
     * used as appropriate afterwards.
     * 
     * @since 2.2.3
     *
     * @throws IllegalArgumentException if input is not valid base64 encoded data
     */
    public void decode(String str, ByteArrayBuilder builder) throws IllegalArgumentException
    {
        int ptr = 0;
        int len = str.length();
        
        main_loop:
        while (ptr < len) {
            // first, we'll skip preceding white space, if any
            char ch;
            do {
                ch = str.charAt(ptr++);
                if (ptr >= len) {
                    break main_loop;
                }
            } while (ch <= INT_SPACE);
            int bits = decodeBase64Char(ch);
            if (bits < 0) {
                _reportInvalidBase64(ch, 0, null);
            }
            int decodedData = bits;
            // then second base64 char; can't get padding yet, nor ws
            if (ptr >= len) {
                _reportBase64EOF();
            }
            ch = str.charAt(ptr++);
            bits = decodeBase64Char(ch);
            if (bits < 0) {
                _reportInvalidBase64(ch, 1, null);
            }
            decodedData = (decodedData << 6) | bits;
            // third base64 char; can be padding, but not ws
            if (ptr >= len) {
                // but as per [JACKSON-631] can be end-of-input, iff not using padding
                if (!usesPadding()) {
                    decodedData >>= 4;
                    builder.append(decodedData);
                    break;
                }
                _reportBase64EOF();
            }
            ch = str.charAt(ptr++);
            bits = decodeBase64Char(ch);
            
            // First branch: can get padding (-> 1 byte)
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    _reportInvalidBase64(ch, 2, null);
                }
                // Ok, must get padding
                if (ptr >= len) {
                    _reportBase64EOF();
                }
                ch = str.charAt(ptr++);
                if (!usesPaddingChar(ch)) {
                    _reportInvalidBase64(ch, 3, "expected padding character '"+getPaddingChar()+"'");
                }
                // Got 12 bits, only need 8, need to shift
                decodedData >>= 4;
                builder.append(decodedData);
                continue;
            }
            // Nope, 2 or 3 bytes
            decodedData = (decodedData << 6) | bits;
            // fourth and last base64 char; can be padding, but not ws
            if (ptr >= len) {
                // but as per [JACKSON-631] can be end-of-input, iff not using padding
                if (!usesPadding()) {
                    decodedData >>= 2;
                    builder.appendTwoBytes(decodedData);
                    break;
                }
                _reportBase64EOF();
            }
            ch = str.charAt(ptr++);
            bits = decodeBase64Char(ch);
            if (bits < 0) {
                if (bits != Base64Variant.BASE64_VALUE_PADDING) {
                    _reportInvalidBase64(ch, 3, null);
                }
                decodedData >>= 2;
                builder.appendTwoBytes(decodedData);
            } else {
                // otherwise, our triple is now complete
                decodedData = (decodedData << 6) | bits;
                builder.appendThreeBytes(decodedData);
            }
        }
    }

    /*
    /**********************************************************
    /* Overridden standard methods
    /**********************************************************
     */

    @Override
    public String toString() { return _name; }
    
    @Override
    public boolean equals(Object o) {
        // identity comparison should be dine
        return (o == this);
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */
    
    /**
     * @param bindex Relative index within base64 character unit; between 0
     *   and 3 (as unit has exactly 4 characters)
     */
    protected void _reportInvalidBase64(char ch, int bindex, String msg)
        throws IllegalArgumentException
    {
        String base;
        if (ch <= INT_SPACE) {
            base = "Illegal white space character (code 0x"+Integer.toHexString(ch)+") as character #"+(bindex+1)+" of 4-char base64 unit: can only used between units";
        } else if (usesPaddingChar(ch)) {
            base = "Unexpected padding character ('"+getPaddingChar()+"') as character #"+(bindex+1)+" of 4-char base64 unit: padding only legal as 3rd or 4th character";
        } else if (!Character.isDefined(ch) || Character.isISOControl(ch)) {
            // Not sure if we can really get here... ? (most illegal xml chars are caught at lower level)
            base = "Illegal character (code 0x"+Integer.toHexString(ch)+") in base64 content";
        } else {
            base = "Illegal character '"+ch+"' (code 0x"+Integer.toHexString(ch)+") in base64 content";
        }
        if (msg != null) {
            base = base + ": " + msg;
        }
        throw new IllegalArgumentException(base);
    }

    protected void _reportBase64EOF() throws IllegalArgumentException {
        throw new IllegalArgumentException("Unexpected end-of-String in base64 content");
    }
}

