# Read-Easy Example Application

A complete working example demonstrating how to use the Read-Easy framework to expose database queries as REST endpoints.

## Project Structure

```
read-easy-example/
├── pom.xml                                          # Maven configuration
├── README.md                                        # This file
├── run-tests.sh                                     # Script to test APIs
└── src/
    └── main/
        ├── java/com/example/readeasy/
        │   ├── ReadEasyExampleApplication.java      # Main Spring Boot app
        │   └── config/
        │       └── MockReadersConfig.java           # Mock reader configuration
        └── resources/
            ├── application.yaml                     # Application configuration
            └── queries/
                ├── users-queries.yaml               # JDBC queries
                └── mongodb-users-queries.yaml       # MongoDB queries
```

## Quick Start

### 1. Build and Run

```bash
cd read-easy-example
mvn spring-boot:run
```

### 2. Test the APIs

```bash
# List all users (JDBC)
curl -X POST http://localhost:8080/read/list?queryId=users.findAll \
  -H "Content-Type: application/json" -d '{}'

# Paginated users
curl -X POST "http://localhost:8080/read/page?queryId=users.findAll&pageNum=1&pageSize=2" \
  -H "Content-Type: application/json" -d '{}'

# Count users
curl -X POST http://localhost:8080/read/count?queryId=users.findAll \
  -H "Content-Type: application/json" -d '{}'

# Find one user
curl -X POST http://localhost:8080/read/one?queryId=users.findAll \
  -H "Content-Type: application/json" -d '{}'

# MongoDB queries
curl -X POST http://localhost:8080/read/list?queryId=mongo-users.findAll \
  -H "Content-Type: application/json" -d '{}'
```

Or run the test script:
```bash
./run-tests.sh
```

## Key Components

### 1. Main Application (`ReadEasyExampleApplication.java`)

```java
@SpringBootApplication
@Import({
    ConfiguredReadController.class,      // REST endpoints
    DynaBeansAutoConfiguration.class,    // Dynamic beans
    RESTExceptionHandler.class           // Proper HTTP error codes
})
public class ReadEasyExampleApplication { ... }
```

### 2. Reader Configuration (`MockReadersConfig.java`)

Provides the `readEasyGeneralReaderMap` bean that maps reader IDs to implementations:

```java
@Bean(name = "readEasyGeneralReaderMap")
public Map<String, GeneralReader> readEasyGeneralReaderMap() {
    Map<String, GeneralReader> readers = new HashMap<>();
    readers.put("default", new MockJdbcReader());    // or JdbcGeneralReader
    readers.put("mongodb", new MockMongoReader());   // or MongoGeneralReader
    return readers;
}
```

### 3. Application Configuration (`application.yaml`)

```yaml
readeasy:
  queryFiles:
    users:                                    # Namespace
      - classpath:queries/users-queries.yaml  # Query file location
    mongo-users:
      - classpath:queries/mongodb-users-queries.yaml
```

### 4. Query Files

Queries are defined in YAML format:

```yaml
queries:
  findAll:                    # Query name -> accessible as {namespace}.findAll
    readerId: default         # Which reader to use
    rawFormat: YAML           # Format of the 'raw' field
    raw: |-                   # The actual query
      {
        "nativeSQL": "SELECT * FROM users ORDER BY id"
      }
```

## REST Endpoints

| Endpoint | Description | Example |
|----------|-------------|---------|
| `POST /read/one` | Find single record | `?queryId=users.findById` |
| `POST /read/list` | Find all records | `?queryId=users.findAll` |
| `POST /read/page` | Paginated results | `?queryId=users.findAll&pageNum=1&pageSize=10` |
| `POST /read/count` | Count records | `?queryId=users.findAll` |

## Using Real Database Readers

Replace mock readers with real implementations:

### JDBC (PostgreSQL, MySQL, etc.)

```java
@Bean(name = "readEasyGeneralReaderMap")
public Map<String, GeneralReader> readEasyGeneralReaderMap(JdbcTemplate jdbcTemplate) {
    Map<String, GeneralReader> readers = new HashMap<>();
    readers.put("default", new JdbcGeneralReader(new SimpleJdbcTemplate(jdbcTemplate)));
    return readers;
}
```

### MongoDB

```java
@Bean(name = "readEasyGeneralReaderMap")
public Map<String, GeneralReader> readEasyGeneralReaderMap(MongoTemplate mongoTemplate) {
    Map<String, GeneralReader> readers = new HashMap<>();
    readers.put("mongodb", new MongoGeneralReader(mongoTemplate));
    return readers;
}
```

## Dependencies

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>com.github.ifrugal</groupId>
    <artifactId>read-easy</artifactId>
    <version>${app-building-blocks.version}</version>
</dependency>
```

## Important Notes

1. **RESTExceptionHandler**: Always import to get HTTP 400 for validation errors
2. **Query ID Format**: `{namespace}.{queryName}` (e.g., `users.findAll`)
3. **Bean Name**: The reader map must be named `readEasyGeneralReaderMap`
4. **DynaBeansConfig**: Required by DynaBeansAutoConfiguration
