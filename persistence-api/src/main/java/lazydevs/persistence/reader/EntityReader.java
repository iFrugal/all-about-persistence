package lazydevs.persistence.reader;


import lazydevs.mapper.utils.BatchIterator;

import java.util.List;
import java.util.Map;

import static lazydevs.persistence.reader.Param.convert;

/**
 * @author Abhijeet Rai
 */
public interface EntityReader<T, Q, P> {

    T findOne(Q query, Map<String, P> params);

    List<T> findAll(Q query, Map<String, P> params);

    Page<T> findPage(Page.PageRequest pageRequest, Q query, Map<String, P> params);

    BatchIterator<T> findAllInBatch(int batchSize, Q query, Map<String, P> params);

    List<Map<String, Object>> distinct(Q query, Map<String, P> params);

    long count(Q query, Map<String, P> params);

    Class<Q> getQueryType();

    default T findOne(Q query, Param<P>... params){
        return findOne(query, convert(params));
    }

    default List<T> findAll(Q query, Param<P>... params){
        return findAll(query, convert(params));
    }

    default Page<T> findPage(Page.PageRequest pageRequest, Q query, Param<P>... params){
        return findPage(pageRequest, query, convert(params));
    }

    default BatchIterator<T> findAllInBatch(int batchSize, Q query, Param<P>... params){
        return findAllInBatch(batchSize, query, convert(params));
    }

    default long count(Q query, Param<P>... params){
        return count(query, convert(params));
    }

    default List<Map<String, Object>> distinct(Q query, Param<P>... params){
        return distinct(query, convert(params));
    }


}
