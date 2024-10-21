package lazydevs.mapper.file.utils;

import lazydevs.mapper.file.flat.excel.CustomRowIterator;
import lazydevs.mapper.utils.BatchIterator;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class FileBatchIterator<T,R> extends BatchIterator<T> {
    final private Iterator<R> iterator;
    private boolean hasNextBatch = true;
    protected long counter = 0;
    private int noOfLinesToIgnore=0;

    public FileBatchIterator(Iterator<R> iterator, int batchSize,int noOfLinesToIgnore) {
        super(batchSize);
        this.iterator=iterator;
        this.noOfLinesToIgnore=noOfLinesToIgnore;
    }

	@Override
    public boolean hasNext() {
        return this.hasNextBatch;
    }

    @Override
    public List<T> next() {
        int recordCount = 0;
        List<T> list = new ArrayList<>();
        while(recordCount < super.batchSize && iterator.hasNext()){
            R line = iterator.next();
            if(noOfLinesToIgnore>0)
            {
                noOfLinesToIgnore--;
                continue;
            }
            if(isRowToIgnore(line)) {
                continue;
            }
            list.add(map(line));
            recordCount++;
        }
        hasNextBatch = iterator.hasNext(); //&&recordCount == batchSize ? true : false;
        return list;
    }

    public abstract T map(R line);

    public abstract boolean isRowToIgnore(R row);
}
