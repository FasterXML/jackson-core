# What is it?

This project contains core core low-level incremental ("streaming") parser and generator abstractions used by
[Jackson Data Processor](http://wiki.fasterxml.com/JacksonHome).

It also includes the default implementation of handler types (parser, generator) that handle JSON format.
The core abstractions are not JSON specific, although naming does contain 'JSON' in many places, due to historical reasons. Only packages that specifically contain word 'json' are JSON-specific.

This package is the base on which [Jackson data-binding](/FasterXML/jackson-annotations)
package builds on.
Alternate data format implementations (like
[Smile (binary JSON)](/FasterXML/jackson-dataformat-smile),
[XML](/FasterXML/jackson-dataformat-xml)
and [CSV](/FasterXML/jackson-dataformat-csv))
build on this base package, implementing the core interfaces, and making it possible to use standard [data-binding package](/FasterXML/jackson-databind).

### Differences from Jackson 1.x

Project contains versions 2.0 and above: source code for earlier (1.x) versions is available from [Codehaus](http://jackson.codehaus.org) SVN repository

Note that the main differences compared to 1.0 core jar are:

* Maven build instead of Ant
* Annotations carved out to a separate package (that this package depends on)
* Java package is now `com.fasterxml.jackson.core` (instead of `org.codehaus.jackson`)

----

# Get it!

## Maven

Functionality of this package is contained in 
Java package `com.fasterxml.jackson.core`.

To use databinding, you need to use following Maven dependency:

    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-core</artifactId>
      <version>2.0.0</version>
    </dependency>

or download jars from Maven repository or [Download page](wiki.fasterxml.com/JacksonDownload).
Core jar is a functional OSGi bundle, with proper import/export declarations.

Package has no external dependencies, except for testing (which uses `JUnit`).

## Non-Maven

For non-Maven use cases, you download jars from [Central Maven repository](http://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/) or [Download page](jackson-binding/wiki/JacksonDownload).

Core jar is also a functional OSGi bundle, with proper import/export declarations, so it can be use on OSGi container as is.

-----

# Use it!

## General

Usage typically starts with creation of a reusable (and thread-safe, once configured) `JsonFactory` instance:

    JsonFactory factory = new JsonFactory();
    // configure, if necessary:
    factory.enable(JsonParser.Feature.ALLOW_COMMENTS);

Alternatively, you have a `ObjectMapper` (from [Jackson Databind package](jackson-databind)) handy; if so, you can do:

    JsonFactory factory = objectMapper.getJsonFactory();

## Usage, simple reading

All reading is by using `JsonParser` (or its sub-classes, in case of data formats other than JSON),
instance of which is constructed by `JsonFactory':

(TO BE WRITTEN)

## Usage, simple writing

All writing is by using `JsonGenerator` (or its sub-classes, in case of data formats other than JSON),
instance of which is constructed by `JsonFactory':

(TO BE WRITTEN)

-----

# Further reading

* [Jackson Project Home](http://wiki.fasterxml.com/JacksonHome)
* [Documentation](http://wiki.fasterxml.com/JacksonDocumentation)
 * [JavaDocs](http://wiki.fasterxml.com/JacksonJavaDocs)
* [Downloads](http://wiki.fasterxml.com/JacksonDownload)

