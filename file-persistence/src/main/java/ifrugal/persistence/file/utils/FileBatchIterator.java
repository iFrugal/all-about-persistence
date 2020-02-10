package ifrugal.persistence.file.utils;

import ifrugal.persistence.utils.BatchIterator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

public abstract class FileBatchIterator<T> extends BatchIterator<T> {
    private final LineIterator lineIterator;
    private boolean hasNextBatch = true;
    private String commentSymbol;
    private int numberOfRowsToIgnoreFromStart;

    public FileBatchIterator<T> commentSymbol(@NonNull String commentSymbol){
        this.commentSymbol = commentSymbol; return this;
    }

    public FileBatchIterator<T> numberOfRowsToIgnoreFromStart(@NonNull int numberOfRowsToIgnoreFromStart){
        if(numberOfRowsToIgnoreFromStart > 0) {
            this.numberOfRowsToIgnoreFromStart = numberOfRowsToIgnoreFromStart;
        }
        return this;
    }


    public FileBatchIterator(int batchSize, File file, Charset charset) {
        super(batchSize);
        try{
            lineIterator = FileUtils.lineIterator(file, charset.name());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public FileBatchIterator(int batchSize, InputStream inputStream, Charset charset) {
        super(batchSize);
        try{
            lineIterator = IOUtils.lineIterator(inputStream, charset.name());
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public FileBatchIterator(int batchSize, File file) {
        this(batchSize, file, Charset.defaultCharset());
    }

    public FileBatchIterator(int batchSize, InputStream inputStream) {
        this(batchSize, inputStream, Charset.defaultCharset());
    }

    @Override
    public void close(){
        try{
            lineIterator.close();
        }catch (Exception e){
            throw new RuntimeException("Error while closing lineIterator", e);
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
        while(recordCount < super.batchSize && lineIterator.hasNext()){
            String line = lineIterator.nextLine();
            if(numberOfRowsToIgnoreFromStart-- > 0){
                continue;
            }
            if(null != commentSymbol && line.startsWith(commentSymbol)){
                continue;
            }
            list.add(map(line));
            recordCount++;
        }
        hasNextBatch = lineIterator.hasNext();
        return list;
    }

    public abstract T map(String line);
}
