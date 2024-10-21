package lazydevs.conman;

import lazydevs.conman.validation.RequestValidator;
import lazydevs.services.basic.filter.BasicRequestFilter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static lazydevs.services.basic.filter.RequestContext.current;

/**
 * @author Abhijeet Rai
 */
public class ConmanServlet extends HttpServlet {
    public static ConmanServlet getInstance(){
        return INSTANCE;
    }
    private static ConmanServlet INSTANCE;
    private final ConmanCache conmanCache;

    public ConmanServlet(ConmanCache conmanCache) {
        this.conmanCache = conmanCache;
        INSTANCE = this;
    }

    private Map<String, Object> decorateDatapoints(Map<String, Object> data){
        Map<String, Object> modifiedData =  new HashMap<>(data);
        modifiedData.put("request", current().get());
        return modifiedData;
    }

    private String getKey(HttpMethod httpMethod, String uri, String tenantId){
        return httpMethod.name() + "_" + uri + "_"+ (null == tenantId ? "null" : tenantId);
    }
    private Map<String, Object> convert(Map<String, String> map){
        return map.entrySet().stream().collect(Collectors.toMap(e-> e.getKey(), e-> e.getValue()));
    }

    void serviceInternal(HttpMethod httpMethod, String uri, String tenantId, HttpServletResponse resp, MockConfig data) throws IOException{
        resp.setContentType(data.getResponse().getContentType());
        Map<String, Object> map = new HashMap<>();
        map.put("request", current().get());
        resp.getOutputStream().write(data.resolveBodyBytes(map));
        resp.setStatus(data.getResponse().getStatusCode());
        if(null != data.getResponse().getResponseHeaders()) {
            for (Map.Entry<String, String> header : data.getResponse().getResponseHeaders().entrySet()) {
                resp.setHeader(header.getKey(), header.getValue());
            }
        }
    }
    private final BasicRequestFilter basicRequestFilter = new BasicRequestFilter();
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException{
        basicRequestFilter.setRequestContext(req, resp);
        HttpMethod httpMethod = HttpMethod.resolve(req.getMethod());
        String uri = req.getRequestURI();
        String tenantId = current().getTenantCode();
        MockConfig data = conmanCache.getMockConfig(httpMethod, uri, tenantId);
        if(data == null) {
            notFound(httpMethod, uri, tenantId, resp);
            return;
        }
        try {
            RequestValidator.validate(req, resp, data, httpMethod, uri, tenantId);
        }catch (Exception e){
            validationException(e, resp);
            return;
        }
        serviceInternal(httpMethod, uri, tenantId, resp, data);
    }

    private void notFound(HttpMethod httpMethod, String uri, String tenantId, HttpServletResponse resp) throws IOException{
        resp.setContentType("application/json");
        resp.setStatus(HttpStatus.NOT_FOUND.value());
        byte[] body =  String.format("{ \n" +
                        "\"status\": \"Conman mapping error\",\n" +
                        "\"message\": \"Mapping not found for method=%s, URI=%s, tenant-id=%s\" \n" +
                        "}\n",
                httpMethod, uri, tenantId).getBytes();
        resp.getOutputStream().write(body);
    }

    private void validationException(Exception e, HttpServletResponse resp) throws IOException{
        resp.setContentType("application/json");
        resp.setStatus(HttpStatus.BAD_REQUEST.value());
        byte[] body =  String.format("{ \n" +
                        "\"status\": \"Request Validation Failed\",\n" +
                        "\"message\": \"%s\" \n" +
                        "}\n",
                e.getMessage()).getBytes();
        resp.getOutputStream().write(body);
    }
}
