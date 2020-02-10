package ifrugal.persistence.api.general;

import ifrugal.persistence.api.ReadablePersistence;
import ifrugal.persistence.utils.BatchIterator;
import ifrugal.persistence.utils.Page;
import ifrugal.persistence.utils.PageRequest;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class GeneralReadablePersistence<I,R> {
    private final ReadablePersistence<I, R> readablePersistence;

    /**
     * reads the persistence all at once and returns a List<T>, converts to T using converter
     * @param input
     * @param converter
     * @return
     */
    public List<Map<String, Object>> readAll(I input, MapConverter<R> converter){
        return readablePersistence.readAll(input, converter, null);
    }


    /**
     * reads the persistence in paginated way. For every page a new reading of persistence happens, a no pointer to last read index is kept.
     * @param pageRequest
     * @param input
     * @param converter
     * @return
     */
   public Page<Map<String, Object>> read(PageRequest pageRequest, I input, MapConverter<R> converter){
       return readablePersistence.read(pageRequest, input, converter, null);
   }


    /**
     * reads the persistence in batches. Since it maintains a pointer to last read index, it is efficient and should only be used when we want to read all data, but in batches.
     *
     * @param input
     * @param batchSize
     * @param converter
     * @return
     */
    public BatchIterator<Map<String, Object>> readInBatches(I input, int batchSize, MapConverter<R> converter){
        return readablePersistence.readInBatches(input, batchSize, converter, null);

    }

    public abstract class MapConverter<R> implements ReadablePersistence.Converter<Map<String, Object>, R> {

    }

}
