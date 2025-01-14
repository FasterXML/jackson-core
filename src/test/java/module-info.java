// jackson-core test Module descriptor: used both for tests and to
// to produce "test-jar" for other Jackson components to use
module tools.jackson.core.testutil
{
    // Additional test lib/framework dependencies
    requires org.assertj.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.params;

    // Requires Main jar for tests
    requires tools.jackson.core;

    // Exports a small set of Classes for downstream Jackson components
    exports tools.jackson.core.testutil;
    exports tools.jackson.core.testutil.failure;

    // Additional test opens for JUnit tests

    opens tools.jackson.core.unittest;
    opens tools.jackson.core.unittest.async;
    opens tools.jackson.core.unittest.base;
    opens tools.jackson.core.unittest.base64;
    opens tools.jackson.core.unittest.constraints;
    opens tools.jackson.core.unittest.dos;
    opens tools.jackson.core.unittest.filter;
    opens tools.jackson.core.unittest.fuzz;
    opens tools.jackson.core.unittest.io;
    opens tools.jackson.core.unittest.io.schubfach;
    opens tools.jackson.core.unittest.json;
    opens tools.jackson.core.unittest.json.async;
    opens tools.jackson.core.unittest.jsonptr;
    opens tools.jackson.core.unittest.read;
    opens tools.jackson.core.unittest.read.loc;
    opens tools.jackson.core.testutil.failure;
    opens tools.jackson.core.unittest.tofix;
    opens tools.jackson.core.unittest.tofix.async;
    opens tools.jackson.core.unittest.sym;
    opens tools.jackson.core.unittest.type;
    opens tools.jackson.core.unittest.util;
    opens tools.jackson.core.unittest.write;
}
