# Read-Easy Example Application

This folder contains a complete working example of how to use the Read-Easy framework.

## Structure

```
example/
├── standalone/
│   └── StandaloneTestApplication.java   # Main Spring Boot application
└── README.md                             # This file
```

## Quick Start

### 1. Run the Example Application

```bash
cd read-easy
./run-api-tests.sh
```

Or manually:

```bash
mvn compile test-compile
mvn exec:java -Dexec.classpathScope=test -Dexec.args="--spring.profiles.active=standalone"
```

### 2. Test the APIs

```bash
# Find all users (JDBC)
curl -X POST http://localhost:9999/read/list?queryId=users.findAll \
  -H "Content-Type: application/json" -d '{}'

# Paginated results
curl -X POST "http://localhost:9999/read/page?queryId=users.findAll&pageNum=1&pageSize=2" \
  -H "Content-Type: application/json" -d '{}'

# Count records
curl -X POST http://localhost:9999/read/count?queryId=users.findAll \
  -H "Content-Type: application/json" -d '{}'

# Find one record
curl -X POST http://localhost:9999/read/one?queryId=users.findAll \
  -H "Content-Type: application/json" -d '{}'

# MongoDB queries
curl -X POST http://localhost:9999/read/list?queryId=mongo-users.findAll \
  -H "Content-Type: application/json" -d '{}'
```

## Key Components

### StandaloneTestApplication.java

Shows how to configure a Read-Easy application:

```java
@SpringBootApplication(exclude = {
    // Exclude auto-configs when using mock readers
    MongoAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    // ... other exclusions
})
@Import({
    ConfiguredReadController.class,      // The REST controller
    DynaBeansAutoConfiguration.class,    // Dynamic beans support
    RESTExceptionHandler.class           // Proper HTTP error codes
})
public class StandaloneTestApplication {

    @Bean(name = "readEasyGeneralReaderMap")
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap() {
        // Provide your GeneralReader implementations
        Map<String, GeneralReader> readers = new HashMap<>();
        readers.put("default", new JdbcGeneralReader(...));
        readers.put("mongodb", new MongoGeneralReader(...));
        return readers;
    }

    @Bean
    @Primary
    public ReadEasyConfig readEasyConfig() {
        ReadEasyConfig config = new ReadEasyConfig();
        // Configure query files
        config.setQueryFiles(Map.of(
            "users", List.of("classpath:queries/users-queries.yaml")
        ));
        return config;
    }
}
```

### Query Files

Query files are YAML files that define your queries. See `src/test/resources/queries/valid/` for examples:

```yaml
# users-queries.yaml
queries:
  findAll:
    readerId: default
    rawFormat: YAML
    raw: |-
      {
        "nativeSQL": "SELECT * FROM users ORDER BY id"
      }

  findById:
    readerId: default
    rawFormat: YAML
    raw: |-
      {
        "nativeSQL": "SELECT * FROM users WHERE id = :id",
        "params": [{"name": "id", "value": "${params.id}"}]
      }
    params:
      id:
        typeFqcn: "java.lang.Integer"
        required: true
```

## REST Endpoints

| Endpoint | Description |
|----------|-------------|
| `POST /read/one?queryId=...` | Find single record |
| `POST /read/list?queryId=...` | Find all records |
| `POST /read/page?queryId=...&pageNum=1&pageSize=10` | Paginated results |
| `POST /read/count?queryId=...` | Count records |

## Important Notes

1. **RESTExceptionHandler**: Always import this to get proper HTTP 400 codes for validation errors
2. **Query ID Format**: `namespace.queryName` (e.g., `users.findAll`)
3. **Mock Readers**: This example uses mock readers; replace with real `JdbcGeneralReader` or `MongoGeneralReader` for production
