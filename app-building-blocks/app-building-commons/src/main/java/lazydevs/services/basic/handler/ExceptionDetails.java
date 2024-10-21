package lazydevs.services.basic.handler;

import lazydevs.services.basic.filter.RequestContext;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.MDC;

import java.util.Date;

import static lazydevs.services.basic.filter.BasicRequestFilter.REQUEST_ID_HEADER;
import static lazydevs.services.basic.filter.RequestContext.current;

@Getter @Setter @ToString
public class ExceptionDetails {
    private Date timestamp;
    private String message;
    private String details;
    private String requestId;
    
    public ExceptionDetails(String message, String details) {
        this.timestamp = new Date();
        this.message = message;
        this.details = details;
        this.requestId = current().getRequestId();
    }

}
