package lazydevs.persistence.util;

import lazydevs.mapper.utils.ValueExtractor;
import lazydevs.persistence.util.ConditionEvaluator.Condition;
import lazydevs.persistence.util.ConditionEvaluator.Operator;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ParseUtils with improved consistent array behavior.
 *
 * CONSISTENT BEHAVIOR:
 * - users                     → List<Map> (all users)
 * - users[condition]          → List<Map> (filtered users)
 * - users[condition].field    → Object (field from first match)
 * - users[0]                  → Map (single user by index)
 * - users[condition][0]       → Map (first from filtered results)
 *
 * SYNTAX RULES:
 * 1. Use COLON (:) as the separator: field:OPERATOR:value
 * 2. Use QUESTION MARK (?) for method calls: field?method:OPERATOR:value
 * 3. OPERATOR must be a valid ConditionEvaluator.Operator enum name
 * 4. Multiple conditions supported with AND/OR logic and parentheses
 * 5. Array conditions return filtered lists unless followed by field access
 *
 * EXAMPLES:
 * - users[role:EQUALS:admin]                    → List<Map> of all admin users
 * - users[role:EQUALS:admin].name               → String name of first admin user
 * - users[role:EQUALS:admin][0].name            → String name of first admin user (explicit)
 * - users[segments?size:GREATER_THAN:1]         → List<Map> of users with multiple segments
 * - users[segments?size:GREATER_THAN:1].email   → String email of first user with multiple segments
 * - users[(role:EQUALS:admin OR role:EQUALS:moderator) AND status:EQUALS:active] → Filtered list
 */
@Slf4j
public class ParseUtils {

    // Existing helper methods
    public static List<Map<String, Object>> getList(Map<String, Object> map, @NonNull String attributeName){
        Object result = get(map, attributeName);
        if (result instanceof List) {
            return (List<Map<String, Object>>) result;
        }
        return null;
    }

    public static Map<String, Object> getMap(Map<String, Object> map, @NonNull String attributeName){
        Object result = get(map, attributeName);
        if (result instanceof Map) {
            return (Map<String, Object>) result;
        }
        return null;
    }

    public static String getString(Map<String, Object> map, @NonNull String attributeName){
        Object value = get(map, attributeName);
        return value != null ? value.toString() : null;
    }

    // Enhanced get method with consistent array behavior
    public static Object get(Map<String, Object> map, @NonNull String attributeName){
        if (map == null) return null;

        String[] tokens = attributeName.split("\\.");
        Object value = map;

        for(int i = 0; i < tokens.length; i++){
            if (value == null) return null;

            if (!(value instanceof Map) && !(value instanceof List)) {
                return null;
            }

            String token = tokens[i];
            boolean isLastToken = (i == tokens.length - 1);

            if(token.contains("[") && token.endsWith("]")){
                String arrayKey = token.substring(0, token.indexOf("["));
                String indexStr = token.substring(token.indexOf("[")+1, token.indexOf("]"));

                // Get the array/list
                Object listObj;
                if (value instanceof Map) {
                    listObj = ((Map<String, Object>)value).get(arrayKey);
                } else if (value instanceof List && arrayKey.isEmpty()) {
                    // Case: someList[condition] where value is already the list
                    listObj = value;
                } else {
                    return null;
                }

                if (listObj == null) return null;
                if (!(listObj instanceof List)) return null;

                List<Map<String, Object>> list = (List<Map<String, Object>>) listObj;

                if(indexStr.matches("-?\\d+")){
                    // Numeric index access - always returns single element
                    int index = Integer.parseInt(indexStr);
                    if (index >= 0 && index < list.size()) {
                        value = list.get(index);
                    } else {
                        return null;
                    }
                } else {
                    // Condition-based filtering
                    try {
                        List<Map<String, Object>> filteredList = list.stream()
                                .filter(e -> e != null && evaluateMultipleConditions(e, indexStr))
                                .collect(Collectors.toList());

                        if (isLastToken) {
                            // If this is the last token, return the filtered list
                            return filteredList;
                        } else {
                            // If there are more tokens, use first match for continued navigation
                            if (filteredList.isEmpty()) {
                                return null;
                            }
                            value = filteredList;
                        }
                    } catch (Exception e) {
                        log.error("Error evaluating conditions: {}", indexStr, e);
                        return null;
                    }
                }
            } else {
                // Simple field access
                if (value instanceof Map) {
                    value = ((Map<String, Object>)value).get(token);
                } else if( value instanceof List){
                    var l = (List<?>) value;
                    if(!l.isEmpty()){
                        var v = l.getFirst();
                        if(v instanceof Map<?,?>){
                            value = ((Map<?, ?>) v).get(token);
                        }
                    }
                }else{
                    // This case shouldn't happen with proper navigation
                    return null;
                }
            }
        }
        return value;
    }

