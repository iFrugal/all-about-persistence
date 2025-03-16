package lazydevs.mapper.rest;

import lazydevs.mapper.rest.multipart.Body;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ToString @Getter @Setter
public class RestInput {
    private HttpMethod httpMethod;
    private String url;
    private String queryParams;
    private Map<String, String> headers;
    private String payload;
    private Map<String, Object> payloadObject;
    private List<Body> bodies;
    private boolean multipart = false;
    private boolean closeResponse = true;

    public RestInput(){ }

    public RestInput(RestInput restInput) {
        httpMethod = restInput.getHttpMethod();
        url = restInput.getUrl();
        queryParams = restInput.getQueryParams();
        headers = restInput.getHeaders() == null ? null : new LinkedHashMap<>(restInput.getHeaders());
        payload = restInput.getPayload();
        payloadObject = restInput.getPayloadObject();
        bodies = restInput.getBodies() == null ? null : new ArrayList<>(restInput.getBodies());
        multipart = restInput.isMultipart();
        closeResponse = restInput.isCloseResponse();
    }

    public RestInput(@NonNull HttpMethod httpMethod, @NonNull String url){
        this.httpMethod = httpMethod;
        this.url = url;
    }

    public enum HttpMethod{
        GET, POST, PUT, PATCH, DELETE;
    }

    public static class MultipartRestInputBuilder extends BaseRestInputBuilder{
        private List<Body> bodies = new ArrayList<>();
        private MultipartRestInputBuilder(){ }

        public MultipartRestInputBuilder addPart(@NonNull Body payload){
            this.bodies.add(payload); return this;
        }

        public MultipartRestInputBuilder httpMethod(@NonNull HttpMethod httpMethod){
            super.httpMethod = httpMethod; return this;
        }

        public MultipartRestInputBuilder url(@NonNull String url){
            super.url = url; return this;
        }

        public MultipartRestInputBuilder queryParams(@NonNull String queryParams){
            super.queryParams = queryParams; return this;
        }

        public MultipartRestInputBuilder addHeader(@NonNull String key, @NonNull String value){
            super.headers.put(key, value); return this;
        }

        public MultipartRestInputBuilder addHeaders(Map<String, String> headers){
            super.headers.putAll(headers); return this;
        }

        public RestInput build(){
            RestInput restInput = new RestInput(super.httpMethod, super.url);
            restInput.setHeaders(super.headers);
            restInput.setQueryParams(super.queryParams);
            restInput.setBodies(this.bodies);
            restInput.setMultipart(true);
            return restInput;
        }

    }

    public static class RestInputBuilder extends BaseRestInputBuilder{
        private String payload;

        private RestInputBuilder(){ }

        public RestInputBuilder payload(@NonNull String payload){
            this.payload = payload; return this;
        }

        public RestInputBuilder httpMethod(@NonNull HttpMethod httpMethod){
            super.httpMethod = httpMethod; return this;
        }

        public RestInputBuilder url(@NonNull String url){
            super.url = url; return this;
        }

        public RestInputBuilder queryParams(@NonNull String queryParams){
            super.queryParams = queryParams; return this;
        }

        public RestInputBuilder addHeader(@NonNull String key, @NonNull String value){
            super.headers.put(key, value); return this;
        }

        public RestInputBuilder addHeaders(Map<String, String> headers){
            super.headers.putAll(headers); return this;
        }

        public RestInput build(){
            RestInput restInput = new RestInput(super.httpMethod, super.url);
            restInput.setHeaders(super.headers);
            restInput.setQueryParams(super.queryParams);
            restInput.setPayload(this.payload);
            return restInput;
        }

    }

    public static class BaseRestInputBuilder{
        private HttpMethod httpMethod;
        private String url;
        private String queryParams;
        private Map<String, String> headers = new LinkedHashMap<>();

        protected BaseRestInputBuilder(){ }
    }

    public static RestInputBuilder builder(){
        return new RestInputBuilder();
    }

    public static MultipartRestInputBuilder multipartBuilder(){
        return new MultipartRestInputBuilder();
    }

}
