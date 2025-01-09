module tools.jackson.core {
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

    provides tools.jackson.core.TokenStreamFactory with
        tools.jackson.core.json.JsonFactory;
}
