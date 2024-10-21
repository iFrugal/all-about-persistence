package lazydevs.services.basic.exception;

import java.net.HttpURLConnection;

public class ServerException extends RESTException {
    
    public ServerException(String message, Throwable cause) {
        super( message, cause, HttpURLConnection.HTTP_INTERNAL_ERROR );
    }
    
    public ServerException(String message) {
        super( message, HttpURLConnection.HTTP_INTERNAL_ERROR  );
    }
}
