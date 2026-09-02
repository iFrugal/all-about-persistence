package lazydevs.readeasy.validation;

import lazydevs.readeasy.validation.QueryValidationException.ValidationError;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryValidatorTest {

    private final QueryValidator validator = new QueryValidator();

    private static final String VALID_FILE = """
            queries:
              byId:
                raw: |
                  {"nativeSQL": "SELECT * FROM users WHERE id = :id",
                   "params": [{"name": "id", "type": "STRING", "value": "${params.id}"}]}
            """;

    @Test
    void validFilePasses() {
        List<ValidationError> errors = validator.validateQueryFile(
                "users", "users.yaml", VALID_FILE, Set.of("default"), true);
        assertTrue(errors.isEmpty(), () -> "expected no errors but got: " + errors);
    }

    @Test
    void unguardedRequestSpecificVariablesAreNotFalsePositives() {
        // Regression guard: validation must be parse-only. A template referencing a
        // variable that exists only at request time is valid.
        String file = """
                queries:
                  byCustomer:
                    raw: |
                      {"nativeSQL": "SELECT * FROM orders WHERE customer_id = :cid",
                       "params": [{"name": "cid", "type": "STRING", "value": "${params.customerId}"},
                                  {"name": "tenant", "type": "STRING", "value": "${request.tenantId}"}]}
                """;
        List<ValidationError> errors = validator.validateQueryFile(
                "orders", "orders.yaml", file, Set.of("default"), true);
        assertTrue(errors.isEmpty(), () -> "parse-only validation must not fail on runtime variables: " + errors);
    }

    @Test
    void missingRawIsFlagged() {
        String file = """
                queries:
                  broken:
                    readerId: default
                """;
        List<ValidationError> errors = validator.validateQueryFile(
                "ns", "f.yaml", file, Set.of("default"), true);
        assertEquals(1, errors.size());
        assertEquals("MISSING_RAW", errors.get(0).getErrorType());
        assertEquals("ns.broken", errors.get(0).getQueryId());
    }

    @Test
    void unknownReaderIdIsFlagged() {
        String file = """
                queries:
                  q1:
                    readerId: mongo
                    raw: '{"collectionName": "users"}'
                """;
        List<ValidationError> errors = validator.validateQueryFile(
                "ns", "f.yaml", file, Set.of("default"), true);
        assertEquals(1, errors.size());
        assertEquals("INVALID_READER", errors.get(0).getErrorType());
    }

    @Test
    void readerCheckSkippedWhenReaderIdsUnknown() {
        String file = """
                queries:
                  q1:
                    readerId: mongo
                    raw: '{"collectionName": "users"}'
                """;
        assertTrue(validator.validateQueryFile("ns", "f.yaml", file, Set.of(), true).isEmpty());
        assertTrue(validator.validateQueryFile("ns", "f.yaml", file, null, true).isEmpty());
    }

    @Test
    void freeMarkerSyntaxErrorIsFlagged() {
        String file = """
                queries:
                  q1:
                    raw: |
                      {"nativeSQL": "SELECT * FROM t <#if params.x?? WHERE x = 1"}
                """;
        List<ValidationError> errors = validator.validateQueryFile(
                "ns", "f.yaml", file, Set.of("default"), true);
        assertEquals(1, errors.size());
        assertEquals("TEMPLATE_SYNTAX", errors.get(0).getErrorType());
    }

    @Test
    void templateCheckSkippedWhenDisabled() {
        String file = """
                queries:
                  q1:
                    raw: |
                      {"nativeSQL": "SELECT 1 <#if broken"}
                """;
        assertTrue(validator.validateQueryFile("ns", "f.yaml", file, Set.of("default"), false).isEmpty());
    }

    @Test
    void invalidYamlIsFlagged() {
        List<ValidationError> errors = validator.validateQueryFile(
                "ns", "f.yaml", "queries: [not, a, map", Set.of("default"), true);
        assertEquals(1, errors.size());
        assertEquals("YAML_SYNTAX", errors.get(0).getErrorType());
    }

    @Test
    void emptyFileIsFlagged() {
        List<ValidationError> errors = validator.validateQueryFile(
                "ns", "f.yaml", "queries: {}", Set.of("default"), true);
        assertEquals(1, errors.size());
        assertEquals("MISSING_QUERIES", errors.get(0).getErrorType());
    }

    @Test
    void validateAndThrowHonorsFailOnError() {
        String broken = """
                queries:
                  broken:
                    readerId: default
                """;
        assertThrows(QueryValidationException.class, () ->
                validator.validateAndThrow("ns", "f.yaml", broken, Set.of("default"), true, true));
        assertDoesNotThrow(() ->
                validator.validateAndThrow("ns", "f.yaml", broken, Set.of("default"), true, false));
    }

    @Test
    void rowTransformerTemplateIsValidated() {
        String file = """
                queries:
                  q1:
                    raw: '{"nativeSQL": "SELECT 1"}'
                    rowTransformer:
                      template: '{"x": "${row.x"}'
                """;
        List<ValidationError> errors = validator.validateQueryFile(
                "ns", "f.yaml", file, Set.of("default"), true);
        assertEquals(1, errors.size());
        assertEquals("TEMPLATE_SYNTAX", errors.get(0).getErrorType());
        assertEquals("ns.q1.rowTransformer", errors.get(0).getQueryId());
    }
}
