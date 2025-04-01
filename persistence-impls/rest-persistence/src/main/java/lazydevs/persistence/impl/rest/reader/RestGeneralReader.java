package lazydevs.persistence.impl.rest.reader;

import lazydevs.mapper.rest.RestInput;
import lazydevs.mapper.rest.RestMapper;
import lazydevs.mapper.rest.RestOutput;
import lazydevs.mapper.rest.impl.ApacheHttpClientRestMapper;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.mapper.utils.reflection.ReflectionUtils;
import lazydevs.persistence.impl.rest.auth.RestAuth;
git import lazydevs.persistence.impl.rest.reader.batchiterator.OffsetLimitBatchIterator;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.persistence.reader.Page;
import lazydevs.persistence.reader.Page.PageRequest;
import lazydevs.persistence.util.Conditional;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;
import static lazydevs.persistence.impl.rest.reader.RestGeneralReader.ResponseType.LIST_OF_MAP_INSIDE_MAP;
import static lazydevs.persistence.impl.rest.reader.RestGeneralReader.ResponseType.MAP_INSIDE_MAP;
import static org.apache.http.HttpStatus.SC_OK;

/**
 * @author Abhijeet Rai
 */
@Slf4j
public class RestGeneralReader implements GeneralReader<RestGeneralReader.RestInstruction, Object> {
    private static final String ERROR_MSG = "Exception while calling Rest API. Status Code: %s, Status Description: %s, Body: %s";
    private static final String NOT_YET_IMPLEMENTED = "Not yet implemented";
    @Setter private RestMapper restMapper = new ApacheHttpClientRestMapper();
    @Setter private RestAuth restAuth;

    private void authorize(RestInstruction restInstruction){
        RestAuth restAuth;
        if(null != restInstruction.getRestAuthInitDTO()){
            restAuth = ReflectionUtils.getInterfaceReference(restInstruction.getRestAuthInitDTO(), RestAuth.class);
        }else{
            restAuth = this.restAuth;
        }
        if(null != restAuth){
            restAuth.authorize(restInstruction);
        }
    }

    @Override
    public Map<String, Object> findOne(@NonNull RestInstruction restInstruction, Map<String, Object> params) {
        List<Map<String,Object>> data = fetchData(restInstruction);
        if(Objects.nonNull(data) && data.size() > 0)
            return data.get(0);
        else
            return null;
    }

    @Override
    public List<Map<String, Object>> findAll(@NonNull RestInstruction restInstruction, Map<String, Object> params) {
        return fetchData(restInstruction);
    }

    Function<RestInstruction, List<Map<String, Object>>> fetchDataFn = restInstruction -> fetchData(restInstruction);

    Function<RestInstruction, Long> countFn = restInstruction -> count(restInstruction);

