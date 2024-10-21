package lazydevs.persistence.impl.file.general.fileMapper;

import lazydevs.mapper.file.FileMapper;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.mapper.utils.reflection.ReflectionUtils;
import lazydevs.persistence.impl.file.general.AbstractGeneralFileWriter;
import lazydevs.persistence.impl.file.general.WriteInstruction;
import lazydevs.persistence.writer.general.GeneralAppender;

import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
public class GeneralFileMapperWriter extends AbstractGeneralFileWriter {
    private final FileMapper fileMapper;

    public GeneralFileMapperWriter(InitDTO fileMapperInit){
        this.fileMapper = ReflectionUtils.getInterfaceReference(fileMapperInit, FileMapper.class);
    }

    public GeneralFileMapperWriter(FileMapper fileMapper) {
        this.fileMapper = fileMapper;
    }

    @Override
    public List<Map<String, Object>> create(List<Map<String, Object>> iterable, WriteInstruction writeInstruction) {
        fileMapper.writeFile(iterable, writeInstruction.getFilePath(), writeInstruction.getColumnIndexMap(), writeInstruction.getHeaders());
        return iterable;
    }

    @Override
    public void create(BatchIterator<Map<String, Object>> batchIterator, WriteInstruction writeInstruction) {
        fileMapper.writeFile(batchIterator, writeInstruction.getFilePath(), writeInstruction.getColumnIndexMap(), writeInstruction.getHeaders());
    }

    @Override
    public Map<String, Object> updateOne(String id, Map<String, Object> fieldsToUpdate, WriteInstruction writeInstruction) {
        throw new UnsupportedOperationException("This is not implemented yet !!");
    }

    @Override
    public long updateMany(Object query, Map<String, Object> fieldsToUpdate, WriteInstruction writeInstruction) {
        throw new UnsupportedOperationException("This is not implemented yet !!");
    }
}
