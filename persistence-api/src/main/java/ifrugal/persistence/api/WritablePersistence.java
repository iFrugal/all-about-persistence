package ifrugal.persistence.api;

import ifrugal.persistence.utils.BatchIterator;
import ifrugal.persistence.utils.Page;
import ifrugal.persistence.utils.PageRequest;
import java.util.List;

public interface WritablePersistence<WI> {
    /**
     * writes to the persistence all at once and returns a List<T>
     *
     * @param list
     * @param <T> represents Type of Data in output
     * @return
     */
    <T> void write(List<T> list, WI writeInput);

    /**
     * iterates over the page reads the persistence in paginated way. For every page a new reading of persistence happens, a no pointer to last read index is kept.
     *
     * @param pageRequest
     * @param <T>
     * @return
     */
    default <T, RI, R> void write(WI writeInput, ReadablePersistence<RI, R> readablePersistence, PageRequest pageRequest, RI input, ReadablePersistence.Converter<T, R> converter, Class<T> type){
        boolean hasNextPage = true;
        int nextPageNum = pageRequest.getPageNum();
        while(hasNextPage){
            Page<T> page =  readablePersistence.read(new PageRequest(nextPageNum, pageRequest.getPageSize()), input, converter, type);
            hasNextPage = page.hasNextPage();
            nextPageNum = page.getNextPageIndex();
            write(page.getData(), writeInput);
        }
    }

    /**
     * reads the persistence in batches way. Since it maintains a pointer to last read index, it is efficient and should only be used when we want to read all data, but in batches.
     *
     * @param batchIterator
     * @param <T>
     * @return
     */
    default <T>  void write(BatchIterator<T> batchIterator, WI writeInput){
        while(batchIterator.hasNext()){
            batchIterator.forEachRemaining(list -> write(list, writeInput));
        }
    }
}
