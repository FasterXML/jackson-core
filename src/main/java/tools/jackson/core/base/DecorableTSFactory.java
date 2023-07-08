package tools.jackson.core.base;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import tools.jackson.core.*;
import tools.jackson.core.io.*;
import tools.jackson.core.util.JsonGeneratorDecorator;

/**
 * Intermediate base {@link TokenStreamFactory} implementation that offers support for
 * streams that allow decoration of low-level input sources and output targets.
 *
 * @since 3.0
 */
public abstract class DecorableTSFactory
    extends TokenStreamFactory
    implements java.io.Serializable
{
    private static final long serialVersionUID = 3L;

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

        protected List<JsonGeneratorDecorator> _generatorDecorators;

        // // // Construction

        protected DecorableTSFBuilder(StreamReadConstraints src,
                StreamWriteConstraints swc,
                int formatPF, int formatGF) {
            super(src, swc, formatPF, formatGF);
            _inputDecorator = null;
            _outputDecorator = null;
            _generatorDecorators = null;
        }

        protected DecorableTSFBuilder(DecorableTSFactory base)
        {
            super(base);
            _inputDecorator = base.getInputDecorator();
            _outputDecorator = base.getOutputDecorator();
            _generatorDecorators = base.getGeneratorDecorators();
        }

        // // // Accessors
        public InputDecorator inputDecorator() { return _inputDecorator; }
        public OutputDecorator outputDecorator() { return _outputDecorator; }
        public List<JsonGeneratorDecorator> generatorDecorators() { return _generatorDecorators; }

        // // // Decorators

        public T inputDecorator(InputDecorator dec) {
            _inputDecorator = dec;
            return _this();
        }

        public T outputDecorator(OutputDecorator dec) {
            _outputDecorator = dec;
            return _this();
        }

        public T addDecorator(JsonGeneratorDecorator dec) {
            if (_generatorDecorators == null) {
                _generatorDecorators = new ArrayList<>();
            }
            _generatorDecorators.add(dec);
            return _this();
        }
    }

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */

    /**
     * Optional helper object that may decorate input sources, to do
     * additional processing on input during parsing.
     */
    protected final InputDecorator _inputDecorator;

    /**
     * Optional helper object that may decorate output object, to do
     * additional processing on output during content generation.
     */
    protected final OutputDecorator _outputDecorator;

    protected List<JsonGeneratorDecorator> _generatorDecorators;

    /*
    /**********************************************************************
    /* Construction
    /**********************************************************************
     */

    protected DecorableTSFactory(StreamReadConstraints src,
            StreamWriteConstraints swc,
            int formatPF, int formatGF) {
        super(src, swc, formatPF, formatGF);
        _inputDecorator = null;
        _outputDecorator = null;
        _generatorDecorators = null;
    }

    /**
     * Constructor used by builders for instantiation.
     *
     * @param baseBuilder Builder with configurations to use
     *
     * @since 3.0
     */
    protected DecorableTSFactory(DecorableTSFBuilder<?,?> baseBuilder)
    {
        super(baseBuilder);
        _inputDecorator = baseBuilder.inputDecorator();
        _outputDecorator = baseBuilder.outputDecorator();
        _generatorDecorators = _copy(baseBuilder.generatorDecorators());
    }

    // Copy constructor.
    protected DecorableTSFactory(DecorableTSFactory src) {
        super(src);
        _inputDecorator = src.getInputDecorator();
        _outputDecorator = src.getOutputDecorator();
        _generatorDecorators = _copy(src._generatorDecorators);
    }

    protected static <T> List<T> _copy(List<T> src) {
        if (src == null) {
            return src;
        }
        return new ArrayList<T>(src);
    }

    /*
    /**********************************************************************
    /* Configuration, decorators
    /**********************************************************************
     */

    public OutputDecorator getOutputDecorator() {
        return _outputDecorator;
    }

    public InputDecorator getInputDecorator() {
        return _inputDecorator;
    }

    public List<JsonGeneratorDecorator> getGeneratorDecorators() {
        return _copy(_generatorDecorators);
    }

    /*
    /**********************************************************************
    /* Decorators, input
    /**********************************************************************
     */

    protected InputStream _decorate(IOContext ioCtxt, InputStream in) throws JacksonException
    {
        if (_inputDecorator != null) {
            InputStream in2 = _inputDecorator.decorate(ioCtxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected Reader _decorate(IOContext ioCtxt, Reader in) throws JacksonException
    {
        if (_inputDecorator != null) {
            Reader in2 = _inputDecorator.decorate(ioCtxt, in);
            if (in2 != null) {
                return in2;
            }
        }
        return in;
    }

    protected DataInput _decorate(IOContext ioCtxt, DataInput in) throws JacksonException
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
    /**********************************************************************
    /* Decorators, output
    /**********************************************************************
     */

    protected OutputStream _decorate(IOContext ioCtxt, OutputStream out) throws JacksonException
    {
        if (_outputDecorator != null) {
            OutputStream out2 = _outputDecorator.decorate(ioCtxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }

    protected Writer _decorate(IOContext ioCtxt, Writer out) throws JacksonException
    {
        if (_outputDecorator != null) {
            Writer out2 = _outputDecorator.decorate(ioCtxt, out);
            if (out2 != null) {
                return out2;
            }
        }
        return out;
    }


    protected JsonGenerator _decorate(JsonGenerator result) {
        if (_generatorDecorators != null) {
            for(JsonGeneratorDecorator decorator : _generatorDecorators) {
                result = decorator.decorate(this, result);
            }
        }
        return result;
    }
}
