package lazydevs.persistence.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.testng.annotations.*;
import org.testng.Assert;

import java.util.*;

import static lazydevs.persistence.util.ConditionEvaluator.*;

public class ConditionEvaluatorTest {

    private ObjectMapper objectMapper;
    private Map<String, Object> testData;

    @BeforeMethod
    public void setUp() {
        objectMapper = new ObjectMapper();
        testData = createTestData();
    }

    // Test data creation
    private Map<String, Object> createTestData() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "error");
        data.put("success", false);
        data.put("count", 42);
        data.put("score", 85.5);
        data.put("message", "Something went wrong");
        data.put("tags", Arrays.asList("urgent", "api", "error"));
        data.put("metadata", null);

        Map<String, Object> response = new HashMap<>();
        data.put("Response", response);

        Map<String, Object> error = new HashMap<>();
        response.put("Error", error);
        error.put("ErrorCode", 500);
        error.put("ErrorMessage", "Internal server error");
        error.put("Severity", "HIGH");

        Map<String, Object> nested = new HashMap<>();
        response.put("Nested", nested);
        nested.put("Deep", new HashMap<String, Object>() {{
            put("Value", "found");
            put("Items", Arrays.asList(
                    new HashMap<String, Object>() {{ put("id", 1); put("name", "item1"); }},
                    new HashMap<String, Object>() {{ put("id", 2); put("name", "item2"); }}
            ));
        }});

        return data;
    }

    // Smart test data provider using JSON
    @DataProvider(name = "singleConditionTests")
    public Object[][] singleConditionTestData() {
        return new Object[][] {
                // Basic equality tests
                {"status", Operator.EQUALS, "error", true, "String equality"},
                {"status", Operator.EQUALS, "success", false, "String equality false"},
                {"success", Operator.EQUALS, false, true, "Boolean equality"},
                {"count", Operator.EQUALS, 42, true, "Integer equality"},

                // Numeric comparison tests
                {"count", Operator.GREATER_THAN, 40, true, "Greater than true"},
                {"count", Operator.GREATER_THAN, 50, false, "Greater than false"},
                {"count", Operator.LESS_THAN, 50, true, "Less than true"},
                {"score", Operator.GREATER_THAN_OR_EQUAL, 85.5, true, "GTE decimal"},
                {"score", Operator.LESS_THAN_OR_EQUAL, 85.5, true, "LTE decimal"},

                // String operations
                {"message", Operator.CONTAINS, "went", true, "Contains substring"},
                {"message", Operator.STARTS_WITH, "Something", true, "Starts with"},
                {"message", Operator.ENDS_WITH, "wrong", true, "Ends with"},
                {"message", Operator.NOT_CONTAINS, "success", true, "Not contains"},

                // Existence checks
                {"status", Operator.EXISTS, null, true, "Field exists"},
                {"nonexistent", Operator.NOT_EXISTS, null, true, "Field not exists"},
                {"metadata", Operator.IS_NULL, null, true, "Field is null"},
                {"status", Operator.IS_NOT_NULL, null, true, "Field is not null"},

                // Collection operations
                {"tags", Operator.NOT_EMPTY, null, true, "Array not empty"},
                {"tags", Operator.CONTAINS, "urgent", true, "Array contains element"},

                // IN operations
                {"status", Operator.IN, Arrays.asList("error", "warning"), true, "Value in list"},
                {"status", Operator.NOT_IN, Arrays.asList("success", "pending"), true, "Value not in list"},

                // Nested field tests
                {"Response.Error.ErrorCode", Operator.EQUALS, 500, true, "Nested field access"},
                {"Response.Error.ErrorMessage", Operator.EXISTS, null, true, "Nested field exists"},
                {"Response.Nested.Deep.Value", Operator.EQUALS, "found", true, "Deep nested access"},

                // Regex tests
                {"status", Operator.REGEX_MATCH, "err.*", true, "Regex match"},
                {"message", Operator.REGEX_MATCH, ".*wrong$", true, "Regex end match"}
        };
    }

    @Test(dataProvider = "singleConditionTests")
    public void testSingleConditionEvaluation(String field, Operator operator, Object value,
                                              boolean expected, String description) {
        Condition condition = new Condition(field, operator, value);
        boolean result = ConditionEvaluator.evaluate(condition, testData);
        Assert.assertEquals(result, expected, description);
    }

    @DataProvider(name = "multipleConditionTests")
    public Object[][] multipleConditionTestData() {
        return new Object[][] {
                // OR tests - any condition matches
                {createConditions(
                        new String[]{"status", "success"},
                        new Operator[]{Operator.EQUALS, Operator.EQUALS},
                        new Object[]{"error", true}
                ), LogicalOperator.OR, true, "OR with first match"},

                {createConditions(
                        new String[]{"status", "count"},
                        new Operator[]{Operator.EQUALS, Operator.GREATER_THAN},
                        new Object[]{"success", 40}
                ), LogicalOperator.OR, true, "OR with second match"},

                // AND tests - all conditions must match
                {createConditions(
                        new String[]{"status", "Response.Error.ErrorCode"},
                        new Operator[]{Operator.EQUALS, Operator.GREATER_THAN},
                        new Object[]{"error", 400}
                ), LogicalOperator.AND, true, "AND all match"},

                {createConditions(
                        new String[]{"status", "success"},
                        new Operator[]{Operator.EQUALS, Operator.EQUALS},
                        new Object[]{"error", true}
                ), LogicalOperator.AND, false, "AND partial match"},
        };
    }

    private List<Condition> createConditions(String[] fields, Operator[] operators, Object[] values) {
        List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < fields.length; i++) {
            conditions.add(new Condition(fields[i], operators[i], values[i]));
        }
        return conditions;
    }

    @Test(dataProvider = "multipleConditionTests")
    public void testMultipleConditionEvaluation(List<Condition> conditions, LogicalOperator logic,
                                                boolean expected, String description) {
        boolean result = ConditionEvaluator.evaluate(conditions, logic, testData);
        Assert.assertEquals(result, expected, description);
    }

    @DataProvider(name = "conditionGroupTests")
    public Object[][] conditionGroupTestData() {
        return new Object[][] {
                // Single group tests
                {createConditionGroup(LogicalOperator.OR, new String[]{"status", "count"},
                        new Operator[]{Operator.EQUALS, Operator.GREATER_THAN},
                        new Object[]{"error", 100}), true, "Group OR first match"},

                {createConditionGroup(LogicalOperator.AND, new String[]{"status", "Response.Error.ErrorCode"},
                        new Operator[]{Operator.EQUALS, Operator.EXISTS},
                        new Object[]{"error", null}), true, "Group AND all match"},
        };
    }

    private ConditionGroup createConditionGroup(LogicalOperator logic, String[] fields,
                                                Operator[] operators, Object[] values) {
        return new ConditionGroup(logic, createConditions(fields, operators, values));
    }

    @Test(dataProvider = "conditionGroupTests")
    public void testConditionGroupEvaluation(ConditionGroup group, boolean expected, String description) {
        boolean result = ConditionEvaluator.evaluate(group, testData);
        Assert.assertEquals(result, expected, description);
    }

    // JSON-based comprehensive test cases
    @DataProvider(name = "jsonTestCases")
    public Object[][] jsonTestCases() {
        return new Object[][] {
                // Single condition JSON
                {"{\"condition\":{\"field\":\"status\",\"operator\":\"EQUALS\",\"value\":\"error\"}}",
                        true, "JSON single condition"},

                // Multiple conditions JSON
                {"{\"conditions\":[" +
                        "{\"field\":\"status\",\"operator\":\"EQUALS\",\"value\":\"error\"}," +
                        "{\"field\":\"count\",\"operator\":\"GREATER_THAN\",\"value\":40}" +
                        "],\"logic\":\"OR\"}",
                        true, "JSON multiple conditions OR"},

                // Complex condition groups JSON
                {"{\"conditionGroups\":[" +
                        "{\"logic\":\"OR\",\"conditions\":[" +
                        "{\"field\":\"status\",\"operator\":\"EQUALS\",\"value\":\"error\"}," +
                        "{\"field\":\"success\",\"operator\":\"EQUALS\",\"value\":false}" +
                        "]}," +
                        "{\"logic\":\"AND\",\"conditions\":[" +
                        "{\"field\":\"Response.Error.ErrorCode\",\"operator\":\"EXISTS\",\"value\":null}," +
                        "{\"field\":\"Response.Error.ErrorMessage\",\"operator\":\"NOT_EMPTY\",\"value\":null}" +
                        "]}" +
                        "],\"groupLogic\":\"OR\"}",
                        true, "JSON complex groups"}
        };
    }

    @Test(dataProvider = "jsonTestCases")
    public void testJsonDeserialization(String json, boolean expected, String description) throws Exception {
        ConditionRequest request = objectMapper.readValue(json, ConditionRequest.class);
        boolean result = request.evaluate(testData);
        Assert.assertEquals(result, expected, description);
    }

    // Edge cases and error handling
    @Test
    public void testEmptyConditionsReturnsFalse() {
        ConditionRequest request = new ConditionRequest();
        Assert.assertFalse(request.evaluate(testData), "Empty request should return false");
    }

    @Test
    public void testNullDataHandling() {
        // Test accessing a field that doesn't exist in nested structure
        Condition condition = new Condition("Response.NonExistent.Field", Operator.EXISTS, null);
        Assert.assertFalse(ConditionEvaluator.evaluate(condition, testData),
                "Non-existent nested field should return false");

        // Test accessing field in null intermediate object
        Map<String, Object> dataWithNull = new HashMap<>();
        dataWithNull.put("level1", null);
        Condition condition3 = new Condition("level1.level2.field", Operator.EXISTS, null);
        Assert.assertFalse(ConditionEvaluator.evaluate(condition3, dataWithNull),
                "Field access through null intermediate should return false");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullDataThrowsException() {
        // Test with completely null data - should throw exception due to @NonNull
        Map<String, Object> nullData = null;
        Condition condition = new Condition("anyField", Operator.EXISTS, null);
        ConditionEvaluator.evaluate(condition, nullData);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testInvalidStringOperator() {
        new Condition("field", "INVALID_OPERATOR", "value");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNumberComparisonWithNonNumbers() {
        Condition condition = new Condition("status", Operator.GREATER_THAN, "error");
        ConditionEvaluator.evaluate(condition, testData);
    }

    // Performance and stress tests
    @Test
    public void testLargeConditionSet() {
        List<Condition> conditions = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            conditions.add(new Condition("count", Operator.GREATER_THAN, i));
        }

        long startTime = System.currentTimeMillis();
        boolean result = ConditionEvaluator.evaluate(conditions, LogicalOperator.OR, testData);
        long endTime = System.currentTimeMillis();

        Assert.assertTrue(result, "Large condition set should find match");
        Assert.assertTrue(endTime - startTime < 1000, "Should complete within 1 second");
    }

    // Comprehensive integration test
    @Test
    public void testComplexRealWorldScenario() {
        // Simulate API error detection scenario
        Map<String, Object> apiResponse = new HashMap<>();
        apiResponse.put("httpStatus", 200);
        apiResponse.put("success", false);

        Map<String, Object> response = new HashMap<>();
        apiResponse.put("response", response);
        response.put("errorCode", "E001");
        response.put("errorMessage", "Payment declined");
        response.put("retryable", true);

        Map<String, Object> details = new HashMap<>();
        response.put("details", details);
        details.put("reason", "Insufficient funds");
        details.put("timestamp", System.currentTimeMillis());

        // Complex condition: HTTP 200 but business logic failure
        ConditionGroup httpOk = new ConditionGroup(LogicalOperator.AND, Arrays.asList(
                new Condition("httpStatus", Operator.EQUALS, 200),
                new Condition("success", Operator.EQUALS, false)
        ));

        ConditionGroup businessError = new ConditionGroup(LogicalOperator.OR, Arrays.asList(
                new Condition("response.errorCode", Operator.EXISTS, null),
                new Condition("response.details.reason", Operator.CONTAINS, "funds")
        ));

        ConditionRequest request = new ConditionRequest();
        request.setConditionGroups(Arrays.asList(httpOk, businessError));
        request.setGroupLogic(LogicalOperator.AND);

        boolean isApiError = request.evaluate(apiResponse);
        Assert.assertTrue(isApiError, "Should detect API business logic error");
    }

    // Test backward compatibility with string operators
    @Test
    public void testStringOperatorBackwardCompatibility() {
        Condition condition = new Condition("status", "equals", "error");
        boolean result = ConditionEvaluator.evaluate(condition, testData);
        Assert.assertTrue(result, "String operator should work via valueOf conversion");
    }

    // Test array access with ParseUtils integration
    @Test
    public void testArrayAccess() {
        Condition condition = new Condition("Response.Nested.Deep.Items[0].name", Operator.EQUALS, "item1");
        boolean result = ConditionEvaluator.evaluate(condition, testData);
        Assert.assertTrue(result, "Should access array elements correctly");
    }

    // Helper method for test data validation
    @Test
    public void validateTestDataStructure() {
        Assert.assertNotNull(testData.get("status"));
        Assert.assertNotNull(testData.get("Response"));
        Assert.assertTrue(testData.get("tags") instanceof List);
        Assert.assertEquals(ParseUtils.get(testData, "Response.Error.ErrorCode"), 500);
    }
}