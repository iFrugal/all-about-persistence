package lazydevs.readeasy.validation;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.readeasy.config.ReadEasyConfig.Query;
import lazydevs.readeasy.config.ReadEasyConfig.QueryWithDynaBeans;
import lazydevs.readeasy.validation.QueryValidationException.ValidationError;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Validates query YAML files. Stateless; safe to instantiate anywhere.
 *
 * <p>Checks performed:</p>
 * <ul>
 *   <li>YAML parses into the query file structure</li>
 *   <li>at least one query is defined</li>
 *   <li>every query has a non-empty {@code raw} template</li>
 *   <li>{@code readerId} matches a configured reader (when the reader set is known)</li>
 *   <li>FreeMarker syntax of {@code raw} and {@code rowTransformer.template} compiles</li>
 * </ul>
 *
 * <p>Template validation is a parse-only check ({@link TemplateEngine#validateSyntax}):
 * it never renders the template, so queries referencing request-specific variables
 * (e.g. {@code ${params.customerId}} or {@code ${request.userId}}) validate cleanly.
 * Whether the rendered output parses as the reader's query type depends on runtime
 * data and is deliberately not checked here.</p>
 *
 * @author Abhijeet Rai
 */
@Slf4j
public class QueryValidator {

    /**
     * Validates one query file's content and returns the problems found.
     *
     * @param namespace          namespace the file is registered under
     * @param filePath           file path, used only for error locations
     * @param content            the YAML content
     * @param availableReaderIds configured reader ids; pass null or empty to skip the reader check
     * @param validateTemplates  whether to compile-check FreeMarker templates
     * @return validation errors, empty when the file is valid
     */
    public List<ValidationError> validateQueryFile(String namespace, String filePath, String content,
                                                   Set<String> availableReaderIds, boolean validateTemplates) {
        List<ValidationError> errors = new ArrayList<>();

        QueryWithDynaBeans queryWithDynaBeans;
        try {
            queryWithDynaBeans = SerDe.YAML.deserialize(content, QueryWithDynaBeans.class);
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .queryId(namespace + ".*")
                    .location(filePath)
                    .errorType("YAML_SYNTAX")
                    .message("Failed to parse YAML: " + QueryTemplateException.summarize(e))
                    .suggestion("Check YAML syntax - proper indentation, no tab characters")
                    .build());
            return errors;
        }

        if (null == queryWithDynaBeans.getQueries() || queryWithDynaBeans.getQueries().isEmpty()) {
            errors.add(ValidationError.builder()
                    .queryId(namespace + ".*")
                    .location(filePath)
                    .errorType("MISSING_QUERIES")
                    .message("No queries defined in file")
                    .suggestion("Add a 'queries:' section with at least one query definition")
                    .build());
            return errors;
        }

        queryWithDynaBeans.getQueries().forEach((queryName, query) ->
                validateQuery(namespace + "." + queryName, filePath + " -> queries." + queryName,
                        query, availableReaderIds, validateTemplates, errors));
        return errors;
    }

    private void validateQuery(String queryId, String location, Query query,
                               Set<String> availableReaderIds, boolean validateTemplates,
                               List<ValidationError> errors) {
        if (null == query.getRaw() || query.getRaw().trim().isEmpty()) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(location)
                    .errorType("MISSING_RAW")
                    .message("Query 'raw' field is required but missing or empty")
                    .suggestion("Add a 'raw:' field with the query template")
                    .build());
            return;
        }

        if (null != availableReaderIds && !availableReaderIds.isEmpty()
                && !availableReaderIds.contains(query.getReaderId())) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(location + ".readerId")
                    .errorType("INVALID_READER")
                    .message("Reader ID '" + query.getReaderId() + "' is not configured")
                    .suggestion("Available readers: " + availableReaderIds
                            + ". Check 'readeasy.generalReaders' in application.yml")
                    .build());
        }

        if (validateTemplates) {
            validateTemplateSyntax(queryId, location + ".raw", query.getRaw(), errors);
            if (null != query.getRowTransformer() && null != query.getRowTransformer().getTemplate()) {
                validateTemplateSyntax(queryId + ".rowTransformer", location + ".rowTransformer.template",
                        query.getRowTransformer().getTemplate(), errors);
            }
        }
    }

    private void validateTemplateSyntax(String queryId, String location, String template,
                                        List<ValidationError> errors) {
        try {
            TemplateEngine.getInstance().validateSyntax(template);
        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .queryId(queryId)
                    .location(location)
                    .errorType("TEMPLATE_SYNTAX")
                    .message("FreeMarker syntax error: " + QueryTemplateException.summarize(e))
                    .suggestion("Fix the directive/expression syntax; every <#if>/<#list> needs a matching closer")
                    .build());
        }
    }

    /**
     * Validates and either throws (failOnError) or logs the problems as warnings.
     */
    public void validateAndThrow(String namespace, String filePath, String content,
                                 Set<String> availableReaderIds, boolean validateTemplates,
                                 boolean failOnError) {
        List<ValidationError> errors = validateQueryFile(namespace, filePath, content,
                availableReaderIds, validateTemplates);
        if (errors.isEmpty()) {
            return;
        }
        if (failOnError) {
            throw new QueryValidationException("Validation failed for query file: " + filePath, errors);
        }
        log.warn("Query validation found {} issue(s) in {}:", errors.size(), filePath);
        errors.forEach(error -> log.warn("  - [{}] {}: {}",
                error.getErrorType(), error.getQueryId(), error.getMessage()));
    }
}
