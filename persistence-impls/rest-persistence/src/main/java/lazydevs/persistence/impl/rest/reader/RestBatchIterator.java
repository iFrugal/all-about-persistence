package lazydevs.persistence.impl.rest.reader;

import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.impl.rest.reader.RestGeneralReader.RestInstruction;

import java.util.Map;

public abstract class RestBatchIterator extends BatchIterator<Map<String, Object>> {
        protected final RestInstruction restInstruction;

        public RestBatchIterator(final int batchSize, RestInstruction restInstruction){
            super(batchSize);
            this.restInstruction = restInstruction;
        }
        @Override
        public void close() {}
}
