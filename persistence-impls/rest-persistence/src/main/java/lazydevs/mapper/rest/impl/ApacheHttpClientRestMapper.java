package lazydevs.mapper.rest.impl;

import lazydevs.mapper.rest.RestInput;
import lazydevs.mapper.rest.RestMapper;
import lazydevs.mapper.rest.RestOutput;
import lazydevs.mapper.rest.multipart.Body;
import lazydevs.mapper.rest.multipart.Body.ByteArrayBody;
import lazydevs.mapper.rest.multipart.Body.FileBody;
import lazydevs.mapper.rest.multipart.Body.InputStreamBody;
import lazydevs.mapper.rest.multipart.Body.StringBody;
import lazydevs.mapper.utils.BatchIterator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class ApacheHttpClientRestMapper implements RestMapper {

    private final CloseableHttpClient closeableHttpClient;

    public ApacheHttpClientRestMapper(){
        RequestConfig globalConfig =  RequestConfig.custom()
                .setConnectTimeout(1000*10*2) //10 sec, connection establishment timeout
                .setConnectionRequestTimeout(1000 * 10 * 2) //10sec , connect request time, ping response from server
                .setSocketTimeout(6000 * 10 * 5) //60 secs, wait for the response to come from server
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setRedirectsEnabled(false)
                .build();

        closeableHttpClient = HttpClients.custom()
                .setDefaultRequestConfig(globalConfig)
                .setMaxConnTotal(500)
                .setMaxConnPerRoute(100)
                .setConnectionTimeToLive(600, TimeUnit.SECONDS)
                .evictIdleConnections(600, TimeUnit.SECONDS)
                .build();
    }

    public static HttpRequestBase getHTTPBase(RestInput restInput){
        try {
            HttpRequestBase httpRequestBase = null;
            String queryParams = restInput.getQueryParams();
            String path =  restInput.getUrl();
            if (null != queryParams && !queryParams.isEmpty()) {
                if (queryParams.startsWith("?")) {
                    path = path + queryParams;
                } else {
                    path = path + "?" + queryParams;
                }
            }
            httpRequestBase = getHttpRequestBase(restInput, path);
            String payload = restInput.getPayload();
            if (httpRequestBase instanceof HttpEntityEnclosingRequestBase) {
                setBody(restInput, (HttpEntityEnclosingRequestBase) httpRequestBase, payload);
            }
            if (null != restInput.getHeaders()) {
                httpRequestBase.setHeaders(convertHeaderMapToList(restInput.getHeaders()));
            }
            return httpRequestBase;
        }catch(Exception e){
            log.error("Exception while forming HTTPRequestBase", e);
            throw new IllegalArgumentException("Exception while forming HTTPRequestBase",e);
        }
    }

    private static HttpRequestBase getHttpRequestBase(RestInput restInput, String path) {
        HttpRequestBase httpRequestBase;
        switch (restInput.getHttpMethod()) {
            case GET:
                httpRequestBase = new HttpGet(path);
                break;
            case POST:
                httpRequestBase = new HttpPost(path);
                break;
            case PUT:
                httpRequestBase = new HttpPut(path);
                break;
            case PATCH:
                httpRequestBase = new HttpPatch(path);
                break;
            case DELETE:
                httpRequestBase = new HttpDelete(path);
                break;
            default:
                throw new IllegalArgumentException("Unhandled httpMethod " + restInput.getHttpMethod());
        }
        return httpRequestBase;
    }

    private static void setBody(RestInput restInput, HttpEntityEnclosingRequestBase httpRequestBase, String payload) throws UnsupportedEncodingException {
        HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase = httpRequestBase;
        if(restInput.isMultipart()){
            final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            restInput.getBodies().stream().forEach(b -> {
                addPart(builder, b);
            });
            httpEntityEnclosingRequestBase.setEntity(builder.build());
        }else {
            if (null != payload) {
                if (isMultipart(payload)) {
                    setMultipart(payload, httpEntityEnclosingRequestBase);
                }else{
                    httpEntityEnclosingRequestBase.setEntity(new StringEntity(payload));
                }
            }
        }
    }

    private static void setMultipart(String payload, HttpEntityEnclosingRequestBase httpEntityEnclosingRequestBase) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        String lines[] = payload.split("\\n");
        for (String line : lines) {
            String arr[] = line.substring(2).split("=");
            String key = arr[0].trim();
            String value = arr[1].trim();
            if (value.startsWith("@")) {
                File file = new File(value.substring(1));
                builder.addBinaryBody(key, file, ContentType.DEFAULT_BINARY, file.getName());
            } else {
                builder.addTextBody(key, value);
            }
        }
        httpEntityEnclosingRequestBase.setEntity(builder.build());
    }

    private static void addPart(MultipartEntityBuilder builder, Body b){
        switch (b.getType()){
            case FILE:
                File file = ((FileBody)b).getBody();
                builder.addBinaryBody(b.getName(), file, ContentType.DEFAULT_BINARY, file.getName());
                return;
            case INPUT_STREAM: builder.addBinaryBody(b.getName(), ((InputStreamBody)b).getBody()); return;
            case BYTE_ARRAY: builder.addBinaryBody(b.getName(), ((ByteArrayBody)b).getBody()); return;
            case STRING: builder.addTextBody(b.getName(), ((StringBody) b).getBody()); return;

        }
    }

    public static boolean isMultipart(String payload){
        return payload.startsWith("F ");
    }

    public static Map<String, String> convertHeadersListToMap(Header[] headers){
        Map<String, String> map = new HashMap<>();
        for (Header header : headers) {
            map.put(header.getName(), header.getValue());
        }
        return map;
    }

    public static Header[] convertHeaderMapToList(Map<String, String> map){
        Header[] headers = new Header[map.size()];
        int i = 0;
        for (Map.Entry<String, String> entry :  map.entrySet()) {
            headers[i++] = new BasicHeader(entry.getKey(), entry.getValue());
        }
        return headers;
    }

    @Override
    public RestOutput call(RestInput restInput) {
        return callInternal(restInput);
    }

    @Override
    public List<RestOutput> call(List<RestInput> restInputs) {
        return null;
    }

    @Override
    public List<RestOutput> call(BatchIterator<RestInput> restInputBatchIterator) {
        return null;
    }

    @Override
    public Future<RestOutput> callAysnc(RestInput restInput) {
        return null;
    }

    private RestOutput callInternal(RestInput restInput){
        CloseableHttpResponse response = null;
        try{
            response = closeableHttpClient.execute(ApacheHttpClientRestMapper.getHTTPBase(restInput));
            RestOutput.RestOutputBuilder restOutputBuilder = RestOutput.builder()
                    .headers(convertHeadersListToMap(response.getAllHeaders()))
                    .statusCode(response.getStatusLine().getStatusCode())
                    .statusDesc(response.getStatusLine().getReasonPhrase());
            if(!restInput.isCloseResponse()){
                restOutputBuilder.payloadInputStream(response.getEntity().getContent(), response);
            }else{
                restOutputBuilder.payloadAsString(IOUtils.toString(response.getEntity().getContent(), UTF_8));
            }
        return restOutputBuilder.build();

        }catch (Exception e){
            throw new RuntimeException("", e);
        }finally {
            if(restInput.isCloseResponse() && null != response){
                try{
                    response.close();
                }catch (Exception ignore){}
            }
        }
    }
}

