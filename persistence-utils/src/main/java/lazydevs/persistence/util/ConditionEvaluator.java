package lazydevs.persistence.util;

import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Generic condition evaluator - returns true/false based on condition evaluation
 */
@Slf4j
public class ConditionEvaluator {

    public enum LogicalOperator {
        AND, OR
    }

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
        GREATER_THAN,
        LESS_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN_OR_EQUAL,
        CONTAINS,
        NOT_CONTAINS,
        EXISTS,
        NOT_EXISTS,
        IS_NULL,
        IS_NOT_NULL,
        IN,
        NOT_IN,
        REGEX_MATCH,
        STARTS_WITH,
        ENDS_WITH,
        NOT_EMPTY,
        IS_EMPTY
    }

    /**
     * Main evaluation method for ConditionRequest wrapper
     */
    public static boolean evaluate(@NonNull ConditionRequest request, @NonNull Map<String, Object> data) {
        log.debug("Evaluating ConditionRequest");

        // Single condition (highest priority)
        if (request.getCondition() != null) {
            log.debug("Evaluating single condition");
            return evaluate(request.getCondition(), data);
        }

        // Multiple conditions with logic
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            LogicalOperator logic = request.getLogic() != null ? request.getLogic() : LogicalOperator.OR;
            log.debug("Evaluating {} conditions with {} logic", request.getConditions().size(), logic);
            return evaluate(request.getConditions(), logic, data);
        }

        // Complex condition groups
        if (request.getConditionGroups() != null && !request.getConditionGroups().isEmpty()) {
            LogicalOperator groupLogic = request.getGroupLogic() != null ? request.getGroupLogic() : LogicalOperator.OR;
            log.debug("Evaluating {} condition groups with {} group logic", request.getConditionGroups().size(), groupLogic);
            return evaluateGroups(request.getConditionGroups(), groupLogic, data);
        }

        log.debug("No conditions found in request, returning false");
        return false;
    }

    /**
     * Evaluate single condition
     */
    public static boolean evaluate(@NonNull Condition condition, @NonNull Map<String, Object> data) {
        Object fieldValue = ParseUtils.get(data, condition.getField());
        boolean result = evaluateCondition(fieldValue, condition.getOperator(), condition.getValue());

        log.debug("Evaluating condition: field={}, operator={}, value={}, fieldValue={}, result={}",
                condition.getField(), condition.getOperator(), condition.getValue(), fieldValue, result);

        return result;
    }

    /**
     * Evaluate multiple conditions with explicit AND/OR logic
     */
    public static boolean evaluate(@NonNull List<Condition> conditions,
                                   @NonNull LogicalOperator logic,
                                   @NonNull Map<String, Object> data) {
        if (conditions.isEmpty()) {
            log.debug("No conditions to evaluate, returning false");
            return false;
        }

        boolean result;
        if (logic == LogicalOperator.AND) {
            result = conditions.stream().allMatch(condition -> evaluate(condition, data));
        } else { // LogicalOperator.OR
            result = conditions.stream().anyMatch(condition -> evaluate(condition, data));
        }

        log.debug("Evaluated {} conditions with {} logic: result={}", conditions.size(), logic, result);
        return result;
    }

    /**
     * Evaluate condition group
     */
    public static boolean evaluate(@NonNull ConditionGroup conditionGroup, @NonNull Map<String, Object> data) {
        log.debug("Evaluating condition group with {} logic and {} conditions",
                conditionGroup.getLogic(), conditionGroup.getConditions().size());

        return evaluate(conditionGroup.getConditions(), conditionGroup.getLogic(), data);
    }

    /**
     * Evaluate multiple condition groups with explicit AND/OR logic between groups
     */
    public static boolean evaluateGroups(@NonNull List<ConditionGroup> conditionGroups,
                                         @NonNull LogicalOperator groupLogic,
                                         @NonNull Map<String, Object> data) {
        if (conditionGroups.isEmpty()) {
            log.debug("No condition groups to evaluate, returning false");
            return false;
        }

        boolean result;
        if (groupLogic == LogicalOperator.AND) {
            result = conditionGroups.stream().allMatch(group -> evaluate(group, data));
        } else { // LogicalOperator.OR
            result = conditionGroups.stream().anyMatch(group -> evaluate(group, data));
        }

        log.debug("Evaluated {} condition groups with {} group logic: result={}",
                conditionGroups.size(), groupLogic, result);
        return result;
    }

    /**
     * Core condition evaluation logic
     */
    private static boolean evaluateCondition(Object fieldValue, Operator operator, Object expectedValue) {
        try {
            switch (operator) {
                case EQUALS:
                    if(expectedValue instanceof String) {
                        fieldValue = String.valueOf(fieldValue);
                    }
                    return Objects.equals(fieldValue, expectedValue);

                case NOT_EQUALS:
                    if(expectedValue instanceof String) {
                        fieldValue = String.valueOf(fieldValue);
                    }
                    return !Objects.equals(fieldValue, expectedValue);

                case GREATER_THAN:
                    return compareNumbers(fieldValue, expectedValue) > 0;

                case LESS_THAN:
                    return compareNumbers(fieldValue, expectedValue) < 0;

                case GREATER_THAN_OR_EQUAL:
                    return compareNumbers(fieldValue, expectedValue) >= 0;

                case LESS_THAN_OR_EQUAL:
                    return compareNumbers(fieldValue, expectedValue) <= 0;

                case CONTAINS:
                    return fieldValue != null && fieldValue.toString().contains(expectedValue.toString());

                case NOT_CONTAINS:
                    return fieldValue == null || !fieldValue.toString().contains(expectedValue.toString());

                case EXISTS:
                    return fieldValue != null;

                case NOT_EXISTS:
                    return fieldValue == null;

                case IS_NULL:
                    return fieldValue == null;

                case IS_NOT_NULL:
                    return fieldValue != null;

                case IN:
                    if (expectedValue instanceof Collection) {
                        return ((Collection<?>) expectedValue).contains(fieldValue);
                    }
                    return false;

                case NOT_IN:
                    if (expectedValue instanceof Collection) {
                        return !((Collection<?>) expectedValue).contains(fieldValue);
                    }
                    return true;

                case REGEX_MATCH:
                    return fieldValue != null &&
                            Pattern.matches(expectedValue.toString(), fieldValue.toString());

                case STARTS_WITH:
                    return fieldValue != null &&
                            fieldValue.toString().startsWith(expectedValue.toString());

                case ENDS_WITH:
                    return fieldValue != null &&
                            fieldValue.toString().endsWith(expectedValue.toString());

                case NOT_EMPTY:
                    return isNotEmpty(fieldValue);

                case IS_EMPTY:
                    return isEmpty(fieldValue);

                default:
                    log.error("Unsupported operator: {}", operator);
                    throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        } catch (Exception e) {
            log.error("Error evaluating condition: operator={}, fieldValue={}, expectedValue={}",
                    operator, fieldValue, expectedValue, e);
            throw e;
        }
    }

    private static boolean isNotEmpty(Object value) {
        if (value == null) return false;
        if (value instanceof String) return !((String) value).trim().isEmpty();
        if (value instanceof Collection) return !((Collection<?>) value).isEmpty();
        if (value instanceof Map) return !((Map<?, ?>) value).isEmpty();
        return true;
    }

    private static boolean isEmpty(Object value) {
        return !isNotEmpty(value);
    }

    private static int compareNumbers(Object value1, Object value2) {
        if (value1 instanceof Number && value2 instanceof Number) {
            double d1 = ((Number) value1).doubleValue();
            double d2 = ((Number) value2).doubleValue();
            return Double.compare(d1, d2);
        }
        log.error("Cannot compare non-numeric values: {} and {}", value1, value2);
        throw new IllegalArgumentException("Cannot compare non-numeric values: " + value1 + " and " + value2);
    }

    // Data classes
    @Data
    public static class Condition {
        private String field;
        private Operator operator;
        private Object value;

        public Condition() {}

        public Condition(String field, Operator operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        // Convenience constructor for backward compatibility with string operators
        public Condition(String field, String operator, Object value) {
            this.field = field;
            this.operator = Operator.valueOf(operator.toUpperCase());
            this.value = value;
        }
    }

    @Data
    public static class ConditionGroup {
        private LogicalOperator logic;
        private List<Condition> conditions;

        public ConditionGroup() {}

        public ConditionGroup(LogicalOperator logic, List<Condition> conditions) {
            this.logic = logic;
            this.conditions = conditions;
        }
    }

    // Wrapper class for REST API - can handle single condition or condition groups
    @Data
    public static class ConditionRequest {
        // Single condition (simplest case)
        private Condition condition;

        // Multiple conditions with logic
        private List<Condition> conditions;
        private LogicalOperator logic;

        // Complex condition groups
        private List<ConditionGroup> conditionGroups;
        private LogicalOperator groupLogic;

        public ConditionRequest() {}

        // Constructor for single condition
        public ConditionRequest(Condition condition) {
            this.condition = condition;
        }

        /**
         * Evaluate this condition request against data
         */
        public boolean evaluate(Map<String, Object> data) {
            return ConditionEvaluator.evaluate(this, data);
        }
    }
}