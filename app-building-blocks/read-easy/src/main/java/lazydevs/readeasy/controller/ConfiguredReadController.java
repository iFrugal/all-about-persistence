package lazydevs.readeasy.controller;


import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.mapper.utils.reflection.ReflectionUtils;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.GeneralTransformer;
import lazydevs.persistence.reader.Page;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.config.ReadEasyConfig.Query;
import lazydevs.readeasy.config.ReadEasyConfig.QueryWithDynaBeans;
import lazydevs.services.basic.exception.RESTException;
import lazydevs.services.basic.exception.ValidationException;
import lazydevs.services.basic.validation.ParamValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static lazydevs.mapper.utils.SerDe.JSON;
import static lazydevs.mapper.utils.engine.ScriptEngines.JAVASCRIPT;
import static lazydevs.mapper.utils.file.FileUtils.readInputStreamAsString;
import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;
import static lazydevs.readeasy.config.ReadEasyConfig.Operation.EXPORT;
import static lazydevs.readeasy.config.ReadEasyConfig.Operation.ONE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static org.springframework.util.StringUtils.hasText;


/**
 * @author Abhijeet Rai
 */



@RestController
@RequestMapping("/read")
@DependsOn("dynaBeansGenerator")
@Slf4j
public class ConfiguredReadController {

    private Map<String, Query> queries;
    @Autowired DynaBeansAutoConfiguration dynaBeansAutoConfiguration;
    @Autowired private ResourceLoader resourceLoader;
    @Autowired(required = false) @Qualifier("readEasyGeneralReaderMap") @Getter private Map<String, GeneralReader> readEasyGeneralReaderMap;
    @Autowired(required = false) private ReadEasyConfig readEasyConfig;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private Environment environment;
    private Supplier<?> requestContextSupplier;
    private Supplier<?> globalContextSupplier;
    @Autowired private ParamValidator paramValidator;
    @Value("${readeasy.banner.path:classpath:read-easy-banner.txt}")
    private String bannerPath;

    @PostConstruct
    public void init() throws IOException {
        log.info(readInputStreamAsString(applicationContext.getResource(bannerPath).getInputStream()));
        this.readEasyGeneralReaderMap = getGeneralReader(readEasyGeneralReaderMap, readEasyConfig, applicationContext);
        queries = new HashMap<>();
        readEasyConfig.getQueryFiles().forEach((namespace, filePaths)-> {
            filePaths.stream().forEach(filePath -> {
                log.info("Registering ReadEasy Config from file = {} with namespace = {}", filePath, namespace);
                try {
                    register(namespace, resourceLoader.getResource(filePath).getInputStream());
                } catch (IOException e) {
                    throw new IllegalStateException("Unable to Start read-easy engine. Error while registering file ="+filePath, e);
                }
            });
        });
        if(null != readEasyConfig.getRequestContextSupplierInit()) {
            this.requestContextSupplier = ReflectionUtils.getInterfaceReference(readEasyConfig.getRequestContextSupplierInit(), Supplier.class, (s) -> applicationContext.getBean(s), environment::getProperty);
        }
        if(null != readEasyConfig.getGlobalContextSupplierInit()) {
            this.globalContextSupplier = ReflectionUtils.getInterfaceReference(readEasyConfig.getGlobalContextSupplierInit(), Supplier.class, (s) -> applicationContext.getBean(s), environment::getProperty);
        }
    }

    private Map<String, GeneralReader> getGeneralReader(Map<String, GeneralReader> readEasyGeneralReaderMap, ReadEasyConfig readEasyConfig, ApplicationContext applicationContext){
        Map<String, GeneralReader> finalReadEasyGeneralReaderMap = new HashMap<>();

        if(null == readEasyGeneralReaderMap){//bean not provided
            if(null == readEasyConfig.getGeneralReaders()){
                readEasyConfig.setGeneralReaders(new HashMap<>());
            }
            if(null != readEasyConfig.getGeneralReaderInit()){
                readEasyConfig.getGeneralReaders().put("default", readEasyConfig.getGeneralReaderInit());
            }
            if(readEasyConfig.getGeneralReaders().isEmpty()){
                throw new IllegalStateException("readEasyGeneralReaderMap is not provided either as @Autowired bean, nor it is provided in property 'readeasy.generalReaders'");
            }
            readEasyConfig.getGeneralReaders().forEach((readerId, readerInit) -> {
                GeneralReader<?,?> generalReader = getInterfaceReference(readerInit, GeneralReader.class, applicationContext::getBean, environment::getProperty);
                finalReadEasyGeneralReaderMap.put(readerId, generalReader);
            });
        }
        return finalReadEasyGeneralReaderMap;
    }

