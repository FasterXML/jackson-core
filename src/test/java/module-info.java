// jackson-core test Module descriptor: contains most if not all main
// plus some test dependencies
module tools.jackson.core {
    // Additional test lib/framework dependencies
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    // FastDoubleParser shaded but JPMS still requires it to compile?
    requires static ch.randelshofer.fastdoubleparser;

    // Test utilities to export
    exports tools.jackson.core.testutil;
    exports tools.jackson.core.testutil.failure;
    
    // Main exports need to switch to "opens" for testing
    opens tools.jackson.core;
    opens tools.jackson.core.async;
    opens tools.jackson.core.base;
    opens tools.jackson.core.exc;
    opens tools.jackson.core.filter;
    opens tools.jackson.core.io;
    opens tools.jackson.core.json;
    opens tools.jackson.core.json.async;
    opens tools.jackson.core.sym;
    opens tools.jackson.core.tree;
    opens tools.jackson.core.type;
    opens tools.jackson.core.util;

    // Additional test opens (not exported by main)

    opens tools.jackson.core.base64;
    opens tools.jackson.core.constraints;
    opens tools.jackson.core.dos;
    opens tools.jackson.core.fuzz;
    opens tools.jackson.core.io.schubfach;
    opens tools.jackson.core.jsonptr;
    opens tools.jackson.core.read;
    opens tools.jackson.core.read.loc;
    opens tools.jackson.core.testutil.failure;
    opens tools.jackson.core.tofix;
    opens tools.jackson.core.tofix.async;
    opens tools.jackson.core.write;

}
