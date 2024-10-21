package lazydevs.persistence.writer.general;

import lazydevs.mapper.utils.BatchIterator;

import java.util.List;
import java.util.Map;
//WI stands for WriteInstruction
public interface GeneralAppender<WI>  {
    Map<String, Object>  create(Map<String, Object> t, WI wi);

    default List<Map<String, Object>> create(List<Map<String, Object>> iterable, WI wi){
        iterable.stream().forEach(e -> create(e, wi));
        return iterable;
    }

    default void create(BatchIterator<Map<String, Object>> batchIterator, WI wi){
        while(batchIterator.hasNext()){
            create(batchIterator.next(), wi);
        }
    }

    Class<WI> getWriteInstructionType();

}
