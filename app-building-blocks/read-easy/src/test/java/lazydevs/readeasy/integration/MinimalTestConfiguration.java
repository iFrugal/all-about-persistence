package lazydevs.readeasy.integration;

import lazydevs.persistence.reader.GeneralReader;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.services.basic.validation.ParamValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal test configuration that provides all required beans
 * for integration tests without needing the full application context.
 */
@TestConfiguration
@Import(DynaBeansAutoConfiguration.class)
public class MinimalTestConfiguration {

    @Bean
    @Primary
    public ParamValidator paramValidator() {
        return new ParamValidator();
    }

    /**
     * Mock dynaBeansGenerator bean to satisfy @DependsOn requirement.
     */
    @Bean(name = "dynaBeansGenerator")
    @Primary
    public String dynaBeansGenerator() {
        return "mock-dyna-beans-generator";
    }

    @Bean(name = "readEasyGeneralReaderMap")
    @Primary
    public Map<String, GeneralReader> readEasyGeneralReaderMap() {
        Map<String, GeneralReader> readers = new HashMap<>();
        readers.put("default", new MockReadersTestConfiguration.MockJdbcGeneralReader());
        readers.put("test", new MockReadersTestConfiguration.MockJdbcGeneralReader());
        readers.put("mongodb", new MockReadersTestConfiguration.MockMongoGeneralReader());
        return readers;
    }

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

        // Disable validation for tests
        ReadEasyConfig.ValidationConfig validation = new ReadEasyConfig.ValidationConfig();
        validation.setEnabled(false);
        validation.setFailOnError(false);
        config.setValidation(validation);

        // Disable devtools
        ReadEasyConfig.DevtoolsConfig devtools = new ReadEasyConfig.DevtoolsConfig();
        devtools.setEnabled(false);
        config.setDevtools(devtools);

        return config;
    }
}
