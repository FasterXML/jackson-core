// jackson-core main Module descriptor
module tools.jackson.core {
    // FastDoubleParser shaded but JPMS still requires it to compile?
    requires static ch.randelshofer.fastdoubleparser;

    // Exports for regular dependencies
    exports tools.jackson.core;
    exports tools.jackson.core.async;
    exports tools.jackson.core.base;
    exports tools.jackson.core.exc;
    exports tools.jackson.core.filter;
    exports tools.jackson.core.io;
    exports tools.jackson.core.json;
    exports tools.jackson.core.json.async;
    exports tools.jackson.core.sym;
    exports tools.jackson.core.tree;
    exports tools.jackson.core.type;
    exports tools.jackson.core.util;

    // But opens only for unit test suite; as well as some extra exports
    opens tools.jackson.core to tools.jackson.core.test;
    opens tools.jackson.core.async to tools.jackson.core.test;
    opens tools.jackson.core.base to tools.jackson.core.test;
    opens tools.jackson.core.exc to tools.jackson.core.test;
    opens tools.jackson.core.filter to tools.jackson.core.test;
    opens tools.jackson.core.io to tools.jackson.core.test;
    exports tools.jackson.core.io.schubfach to tools.jackson.core.test;
    opens tools.jackson.core.json to tools.jackson.core.test;
    opens tools.jackson.core.json.async to tools.jackson.core.test;
    opens tools.jackson.core.sym to tools.jackson.core.test;
    opens tools.jackson.core.tree to tools.jackson.core.test;
    opens tools.jackson.core.type to tools.jackson.core.test;
    opens tools.jackson.core.util to tools.jackson.core.test;

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.core.json.JsonFactory;
}
