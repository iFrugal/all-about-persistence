package lazydevs.readeasy.validation;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.config.ReadEasyConfig.Query;
import lazydevs.readeasy.config.ReadEasyConfig.QueryWithDynaBeans;
import lazydevs.readeasy.validation.QueryValidationException.ValidationError;
import lazydevs.services.basic.validation.Param;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates query configurations at startup to catch errors early.
 *
 * <p>This validator performs comprehensive checks on query YAML files including:</p>
 * <ul>
 *   <li>YAML syntax validation</li>
 *   <li>Required field presence</li>
 *   <li>FreeMarker template syntax validation</li>
 *   <li>Reader ID existence check</li>
 *   <li>Parameter definition validation</li>
 * </ul>
 *
 * <p>Enable validation in application.yml:</p>
 * <pre>{@code
 * readeasy:
 *   validation:
 *     enabled: true           # Enable startup validation (default: true)
 *     failOnError: true       # Fail startup on validation errors (default: true)
 *     validateTemplates: true # Validate FreeMarker syntax (default: true)
 * }</pre>
 *
 * @author Abhijeet Rai
 */
@Slf4j
@Component
public class QueryValidator {

    // Pattern to find FreeMarker directives
    private static final Pattern FREEMARKER_DIRECTIVE_PATTERN = Pattern.compile("<#[^>]+>");
    private static final Pattern FREEMARKER_INTERPOLATION_PATTERN = Pattern.compile("\\$\\{[^}]+\\}");
    private static final Pattern UNCLOSED_IF_PATTERN = Pattern.compile("<#if[^>]*>(?!.*</#if>)");

    /**
     * Validates a query YAML content and returns validation errors.
     *
     * @param namespace The namespace for the queries
     * @param filePath  The file path (for error messages)
     * @param content   The YAML content to validate
     * @param availableReaderIds Set of available reader IDs
     * @return List of validation errors (empty if valid)
     */
    public List<ValidationError> validateQueryFile(String namespace, String filePath,
                                                    String content, Set<String> availableReaderIds) {
        List<ValidationError> errors = new ArrayList<>();

        // Step 1: Validate YAML syntax
        QueryWithDynaBeans queryWithDynaBeans;
        try {
            queryWithDynaBeans = SerDe.YAML.deserialize(content, QueryWithDynaBeans.class);
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .queryId(namespace + ".*")
                    .location(filePath)
                    .errorType("YAML_SYNTAX")
                    .message("Failed to parse YAML: " + extractRootCause(e))
                    .suggestion("Check YAML syntax - ensure proper indentation and no tab characters")
                    .build());
            return errors; // Can't continue if YAML is invalid
        }

        // Step 2: Validate queries exist
        if (queryWithDynaBeans.getQueries() == null || queryWithDynaBeans.getQueries().isEmpty()) {
            errors.add(ValidationError.builder()
                    .queryId(namespace + ".*")
                    .location(filePath)
                    .errorType("MISSING_QUERIES")
                    .message("No queries defined in file")
                    .suggestion("Add a 'queries:' section with at least one query definition")
                    .build());
            return errors;
        }

        // Step 3: Validate each query
        queryWithDynaBeans.getQueries().forEach((queryId, query) -> {
            String fullQueryId = namespace + "." + queryId;
            errors.addAll(validateQuery(fullQueryId, filePath, query, availableReaderIds));
        });

