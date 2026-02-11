package lazydevs.readeasy.integration;

import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.services.basic.validation.ParamValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.*;

/**
 * Test configuration providing mock beans for integration tests.
 * Creates mock GeneralReader implementations for both JDBC and MongoDB
 * that return appropriate test data.
 */
@TestConfiguration
@Import(DynaBeansAutoConfiguration.class)
public class MockReadersTestConfiguration {

    /**
     * Provides ParamValidator required by the controller.
     */
    @Bean
    @Primary
    public ParamValidator paramValidator() {
        return new ParamValidator();
    }

    /**
     * Mock dynaBeansGenerator bean to satisfy @DependsOn requirement.
     * This bypasses the actual DynaBeansAutoConfiguration initialization.
     */
    @Bean(name = "dynaBeansGenerator")
    @Primary
    public String dynaBeansGenerator() {
        return "mock-dyna-beans-generator";
    }

    /**
     * Mock GeneralReader map that includes both JDBC and MongoDB readers.
     * This avoids the need for actual database connections in tests.
     * Note: Qualified as "readEasyGeneralReaderMap" to match controller's @Qualifier
     */
    @Bean(name = "readEasyGeneralReaderMap")
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap() {
        Map<String, GeneralReader> readers = new HashMap<>();
        readers.put("default", new MockJdbcGeneralReader());  // JDBC reader
        readers.put("test", new MockJdbcGeneralReader());     // Additional JDBC reader
        readers.put("mongodb", new MockMongoGeneralReader()); // MongoDB reader
        return readers;
    }

    /**
     * Provides ReadEasyConfig with generalReaders configured to use our mock readers.
     * This ensures the controller can initialize properly even when the bean-based
     * reader map is overwritten in getGeneralReader().
     */
    @Bean
    @Primary
    public ReadEasyConfig readEasyConfig() {
        ReadEasyConfig config = new ReadEasyConfig();

        // Set up query files
        Map<String, List<String>> queryFiles = new HashMap<>();
        queryFiles.put("users", List.of("classpath:queries/valid/users-queries.yaml"));
        queryFiles.put("orders", List.of("classpath:queries/valid/orders-queries.yaml"));
        queryFiles.put("mongo-users", List.of("classpath:queries/valid/mongodb-users-queries.yaml"));
        queryFiles.put("mongo-orders", List.of("classpath:queries/valid/mongodb-orders-queries.yaml"));
        config.setQueryFiles(queryFiles);

        // Set up validation
        ReadEasyConfig.ValidationConfig validation = new ReadEasyConfig.ValidationConfig();
        validation.setEnabled(false);  // Disable validation for mock tests
        validation.setFailOnError(false);
        config.setValidation(validation);

        // Set up operation instructions
        Map<ReadEasyConfig.Operation, Map<String, Object>> operationInstruction = new HashMap<>();
        Map<String, Object> oneInstruction = new HashMap<>();
        oneInstruction.put("statusCodeWhenNoRecordsFound", 404);
        operationInstruction.put(ReadEasyConfig.Operation.ONE, oneInstruction);
        config.setOperationInstruction(operationInstruction);

        return config;
    }

    /**
     * Mock implementation of GeneralReader for JDBC-style queries.
     * Expects queries with "nativeSQL" field.
     */
    @SuppressWarnings("unchecked")
    public static class MockJdbcGeneralReader implements GeneralReader<Map<String, Object>, Object> {

        private final List<Map<String, Object>> testData;

        public MockJdbcGeneralReader() {
            this.testData = createJdbcTestData();
        }

