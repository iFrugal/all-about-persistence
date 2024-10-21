package lazydevs.persistence.writer.general;

import lazydevs.persistence.reader.Param;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lazydevs.persistence.reader.Param.convert;

//WI stands for WriteInstruction
public interface GeneralUpdater<Q, WI> extends GeneralAppender<WI> {

    Map<String, Object> replace(Map<String, Object> t, WI wi);

    Map<String, Object> update(Map<String, Object> t, WI wi);

    default Map<String, Object> createOrReplace(Map<String, Object> t, WI wi){
        return replace(t, wi);
    }

    Map<String, Object> updateOne(String id, Map<String, Object> fieldsToUpdate, WI wi);

    long updateMany(Q query, Map<String, Object> fieldsToUpdate, WI wi);


    Map<String, Object> delete(Map<String, Object> t,  WI wi);

    Map<String, Object> delete(String id,  WI wi);


    default List<Map<String, Object>> replace(List<Map<String, Object>> iterable,  WI wi){
        return iterable.stream().map(e-> replace(e, wi)).collect(Collectors.toList());
    }

    default List<Map<String, Object>> update(List<Map<String, Object>> iterable, WI wi){
        return iterable.stream().map(e-> update(e, wi)).collect(Collectors.toList());
    }

    default List<Map<String, Object>> createOrReplace(List<Map<String, Object>> iterable, WI wi){
        return iterable.stream().map(e-> createOrReplace(e, wi)).collect(Collectors.toList());
    }

    default List<Map<String, Object>> delete(List<Map<String, Object>> iterable, WI wi){
        return iterable.stream().map(e-> delete(e, wi)).collect(Collectors.toList());
    }
}
