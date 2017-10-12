package com.fasterxml.jackson.core.base;

import java.io.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.*;

/**
 * Intermediate base {@link TokenStreamFactory} implementation that offers support for
 * streams that allow decoration of low-level input sources and output targets.
 *
 * @since 3.0
 */
public abstract class DecorableTSFactory
    extends TokenStreamFactory
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Optional helper object that may decorate input sources, to do
     * additional processing on input during parsing.
     */
    protected InputDecorator _inputDecorator;

    /**
     * Optional helper object that may decorate output object, to do
     * additional processing on output during content generation.
     */
    protected OutputDecorator _outputDecorator;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */
    
    protected DecorableTSFactory() { }

    /**
     * Constructor used when copy()ing a factory instance.
     */
    protected DecorableTSFactory(DecorableTSFactory src) {
        super(src);
    }

    /*
    /**********************************************************
    /* Configuration, decorators
    /**********************************************************
     */

    public OutputDecorator getOutputDecorator() {
        return _outputDecorator;
    }

    public DecorableTSFactory setOutputDecorator(OutputDecorator d) {
        _outputDecorator = d;
        return this;
    }

    public InputDecorator getInputDecorator() {
        return _inputDecorator;
    }

    public DecorableTSFactory setInputDecorator(InputDecorator d) {
        _inputDecorator = d;
        return this;
    }

    /*
    /**********************************************************
    /* Decorators, input
    /**********************************************************
     */

    protected InputStream _decorate(IOContext ioCtxt, InputStream in) throws IOException
    {
        if (_inputDecorator != null) {
            InputStream in2 = _inputDecorator.decorate(ioCtxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected Reader _decorate(IOContext ioCtxt, Reader in) throws IOException
    {
        if (_inputDecorator != null) {
            Reader in2 = _inputDecorator.decorate(ioCtxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected DataInput _decorate(IOContext ioCtxt, DataInput in) throws IOException
    {
        if (_inputDecorator != null) {
            DataInput in2 = _inputDecorator.decorate(ioCtxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    /*
    /**********************************************************
    /* Decorators, output
    /**********************************************************
     */

    protected OutputStream _decorate(IOContext ioCtxt, OutputStream out) throws IOException
    {
        if (_outputDecorator != null) {
            OutputStream out2 = _outputDecorator.decorate(ioCtxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }

    protected Writer _decorate(IOContext ioCtxt, Writer out) throws IOException
    {
        if (_outputDecorator != null) {
            Writer out2 = _outputDecorator.decorate(ioCtxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }
}
