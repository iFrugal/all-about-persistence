package lazydevs.readeasy.example.standalone;

import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.controller.ConfiguredReadController;
import lazydevs.services.basic.handler.RESTExceptionHandler;
import lazydevs.services.basic.validation.ParamValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import lazydevs.springhelpers.dynabeans.DynaBeansConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import de.flapdoodle.embed.mongo.spring.autoconfigure.EmbeddedMongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.*;

/**
 * Standalone Spring Boot application for testing Read-Easy APIs.
 * Run this application, test with curl, then stop it.
 * Excludes database auto-configurations since we use mock readers.
 */
@SpringBootApplication(exclude = {
    MongoAutoConfiguration.class,
    MongoDataAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    JpaRepositoriesAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    EmbeddedMongoAutoConfiguration.class
})
@Import({ConfiguredReadController.class, DynaBeansAutoConfiguration.class, RESTExceptionHandler.class})
public class StandaloneTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(StandaloneTestApplication.class, args);
    }

    @Bean
    @Primary
    public ParamValidator paramValidator() {
        return new ParamValidator();
    }

    @Bean
    @Primary
    public DynaBeansConfig dynaBeansConfig() {
        return new DynaBeansConfig();
    }

    @Bean(name = "readEasyGeneralReaderMap")
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap() {
        Map<String, GeneralReader> readers = new HashMap<>();
        readers.put("default", new MockJdbcReader());
        readers.put("mongodb", new MockMongoReader());
        return readers;
    }

    @Bean
    @Primary
    public ReadEasyConfig readEasyConfig() {
        ReadEasyConfig config = new ReadEasyConfig();

        Map<String, List<String>> queryFiles = new HashMap<>();
        queryFiles.put("users", List.of("classpath:queries/valid/users-queries.yaml"));
        queryFiles.put("orders", List.of("classpath:queries/valid/orders-queries.yaml"));
        queryFiles.put("mongo-users", List.of("classpath:queries/valid/mongodb-users-queries.yaml"));
        queryFiles.put("mongo-orders", List.of("classpath:queries/valid/mongodb-orders-queries.yaml"));
        config.setQueryFiles(queryFiles);

        ReadEasyConfig.ValidationConfig validation = new ReadEasyConfig.ValidationConfig();
        validation.setEnabled(false);
        config.setValidation(validation);

        ReadEasyConfig.DevtoolsConfig devtools = new ReadEasyConfig.DevtoolsConfig();
        devtools.setEnabled(false);
        config.setDevtools(devtools);

        Map<ReadEasyConfig.Operation, Map<String, Object>> opInstr = new HashMap<>();
        Map<String, Object> oneInstr = new HashMap<>();
        oneInstr.put("statusCodeWhenNoRecordsFound", 404);
        opInstr.put(ReadEasyConfig.Operation.ONE, oneInstr);
        config.setOperationInstruction(opInstr);

        return config;
    }

    // ========== Mock Readers ==========

    @SuppressWarnings("unchecked")
    public static class MockJdbcReader implements GeneralReader<Map<String, Object>, Object> {
        private final List<Map<String, Object>> data = createTestData();

        private static List<Map<String, Object>> createTestData() {
            List<Map<String, Object>> list = new ArrayList<>();

            Map<String, Object> u1 = new LinkedHashMap<>();
            u1.put("id", 1);
            u1.put("name", "John Doe");
            u1.put("email", "john@example.com");
            u1.put("status", "ACTIVE");
            list.add(u1);

            Map<String, Object> u2 = new LinkedHashMap<>();
            u2.put("id", 2);
            u2.put("name", "Jane Smith");
            u2.put("email", "jane@example.com");
            u2.put("status", "ACTIVE");
            list.add(u2);

            Map<String, Object> u3 = new LinkedHashMap<>();
            u3.put("id", 3);
            u3.put("name", "Bob Wilson");
            u3.put("email", "bob@example.com");
            u3.put("status", "INACTIVE");
            list.add(u3);

            return list;
        }

        @Override
        public Map<String, Object> findOne(Map<String, Object> query, Map<String, Object> params) {
            return data.isEmpty() ? null : data.get(0);
        }

        @Override
        public List<Map<String, Object>> findAll(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(data);
        }

        @Override
        public long count(Map<String, Object> query, Map<String, Object> params) {
            return data.size();
        }

        @Override
        public Page<Map<String, Object>> findPage(Page.PageRequest req, Map<String, Object> query, Map<String, Object> params) {
            int start = (req.getPageNum() - 1) * req.getPageSize();
            int end = Math.min(start + req.getPageSize(), data.size());
            List<Map<String, Object>> pageData = start < data.size() ?
                    new ArrayList<>(data.subList(start, end)) : Collections.emptyList();
            return Page.<Map<String, Object>>builder(req)
                    .totalNoOfRecords(data.size())
                    .data(pageData)
                    .build();
        }

        @Override
        public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Map<String, Object> query, Map<String, Object> params) {
            return new BatchIterator<>(batchSize) {
                private int idx = 0;
                @Override public boolean hasNext() { return idx < data.size(); }
                @Override public List<Map<String, Object>> next() {
                    int end = Math.min(idx + batchSize, data.size());
                    List<Map<String, Object>> batch = new ArrayList<>(data.subList(idx, end));
                    idx = end;
                    return batch;
                }
                @Override public void close() {}
            };
        }

        @Override
        public List<Map<String, Object>> distinct(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(data);
        }

        @Override
        public Class<Map<String, Object>> getQueryType() {
            return (Class<Map<String, Object>>) (Class<?>) Map.class;
        }
    }

    @SuppressWarnings("unchecked")
    public static class MockMongoReader implements GeneralReader<Map<String, Object>, Object> {
        private final Map<String, List<Map<String, Object>>> collections = createCollections();

        private static Map<String, List<Map<String, Object>>> createCollections() {
            Map<String, List<Map<String, Object>>> colls = new HashMap<>();

            List<Map<String, Object>> users = new ArrayList<>();
            Map<String, Object> u1 = new LinkedHashMap<>();
            u1.put("_id", "mongo-001");
            u1.put("name", "Mongo User 1");
            u1.put("email", "mongo1@example.com");
            users.add(u1);

            Map<String, Object> u2 = new LinkedHashMap<>();
            u2.put("_id", "mongo-002");
            u2.put("name", "Mongo User 2");
            u2.put("email", "mongo2@example.com");
            users.add(u2);
            colls.put("users", users);

            List<Map<String, Object>> orders = new ArrayList<>();
            Map<String, Object> o1 = new LinkedHashMap<>();
            o1.put("_id", "order-001");
            o1.put("userId", "mongo-001");
            o1.put("total", 199.99);
            orders.add(o1);
            colls.put("orders", orders);

            return colls;
        }

        private String getCollection(Map<String, Object> query) {
            return query != null && query.get("collectionName") != null ?
                    query.get("collectionName").toString() : "unknown";
        }

        @Override
        public Map<String, Object> findOne(Map<String, Object> query, Map<String, Object> params) {
            List<Map<String, Object>> coll = collections.getOrDefault(getCollection(query), Collections.emptyList());
            return coll.isEmpty() ? null : coll.get(0);
        }

        @Override
        public List<Map<String, Object>> findAll(Map<String, Object> query, Map<String, Object> params) {
            return new ArrayList<>(collections.getOrDefault(getCollection(query), Collections.emptyList()));
        }

        @Override
        public long count(Map<String, Object> query, Map<String, Object> params) {
            return collections.getOrDefault(getCollection(query), Collections.emptyList()).size();
        }

        @Override
        public Page<Map<String, Object>> findPage(Page.PageRequest req, Map<String, Object> query, Map<String, Object> params) {
            List<Map<String, Object>> coll = collections.getOrDefault(getCollection(query), Collections.emptyList());
            int start = (req.getPageNum() - 1) * req.getPageSize();
            int end = Math.min(start + req.getPageSize(), coll.size());
            List<Map<String, Object>> pageData = start < coll.size() ?
                    new ArrayList<>(coll.subList(start, end)) : Collections.emptyList();
            return Page.<Map<String, Object>>builder(req)
                    .totalNoOfRecords(coll.size())
                    .data(pageData)
                    .build();
        }

        @Override
        public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, Map<String, Object> query, Map<String, Object> params) {
            List<Map<String, Object>> coll = collections.getOrDefault(getCollection(query), Collections.emptyList());
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
            return new ArrayList<>(collections.getOrDefault(getCollection(query), Collections.emptyList()));
        }

        @Override
        public Class<Map<String, Object>> getQueryType() {
            return (Class<Map<String, Object>>) (Class<?>) Map.class;
        }
    }
}
