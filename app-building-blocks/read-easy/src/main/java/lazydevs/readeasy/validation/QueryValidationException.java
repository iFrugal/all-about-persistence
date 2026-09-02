package lazydevs.readeasy.validation;

import lazydevs.services.basic.exception.RESTException;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * Thrown when query YAML validation fails. At startup (with failOnError enabled)
 * it aborts the boot with a readable error listing; thrown from the admin
 * /read/register endpoint it maps to HTTP 400 via the shared RESTException handling.
 *
 * @author Abhijeet Rai
 */
public class QueryValidationException extends RESTException {

    @Getter
    private final List<ValidationError> errors;

    public QueryValidationException(String message, List<ValidationError> errors) {
        super(formatMessage(message, errors), HttpURLConnection.HTTP_BAD_REQUEST);
        this.errors = List.copyOf(errors);
    }

    private static String formatMessage(String message, List<ValidationError> errors) {
        StringBuilder sb = new StringBuilder(message);
        sb.append("\nVALIDATION ERRORS (").append(errors.size()).append(" found):\n");
        for (int i = 0; i < errors.size(); i++) {
            ValidationError error = errors.get(i);
            sb.append(String.format("%d. [%s] %s%n", i + 1, error.getErrorType(), error.getQueryId()));
            sb.append("   Location: ").append(error.getLocation()).append("\n");
            sb.append("   Problem:  ").append(error.getMessage()).append("\n");
            if (null != error.getSuggestion()) {
                sb.append("   Fix:      ").append(error.getSuggestion()).append("\n");
            }
        }
        return sb.toString();
    }

    /** A single validation problem with enough context to locate and fix it. */
    @Value
    @Builder
    public static class ValidationError {
        String queryId;
        String location;
        String errorType;
        String message;
        String suggestion;
    }
}
