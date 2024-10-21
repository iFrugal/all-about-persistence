package lazydevs.persistence.impl.file.general.fileMapper;

import lazydevs.mapper.file.FileMapper;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.mapper.utils.reflection.ReflectionUtils;
import lazydevs.persistence.impl.file.general.AbstractGeneralFileReader;
import lazydevs.persistence.impl.file.general.ReadInstruction;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
public class GeneralFileMapperReader extends AbstractGeneralFileReader {
    private final FileMapper fileMapper;

    public GeneralFileMapperReader(InitDTO fileMapperInit){
        this.fileMapper = ReflectionUtils.getInterfaceReference(fileMapperInit, FileMapper.class);
    }

    public GeneralFileMapperReader(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }


    @Override
    public List<Map<String, Object>> findAll(ReadInstruction readInstruction, Map<String, Object> params) {
        ensureFileExists(readInstruction);
        return fileMapper.readFile(readInstruction.getFilePath(), (String) null, readInstruction.getNoOfLinesToIgnore() );
    }

    @Override
    public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, ReadInstruction readInstruction, Map<String, Object> params) {
        ensureFileExists(readInstruction);
        return fileMapper.readFileInBatches(readInstruction.getFilePath(), null, batchSize, readInstruction.getNoOfLinesToIgnore() );
    }

    protected boolean ensureFileExists(ReadInstruction readInstruction){
        return Files.exists(Paths.get(readInstruction.getFilePath()));
    }

}
