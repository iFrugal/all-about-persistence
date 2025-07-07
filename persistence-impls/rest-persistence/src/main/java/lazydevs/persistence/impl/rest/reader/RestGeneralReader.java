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
import lazydevs.persistence.Auditor;
import lazydevs.persistence.impl.rest.auth.RestAuth;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.persistence.reader.Page;
import lazydevs.persistence.reader.Page.PageRequest;
import lazydevs.persistence.util.ConditionEvaluator;
import lazydevs.persistence.util.Conditional;
import lazydevs.persistence.util.ParseUtils;
import lazydevs.services.basic.exception.RESTException;
import lazydevs.services.basic.filter.RequestContext;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;
import static lazydevs.persistence.impl.rest.reader.RestGeneralReader.ResponseType.LIST_OF_MAP_INSIDE_MAP;
import static lazydevs.persistence.impl.rest.reader.RestGeneralReader.ResponseType.MAP_INSIDE_MAP;

/**
 * @author Abhijeet Rai
 */
@Slf4j
public class RestGeneralReader implements GeneralReader<RestGeneralReader.RestInstruction, Object> {
    private static final String ERROR_MSG = "Exception while calling Rest API. Status Code: %s, Status Description: %s, Body: %s";
    private static final String NOT_YET_IMPLEMENTED = "Not yet implemented";
    @Setter private RestMapper restMapper = new ApacheHttpClientRestMapper();
    @Setter private RestAuth restAuth;
    @Setter private Auditor auditor = new NoOpsAuditor();

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

