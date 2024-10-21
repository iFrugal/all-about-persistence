package lazydevs.mapper.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * @author Abhijeet Rai
 */
public class DefaultBatchIterator<T> extends BatchIterator<T> {
    private boolean hasNextBatch = true;
    private Iterator<T> iterator;
    private Iterator<?> originalIterator;
    private boolean conversionRequired = false;
    private Function<Object, T> converter;

    public DefaultBatchIterator(final Iterator<T> iterator, final int batchSize){
        super(batchSize);
        this.iterator = iterator;
    }

    public DefaultBatchIterator(final Iterable<T> iterable, final int batchSize){
        this(iterable.iterator(), batchSize);
    }

    public <X>DefaultBatchIterator(final Iterable<X> iterable, final int batchSize, final Function<X, T> converter){
        super(batchSize);
        this.originalIterator = iterable.iterator();
        this.converter = (Function<Object, T>) converter;
        this.conversionRequired = true;
    }

    @Override
    public void close() {
        if(conversionRequired){
            originalIterator.forEachRemaining(document -> {
            });
        }else {
            iterator.forEachRemaining(document -> {
            });
        }
    }

    @Override
    public boolean hasNext() {
        return this.hasNextBatch;
    }

    @Override
    public List<T> next() {
        int recordCount = 0;
        List<T> list = new ArrayList<>();
        Iterator it = conversionRequired ? originalIterator : iterator;
        try {
            while (recordCount < batchSize && it.hasNext()) {
                T t;
                if(conversionRequired) {
                    t = converter.apply(originalIterator.next());
                }else{
                    t = iterator.next();
                }
                list.add(t);
                recordCount++;
            }
        }catch (Exception e){
            throw new RuntimeException("Error while iterating over original iterator", e);
        }
        hasNextBatch = recordCount == batchSize;
        return list;
    }
}

