package lazydevs.services.basic.handler;

import lazydevs.services.basic.exception.RESTException;
import lazydevs.services.basic.filter.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;

import static lazydevs.services.basic.filter.RequestContext.current;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@ControllerAdvice
@Slf4j
public class RESTExceptionHandler {


    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ExceptionDetails> handleValidationExceptions(Exception ex, WebRequest request) {
        return handle("Received Validation Exception", HttpStatus.BAD_REQUEST,  ex , request);
    }

    @ExceptionHandler(RESTException.class)
    public ResponseEntity<ExceptionDetails> handleRESTExceptions(RESTException ex, WebRequest request) {
        return handle("Received RESTException", HttpStatus.valueOf(ex.getStatusCode()),  ex , request);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDetails> handleAllExceptions(Exception ex, WebRequest request) {
        return handle("Received unexpected exception", INTERNAL_SERVER_ERROR,  ex , request);
    }

    protected ResponseEntity<ExceptionDetails> handle(String logMessage, HttpStatus httpStatus, Exception ex, WebRequest request){
        log.error(logMessage + stackTraceToString(ex));
        ExceptionDetails errorDetails = new ExceptionDetails( ex.getMessage(), request.getDescription( false ));
        return new ResponseEntity<>( errorDetails, httpStatus);
    }

    public static String stackTraceToString(Throwable t){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        return sw.toString();
    }
}


