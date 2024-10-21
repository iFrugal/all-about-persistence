package lazydevs.persistence.impl.file.general;

import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;

import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */
public abstract class AbstractGeneralFileReader implements GeneralReader<ReadInstruction, Object> {

    @Override
    public Map<String, Object> findOne(ReadInstruction query, Map<String, Object> params) {
        List<Map<String, Object>> list = findAll(query, params);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, ReadInstruction query, Map<String, Object> params) {
        throw new UnsupportedOperationException("No need to implement this for transporter");
    }

    @Override
    public List<Map<String, Object>> distinct(ReadInstruction query, Map<String, Object> params) {
        throw new UnsupportedOperationException("No need to implement this for transporter");
    }

    @Override
    public long count(ReadInstruction query, Map<String, Object> params) {
        throw new UnsupportedOperationException("No need to implement this for transporter");
    }

    @Override
    public Class<ReadInstruction> getQueryType() {
        return ReadInstruction.class;
    }
}