        return errors;
    }

    /**
     * Validates a single query configuration.
     */
    private List<ValidationError> validateQuery(String queryId, String filePath,
                                                 Query query, Set<String> availableReaderIds) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate raw query is present
        if (query.getRaw() == null || query.getRaw().trim().isEmpty()) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(filePath + " -> queries." + queryId.substring(queryId.lastIndexOf('.') + 1))
                    .errorType("MISSING_RAW")
                    .message("Query 'raw' field is required but missing or empty")
                    .suggestion("Add a 'raw:' field with the query template")
                    .build());
            return errors; // Can't validate template if raw is missing
        }

        // Validate reader ID exists
        String readerId = query.getReaderId();
        if (availableReaderIds != null && !availableReaderIds.isEmpty() &&
                !availableReaderIds.contains(readerId)) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(filePath + " -> queries." + queryId.substring(queryId.lastIndexOf('.') + 1) + ".readerId")
                    .errorType("INVALID_READER")
                    .message("Reader ID '" + readerId + "' is not configured")
                    .suggestion("Available readers: " + availableReaderIds + ". Check 'readeasy.generalReaders' in application.yml")
                    .build());
        }

        // Validate FreeMarker template syntax
        errors.addAll(validateFreeMarkerTemplate(queryId, filePath, query.getRaw()));

        // Validate JSON/YAML structure (with sample data)
        errors.addAll(validateQueryFormat(queryId, filePath, query));

        // Validate parameter definitions
        if (query.getParams() != null) {
            errors.addAll(validateParams(queryId, filePath, query.getParams()));
        }

        // Validate rowTransformer if present
        if (query.getRowTransformer() != null && query.getRowTransformer().getTemplate() != null) {
            errors.addAll(validateFreeMarkerTemplate(
                    queryId + ".rowTransformer",
                    filePath,
                    query.getRowTransformer().getTemplate()
            ));
        }

        return errors;
    }

    /**
     * Validates FreeMarker template syntax.
     */
    private List<ValidationError> validateFreeMarkerTemplate(String queryId, String filePath, String template) {
        List<ValidationError> errors = new ArrayList<>();

        // Check for common FreeMarker syntax errors

        // 1. Check for unclosed #if directives
        int ifCount = countOccurrences(template, "<#if");
        int endIfCount = countOccurrences(template, "</#if>");
        if (ifCount != endIfCount) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(filePath)
                    .errorType("TEMPLATE_SYNTAX")
                    .message("Mismatched <#if> and </#if> tags. Found " + ifCount + " <#if> and " + endIfCount + " </#if>")
                    .suggestion("Ensure every <#if ...> has a matching </#if>")
                    .build());
        }

        // 2. Check for unclosed #list directives
        int listCount = countOccurrences(template, "<#list");
        int endListCount = countOccurrences(template, "</#list>");
        if (listCount != endListCount) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(filePath)
                    .errorType("TEMPLATE_SYNTAX")
                    .message("Mismatched <#list> and </#list> tags. Found " + listCount + " <#list> and " + endListCount + " </#list>")
                    .suggestion("Ensure every <#list ...> has a matching </#list>")
                    .build());
        }

        // 3. Try to compile the template with FreeMarker
        try {
            // Create a test context with dummy values
            Map<String, Object> testContext = createTestContext();
            StringWriter writer = new StringWriter();
            TemplateEngine.getInstance().generate(writer, template, testContext);
        } catch (Exception e) {
            String errorMsg = extractFreeMarkerError(e);
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(filePath)
                    .errorType("TEMPLATE_ERROR")
                    .message("FreeMarker template error: " + errorMsg)
                    .suggestion(suggestFreeMarkerFix(errorMsg))
                    .build());
        }

        return errors;
    }

    /**
     * Validates that the rendered query can be parsed as the expected format.
     */
    private List<ValidationError> validateQueryFormat(String queryId, String filePath, Query query) {
        List<ValidationError> errors = new ArrayList<>();

        try {
            // Render template with test data
            Map<String, Object> testContext = createTestContext();
            String rendered = TemplateEngine.getInstance().generate(query.getRaw(), testContext);

            // Try to parse as the specified format
            SerDe format = query.getRawFormat();
            try {
                format.deserializeToMap(rendered);
            } catch (Exception e) {
                errors.add(ValidationError.builder()
                        .queryId(queryId)
                        .location(filePath)
                        .errorType("FORMAT_ERROR")
                        .message("Rendered query is not valid " + format + ": " + extractRootCause(e))
                        .suggestion("Ensure the 'raw' template produces valid " + format + " after variable substitution")
                        .build());
            }
        } catch (Exception e) {
            // Template rendering error - already caught in validateFreeMarkerTemplate
        }

        return errors;
    }

    /**
     * Validates parameter definitions.
     */
    private List<ValidationError> validateParams(String queryId, String filePath, Map<String, Param> params) {
        List<ValidationError> errors = new ArrayList<>();

        params.forEach((paramName, param) -> {
            // Check for invalid param configurations
            if (param == null) {
                errors.add(ValidationError.builder()
                        .queryId(queryId)
                        .location(filePath + " -> params." + paramName)
                        .errorType("INVALID_PARAM")
                        .message("Parameter '" + paramName + "' has null configuration")
                        .suggestion("Remove the parameter or provide valid configuration")
                        .build());
            }
        });

        return errors;
    }

    /**
     * Creates a test context with dummy values for template validation.
     */
    private Map<String, Object> createTestContext() {
        Map<String, Object> context = new HashMap<>();

        // Add params with some test values
        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("name", "test");
        params.put("status", "ACTIVE");
        params.put("limit", 10);
        params.put("offset", 0);
        params.put("sort", "{}");
        context.put("params", params);

        // Add empty request and global contexts
        context.put("request", new HashMap<>());
        context.put("global", new HashMap<>());

        return context;
    }

    /**
     * Extracts a user-friendly error message from FreeMarker exceptions.
     */
    private String extractFreeMarkerError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return e.getClass().getSimpleName();
        }

        // Extract line/column info if present
        Pattern linePattern = Pattern.compile("line (\\d+), column (\\d+)");
        Matcher matcher = linePattern.matcher(message);
        if (matcher.find()) {
            return String.format("Error at line %s, column %s: %s",
                    matcher.group(1), matcher.group(2),
                    message.substring(0, Math.min(message.length(), 200)));
        }

        // Truncate long messages
        return message.length() > 200 ? message.substring(0, 200) + "..." : message;
    }

    /**
     * Suggests fixes for common FreeMarker errors.
     */
    private String suggestFreeMarkerFix(String errorMsg) {
        if (errorMsg.contains("expecting")) {
            return "Check FreeMarker syntax - you may have a typo in a directive";
        }
        if (errorMsg.contains("undefined") || errorMsg.contains("null")) {
            return "Use the ?? operator to check for null (e.g., <#if params.name??>) or provide a default with ! (e.g., ${params.name!'default'})";
        }
        if (errorMsg.contains("hash") || errorMsg.contains("sequence")) {
            return "Check that you're using the correct FreeMarker type - sequences use <#list>, hashes use .key or [\"key\"]";
        }
        return "Review FreeMarker documentation at https://freemarker.apache.org/docs/";
    }

    /**
     * Extracts the root cause message from nested exceptions.
     */
    private String extractRootCause(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return msg != null ? msg : cause.getClass().getSimpleName();
    }

    /**
     * Counts occurrences of a substring in a string.
     */
    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }

    /**
     * Performs full validation and throws if errors found (when failOnError is true).
     */
    public void validateAndThrow(String namespace, String filePath, String content,
                                  Set<String> availableReaderIds, boolean failOnError) {
        List<ValidationError> errors = validateQueryFile(namespace, filePath, content, availableReaderIds);

        if (!errors.isEmpty()) {
            String message = String.format("Validation failed for query file: %s", filePath);

            if (failOnError) {
                throw new QueryValidationException(message, errors);
            } else {
                // Log errors but don't fail
                log.warn("Query validation warnings for {}: {} issues found", filePath, errors.size());
                errors.forEach(error -> log.warn("  - [{}] {}: {}",
                        error.getErrorType(), error.getQueryId(), error.getMessage()));
            }
        }
    }
}