    private void register(String namespace, InputStream inputStream){
        QueryWithDynaBeans queryWithDynaBeans = SerDe.YAML.deserialize(readInputStreamAsString(inputStream), QueryWithDynaBeans.class);
        dynaBeansAutoConfiguration.initializeAndInject(namespace, queryWithDynaBeans.getDynaBeans());
        queryWithDynaBeans.getQueries().forEach((queryId, query) -> queries.put(namespace+"."+queryId, query));
    }

    private Map<String, Object> decorateDatapoints(Map<String, Object> data, Map<String, Object> params){
        Map<String, Object> modifiedData =  new HashMap<>(data);
        modifiedData.put("request", null == requestContextSupplier ? null : requestContextSupplier.get());
        modifiedData.put("global", null == globalContextSupplier ? null : globalContextSupplier.get());
        modifiedData.put("params", params);
        return modifiedData;
    }

    private Object getQuery(String queryId, Map<String,Object> params, String orderby, String orderdir){
        Query query = getRegisteredQuery(queryId);
        params = new HashMap<>(params);
        paramValidator.validate(queryId, query.getParams(), params);
        String sort = "{}";

        if(hasText(orderby)){
            sort = format("{\"%s\" : %s}", orderby, ("desc".equals(orderdir) ? -1 : 1));
            params.put("sort", sort);
        } else if (params.containsKey("sort")) {
            Object sortObj = params.get("sort");

            if (sortObj instanceof Map) {
                sort = JSON.serialize(sortObj);
                params.put("sort", sort);
            }
        } else {
            params.put("sort", sort);
        }

        String transformedQuery = TemplateEngine.getInstance().generate(query.getRaw(), decorateDatapoints(params, null));
        return query.getRawFormat().deserialize(transformedQuery, getGeneralReader(query.getReaderId()).getQueryType());
    }

    private Query getRegisteredQuery(String queryId) {
        Query query = queries.get(queryId);
        if(null == query){
            throw new ValidationException("No query found registered for queryId = "+queryId);
        }
        return query;
    }



    private List<Map<String, Object>> convert(List<Map<String, Object>> list, GeneralTransformer transformer, Map<String, Object> params){
        if(null == transformer){
            return list;
        }
        return list.stream().map(row ->  convert(row, transformer, params)).collect(toList());
    }

    private Map<String, Object> convert(Map<String, Object> row, GeneralTransformer transformer, Map<String, Object> params){
        if(null == transformer){
            return row;
        }
        Map<String, Object> modifiedRow = decorateDatapoints(row, params);
        Map<String, Object> convert = transformer.convert(modifiedRow);
        convert.remove("request");
        convert.remove("global");
        convert.remove("params");
        return convert;
    }

    private GeneralReader getGeneralReader(String generalReaderId){
        if(!readEasyGeneralReaderMap.containsKey(generalReaderId)){
            throw new IllegalStateException("No reader found register against readerId = "+ generalReaderId);
        }
        return readEasyGeneralReaderMap.get(generalReaderId);
    }

    @ConditionalOnProperty("readeasy.admin.enabled")
    @PostMapping(value = "/register", consumes = MULTIPART_FORM_DATA_VALUE)
    public void register(@RequestPart(required = false) String namespace,
                         @RequestPart MultipartFile registrationFile) throws IOException {
        register(namespace, registrationFile.getInputStream());
    }

    @PostMapping("/one")
    public ResponseEntity<Object> findOne(@RequestParam("queryId") String queryId, @RequestBody Map<String,Object> params){
        Query query = getRegisteredQuery(queryId);
        Map<String, Object> response = (Map<String, Object>) getGeneralReader(query.getReaderId()).findOne(getQuery(queryId, params, null, null));
        if (null == response) {
            Map<String, Object> instruction = query.getOperationInstruction().getOrDefault(ONE, readEasyConfig.getOperationInstruction().getOrDefault(ONE, new HashMap<>()));
            Integer statusCodeWhenNoRecordsFound = (Integer) instruction.getOrDefault("statusCodeWhenNoRecordsFound", HttpStatus.NOT_FOUND.value());
            return ResponseEntity.status(HttpStatus.valueOf(statusCodeWhenNoRecordsFound)).body("Resource not found with id=" + queryId);
        }
        return ResponseEntity.ok(convert(response, query.getRowTransformer(), params));
    }

    public Map<String, Object> findOneAsMap(@RequestParam("queryId") String queryId, @RequestBody Map<String,Object> params){
        Query query = getRegisteredQuery(queryId);
        Map<String, Object> response = (Map<String, Object>) getGeneralReader(query.getReaderId()).findOne(getQuery(queryId, params, null, null));
        if (null != response) {
            response = convert(response, query.getRowTransformer(), params);
        }
        return response;
    }


    @PostMapping("/count")
    public long count(@RequestParam("queryId") String queryId, @RequestBody Map<String,Object> params){
        Query query = getRegisteredQuery(queryId);
        return getGeneralReader(query.getReaderId()).count(getQuery(queryId, params, null, null));
    }

