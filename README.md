# Overview

This project contains core low-level incremental ("streaming") parser and generator abstractions used by
[Jackson Data Processor](http://wiki.fasterxml.com/JacksonHome).
It also includes the default implementation of handler types (parser, generator) that handle JSON format.
The core abstractions are not JSON specific, although naming does contain 'JSON' in many places, due to historical reasons. Only packages that specifically contain word 'json' are JSON-specific.

This package is the base on which [Jackson data-binding](https://github.com/FasterXML/jackson-databind) package builds on.

Alternate data format implementations (like
[Smile (binary JSON)](https://github.com/FasterXML/jackson-dataformat-smile),
[XML](https://github.com/FasterXML/jackson-dataformat-xml)
and [CSV](https://github.com/FasterXML/jackson-dataformat-csv))
also build on this base package, implementing the core interfaces,
making it possible to use standard [data-binding package](https://github.com/FasterXML/jackson-databind) regardless of underlying data format.

Project contains versions 2.0 and above: source code for earlier (1.x) versions is available from [Codehaus](http://jackson.codehaus.org) SVN repository.

[![Build Status](https://fasterxml.ci.cloudbees.com/job/jackson-core-master/badge/icon)](https://fasterxml.ci.cloudbees.com/job/jackson-core-master/)

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

To use the package, you need to use following Maven dependency:

```xml
<dependency>
  <groupId>com.fasterxml.jackson.core</groupId>
  <artifactId>jackson-core</artifactId>
  <version>2.2.0</version>
</dependency>
```

or download jars from Maven repository or [Download page](http://wiki.fasterxml.com/JacksonDownload).
Core jar is a functional OSGi bundle, with proper import/export declarations.

Package has no external dependencies, except for testing (which uses `JUnit`).

## Non-Maven

For non-Maven use cases, you download jars from [Central Maven repository](http://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/) or [Download page](https://github.com/FasterXML/jackson-core/wiki/Downloads).

Core jar is also a functional OSGi bundle, with proper import/export declarations, so it can be use on OSGi container as is.

-----
# Use it!

## General

Usage typically starts with creation of a reusable (and thread-safe, once configured) `JsonFactory` instance:

```java
JsonFactory factory = new JsonFactory();
// configure, if necessary:
factory.enable(JsonParser.Feature.ALLOW_COMMENTS);
```

Alternatively, you have a `ObjectMapper` (from [Jackson Databind package](https://github.com/FasterXML/jackson-databind)) handy; if so, you can do:

```java
JsonFactory factory = objectMapper.getJsonFactory();
```

More information can be found from [Streaming API](http://wiki.fasterxml.com/JacksonStreamingApi
) at Jackson Wiki.

## Usage, simple reading

All reading is by using `JsonParser` (or its sub-classes, in case of data formats other than JSON),
instance of which is constructed by `JsonFactory`.

An example can be found from [Reading and Writing Event Streams](http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html)

## Usage, simple writing

All writing is by using `JsonGenerator` (or its sub-classes, in case of data formats other than JSON),
instance of which is constructed by `JsonFactory`:

An example can be found from [Reading and Writing Event Streams](http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html)

-----

# Further reading

* [Documentation](https://github.com/FasterXML/jackson-core/wiki/Documentation) has other project documentation
* [Jackson Project Home](http://wiki.fasterxml.com/JacksonHome)
