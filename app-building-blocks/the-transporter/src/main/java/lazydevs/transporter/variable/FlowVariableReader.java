package lazydevs.transporter.variable;

import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lazydevs.transporter.ConsoleWriter;
import lazydevs.transporter.config.PipelineContext;

import java.util.List;
import java.util.Map;

import static lazydevs.transporter.ConsoleWriter.NOT_IMPLEMENTED;

/**
 * @author Abhijeet Rai
 */
public class FlowVariableReader implements GeneralReader<String, Object> {

    @Override
    public Map<String, Object> findOne(String key, Map<String, Object> map) {
        return (Map<String, Object>) PipelineContext.getCurrentContext().get(key);
    }

    @Override
    public List<Map<String, Object>> findAll(String key, Map<String, Object> map) {
        return (List<Map<String, Object>>) PipelineContext.getCurrentContext().get(key);
    }

    @Override
    public Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, String s, Map<String, Object> map) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public BatchIterator<Map<String, Object>> findAllInBatch(int i, String s, Map<String, Object> map) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public List<Map<String, Object>> distinct(String s, Map<String, Object> map) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public long count(String s, Map<String, Object> map) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Class<String> getQueryType() {
        return String.class;
    }
}
