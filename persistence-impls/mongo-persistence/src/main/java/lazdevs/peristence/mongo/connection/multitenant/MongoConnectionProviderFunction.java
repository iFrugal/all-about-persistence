package lazdevs.peristence.mongo.connection.multitenant;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Abhijeet Rai
 */
@RequiredArgsConstructor
public class MongoConnectionProviderFunction implements Function<String, MongoDatabase> {
    private final MongoClient mongoClient;
    private final Map<String, String> tenantIdVsDbNameMap;

    @Override
    public MongoDatabase apply(String tenantId) {
        return mongoClient.getDatabase(tenantIdVsDbNameMap.get(tenantId));
    }
}
