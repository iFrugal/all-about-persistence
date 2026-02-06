package lazydevs.readeasy.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when query validation fails during startup or runtime.
 * Provides detailed, user-friendly error messages to help developers
 * quickly identify and fix configuration issues.
 *
 * @author Abhijeet Rai
 */
public class QueryValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    public QueryValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }

    public QueryValidationException(String message, List<ValidationError> errors) {
        super(formatMessage(message, errors));
        this.errors = errors;
    }

    public QueryValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = new ArrayList<>();
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    private static String formatMessage(String message, List<ValidationError> errors) {
        StringBuilder sb = new StringBuilder(message);
        sb.append("\n\n");
        sb.append("=".repeat(60)).append("\n");
        sb.append("VALIDATION ERRORS (").append(errors.size()).append(" found)\n");
        sb.append("=".repeat(60)).append("\n\n");

        for (int i = 0; i < errors.size(); i++) {
            ValidationError error = errors.get(i);
            sb.append(String.format("%d. [%s] %s\n", i + 1, error.getErrorType(), error.getQueryId()));
            sb.append("   Location: ").append(error.getLocation()).append("\n");
            sb.append("   Problem:  ").append(error.getMessage()).append("\n");
            if (error.getSuggestion() != null) {
                sb.append("   Fix:      ").append(error.getSuggestion()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Represents a single validation error with context information.
     */
    public static class ValidationError {
        private final String queryId;
        private final String location;
        private final String errorType;
        private final String message;
        private final String suggestion;

        public ValidationError(String queryId, String location, String errorType, String message, String suggestion) {
            this.queryId = queryId;
            this.location = location;
            this.errorType = errorType;
            this.message = message;
            this.suggestion = suggestion;
        }

        public String getQueryId() { return queryId; }
        public String getLocation() { return location; }
        public String getErrorType() { return errorType; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String queryId;
            private String location;
            private String errorType;
            private String message;
            private String suggestion;

            public Builder queryId(String queryId) { this.queryId = queryId; return this; }
            public Builder location(String location) { this.location = location; return this; }
            public Builder errorType(String errorType) { this.errorType = errorType; return this; }
            public Builder message(String message) { this.message = message; return this; }
            public Builder suggestion(String suggestion) { this.suggestion = suggestion; return this; }

            public ValidationError build() {
                return new ValidationError(queryId, location, errorType, message, suggestion);
            }
        }
    }
}
