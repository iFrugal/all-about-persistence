package lazydevs.readeasy.integration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.flapdoodle.embed.mongo.commands.ServerAddress;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import lazdevs.peristence.mongo.reader.general.MongoGeneralReader;
import lazydevs.persistence.reader.GeneralReader;
import org.bson.Document;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for MongoDB integration tests using embedded MongoDB.
 * This configuration creates actual MongoGeneralReader instances connected to embedded MongoDB.
 */
@TestConfiguration
@Profile("mongodb-test")
public class MongoTestConfiguration {

    private TransitionWalker.ReachedState<RunningMongodProcess> runningMongod;
    private MongoClient mongoClient;
    private static final String DATABASE_NAME = "testdb";

    @PostConstruct
    public void setup() {
        // Start embedded MongoDB using Flapdoodle 4.x API
        runningMongod = Mongod.instance().start(Version.Main.V6_0);

        // Get the server address
        ServerAddress serverAddress = runningMongod.current().getServerAddress();
        String connectionString = String.format("mongodb://%s:%d",
                serverAddress.getHost(), serverAddress.getPort());

        // Create MongoClient
        mongoClient = MongoClients.create(connectionString);

        // Initialize test data
        initTestData();
    }

    @PreDestroy
    public void cleanup() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (runningMongod != null) {
            runningMongod.close();
        }
    }

    /**
     * Initialize MongoDB with test data.
     */
    private void initTestData() {
        MongoDatabase database = mongoClient.getDatabase(DATABASE_NAME);

        // Create users collection with test data
        MongoCollection<Document> usersCollection = database.getCollection("users");
        usersCollection.drop(); // Clean slate
        usersCollection.insertMany(Arrays.asList(
                new Document()
                        .append("_id", "507f1f77bcf86cd799439011")
                        .append("name", "John Doe")
                        .append("email", "john.doe@example.com")
                        .append("status", "ACTIVE")
                        .append("createdAt", "2024-01-15T10:00:00Z"),
                new Document()
                        .append("_id", "507f1f77bcf86cd799439012")
                        .append("name", "Jane Smith")
                        .append("email", "jane.smith@example.com")
                        .append("status", "ACTIVE")
                        .append("createdAt", "2024-01-16T11:30:00Z"),
                new Document()
                        .append("_id", "507f1f77bcf86cd799439013")
                        .append("name", "Bob Wilson")
                        .append("email", "bob.wilson@example.com")
                        .append("status", "INACTIVE")
                        .append("createdAt", "2024-01-17T09:15:00Z")
        ));

        // Create orders collection with test data
        MongoCollection<Document> ordersCollection = database.getCollection("orders");
        ordersCollection.drop();
        ordersCollection.insertMany(Arrays.asList(
                new Document()
                        .append("_id", "60d5ec9af682fbd12a0b1234")
                        .append("userId", "507f1f77bcf86cd799439011")
                        .append("totalAmount", 1349.98)
                        .append("status", "COMPLETED")
                        .append("createdAt", "2024-02-01T10:30:00Z"),
                new Document()
                        .append("_id", "60d5ec9af682fbd12a0b1235")
                        .append("userId", "507f1f77bcf86cd799439011")
                        .append("totalAmount", 79.99)
                        .append("status", "COMPLETED")
                        .append("createdAt", "2024-02-05T14:15:00Z"),
                new Document()
                        .append("_id", "60d5ec9af682fbd12a0b1236")
                        .append("userId", "507f1f77bcf86cd799439012")
                        .append("totalAmount", 449.98)
                        .append("status", "SHIPPED")
                        .append("createdAt", "2024-02-10T09:00:00Z")
        ));

        // Create products collection
        MongoCollection<Document> productsCollection = database.getCollection("products");
        productsCollection.drop();
        productsCollection.insertMany(Arrays.asList(
                new Document()
                        .append("_id", "prod001")
                        .append("name", "Laptop Pro")
                        .append("price", 1299.99)
                        .append("category", "Electronics")
                        .append("stock", 50),
                new Document()
                        .append("_id", "prod002")
                        .append("name", "Wireless Mouse")
                        .append("price", 49.99)
                        .append("category", "Electronics")
                        .append("stock", 200)
        ));
    }

    @Bean
    public MongoClient mongoClient() {
        return mongoClient;
    }

    @Bean
    public MongoDatabase mongoDatabase() {
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    /**
     * Creates GeneralReader map with actual MongoGeneralReader connected to embedded MongoDB.
     */
    @Bean
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap() {
        Map<String, GeneralReader> readers = new HashMap<>();

        // Create MongoGeneralReader with embedded MongoDB
        MongoGeneralReader mongoReader = new MongoGeneralReader(mongoClient, DATABASE_NAME);
        readers.put("mongodb", mongoReader);

        return readers;
    }
}