    @Override
    public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, @NonNull RestInstruction restInstruction, Map<String, Object> params) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size should be greater than 0");
        }
        if(null == restInstruction.getBatchIteratorInitDTO()){
            return new OffsetLimitBatchIterator(batchSize, restInstruction, fetchDataFn);
        }else{
            return getInterfaceReference(restInstruction.getBatchIteratorInitDTO(), RestBatchIterator.class);
        }
    }

    @Override
    public Page<Map<String, Object>> findPage(@NonNull PageRequest pageRequest, @NonNull RestInstruction restInstruction, Map<String, Object> params) {
        throw new UnsupportedOperationException(NOT_YET_IMPLEMENTED);
    }

    @Override
    public long count(@NonNull RestInstruction restInstruction, Map<String, Object> params) {
        RestCountInstruction countInstruction = Objects.requireNonNull(restInstruction.getCountInstruction(), "countInstruction can't be null for count() method");
        RestInput restInput = null == countInstruction.getRequest() ? restInstruction.getRequest() : countInstruction.getRequest();
        RestOutput restOutput = getResponse(restMapper, restInput);

        if(countInstruction.isExtractFromHeader()){
            String headerVal = null;
            if(restOutput.getHeaders().containsKey(countInstruction.getAttributeToExtract())) {
                headerVal = restOutput.getHeader(countInstruction.getAttributeToExtract());
            }else{// try with lowercase
                headerVal = restOutput.getHeaders().entrySet().stream().collect(Collectors.toMap(entry -> entry.getKey().toLowerCase(), Map.Entry::getValue)).get(countInstruction.getAttributeToExtract().toLowerCase());
            }
            return Long.parseLong(headerVal);
        }else{
            Object o = extractFromComplexObject(countInstruction.getAttributeToExtract(), restOutput.getJsonPayloadAsMap());
            return Long.parseLong(String.valueOf(o));
        }
    }


    @Override
    public List<Map<String, Object>> distinct(@NonNull RestInstruction restInput, Map<String, Object> params) {
        throw new UnsupportedOperationException(NOT_YET_IMPLEMENTED);
    }

    @Override
    public Class<RestInstruction> getQueryType() {
        return RestInstruction.class;
    }

    private List<Map<String, Object>> fetchData(@NonNull RestInstruction restInstruction) {
        RestOutput restOutput = authorizeAndCall(restInstruction);
        if(Objects.nonNull(restInstruction.getResponseExtractionLogic()))
            return parseResponse(restOutput, restInstruction.getResponseExtractionLogic());
        else
            return new ArrayList<>();
    }

    private List<Map<String, Object>> fetchDataForBatchIterator(@NonNull RestInstruction restInstruction) {
        RestOutput restOutput = authorizeAndCall(restInstruction);
        if(Objects.nonNull(restInstruction.getResponseExtractionLogic()))
            return parseResponse(restOutput, restInstruction.getResponseExtractionLogic());
        else
            return new ArrayList<>();
    }

    private RestOutput authorizeAndCall(RestInstruction restInstruction){
        authorize(restInstruction);
        return getResponse(restMapper, restInstruction.getRequest());
    }

  private RestOutput getResponse(@NonNull RestMapper restMapper, @NonNull RestInput restInput) {
        Map<String, String> headersToPrintInLog = new HashMap<>(restInput.getHeaders());
        if(null != restInput.getPayloadObject()) {
            restInput.setPayload(SerDe.JSON.serialize(restInput.getPayloadObject()));
        }
        Optional<String> authHeader = headersToPrintInLog.keySet().stream().filter(key-> "authorization".equals(key.trim().toLowerCase())).findFirst();
        if(authHeader.isPresent()){
            headersToPrintInLog.put(authHeader.get(), "XXXXXXXXXXXX");
        }
        log.info("Calling API, Request : ( url = {}, params = {}, headers = {}, payload = {})",  restInput.getUrl(), restInput.getQueryParams(), headersToPrintInLog, restInput.getPayload());
        RestOutput restOutput = restMapper.call(restInput);
        log.info("Response :  ( Status = {}, payload = {}, header = {})", restOutput.getStatusCode(), (restInput.isCloseResponse() ? restOutput.getPayloadAsString() : "payload is big, input-stream"), restOutput.getHeaders());
        if (restOutput.getStatusCode() != SC_OK) {
            throw new IllegalArgumentException(format(ERROR_MSG, restOutput.getStatusCode(), restOutput.getStatusDesc(), restOutput.getPayloadAsString()));
        }

        return restOutput;
    }

    private List<Map<String, Object>> parseResponse(@NonNull RestOutput restOutput, @NonNull RestOutputExtractionLogic extractionLogic) {
        switch (extractionLogic.getResponseType()) {
            case MAP:
                return Collections.singletonList(restOutput.getPayloadAsMap(extractionLogic.getSerDe()));
            case LIST_OF_MAP:
                return restOutput.getPayloadAsListOfMap(extractionLogic.getSerDe());
            case MAP_INSIDE_MAP:
            case LIST_OF_MAP_INSIDE_MAP:
                return extractFromComplexObject(restOutput, extractionLogic);
            default:
                throw new IllegalArgumentException("Not Supported extractionLogic.getResponseType() =  " + extractionLogic.getResponseType());
        }
    }

    private List<Map<String, Object>> extractFromComplexObject(RestOutput restOutput, RestOutputExtractionLogic extractionLogic){
        if(!Conditional.of(extractionLogic.responseType).in(MAP_INSIDE_MAP, LIST_OF_MAP_INSIDE_MAP)){
            throw new IllegalArgumentException("This method extractFromComplexObject should be called only for "+ Arrays.asList(MAP_INSIDE_MAP, LIST_OF_MAP_INSIDE_MAP));
        }
        String attributeName = Objects.requireNonNull(extractionLogic.getAttributeToExtract());
        Map<String, Object> map = restOutput.getPayloadAsMap(extractionLogic.getSerDe());
        Object o = extractFromComplexObject(attributeName, map);
        return MAP_INSIDE_MAP.equals(extractionLogic.responseType) ? Arrays.asList((Map<String, Object>)o) : (List<Map<String, Object>>) o;
    }

    private Object extractFromComplexObject(@NonNull String attributeName, Map<String, Object> map){
        String[] tokens = attributeName.split("\\.");
        for(int i = 0; i < (tokens.length - 1); i++){
            map = (Map<String, Object>) map.get(tokens[i]);
        }
        return map.get(tokens[tokens.length - 1]);
    }

    @Getter @Setter @ToString
    public static class RestInstruction{
        private RestInput request;
        private GeneralTransformer transformer;
        private RestOutputExtractionLogic responseExtractionLogic;
        private RestCountInstruction countInstruction;
        private InitDTO batchIteratorInitDTO;
        private InitDTO restAuthInitDTO;
        private boolean skipCallOnNullPayload = false;
    }

    @Getter @Setter @ToString
    public static class RestCountInstruction{
        private RestInput request;
        private boolean extractFromHeader = true;
        private String attributeToExtract;
    }

    @Getter @Setter @ToString
    private static class RestOutputExtractionLogic{
        @NonNull private ResponseType responseType;
        private String attributeToExtract;
        @NonNull private SerDe serDe = SerDe.JSON;
    }


    public enum ResponseType{
        MAP, LIST_OF_MAP, MAP_INSIDE_MAP, LIST_OF_MAP_INSIDE_MAP;
    }





}