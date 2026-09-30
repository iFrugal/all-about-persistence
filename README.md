# all-about-persistence




![Maven Central](https://img.shields.io/maven-central/v/com.github.ifrugal/all-about-persistence?style=for-the-badge)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=iFrugal_all-about-persistence&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=iFrugal_all-about-persistence)
[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=iFrugal_all-about-persistence)

## Spring Boot compatibility

There is a single release line, 1.0.x, published from master.
It receives all fixes.
There is no separate Boot 3 line and no separate Boot 4 line.

The libraries compile against Spring Boot 3.5.x, and all Spring dependencies are provided scope.
The consuming application supplies Spring Boot.
Continuous integration runs the full test suite on the matrix Spring Boot 3.5.x and 4.1.x, each on JDK 21 and JDK 25.
The bytecode target is Java 21.

| Library version line | Spring Boot | Spring Framework | Jackson used internally | Status |
|---|---|---|---|---|
| 1.0.x | 3.5.x | 6.2.x | Jackson 2 | tested in CI |
| 1.0.x | 4.0.x | 7.0.x | Jackson 2 | expected to work, not in CI |
| 1.0.x | 4.1.x | 7.0.x | Jackson 2 | tested in CI |
| 1.0.x | 3.0.x to 3.4.x | 6.0.x to 6.1.x | Jackson 2 | not tested, these Boot lines are end of life |

Spring Boot 3.5 open source support ended 2026-06-30.
Boot 4.0 ends 2026-12-31.
Boot 4.1 ends 2027-07-31.
Consumers should move to Boot 4.1.

### Auto-configuration

read-easy, dyna-beans-injector, just-schedule-it-core and the-transporter register their auto-configurations through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
Hosts no longer need a manual `@Import` of `lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration`.
An existing manual import is harmless and can be removed.

## Jackson

The libraries own their Jackson 2 dependency (groupId `com.fasterxml.jackson`).
They never depend on the host application's auto-configured `ObjectMapper` bean.
The internal mapper is created by `lazydevs.mapper.utils.SerDe` in persistence-utils.

### Hosts on Spring Boot 4

Nothing needs to be configured.
Jackson 3 (`tools.jackson`) remains the host's default mapper for HTTP message conversion.
Jackson 2 is present on the classpath only as a library-internal dependency.
The host does not need `spring-boot-jackson2`.
The two Jackson generations coexist because they use different packages and coordinates and share the `jackson-annotations` artifact.

### HTTP responses

Response bodies produced by read-easy controllers are serialized by the host's message converter.
The host's Jackson (3 on Boot 4, 2 on Boot 3) and its `spring.jackson.*` properties therefore apply to response formatting.

### Why not Jackson 3 now

Two public APIs expose Jackson 2 types.
`SerDe.getOBJECT_MAPPER()` returns `com.fasterxml.jackson.databind.ObjectMapper`, and conman's `JsonSchemaValidator` returns `JsonNode`.
In addition, crud-service depends on json-patch and conman depends on networknt json-schema-validator, both of which support Jackson 2 only.
A switch to Jackson 3 would be a breaking change and would drop Boot 3.5 hosts.
It is deferred to a future major version.

### Native images

The libraries add no new reflection in this change and ship no GraalVM `RuntimeHints`.
dyna-beans-injector and the read-easy YAML configuration instantiate classes named by the host's configuration through reflection.
A host building a native image must therefore register hints for the classes it names in YAML.
This is unchanged from previous versions.