    @PostMapping("/list")
    public List<Map<String, Object>> findAll(@RequestParam("queryId") String queryId, @RequestBody Map<String,Object> params,
                                             @RequestParam(value = "orderby", required = false) String orderby,
                                             @RequestParam(value = "orderdir", required = false) String orderdir,
                           @RequestParam(value = "cache", required = false, defaultValue = "true") boolean cache){
        Query query = getRegisteredQuery(queryId);
        List<Map<String, Object>> list = null;
        if(cache && null != query.getCacheFetchInstruction()){
            paramValidator.validate(queryId, query.getParams(), params);
            List<Object> args = query.getCacheFetchInstruction().getArgs().stream().map(arg -> params.getOrDefault(arg, null)).collect(toList());
            list = (List<Map<String, Object>>) JAVASCRIPT.invokeFunction(query.getCacheFetchInstruction().getJsFunctionName(), args.toArray(new Object[args.size()]));
        }else{
            list = (List<Map<String, Object>>) getGeneralReader(query.getReaderId()).findAll(getQuery(queryId, params, orderby, orderdir));
        }
        return convert(list,  query.getRowTransformer(), params);
    }

    @PostMapping("/page")
    public Page<List<Map<String, Object>>> findPage(@RequestParam("queryId") String queryId, @RequestBody Map<String,Object> params,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "10") int pageSize,
                                              @RequestParam(value = "orderby", required = false) String orderby,
                                              @RequestParam(value = "orderdir", required = false) String orderdir){
        Query query = getRegisteredQuery(queryId);
        Page page = getGeneralReader(query.getReaderId()).findPage(Page.PageRequest.builder().pageNum(pageNum).pageSize(pageSize).build(),
                getQuery(queryId, params, orderby, orderdir));
        page.setData(convert(page.getData(), query.getRowTransformer(), params));
        return page;
    }


    @PostMapping("/export")
    public void export(@RequestParam("queryId") String queryId, @RequestBody Map<String,Object> params, @RequestParam(value = "orderby", required = false) String orderby,
                       @RequestParam(value = "orderdir", required = false) String orderdir,
                       @RequestParam(required = false) Long countFromClient,
                       HttpServletResponse response) throws IOException {

        export(response, countFromClient, queryId, params, orderby, orderdir);
    }

    private void export(HttpServletResponse response, Long countFromClient, String queryId, Map<String,Object> params, String orderby, String orderdir) throws IOException{
        Object persistenceQuery = getQuery(queryId, params, orderby, orderdir);
        Query configuredQuery =  getRegisteredQuery(queryId);
        Map<String, Object> instruction = configuredQuery.getOperationInstruction().getOrDefault(EXPORT, readEasyConfig.getOperationInstruction().getOrDefault(EXPORT, new HashMap<>()));
        if(null == instruction){
            throw new IllegalStateException("export instruction is not provided either at query level or at app level");
        }
        final GeneralReader generalReader = getGeneralReader(configuredQuery.getReaderId());
        if(null != instruction && (Boolean)instruction.getOrDefault("countCheckRequired", false) && null != instruction.get("maxCountToExport")){
            long countInDb = null == countFromClient ? generalReader.count(persistenceQuery) : countFromClient;
            Long maxCountToExport = (Long) instruction.get("maxCountToExport");
            if(countInDb > maxCountToExport){
                if((Boolean)instruction.getOrDefault("rejectRequestIfCountGreaterThanMaxCountToExport", false)){
                    throw new ValidationException(String.format("There is an upper limit set on the server to not to export more than %s records from DB", maxCountToExport));
                }
            }
        }
        final Map<String, Object> map = new HashMap<>();
        final String exportFileName = TemplateEngine.getInstance().generate((String) instruction.getOrDefault("exportFileNameTemplate", "export.csv"), decorateDatapoints(map, params));
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportFileName + "\"");
        response.setHeader(HttpHeaders.CONTENT_TYPE, "application/octet-stream;charset=UTF-8");
        Writer writer = response.getWriter();

        try(BatchIterator<Map<String, Object>> batchIterator = generalReader.findAllInBatch((Integer) instruction.getOrDefault("readBatchSize", 1_000), persistenceQuery)) {
            while (batchIterator.hasNext()) {
                String exportTemplate = (String) instruction.get("exportTemplate");
                if (null == exportTemplate) {
                    writer.write(batchIterator.next().stream().map(
                            row -> row.entrySet().stream().map(e -> Objects.toString(e.getValue())).collect(Collectors.joining(","))
                    ).collect(Collectors.joining("\n")) + "\n");
                } else {
                    map.put("list", batchIterator.next());
                    TemplateEngine.getInstance().generate(writer, exportTemplate, decorateDatapoints(map, params));
                }
            }
        }
    }
}
