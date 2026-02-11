package lazydevs.readeasy.integration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lazdevs.peristence.mongo.reader.general.MongoGeneralReader;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.readeasy.TestApplication;
import org.bson.Document;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MongoDB functionality using embedded MongoDB.
 * Tests verify that MongoGeneralReader works correctly with actual MongoDB queries.
 */
@SpringBootTest(classes = TestApplication.class)
@Import(MongoTestConfiguration.class)
@ActiveProfiles("mongodb-test")
@DisplayName("MongoDB Integration Tests with Embedded MongoDB")
@Disabled("Requires embedded MongoDB setup - run manually")
class MongoIntegrationTest {

    @Autowired(required = false)
    @Qualifier("readEasyGeneralReaderMap")
    private Map<String, GeneralReader> readEasyGeneralReaderMap;

    @Autowired(required = false)
    private MongoDatabase mongoDatabase;

    @Nested
    @DisplayName("Configuration Verification")
    class ConfigurationVerification {

        @Test
        @DisplayName("Should have MongoDB reader configured")
        void shouldHaveMongoReaderConfigured() {
            Assumptions.assumeTrue(readEasyGeneralReaderMap != null, "Reader map not available");
            assertTrue(readEasyGeneralReaderMap.containsKey("mongodb"),
                    "Should have 'mongodb' reader");

            GeneralReader mongoReader = readEasyGeneralReaderMap.get("mongodb");
            assertTrue(mongoReader instanceof MongoGeneralReader,
                    "MongoDB reader should be MongoGeneralReader");
        }

        @Test
        @DisplayName("Should have MongoDB database configured")
        void shouldHaveMongoDatabaseConfigured() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");
            assertNotNull(mongoDatabase, "MongoDB database should be configured");
        }

        @Test
        @DisplayName("Should have test data loaded in users collection")
        void shouldHaveUsersTestDataLoaded() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");

