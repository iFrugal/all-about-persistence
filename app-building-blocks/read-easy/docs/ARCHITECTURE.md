# Read-Easy Architecture

This document describes the architecture and component interactions within the Read-Easy framework.

## High-Level Overview

Read-Easy is a configuration-driven query framework that sits between REST clients and various data sources, providing a declarative way to expose queries as REST endpoints.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              REST CLIENT                                     │
│                    (Web App, Mobile App, API Consumer)                       │
└────────────────────────────────────┬────────────────────────────────────────┘
                                     │
                                     │ HTTP Requests
                                     │ POST /read/{one|list|page|count|export}
                                     ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SPRING BOOT APPLICATION                               │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                    ConfiguredReadController                            │  │
│  │  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐  │  │
│  │  │ /read/one   │ │ /read/list  │ │ /read/page  │ │ /read/export    │  │  │
│  │  │             │ │             │ │ /read/count │ │                 │  │  │
│  │  └─────────────┘ └─────────────┘ └─────────────┘ └─────────────────┘  │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
│                                      │                                       │
│                    ┌─────────────────┼─────────────────┐                    │
│                    ▼                 ▼                 ▼                    │
│  ┌─────────────────────┐ ┌─────────────────┐ ┌─────────────────────────┐   │
│  │   ReadEasyConfig    │ │  ParamValidator │ │    TemplateEngine       │   │
│  │  (YAML → Queries)   │ │ (Input Check)   │ │    (FreeMarker)         │   │
│  └─────────────────────┘ └─────────────────┘ └─────────────────────────┘   │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      GeneralReader Interface                           │  │
│  │                  findOne | findAll | findPage | count                  │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
│                                      │                                       │
│        ┌─────────────────────────────┼─────────────────────────────┐        │
│        ▼                             ▼                             ▼        │
│  ┌───────────────┐         ┌─────────────────┐           ┌───────────────┐ │
│  │ JdbcGeneral   │         │ MongoGeneral    │           │ RestGeneral   │ │
│  │ Reader        │         │ Reader          │           │ Reader        │ │
│  └───────┬───────┘         └────────┬────────┘           └───────┬───────┘ │
│          │                          │                            │          │
└──────────┼──────────────────────────┼────────────────────────────┼──────────┘
           │                          │                            │
           ▼                          ▼                            ▼
    ┌──────────────┐         ┌─────────────────┐          ┌───────────────┐
    │  SQL         │         │  MongoDB        │          │  External     │
    │  Database    │         │  Cluster        │          │  REST API     │
    └──────────────┘         └─────────────────┘          └───────────────┘
