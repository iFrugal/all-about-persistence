package lazdevs.peristence.mongo.writer.general;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lazydevs.persistence.connection.ConnectionProvider;
import lazydevs.persistence.writer.general.GeneralAppender;
import lombok.NonNull;
import org.bson.Document;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * @author Abhijeet Rai
 */
public class MongoGeneralAppender implements GeneralAppender<MongoWriteInstruction> {
    private final String defaultCollectionName;
    private final ConnectionProvider<MongoDatabase> connectionProvider;

    public MongoGeneralAppender(@NonNull ConnectionProvider<MongoDatabase> connectionProvider, @NonNull String defaultCollectionName) {
        this.connectionProvider = connectionProvider;
        this.defaultCollectionName = defaultCollectionName;
    }

    public MongoGeneralAppender(@NonNull MongoDatabase mongoDatabase, @NonNull String defaultCollectionName) {
        this(() -> mongoDatabase, defaultCollectionName);
    }

    public MongoGeneralAppender(@NonNull MongoClient mongoClient, @NonNull String dbName, @NonNull String defaultCollectionName) {
        this(() -> mongoClient.getDatabase(dbName), defaultCollectionName);
    }

    protected MongoCollection<Document> getCollection(MongoWriteInstruction mongoWriteInstruction) {
        return connectionProvider.getConnection().getCollection(null == mongoWriteInstruction || null == mongoWriteInstruction.getCollectionName() ? this.defaultCollectionName : mongoWriteInstruction.getCollectionName());
    }

    @Override
    public Map<String, Object> create(Map<String, Object> t, MongoWriteInstruction writeInstruction) {
        getCollection(writeInstruction).insertOne(new Document(t));
        return t;
    }

    @Override
    public List<Map<String, Object>> create(List<Map<String, Object>> list, MongoWriteInstruction writeInstruction) {
        getCollection(writeInstruction).insertMany(list.stream()
                .map(Document::new)
                .collect(toList()));
        return list;
    }

    @Override
    public Class<MongoWriteInstruction> getWriteInstructionType() {
        return MongoWriteInstruction.class;
    }


}
