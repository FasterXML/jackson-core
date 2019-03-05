module com.fasterxml.jackson.core {
    // 04-Mar-2019, tatu: Ugh. Can not use wildcards, stupid ass JDK 9+ module system...
    //    So, for 2.x core need to make sure we manually include everything.
    //    Worse, there is only syntactic validation, not contents, so we can both miss
    //    AND add bogus packages.
    exports com.fasterxml.jackson.core;
    exports com.fasterxml.jackson.core.async;
    exports com.fasterxml.jackson.core.base;
    exports com.fasterxml.jackson.core.exc;
    exports com.fasterxml.jackson.core.filter;
    exports com.fasterxml.jackson.core.format;
    exports com.fasterxml.jackson.core.io;
    exports com.fasterxml.jackson.core.json;
    exports com.fasterxml.jackson.core.json.async;
    exports com.fasterxml.jackson.core.sym;
    exports com.fasterxml.jackson.core.type;
    exports com.fasterxml.jackson.core.util;
}
