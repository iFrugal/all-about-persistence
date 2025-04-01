package lazydevs.persistence.impl.rest.reader.batchiterator;

import lazydevs.persistence.impl.rest.reader.RestGeneralReader.RestInstruction;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public class OffsetLimitBatchIterator extends GenericBatchIterator {

    public OffsetLimitBatchIterator(int batchSize, RestInstruction restInstruction, Function<RestInstruction, List<Map<String, Object>>> fetchDataFn) {
        super(batchSize, restInstruction, fetchDataFn);
    }

    @Override
    protected boolean hasNextAfterFetchingData(List<Map<String, Object>> data) {
        return !(data.size() < batchSize);
    }

    @Override
    protected void updateStateBeforeFetchingData(Map<String, Object> state) {
        int offset = (int) state.get("");
        offset = (batchNum - 1) * batchSize;
        state.put("offset", offset);
        state.put("limit", batchSize);
    }

}