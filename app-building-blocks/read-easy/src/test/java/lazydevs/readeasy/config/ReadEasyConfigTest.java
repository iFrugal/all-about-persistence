package lazydevs.readeasy.config;

import lazydevs.mapper.utils.SerDe;
import lazydevs.mapper.utils.reflection.InitDTO;
import lazydevs.readeasy.config.ReadEasyConfig.*;
import lazydevs.services.basic.validation.Param;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ReadEasyConfig and its inner classes.
 * Tests configuration defaults, setters/getters, and nested config classes.
 */
@DisplayName("ReadEasyConfig Tests")
class ReadEasyConfigTest {

    @Nested
    @DisplayName("ValidationConfig")
    class ValidationConfigTests {

        @Test
        @DisplayName("Should have correct defaults")
        void shouldHaveCorrectDefaults() {
            ValidationConfig config = new ValidationConfig();

            assertTrue(config.isEnabled(),
                    "Validation should be enabled by default");
            assertTrue(config.isFailOnError(),
                    "FailOnError should be true by default");
            assertTrue(config.isValidateTemplates(),
                    "ValidateTemplates should be true by default");
        }

        @Test
        @DisplayName("Should allow disabling validation")
        void shouldAllowDisablingValidation() {
            ValidationConfig config = new ValidationConfig();
            config.setEnabled(false);

            assertFalse(config.isEnabled());
        }

        @Test
        @DisplayName("Should allow setting failOnError to false")
        void shouldAllowSettingFailOnErrorToFalse() {
            ValidationConfig config = new ValidationConfig();
            config.setFailOnError(false);

            assertFalse(config.isFailOnError());
        }

        @Test
        @DisplayName("Should allow disabling template validation")
        void shouldAllowDisablingTemplateValidation() {
            ValidationConfig config = new ValidationConfig();
            config.setValidateTemplates(false);

            assertFalse(config.isValidateTemplates());
        }

        @Test
        @DisplayName("Should have toString method")
        void shouldHaveToString() {
            ValidationConfig config = new ValidationConfig();
            String str = config.toString();

            assertNotNull(str);
            assertTrue(str.contains("enabled"));
            assertTrue(str.contains("failOnError"));
            assertTrue(str.contains("validateTemplates"));
        }
    }

    @Nested
    @DisplayName("DevtoolsConfig")
    class DevtoolsConfigTests {

        @Test
        @DisplayName("Should have correct defaults")
        void shouldHaveCorrectDefaults() {
            DevtoolsConfig config = new DevtoolsConfig();

            assertFalse(config.isEnabled(),
                    "Devtools should be disabled by default");
            assertEquals(2000L, config.getWatchIntervalMs(),
                    "Watch interval should be 2000ms by default");
            assertTrue(config.isValidateOnReload(),
                    "ValidateOnReload should be true by default");
        }

        @Test
        @DisplayName("Should allow enabling devtools")
        void shouldAllowEnablingDevtools() {
            DevtoolsConfig config = new DevtoolsConfig();
            config.setEnabled(true);

            assertTrue(config.isEnabled());
        }

        @Test
        @DisplayName("Should allow setting custom watch interval")
        void shouldAllowSettingCustomWatchInterval() {
            DevtoolsConfig config = new DevtoolsConfig();
            config.setWatchIntervalMs(5000L);

            assertEquals(5000L, config.getWatchIntervalMs());
        }

        @Test
        @DisplayName("Should allow disabling validation on reload")
        void shouldAllowDisablingValidationOnReload() {
            DevtoolsConfig config = new DevtoolsConfig();
            config.setValidateOnReload(false);

            assertFalse(config.isValidateOnReload());
        }

        @Test
        @DisplayName("Should have toString method")
        void shouldHaveToString() {
            DevtoolsConfig config = new DevtoolsConfig();
            String str = config.toString();

            assertNotNull(str);
            assertTrue(str.contains("enabled"));
            assertTrue(str.contains("watchIntervalMs"));
            assertTrue(str.contains("validateOnReload"));
        }
    }

    @Nested
    @DisplayName("Query Configuration")
    class QueryConfigTests {

