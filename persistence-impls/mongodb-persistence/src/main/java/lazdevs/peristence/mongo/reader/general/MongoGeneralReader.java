package lazdevs.peristence.mongo.reader.general;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import lazdevs.peristence.mongo.common.BaseMongoReader;
import lazdevs.peristence.mongo.common.BaseMongoReader.BaseMongoQuery;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.mapper.utils.DefaultBatchIterator;
import lazydevs.persistence.connection.ConnectionProvider;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static lazdevs.peristence.mongo.common.BaseMongoReader.*;



/**
 * @author Abhijeet Rai
 */
@RequiredArgsConstructor
public class MongoGeneralReader implements GeneralReader<MongoGeneralQuery, Object> {
    private final ConnectionProvider<MongoDatabase> mongoDatabaseConnectionProvider;

    public MongoGeneralReader(@NonNull MongoClient mongoClient, @NonNull String dbName) {
        this.mongoDatabaseConnectionProvider = () -> mongoClient.getDatabase(dbName);
    }

    private BaseMongoQuery<Document> covert(MongoGeneralQuery query){
        return new BaseMongoQuery<>(query.getQuery(), this.mongoDatabaseConnectionProvider.getConnection().getCollection(query.getCollection()));
    }

    @Override
    public Map<String, Object> findOne(MongoGeneralQuery query, Map<String, Object> params) {
        return getMongoIterable(covert(query)).first();
    }

    @Override
    public List<Map<String, Object>> findAll(MongoGeneralQuery query, Map<String, Object> params) {
        return getMongoIterable(covert(query)).into(new ArrayList<>());
    }

    @Override
    public Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, MongoGeneralQuery generalQuery, Map<String, Object> params) {
        return get(pageRequest, covert(generalQuery));
    }


    @Override
    public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, MongoGeneralQuery query, Map<String, Object> params) {
        MongoCursor<Document> iterator = getMongoIterable(covert(query)).iterator();
        return new DefaultBatchIterator<Map<String, Object>>((Iterable<Map<String, Object>>) iterator, batchSize);
    }

    @Override
    public List<Map<String, Object>> distinct(MongoGeneralQuery query, Map<String, Object> params) {
        String key = query.getQuery().getDistinct().getFieldName();
        return getDistinctIterable(covert(query)).into(new ArrayList<>()).stream().map(result -> {
            Map<String, Object> map = new HashMap<>();
            map.put(key, result);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public long count(MongoGeneralQuery query, Map<String, Object> params) {
        return BaseMongoReader.count(covert(query));
    }


    @Override
    public Class<MongoGeneralQuery> getQueryType() {
        return MongoGeneralQuery.class;
    }
}
