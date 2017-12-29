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
    /**
     * Since factory instances are immutable, a Builder class is needed for creating
     * configurations for differently configured factory instances.
     *
     * @since 3.0
     */
    public abstract static class DecorableTSFBuilder<F extends TokenStreamFactory,
        T extends TSFBuilder<F,T>>
        extends TSFBuilder<F,T>
    {
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

        // // // Construction

        protected DecorableTSFBuilder() {
            super();
            _inputDecorator = null;
            _outputDecorator = null;
        }

        protected DecorableTSFBuilder(DecorableTSFactory base)
        {
            super(base);
            _inputDecorator = base.getInputDecorator();
            _outputDecorator = base.getOutputDecorator();
        }

        // // // Accessors
        public InputDecorator inputDecorator() { return _inputDecorator; }
        public OutputDecorator outputDecorator() { return _outputDecorator; }

        // // // Decorators

        public T inputDecorator(InputDecorator dec) {
            _inputDecorator = dec;
            return _this();
        }

        public T outputDecorator(OutputDecorator dec) {
            _outputDecorator = dec;
            return _this();
        }
    }

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
     * Constructors used by builders for instantiation.
     *
     * @since 3.0
     */
    protected DecorableTSFactory(DecorableTSFBuilder<?,?> baseBuilder)
    {
        super(baseBuilder);
        _inputDecorator = baseBuilder.inputDecorator();
        _outputDecorator = baseBuilder.outputDecorator();
    }

    /**
     * Constructor used when copy()ing a factory instance.
     */
    protected DecorableTSFactory(DecorableTSFactory src) {
        super(src);
        _inputDecorator = src.getInputDecorator();
        _outputDecorator = src.getOutputDecorator();
    }

    /*
    /**********************************************************
    /* Configuration, decorators
    /**********************************************************
     */

    public OutputDecorator getOutputDecorator() {
        return _outputDecorator;
    }

    @Deprecated // since 3.0
    public DecorableTSFactory setOutputDecorator(OutputDecorator d) {
        _outputDecorator = d;
        return this;
    }

    public InputDecorator getInputDecorator() {
        return _inputDecorator;
    }

    @Deprecated // since 3.0
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
