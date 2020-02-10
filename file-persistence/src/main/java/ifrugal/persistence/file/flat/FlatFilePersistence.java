package ifrugal.persistence.file.flat;

import ifrugal.persistence.api.ReadablePersistence;
import ifrugal.persistence.file.FilePersistence;
import ifrugal.persistence.file.FileReadInput;
import ifrugal.persistence.file.FileWriteInput;
import ifrugal.persistence.utils.BatchIterator;
import ifrugal.persistence.utils.Page;
import ifrugal.persistence.utils.PageRequest;

import java.util.List;

public class FlatFilePersistence extends FilePersistence {

    public <I, R> FlatFilePersistence(ReadablePersistence<I, R> readablePersistence) {
        super(readablePersistence);
    }

    @Override
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
    public <T> void write(List<T> list, FileWriteInput writeInput) {

    }
}