        @Test
        @DisplayName("Should have correct defaults")
        void shouldHaveCorrectDefaults() {
            Query query = new Query();

            assertEquals("default", query.getReaderId(),
                    "ReaderId should default to 'default'");
            assertEquals(SerDe.JSON, query.getRawFormat(),
                    "RawFormat should default to JSON");
            assertNull(query.getRaw(),
                    "Raw should be null by default");
            assertNotNull(query.getParams(),
                    "Params should not be null");
            assertTrue(query.getParams().isEmpty(),
                    "Params should be empty by default");
            assertNull(query.getCacheFetchInstruction(),
                    "CacheFetchInstruction should be null by default");
            assertNull(query.getRowTransformer(),
                    "RowTransformer should be null by default");
            assertNotNull(query.getOperationInstruction(),
                    "OperationInstruction should not be null");
            assertTrue(query.getOperationInstruction().isEmpty(),
                    "OperationInstruction should be empty by default");
        }

        @Test
        @DisplayName("Should allow setting readerId")
        void shouldAllowSettingReaderId() {
            Query query = new Query();
            query.setReaderId("mongodb");

            assertEquals("mongodb", query.getReaderId());
        }

        @Test
        @DisplayName("Should allow setting rawFormat")
        void shouldAllowSettingRawFormat() {
            Query query = new Query();
            query.setRawFormat(SerDe.YAML);

            assertEquals(SerDe.YAML, query.getRawFormat());
        }

        @Test
        @DisplayName("Should allow setting raw template")
        void shouldAllowSettingRawTemplate() {
            Query query = new Query();
            String template = "{\"nativeSQL\": \"SELECT * FROM users\"}";
            query.setRaw(template);

            assertEquals(template, query.getRaw());
        }

        @Test
        @DisplayName("Should allow setting params")
        void shouldAllowSettingParams() {
            Query query = new Query();
            Map<String, Param> params = new HashMap<>();
            Param param = new Param();
            params.put("id", param);
            query.setParams(params);

            assertEquals(1, query.getParams().size());
            assertNotNull(query.getParams().get("id"));
        }

        @Test
        @DisplayName("Should allow setting operation instructions")
        void shouldAllowSettingOperationInstructions() {
            Query query = new Query();
            Map<Operation, Map<String, Object>> instructions = new HashMap<>();
            Map<String, Object> oneInstruction = new HashMap<>();
            oneInstruction.put("statusCodeWhenNoRecordsFound", 200);
            instructions.put(Operation.ONE, oneInstruction);
            query.setOperationInstruction(instructions);

            assertEquals(200, query.getOperationInstruction()
                    .get(Operation.ONE)
                    .get("statusCodeWhenNoRecordsFound"));
        }

        @Test
        @DisplayName("Should have toString method")
        void shouldHaveToString() {
            Query query = new Query();
            query.setReaderId("test");
            query.setRaw("SELECT 1");
            String str = query.toString();

            assertNotNull(str);
            assertTrue(str.contains("readerId"));
            assertTrue(str.contains("raw"));
        }
    }

    @Nested
    @DisplayName("CacheFetchInstruction")
    class CacheFetchInstructionTests {

        @Test
        @DisplayName("Should have correct defaults")
        void shouldHaveCorrectDefaults() {
            CacheFetchInstruction instruction = new CacheFetchInstruction();

            assertNull(instruction.getJsFunctionName());
            assertNotNull(instruction.getArgs());
            assertTrue(instruction.getArgs().isEmpty());
        }

        @Test
        @DisplayName("Should allow setting function name")
        void shouldAllowSettingFunctionName() {
            CacheFetchInstruction instruction = new CacheFetchInstruction();
            instruction.setJsFunctionName("getCachedData");

            assertEquals("getCachedData", instruction.getJsFunctionName());
        }

        @Test
        @DisplayName("Should allow setting args")
        void shouldAllowSettingArgs() {
            CacheFetchInstruction instruction = new CacheFetchInstruction();
            instruction.setArgs(Arrays.asList("category", "status"));

            assertEquals(2, instruction.getArgs().size());
            assertEquals("category", instruction.getArgs().get(0));
            assertEquals("status", instruction.getArgs().get(1));
        }

        @Test
        @DisplayName("Should have toString method")
        void shouldHaveToString() {
            CacheFetchInstruction instruction = new CacheFetchInstruction();
            instruction.setJsFunctionName("myFunction");
            String str = instruction.toString();

            assertNotNull(str);
            assertTrue(str.contains("jsFunctionName"));
        }
    }

