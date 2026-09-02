package lazydevs.readeasy.validation;

import lazydevs.services.basic.exception.RESTException;
import lombok.Getter;

import java.net.HttpURLConnection;

/**
 * Thrown when a registered query fails to render or parse at request time.
 * Extends RESTException so the shared handler maps it to HTTP 400 (these
 * failures are typically caused by the caller's parameters).
 *
 * <p>The message is intentionally client-safe: it names the query and the kind
 * of failure but never embeds the query template or the rendered query text.
 * Full details belong in the server log at the throw site.</p>
 *
 * @author Abhijeet Rai
 */
public class QueryTemplateException extends RESTException {

    private static final int MAX_CAUSE_SUMMARY_LENGTH = 200;

    @Getter
    private final String queryId;

    public QueryTemplateException(String queryId, String clientSafeMessage, Throwable cause) {
        super(clientSafeMessage, cause, HttpURLConnection.HTTP_BAD_REQUEST);
        this.queryId = queryId;
    }

    /**
     * A short, single-line summary of the root cause, safe to include in a client
     * message: FreeMarker's variable-reference errors name the missing variable
     * (useful to the caller) without exposing the query text.
     */
    public static String summarize(Throwable cause) {
        Throwable root = cause;
        while (null != root.getCause()) {
            root = root.getCause();
        }
        String message = root.getMessage();
        if (null == message || message.isEmpty()) {
            return root.getClass().getSimpleName();
        }
        message = message.replaceAll("\\s+", " ").trim();
        if (message.length() > MAX_CAUSE_SUMMARY_LENGTH) {
            message = message.substring(0, MAX_CAUSE_SUMMARY_LENGTH) + "...";
        }
        return message;
    }
}
