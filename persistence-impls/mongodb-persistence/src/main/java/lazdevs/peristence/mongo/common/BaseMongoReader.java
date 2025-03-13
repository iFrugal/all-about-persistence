package lazdevs.peristence.mongo.common;


import com.mongodb.client.DistinctIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import lazydevs.mapper.utils.reflection.ClassUtils;
import lazydevs.persistence.reader.Page;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static lazdevs.peristence.mongo.common.ReadMode.FIND;

/**
 * @author Abhijeet Rai
 */

public class BaseMongoReader {
    private BaseMongoReader(){}

    @Getter @Setter @RequiredArgsConstructor
    public static class BaseMongoQuery<T>{
        @NonNull private final MongoQuery query;
        @NonNull private final MongoCollection<T> collection;
    }

    public static Page<Map<String, Object>> get(Page.PageRequest pageRequest, BaseMongoQuery<Document> baseMongoQuery){
        MongoQuery query = baseMongoQuery.getQuery();
        if(!FIND.equals(query.getReadMode())){
            throw new IllegalArgumentException("Pagination is available only for ReadMode = FIND, not supported in MongoDb-Api itself");
        }
        validate(query, true);
        query.setCount(new Filterable(query.getFind().getFilter()));
        return Page.<Map<String, Object>>builder(pageRequest)
                .totalNoOfRecords(count(baseMongoQuery))
                .data(
                        ((FindIterable<Document>) getMongoIterable(baseMongoQuery))
                                .skip((pageRequest.getPageNum() - 1) * pageRequest.getPageSize())
                                .limit(pageRequest.getPageSize())
                                .into(new ArrayList<>())
                )
                .build();
    }


    public static <T> Page<T> getPageOfEntity(Page.PageRequest pageRequest, BaseMongoQuery<T> baseMongoQuery){
        MongoQuery query = baseMongoQuery.getQuery();
        if(!FIND.equals(query.getReadMode())){
            throw new IllegalArgumentException("Pagination is available only for ReadMode = FIND, not supported in MongoDb-Api itself");
        }
        validate(query, true);
        query.setCount(new Filterable(query.getFind().getFilter()));
        return Page.<T>builder(pageRequest)
                .totalNoOfRecords(count(baseMongoQuery))
                .data(
                        ((FindIterable<T>) getMongoIterable(baseMongoQuery))
                                .skip((pageRequest.getPageNum() - 1) * pageRequest.getPageSize())
                                .limit(pageRequest.getPageSize())
                                .into(new ArrayList<>())
                )
                .build();
    }

    public static <T> long count(BaseMongoQuery<T> baseMongoQuery) {
        MongoQuery query = baseMongoQuery.getQuery();
        validate(query);
        if (null == query.getCount()) {
            query.setCount(new Filterable(query.getFind().getFilter()));
        }
        return baseMongoQuery.getCollection().countDocuments(new Document(query.getCount().getFilter()));
    }

    public static <T> MongoIterable<T> getMongoIterable(BaseMongoQuery<T> baseMongoQuery){
        MongoQuery query = baseMongoQuery.getQuery();
        MongoCollection<T> collection = baseMongoQuery.getCollection();
        MongoIterable<T> mongoIterable = null;
        switch (query.getReadMode()){
            case FIND:
                Find find = query.getFind();
                mongoIterable = collection.find(new Document(find.getFilter()))
                        .projection(new Document(get(find.getProjection(), new HashMap<>())))
                        .sort(new Document(get(find.getSort(), new HashMap<>())));
                break;

            case AGGREGATE:
                Aggregate aggregate = query.getAggregate();
                mongoIterable = collection.aggregate(aggregate.getPipeline().stream().map(Document::new).collect(toList()));
                break;

            case MAP_REDUCE:
                MapReduce mapReduce = query.getMapReduce();
                mongoIterable = collection.mapReduce(mapReduce.getMapFunction(), mapReduce.getReduceFunction());
                break;

            default:
                throw new UnsupportedOperationException("ReadMode not supported. readMode = " + query.getReadMode());

        }
        if(null != query.getBatchSize()){
            mongoIterable.batchSize(query.getBatchSize());
        }
        return mongoIterable;
    }

    public static DistinctIterable<?> getDistinctIterable(BaseMongoQuery baseMongoQuery){
        Distinct distinct = baseMongoQuery.getQuery().getDistinct();
        return baseMongoQuery.getCollection().distinct(distinct.getFieldName(), new Document(distinct.getFilter()), null == distinct.getResultClass() ? ClassUtils.loadClass(distinct.getResultClassFqcn()): distinct.getResultClass());
    }


    private static <T> T get(T originalVal, T defaultVal){
        return null == originalVal ? defaultVal : originalVal;
    }

    public static void validate(@NonNull MongoQuery mongoDbQuery) {
        validate(mongoDbQuery, false);
    }

    public static void validate(@NonNull MongoQuery mongoDbQuery, boolean isPage){
        validateNonNull(mongoDbQuery.getReadMode(), "readMode");
        switch (mongoDbQuery.getReadMode()){
            case FIND:
                Find find = mongoDbQuery.getFind();
                validateNonNull(find, "find", "default readMode is 'FIND', you may want to change the readMode, or else, set the property 'find'");
                if(isPage){
                    Map<String, Object> sort = find.getSort();
                    if(null == sort || sort.isEmpty()){
                        find.setSort(Collections.singletonMap("_id", 1));
                    }
                }
                break;
            case COUNT:
                Filterable count = mongoDbQuery.getCount();
                if(null == count){
                    Find find1 = mongoDbQuery.getFind();
                    validateNonNull(find1, "find", " The field 'count' is null, tried getting the filter from 'find', but that is also null. You may want to change the readMode, or else, set the property 'find.query' or 'count.query'");
                }else{
                    validateNonNull(count.getFilter(), "count.filter");break;
                }
                break;
            default:
                break;
        }
    }

    private static void validateNonNull(Object o, String fieldName){
        if(null == o){
            throw new IllegalArgumentException(String.format("The field '%s' is null.", fieldName));
        }
    }

    private static void validateNonNull(Object o, String fieldName, String customMessage){
        if(null == o){
            throw new IllegalArgumentException(String.format("The field '%s' is null. %s", fieldName, customMessage));
        }
    }

}
