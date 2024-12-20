package lazydevs.mapper.rest;

import lazydevs.mapper.utils.SerDe;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ToString @Getter
public class RestOutput implements Closeable {

    private final int statusCode;
    private final String statusDesc;
    private final Map<String, String> headers;
    private final InputStream payloadInputStream;
    private final String payloadAsString;
    private final Closeable closeable;

    @Override
    public void close() throws IOException {
        if(this.closeable != null){
            this.closeable.close();
        }
    }

    public Map<String, Object> getPayloadAsMap(SerDe serDe){
        return null == payloadAsString ? serDe.deserializeToMap(payloadInputStream) : serDe.deserializeToMap(payloadAsString);
    }

    public <T> T getPayload(Class<T> tClass, SerDe serDe){
        return null == payloadAsString ? serDe.deserialize(payloadInputStream, tClass) : serDe.deserialize(payloadAsString, tClass);
    }

    public <T> List<T> getPayloadAsList(Class<T> tClass, SerDe serDe){
        return null == payloadAsString ? serDe.deserializeToList(payloadInputStream, tClass) : serDe.deserializeToList(payloadAsString, tClass);
    }

    public List<Map<String, Object>> getPayloadAsListOfMap(SerDe serDe){
        return null == payloadAsString ? serDe.deserializeToListOfMap(payloadInputStream) : serDe.deserializeToListOfMap(payloadAsString);
    }


    public Map<String, Object> getJsonPayloadAsMap(){
        return getPayloadAsMap(SerDe.JSON);
    }

    public <T> T getPayload(Class<T> tClass){
        return getPayload(tClass, SerDe.JSON);
    }

    public <T> List<T> getPayloadAsList(Class<T> tClass){
        return getPayloadAsList(tClass, SerDe.JSON);
    }

    public List<Map<String, Object>> getPayloadAsListOfMap(){
        return getPayloadAsListOfMap(SerDe.JSON);
    }



    private RestOutput(int statusCode, String statusDesc, Map<String, String> headers, String payloadAsString) {
        this.statusCode = statusCode;
        this.statusDesc = statusDesc;
        this.headers = headers;
        this.payloadAsString = payloadAsString;
        this.payloadInputStream = null;
        this.closeable = null;
    }

    private RestOutput(int statusCode, String statusDesc, Map<String, String> headers, final InputStream payloadInputStream, Closeable closeable) {
        this.statusCode = statusCode;
        this.statusDesc = statusDesc;
        this.headers = headers;
        this.payloadInputStream = payloadInputStream;
        this.closeable = closeable;
        this.payloadAsString = null;
    }

    public String getHeader(@NonNull String key) {
        return headers.get(key);
    }



    public static class RestOutputBuilder{
        private int statusCode;
        private String statusDesc;
        private Map<String, String> headers;
        private InputStream payloadInputStream;
        private String payloadAsString;
        private Closeable closable;


        private RestOutputBuilder(){}

        public RestOutputBuilder statusCode(@NonNull int statusCode){
            this.statusCode = statusCode; return this;
        }

        public RestOutputBuilder statusDesc(@NonNull String statusDesc){
            this.statusDesc = statusDesc; return this;
        }

        public RestOutputBuilder payloadInputStream(@NonNull InputStream payloadInputStream, Closeable closeable){
            this.payloadInputStream = payloadInputStream;
            this.closable = closeable;
            return this;
        }

        public RestOutputBuilder payloadAsString(@NonNull String payloadAsString){
            this.payloadAsString = payloadAsString; return this;
        }

        public RestOutputBuilder addHeader(@NonNull String key, @NonNull String value){
            if(null == this.headers){
                this.headers = new LinkedHashMap<>();
            }
            this.headers.put(key, value); return this;
        }

        public RestOutputBuilder headers(@NonNull Map<String, String> headers){
            if(null == this.headers){
                this.headers = new LinkedHashMap<>();
            }
            this.headers.putAll(headers); return this;
        }

        public RestOutput build(){
            if(null == payloadInputStream) {
                return new RestOutput(statusCode, statusDesc, headers,  payloadAsString);
            }
            return new RestOutput(statusCode, statusDesc, headers, payloadInputStream, closable);
        }
    }

    public static RestOutputBuilder builder(){
        return new RestOutputBuilder();
    }

}
