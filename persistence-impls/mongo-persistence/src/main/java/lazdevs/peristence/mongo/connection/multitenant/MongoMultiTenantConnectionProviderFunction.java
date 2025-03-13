package lazdevs.peristence.mongo.connection.multitenant;

import com.mongodb.client.MongoDatabase;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;

/**
 * @author Abhijeet Rai
 */
@RequiredArgsConstructor
public class MongoMultiTenantConnectionProviderFunction implements Function<String, MongoDatabase> {
    private final Map<String, MongoDatabase> tenantIdVsDbMap;

    @Override
    public MongoDatabase apply(String tenantId) {
        return tenantIdVsDbMap.get(tenantId);
    }
}