    @Nested
    @DisplayName("QueryWithDynaBeans")
    class QueryWithDynaBeansTests {

        @Test
        @DisplayName("Should allow setting queries")
        void shouldAllowSettingQueries() {
            QueryWithDynaBeans container = new QueryWithDynaBeans();
            Map<String, Query> queries = new HashMap<>();
            queries.put("findById", new Query());
            queries.put("findAll", new Query());
            container.setQueries(queries);

            assertEquals(2, container.getQueries().size());
            assertNotNull(container.getQueries().get("findById"));
            assertNotNull(container.getQueries().get("findAll"));
        }

        @Test
        @DisplayName("Should allow null queries")
        void shouldAllowNullQueries() {
            QueryWithDynaBeans container = new QueryWithDynaBeans();
            container.setQueries(null);

            assertNull(container.getQueries());
        }

        @Test
        @DisplayName("Should allow null dynaBeans")
        void shouldAllowNullDynaBeans() {
            QueryWithDynaBeans container = new QueryWithDynaBeans();
            container.setDynaBeans(null);

            assertNull(container.getDynaBeans());
        }
    }

    @Nested
    @DisplayName("Operation Enum")
    class OperationEnumTests {

        @Test
        @DisplayName("Should have all expected operations")
        void shouldHaveAllExpectedOperations() {
            Operation[] operations = Operation.values();

            assertEquals(5, operations.length);
            assertTrue(Arrays.asList(operations).contains(Operation.ONE));
            assertTrue(Arrays.asList(operations).contains(Operation.LIST));
            assertTrue(Arrays.asList(operations).contains(Operation.PAGE));
            assertTrue(Arrays.asList(operations).contains(Operation.COUNT));
            assertTrue(Arrays.asList(operations).contains(Operation.EXPORT));
        }

        @Test
        @DisplayName("Should convert from string")
        void shouldConvertFromString() {
            assertEquals(Operation.ONE, Operation.valueOf("ONE"));
            assertEquals(Operation.LIST, Operation.valueOf("LIST"));
            assertEquals(Operation.PAGE, Operation.valueOf("PAGE"));
            assertEquals(Operation.COUNT, Operation.valueOf("COUNT"));
            assertEquals(Operation.EXPORT, Operation.valueOf("EXPORT"));
        }
    }

    @Nested
    @DisplayName("ReadEasyConfig Root")
    class ReadEasyConfigRootTests {

        @Test
        @DisplayName("Should have correct defaults")
        void shouldHaveCorrectDefaults() {
            ReadEasyConfig config = new ReadEasyConfig();

            assertNull(config.getGeneralReaderInit());
            assertNull(config.getGeneralReaders());
            assertNull(config.getRequestContextSupplierInit());
            assertNull(config.getGlobalContextSupplierInit());

            assertNotNull(config.getQueryFiles());
            assertTrue(config.getQueryFiles().isEmpty());

            assertNotNull(config.getOperationInstruction());
            assertTrue(config.getOperationInstruction().isEmpty());

            assertNotNull(config.getValidation());
            assertNotNull(config.getDevtools());
        }

        @Test
        @DisplayName("Should have nested validation config with defaults")
        void shouldHaveNestedValidationConfigWithDefaults() {
            ReadEasyConfig config = new ReadEasyConfig();

            assertTrue(config.getValidation().isEnabled());
            assertTrue(config.getValidation().isFailOnError());
            assertTrue(config.getValidation().isValidateTemplates());
        }

        @Test
        @DisplayName("Should have nested devtools config with defaults")
        void shouldHaveNestedDevtoolsConfigWithDefaults() {
            ReadEasyConfig config = new ReadEasyConfig();

            assertFalse(config.getDevtools().isEnabled());
            assertEquals(2000L, config.getDevtools().getWatchIntervalMs());
            assertTrue(config.getDevtools().isValidateOnReload());
        }

        @Test
        @DisplayName("Should allow setting generalReaderInit")
        void shouldAllowSettingGeneralReaderInit() {
            ReadEasyConfig config = new ReadEasyConfig();
            InitDTO initDTO = new InitDTO();
            initDTO.setFqcn("com.example.Reader");
            config.setGeneralReaderInit(initDTO);

            assertNotNull(config.getGeneralReaderInit());
            assertEquals("com.example.Reader", config.getGeneralReaderInit().getFqcn());
        }