    /**
     * Evaluate multiple conditions with logical operators (AND/OR) and parentheses support
     * Default logic is AND unless OR is explicitly specified
     * Supports parentheses for grouping and precedence control
     */
    private static boolean evaluateMultipleConditions(Map<String, Object> item, String conditionsStr) {
        if (item == null || conditionsStr == null || conditionsStr.trim().isEmpty()) {
            return false;
        }

        String conditions = conditionsStr.trim();
        log.debug("Evaluating multiple conditions: {} on item: {}", conditions, item.get("id"));

        try {
            // Handle legacy containsKey format for backward compatibility
            if (conditions.startsWith("containsKey=")) {
                String key = conditions.substring(12).replace("'", "").replace("\"", "");
                return item.containsKey(key);
            }

            // Check if there are parentheses, AND, or OR operators
            if (conditions.contains("(") || conditions.contains(" AND ") || conditions.contains(" OR ")) {
                return evaluateComplexConditionsWithParentheses(item, conditions);
            } else {
                // Single condition - use existing logic
                return evaluateCondition(item, conditions);
            }

        } catch (Exception e) {
            log.error("Error evaluating multiple conditions: {}", conditions, e);
            return false;
        }
    }

    /**
     * Handle complex conditions with AND/OR logic and parentheses support
     * Uses recursive descent parsing for proper precedence handling
     */
    private static boolean evaluateComplexConditionsWithParentheses(Map<String, Object> item, String conditionsStr) {
        try {
            // Parse and evaluate the expression with proper precedence
            return parseExpression(item, conditionsStr.trim());

        } catch (Exception e) {
            log.error("Error evaluating complex conditions with parentheses: {}", conditionsStr, e);
            return false;
        }
    }

    /**
     * Parse logical expression with proper precedence:
     * 1. Parentheses (highest)
     * 2. AND
     * 3. OR (lowest)
     */
    private static boolean parseExpression(Map<String, Object> item, String expression) {
        return parseOrExpression(item, expression);
    }

