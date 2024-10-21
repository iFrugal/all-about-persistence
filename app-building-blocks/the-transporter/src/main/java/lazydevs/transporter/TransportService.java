package lazydevs.transporter;

import freemarker.template.DefaultListAdapter;
import freemarker.template.DefaultMapAdapter;
import freemarker.template.TemplateMethodModelEx;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.ScriptEngines;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.writer.general.GeneralAppender;
import lazydevs.persistence.writer.general.GeneralUpdater;
import lazydevs.scheduleit.ScheduleItService;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration.BeanSupplier;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig;
import lazydevs.transporter.config.Flow;
import lazydevs.transporter.config.Pipeline;
import lazydevs.transporter.config.PipelineContext;
import lazydevs.transporter.config.TransporterConfig;
import lazydevs.transporter.enums.Actions;
import lazydevs.transporter.enums.Modes;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static lazydevs.mapper.utils.file.FileUtils.readInputStreamAsString;
import static lazydevs.mapper.utils.reflection.ReflectionUtils.getInterfaceReference;
import static lazydevs.transporter.TransporterController.PIPELINE_RUN_ID;
import static lazydevs.transporter.config.PipelineContext.getCurrentContext;

/**
 * @author Abhijeet Rai
 */
@Service @Slf4j
@DependsOn("dynaBeansGenerator")
public class TransportService {
    private static final Logger FAILED_RECORD_LOGGER = org.slf4j.LoggerFactory.getLogger("FAILED_RECORD_LOGGER");
    public static final String PIPELINE_KEY = "pipelineKey";
    public static final String BATCH_INDEX = "batchIndex";
    @Autowired private ResourceLoader resourceLoader;
    @Autowired private TransporterConfig transporterConfig;
    @Autowired private ApplicationContext applicationContext;
    @Autowired private Environment environment;
    @Autowired DynaBeansAutoConfiguration dynaBeansAutoConfiguration;
    @Autowired private ScheduleItService scheduleItService;

    @Getter private final Map<String, Pipeline> pipelines = new HashMap<>();
    @Value("${the-transporter.banner.path:classpath:the-transporter-banner.txt}")
    private String bannerPath;

    @PostConstruct
    public void init() throws IOException {
        log.info(readInputStreamAsString(applicationContext.getResource(bannerPath).getInputStream()));
        log.info("transporterConfig.getPipelines() = {}", transporterConfig.getPipelines());
        transporterConfig.getPipelines().forEach(this::registerPipeline);
    }

