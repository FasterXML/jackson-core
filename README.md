# Overview

This project contains core low-level incremental ("streaming") parser and generator abstractions used by
[Jackson Data Processor](https://github.com/FasterXML/jackson).
It also includes the default implementation of handler types (parser, generator) that handle JSON format.
The core abstractions are not JSON specific, although naming does contain 'JSON' in many places, due to historical reasons. Only packages that specifically contain word 'json' are JSON-specific.

This package is the base on which [Jackson data-binding](https://github.com/FasterXML/jackson-databind) package builds on.
It is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Alternate data format implementations (like
[Smile (binary JSON)](https://github.com/FasterXML/jackson-dataformats-binary/tree/master/smile),
[XML](https://github.com/FasterXML/jackson-dataformat-xml),
[CSV](https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv),
[Protobuf](https://github.com/FasterXML/jackson-dataformats-binary/tree/master/protobuf),
and [CBOR](https://github.com/FasterXML/jackson-dataformats-binary/tree/master/cbor))
also build on this base package, implementing the core interfaces,
making it possible to use standard [data-binding package](https://github.com/FasterXML/jackson-databind) regardless of underlying data format.

Project contains versions 2.0 and above: source code for earlier (1.x) versions can be found from
[Jackson-1](../../../jackson-1) github repo.

## Status

| Type | Status |
| ---- | ------ |
| Build (CI) | [![Build (github)](https://github.com/FasterXML/jackson-core/actions/workflows/main.yml/badge.svg)](https://github.com/FasterXML/jackson-core/actions/workflows/main.yml) |
| Artifact | [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.core/jackson-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.fasterxml.jackson.core/jackson-core) |
| OSS Sponsorship | [![Tidelift](https://tidelift.com/badges/package/maven/com.fasterxml.jackson.core:jackson-core)](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-core?utm_source=maven-com-fasterxml-jackson-core-jackson-core&utm_medium=referral&utm_campaign=readme) |
| Javadocs | [![Javadoc](https://javadoc.io/badge/com.fasterxml.jackson.core/jackson-core.svg)](https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-core) |
| Code coverage (2.15) | [![codecov.io](https://codecov.io/github/FasterXML/jackson-core/coverage.svg?branch=2.15)](https://codecov.io/github/FasterXML/jackson-core?branch=2.15) |
| CodeQ (ClusterFuzz) | [![Fuzzing Status](https://oss-fuzz-build-logs.storage.googleapis.com/badges/jackson-core.svg)](https://bugs.chromium.org/p/oss-fuzz/issues/list?sort=-opened&can=1&q=proj:jackson-core) |

# Get it!

## Maven

Functionality of this package is contained in 
Java package `com.fasterxml.jackson.core`.

To use the package, you need to use following Maven dependency:

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>${jackson.version.core}</version>
</dependency>
```

or download jars from Maven repository or links on [Wiki](../../wiki).
Core jar is a functional OSGi bundle, with proper import/export declarations.

Package has no external dependencies, except for testing (which uses `JUnit`).

## Non-Maven

For non-Maven use cases, you download jars from [Central Maven repository](https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/).

Core jar is also a functional OSGi bundle, with proper import/export declarations, so it can be use on OSGi container as is.

Jackson 2.10 and above include `module-info.class` definitions so the jar is also a proper Java module (JPMS).

Jackson 2.12 and above include additional Gradle 6 Module Metadata for version alignment with Gradle.

-----
# Use it!

## General

Usage typically starts with creation of a reusable (and thread-safe, once configured) `JsonFactory` instance:

```java
// Builder-style since 2.10:
JsonFactory factory = JsonFactory.builder()
// configure, if necessary:
     .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
     .build();

// older 2.x mechanism, still supported for 2.x
JsonFactory factory = new JsonFactory();
// configure, if necessary:
factory.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS);
```

Alternatively, you have an `ObjectMapper` (from [Jackson Databind package](https://github.com/FasterXML/jackson-databind)) handy; if so, you can do:

```java
JsonFactory factory = objectMapper.getFactory();
```

## Usage, simple reading

All reading is by using `JsonParser` (or its sub-classes, in case of data formats other than JSON),
instance of which is constructed by `JsonFactory`.

An example can be found from [Reading and Writing Event Streams](http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html)

## Usage, simple writing

All writing is by using `JsonGenerator` (or its sub-classes, in case of data formats other than JSON),
instance of which is constructed by `JsonFactory`:

An example can be found from [Reading and Writing Event Streams](http://www.cowtowncoder.com/blog/archives/2009/01/entry_132.html)

-----

## Compatibility

### JDK

Jackson-core package baseline JDK requirement:

* Versions 2.0 - 2.13 require JDK 6
* Versions 2.14 and above require JDK 8

### Android

List is incomplete due to recent addition of compatibility checker.

* 2.13: Android SDK 19+
* 2.14: Android SDK 26+

for information on Android SDK versions to Android Release names see [Android version history](https://en.wikipedia.org/wiki/Android_version_history)

-----

## Release Process

Starting with Jackson 2.15, releases of this module will be [SLSA](https://slsa.dev/) compliant: see issue #844 for details.

Release process is triggered by

    ./release.sh

script which uses Maven Release plug-in under the hood (earlier release plug-in was directly invoked).

-----

## Support

### Community support

Jackson components are supported by the Jackson community through mailing lists, Gitter forum, Github issues. See [Participation, Contributing](../../../jackson#participation-contributing) for full details.

### Enterprise support

Available as part of the [Tidelift](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-databind) Subscription.

The maintainers of `jackson-core` and thousands of other packages are working with Tidelift to deliver commercial support and maintenance for the open source dependencies you use to build your applications. Save time, reduce risk, and improve code health, while paying the maintainers of the exact dependencies you use. [Learn more.](https://tidelift.com/subscription/pkg/maven-com-fasterxml-jackson-core-jackson-core?utm_source=maven-com-fasterxml-jackson-core-jackson-core&utm_medium=referral&utm_campaign=enterprise&utm_term=repo)

-----

# Further reading

## Differences from Jackson 1.x

Project contains versions 2.0 and above: source code for the latest 1.x version (1.9.13) is available from
[FasterXML/jackson-1](https://github.com/FasterXML/jackson-1) repo (unmaintained).

Note that the main differences compared to 1.0 core jar are:

* Maven build instead of Ant
* Annotations carved out to a separate package (that this package depends on)
* Java package is now `com.fasterxml.jackson.core` (instead of `org.codehaus.jackson`)

## Links

* Project  [Wiki](../../wiki) has JavaDocs and links to downloadable artifacts
* [Jackson (portal)](https://github.com/FasterXML/jackson) has links to all FasterXML-maintained "official" Jackson components
* [Jackson Docs](https://github.com/FasterXML/jackson-docs) is the portal/hub for all kinds of Jackson documentation
