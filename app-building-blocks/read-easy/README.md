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

## Query Validation and Dev Tools

### Startup validation

Read-Easy validates query files during registration.
The check covers YAML structure, presence of `raw`, known `readerId` values, and FreeMarker syntax of `raw` and `rowTransformer.template`.
Template validation is parse-only: it never renders the template, so queries referencing request-time variables such as `${params.customerId}` or `${request.userId}` validate cleanly.

```yaml
readeasy:
  validation:
    enabled: true           # validate query files at startup (default: true)
    failOnError: false      # abort startup on errors; false logs warnings (default: false)
    validateTemplates: true # compile-check FreeMarker syntax (default: true)
```

Set `failOnError: true` in development to fail fast on broken query files.
The default keeps existing applications booting and reports problems as warnings.

### Hot reload (dev mode)

With dev tools enabled, changed query files are re-registered without a restart.
Only `file:` resources can be watched; classpath resources inside a JAR are skipped with a warning.
An invalid save is rejected and the previous queries stay active.
Reloading one file only replaces the queries that file contributed, even when several files share a namespace.

```yaml
readeasy:
  devtools:
    enabled: true           # default: false; only enable in development
    watchIntervalMs: 2000   # file poll interval
    validateOnReload: true  # validate a changed file before applying it
```

### Runtime template errors

A query that fails to render or parse at request time returns HTTP 400 with a short, client-safe message naming the query and the missing variable.
The full template and rendered query are written to the server log only, never to the HTTP response.

## Multi-Tenancy with PostgreSQL Row-Level Security

Read-Easy can enforce tenant isolation with PostgreSQL row-level security, driven entirely by configuration.
The tenant id arrives in a request header, is held in `TenantContext` for the request, and is bound to a PostgreSQL session variable on every JDBC connection the reader uses.
Your RLS policies read that variable, so the database itself filters rows per tenant and no query needs a `WHERE tenant_id = ...` clause.

### 1. Database side (your SQL migrations)

```sql
ALTER TABLE employee ADD COLUMN tenant_id uuid;

-- The app must connect as a role that does NOT own the table (owners bypass RLS),
-- or use ALTER TABLE employee FORCE ROW LEVEL SECURITY.
ALTER TABLE employee ENABLE ROW LEVEL SECURITY;

CREATE POLICY employee_tenant_isolation ON employee
    USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
```

**Write the `nullif(..., '')` in, even if you think you do not need it.** A custom
PostgreSQL GUC has three possible states, not two, and the third one bites:

| session state | `current_setting('app.tenant_id', true)` |
|---|---|
| never written in this session | `NULL` |
| bound to a tenant | the tenant id |
| **released back to the pool** | `''` — *not* `NULL` |

There is no way back to the first state once the variable has been written:
`set_config(name, NULL, false)`, `RESET name` and even `DISCARD ALL` all leave the
empty string. So on a pooled connection the empty string is the normal cleared
state, and `''::uuid` raises `invalid input syntax for type uuid: ""` rather than
matching nothing. `nullif(current_setting(...), '')::uuid` folds it back to NULL,
which is correct in all three states. A `text` tenant column needs no cast and is
safe either way.

### 2. Shared rows: `tenant_id IS NULL` visible to every tenant

Plenty of schemas are not "every row belongs to exactly one tenant". A row with
no owner — a platform default, a shared catalogue, a pool every customer can
draw from — is usually modelled as a nullable `tenant_id`, readable by all and
writable by none of them:

```sql
CREATE POLICY employee_read ON employee FOR SELECT
    USING (tenant_id IS NULL                                                -- shared
           OR tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);

CREATE POLICY employee_write ON employee FOR ALL
    USING      (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid)
    WITH CHECK (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
```

> **Read this before splitting a policy in two: multiple permissive policies are
> combined with OR, not AND.** Adding `employee_read` next to `employee_write`
> *widens* what SELECT can see (`FOR ALL` also applies to SELECT, and the two
> `USING` clauses are OR'd) — it does not restrict it. That is what makes the
> pair above correct: reads see shared + own, while writes are confined to own by
> `employee_write`'s `WITH CHECK`, which `employee_read` cannot relax because a
> `FOR SELECT` policy has no `WITH CHECK`. If you want a policy that genuinely
> narrows, that is `CREATE POLICY ... AS RESTRICTIVE`. A single policy carrying
> both `USING` and `WITH CHECK` avoids the question entirely and is the simpler
> choice when one predicate covers reads.

The corollary is a request with **no tenant at all** — a public read, a warm-up
job, a health probe — which must still see the shared rows. Configure the reader
with a stand-in value so those connections are bound rather than left to chance
(see §4).

### 3. Application side (YAML only)

```yaml
readeasy:
  multitenancy:
    enabled: true
    headerName: X-Tenant-Id
    required: true                        # 400 when the header is missing
    tenantIdPattern: "[0-9a-fA-F-]{36}"   # optional validation
    urlPatterns:
      - /read/*

  generalReaders:
    default:
      fqcn: lazydevs.persistence.jdbc.general.JdbcGeneralReader
      constructorArgs:
        - typeFqcn: javax.sql.DataSource
          beanName: dataSource
        - typeFqcn: java.lang.String
          val: app.tenant_id              # enables RLS binding for this reader
```

The two-argument `JdbcGeneralReader(dataSource, settingName)` constructor wraps the DataSource in `RlsDataSource`, which runs `SELECT set_config('app.tenant_id', '<tenant>', false)` when a connection is checked out and clears it when the connection is released - including the long-held connection used by `/read/export` streaming.
A pooled connection therefore never carries one request's tenant into the next.
Omit the second constructor argument and the reader behaves exactly as before - RLS is opt-in per reader.

`JdbcGeneralUpdater` has the same constructors for RLS-enforced writes.

### 4. Requests with no tenant in scope

`RlsSettings.missingTenant` decides what happens when a connection is acquired
and `TenantContext` is empty:

| `MissingTenant` | behaviour |
|---|---|
| `FAIL` (default) | throw `IllegalStateException`. A tenant-less checkout is usually a wiring mistake, and the policy failing closed hides it. |
| `BIND` | bind `missingTenantValue` (default `''`) and wrap the connection exactly as a tenant-scoped one, so the reset-on-close guarantee holds and the session state is the same on every checkout. |

`BIND` is the mode for the shared-rows model in §2: the predicate's own-tenant
branch matches nothing, the `tenant_id IS NULL` branch matches, and the caller
gets the shared rows and only those. Select it from YAML with a third
constructor argument:

```yaml
      constructorArgs:
        - typeFqcn: javax.sql.DataSource
          beanName: dataSource
        - typeFqcn: java.lang.String
          val: app.tenant_id
        - typeFqcn: java.lang.String
          val: ""                         # presence of this arg selects BIND
```

or in code:

```java
new JdbcGeneralReader(dataSource, "app.tenant_id", "");

new RlsDataSource(dataSource,
        new RlsSettings("app.tenant_id", MissingTenant.BIND, "", TenantContext::getTenantId));
```

**Upgrading from `failWhenTenantMissing = false`.** That flag is deprecated and
now maps to `BIND("")`. It used to hand back an *unbound* connection, which read
back as `NULL` on a connection the pool had never used and `''` on one it had —
so under a casting policy the same tenant-less request succeeded against a cold
pool and failed against a warm one. It is now deterministic, which means a
`::uuid` policy without `nullif` will fail *every* time instead of
intermittently. **Adopt the `nullif(current_setting(...), '')::uuid` form from §1
in the same change as the version bump**, not as a follow-up.

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
