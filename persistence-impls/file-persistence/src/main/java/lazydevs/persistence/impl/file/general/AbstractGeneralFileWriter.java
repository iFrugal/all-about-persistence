package lazydevs.persistence.impl.file.general;

import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lazydevs.persistence.writer.general.GeneralUpdater;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
public abstract class AbstractGeneralFileWriter implements GeneralUpdater<Object, WriteInstruction> {

    @Override
    public Map<String, Object> create(Map<String, Object> t, WriteInstruction writeInstruction) {
        create(Arrays.asList(t), writeInstruction);
        return t;
    }

    @Override
    public Class<WriteInstruction> getWriteInstructionType() {
        return WriteInstruction.class;
    }

    @Override
    public Map<String, Object> replace(Map<String, Object> t, WriteInstruction writeInstruction) {
        throw new UnsupportedOperationException("replace() does not makes sense of Files");
    }

    @Override
    public Map<String, Object> update(Map<String, Object> t, WriteInstruction writeInstruction) {
        throw new UnsupportedOperationException("update() does not makes sense of Files");
    }

    @Override
    public Map<String, Object> delete(Map<String, Object> t, WriteInstruction writeInstruction) {
        throw new UnsupportedOperationException("delete() does not makes sense of Files");
    }

    @Override
    public Map<String, Object> delete(String id, WriteInstruction writeInstruction) {
        throw new UnsupportedOperationException("delete() does not makes sense of Files");
    }
}
