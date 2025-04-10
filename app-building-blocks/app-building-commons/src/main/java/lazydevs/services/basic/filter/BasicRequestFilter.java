package lazydevs.services.basic.filter;

import lazydevs.persistence.connection.multitenant.TenantContext;
import lazydevs.services.basic.exception.ValidationException;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.*;

import static java.lang.String.format;
import static lazydevs.services.basic.filter.RequestContext.current;


/**
 * @author Abhijeet Rai
 */
public class BasicRequestFilter extends OncePerRequestFilter {
    @Autowired private HandlerExceptionResolver handlerExceptionResolver;

    public static final String USER_ID_HEADER = "x-user-id";
    public static final String ROLE_HEADER = "x-role";
    public static final String REQUEST_ID_HEADER = "x-request-id";

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) {
        try {
            setRequestContext(httpServletRequest, httpServletResponse);
            setApplicationSpecificAttributes();
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        }catch (Exception e){
            handlerExceptionResolver.resolveException(httpServletRequest, httpServletResponse, null, e);
        } finally {
            MDC.clear();
            current().clear();
            TenantContext.reset();
        }
    }

    public void setRequestContext(HttpServletRequest request, HttpServletResponse response){
        RequestContext context = current();
        context.setRequestUri(request.getRequestURI());
        context.setHttpMethod(HttpMethod.valueOf(request.getMethod()));
        MDC.put("request", format("'%s'-'%s'", context.getRequestUri(), context.getHttpMethod()));
        context.setParams(new LinkedHashMap<>(request.getParameterMap()));
        Enumeration<String> headerNames = request.getHeaderNames();
        final Map<String, String> headerMap = new HashMap<>();
        while(headerNames.hasMoreElements()){
            String headerName = headerNames.nextElement();
            headerMap.put(headerName, request.getHeader(headerName));
        }
        context.setHeaders(headerMap);
        context.setUserId(extractAndSetToMDC(USER_ID_HEADER, false));
        context.setRole(extractAndSetToMDC(ROLE_HEADER, false));
        context.setRequestId(extractAndSetToMDC(REQUEST_ID_HEADER, false));
        if(null == context.getRequestId()){
            context.setRequestId(UUID.randomUUID().toString());
            MDC.put(REQUEST_ID_HEADER, context.getRequestId());
        }
        response.addHeader(REQUEST_ID_HEADER, context.getRequestId());
        TenantContext.setTenantId(StringUtils.hasText(context.getSelectedTenantCode())? context.getTenantCode() : context.getSelectedTenantCode());
    }

    protected void setApplicationSpecificAttributes(){
        // Do nothing
    }

    private String extractAndSetToMDC(String key, boolean required){
        String val = current().getHeaders().get(key);
        if(null == val){
            val = current().getParameter(key);
        }
        if(required && StringUtils.isEmpty(val)){
            throw new ValidationException(format("Param '%s' can't be null or empty. requestUri = %s", key, current().getRequestUri()));
        }
        MDC.put(key, val);
        return val;
    }

}