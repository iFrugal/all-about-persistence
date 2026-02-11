package lazydevs.readeasy.validation;

import lazydevs.readeasy.validation.QueryValidationException.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryValidationException.
 * Tests exception formatting, error aggregation, and message quality.
 */
@DisplayName("QueryValidationException Tests")
class QueryValidationExceptionTest {

    @Nested
    @DisplayName("ValidationError Builder")
    class ValidationErrorBuilder {

        @Test
        @DisplayName("Should build ValidationError with all fields")
        void shouldBuildWithAllFields() {
            ValidationError error = ValidationError.builder()
                    .queryId("users.byId")
                    .location("classpath:queries/users.yaml")
                    .errorType("MISSING_RAW")
                    .message("Query 'raw' field is required")
                    .suggestion("Add a 'raw:' field with the query template")
                    .build();

            assertEquals("users.byId", error.getQueryId());
            assertEquals("classpath:queries/users.yaml", error.getLocation());
            assertEquals("MISSING_RAW", error.getErrorType());
            assertEquals("Query 'raw' field is required", error.getMessage());
            assertEquals("Add a 'raw:' field with the query template", error.getSuggestion());
        }

        @Test
        @DisplayName("Should build ValidationError with null suggestion")
        void shouldBuildWithNullSuggestion() {
            ValidationError error = ValidationError.builder()
                    .queryId("users.byId")
                    .location("test.yaml")
                    .errorType("ERROR")
                    .message("Some error")
                    .build();

            assertNull(error.getSuggestion());
        }
    }

    @Nested
    @DisplayName("Exception Message Formatting")
    class ExceptionMessageFormatting {

        @Test
        @DisplayName("Should format message with single error")
        void shouldFormatMessageWithSingleError() {
            List<ValidationError> errors = Collections.singletonList(
                    ValidationError.builder()
                            .queryId("users.byId")
                            .location("users.yaml")
                            .errorType("MISSING_RAW")
                            .message("Raw field is missing")
                            .suggestion("Add raw field")
                            .build()
            );

            QueryValidationException exception = new QueryValidationException(
                    "Validation failed", errors);

            String message = exception.getMessage();

            assertTrue(message.contains("VALIDATION ERRORS (1 found)"),
                    "Should show error count");
            assertTrue(message.contains("users.byId"),
                    "Should include query ID");
            assertTrue(message.contains("users.yaml"),
                    "Should include location");
            assertTrue(message.contains("MISSING_RAW"),
                    "Should include error type");
            assertTrue(message.contains("Raw field is missing"),
                    "Should include error message");
            assertTrue(message.contains("Add raw field"),
                    "Should include suggestion");
        }

        @Test
        @DisplayName("Should format message with multiple errors")
        void shouldFormatMessageWithMultipleErrors() {
            List<ValidationError> errors = Arrays.asList(
                    ValidationError.builder()
                            .queryId("users.byId")
                            .location("users.yaml")
                            .errorType("ERROR1")
                            .message("First error")
                            .build(),
                    ValidationError.builder()
                            .queryId("orders.byId")
                            .location("orders.yaml")
                            .errorType("ERROR2")
                            .message("Second error")
                            .build(),
                    ValidationError.builder()
                            .queryId("products.byId")
                            .location("products.yaml")
                            .errorType("ERROR3")
                            .message("Third error")
                            .build()
            );

            QueryValidationException exception = new QueryValidationException(
                    "Validation failed", errors);

            String message = exception.getMessage();

            assertTrue(message.contains("VALIDATION ERRORS (3 found)"),
                    "Should show correct error count");
            assertTrue(message.contains("1."),
                    "Should number errors");
            assertTrue(message.contains("2."),
                    "Should have second error");
            assertTrue(message.contains("3."),
                    "Should have third error");
        }

