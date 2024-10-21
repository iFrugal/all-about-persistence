package lazydevs.persistence.reader;

import lazydevs.mapper.utils.BatchIterator;

import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
public class TransformedBatchIterator extends BatchIterator<Map<String, Object>> {
    private final BatchIterator<Map<String, Object>> originalBatchIterator;
    private final GeneralTransformer generalTransformer;


    public TransformedBatchIterator(BatchIterator<Map<String, Object>> originalBatchIterator, GeneralTransformer generalTransformer) {
        super(1000);
        this.originalBatchIterator = originalBatchIterator;
        this.generalTransformer = generalTransformer;
    }


    @Override
    public boolean hasNext() {
        return originalBatchIterator.hasNext();
    }

    @Override
    public List<Map<String, Object>> next() {
        List<Map<String, Object>> list = originalBatchIterator.next();
        return generalTransformer.convert(list);
    }

    @Override
    public void close() {
        originalBatchIterator.close();
    }
}