    /**
     * Parse OR expressions (lowest precedence)
     */
    private static boolean parseOrExpression(Map<String, Object> item, String expression) {
        // Split by OR, but be careful about parentheses
        java.util.List<String> orParts = splitByOperatorRespectingParentheses(expression, " OR ");

        if (orParts.size() == 1) {
            // No OR operator, delegate to AND parsing
            return parseAndExpression(item, expression);
        }

        // Evaluate each OR part - if any is true, return true
        for (String orPart : orParts) {
            if (parseAndExpression(item, orPart.trim())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse AND expressions (higher precedence than OR)
     */
    private static boolean parseAndExpression(Map<String, Object> item, String expression) {
        // Split by AND, but be careful about parentheses
        java.util.List<String> andParts = splitByOperatorRespectingParentheses(expression, " AND ");

        if (andParts.size() == 1) {
            // No AND operator, handle parentheses or single condition
            return parsePrimaryExpression(item, expression);
        }

        // Evaluate each AND part - all must be true
        for (String andPart : andParts) {
            if (!parsePrimaryExpression(item, andPart.trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parse primary expressions (parentheses or single conditions)
     */
    private static boolean parsePrimaryExpression(Map<String, Object> item, String expression) {
        expression = expression.trim();

        // Handle parentheses
        if (expression.startsWith("(") && expression.endsWith(")")) {
            // Remove outer parentheses and recurse
            String inner = expression.substring(1, expression.length() - 1).trim();
            return parseExpression(item, inner);
        }

        // Single condition
        return evaluateCondition(item, expression);
    }

    /**
     * Split string by operator while respecting parentheses
     * This ensures we don't split inside parentheses
     */
    private static java.util.List<String> splitByOperatorRespectingParentheses(String expression, String operator) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int parenthesesLevel = 0;
        int start = 0;

        for (int i = 0; i <= expression.length() - operator.length(); i++) {
            char c = expression.charAt(i);

            if (c == '(') {
                parenthesesLevel++;
            } else if (c == ')') {
                parenthesesLevel--;
            } else if (parenthesesLevel == 0 && expression.substring(i).startsWith(operator)) {
                // Found operator at top level (not inside parentheses)
                parts.add(expression.substring(start, i));
                start = i + operator.length();
                i += operator.length() - 1; // Skip the operator
            }
        }

        // Add the last part
        parts.add(expression.substring(start));

        return parts;
    }

    /**
     * Evaluate single condition using standardized colon-separated syntax with method calls
     * Leverages ConditionEvaluator for actual condition evaluation
     */
    private static boolean evaluateCondition(Map<String, Object> item, String conditionStr) {
        if (item == null || conditionStr == null || conditionStr.trim().isEmpty()) {
            return false;
        }

        String condition = conditionStr.trim();
        log.debug("Evaluating condition: {} on item: {}", condition, item.get("id"));

        try {
            // Handle legacy containsKey format for backward compatibility
            if (condition.startsWith("containsKey=")) {
                String key = condition.substring(12).replace("'", "").replace("\"", "");
                return item.containsKey(key);
            }

            // Check if this is a method call condition (contains ?)
            if (condition.contains("?")) {
                return evaluateMethodCallCondition(item, condition);
            }

            // Parse regular colon-separated condition: field:operation:value
            String[] parts = condition.split(":", 3);
            if (parts.length < 2) {
                log.debug("Invalid condition format: {}", condition);
                return false;
            }

            String field = parts[0].trim();
            String operation = parts[1].trim();
            String valueStr = parts.length > 2 ? parts[2].trim() : null;

            // Convert our syntax to ConditionEvaluator operators and use it
            Condition conditionObj = createConditionFromParts(field, operation, valueStr);
            if (conditionObj != null) {
                return ConditionEvaluator.evaluate(conditionObj, item);
            }

            log.debug("Could not create condition from: {}", condition);
            return false;

        } catch (Exception e) {
            log.error("Error evaluating condition: {}", condition, e);
            return false;
        }
    }

    /**
     * Handle method call conditions: field?method:operation:value
     * Creates a temporary map with the method result and uses ConditionEvaluator
     */
    private static boolean evaluateMethodCallCondition(Map<String, Object> item, String condition) {
        try {
            String[] parts = condition.split(":", 3); // Split only into 3 parts max
            if (parts.length < 3) {
                log.debug("Invalid method call operation format: {}", condition);
                return false;
            }

            // Extract path, operation, and value
            String pathExpr = parts[0].trim();       // e.g., settings?cache?enabled
            String operation = parts[1].trim();       // e.g., EQUALS
            String valueStr = parts[2].trim();        // e.g., true

            // Resolve path to final value
            Object value = resolvePath(item, pathExpr);
            if (value == null) {
                return false;
            }

            // Evaluate using a temporary condition object
            Map<String, Object> tempMap = new HashMap<>();
            tempMap.put("resolved", value);

            Condition conditionObj = createConditionFromParts("resolved", operation, valueStr);
            return conditionObj != null && ConditionEvaluator.evaluate(conditionObj, tempMap);

        } catch (Exception e) {
            log.error("Error evaluating method call condition: {}", condition, e);
            return false;
        }
    }

    private static Object resolvePath(Object current, String pathExpr) {
        String[] tokens = pathExpr.split("\\?");
        Object value = current;

        for (String token : tokens) {
            if (value == null || token.isEmpty()) {
                return null;
            }

            token = token.trim();

            if (value instanceof Map) {
                value = ((Map<?, ?>) value).get(token);
            } else {
                value = callMethod(value, token);
            }
        }

        return value;
    }

    /**
     * Call a no-argument method on an object using reflection
     */
    private static Object callMethod(Object obj, String methodName) {
        try {
            // Handle common methods efficiently without reflection
            switch (methodName) {
                case "size":
                    if (obj instanceof List) return ((List<?>) obj).size();
                    if (obj instanceof Map) return ((Map<?, ?>) obj).size();
                    if (obj instanceof String) return ((String) obj).length(); // String doesn't have size(), use length()
                    break;

                case "length":
                    if (obj instanceof String) return ((String) obj).length();
                    if (obj instanceof List) return ((List<?>) obj).size(); // List doesn't have length(), use size()
                    break;

                case "isEmpty":
                    if (obj instanceof List) return ((List<?>) obj).isEmpty();
                    if (obj instanceof Map) return ((Map<?, ?>) obj).isEmpty();
                    if (obj instanceof String) return ((String) obj).isEmpty();
                    break;

                case "toString":
                    return obj.toString();

                case "toLowerCase":
                    if (obj instanceof String) return ((String) obj).toLowerCase();
                    break;

                case "toUpperCase":
                    if (obj instanceof String) return ((String) obj).toUpperCase();
                    break;

                case "trim":
                    if (obj instanceof String) return ((String) obj).trim();
                    break;
            }

            // Fallback to reflection for other methods
            try {
                java.lang.reflect.Method method = obj.getClass().getMethod(methodName);
                return method.invoke(obj);
            } catch (Exception e) {
                log.debug("Method {} not found or failed on object of type {}", methodName, obj.getClass().getSimpleName());
                return null;
            }

        } catch (Exception e) {
            log.error("Error calling method {} on object {}", methodName, obj, e);
            return null;
        }
    }

    /**
     * Convert our colon syntax to ConditionEvaluator.Condition objects
     * Assumes operation is always a valid ConditionEvaluator.Operator enum name
     */
    private static Condition createConditionFromParts(String field, String operation, String valueStr) {
        try {
            // Directly parse operation as Operator enum
            Operator operator = Operator.valueOf(operation.toUpperCase());

            // Handle operations that don't need values
            if (operator == Operator.EXISTS || operator == Operator.IS_EMPTY ||
                    operator == Operator.NOT_EXISTS || operator == Operator.IS_NULL ||
                    operator == Operator.IS_NOT_NULL || operator == Operator.NOT_EMPTY) {
                return new Condition(field, operator, null);
            }

            // Handle IN/NOT_IN operators (need list of values)
            if (operator == Operator.IN || operator == Operator.NOT_IN) {
                return new Condition(field, operator, parseInValues(valueStr));
            }

            // Handle numeric operations (need numeric values for better type safety)
            if (operator == Operator.GREATER_THAN || operator == Operator.LESS_THAN ||
                    operator == Operator.GREATER_THAN_OR_EQUAL || operator == Operator.LESS_THAN_OR_EQUAL) {
                return new Condition(field, operator, parseNumericValue(valueStr));
            }

            // Handle all other operations with type coercion
            return new Condition(field, operator, parseValue(valueStr));

        } catch (IllegalArgumentException e) {
            log.error("Invalid operator: {}. Must be a valid ConditionEvaluator.Operator enum value", operation);
            return null;
        } catch (Exception e) {
            log.error("Error creating condition from parts: field={}, operation={}, value={}", field, operation, valueStr, e);
            return null;
        }
    }

    /**
     * Parse value with type coercion
     */
    private static Object parseValue(String valueStr) {
        if (valueStr == null) return null;

        // Try to parse as number first
        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Integer.parseInt(valueStr);
            }
        } catch (NumberFormatException e) {
            // Return as string if not a number
            return valueStr;
        }
    }

    /**
     * Parse numeric value
     */
    private static Object parseNumericValue(String valueStr) {
        if (valueStr == null) return null;

        try {
            if (valueStr.contains(".")) {
                return Double.parseDouble(valueStr);
            } else {
                return Integer.parseInt(valueStr);
            }
        } catch (NumberFormatException e) {
            log.error("Invalid numeric value: {}", valueStr);
            return null;
        }
    }

    /**
     * Parse comma-separated values for IN operator
     */
    private static java.util.List<String> parseInValues(String valueStr) {
        if (valueStr == null) return java.util.Collections.emptyList();

        String[] values = valueStr.split(",");
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String value : values) {
            result.add(value.trim());
        }
        return result;
    }

    /**
     * Check if value is empty (used for validation, ConditionEvaluator handles the actual logic)
     */
    private static boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof String) return ((String) value).trim().isEmpty();
        if (value instanceof List) return ((List<?>) value).isEmpty();
        if (value instanceof Map) return ((Map<?, ?>) value).isEmpty();
        return false;
    }

    // Additional helper methods
    public static Object getOrDefault(Map<String, Object> map, @NonNull String attributeName, Object defaultValue) {
        Object result = get(map, attributeName);
        return result != null ? result : defaultValue;
    }

    public static boolean exists(Map<String, Object> map, @NonNull String attributeName) {
        return get(map, attributeName) != null;
    }

    public static Integer getInteger(Map<String, Object> map, @NonNull String attributeName) {
        Object value = get(map, attributeName);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double getDouble(Map<String, Object> map, @NonNull String attributeName) {
        Object value = get(map, attributeName);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Boolean getBoolean(Map<String, Object> map, @NonNull String attributeName) {
        Object value = get(map, attributeName);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;

        String str = value.toString().toLowerCase().trim();
        if ("true".equals(str) || "1".equals(str) || "yes".equals(str)) return true;
        if ("false".equals(str) || "0".equals(str) || "no".equals(str)) return false;

        return null;
    }

    public static String getStringOrDefault(Map<String, Object> map, @NonNull String attributeName, String defaultValue) {
        Object value = get(map, attributeName);
        return value != null ? value.toString() : defaultValue;
    }
}