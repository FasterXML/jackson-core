# jackson-core JMH benchmarks

Standalone [JMH](https://github.com/openjdk/jmh) project, not part of the main build or release.
It benchmarks the `jackson-core` jar installed in your local Maven repository.

## Build and run

From the repository root:

```bash
./mvnw install -DskipTests              # install the jackson-core snapshot under test
./mvnw -f jmh/pom.xml package           # builds jmh/target/benchmarks.jar
java -jar jmh/target/benchmarks.jar StringWriteBench
```

`benchmarks.jar` bundles `jackson-core`, so to compare two branches, build it once per branch
and keep a copy of each:

```bash
git switch 3.x && ./mvnw install -DskipTests && ./mvnw -f jmh/pom.xml package
cp jmh/target/benchmarks.jar /tmp/bench-base.jar
git switch my-branch && ./mvnw install -DskipTests && ./mvnw -f jmh/pom.xml package
cp jmh/target/benchmarks.jar /tmp/bench-branch.jar
```

To benchmark a different `jackson-core` version, pass `-Djackson.version=...`.

## Useful JMH options

```bash
# narrow the parameter matrix
java -jar jmh/target/benchmarks.jar 'StringWriteBench.utf8String' -p length=8,24 -p content=ascii

# more forks for small differences
java -jar jmh/target/benchmarks.jar StringWriteBench -f 10

# C2-only compilation: removes tiered-profile noise when comparing short-string code shapes
java -jar jmh/target/benchmarks.jar StringWriteBench -jvmArgsAppend '-XX:-TieredCompilation -Xbatch'

# machine-readable results
java -jar jmh/target/benchmarks.jar StringWriteBench -rf json -rff results.json
```

## Benchmarks

- `StringWriteBench`: `writeString(String)`, `writeString(char[], int, int)` and
  `writeName(String)` on `UTF8JsonGenerator` and `WriterBasedJsonGenerator`, for ASCII,
  escaped and Latin-1 values of several lengths ([#1680]).
  By default (`polluteProfile=true`) it touches a UTF-16 `String` before measuring, as almost
  every real application will have done; with `-p polluteProfile=false`, `String.charAt` loops
  can be specialized for Latin-1 only, which is not representative.

Short-string results are sensitive to CPU (in particular, AVX-512 support for inlined short
array copies), JDK version and JIT profile, so check changes on more than one setup.

[#1680]: https://github.com/FasterXML/jackson-core/issues/1680
