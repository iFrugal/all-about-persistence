package lazydevs.readeasy.validation;

/**
 * Exception thrown when a query template fails to render at runtime.
 * Provides user-friendly error messages with context about what went wrong.
 *
 * @author Abhijeet Rai
 */
public class QueryTemplateException extends RuntimeException {

    private final String queryId;
    private final String templateSnippet;
    private final String suggestion;

    public QueryTemplateException(String queryId, String message, Throwable cause) {
        super(formatMessage(queryId, message, null, null), cause);
        this.queryId = queryId;
        this.templateSnippet = null;
        this.suggestion = null;
    }

    public QueryTemplateException(String queryId, String message, String templateSnippet, String suggestion, Throwable cause) {
        super(formatMessage(queryId, message, templateSnippet, suggestion), cause);
        this.queryId = queryId;
        this.templateSnippet = templateSnippet;
        this.suggestion = suggestion;
    }

    private static String formatMessage(String queryId, String message, String templateSnippet, String suggestion) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("═".repeat(60)).append("\n");
        sb.append("QUERY TEMPLATE ERROR\n");
        sb.append("═".repeat(60)).append("\n\n");

        sb.append("Query ID: ").append(queryId).append("\n");
        sb.append("Error:    ").append(message).append("\n");

        if (templateSnippet != null && !templateSnippet.isEmpty()) {
            sb.append("\nTemplate snippet:\n");
            sb.append("─".repeat(40)).append("\n");
            sb.append(truncateSnippet(templateSnippet, 500)).append("\n");
            sb.append("─".repeat(40)).append("\n");
        }

        if (suggestion != null && !suggestion.isEmpty()) {
            sb.append("\nSuggestion: ").append(suggestion).append("\n");
        }

        sb.append("\n").append("═".repeat(60));

        return sb.toString();
    }

    private static String truncateSnippet(String snippet, int maxLength) {
        if (snippet.length() <= maxLength) {
            return snippet;
        }
        return snippet.substring(0, maxLength) + "\n... (truncated)";
    }

    public String getQueryId() {
        return queryId;
    }

    public String getTemplateSnippet() {
        return templateSnippet;
    }

    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Creates a QueryTemplateException from a FreeMarker or other template error.
     *
     * @param queryId The query ID that failed
     * @param template The template that was being processed
     * @param cause The original exception
     * @return A QueryTemplateException with helpful context
     */
    public static QueryTemplateException fromTemplateError(String queryId, String template, Throwable cause) {
        String message = extractErrorMessage(cause);
        String suggestion = suggestFix(message, cause);

        return new QueryTemplateException(queryId, message, template, suggestion, cause);
    }

    private static String extractErrorMessage(Throwable cause) {
        String msg = cause.getMessage();
        if (msg == null || msg.isEmpty()) {
            return cause.getClass().getSimpleName();
        }

        // Clean up FreeMarker error messages
        if (msg.contains("FreeMarker")) {
            // Extract the useful part of the message
            int idx = msg.indexOf(":");
            if (idx > 0 && idx < msg.length() - 1) {
                msg = msg.substring(idx + 1).trim();
            }
        }

        // Limit message length
        if (msg.length() > 300) {
            msg = msg.substring(0, 300) + "...";
        }

        return msg;
    }

    private static String suggestFix(String message, Throwable cause) {
        String lowerMsg = message.toLowerCase();

        if (lowerMsg.contains("undefined") || lowerMsg.contains("null")) {
            return "A variable is null or undefined. Use the ?? operator to check existence " +
                    "(e.g., <#if params.name??>) or provide a default with ! (e.g., ${params.name!'default'})";
        }

        if (lowerMsg.contains("cannot be evaluated") || lowerMsg.contains("expecting")) {
            return "FreeMarker syntax error. Check for typos in directives like <#if>, <#list>, etc. " +
                    "Ensure all tags are properly closed.";
        }

        if (lowerMsg.contains("hash") || lowerMsg.contains("string")) {
            return "Type mismatch. You may be trying to use a string as a map or vice versa. " +
                    "Check your variable types.";
        }

        if (lowerMsg.contains("sequence") || lowerMsg.contains("list")) {
            return "Sequence/list error. Use <#list items as item> for iteration. " +
                    "Check that the variable is actually a list.";
        }

        if (lowerMsg.contains("json") || lowerMsg.contains("parse")) {
            return "The rendered template may not be valid JSON. Check for unescaped quotes, " +
                    "missing commas, or other JSON syntax issues.";
        }

        return "Check the template syntax and ensure all required parameters are provided.";
    }
}
