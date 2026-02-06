# Read-Easy

[![Maven Central](https://img.shields.io/maven-central/v/com.github.ifrugal/read-easy?style=flat-square)](https://search.maven.org/artifact/com.github.ifrugal/read-easy)

**Read-Easy** is a configuration-driven query framework that lets you expose database queries as REST endpoints using simple YAML configuration - no Java code required for basic use cases.

## Features

- **Zero-Code Query Endpoints**: Define queries in YAML, get REST APIs automatically
- **Multi-Database Support**: Works with JDBC, MongoDB, REST APIs, and Files
- **Template-Based Queries**: Use FreeMarker templates for dynamic query generation
- **Built-in Pagination**: Automatic support for paginated results
- **Data Export**: Stream large datasets as CSV with batch processing
- **Result Transformation**: Transform query results using templates or JavaScript
- **Caching Support**: Optional JavaScript-based caching layer
- **Spring Boot Integration**: Auto-configuration for seamless Spring Boot apps

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>com.github.ifrugal</groupId>
    <artifactId>read-easy</artifactId>
    <version>1.0.46-SNAPSHOT</version>
</dependency>
```

### 2. Configure Application Properties

```yaml
# application.yml
readeasy:
  # Option A: Single default reader
  generalReaderInit:
    fqcn: lazydevs.persistence.impl.jdbc.JdbcGeneralReader
    args:
      - beanRef:simpleJdbcTemplate

  # Option B: Multiple named readers
  generalReaders:
    default:
      fqcn: lazydevs.persistence.impl.jdbc.JdbcGeneralReader
      args:
        - beanRef:simpleJdbcTemplate
    mongodb:
      fqcn: lazydevs.persistence.impl.mongodb.MongoGeneralReader
      args:
        - beanRef:mongoTemplate

  # Query file locations (namespace -> file paths)
  queryFiles:
    users:
      - classpath:queries/users.yaml
    products:
      - classpath:queries/products.yaml
```

### 3. Define Your Queries

Create `src/main/resources/queries/users.yaml`:

```yaml
dynaBeans: {}

queries:
  # Simple query to find user by ID
  byId:
    readerId: default
    raw: |
      {
        "nativeSQL": "SELECT * FROM users WHERE id = :id",
        "params": [
          {"name": "id", "value": "${params.id}"}
        ]
      }
    params:
      id:
        required: true
        type: INTEGER

  # List all active users with pagination support
  activeUsers:
    readerId: default
    raw: |
      {
        "nativeSQL": "SELECT * FROM users WHERE status = 'ACTIVE' ORDER BY created_at DESC"
      }

  # Search users with dynamic filters
  search:
    readerId: default
    raw: |
      {
        "nativeSQL": "SELECT * FROM users WHERE 1=1 <#if params.name??> AND name LIKE :name</#if> <#if params.email??> AND email = :email</#if>",
        "params": [
          <#if params.name??>{"name": "name", "value": "%${params.name}%"}</#if>
          <#if params.email??><#if params.name??>,</#if>{"name": "email", "value": "${params.email}"}</#if>
        ]
      }
```

### 4. Use the REST APIs

Once configured, Read-Easy automatically exposes these endpoints:

```bash
# Find one record
POST /read/one?queryId=users.byId
Content-Type: application/json
{"id": 123}

# List all records
POST /read/list?queryId=users.activeUsers
Content-Type: application/json
{}

# Paginated results
POST /read/page?queryId=users.activeUsers&pageNum=1&pageSize=20
Content-Type: application/json
{}

# Count records
POST /read/count?queryId=users.activeUsers
Content-Type: application/json
{}

# Export to CSV
POST /read/export?queryId=users.activeUsers
Content-Type: application/json
{}
```

## API Reference

### Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/read/one` | POST | Returns a single record matching the query |
| `/read/list` | POST | Returns all records matching the query |
| `/read/page` | POST | Returns paginated results |
| `/read/count` | POST | Returns count of matching records |
| `/read/export` | POST | Streams results as downloadable file |
| `/read/register` | POST | Dynamically register new queries (admin) |

### Common Parameters

| Parameter | Type | Description |
|-----------|------|-------------|
| `queryId` | Query | Required. Format: `namespace.queryName` |
| `pageNum` | Query | Page number (1-indexed) for `/page` endpoint |
| `pageSize` | Query | Records per page (default: 10) |
| `orderby` | Query | Field name to sort by |
| `orderdir` | Query | Sort direction: `asc` or `desc` |
| `cache` | Query | Enable/disable caching (default: true) |

### Request Body

All endpoints accept a JSON object with query parameters:

```json
{
  "id": 123,
  "name": "John",
  "status": "ACTIVE"
}
```

## Configuration Reference

### ReadEasyConfig Properties

```yaml
readeasy:
  # Single reader configuration
  generalReaderInit:
    fqcn: <fully-qualified-class-name>
    args: [<constructor-arguments>]

  # Multiple readers configuration
  generalReaders:
    <readerId>:
      fqcn: <fully-qualified-class-name>
      args: [<constructor-arguments>]

  # Query files by namespace
  queryFiles:
    <namespace>:
      - <file-path>

  # Request context supplier (for injecting request data)
  requestContextSupplierInit:
    fqcn: <supplier-class>

  # Global context supplier (for app-level data)
  globalContextSupplierInit:
    fqcn: <supplier-class>

  # Default operation instructions
  operationInstruction:
    ONE:
      statusCodeWhenNoRecordsFound: 404
    EXPORT:
      exportFileNameTemplate: "export-${.now?string('yyyyMMdd')}.csv"
      exportTemplate: "<#list list as row>..."
      readBatchSize: 1000
      countCheckRequired: true
      maxCountToExport: 100000

  # Enable admin endpoints
  admin:
    enabled: false
```

### Query Configuration

```yaml
queries:
  queryName:
    # Which reader to use (default: "default")
    readerId: default

    # Query format (JSON, YAML, XML)
    rawFormat: JSON

    # The query template (FreeMarker syntax)
    raw: |
      {
        "nativeSQL": "SELECT * FROM table WHERE id = :id"
      }

    # Parameter definitions
    params:
      paramName:
        required: true
        type: STRING
        defaultValue: "default"
        pattern: "^[a-zA-Z]+$"

    # Optional caching configuration
    cacheFetchInstruction:
      jsFunctionName: "getCachedData"
      args: ["param1", "param2"]

    # Transform each row of results
    rowTransformer:
      template: |
        {
          "fullName": "${firstName} ${lastName}",
          "email": "${email}"
        }

    # Per-query operation settings
    operationInstruction:
      ONE:
        statusCodeWhenNoRecordsFound: 200
      EXPORT:
        exportFileNameTemplate: "users-${params.date}.csv"
```

## Template Variables

Within query templates and transformers, you have access to:

| Variable | Description |
|----------|-------------|
| `params` | Request parameters passed in the body |
| `request` | Request context (if `requestContextSupplierInit` configured) |
| `global` | Global context (if `globalContextSupplierInit` configured) |
| `sort` | Sort specification (auto-generated from orderby/orderdir) |

### FreeMarker Examples

```ftl
<#-- Conditional WHERE clause -->
<#if params.status??>AND status = :status</#if>

<#-- Default value -->
${params.limit!10}

<#-- List iteration -->
<#list params.ids as id>${id}<#sep>,</#list>

<#-- Date formatting -->
${.now?string('yyyy-MM-dd')}
```

## Working with Different Databases

### JDBC (SQL Databases)

```yaml
# application.yml
readeasy:
  generalReaderInit:
    fqcn: lazydevs.persistence.impl.jdbc.JdbcGeneralReader
    args:
      - beanRef:simpleJdbcTemplate

# queries/orders.yaml
queries:
  recent:
    raw: |
      {
        "nativeSQL": "SELECT * FROM orders WHERE created_at > :since ORDER BY created_at DESC",
        "params": [
          {"name": "since", "value": "${params.since}"}
        ]
      }
```

### MongoDB

```yaml
# application.yml
readeasy:
  generalReaders:
    mongo:
      fqcn: lazydevs.persistence.impl.mongodb.MongoGeneralReader
      args:
        - beanRef:mongoTemplate

# queries/products.yaml
queries:
  byCategory:
    readerId: mongo
    raw: |
      {
        "collectionName": "products",
        "query": {
          "category": "${params.category}",
          "active": true
        }
      }
```

### REST API Backend

```yaml
# application.yml
readeasy:
  generalReaders:
    api:
      fqcn: lazydevs.persistence.impl.rest.RestGeneralReader
      args:
        - beanRef:restTemplate

# queries/external.yaml
queries:
  weather:
    readerId: api
    raw: |
      {
        "url": "https://api.weather.com/v1/current",
        "method": "GET",
        "queryParams": {
          "city": "${params.city}"
        }
      }
```

## Advanced Features

### Result Transformation

Transform query results before returning to the client:

```yaml
queries:
  usersWithFullName:
    raw: |
      {"nativeSQL": "SELECT first_name, last_name, email FROM users"}
    rowTransformer:
      template: |
        {
          "name": "${first_name} ${last_name}",
          "email": "${email}",
          "initials": "${first_name[0]}${last_name[0]}"
        }
```

### Custom Export Templates

```yaml
queries:
  exportUsers:
    raw: |
      {"nativeSQL": "SELECT * FROM users"}
    operationInstruction:
      EXPORT:
        exportFileNameTemplate: "users-${.now?string('yyyyMMdd-HHmmss')}.csv"
        exportTemplate: |
          <#if list?is_first>name,email,status
          </#if><#list list as row>${row.name},${row.email},${row.status}
          </#list>
        readBatchSize: 5000
```

### Caching with JavaScript

```yaml
dynaBeans:
  cacheScript:
    type: SCRIPT
    script: |
      function getUsersFromCache(status) {
        // Return cached data or null
        return CacheManager.get('users_' + status);
      }

queries:
  cachedUsers:
    raw: |
      {"nativeSQL": "SELECT * FROM users WHERE status = :status"}
    cacheFetchInstruction:
      jsFunctionName: getUsersFromCache
      args: ["status"]
```

### Request Context Injection

```java
// Create a request context supplier
@Component
public class RequestContextSupplier implements Supplier<Map<String, Object>> {
    @Override
    public Map<String, Object> get() {
        return Map.of(
            "userId", SecurityContextHolder.getContext().getAuthentication().getName(),
            "tenantId", TenantContext.getCurrentTenant()
        );
    }
}
```

```yaml
# application.yml
readeasy:
  requestContextSupplierInit:
    fqcn: com.example.RequestContextSupplier
```

```yaml
# queries/secure.yaml
queries:
  myOrders:
    raw: |
      {
        "nativeSQL": "SELECT * FROM orders WHERE user_id = :userId",
        "params": [{"name": "userId", "value": "${request.userId}"}]
      }
```

## Error Handling

Read-Easy returns appropriate HTTP status codes:

| Status | Condition |
|--------|-----------|
| 200 | Success |
| 400 | Invalid parameters or validation error |
| 404 | Query not found or no record found (for `/one`) |
| 500 | Server error |

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      REST Client                            │
└─────────────────────────┬───────────────────────────────────┘
                          │ HTTP Request
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                ConfiguredReadController                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │ /read/one   │  │ /read/list  │  │ /read/page|count|   │ │
│  └─────────────┘  └─────────────┘  │     export          │ │
│                                     └─────────────────────┘ │
└─────────────────────────┬───────────────────────────────────┘
                          │
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
┌─────────────────┐ ┌───────────┐ ┌─────────────────┐
│  ReadEasyConfig │ │  ParamVal │ │ TemplateEngine  │
│  (YAML Queries) │ │  idator   │ │  (FreeMarker)   │
└─────────────────┘ └───────────┘ └─────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    GeneralReader Interface                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
    ┌─────────────────────┼─────────────────────┐
    ▼                     ▼                     ▼
┌─────────┐        ┌───────────┐         ┌──────────┐
│  JDBC   │        │  MongoDB  │         │   REST   │
│ Reader  │        │  Reader   │         │  Reader  │
└─────────┘        └───────────┘         └──────────┘
    │                     │                     │
    ▼                     ▼                     ▼
┌─────────┐        ┌───────────┐         ┌──────────┐
│   SQL   │        │  MongoDB  │         │ External │
│   DB    │        │  Cluster  │         │   API    │
└─────────┘        └───────────┘         └──────────┘
```

## Troubleshooting

### Query Not Found
```
ValidationException: No query found registered for queryId = users.byId
```
- Check that `queryFiles` in application.yml includes the correct namespace
- Verify the YAML file path is correct
- Ensure the query name matches exactly

### Template Parsing Error
```
FreeMarkerException: Syntax error in template
```
- Validate FreeMarker syntax in your `raw` query
- Check for unclosed tags or missing `</#if>` closures
- Ensure JSON is valid after template rendering

### Reader Not Found
```
IllegalStateException: No reader found register against readerId = xyz
```
- Verify `generalReaders` configuration in application.yml
- Check the readerId in your query matches a configured reader

## Contributing

Contributions are welcome! Please read the contributing guidelines before submitting pull requests.

## License

This project is licensed under the MIT License - see the [LICENSE](../../LICENSE) file for details.