    @Override
    public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, @NonNull RestInstruction restInstruction, Map<String, Object> params) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size should be greater than 0");
        }
        if(null == restInstruction.getBatchIteratorInitDTO()){
            return new OffsetLimitBatchIterator(batchSize, restInstruction);
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

    private long getPageCount(long totalRecords, int batchSize) {
        long quotient = totalRecords / batchSize;
        long remainder = totalRecords % batchSize;
        return remainder == 0 ? quotient : quotient + 1;
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
        authorize(restInstruction);
        RestOutput restOutput = null;
        try {
            RequestContext.current().put("restInstruction", restInstruction);
            restOutput = getResponse(restMapper, restInstruction.getRequest());
            RequestContext.current().put("restOutput", restOutput);
            if (restOutput.getStatusCode() != HttpStatus.SC_OK) {
                throw new IllegalArgumentException(format(ERROR_MSG, restOutput.getStatusCode(), restOutput.getStatusDesc(), restOutput.getPayloadAsString()));
            }
        }finally {
            if(restInstruction.audit) {
                auditor.audit();
            }
        }
        if(Objects.nonNull(restInstruction.getResponseExtractionLogic()))
            return parseResponse(restOutput, restInstruction.getResponseExtractionLogic(), restInstruction.getExceptionHandlingRules());
        else
            return new ArrayList<>();
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
        log.debug("Response :  ( Status = {}, payload = {}, header = {})", restOutput.getStatusCode(), (restInput.isCloseResponse() ? restOutput.getPayloadAsString() : "payload is big, input-stream"), restOutput.getHeaders());
        return restOutput;
    }

    private List<Map<String, Object>> parseResponse(@NonNull RestOutput restOutput, @NonNull RestOutputExtractionLogic extractionLogic, List<ExceptionHandling> exceptionHandlingRules) {
        if (extractionLogic.getResponseType() == ResponseType.LIST_OF_MAP) {
            return restOutput.getPayloadAsListOfMap(extractionLogic.getSerDe());
        } else {
            Map<String, Object> payloadAsMap = restOutput.getPayloadAsMap(extractionLogic.getSerDe());
            applyExceptionRules(payloadAsMap, exceptionHandlingRules);
            if(extractionLogic.getResponseType() == ResponseType.MAP) {
                return Collections.singletonList(payloadAsMap);
            }else {
                return extractFromComplexObject(payloadAsMap, extractionLogic);
            }
        }
    }

    private void applyExceptionRules(Map<String, Object> payloadAsMap, List<ExceptionHandling> exceptionHandlingRules) {
        if(null == exceptionHandlingRules){
            return;
        }
        for(ExceptionHandling rule : exceptionHandlingRules){
            if(ConditionEvaluator.evaluate(rule.getFailureCondition(), payloadAsMap)){
                log.warn("API failure detected for rule : {}", rule);
                // Extract error details from response
                String message = extractErrorMessage(rule.getErrorResponse(), payloadAsMap);
                String customMessage = extractCustomMessage(rule.getErrorResponse(), payloadAsMap);

                // Throw RESTException with extracted details
                throw new RESTException(message, rule.getErrorResponse().getHttpStatus())
                        .errorCode(rule.getErrorResponse().getErrorCode())
                        .errorDesc(customMessage);
            }
        }
    }

    private String extractErrorMessage(ErrorResponse errorResponse, Map<String, Object> payloadAsMap) {
        // Try primary message source
        String message = Optional.ofNullable(errorResponse.getMessageFrom())
                .map(field -> ParseUtils.getString(payloadAsMap, field))
                .orElse(null);

        // Try fallback message source
        if (message == null) {
            message = Optional.ofNullable(errorResponse.getFallbackMessageFrom())
                    .map(field -> ParseUtils.getString(payloadAsMap, field))
                    .orElse(null);
        }

        // Use static fallback message
        if (message == null) {
            message = errorResponse.getFallbackMessage();
        }

        return Optional.ofNullable(message)
                .map(template -> TemplateEngine.getInstance().generate(template, payloadAsMap))
                .orElse("API request failed");
    }

    /**
     * Extract custom message (for errorDesc) from response using configuration and FreeMarker templating
     */
    private String extractCustomMessage(ErrorResponse errorResponse, Map<String, Object> payloadAsMap) {
        return Optional.ofNullable(errorResponse.getCustomMessage())
                .map(template -> TemplateEngine.getInstance().generate(template, payloadAsMap))
                .orElseGet(() -> extractErrorMessage(errorResponse, payloadAsMap));
    }

    private List<Map<String, Object>> extractFromComplexObject(Map<String, Object> payloadAsMap, RestOutputExtractionLogic extractionLogic){
        if(!Conditional.of(extractionLogic.responseType).in(MAP_INSIDE_MAP, LIST_OF_MAP_INSIDE_MAP)){
            throw new IllegalArgumentException("This method extractFromComplexObject should be called only for "+ Arrays.asList(MAP_INSIDE_MAP, LIST_OF_MAP_INSIDE_MAP));
        }
        String attributeName = Objects.requireNonNull(extractionLogic.getAttributeToExtract());
        Object o = extractFromComplexObject(attributeName, payloadAsMap);
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
        private List<ExceptionHandling> exceptionHandlingRules;
        private GeneralTransformer transformer;
        private RestOutputExtractionLogic responseExtractionLogic;
        private RestCountInstruction countInstruction;
        private InitDTO batchIteratorInitDTO;
        private InitDTO restAuthInitDTO;
        private boolean skipCallOnNullPayload = false;
        private boolean audit = false;
    }

    @Data
    public static class ErrorResponse {
        private String messageFrom;
        private String fallbackMessageFrom;
        private String customMessage;
        private String fallbackMessage;
        private int httpStatus;
        private String errorCode;
        private Map<String, Object> additionalFields;
    }

    @Getter @Setter @ToString
    public static class ExceptionHandling{
        private ConditionEvaluator.ConditionRequest failureCondition;
        private ErrorResponse errorResponse;
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

    private abstract class RestBatchIterator extends BatchIterator<Map<String, Object>>{
        protected final RestInstruction restInstruction;

        public RestBatchIterator(final int batchSize, RestInstruction restInstruction){
            super(batchSize);
            this.restInstruction = restInstruction;
        }
        @Override
        public void close() {}
    }

    private class OffsetLimitBatchIterator extends RestBatchIterator {
        private int offset;
        private int pageNum = 1;
        private boolean hasNextBatch = true;
        public OffsetLimitBatchIterator(final int batchSize, RestInstruction restInstruction){
            super(batchSize, restInstruction);
        }

        @Override
        public boolean hasNext() {
            return this.hasNextBatch;
        }

        @Override
        public List<Map<String, Object>> next() {
            log.info("Batch no.: {}, Batch size: {}", pageNum, batchSize);
            if (!hasNextBatch) {
                throw new NoSuchElementException("Could not find next batch.");
            }
            offset = (pageNum - 1) * batchSize;
            List<Map<String, Object>> data = fetchData(copyRestInstruction(offset, batchSize));
            hasNextBatch = !(data.size() < batchSize);
            log.info("Records fetched: {}" , data.size());
            pageNum++;
            return data;
        }

        private RestInstruction copyRestInstruction(int offset, int limit) {
            RestInstruction copy = new RestInstruction();
            copy.setResponseExtractionLogic(restInstruction.getResponseExtractionLogic());
            copy.setCountInstruction(restInstruction.getCountInstruction());
            RestInput restInput = new RestInput(restInstruction.getRequest());
            Map<String, Object> map = new HashMap<>();
            map.put("offset", offset);
            map.put("limit", limit);
            restInput.setQueryParams(TemplateEngine.getInstance().generate(restInput.getQueryParams(), map));
            copy.setRequest(restInput);
            copy.setRestAuthInitDTO(restInstruction.getRestAuthInitDTO());
            return copy;
        }
    }

    private class PageBasedBatchIterator extends RestBatchIterator {
        private long totalPages;
        private boolean hasNextBatch = true;
        private int pageNum = 1;

        public PageBasedBatchIterator(final int batchSize, RestInstruction restInstruction){
            super(batchSize, restInstruction);
            this.totalPages = getPageCount(count(copyRestInstruction(pageNum, 1)), batchSize);
        }

        @Override
        public boolean hasNext() {
            return this.hasNextBatch;
        }

        @Override
        public List<Map<String, Object>> next() {
            log.info("Batch no.: {}, Batch size: {}", pageNum, batchSize);
            if (!hasNextBatch) {
                throw new NoSuchElementException("Could not find next batch.");
            }
            List<Map<String, Object>> data = fetchData(copyRestInstruction(pageNum, batchSize));
            hasNextBatch = (pageNum != totalPages);
            log.info("Records fetched: {}" , data.size());
            pageNum++;
            return data;
        }

        private RestInstruction copyRestInstruction(int pageNum, int pageSize) {
            RestInstruction copy = new RestInstruction();
            copy.setResponseExtractionLogic(restInstruction.getResponseExtractionLogic());
            copy.setCountInstruction(restInstruction.getCountInstruction());
            RestInput restInput = new RestInput(restInstruction.getRequest());
            Map<String, Object> map = new HashMap<>();
            map.put("pageNum", pageNum);
            map.put("pageSize", pageSize);
            restInput.setQueryParams(TemplateEngine.getInstance().generate(restInput.getQueryParams(), map));
            copy.setRequest(restInput);
            return copy;
        }
    }

    public static class NoOpsAuditor implements Auditor {
        @Override
        public void audit() {

        }
    }
}