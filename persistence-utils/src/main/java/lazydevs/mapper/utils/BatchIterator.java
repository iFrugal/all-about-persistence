package lazydevs.mapper.utils;

import java.util.Iterator;
import java.util.List;

public abstract class BatchIterator<T> implements Iterator<List<T>>, AutoCloseable{
    protected final int batchSize;

    protected BatchIterator(int batchSize){
        this.batchSize = batchSize;
    }

    @Override
    public abstract void close();
}
