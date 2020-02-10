package ifrugal.persistence.utils;

import java.util.Iterator;
import java.util.List;

public abstract class BatchIterator<T> implements Iterator<List<T>>, AutoCloseable {
    protected final int batchSize;

    public BatchIterator(int batchSize) {
        if(batchSize <= 0) {
            throw new IllegalArgumentException("batchSize should be greater than zero (0). provided value = " + batchSize);
        }
        this.batchSize = batchSize;
    }
}
