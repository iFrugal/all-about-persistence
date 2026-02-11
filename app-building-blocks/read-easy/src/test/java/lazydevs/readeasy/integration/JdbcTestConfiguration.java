package lazydevs.readeasy.integration;

import lazydevs.persistence.jdbc.general.JdbcGeneralReader;
import lazydevs.persistence.reader.GeneralReader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for JDBC integration tests using H2 in-memory database.
 * This configuration creates actual JdbcGeneralReader instances connected to H2.
 */
@TestConfiguration
@Profile("jdbc-test")
public class JdbcTestConfiguration {

    /**
     * Creates H2 in-memory DataSource with test schema and data.
     */
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

    /**
     * JdbcTemplate for verifying test data.
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * Creates GeneralReader map with actual JdbcGeneralReader connected to H2.
     */
    @Bean
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap(DataSource dataSource) {
        Map<String, GeneralReader> readers = new HashMap<>();

        // Create JdbcGeneralReader with H2 DataSource
        JdbcGeneralReader jdbcReader = new JdbcGeneralReader(dataSource);
        readers.put("default", jdbcReader);
        readers.put("jdbc", jdbcReader);

        return readers;
    }
}
