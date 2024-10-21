package lazydevs.conman;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.networknt.schema.JsonSchema;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.services.basic.validation.Param;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import java.util.Map;
import java.util.Set;

@Getter @Setter @ToString
public class MockConfig {

    private String tenantId;
    private Set<String> tenantIds;
    private Request request;
    private Response response;

    @Getter @Setter @ToString
    public static class Request{
        private String uri;
        private HttpMethod httpMethod;
        private RequestValidation validation;
    }

    @Getter @Setter @ToString
    public static class RequestValidation{
        private String bodySchema;
        @JsonIgnore private JsonSchema bodySchemaInternal;
        private Map<String, Param> headers;
        private Map<String, Param> queryParams;
    }

    @Getter @Setter @ToString
    public static class Response{
        private Map<String, Object> bodyObj;
        private String body;
        private String contentType;
        private int statusCode;
        private Map<String, String> responseHeaders;
        private boolean bodyTemplate;
    }

    public byte[] resolveBodyBytes(Map<String, Object> params) {
        String bodyLocal = this.response.body;
        if(this.response.bodyObj != null){
            bodyLocal = SerDe.JSON.serialize(this.response.bodyObj, true);
        }
        if(this.response.bodyTemplate){
            bodyLocal = TemplateEngine.getInstance().generate(bodyLocal, params);
        }
        return StringUtils.isEmpty(bodyLocal) ? new byte[0] : bodyLocal.getBytes();
    }
}
