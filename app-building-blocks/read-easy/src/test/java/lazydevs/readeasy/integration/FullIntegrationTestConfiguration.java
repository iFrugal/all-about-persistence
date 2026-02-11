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
import lazydevs.persistence.jdbc.general.JdbcGeneralReader;
import lazydevs.persistence.reader.GeneralReader;
import org.bson.Document;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Full integration test configuration with both H2 (JDBC) and embedded MongoDB.
 * Use this configuration when tests need to verify behavior with both database types.
 */
@TestConfiguration
@Profile("full-integration-test")
public class FullIntegrationTestConfiguration {

    private TransitionWalker.ReachedState<RunningMongodProcess> runningMongod;
    private MongoClient mongoClient;
    private static final String MONGO_DATABASE_NAME = "testdb";

    // ==================== MongoDB Setup ====================

    @PostConstruct
    public void setupMongo() {
        // Start embedded MongoDB using Flapdoodle 4.x API
        runningMongod = Mongod.instance().start(Version.Main.V6_0);

        // Get the server address
        ServerAddress serverAddress = runningMongod.current().getServerAddress();
        String connectionString = String.format("mongodb://%s:%d",
                serverAddress.getHost(), serverAddress.getPort());

        // Create MongoClient
        mongoClient = MongoClients.create(connectionString);

        initMongoTestData();
    }

    @PreDestroy
    public void cleanupMongo() {
        if (mongoClient != null) {
            mongoClient.close();
        }
        if (runningMongod != null) {
            runningMongod.close();
        }
    }

    private void initMongoTestData() {
        MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE_NAME);

        // Users collection
        MongoCollection<Document> usersCollection = database.getCollection("users");
        usersCollection.drop();
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

        // Orders collection
        MongoCollection<Document> ordersCollection = database.getCollection("orders");
        ordersCollection.drop();
        ordersCollection.insertMany(Arrays.asList(
                new Document()
                        .append("_id", "60d5ec9af682fbd12a0b1234")
                        .append("userId", "507f1f77bcf86cd799439011")
                        .append("totalAmount", 1349.98)
                        .append("status", "COMPLETED"),
                new Document()
                        .append("_id", "60d5ec9af682fbd12a0b1235")
                        .append("userId", "507f1f77bcf86cd799439011")
                        .append("totalAmount", 79.99)
                        .append("status", "COMPLETED"),
                new Document()
                        .append("_id", "60d5ec9af682fbd12a0b1236")
                        .append("userId", "507f1f77bcf86cd799439012")
                        .append("totalAmount", 449.98)
                        .append("status", "SHIPPED")
        ));
    }

    // ==================== H2 Setup ====================

    @Bean
    @Primary
    public DataSource testDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb;DB_CLOSE_DELAY=-1;MODE=MySQL")
                .addScript("classpath:schema.sql")
                .addScript("classpath:data.sql")
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public MongoClient mongoClient() {
        return mongoClient;
    }

    @Bean
    public MongoDatabase mongoDatabase() {
        return mongoClient.getDatabase(MONGO_DATABASE_NAME);
    }

    // ==================== GeneralReader Map with Both Readers ====================

    /**
     * Creates GeneralReader map with both JDBC and MongoDB readers.
     * - "default" and "jdbc" -> JdbcGeneralReader (H2)
     * - "mongodb" -> MongoGeneralReader (embedded MongoDB)
     */
    @Bean
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap(DataSource dataSource) {
        Map<String, GeneralReader> readers = new HashMap<>();

        // JDBC readers
        JdbcGeneralReader jdbcReader = new JdbcGeneralReader(dataSource);
        readers.put("default", jdbcReader);
        readers.put("jdbc", jdbcReader);

        // MongoDB reader
        MongoGeneralReader mongoReader = new MongoGeneralReader(mongoClient, MONGO_DATABASE_NAME);
        readers.put("mongodb", mongoReader);

        return readers;
    }
}
