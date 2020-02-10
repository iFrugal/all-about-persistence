package ifrugal.persistence.api;

import ifrugal.persistence.utils.BatchIterator;
import ifrugal.persistence.utils.Page;
import ifrugal.persistence.utils.PageRequest;

import java.util.List;

public interface ReadablePersistence<I, R> {


    /**
     * reads the persistence all at once and returns a List<T>, converts to T using rowTransformationTemplate
     * @param input
     * @param converter
     * @param type
     * @param <T>
     * @return
     */
    <T> List<T> readAll(I input, Converter<T, R> converter, Class<T> type);


    /**
     * reads the persistence in paginated way. For every page a new reading of persistence happens, a no pointer to last read index is kept.
     * @param pageRequest
     * @param input
     * @param converter
     * @param type
     * @param <T>
     * @return
     */
    <T> Page<T> read(PageRequest pageRequest, I input, Converter<T, R> converter, Class<T> type);


    /**
     *  reads the persistence in batches. Since it maintains a pointer to last read index, it is efficient and should only be used when we want to read all data, but in batches.
     *
     * @param input
     * @param batchSize
     * @param converter
     * @param type
     * @param <T>
     * @return
     */
    <T> BatchIterator<T> readInBatches(I input, int batchSize, Converter<T, R> converter, Class<T> type);


    interface Converter<T, R>{
        T convert(R record);
    }
}