        @Test
        @DisplayName("Should allow setting multiple general readers")
        void shouldAllowSettingMultipleGeneralReaders() {
            ReadEasyConfig config = new ReadEasyConfig();
            Map<String, InitDTO> readers = new HashMap<>();

            InitDTO jdbcReader = new InitDTO();
            jdbcReader.setFqcn("lazydevs.persistence.impl.jdbc.JdbcGeneralReader");
            readers.put("default", jdbcReader);

            InitDTO mongoReader = new InitDTO();
            mongoReader.setFqcn("lazydevs.persistence.impl.mongodb.MongoGeneralReader");
            readers.put("mongodb", mongoReader);

            config.setGeneralReaders(readers);

            assertEquals(2, config.getGeneralReaders().size());
            assertNotNull(config.getGeneralReaders().get("default"));
            assertNotNull(config.getGeneralReaders().get("mongodb"));
        }

        @Test
        @DisplayName("Should allow setting query files")
        void shouldAllowSettingQueryFiles() {
            ReadEasyConfig config = new ReadEasyConfig();
            Map<String, List<String>> queryFiles = new LinkedHashMap<>();
            queryFiles.put("users", Arrays.asList(
                    "classpath:queries/users.yaml",
                    "classpath:queries/users-admin.yaml"
            ));
            queryFiles.put("orders", Collections.singletonList(
                    "classpath:queries/orders.yaml"
            ));
            config.setQueryFiles(queryFiles);

            assertEquals(2, config.getQueryFiles().size());
            assertEquals(2, config.getQueryFiles().get("users").size());
            assertEquals(1, config.getQueryFiles().get("orders").size());
        }

        @Test
        @DisplayName("Should allow setting global operation instructions")
        void shouldAllowSettingGlobalOperationInstructions() {
            ReadEasyConfig config = new ReadEasyConfig();
            Map<Operation, Map<String, Object>> instructions = new HashMap<>();

            Map<String, Object> oneInstruction = new HashMap<>();
            oneInstruction.put("statusCodeWhenNoRecordsFound", 404);
            instructions.put(Operation.ONE, oneInstruction);

            Map<String, Object> exportInstruction = new HashMap<>();
            exportInstruction.put("readBatchSize", 1000);
            exportInstruction.put("maxCountToExport", 100000);
            instructions.put(Operation.EXPORT, exportInstruction);

            config.setOperationInstruction(instructions);

            assertEquals(2, config.getOperationInstruction().size());
            assertEquals(404, config.getOperationInstruction()
                    .get(Operation.ONE)
                    .get("statusCodeWhenNoRecordsFound"));
            assertEquals(1000, config.getOperationInstruction()
                    .get(Operation.EXPORT)
                    .get("readBatchSize"));
        }

        @Test
        @DisplayName("Should allow replacing validation config")
        void shouldAllowReplacingValidationConfig() {
            ReadEasyConfig config = new ReadEasyConfig();
            ValidationConfig validation = new ValidationConfig();
            validation.setEnabled(false);
            validation.setFailOnError(false);
            config.setValidation(validation);

            assertFalse(config.getValidation().isEnabled());
            assertFalse(config.getValidation().isFailOnError());
        }

        @Test
        @DisplayName("Should allow replacing devtools config")
        void shouldAllowReplacingDevtoolsConfig() {
            ReadEasyConfig config = new ReadEasyConfig();
            DevtoolsConfig devtools = new DevtoolsConfig();
            devtools.setEnabled(true);
            devtools.setWatchIntervalMs(5000);
            config.setDevtools(devtools);

            assertTrue(config.getDevtools().isEnabled());
            assertEquals(5000, config.getDevtools().getWatchIntervalMs());
        }

        @Test
        @DisplayName("Should have toString method")
        void shouldHaveToString() {
            ReadEasyConfig config = new ReadEasyConfig();
            String str = config.toString();

            assertNotNull(str);
            assertTrue(str.contains("validation"));
            assertTrue(str.contains("devtools"));
            assertTrue(str.contains("queryFiles"));
        }
    }
}
