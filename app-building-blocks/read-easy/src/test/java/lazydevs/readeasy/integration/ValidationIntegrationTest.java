package lazydevs.readeasy.integration;

import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.validation.QueryValidationException;
import lazydevs.readeasy.validation.QueryValidator;
import lazydevs.readeasy.validation.QueryValidationException.ValidationError;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for query validation functionality.
 * Tests validation of various query file scenarios.
 */
@SpringBootTest(classes = {
        lazydevs.readeasy.validation.QueryValidator.class,
        ReadEasyConfig.class
})
@ActiveProfiles("test")
@DisplayName("Query Validation Integration Tests")
@Disabled("Requires Spring context - run manually")
class ValidationIntegrationTest {

    @Autowired
    private QueryValidator queryValidator;

    private Set<String> availableReaderIds;

    @BeforeEach
    void setUp() {
        availableReaderIds = new HashSet<>(Arrays.asList("default", "test", "mongodb"));
    }

    @Nested
    @DisplayName("Valid Query Files")
    class ValidQueryFiles {

        @Test
        @DisplayName("Should validate users-queries.yaml without errors")
        void shouldValidateUsersQueriesWithoutErrors() throws IOException {
            String content = readResourceFile("queries/valid/users-queries.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "users", "classpath:queries/valid/users-queries.yaml",
                    content, availableReaderIds);

            assertTrue(errors.isEmpty(),
                    "Valid users-queries.yaml should have no validation errors. Found: " + errors);
        }

        @Test
        @DisplayName("Should validate orders-queries.yaml without errors")
        void shouldValidateOrdersQueriesWithoutErrors() throws IOException {
            String content = readResourceFile("queries/valid/orders-queries.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "orders", "classpath:queries/valid/orders-queries.yaml",
                    content, availableReaderIds);

            assertTrue(errors.isEmpty(),
                    "Valid orders-queries.yaml should have no validation errors. Found: " + errors);
        }
    }

    @Nested
    @DisplayName("Invalid Query Files - Missing Raw")
    class InvalidMissingRaw {

        @Test
        @DisplayName("Should detect missing raw field")
        void shouldDetectMissingRawField() throws IOException {
            String content = readResourceFile("queries/invalid/missing-raw.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "classpath:queries/invalid/missing-raw.yaml",
                    content, availableReaderIds);

            assertFalse(errors.isEmpty(),
                    "Should detect errors in file with missing raw field");
            assertTrue(errors.stream().anyMatch(e -> e.getErrorType().equals("MISSING_RAW")),
                    "Should include MISSING_RAW error type");
        }

