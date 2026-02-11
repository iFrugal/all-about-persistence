package com.example.readeasy.config;

import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.*;

/**
 * Configuration class providing mock GeneralReader implementations.
 *
 * <p>In a real application, you would replace these mocks with actual implementations:</p>
 * <ul>
 *   <li>For JDBC: {@code lazydevs.persistence.impl.jdbc.JdbcGeneralReader}</li>
 *   <li>For MongoDB: {@code lazydevs.persistence.impl.mongodb.MongoGeneralReader}</li>
 * </ul>
 *
 * <h2>Example with real JDBC reader:</h2>
 * <pre>
 * &#64;Bean(name = "readEasyGeneralReaderMap")
 * public Map<String, GeneralReader> readEasyGeneralReaderMap(JdbcTemplate jdbcTemplate) {
 *     Map<String, GeneralReader> readers = new HashMap<>();
 *     readers.put("default", new JdbcGeneralReader(new SimpleJdbcTemplate(jdbcTemplate)));
 *     return readers;
 * }
 * </pre>
 */
@Configuration
public class MockReadersConfig {

    /**
     * Provides DynaBeansConfig for the DynaBeansAutoConfiguration.
     */
    @Bean
    @Primary
    public DynaBeansConfig dynaBeansConfig() {
        return new DynaBeansConfig();
    }

    /**
     * Provides ReadEasyConfig with query file locations and settings.
     */
    @Bean
    @Primary
    public ReadEasyConfig readEasyConfig() {
        ReadEasyConfig config = new ReadEasyConfig();

        // Query files - maps namespace to file locations
        Map<String, List<String>> queryFiles = new HashMap<>();
        queryFiles.put("users", List.of("classpath:queries/users-queries.yaml"));
        queryFiles.put("mongo-users", List.of("classpath:queries/mongodb-users-queries.yaml"));
        config.setQueryFiles(queryFiles);

        // Validation settings
        ReadEasyConfig.ValidationConfig validation = new ReadEasyConfig.ValidationConfig();
        validation.setEnabled(false);
        validation.setFailOnError(false);
        config.setValidation(validation);

        // Devtools settings
        ReadEasyConfig.DevtoolsConfig devtools = new ReadEasyConfig.DevtoolsConfig();
        devtools.setEnabled(false);
        config.setDevtools(devtools);

        // Operation instructions
        Map<ReadEasyConfig.Operation, Map<String, Object>> opInstr = new HashMap<>();
        Map<String, Object> oneInstr = new HashMap<>();
        oneInstr.put("statusCodeWhenNoRecordsFound", 404);
        opInstr.put(ReadEasyConfig.Operation.ONE, oneInstr);
        config.setOperationInstruction(opInstr);

        return config;
    }

    /**
     * Provides the map of GeneralReader implementations.
     *
     * <p>The bean name "readEasyGeneralReaderMap" is important - the
     * ConfiguredReadController uses @Qualifier to find this bean.</p>
     *
     * @return Map of reader ID to GeneralReader implementation
     */
    @Bean(name = "readEasyGeneralReaderMap")
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap() {
        Map<String, GeneralReader> readers = new HashMap<>();
        readers.put("default", new MockJdbcReader());    // JDBC-style reader
        readers.put("mongodb", new MockMongoReader());   // MongoDB-style reader
        return readers;
    }

    // =========================================================================
    // Mock JDBC Reader
    // =========================================================================

    /**
     * Mock implementation of GeneralReader for JDBC-style queries.
     * Replace with JdbcGeneralReader in production.
     */
    @SuppressWarnings("unchecked")
    public static class MockJdbcReader implements GeneralReader<Map<String, Object>, Object> {

        private final List<Map<String, Object>> testData;

        public MockJdbcReader() {
            this.testData = createTestData();
        }

        private static List<Map<String, Object>> createTestData() {
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
        public Page<Map<String, Object>> findPage(Page.PageRequest req, Map<String, Object> query, Map<String, Object> params) {
            int start = (req.getPageNum() - 1) * req.getPageSize();
            int end = Math.min(start + req.getPageSize(), testData.size());
            List<Map<String, Object>> pageData = start < testData.size()
                    ? new ArrayList<>(testData.subList(start, end))
                    : Collections.emptyList();
            return Page.<Map<String, Object>>builder(req)
                    .totalNoOfRecords(testData.size())
                    .data(pageData)
                    .build();
        }

        @Override
        public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Map<String, Object> query, Map<String, Object> params) {
            return new BatchIterator<>(batchSize) {
                private int idx = 0;
                @Override public boolean hasNext() { return idx < testData.size(); }
                @Override public List<Map<String, Object>> next() {
                    int end = Math.min(idx + batchSize, testData.size());
                    List<Map<String, Object>> batch = new ArrayList<>(testData.subList(idx, end));
                    idx = end;
                    return batch;
                }
                @Override public void close() {}
            };
        }

