package lazydevs.persistence.impl.rest.reader.batchiterator;

import lazydevs.mapper.rest.RestInput;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.persistence.impl.rest.reader.RestBatchIterator;
import lazydevs.persistence.impl.rest.reader.RestGeneralReader;
import lazydevs.persistence.impl.rest.reader.RestGeneralReader.RestInstruction;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

@Slf4j
public class PageBasedBatchIterator extends GenericBatchIterator {
        private long totalPages;
        private final Function<RestInstruction, Long> countFn;

        public PageBasedBatchIterator(final int batchSize, RestInstruction restInstruction,
                                      Function<RestInstruction, List<Map<String, Object>>> fetchDataFn,
                                      Function<RestInstruction, Long> countFn){
            super(batchSize, restInstruction, fetchDataFn);
            this.countFn = countFn;
             Map<String, Object> map = Map.of("pageNum", batchNum, "pageSize", 1);
            this.totalPages = getPageCount(count(copyRestInstruction(map)), batchSize);
        }

    private long count(RestInstruction restInstruction) {
            return countFn.apply(restInstruction);
    }


    @Override
    protected boolean hasNextAfterFetchingData(List<Map<String, Object>> data) {
        return (batchNum != totalPages);
    }

    @Override
    protected void updateStateBeforeFetchingData(Map<String, Object> state) {
        state.put("pageNum", batchNum);
        state.put("pageSize", batchSize);
    }




    private long getPageCount(long totalRecords, int batchSize) {
        long quotient = totalRecords / batchSize;
        long remainder = totalRecords % batchSize;
        return remainder == 0 ? quotient : quotient + 1;
    }
}