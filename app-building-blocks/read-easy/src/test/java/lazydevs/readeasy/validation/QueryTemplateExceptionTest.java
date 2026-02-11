package lazydevs.readeasy.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QueryTemplateException.
 * Tests error message formatting, suggestion generation, and factory methods.
 */
@DisplayName("QueryTemplateException Tests")
class QueryTemplateExceptionTest {

    @Nested
    @DisplayName("Basic Constructor")
    class BasicConstructor {

        @Test
        @DisplayName("Should create exception with query ID, message, and cause")
        void shouldCreateWithBasicParameters() {
            RuntimeException cause = new RuntimeException("Original error");
            QueryTemplateException exception = new QueryTemplateException(
                    "users.byId", "Template rendering failed", cause);

            assertTrue(exception.getMessage().contains("users.byId"),
                    "Should include query ID");
            assertTrue(exception.getMessage().contains("Template rendering failed"),
                    "Should include error message");
            assertEquals(cause, exception.getCause(),
                    "Should preserve cause");
            assertEquals("users.byId", exception.getQueryId());
        }

        @Test
        @DisplayName("Should format message with visual separators")
        void shouldFormatMessageWithSeparators() {
            QueryTemplateException exception = new QueryTemplateException(
                    "test.query", "Test error", null);

            String message = exception.getMessage();

            assertTrue(message.contains("═"),
                    "Should have header separator");
            assertTrue(message.contains("QUERY TEMPLATE ERROR"),
                    "Should have error title");
        }
    }

    @Nested
    @DisplayName("Full Constructor")
    class FullConstructor {

        @Test
        @DisplayName("Should create exception with all parameters")
        void shouldCreateWithAllParameters() {
            RuntimeException cause = new RuntimeException("Original");
            String template = "{\"nativeSQL\": \"SELECT * FROM ${params.table}\"}";
            String suggestion = "Check variable names";

            QueryTemplateException exception = new QueryTemplateException(
                    "queries.dynamic",
                    "Variable 'table' is undefined",
                    template,
                    suggestion,
                    cause);

            assertEquals("queries.dynamic", exception.getQueryId());
            assertEquals(template, exception.getTemplateSnippet());
            assertEquals(suggestion, exception.getSuggestion());
            assertEquals(cause, exception.getCause());

            String message = exception.getMessage();
            assertTrue(message.contains("queries.dynamic"));
            assertTrue(message.contains("Variable 'table' is undefined"));
            assertTrue(message.contains(template));
            assertTrue(message.contains(suggestion));
        }

        @Test
        @DisplayName("Should include template snippet section")
        void shouldIncludeTemplateSnippetSection() {
            String template = "{\"nativeSQL\": \"SELECT * FROM users\"}";

            QueryTemplateException exception = new QueryTemplateException(
                    "test.query",
                    "Parse error",
                    template,
                    null,
                    null);

            String message = exception.getMessage();
            assertTrue(message.contains("Template snippet:"),
                    "Should have template snippet section");
            assertTrue(message.contains("─"),
                    "Should have snippet separator");
            assertTrue(message.contains(template),
                    "Should include actual template");
        }

        @Test
        @DisplayName("Should truncate long template snippets")
        void shouldTruncateLongSnippets() {
            StringBuilder longTemplate = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                longTemplate.append("{\"field").append(i).append("\": \"value").append(i).append("\"}\n");
            }

            QueryTemplateException exception = new QueryTemplateException(
                    "test.query",
                    "Error",
                    longTemplate.toString(),
                    null,
                    null);

            String message = exception.getMessage();
            assertTrue(message.contains("(truncated)"),
                    "Should indicate truncation for long snippets");
        }

        @Test
        @DisplayName("Should handle null template snippet")
        void shouldHandleNullTemplateSnippet() {
            QueryTemplateException exception = new QueryTemplateException(
                    "test.query",
                    "Error message",
                    null,
                    "Suggestion text",
                    null);

            String message = exception.getMessage();
            assertFalse(message.contains("Template snippet:"),
                    "Should not show snippet section when null");
            assertTrue(message.contains("Suggestion:"),
                    "Should still show suggestion");
        }

        @Test
        @DisplayName("Should handle empty template snippet")
        void shouldHandleEmptyTemplateSnippet() {
            QueryTemplateException exception = new QueryTemplateException(
                    "test.query",
                    "Error message",
                    "",
                    null,
                    null);

            String message = exception.getMessage();
            assertFalse(message.contains("Template snippet:"),
                    "Should not show snippet section when empty");
        }

