// Generated 14-Mar-2019 using Moditect maven plugin
module com.fasterxml.jackson.core {
    exports com.fasterxml.jackson.core;
    exports com.fasterxml.jackson.core.async;
    exports com.fasterxml.jackson.core.base;
    exports com.fasterxml.jackson.core.exc;
    exports com.fasterxml.jackson.core.filter;
    exports com.fasterxml.jackson.core.io;
    exports com.fasterxml.jackson.core.json;
    exports com.fasterxml.jackson.core.json.async;
    exports com.fasterxml.jackson.core.sym;
    exports com.fasterxml.jackson.core.tree;
    exports com.fasterxml.jackson.core.type;
    exports com.fasterxml.jackson.core.util;

    provides com.fasterxml.jackson.core.TokenStreamFactory with
        com.fasterxml.jackson.core.json.JsonFactory;
}
