package lazydevs.persistence.reader;


import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lazydevs.mapper.utils.engine.ScriptEngines.JAVASCRIPT;

/**
 * @author Abhijeet Rai
 */
public interface GeneralReader<Q, P> extends EntityReader<Map<String, Object>, Q, P> {

    Map<String, Object> findOne(Q query, Map<String, P> params);

    List<Map<String, Object>> findAll(Q query, Map<String, P> params);

    Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, Q query, Map<String, P> params);

    BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Q query, Map<String, P> params);

    Class<Q> getQueryType();


    default Map<String, Object> findOne(Q query, Map<String, P> params, GeneralTransformer generalTransformer){
        return generalTransformer.convert(findOne(query, params));
    }

    default List<Map<String, Object>> findAll(Q query, Map<String, P> params, GeneralTransformer generalTransformer){
        return generalTransformer.convert(findAll(query, params));
    }

    default Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, Q query, Map<String, P> params, GeneralTransformer generalTransformer){
        Page<Map<String, Object>> page = findPage(pageRequest, query, params);
        page.setData(generalTransformer.convert(page.getData()));
        return page;
    }

    default BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Q query, Map<String, P> params, GeneralTransformer generalTransformer){
        return new TransformedBatchIterator(findAllInBatch(batchSize, query, params), generalTransformer);
    }

}