        @Override
        public List<Map<String, Object>> distinct(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(testData);
        }

        @Override
        public Class<Map<String, Object>> getQueryType() {
            return (Class<Map<String, Object>>) (Class<?>) Map.class;
        }
    }

    // =========================================================================
    // Mock MongoDB Reader
    // =========================================================================

    /**
     * Mock implementation of GeneralReader for MongoDB-style queries.
     * Replace with MongoGeneralReader in production.
     */
    @SuppressWarnings("unchecked")
    public static class MockMongoReader implements GeneralReader<Map<String, Object>, Object> {

        private final Map<String, List<Map<String, Object>>> collections;

        public MockMongoReader() {
            this.collections = createCollections();
        }

        private static Map<String, List<Map<String, Object>>> createCollections() {
            Map<String, List<Map<String, Object>>> colls = new HashMap<>();

            // Users collection
            List<Map<String, Object>> users = new ArrayList<>();

            Map<String, Object> user1 = new LinkedHashMap<>();
            user1.put("_id", "507f1f77bcf86cd799439011");
            user1.put("name", "Mongo User 1");
            user1.put("email", "mongo1@example.com");
            user1.put("status", "ACTIVE");
            users.add(user1);

            Map<String, Object> user2 = new LinkedHashMap<>();
            user2.put("_id", "507f1f77bcf86cd799439012");
            user2.put("name", "Mongo User 2");
            user2.put("email", "mongo2@example.com");
            user2.put("status", "ACTIVE");
            users.add(user2);

            colls.put("users", users);

            // Orders collection
            List<Map<String, Object>> orders = new ArrayList<>();

            Map<String, Object> order1 = new LinkedHashMap<>();
            order1.put("_id", "60d5ec9af682fbd12a0b1234");
            order1.put("userId", "507f1f77bcf86cd799439011");
            order1.put("total", 199.99);
            order1.put("status", "COMPLETED");
            orders.add(order1);

            colls.put("orders", orders);

            return colls;
        }

        private String getCollectionName(Map<String, Object> query) {
            if (query != null && query.get("collectionName") != null) {
                return query.get("collectionName").toString();
            }
            return "unknown";
        }

        @Override
        public Map<String, Object> findOne(Map<String, Object> query, Map<String, Object> params) {
            List<Map<String, Object>> coll = collections.getOrDefault(getCollectionName(query), Collections.emptyList());
            return coll.isEmpty() ? null : coll.get(0);
        }

        @Override
        public List<Map<String, Object>> findAll(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(collections.getOrDefault(getCollectionName(query), Collections.emptyList()));
        }

        @Override
        public long count(Map<String, Object> query, Map<String, Object> params) {
            return collections.getOrDefault(getCollectionName(query), Collections.emptyList()).size();
        }

        @Override
        public Page<Map<String, Object>> findPage(Page.PageRequest req, Map<String, Object> query, Map<String, Object> params) {
            List<Map<String, Object>> coll = collections.getOrDefault(getCollectionName(query), Collections.emptyList());
            int start = (req.getPageNum() - 1) * req.getPageSize();
            int end = Math.min(start + req.getPageSize(), coll.size());
            List<Map<String, Object>> pageData = start < coll.size()
                    ? new ArrayList<>(coll.subList(start, end))
                    : Collections.emptyList();
            return Page.<Map<String, Object>>builder(req)
                    .totalNoOfRecords(coll.size())
                    .data(pageData)
                    .build();
        }

        @Override
        public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Map<String, Object> query, Map<String, Object> params) {
            List<Map<String, Object>> coll = collections.getOrDefault(getCollectionName(query), Collections.emptyList());
            return new BatchIterator<>(batchSize) {
                private int idx = 0;
                @Override public boolean hasNext() { return idx < coll.size(); }
                @Override public List<Map<String, Object>> next() {
                    int end = Math.min(idx + batchSize, coll.size());
                    List<Map<String, Object>> batch = new ArrayList<>(coll.subList(idx, end));
                    idx = end;
                    return batch;
                }
                @Override public void close() {}
            };
        }

        @Override
        public List<Map<String, Object>> distinct(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(collections.getOrDefault(getCollectionName(query), Collections.emptyList()));
        }

        @Override
        public Class<Map<String, Object>> getQueryType() {
            return (Class<Map<String, Object>>) (Class<?>) Map.class;
        }
    }
}
