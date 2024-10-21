package lazydevs.conman.validation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersionDetector;
import lazydevs.conman.ConmanCache;
import lazydevs.conman.MockConfig;
import lazydevs.mapper.utils.SerDe;
import lazydevs.services.basic.filter.RequestContext;
import lazydevs.services.basic.validation.ParamValidator;
import org.springframework.http.HttpMethod;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import static lazydevs.conman.validation.JsonSchemaValidator.convert;
import static lazydevs.conman.validation.JsonSchemaValidator.valid;

public class RequestValidator {
    private static ParamValidator paramValidator = new ParamValidator();
    public static void validate(HttpServletRequest req, HttpServletResponse res, MockConfig mockConfig, HttpMethod httpMethod, String uri, String tenantId){
        MockConfig.RequestValidation requestValidation = mockConfig.getRequest().getValidation();
        String key = ConmanCache.getKey(httpMethod, uri, tenantId);
        if(null != requestValidation){
            if(null != requestValidation.getHeaders()){
                paramValidator.validate(false, "Header", key, requestValidation.getHeaders(),
                        RequestContext.current().getHeaders().entrySet().stream().collect(Collectors.toMap(e-> e.getKey(), e-> e.getValue())));
            }
            if(null != requestValidation.getQueryParams()){
                paramValidator.validate(false, "Query", key, requestValidation.getQueryParams(),
                        RequestContext.current().getParams().entrySet().stream().collect(Collectors.toMap(e-> e.getKey(), e-> e.getValue())));
            }
            if(null != requestValidation.getBodySchema() && null == requestValidation.getBodySchemaInternal()) {
                JsonNode schemaJsonNode = convert(requestValidation.getBodySchema());
                JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersionDetector.detect(schemaJsonNode)).getSchema(schemaJsonNode);
                requestValidation.setBodySchemaInternal(schema);
            }
            if(null != requestValidation.getBodySchemaInternal()) {
                try {
                    JsonNode jsonNode = convert(req.getInputStream());
                    RequestContext.current().setBody(SerDe.JSON.getOBJECT_MAPPER().convertValue(jsonNode, new TypeReference<Map<String, Object>>(){}));
                    valid(jsonNode, requestValidation.getBodySchemaInternal());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
