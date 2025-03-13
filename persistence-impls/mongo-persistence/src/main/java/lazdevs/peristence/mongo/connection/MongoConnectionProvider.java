package lazdevs.peristence.mongo.connection;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lazydevs.persistence.connection.ConnectionProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Abhijeet Rai
 */

@RequiredArgsConstructor
public class MongoConnectionProvider implements ConnectionProvider<MongoDatabase> {

    private final MongoDatabase mongoDatabase;

    public MongoConnectionProvider(@NonNull MongoClient mongoClient, @NonNull String dbName) {
        this.mongoDatabase = mongoClient.getDatabase(dbName);
    }

    @Override
    public MongoDatabase getConnection() {
        return mongoDatabase;
    }
}
