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
    exports tools.jackson.core.io.schubfach to tools.jackson.core.unittest;
    opens tools.jackson.core.json to tools.jackson.core.unittest;

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.core.json.JsonFactory;
}
