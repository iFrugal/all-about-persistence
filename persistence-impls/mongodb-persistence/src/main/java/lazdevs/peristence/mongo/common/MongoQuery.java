package lazdevs.peristence.mongo.common;

import lombok.*;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static lazydevs.mapper.utils.SerDe.JSON;
import static lazdevs.peristence.mongo.common.ReadMode.*;

/**
 * @author Abhijeet Rai
 */
@Getter @Setter @ToString @NoArgsConstructor @AllArgsConstructor
public class MongoQuery {
    private Integer batchSize;
    private ReadMode readMode = ReadMode.FIND;
    private Find find = new Find();
    private Aggregate aggregate;
    private MapReduce mapReduce;
    private Filterable count;
    private Distinct distinct;

    public static MongoQuery findQuery(Find find) {
        return findQuery(find, null);
    }

    public static MongoQuery findQuery(Find find, Integer batchSize) {
        return new MongoQuery(batchSize, ReadMode.FIND, find, null, null, null, null);
    }

    public static MongoQuery countQuery(String filterJsonString) {
        return new MongoQuery(null, COUNT , null, null, null, new Filterable(JSON.deserializeToMap(filterJsonString)), null);
    }

    public static MongoQuery countQuery(Map<String, Object> filter) {
        return new MongoQuery(null, COUNT, null, null, null, new Filterable(filter), null);
    }

    public static MongoQuery aggregateQuery(List<Map<String, Object>> pipeline) {
        return new MongoQuery(null, AGGREGATE, null, new Aggregate(pipeline), null, null, null);
    }

    public static MongoQuery aggregateQuery(Map<String, Object>... pipeline) {
        return new MongoQuery(null, AGGREGATE, null, new Aggregate(asList(pipeline)), null, null, null);
    }

    public static MongoQuery mapReduce(String mapFunction, String reduceFunction) {
        return new MongoQuery(null, MAP_REDUCE, null, null, new MapReduce(mapFunction, reduceFunction), null, null);
    }

    public static MongoQuery distinct(String fieldName, Map<String, Object> filter, Class<?> resultClass){
        return new MongoQuery(null, DISTINCT, null, null, null, null, new Distinct(fieldName, filter, resultClass));
    }

    public static MongoQuery distinct(String fieldName, String filterJsonString, Class<?> resultClass){
        return new MongoQuery(null, DISTINCT, null, null, null, null, new Distinct(fieldName, JSON.deserializeToMap(filterJsonString), resultClass));
    }
}