        @Test
        @DisplayName("Should provide suggestion for missing raw field")
        void shouldProvideSuggestionForMissingRaw() throws IOException {
            String content = readResourceFile("queries/invalid/missing-raw.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "classpath:queries/invalid/missing-raw.yaml",
                    content, availableReaderIds);

            ValidationError rawError = errors.stream()
                    .filter(e -> e.getErrorType().equals("MISSING_RAW"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(rawError, "Should have MISSING_RAW error");
            assertNotNull(rawError.getSuggestion(), "Should provide suggestion");
            assertTrue(rawError.getSuggestion().toLowerCase().contains("raw"),
                    "Suggestion should mention 'raw' field");
        }
    }

    @Nested
    @DisplayName("Invalid Query Files - YAML Syntax")
    class InvalidYamlSyntax {

        @Test
        @DisplayName("Should detect YAML syntax errors")
        void shouldDetectYamlSyntaxErrors() throws IOException {
            String content = readResourceFile("queries/invalid/invalid-yaml-syntax.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "classpath:queries/invalid/invalid-yaml-syntax.yaml",
                    content, availableReaderIds);

            assertFalse(errors.isEmpty(),
                    "Should detect YAML syntax errors");
            assertTrue(errors.stream().anyMatch(e -> e.getErrorType().equals("YAML_SYNTAX")),
                    "Should include YAML_SYNTAX error type");
        }
    }

    @Nested
    @DisplayName("Invalid Query Files - Invalid Reader ID")
    class InvalidReaderId {

        @Test
        @DisplayName("Should detect invalid reader ID")
        void shouldDetectInvalidReaderId() throws IOException {
            String content = readResourceFile("queries/invalid/invalid-reader-id.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "classpath:queries/invalid/invalid-reader-id.yaml",
                    content, availableReaderIds);

            assertFalse(errors.isEmpty(),
                    "Should detect invalid reader ID");
            assertTrue(errors.stream().anyMatch(e -> e.getErrorType().equals("INVALID_READER")),
                    "Should include INVALID_READER error type");
        }

        @Test
        @DisplayName("Should suggest available readers")
        void shouldSuggestAvailableReaders() throws IOException {
            String content = readResourceFile("queries/invalid/invalid-reader-id.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "classpath:queries/invalid/invalid-reader-id.yaml",
                    content, availableReaderIds);

            ValidationError readerError = errors.stream()
                    .filter(e -> e.getErrorType().equals("INVALID_READER"))
                    .findFirst()
                    .orElse(null);

            assertNotNull(readerError, "Should have INVALID_READER error");
            assertNotNull(readerError.getSuggestion(), "Should provide suggestion");
            assertTrue(readerError.getSuggestion().contains("default") ||
                            readerError.getSuggestion().contains("Available"),
                    "Suggestion should mention available readers");
        }
    }

    @Nested
    @DisplayName("Invalid Query Files - FreeMarker Syntax")
    class InvalidFreeMarkerSyntax {

        @Test
        @DisplayName("Should detect FreeMarker template errors")
        void shouldDetectFreeMarkerErrors() throws IOException {
            String content = readResourceFile("queries/invalid/invalid-freemarker.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "classpath:queries/invalid/invalid-freemarker.yaml",
                    content, availableReaderIds);

            assertFalse(errors.isEmpty(),
                    "Should detect FreeMarker template errors");
            assertTrue(errors.stream().anyMatch(e ->
                            e.getErrorType().equals("TEMPLATE_SYNTAX") ||
                                    e.getErrorType().equals("TEMPLATE_ERROR")),
                    "Should include template-related error type");
        }
    }

    @Nested
    @DisplayName("validateAndThrow Behavior")
    class ValidateAndThrowBehavior {

        @Test
        @DisplayName("Should throw exception when failOnError is true")
        void shouldThrowExceptionWhenFailOnErrorTrue() throws IOException {
            String content = readResourceFile("queries/invalid/missing-raw.yaml");

            assertThrows(QueryValidationException.class, () ->
                            queryValidator.validateAndThrow(
                                    "invalid",
                                    "classpath:queries/invalid/missing-raw.yaml",
                                    content,
                                    availableReaderIds,
                                    true),
                    "Should throw QueryValidationException when failOnError=true");
        }

        @Test
        @DisplayName("Should not throw exception when failOnError is false")
        void shouldNotThrowExceptionWhenFailOnErrorFalse() throws IOException {
            String content = readResourceFile("queries/invalid/missing-raw.yaml");

            assertDoesNotThrow(() ->
                            queryValidator.validateAndThrow(
                                    "invalid",
                                    "classpath:queries/invalid/missing-raw.yaml",
                                    content,
                                    availableReaderIds,
                                    false),
                    "Should not throw when failOnError=false (logs warning instead)");
        }

        @Test
        @DisplayName("Should not throw for valid files regardless of failOnError")
        void shouldNotThrowForValidFiles() throws IOException {
            String content = readResourceFile("queries/valid/users-queries.yaml");

            assertDoesNotThrow(() ->
                            queryValidator.validateAndThrow(
                                    "users",
                                    "classpath:queries/valid/users-queries.yaml",
                                    content,
                                    availableReaderIds,
                                    true),
                    "Should not throw for valid files even when failOnError=true");
        }
    }

    @Nested
    @DisplayName("Error Message Quality")
    class ErrorMessageQuality {

        @Test
        @DisplayName("Should include query ID in error")
        void shouldIncludeQueryIdInError() throws IOException {
            String content = readResourceFile("queries/invalid/missing-raw.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "testns", "test.yaml", content, availableReaderIds);

            assertTrue(errors.stream().allMatch(e ->
                            e.getQueryId() != null && e.getQueryId().startsWith("testns.")),
                    "All errors should include query ID with namespace");
        }

        @Test
        @DisplayName("Should include file location in error")
        void shouldIncludeFileLocationInError() throws IOException {
            String content = readResourceFile("queries/invalid/missing-raw.yaml");
            String filePath = "classpath:queries/invalid/missing-raw.yaml";

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", filePath, content, availableReaderIds);

            assertTrue(errors.stream().allMatch(e ->
                            e.getLocation() != null && e.getLocation().contains(filePath)),
                    "All errors should include file location");
        }
    }

    @Nested
    @DisplayName("Empty Reader IDs Set")
    class EmptyReaderIds {

        @Test
        @DisplayName("Should skip reader validation when no reader IDs provided")
        void shouldSkipReaderValidationWhenNoReaderIds() throws IOException {
            String content = readResourceFile("queries/invalid/invalid-reader-id.yaml");

            // Empty set should skip reader ID validation
            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "test.yaml", content, Collections.emptySet());

            assertTrue(errors.stream().noneMatch(e -> e.getErrorType().equals("INVALID_READER")),
                    "Should not report INVALID_READER when no reader IDs configured");
        }

        @Test
        @DisplayName("Should skip reader validation when null reader IDs provided")
        void shouldSkipReaderValidationWhenNullReaderIds() throws IOException {
            String content = readResourceFile("queries/invalid/invalid-reader-id.yaml");

            List<ValidationError> errors = queryValidator.validateQueryFile(
                    "invalid", "test.yaml", content, null);

            assertTrue(errors.stream().noneMatch(e -> e.getErrorType().equals("INVALID_READER")),
                    "Should not report INVALID_READER when null reader IDs");
        }
    }

    private String readResourceFile(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        return new String(Files.readAllBytes(resource.getFile().toPath()), StandardCharsets.UTF_8);
    }
}