        private List<Map<String, Object>> createJdbcTestData() {
            List<Map<String, Object>> data = new ArrayList<>();

            Map<String, Object> user1 = new LinkedHashMap<>();
            user1.put("id", 1);
            user1.put("name", "John Doe");
            user1.put("email", "john.doe@example.com");
            user1.put("status", "ACTIVE");
            user1.put("created_at", "2024-01-15 10:00:00");
            data.add(user1);

            Map<String, Object> user2 = new LinkedHashMap<>();
            user2.put("id", 2);
            user2.put("name", "Jane Smith");
            user2.put("email", "jane.smith@example.com");
            user2.put("status", "ACTIVE");
            user2.put("created_at", "2024-01-16 11:30:00");
            data.add(user2);

            Map<String, Object> user3 = new LinkedHashMap<>();
            user3.put("id", 3);
            user3.put("name", "Bob Wilson");
            user3.put("email", "bob.wilson@example.com");
            user3.put("status", "INACTIVE");
            user3.put("created_at", "2024-01-17 09:15:00");
            data.add(user3);

            return data;
        }

        @Override
        public Map<String, Object> findOne(Map<String, Object> query, Map<String, Object> params) {
            return testData.isEmpty() ? null : testData.get(0);
        }

        @Override
        public List<Map<String, Object>> findAll(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(testData);
        }

        @Override
        public long count(Map<String, Object> query, Map<String, Object> params) {
            return testData.size();
        }

        @Override
        public Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, Map<String, Object> query, Map<String, Object> params) {
            int start = (pageRequest.getPageNum() - 1) * pageRequest.getPageSize();
            int end = Math.min(start + pageRequest.getPageSize(), testData.size());

            List<Map<String, Object>> pageData = start < testData.size() ?
                    new ArrayList<>(testData.subList(start, end)) : Collections.emptyList();

            return Page.<Map<String, Object>>builder(pageRequest)
                    .totalNoOfRecords(testData.size())
                    .data(pageData)
                    .build();
        }