    public void registerPipeline(String pipelineKey, String pipelinePath) {
        Pipeline pipeline;
        log.info("Registering the pipeline '{}' from filePath = {}", pipelineKey, pipelinePath);
        try {
            pipeline = SerDe.YAML.deserialize(readInputStreamAsString(resourceLoader.getResource(pipelinePath).getInputStream()), Pipeline.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error while registering the pipeline '%s' from filePath = %s ", pipelineKey, pipelinePath), e);
        }
        dynaBeansAutoConfiguration.initializeAndInject(pipelineKey, pipeline.getDynaBeans());
        pipelines.put(pipelineKey, pipeline);
       if(pipeline.isScheduleItEnabled()) {
           LockProvider lockProvider = null;
           try {
               lockProvider = getBean("scheduleItLockProvider", LockProvider.class);
           }catch (BeansException e){
               log.warn("No LockProvider bean is provided", e);
           }
           for (Pipeline.Schedule schedule : pipeline.getSchedules()) {
               scheduleItService.addSchedule(lockProvider, schedule.getCronAttributes(), schedule.getScheduleName(), () -> {
                   transport(pipelineKey, schedule.getParamList());
               }, schedule.getLockAttributes());
           }
       }
    }

    private void execute(Pipeline.Instruction instruction, String pipelineKey){
        if(null == instruction){
            return;
        }
        if(null != instruction.getInlineJavascript()){
            ScriptEngines.JAVASCRIPT.loadScript(TemplateEngine.getInstance().generate(instruction.getInlineJavascript(), PipelineContext.getCurrentContext()));
        }
        if(null != instruction.getRunnableInit()){
            Runnable runnable = getInterfaceReference(instruction.getRunnableInit(), Runnable.class, (beanName)-> getBean(beanName), environment::getProperty);
            runnable.run();
        }
        if(null != instruction.getScriptInstruction()){
            DynaBeansConfig.ScriptInstruction script = instruction.getScriptInstruction();
            script.getEngine().invokeFunction(script.getFunctionName());
        }
    }
    // Copied from DynaBeanAutoconfiguration STARTS

    private Object getInstruction(String instructionStr, Class<?> instructionType, SerDe serDe){
        Object instruction = instructionStr;
        if(null != instruction) {
            instruction = TemplateEngine.getInstance().generate(instructionStr, getCurrentContext());
            instruction = String.class.equals(instructionType)
                    ? instruction
                    : serDe.deserialize(instruction.toString(), instructionType);
        }
        return instruction;
    }

    private Object getInstruction(String instructionStr, Class<?> instructionType){
        return getInstruction(instructionStr, instructionType, SerDe.JSON);
    }

    @Getter@Setter
    static class WriterAttributes
    {
        private Object writer;
        private Object writeInstruction;
        boolean isAppenderOnly;
        Actions writeAction;
        boolean enabled = true;
        private Flow.Writer onSuccess;
        private Flow.Writer onFailure;
        private String id = "";
        private String desc = "";
    }

    public void transport(String pipelineRunId, String pipelineKey, List<Map<String, Object>> paramsList){
        MDC.put(PIPELINE_RUN_ID, pipelineRunId);
        MDC.put(PIPELINE_KEY, pipelineKey);
        Pipeline pipeline = pipelines.get(pipelineKey);
        if((null == paramsList || paramsList.isEmpty()) && null != pipeline.getParamsListProviderScript()){
            paramsList = (List<Map<String, Object>>) dynaBeansAutoConfiguration.callScript(pipeline.getParamsListProviderScript(), pipelineKey);
        }
        if(null != paramsList) {
            paramsList.forEach(params -> {
                getCurrentContext().clear();
                PipelineContext.getCurrentContext().set(PIPELINE_RUN_ID, pipelineRunId);
                MDC.put("params", SerDe.JSON.serialize(params));
                if (null != params) {
                    getCurrentContext().putAll(params);
                }
                transport(pipelineKey);
            });
        }
    }

    public void transport(String pipelineKey, List<Map<String, Object>> paramsList){
        getCurrentContext().clear();
        String pipelineRunId = UUID.randomUUID().toString();
        transport(pipelineRunId, pipelineKey, paramsList);
    }


    private void transport(String pipelineKey){
        Pipeline pipeline = pipelines.get(pipelineKey);
        getCurrentContext().set(PIPELINE_KEY, pipelineKey);
        Objects.requireNonNull(pipeline, "Pipeline not found for pipelineKey = " + pipelineKey);
        execute(pipeline.getPre(), pipelineKey);
        pipeline.getFlows().forEach(flow -> executeFlow(pipelineKey, flow));
        execute(pipeline.getPost(), pipelineKey);
    }

    private String generateFromContext(String template){
        return TemplateEngine.getInstance().generate(template, PipelineContext.getCurrentContext());
    }

    public void executeFlow(String pipelineKey, Flow flow) {
        log.info("****** [FLOW] ******  Executing the flow with id = '{}', desc = '{}'", generateFromContext(flow.getId()), generateFromContext(flow.getDesc()));
        try {
            if(flow.getPipelineTriggerInstruction() != null){
                triggerAnotherPipelineInSameContext(flow.getPipelineTriggerInstruction().getPipelineKey(), flow.getPipelineTriggerInstruction().getParams());
            }else {
                GeneralReader reader = getBean(flow.getReader().getBeanName(), GeneralReader.class);
                Object readInstruction = getInstruction(flow.getReader().getReadInstruction(), reader.getQueryType(), flow.getReader().getInstructionSerDe());
                List<WriterAttributes> writerAttributesList = new ArrayList<>();

                if (flow.getWriters() != null && !flow.getWriters().isEmpty()) {
                    flow.getWriters().forEach(writerObj ->
                            writerAttributesList.add(getWriterAttributes(pipelineKey, writerObj)));
                }
                log.info("****** [Reader] ****** flowId = '{}',  Executing the reader with id = '{}', desc = '{}'", generateFromContext(flow.getId()), generateFromContext(flow.getReader().getId()), generateFromContext(flow.getReader().getDesc()));
                writeAccordingToMode(flow, reader, readInstruction, writerAttributesList);
            }
        }catch (Exception e){
            log.error("Error while executing pipeline = {}, flow = {}", pipelineKey, SerDe.JSON.serialize(flow), e);
            throw new RuntimeException(e);
        }
    }

    private void triggerAnotherPipelineInSameContext(String pipelineKey, List<Map<String, Object>> paramsList){
        Pipeline pipeline = pipelines.get(pipelineKey);
        Objects.requireNonNull(pipeline, "Pipeline not found for pipelineKey = " + pipelineKey);
        if((null == paramsList || paramsList.isEmpty()) ){
            Map<String, Object> map = new HashMap<>();
            map.put("dummyKey", "dummyVal");
            paramsList = Arrays.asList(map);
        }
        paramsList.forEach(params -> {
            if (null != params) {
                getCurrentContext().putAll(params);
            }
            execute(pipeline.getPre(), pipelineKey);
            pipeline.getFlows().forEach(flow -> executeFlow(pipelineKey, flow));
            execute(pipeline.getPost(), pipelineKey);
        });

    }

    private WriterAttributes getWriterAttributes(Flow.Writer writerObj) {
        return getWriterAttributes((String)PipelineContext.getCurrentContext().get(PIPELINE_KEY), writerObj);
    }

    private WriterAttributes getWriterAttributes(String pipelineKey, Flow.Writer writerObj) {
        WriterAttributes writerAttributes = new WriterAttributes();
        String writerBeanName = "consoleWriter";
        if (null != writerObj.getBeanName()) {
            writerBeanName = writerObj.getBeanName();
        }
        Object writer =  getBean(writerBeanName);
        writerAttributes.setWriter(writer);

        boolean isAppenderOnly = false;
        if (writer instanceof GeneralUpdater) {

        } else if (writer instanceof GeneralAppender) {
            isAppenderOnly = true;
        } else {
            throw new IllegalArgumentException(String.format("Bean with beanName = '%s' is neither instance of GeneralUpdater nor of GeneralAppender.", writerBeanName));
        }
        writerAttributes.setAppenderOnly(isAppenderOnly);
        GeneralAppender writerAsAppender = (GeneralAppender) writer;
        Object writeInstruction = getInstruction(writerObj.getWriteInstruction(), writerAsAppender.getWriteInstructionType(), writerObj.getInstructionSerDe());
        writerAttributes.setWriteInstruction(writeInstruction);
        writerAttributes.setEnabled(writerObj.isEnabled());
        writerAttributes.setWriteAction(writerObj.getAction() != null ? writerObj.getAction() : Actions.CREATE);
        writerAttributes.setOnSuccess(writerObj.getOnSuccess());
        writerAttributes.setOnFailure(writerObj.getOnFailure());
        writerAttributes.setId(writerObj.getId());
        writerAttributes.setDesc(writerObj.getDesc());
        return writerAttributes;
    }

    private void writeAccordingToMode(Flow flow, GeneralReader reader, Object readInstruction, List<WriterAttributes> writerAttributesList) {
        if (Modes.BATCHED.equals(flow.getReader().getMode())) {
            readAndWriteInBatches(flow, reader, readInstruction, writerAttributesList);
        } else if (Modes.ALL_AT_ONCE.equals(flow.getReader().getMode())) {
            List<Map<String, Object>> transformedRows = transform(flow, reader.findAll(readInstruction));
            setVariableToFlowContext(flow.getReader().getWriteToVariableName(), transformedRows);
            writerAttributesList.forEach(writerAttributes -> {
                   write(transformedRows, writerAttributes);
            });
        } else if (Modes.ONE.equals(flow.getReader().getMode())) {
            Map<String, Object> readerOne = reader.findOne(readInstruction, (Map<String, Object>) null);
            List<Map<String, Object>> transformedRows = readerOne == null ? null : transform(flow, Arrays.asList(readerOne));
            if (transformedRows != null && transformedRows.size() > 0) {
                Map<String, Object> transformedRow = transformedRows.get(0);
                setVariableToFlowContext(flow.getReader().getWriteToVariableName(), transformedRow);
                writerAttributesList.forEach(writerAttributes -> {
                    write(transformedRow, writerAttributes);
                });
            }
        }
    }

    private void setVariableToFlowContext(String key, Object value){
      if (null != key){
          getCurrentContext().set(key, value);
      }
    }

    private void readAndWriteInBatches(Flow flow, GeneralReader reader, Object readInstruction,List<WriterAttributes> writerAttributesList) {
        int batchCounter = 1;

        try (BatchIterator<Map<String, Object>> batchIterator = reader.findAllInBatch(flow.getReader().getBatchSize(), readInstruction)) {
            while (batchIterator.hasNext()) {
                getCurrentContext().set(BATCH_INDEX, batchCounter);
                getCurrentContext().set("batchSize", flow.getReader().getBatchSize());
                log.info("Processing Batch : index={}, size={}", batchCounter, flow.getReader().getBatchSize());
                List<Map<String, Object>> batch = batchIterator.next();
                if(!batch.isEmpty()) {
                    List<Map<String, Object>> transformedAndFilterBatch = transform(flow, batch);
                    if(transformedAndFilterBatch.isEmpty()){
                        log.info("Empty Batch after transform-and-Filter : index={}, size={}", batchCounter, flow.getReader().getBatchSize());
                    }else {
                        writerAttributesList.forEach(writerAttributes -> write(transformedAndFilterBatch, writerAttributes));
                    }
                }else{
                    log.info("Empty Batch Read: index={}, size={}", batchCounter, flow.getReader().getBatchSize());
                }
                batchCounter++;
            }
        }
    }

    private void insertIntoMap(Map<String, Object> map){
        map.put(
                "toJson",
                (TemplateMethodModelEx)
                        list -> {
                            Object obj = list.get(0);
                            if (null == obj) return "null";
                            if (obj instanceof DefaultListAdapter) {
                                obj = ((DefaultListAdapter) obj).getWrappedObject();
                            } else if (obj instanceof DefaultMapAdapter) {
                                obj = ((DefaultMapAdapter) obj).getWrappedObject();
                            }
                            return SerDe.JSON.serialize(obj, false);
                        });
    }
    private List<Map<String, Object>> transform(@NonNull Flow flow, @NonNull List<Map<String, Object>> rows) {
        rows.forEach(row-> insertIntoMap(row));
        Map<String, Object> datapoints = new HashMap<>(getCurrentContext());
        rows.stream().forEach(row->row.putAll(datapoints));
        List<Map<String, Object>> transformedRows = rows;
        if(null != flow.getReader().getTransformer()){
            if(flow.getReader().isTransformAndMergeToOriginal()) {
                flow.getReader().getTransformer().setTransformAndMergeToOriginal(true);
            }
            transformedRows = flow.getReader().getTransformer().convert(rows);
        }
        if(null != flow.getReader().getEnrichmentOrFilterFunction()){
            transformedRows = (List<Map<String, Object>>) ScriptEngines.JAVASCRIPT.invokeFunction(flow.getReader().getEnrichmentOrFilterFunction(), transformedRows);
        }
        return transformedRows;
    }

    /*private void write(List<Map<String, Object>> list, WriterAttributes writerAttributes){
        write(list, writerAttributes.getWriter(), writerAttributes.isAppenderOnly, writerAttributes.getWriteAction(), writerAttributes.getWriteInstruction(), writerAttributes.isEnabled());
    }*/

    private void write(List<Map<String, Object>> list, Flow.Writer writer){
        write(list, getWriterAttributes((String)PipelineContext.getCurrentContext().get(PIPELINE_KEY), writer));
    }

    private void write(List<Map<String, Object>> list, WriterAttributes writerAttributes){
        log.info("****** [Writer] ****** Executing the writer with id = '{}', desc = '{}'", generateFromContext(writerAttributes.getId()), generateFromContext(writerAttributes.getDesc()));

        Object writer = writerAttributes.getWriter();
        boolean isAppenderOnly = writerAttributes.isAppenderOnly;
        Actions action = writerAttributes.writeAction;
        Object writeInstruction = writerAttributes.getWriteInstruction();
        boolean enabled = writerAttributes.enabled;
        if(!enabled){
            log.info("Writer is disabled, writer = "+ writer);
            return;
        }
        if(list.isEmpty()){
            log.info("Nothing to Write, list is empty");
            return;
        }
        GeneralAppender appender = (GeneralAppender)writer;
        if(Actions.CREATE.equals(action)){
            consumeBatch(list, (batch)-> appender.create(batch, writeInstruction), writerAttributes);
        }else{
            if(isAppenderOnly){
                throw new IllegalArgumentException("writer provider is not an updater, cant execute any action other than CREATE");
            }
            GeneralUpdater updater = (GeneralUpdater)writer;
            switch (action){
                case UPDATE:
                    consumeBatch(list, (batch)-> updater.update(batch, writeInstruction), writerAttributes);
                    break;
                case REPLACE:
                    consumeBatch(list, (batch)-> updater.replace(batch, writeInstruction), writerAttributes);
                    break;
                case CREATE_OR_REPLACE:
                    consumeBatch(list, (batch)-> updater.createOrReplace(batch, writeInstruction), writerAttributes);
                    break;
                case CREATE:
                    consumeBatch(list, (batch)-> updater.create(batch, writeInstruction), writerAttributes);
                    break;
            }
        }
    }

    private void consumeBatch(List<Map<String, Object>> list, Consumer<List<Map<String, Object>>> first, WriterAttributes writerAttributes){
        List<Map<String, Object>> failedRecords = new ArrayList<>();
        try {
            first.accept(list);
        }catch (Throwable t){
            log.error("Error while saving batch, Doing one by one save", t);
            list.forEach(row -> {
                try {
                    write(row, writerAttributes);
                }catch (Throwable t1){
                    log.error("Error while saving the record = {}", row);
                    failedRecords.add(row);
                }
            });
            if(!failedRecords.isEmpty()){
                if(writerAttributes.onFailure != null){
                    log.error("**** [Exception-Handling] Handling onFailure instructions, failedWriterId = {}, onFailureHandledId = {}", writerAttributes.getId(), writerAttributes.getOnFailure().getId());
                    write(failedRecords, getWriterAttributes(writerAttributes.onFailure));
                }
                handleFailedRecords(failedRecords);
            }
        }
    }

    private void handleFailedRecords(List<Map<String, Object>> failedRecords) {
        FAILED_RECORD_LOGGER.error("Failed Records in batch, count = {}, records = {}", failedRecords.size() , SerDe.JSON.serialize(failedRecords));
        GeneralAppender failedRecordsAppender = null;
        String failedRecordsAppenderWriteInstruction = null;
        try{
            failedRecordsAppender = getBean("failedRecordsAppender", GeneralAppender.class);
            failedRecordsAppenderWriteInstruction = getBean("failedRecordsAppenderWriteInstruction", String.class);
        }catch (Exception e){
            log.error("FailedRecordsAppender not found", e);
        }

        if(null != failedRecordsAppender && null != failedRecordsAppenderWriteInstruction) {
            List<Map<String, Object>> transformedFailedRecords = failedRecords.stream().map(record -> transformFailedRecord(record)).collect(Collectors.toList());
            Object failedRecordWriteInstruction = (null == failedRecordsAppenderWriteInstruction || failedRecordsAppenderWriteInstruction.trim().isEmpty())? null : getInstruction(failedRecordsAppenderWriteInstruction, failedRecordsAppender.getWriteInstructionType());
            failedRecordsAppender.create(transformedFailedRecords, failedRecordWriteInstruction);
        }
    }


    private Map<String, Object> transformFailedRecord(Map<String, Object> failedRecord){
        Map<String, Object> map = new HashMap<>();
        map.put(PIPELINE_KEY, getCurrentContext().get(PIPELINE_KEY));
        map.put(BATCH_INDEX, getCurrentContext().get(BATCH_INDEX));
        map.put("failedRecord", failedRecord);
        return map;
    }

    private void write(Map<String, Object> row, WriterAttributes writerAttributes){
        write(row, writerAttributes.getWriter(), writerAttributes.isAppenderOnly, writerAttributes.getWriteAction(), writerAttributes.getWriteInstruction(), writerAttributes.isEnabled());
    }

    private void write(Map<String, Object> row, Flow.Writer writer){
        write(row, getWriterAttributes((String)PipelineContext.getCurrentContext().get(PIPELINE_KEY), writer));
    }

    private void write(Map<String, Object> row, Object writer, boolean isAppenderOnly, Actions action, Object writeInstruction, boolean enabled){
        if(!enabled){
            log.info("Writer is disabled, writer = "+ writer);
            return;
        }
        GeneralAppender appender = (GeneralAppender)writer;
        if(Actions.CREATE.equals(action)){
            appender.create(row, writeInstruction);
        }else{
            if(isAppenderOnly){
                throw new IllegalArgumentException("writer provider is not an updater, cant execute any action other than CREATE");
            }
            GeneralUpdater updater = (GeneralUpdater)writer;
            switch (action){
                case UPDATE:
                    updater.update(row, writeInstruction); break;
                case REPLACE:
                    updater.replace(row, writeInstruction); break;
                case CREATE_OR_REPLACE:
                    updater.createOrReplace(row, writeInstruction); break;
                case CREATE:
                    updater.create(row, writeInstruction); break;
            }

        }
    }

    private <T> T getBean(String beanName, Class<T> type){
        Object bean = getBean(beanName);
        if(type.isInstance(bean)){
            return type.cast(bean);
        } else {
            throw new IllegalArgumentException("Bean is not of the required type: " + type.getName());
        }
    }

    private Object getBean(String beanName){
        beanName = generateFromContext(beanName);
        return new BeanSupplier(applicationContext, (String) getCurrentContext().get(PIPELINE_KEY)).apply(beanName);
    }
}