```

## Component Details

### 1. ConfiguredReadController

The main REST controller that exposes query endpoints. Located at `/read/*`.

**Responsibilities:**
- Register queries from YAML files at startup
- Validate request parameters
- Execute queries through appropriate GeneralReader
- Transform results if configured
- Handle caching if configured
- Stream export data efficiently

**Endpoints:**

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/read/one` | POST | Single record by query |
| `/read/list` | POST | All matching records |
| `/read/page` | POST | Paginated results |
| `/read/count` | POST | Record count |
| `/read/export` | POST | File download (CSV) |
| `/read/register` | POST | Dynamic registration (admin) |

### 2. ReadEasyConfig

Spring configuration properties class that loads settings from `application.yml`.

**Key Properties:**
- `generalReaders` - Map of reader ID to reader initialization config
- `queryFiles` - Map of namespace to YAML file paths
- `operationInstruction` - Default settings for operations
- `requestContextSupplierInit` - Request-scoped data supplier
- `globalContextSupplierInit` - Application-wide data supplier

### 3. Query Definition Structure

Each query in YAML files follows this structure:

```yaml
queryName:
  readerId: string          # Which reader to use
  rawFormat: JSON|YAML|XML  # Format of raw query
  raw: string               # Query template (FreeMarker)
  params:                   # Parameter definitions
    paramName:
      required: boolean
      type: STRING|INTEGER|LONG|DOUBLE|BOOLEAN|DATE
      defaultValue: any
      pattern: regex
  rowTransformer:           # Result transformation
    template: string
  cacheFetchInstruction:    # Caching config
    jsFunctionName: string
    args: [string]
  operationInstruction:     # Per-query overrides
    ONE|LIST|PAGE|COUNT|EXPORT:
      key: value
```

### 4. Request Processing Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│                           REQUEST FLOW                                    │
└──────────────────────────────────────────────────────────────────────────┘

  1. HTTP Request
     │
     ▼
  2. Extract queryId and params from request
     │
     ▼
  3. Look up Query configuration
     │
     ├── Not Found → Return 400 ValidationException
     │
     ▼
  4. Validate parameters against Query.params
     │
     ├── Invalid → Return 400 ValidationException
     │
     ▼
  5. Check cache (if cacheFetchInstruction configured)
     │
     ├── Cache Hit → Skip to step 8
     │
     ▼
  6. Build context map:
     │   - params: request parameters
     │   - request: from requestContextSupplier
     │   - global: from globalContextSupplier
     │   - sort: from orderby/orderdir
     │
     ▼
  7. Process template:
     │   - Render raw query with FreeMarker
     │   - Deserialize to query object
     │
     ▼
  8. Execute query via GeneralReader
     │   - findOne() / findAll() / findPage() / count()
     │
     ▼
  9. Transform results (if rowTransformer configured)
     │
     ▼
 10. Return HTTP Response
```

### 5. GeneralReader Interface

The abstraction layer for different data sources.

```java
public interface GeneralReader<Q, P> {
    Map<String, Object> findOne(Q query);
    List<Map<String, Object>> findAll(Q query);
    Page<List<Map<String, Object>>> findPage(PageRequest pageRequest, Q query);
    long count(Q query);
    BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Q query);
}
```

**Implementations:**

| Implementation | Query Type | Use Case |
|----------------|------------|----------|
| `JdbcGeneralReader` | `JdbcOperation` | SQL databases |
| `MongoGeneralReader` | `MongoGeneralQuery` | MongoDB |
| `RestGeneralReader` | `RestInstruction` | External APIs |
| `FileGeneralReader` | `FileQuery` | CSV, Excel, JSON files |

### 6. Template Processing

Queries use FreeMarker templates for dynamic generation.

**Available Variables:**

| Variable | Source | Description |
|----------|--------|-------------|
| `params` | Request body | Client-provided parameters |
| `request` | requestContextSupplier | User/session context |
| `global` | globalContextSupplier | App-level context |
| `sort` | Query params | Sort specification |

**Common Patterns:**

```ftl
<#-- Conditional clause -->
<#if params.status??>AND status = :status</#if>

<#-- Default value -->
${params.limit!10}

<#-- List iteration -->
<#list params.ids as id>${id}<#sep>,</#list>

<#-- Date formatting -->
${.now?string('yyyy-MM-dd')}
```

### 7. Result Transformation

Results can be transformed before returning to the client.

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────┐
│  Database   │ ──▶ │  Row Transformer │ ──▶ │  Response   │
│  Row        │     │  (Template/JS)   │     │  Object     │
└─────────────┘     └─────────────────┘     └─────────────┘

Example:
  DB Row:     {first_name: "John", last_name: "Doe", email: "j@d.com"}
  Template:   {"fullName": "${first_name} ${last_name}", "email": "${email}"}
  Response:   {fullName: "John Doe", email: "j@d.com"}
```

### 8. Export Processing

Large exports are handled efficiently using batch processing.

```
┌────────────────────────────────────────────────────────────────────────┐
│                         EXPORT FLOW                                     │
└────────────────────────────────────────────────────────────────────────┘

  1. Validate export is allowed (count check if configured)
     │
     ▼
  2. Set response headers (Content-Disposition, Content-Type)
     │
     ▼
  3. Get BatchIterator from GeneralReader
     │
     ▼
  4. While hasNext():
     │   ├── Fetch next batch (e.g., 1000 records)
     │   ├── Apply exportTemplate (FreeMarker)
     │   ├── Write to response stream
     │   └── Flush
     │
     ▼
  5. Close iterator and complete response
```

## Module Dependencies

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         all-about-persistence                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────┐                                                   │
│  │ persistence-api  │  Core interfaces (GeneralReader, etc.)            │
│  └────────┬─────────┘                                                   │
│           │                                                              │
│           ▼                                                              │
│  ┌──────────────────┐                                                   │
│  │ persistence-utils│  SerDe, TemplateEngine, BatchIterator             │
│  └────────┬─────────┘                                                   │
│           │                                                              │
│           ▼                                                              │
│  ┌──────────────────┐                                                   │
│  │ persistence-impls│  JdbcReader, MongoReader, RestReader, FileReader  │
│  └────────┬─────────┘                                                   │
│           │                                                              │
│           ▼                                                              │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     app-building-blocks                           │   │
│  │  ┌────────────────────┐  ┌─────────────────┐  ┌───────────────┐  │   │
│  │  │ app-building-      │  │ dyna-beans-     │  │   read-easy   │  │   │
│  │  │ commons            │  │ injector        │  │               │  │   │
│  │  │ (Validation, etc.) │  │ (Dynamic Beans) │  │ (This Module) │  │   │
│  │  └────────────────────┘  └─────────────────┘  └───────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Security Considerations

1. **Parameter Validation**: Always define `params` with appropriate validation rules
2. **SQL Injection**: Use parameterized queries in templates
3. **Admin Endpoints**: `/read/register` should be disabled or secured in production
4. **Rate Limiting**: Consider adding rate limits for expensive queries
5. **Authorization**: Implement requestContextSupplier for user-based access control

## Performance Considerations

1. **Caching**: Use `cacheFetchInstruction` for frequently accessed, slowly changing data
2. **Batch Size**: Configure appropriate `readBatchSize` for exports
3. **Connection Pooling**: Configure at the datasource level
4. **Pagination**: Use `/read/page` for large result sets
5. **Export Limits**: Set `maxCountToExport` to prevent memory issues
