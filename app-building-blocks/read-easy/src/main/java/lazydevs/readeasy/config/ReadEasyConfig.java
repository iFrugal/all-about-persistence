package lazydevs.readeasy.config;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.services.basic.validation.Param;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Main configuration class for Read-Easy framework.
 *
 * <p>Read-Easy provides a configuration-driven approach to expose database queries
 * as REST endpoints. This class holds all the configuration properties loaded from
 * application.yml under the {@code readeasy} prefix.</p>
 *
 * <h2>Configuration Example:</h2>
 * <pre>{@code
 * readeasy:
 *   generalReaders:
 *     default:
 *       fqcn: lazydevs.persistence.impl.jdbc.JdbcGeneralReader
 *       args:
 *         - beanRef:simpleJdbcTemplate
 *   queryFiles:
 *     users:
 *       - classpath:queries/users.yaml
 * }</pre>
 *
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link #generalReaderInit} - Single default reader configuration</li>
 *   <li>{@link #generalReaders} - Multiple named readers for different data sources</li>
 *   <li>{@link #queryFiles} - Mapping of namespace to query YAML file paths</li>
 *   <li>{@link #operationInstruction} - Default settings for each operation type</li>
 * </ul>
 *
 * @author Abhijeet Rai
 * @see ConfiguredReadController
 * @see Query
 * @see Operation
 */
@Getter @Setter @ToString
@Configuration @ConfigurationProperties(prefix = "readeasy")
public class ReadEasyConfig{

    /**
     * Single default GeneralReader initialization configuration.
     * Use this when you only have one data source.
     *
     * <p>Example:</p>
     * <pre>{@code
     * generalReaderInit:
     *   fqcn: lazydevs.persistence.impl.jdbc.JdbcGeneralReader
     *   args:
     *     - beanRef:simpleJdbcTemplate
     * }</pre>
     *
     * @see #generalReaders for multiple readers
     */
    private InitDTO generalReaderInit;

    /**
     * Map of named GeneralReader configurations for multiple data sources.
     * Each key is a reader ID that can be referenced in query definitions.
     *
     * <p>Example:</p>
     * <pre>{@code
     * generalReaders:
     *   default:
     *     fqcn: lazydevs.persistence.impl.jdbc.JdbcGeneralReader
     *     args:
     *       - beanRef:simpleJdbcTemplate
     *   mongodb:
     *     fqcn: lazydevs.persistence.impl.mongodb.MongoGeneralReader
     *     args:
     *       - beanRef:mongoTemplate
     * }</pre>
     */
    Map<String, InitDTO> generalReaders;

    /**
     * Supplier for request-scoped context data.
     * The supplier must implement {@code Supplier<?>} and return data
     * accessible in queries via the {@code request} variable.
     *
     * <p>Useful for injecting:</p>
     * <ul>
     *   <li>Current user ID from security context</li>
     *   <li>Tenant ID for multi-tenant applications</li>
     *   <li>Request headers or attributes</li>
     * </ul>
     *
     * <p>Example usage in query:</p>
     * <pre>{@code
     * "nativeSQL": "SELECT * FROM orders WHERE user_id = :userId",
     * "params": [{"name": "userId", "value": "${request.userId}"}]
     * }</pre>
     */
    private InitDTO requestContextSupplierInit;

    /**
     * Supplier for application-wide global context data.
     * Similar to {@link #requestContextSupplierInit} but for global/static data.
     * Accessible in queries via the {@code global} variable.
     *
     * <p>Useful for:</p>
     * <ul>
     *   <li>Environment-specific configurations</li>
     *   <li>Feature flags</li>
     *   <li>Static lookup values</li>
     * </ul>
     */
    private InitDTO globalContextSupplierInit;

    /**
     * Mapping of namespace to query file paths.
     * Each namespace groups related queries, and the full query ID
     * is {@code namespace.queryName}.
     *
     * <p>Example:</p>
     * <pre>{@code
     * queryFiles:
     *   users:                           # namespace
     *     - classpath:queries/users.yaml # file containing queries
     *   products:
     *     - classpath:queries/products.yaml
     *     - classpath:queries/products-reports.yaml
     * }</pre>
     *
     * <p>Queries in users.yaml with name "byId" would be accessed as "users.byId"</p>
     */
    private Map<String, List<String>> queryFiles = new LinkedHashMap<>();

    /**
     * Default operation instructions applied to all queries.
     * Individual queries can override these settings.
     *
     * <p>Supported keys by operation:</p>
     * <ul>
     *   <li><b>ONE:</b> statusCodeWhenNoRecordsFound (default: 404)</li>
     *   <li><b>EXPORT:</b> exportFileNameTemplate, exportTemplate, readBatchSize,
     *       countCheckRequired, maxCountToExport, rejectRequestIfCountGreaterThanMaxCountToExport</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * operationInstruction:
     *   ONE:
     *     statusCodeWhenNoRecordsFound: 404
     *   EXPORT:
     *     readBatchSize: 1000
     *     maxCountToExport: 100000
     * }</pre>
     */
    private Map<Operation, Map<String, Object>> operationInstruction = new HashMap<>();


    /**
     * Container class for query YAML file structure.
     * Each query file can optionally define dynamic beans and must define queries.
     *
     * <p>YAML Structure:</p>
     * <pre>{@code
     * dynaBeans:
     *   # Optional dynamic bean definitions
     * queries:
     *   queryName:
     *     # Query configuration
     * }</pre>
     */
    @Getter @Setter
    public static class QueryWithDynaBeans{
        /**
         * Dynamic bean configurations for this query file.
         * Can include JavaScript functions, custom transformers, etc.
         */
        private DynaBeansConfig dynaBeans;

        /**
         * Map of query name to query configuration.
         * Keys become the second part of the query ID (namespace.queryName).
         */
        private Map<String, Query> queries;
    }

    /**
     * Configuration for a single query definition.
     *
     * <p>Queries are defined in YAML and expose database operations as REST endpoints.
     * Each query specifies the reader to use, the query template, parameter definitions,
     * and optional transformation/caching configurations.</p>
     *
     * <h3>Minimal Example:</h3>
     * <pre>{@code
     * myQuery:
     *   raw: |
     *     {"nativeSQL": "SELECT * FROM users WHERE id = :id"}
     *   params:
     *     id:
     *       required: true
     *       type: INTEGER
     * }</pre>
     *
     * <h3>Full Example:</h3>
     * <pre>{@code
     * advancedQuery:
     *   readerId: mongodb
     *   rawFormat: JSON
     *   raw: |
     *     {"collectionName": "users", "query": {"status": "${params.status}"}}
     *   params:
     *     status:
     *       required: true
     *       type: STRING
     *   rowTransformer:
     *     template: |
     *       {"fullName": "${firstName} ${lastName}"}
     *   cacheFetchInstruction:
     *     jsFunctionName: getCachedUsers
     *     args: ["status"]
     *   operationInstruction:
     *     ONE:
     *       statusCodeWhenNoRecordsFound: 200
     * }</pre>
     */
    @Getter @Setter @ToString
    public static class Query {
        /**
         * ID of the GeneralReader to use for this query.
         * Must match a key in {@link ReadEasyConfig#generalReaders}.
         * Defaults to "default".
         */
        private String readerId = "default";

        /**
         * Format of the {@link #raw} query string.
         * Supported values: JSON (default), YAML, XML.
         * The raw string is deserialized using this format after template processing.
         */
        private SerDe rawFormat = SerDe.JSON;

        /**
         * The query template using FreeMarker syntax.
         *
         * <p>Available template variables:</p>
         * <ul>
         *   <li>{@code params} - Request parameters from the client</li>
         *   <li>{@code request} - Request context (if configured)</li>
         *   <li>{@code global} - Global context (if configured)</li>
         *   <li>{@code sort} - Sort specification (auto-generated)</li>
         * </ul>
         *
         * <p>Example:</p>
         * <pre>{@code
         * raw: |
         *   {
         *     "nativeSQL": "SELECT * FROM users WHERE 1=1
         *       <#if params.name??>AND name LIKE :name</#if>",
         *     "params": [
         *       <#if params.name??>{"name": "name", "value": "%${params.name}%"}</#if>
         *     ]
         *   }
         * }</pre>
         */
        private String raw;

        /**
         * Parameter definitions for validation.
         * Keys are parameter names, values define validation rules.
         *
         * <p>Param properties:</p>
         * <ul>
         *   <li>required - Whether the parameter is mandatory</li>
         *   <li>type - Data type (STRING, INTEGER, LONG, DOUBLE, BOOLEAN, DATE)</li>
         *   <li>defaultValue - Value if not provided</li>
         *   <li>pattern - Regex pattern for validation</li>
         * </ul>
         */
        private Map<String, Param> params = new HashMap<>();

        /**
         * Optional caching configuration.
         * When specified, the cache function is called first before querying the database.
         * If the function returns a non-null value, that value is returned directly.
         *
         * @see CacheFetchInstruction
         */
        private CacheFetchInstruction cacheFetchInstruction;

        /**
         * Optional row transformer to reshape query results.
         * Applied to each row before returning to the client.
         *
         * <p>The transformer can use:</p>
         * <ul>
         *   <li>Template-based transformation (FreeMarker)</li>
         *   <li>JavaScript function transformation</li>
         *   <li>Custom transformer class</li>
         * </ul>
         */
        private GeneralTransformer rowTransformer;

        /**
         * Query-specific operation instructions.
         * Overrides the global {@link ReadEasyConfig#operationInstruction} settings.
         */
        private Map<Operation, Map<String, Object>> operationInstruction = new HashMap<>();

    }

    /**
     * Enumeration of supported REST operations.
     * Each operation corresponds to an endpoint in {@link ConfiguredReadController}.
     */
    public enum Operation{
        /**
         * Find single record. Endpoint: POST /read/one
         * <p>Instruction keys: statusCodeWhenNoRecordsFound</p>
         */
        ONE,

        /**
         * Find all records. Endpoint: POST /read/list
         */
        LIST,

        /**
         * Find paginated records. Endpoint: POST /read/page
         */
        PAGE,

        /**
         * Count matching records. Endpoint: POST /read/count
         */
        COUNT,

        /**
         * Export records as file. Endpoint: POST /read/export
         * <p>Instruction keys: exportFileNameTemplate, exportTemplate, readBatchSize,
         * countCheckRequired, maxCountToExport, rejectRequestIfCountGreaterThanMaxCountToExport</p>
         */
        EXPORT
    }

    /**
     * Configuration for JavaScript-based caching.
     *
     * <p>When configured on a query, Read-Easy will first call the specified
     * JavaScript function with the given arguments. If the function returns
     * a non-null value, that value is used instead of querying the database.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * cacheFetchInstruction:
     *   jsFunctionName: getCachedData
     *   args:
     *     - category  # Parameter name to pass to the function
     *     - status
     * }</pre>
     *
     * <p>The JavaScript function must be defined in the dynaBeans section:</p>
     * <pre>{@code
     * dynaBeans:
     *   cacheScript:
     *     type: SCRIPT
     *     script: |
     *       function getCachedData(category, status) {
     *         return CacheManager.get(category + '_' + status);
     *       }
     * }</pre>
     */
    @Getter @Setter @ToString
    public static class CacheFetchInstruction{
        /**
         * Name of the JavaScript function to call for cache lookup.
         * The function must be defined in the dynaBeans section of the query file.
         */
        private String jsFunctionName;

        /**
         * List of parameter names to pass to the cache function.
         * Values are extracted from the request params and passed in order.
         */
        private List<String> args = new ArrayList<>();
    }

}