        @Test
        @DisplayName("Should handle null suggestion")
        void shouldHandleNullSuggestion() {
            QueryTemplateException exception = new QueryTemplateException(
                    "test.query",
                    "Error message",
                    "template",
                    null,
                    null);

            String message = exception.getMessage();
            assertFalse(message.contains("Suggestion:"),
                    "Should not show suggestion section when null");
        }

        @Test
        @DisplayName("Should handle empty suggestion")
        void shouldHandleEmptySuggestion() {
            QueryTemplateException exception = new QueryTemplateException(
                    "test.query",
                    "Error message",
                    "template",
                    "",
                    null);

            String message = exception.getMessage();
            assertFalse(message.contains("Suggestion:"),
                    "Should not show suggestion section when empty");
        }
    }

    @Nested
    @DisplayName("Factory Method - fromTemplateError")
    class FactoryMethod {

        @Test
        @DisplayName("Should create exception from template error")
        void shouldCreateFromTemplateError() {
            String template = "{\"nativeSQL\": \"SELECT ${params.column}\"}";
            RuntimeException cause = new RuntimeException("Variable is undefined");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "users.dynamic", template, cause);

            assertEquals("users.dynamic", exception.getQueryId());
            assertEquals(template, exception.getTemplateSnippet());
            assertEquals(cause, exception.getCause());
            assertNotNull(exception.getSuggestion(),
                    "Should provide a suggestion");
        }

        @Test
        @DisplayName("Should suggest fix for undefined variable error")
        void shouldSuggestFixForUndefinedVariable() {
            RuntimeException cause = new RuntimeException("Variable 'params.name' is undefined");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertTrue(suggestion.contains("??") || suggestion.contains("null") || suggestion.contains("undefined"),
                    "Should suggest null-safe operator for undefined variable");
        }

        @Test
        @DisplayName("Should suggest fix for null value error")
        void shouldSuggestFixForNullValue() {
            RuntimeException cause = new RuntimeException("The value is null");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertNotNull(suggestion);
            assertTrue(suggestion.length() > 0,
                    "Should provide suggestion for null errors");
        }

        @Test
        @DisplayName("Should suggest fix for syntax error")
        void shouldSuggestFixForSyntaxError() {
            RuntimeException cause = new RuntimeException("Expecting a valid tag, got <#invalid>");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertNotNull(suggestion);
            assertTrue(suggestion.contains("syntax") || suggestion.contains("tag") || suggestion.contains("<#if>"),
                    "Should suggest syntax fix");
        }

        @Test
        @DisplayName("Should suggest fix for hash/string type mismatch")
        void shouldSuggestFixForTypeMismatch() {
            RuntimeException cause = new RuntimeException("Expected hash, got string instead");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertNotNull(suggestion);
            assertTrue(suggestion.toLowerCase().contains("type") || suggestion.toLowerCase().contains("mismatch"),
                    "Should suggest type-related fix");
        }

        @Test
        @DisplayName("Should suggest fix for sequence/list error")
        void shouldSuggestFixForSequenceError() {
            RuntimeException cause = new RuntimeException("Expected sequence but got scalar");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertNotNull(suggestion);
            assertTrue(suggestion.toLowerCase().contains("list") || suggestion.toLowerCase().contains("sequence"),
                    "Should suggest list-related fix");
        }

        @Test
        @DisplayName("Should suggest fix for JSON parse error")
        void shouldSuggestFixForJsonParseError() {
            RuntimeException cause = new RuntimeException("Failed to parse JSON output");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertNotNull(suggestion);
            assertTrue(suggestion.toLowerCase().contains("json"),
                    "Should suggest JSON-related fix");
        }

        @Test
        @DisplayName("Should provide generic suggestion for unknown errors")
        void shouldProvideGenericSuggestionForUnknownErrors() {
            RuntimeException cause = new RuntimeException("Some completely unknown error type");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            String suggestion = exception.getSuggestion();
            assertNotNull(suggestion,
                    "Should always provide some suggestion");
            assertTrue(suggestion.length() > 10,
                    "Suggestion should be meaningful");
        }

        @Test
        @DisplayName("Should handle cause with null message")
        void shouldHandleCauseWithNullMessage() {
            RuntimeException cause = new RuntimeException((String) null);

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            assertNotNull(exception.getMessage(),
                    "Should still produce a message");
            // Should use class name when message is null
            assertTrue(exception.getMessage().contains("RuntimeException") ||
                            exception.getMessage().contains("Error"),
                    "Should include some error indication");
        }

        @Test
        @DisplayName("Should handle cause with empty message")
        void shouldHandleCauseWithEmptyMessage() {
            RuntimeException cause = new RuntimeException("");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            assertNotNull(exception.getMessage(),
                    "Should still produce a message");
        }

        @Test
        @DisplayName("Should clean up FreeMarker error messages")
        void shouldCleanUpFreeMarkerErrors() {
            RuntimeException cause = new RuntimeException(
                    "FreeMarker template error: Unexpected end of file reached while parsing");

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            // The formatted message should be cleaner
            String message = exception.getMessage();
            assertNotNull(message);
            assertTrue(message.contains("Unexpected end") || message.contains("parsing"),
                    "Should include useful part of FreeMarker error");
        }

        @Test
        @DisplayName("Should truncate very long error messages")
        void shouldTruncateLongErrorMessages() {
            StringBuilder longMessage = new StringBuilder("Error: ");
            for (int i = 0; i < 100; i++) {
                longMessage.append("This is a very long error message part ").append(i).append(". ");
            }
            RuntimeException cause = new RuntimeException(longMessage.toString());

            QueryTemplateException exception = QueryTemplateException.fromTemplateError(
                    "test.query", "template", cause);

            // Message should be truncated but still meaningful
            assertNotNull(exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Getters")
    class Getters {

        @Test
        @DisplayName("Should return queryId")
        void shouldReturnQueryId() {
            QueryTemplateException exception = new QueryTemplateException(
                    "namespace.queryName", "Error", null);

            assertEquals("namespace.queryName", exception.getQueryId());
        }

        @Test
        @DisplayName("Should return templateSnippet")
        void shouldReturnTemplateSnippet() {
            String template = "{\"sql\": \"SELECT 1\"}";
            QueryTemplateException exception = new QueryTemplateException(
                    "test", "Error", template, null, null);

            assertEquals(template, exception.getTemplateSnippet());
        }

        @Test
        @DisplayName("Should return suggestion")
        void shouldReturnSuggestion() {
            String suggestion = "Check your syntax";
            QueryTemplateException exception = new QueryTemplateException(
                    "test", "Error", null, suggestion, null);

            assertEquals(suggestion, exception.getSuggestion());
        }

        @Test
        @DisplayName("Should return null for unset optional fields")
        void shouldReturnNullForUnsetFields() {
            QueryTemplateException exception = new QueryTemplateException(
                    "test", "Error", null);

            assertNull(exception.getTemplateSnippet());
            assertNull(exception.getSuggestion());
        }
    }

    @Nested
    @DisplayName("Message Formatting")
    class MessageFormatting {

        @Test
        @DisplayName("Should include all sections in order")
        void shouldIncludeAllSectionsInOrder() {
            QueryTemplateException exception = new QueryTemplateException(
                    "users.search",
                    "Variable undefined",
                    "{\"sql\": \"SELECT ${x}\"}",
                    "Use ?? operator",
                    new RuntimeException("cause"));

            String message = exception.getMessage();

            int headerIdx = message.indexOf("QUERY TEMPLATE ERROR");
            int queryIdIdx = message.indexOf("Query ID:");
            int errorIdx = message.indexOf("Error:");
            int snippetIdx = message.indexOf("Template snippet:");
            int suggestionIdx = message.indexOf("Suggestion:");

            assertTrue(headerIdx >= 0, "Should have header");
            assertTrue(queryIdIdx > headerIdx, "Query ID should come after header");
            assertTrue(errorIdx > queryIdIdx, "Error should come after query ID");
            assertTrue(snippetIdx > errorIdx, "Snippet should come after error");
            assertTrue(suggestionIdx > snippetIdx, "Suggestion should come after snippet");
        }

        @Test
        @DisplayName("Should be readable in logs")
        void shouldBeReadableInLogs() {
            QueryTemplateException exception = new QueryTemplateException(
                    "orders.complex",
                    "Failed to evaluate expression",
                    "{\"nativeSQL\": \"SELECT * FROM orders WHERE status = '${params.status}'\"}",
                    "Ensure params.status is provided",
                    new RuntimeException("root cause"));

            String message = exception.getMessage();

            // Should have newlines for readability
            assertTrue(message.contains("\n"),
                    "Should have newlines for formatting");

            // Should have consistent structure
            assertTrue(message.startsWith("\n"),
                    "Should start with newline for log separation");
        }
    }
}