        @Override
        public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Map<String, Object> query, Map<String, Object> params) {
            return new BatchIterator<Map<String, Object>>(batchSize) {
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return currentIndex < testData.size();
                }

                @Override
                public List<Map<String, Object>> next() {
                    int end = Math.min(currentIndex + batchSize, testData.size());
                    List<Map<String, Object>> batch = new ArrayList<>(testData.subList(currentIndex, end));
                    currentIndex = end;
                    return batch;
                }

                @Override
                public void close() {
                    // No resources to close
                }
            };
        }

        @Override
        public List<Map<String, Object>> distinct(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(testData);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<Map<String, Object>> getQueryType() {
            return (Class<Map<String, Object>>) (Class<?>) Map.class;
        }
    }

    /**
     * Mock implementation of GeneralReader for MongoDB-style queries.
     * Expects queries with "collectionName", "operation", "filter" fields.
     */
    @SuppressWarnings("unchecked")
    public static class MockMongoGeneralReader implements GeneralReader<Map<String, Object>, Object> {

        private final Map<String, List<Map<String, Object>>> collections;

        public MockMongoGeneralReader() {
            this.collections = createMongoTestData();
        }

        private Map<String, List<Map<String, Object>>> createMongoTestData() {
            Map<String, List<Map<String, Object>>> data = new HashMap<>();

            // Users collection
            List<Map<String, Object>> users = new ArrayList<>();

            Map<String, Object> user1 = new LinkedHashMap<>();
            user1.put("_id", "507f1f77bcf86cd799439011");
            user1.put("name", "John Doe");
            user1.put("email", "john.doe@example.com");
            user1.put("status", "ACTIVE");
            user1.put("createdAt", "2024-01-15T10:00:00Z");
            users.add(user1);

            Map<String, Object> user2 = new LinkedHashMap<>();
            user2.put("_id", "507f1f77bcf86cd799439012");
            user2.put("name", "Jane Smith");
            user2.put("email", "jane.smith@example.com");
            user2.put("status", "ACTIVE");
            user2.put("createdAt", "2024-01-16T11:30:00Z");
            users.add(user2);

            Map<String, Object> user3 = new LinkedHashMap<>();
            user3.put("_id", "507f1f77bcf86cd799439013");
            user3.put("name", "Bob Wilson");
            user3.put("email", "bob.wilson@example.com");
            user3.put("status", "INACTIVE");
            user3.put("createdAt", "2024-01-17T09:15:00Z");
            users.add(user3);

            data.put("users", users);

            // Orders collection
            List<Map<String, Object>> orders = new ArrayList<>();

            Map<String, Object> order1 = new LinkedHashMap<>();
            order1.put("_id", "60d5ec9af682fbd12a0b1234");
            order1.put("userId", "507f1f77bcf86cd799439011");
            order1.put("totalAmount", 1349.98);
            order1.put("status", "COMPLETED");
            order1.put("createdAt", "2024-02-01T10:30:00Z");
            orders.add(order1);

            Map<String, Object> order2 = new LinkedHashMap<>();
            order2.put("_id", "60d5ec9af682fbd12a0b1235");
            order2.put("userId", "507f1f77bcf86cd799439011");
            order2.put("totalAmount", 79.99);
            order2.put("status", "COMPLETED");
            order2.put("createdAt", "2024-02-05T14:15:00Z");
            orders.add(order2);

            Map<String, Object> order3 = new LinkedHashMap<>();
            order3.put("_id", "60d5ec9af682fbd12a0b1236");
            order3.put("userId", "507f1f77bcf86cd799439012");
            order3.put("totalAmount", 449.98);
            order3.put("status", "SHIPPED");
            order3.put("createdAt", "2024-02-10T09:00:00Z");
            orders.add(order3);

            data.put("orders", orders);

            return data;
        }

        @Override
        public Map<String, Object> findOne(Map<String, Object> query, Map<String, Object> params) {
            String collectionName = getCollectionName(query);
            List<Map<String, Object>> collection = collections.getOrDefault(collectionName, Collections.emptyList());
            return collection.isEmpty() ? null : collection.get(0);
        }

        @Override
        public List<Map<String, Object>> findAll(Map<String, Object> query, Map<String, Object> params) {
            String collectionName = getCollectionName(query);
            return new ArrayList<>(collections.getOrDefault(collectionName, Collections.emptyList()));
        }

        @Override
        public long count(Map<String, Object> query, Map<String, Object> params) {
            String collectionName = getCollectionName(query);
            return collections.getOrDefault(collectionName, Collections.emptyList()).size();
        }

        @Override
        public Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, Map<String, Object> query, Map<String, Object> params) {
            String collectionName = getCollectionName(query);
            List<Map<String, Object>> collection = collections.getOrDefault(collectionName, Collections.emptyList());

            int start = (pageRequest.getPageNum() - 1) * pageRequest.getPageSize();
            int end = Math.min(start + pageRequest.getPageSize(), collection.size());

            List<Map<String, Object>> pageData = start < collection.size() ?
                    new ArrayList<>(collection.subList(start, end)) : Collections.emptyList();

            return Page.<Map<String, Object>>builder(pageRequest)
                    .totalNoOfRecords(collection.size())
                    .data(pageData)
                    .build();
        }

        @Override
        public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Map<String, Object> query, Map<String, Object> params) {
            String collectionName = getCollectionName(query);
            List<Map<String, Object>> collection = collections.getOrDefault(collectionName, Collections.emptyList());

            return new BatchIterator<Map<String, Object>>(batchSize) {
                private int currentIndex = 0;

                @Override
                public boolean hasNext() {
                    return currentIndex < collection.size();
                }

                @Override
                public List<Map<String, Object>> next() {
                    int end = Math.min(currentIndex + batchSize, collection.size());
                    List<Map<String, Object>> batch = new ArrayList<>(collection.subList(currentIndex, end));
                    currentIndex = end;
                    return batch;
                }

                @Override
                public void close() {
                    // No resources to close
                }
            };
        }

        @Override
        public List<Map<String, Object>> distinct(Map<String, Object> query, Map<String, Object> params) {
            String collectionName = getCollectionName(query);
            return new ArrayList<>(collections.getOrDefault(collectionName, Collections.emptyList()));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Class<Map<String, Object>> getQueryType() {
            return (Class<Map<String, Object>>) (Class<?>) Map.class;
        }

        private String getCollectionName(Map<String, Object> query) {
            if (query != null) {
                Object collectionName = query.get("collectionName");
                if (collectionName != null) {
                    return collectionName.toString();
                }
            }
            return "unknown";
        }
    }
}
