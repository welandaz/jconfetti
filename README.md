# jconfetti

A Java parser for the [Confetti](https://confetti.hgs3.me/) configuration language.

Confetti is a minimalistic, untyped, and unopinionated configuration language designed for human-editable
configuration files. See the [specification](https://confetti.hgs3.me/specification) and
[examples](https://confetti.hgs3.me/learn/) to learn more.

## Features

- Zero external dependencies
- Java 8+ compatible
- Full [Confetti 1.0 specification](https://confetti.hgs3.me/specification) compliance (passes the entire conformance test suite)
- All three official extensions: C-style comments, expression arguments, and punctuator arguments
- Strict UTF-8 validation
- Lightweight API (`ConfigurationUnit` / `Directive`)
- Reflection-based object mapping

## Installation

### Gradle

```kotlin
dependencies {
    implementation("io.github.welandaz:jconfetti:1.0.0")
}
```

### Maven

```xml
<dependency>
    <groupId>io.github.welandaz</groupId>
    <artifactId>jconfetti</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

Given a configuration file `app.conf`:

```
server {
    host 127.0.0.1
    port 8080
}

database {
    url "jdbc:postgresql://localhost/mydb"
    pool_size 10
}
```

Parse it in Java:

```java
import io.github.welandaz.*;
import java.nio.file.Paths;

final Confetti confetti = new Confetti();
final ConfigurationUnit config = confetti.parse(Paths.get("app.conf"));

for (final Directive directive : config.directives()) {
    System.out.println(directive.arguments().get(0)); // "server", "database"

    for (final Directive child : directive.subdirectives()) {
        final String key = child.arguments().get(0);
        final String value = child.arguments().get(1);
        System.out.println("  " + key + " = " + value);
    }
}
```

You can also parse a string directly:

```java
final ConfigurationUnit config = new Confetti().parse("login johndoe ; password somepass123");
```

Or create a parser with custom default options:

```java
final Confetti confetti = new Confetti(
        ConfettiOptions.builder()
                .cStyleComments(true)
                .build());

final ConfigurationUnit config = confetti.parse(Paths.get("app.conf"));
```

## Extensions

Confetti defines three optional extensions. Enable them via `ConfettiOptions`:

```java
final ConfettiOptions options = ConfettiOptions.builder()
        .cStyleComments(true)        // C-style // and /* */ comments
        .expressionArguments(true)   // Parenthesized expressions: func(x + 1)
        .punctuators("=", ":=", "+=")  // Custom punctuator arguments
        .build();

final ConfigurationUnit config = new Confetti(options).parse(Paths.get("app.conf"));
```

## Object mapping

The library can also map a parsed configuration directly onto Java objects using reflection:

```java
class AppConfig {
    Server server;
    Database database;
}

class Server {
    String host;
    int port;
    boolean debug;
}

class Database {
    String url;

    @ConfettiName("pool_size")
    int poolSize;
}

// Parse and map in one step
final Confetti confetti = new Confetti();
final AppConfig config = confetti.map(confetti.parse(Paths.get("app.conf")), AppConfig.class);

System.out.println(config.server.host);
System.out.println(config.server.port);
System.out.println(config.database.poolSize);
```

You can also parse and map directly:

```java
final AppConfig config = new Confetti().map(Paths.get("app.conf"), AppConfig.class);
```

### Supported field types

| Type | Mapping |
|------|---------|
| `String` | Directive value argument |
| `int` / `Integer`, `long` / `Long`, `double` / `Double`, `float` / `Float` | Parsed from value argument |
| `boolean` / `Boolean` | Accepts `true`/`false`, `yes`/`no`, `on`/`off`, `1`/`0` |
| Any class with no-arg constructor | Populated from subdirectives |
| `List<T>` | Collects repeated directives with the same name |
| `Map<String, String>` | Populated from `name key value` directives |

## Building from source

```bash
./gradlew build
```

## Running tests

The test suite runs the full [Confetti conformance tests](https://github.com/hgs3/confetti):

```bash
./gradlew test
```

## Publishing

This project is configured to publish as:

- `groupId`: `io.github.welandaz`
- `artifactId`: `jconfetti`

The release workflow is located at [.github/workflows/publish.yml](https://github.com/welandaz/jconfetti/blob/main/.github/workflows/publish.yml).

Before publishing, update the version in [build.gradle.kts](https://github.com/welandaz/jconfetti/blob/main/build.gradle.kts) and push that commit to GitHub.

Then run the workflow manually from the GitHub Actions UI.

The workflow:

1. reads the version from `build.gradle.kts`
2. runs the test suite
3. publishes that version to Maven Central
4. creates and pushes a Git tag with the same version only after publishing succeeds

The publish step runs:

```bash
./gradlew test publishToCentral
```

The task uploads the signed Maven publication and then requests automatic publication through Sonatype Central.
If `build.gradle.kts` contains `version = "1.0.0"`, the workflow will publish `1.0.0` and then create the Git tag `1.0.0`.

## License

[MIT](LICENSE)