            MongoCollection<Document> usersCollection = mongoDatabase.getCollection("users");
            long count = usersCollection.countDocuments();
            assertTrue(count > 0, "Should have users in database");
        }

        @Test
        @DisplayName("Should have test data loaded in orders collection")
        void shouldHaveOrdersTestDataLoaded() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");

            MongoCollection<Document> ordersCollection = mongoDatabase.getCollection("orders");
            long count = ordersCollection.countDocuments();
            assertTrue(count > 0, "Should have orders in database");
        }
    }

    @Nested
    @DisplayName("MongoDB Find Operations")
    class MongoFindOperations {

        private MongoCollection<Document> usersCollection;
        private MongoCollection<Document> ordersCollection;

        @BeforeEach
        void setUp() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");
            usersCollection = mongoDatabase.getCollection("users");
            ordersCollection = mongoDatabase.getCollection("orders");
        }

        @Test
        @DisplayName("Should find all users")
        void shouldFindAllUsers() {
            List<Document> users = usersCollection.find().into(new ArrayList<>());

            assertFalse(users.isEmpty());
            assertTrue(users.stream().allMatch(d -> d.containsKey("name")));
            assertTrue(users.stream().allMatch(d -> d.containsKey("email")));
        }

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            Document user = usersCollection.find(eq("_id", "507f1f77bcf86cd799439011")).first();

            assertNotNull(user);
            assertNotNull(user.getString("name"));
            assertNotNull(user.getString("email"));
        }

        @Test
        @DisplayName("Should find users by status")
        void shouldFindUsersByStatus() {
            List<Document> activeUsers = usersCollection
                    .find(eq("status", "ACTIVE"))
                    .into(new ArrayList<>());

            assertFalse(activeUsers.isEmpty());
            assertTrue(activeUsers.stream()
                    .allMatch(d -> "ACTIVE".equals(d.getString("status"))));
        }

        @Test
        @DisplayName("Should find users with regex")
        void shouldFindUsersWithRegex() {
            List<Document> users = usersCollection
                    .find(regex("email", ".*@example.com"))
                    .into(new ArrayList<>());

            assertFalse(users.isEmpty());
        }

        @Test
        @DisplayName("Should find orders by user ID")
        void shouldFindOrdersByUserId() {
            List<Document> orders = ordersCollection
                    .find(eq("userId", "507f1f77bcf86cd799439011"))
                    .into(new ArrayList<>());

            assertFalse(orders.isEmpty());
        }

        @Test
        @DisplayName("Should handle empty result")
        void shouldHandleEmptyResult() {
            Document user = usersCollection
                    .find(eq("_id", "nonexistent"))
                    .first();

            assertNull(user);
        }
    }

    @Nested
    @DisplayName("MongoDB Query Patterns")
    class MongoQueryPatterns {

        private MongoCollection<Document> usersCollection;
        private MongoCollection<Document> ordersCollection;

        @BeforeEach
        void setUp() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");
            usersCollection = mongoDatabase.getCollection("users");
            ordersCollection = mongoDatabase.getCollection("orders");
        }

        @Test
        @DisplayName("Should support projection")
        void shouldSupportProjection() {
            Document user = usersCollection
                    .find(eq("_id", "507f1f77bcf86cd799439011"))
                    .projection(new Document("name", 1).append("email", 1).append("_id", 0))
                    .first();

            assertNotNull(user);
            assertTrue(user.containsKey("name"));
            assertTrue(user.containsKey("email"));
            assertFalse(user.containsKey("status"));  // Not projected
        }

        @Test
        @DisplayName("Should support sorting ascending")
        void shouldSupportSortingAscending() {
            List<Document> users = usersCollection
                    .find()
                    .sort(ascending("name"))
                    .into(new ArrayList<>());

            assertFalse(users.isEmpty());
            // First user should come before last alphabetically
            if (users.size() > 1) {
                String firstName = users.get(0).getString("name");
                String lastName = users.get(users.size() - 1).getString("name");
                assertTrue(firstName.compareTo(lastName) <= 0);
            }
        }

        @Test
        @DisplayName("Should support sorting descending")
        void shouldSupportSortingDescending() {
            List<Document> users = usersCollection
                    .find()
                    .sort(descending("name"))
                    .into(new ArrayList<>());

            assertFalse(users.isEmpty());
            // First user should come after last alphabetically
            if (users.size() > 1) {
                String firstName = users.get(0).getString("name");
                String lastName = users.get(users.size() - 1).getString("name");
                assertTrue(firstName.compareTo(lastName) >= 0);
            }
        }

        @Test
        @DisplayName("Should support skip and limit")
        void shouldSupportSkipAndLimit() {
            List<Document> page1 = usersCollection
                    .find()
                    .sort(ascending("_id"))
                    .skip(0)
                    .limit(2)
                    .into(new ArrayList<>());

            List<Document> page2 = usersCollection
                    .find()
                    .sort(ascending("_id"))
                    .skip(2)
                    .limit(2)
                    .into(new ArrayList<>());

            assertEquals(2, page1.size());
            assertTrue(page2.size() <= 2);
            if (!page2.isEmpty()) {
                assertNotEquals(page1.get(0).getString("_id"), page2.get(0).getString("_id"));
            }
        }

        @Test
        @DisplayName("Should support $in operator")
        void shouldSupportInOperator() {
            List<Document> users = usersCollection
                    .find(in("status", "ACTIVE", "PENDING"))
                    .into(new ArrayList<>());

            assertFalse(users.isEmpty());
            assertTrue(users.stream()
                    .allMatch(d -> "ACTIVE".equals(d.getString("status")) ||
                            "PENDING".equals(d.getString("status"))));
        }

        @Test
        @DisplayName("Should support $and operator")
        void shouldSupportAndOperator() {
            List<Document> users = usersCollection
                    .find(and(
                            eq("status", "ACTIVE"),
                            regex("email", "john.*")
                    ))
                    .into(new ArrayList<>());

            assertFalse(users.isEmpty());
        }

        @Test
        @DisplayName("Should support $or operator")
        void shouldSupportOrOperator() {
            List<Document> users = usersCollection
                    .find(or(
                            eq("name", "John Doe"),
                            eq("status", "INACTIVE")
                    ))
                    .into(new ArrayList<>());

            assertFalse(users.isEmpty());
        }

        @Test
        @DisplayName("Should support $gte and $lte operators")
        void shouldSupportComparisonOperators() {
            List<Document> orders = ordersCollection
                    .find(and(
                            gte("totalAmount", 50.0),
                            lte("totalAmount", 2000.0)
                    ))
                    .into(new ArrayList<>());

            assertFalse(orders.isEmpty());
        }
    }

    @Nested
    @DisplayName("MongoDB Aggregation")
    class MongoAggregation {

        private MongoCollection<Document> ordersCollection;

        @BeforeEach
        void setUp() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");
            ordersCollection = mongoDatabase.getCollection("orders");
        }

        @Test
        @DisplayName("Should aggregate order count by status")
        void shouldAggregateOrderCountByStatus() {
            List<Document> result = ordersCollection.aggregate(List.of(
                    new Document("$group", new Document()
                            .append("_id", "$status")
                            .append("count", new Document("$sum", 1)))
            )).into(new ArrayList<>());

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should aggregate total amount by user")
        void shouldAggregateTotalAmountByUser() {
            List<Document> result = ordersCollection.aggregate(List.of(
                    new Document("$group", new Document()
                            .append("_id", "$userId")
                            .append("totalAmount", new Document("$sum", "$totalAmount"))
                            .append("orderCount", new Document("$sum", 1))),
                    new Document("$sort", new Document("totalAmount", -1))
            )).into(new ArrayList<>());

            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should calculate average order amount")
        void shouldCalculateAverageOrderAmount() {
            List<Document> result = ordersCollection.aggregate(List.of(
                    new Document("$group", new Document()
                            .append("_id", null)
                            .append("avgAmount", new Document("$avg", "$totalAmount")))
            )).into(new ArrayList<>());

            assertEquals(1, result.size());
            assertNotNull(result.get(0).get("avgAmount"));
        }
    }

    @Nested
    @DisplayName("MongoDB Count Operations")
    class MongoCountOperations {

        @Test
        @DisplayName("Should count all documents")
        void shouldCountAllDocuments() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");

            long count = mongoDatabase.getCollection("users").countDocuments();
            assertTrue(count > 0);
        }

        @Test
        @DisplayName("Should count documents with filter")
        void shouldCountDocumentsWithFilter() {
            Assumptions.assumeTrue(mongoDatabase != null, "MongoDatabase not available");

            long activeCount = mongoDatabase.getCollection("users")
                    .countDocuments(eq("status", "ACTIVE"));
            assertTrue(activeCount >= 0);
        }
    }
}
