package lazydevs.services.basic.exception;

import java.net.HttpURLConnection;

public class ValidationException extends RESTException {
    
    public ValidationException(String message, Throwable cause) {
        super( message, cause, HttpURLConnection.HTTP_BAD_REQUEST );
    }
    
    public ValidationException(String message) {
        super( message, HttpURLConnection.HTTP_BAD_REQUEST  );
    }
}
