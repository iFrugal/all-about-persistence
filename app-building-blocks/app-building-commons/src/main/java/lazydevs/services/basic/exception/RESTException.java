package lazydevs.services.basic.exception;

import lombok.Getter;

//Base exception for all the exceptions to be thrown by this microservice
@Getter
public class RESTException extends RuntimeException {
    
    private int statusCode;
    private String errorCode;
    private String errorDesc;
    
    public RESTException(String message, int statusCode) {
        super( message );
        this.statusCode = statusCode;
    }
    
    public RESTException(String message, Throwable cause, int statusCode) {
        super( message, cause );
        this.statusCode = statusCode;
    }


    public RESTException statusCode(int statusCode){
        this.statusCode = statusCode;
        return this;
    }

    public RESTException errorCode(String errorCode){
        this.errorCode = errorCode;
        return this;
    }
    public RESTException errorDesc(String errorDesc){
        this.errorDesc = errorDesc;
        return this;
    }
}
