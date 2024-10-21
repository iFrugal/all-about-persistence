package lazydevs.transporter.variable;

import lazydevs.persistence.writer.general.GeneralUpdater;
import lazydevs.transporter.config.PipelineContext;

import java.util.Map;

import static lazydevs.transporter.ConsoleWriter.NOT_IMPLEMENTED;

/**
 * @author Abhijeet Rai
 */
public class FlowVariableWriter implements GeneralUpdater<Object, String> {

    @Override
    public Map<String, Object> replace(Map<String, Object> map, String s) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> update(Map<String, Object> map, String s) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> delete(Map<String, Object> map, String s) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> delete(String s, String s2) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> map, String variableKey) {
         PipelineContext.getCurrentContext().set(variableKey, map);
         return map;
    }

    @Override
    public Class<String> getWriteInstructionType() {
        return String.class;
    }

    @Override
    public Map<String, Object> createOrReplace(Map<String, Object> t, String wi) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> updateOne(String id, Map<String, Object> fieldsToUpdate, String s) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public long updateMany(Object query, Map<String, Object> fieldsToUpdate, String s) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