        @Test
        @DisplayName("Should include visual separators")
        void shouldIncludeVisualSeparators() {
            List<ValidationError> errors = Collections.singletonList(
                    ValidationError.builder()
                            .queryId("test")
                            .location("test.yaml")
                            .errorType("ERROR")
                            .message("Test error")
                            .build()
            );

            QueryValidationException exception = new QueryValidationException(
                    "Validation failed", errors);

            String message = exception.getMessage();

            assertTrue(message.contains("="),
                    "Should include separator lines");
        }
    }

    @Nested
    @DisplayName("Exception Getters")
    class ExceptionGetters {

        @Test
        @DisplayName("Should return errors list")
        void shouldReturnErrorsList() {
            List<ValidationError> errors = Arrays.asList(
                    ValidationError.builder()
                            .queryId("error1")
                            .location("test.yaml")
                            .errorType("TYPE1")
                            .message("Message 1")
                            .build(),
                    ValidationError.builder()
                            .queryId("error2")
                            .location("test.yaml")
                            .errorType("TYPE2")
                            .message("Message 2")
                            .build()
            );

            QueryValidationException exception = new QueryValidationException(
                    "Validation failed", errors);

            assertEquals(2, exception.getErrors().size());
            assertEquals("error1", exception.getErrors().get(0).getQueryId());
            assertEquals("error2", exception.getErrors().get(1).getQueryId());
        }

        @Test
        @DisplayName("Should return empty list when no errors provided")
        void shouldReturnEmptyListWhenNoErrors() {
            QueryValidationException exception = new QueryValidationException(
                    "Simple message");

            assertNotNull(exception.getErrors());
            assertTrue(exception.getErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("Exception with Cause")
    class ExceptionWithCause {

        @Test
        @DisplayName("Should preserve cause")
        void shouldPreserveCause() {
            RuntimeException cause = new RuntimeException("Root cause");
            QueryValidationException exception = new QueryValidationException(
                    "Validation error", cause);

            assertEquals(cause, exception.getCause());
            assertTrue(exception.getMessage().contains("Validation error"));
        }
    }

    @Nested
    @DisplayName("Error Message Without Suggestion")
    class ErrorMessageWithoutSuggestion {

        @Test
        @DisplayName("Should format correctly when suggestion is null")
        void shouldFormatWithoutSuggestion() {
            List<ValidationError> errors = Collections.singletonList(
                    ValidationError.builder()
                            .queryId("test.query")
                            .location("test.yaml")
                            .errorType("ERROR")
                            .message("Error without suggestion")
                            .suggestion(null)
                            .build()
            );

            QueryValidationException exception = new QueryValidationException(
                    "Validation failed", errors);

            String message = exception.getMessage();

            assertTrue(message.contains("Error without suggestion"));
            // Should not throw NPE or have "null" in output
            assertFalse(message.contains("Fix:      null"));
        }
    }

    @Nested
    @DisplayName("Error Numbering")
    class ErrorNumbering {

        @Test
        @DisplayName("Should number errors sequentially")
        void shouldNumberErrorsSequentially() {
            List<ValidationError> errors = Arrays.asList(
                    ValidationError.builder()
                            .queryId("first").location("test.yaml")
                            .errorType("E1").message("First").build(),
                    ValidationError.builder()
                            .queryId("second").location("test.yaml")
                            .errorType("E2").message("Second").build(),
                    ValidationError.builder()
                            .queryId("third").location("test.yaml")
                            .errorType("E3").message("Third").build()
            );

            QueryValidationException exception = new QueryValidationException(
                    "Validation failed", errors);

            String message = exception.getMessage();

            // Check sequential numbering
            int index1 = message.indexOf("1. [E1]");
            int index2 = message.indexOf("2. [E2]");
            int index3 = message.indexOf("3. [E3]");

            assertTrue(index1 >= 0, "Should have first numbered error");
            assertTrue(index2 > index1, "Second error should come after first");
            assertTrue(index3 > index2, "Third error should come after second");
        }
    }
}
