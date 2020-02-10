package ifrugal.persistence.file;

import ifrugal.persistence.api.ReadablePersistence;
import ifrugal.persistence.api.WritablePersistence;
import ifrugal.persistence.api.general.GeneralReadablePersistence;
import ifrugal.persistence.utils.BatchIterator;
import ifrugal.persistence.utils.Page;
import ifrugal.persistence.utils.PageRequest;

import java.util.List;

public abstract class FilePersistence extends GeneralReadablePersistence implements ReadablePersistence<FileReadInput, String>, WritablePersistence<FileWriteInput>{

    public <I,R> FilePersistence(ReadablePersistence<I,R> readablePersistence) {
        super(readablePersistence);
    }

    /*@Override
    public <T> List<T> readAll(FileReadInput input, Converter<T, String> converter, Class<T> type) {
        return null;
    }

    @Override
    public <T> Page<T> read(PageRequest pageRequest, FileReadInput input, Converter<T, String> converter, Class<T> type) {
        return null;
    }

    @Override
    public <T> BatchIterator<T> readInBatches(FileReadInput input, int batchSize, Converter<T, String> converter, Class<T> type) {
        return null;
    }

    @Override
    public <T> void write(List<T> list, FileWriteInput fileWriteInput) {

    }*/
}
