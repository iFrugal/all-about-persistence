package lazydevs.services.basic.filter;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j @Getter @Setter @EqualsAndHashCode
public class RequestContext extends ConcurrentHashMap<String, Object> implements Supplier {

    private static final ThreadLocal<RequestContext> CONTEXT_HOLDER = ThreadLocal.withInitial(RequestContext::new);

    private String userId;
    private String tenantCode;
    private String selectedTenantCode;
    private String role;
    private String requestId;
    private Map<String, String[]> params = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private transient Map<String, Object> body;
    private String requestUri;
    private HttpMethod httpMethod;

    public static RequestContext current() {
        return CONTEXT_HOLDER.get();
    }

    public static void reset(){
        CONTEXT_HOLDER.remove();
    }


    public String getParameter(String name) {
        String[] value = getParameterValues(name);
        return value == null ? null : value[0];
    }

    public String[] getParameterValues(String name) {
        return this.params.get(name);
    }

    public void set(String key, Object value) {
        if (value != null) {
            this.put(key, value);
        } else {
            this.remove(key);
        }
    }

    @Override
    public void clear() {
        super.clear();
        RequestContext context = current();
        context.setUserId(null);
        context.setTenantCode(null);
        context.setSelectedTenantCode(null);
        context.setRole(null);
        context.setParams(new HashMap<>());
        context.setHeaders(new HashMap<>());
        context.setBody(null);
        context.setHttpMethod(null);
    }

    @Override
    public Map<String, Object> get() {
        Map<String, Object> map = new HashMap<>();
        RequestContext context = current();
        map.put("userId", context.getUserId());
        map.put("tenantCode", context.getTenantCode());
        map.put("selectedTenantCode", context.getSelectedTenantCode());
        map.put("role", context.getRole());
        map.put("params", context.getParams());
        map.put("headers", context.getHeaders());
        map.put("body", context.getBody());
        map.putAll(context);
        return map;
    }


}