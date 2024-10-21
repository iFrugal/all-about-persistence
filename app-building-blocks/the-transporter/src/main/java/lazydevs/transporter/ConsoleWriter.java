package lazydevs.transporter;

import lazydevs.mapper.utils.SerDe;
import lazydevs.persistence.writer.general.GeneralUpdater;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Abhijeet Rai
 */
@Component
public class ConsoleWriter implements GeneralUpdater<String, Object> {

    public static final String NOT_IMPLEMENTED = "No need to implement this, hence not implemented";

    @Override
    public Map<String, Object> replace(Map<String, Object> map, Object o) {
        System.out.println(SerDe.JSON.serialize(map, false));
        return map;
    }

    @Override
    public Map<String, Object> update(Map<String, Object> map, Object o) {
        System.out.println(SerDe.JSON.serialize(map, false));
        return map;
    }

    @Override
    public Map<String, Object> delete(Map<String, Object> map, Object o) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> delete(String s, Object o) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public Map<String, Object> create(Map<String, Object> map, Object o) {
        System.out.println(SerDe.JSON.serialize(map, false));
        return map;
    }

    @Override
    public Class<Object> getWriteInstructionType() {
        return Object.class;
    }

    @Override
    public Map<String, Object> createOrReplace(Map<String, Object> map, Object o) {
        System.out.println(SerDe.JSON.serialize(map, false));
        return map;
    }

    @Override
    public Map<String, Object> updateOne(String id, Map<String, Object> fieldsToUpdate, Object o) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    @Override
    public long updateMany(String query, Map<String, Object> fieldsToUpdate, Object o) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}