package lazydevs.persistence.impl.rest.reader.batchiterator;

import lazydevs.mapper.rest.RestInput;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.persistence.impl.rest.reader.RestBatchIterator;
import lazydevs.persistence.impl.rest.reader.RestGeneralReader.RestInstruction;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

@Slf4j
public abstract class GenericBatchIterator extends RestBatchIterator {
    private Map<String, Object> state = new HashMap<>();

    protected int batchNum = 1;
    private boolean hasNextBatch = true;
    private final Function<RestInstruction, List<Map<String, Object>>> fetchDataFn;

    public GenericBatchIterator(final int batchSize, RestInstruction restInstruction,
                                    Function<RestInstruction, List<Map<String, Object>>> fetchDataFn){
        super(batchSize, restInstruction);
        this.fetchDataFn = fetchDataFn;
    }


    @Override
    public boolean hasNext() {
        return this.hasNextBatch;
    }

    @Override
    public List<Map<String, Object>> next() {
        if (!hasNextBatch) {
            throw new NoSuchElementException("Could not find next batch.");
        }
        updateStateBeforeFetchingData(state);
        RestInstruction copiedRestInstruction = copyRestInstruction(state);
        List<Map<String, Object>> data = fetchDataFn.apply(copiedRestInstruction);
        hasNextBatch = hasNextAfterFetchingData(data);
        batchNum++;
        log.info("Records fetched: {}" , data.size());
        return data;
    }

    protected abstract boolean hasNextAfterFetchingData(List<Map<String, Object>> data);

    protected abstract void updateStateBeforeFetchingData(Map<String, Object> state);

    protected RestInstruction copyRestInstruction(Map<String, Object> map) {
        RestInstruction copy = new RestInstruction();
        copy.setResponseExtractionLogic(restInstruction.getResponseExtractionLogic());
        copy.setCountInstruction(restInstruction.getCountInstruction());
        RestInput restInput = new RestInput(restInstruction.getRequest());
        restInput.setQueryParams(TemplateEngine.getInstance().generate(restInput.getQueryParams(), map));
        restInput.setUrl(TemplateEngine.getInstance().generate(restInput.getUrl(), map));
        copy.setRequest(restInput);
        copy.setRestAuthInitDTO(restInstruction.getRestAuthInitDTO());
        return copy;
    }
}