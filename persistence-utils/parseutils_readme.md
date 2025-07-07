# ParseUtils - Advanced Map Navigation and Filtering

A powerful utility class for navigating nested Map structures using dot notation and advanced filtering with condition-based array searching.

## Table of Contents
- [Overview](#overview)
- [Basic Methods](#basic-methods)
- [Dot Notation Navigation](#dot-notation-navigation)
- [Array Access](#array-access)
- [Condition-Based Filtering](#condition-based-filtering)
- [Method Calls with FreeMarker Style](#method-calls-with-freemarker-style)
- [Multiple Conditions with AND/OR Logic](#multiple-conditions-with-andor-logic)
- [Parentheses and Complex Grouping](#parentheses-and-complex-grouping)
- [Type Conversion Methods](#type-conversion-methods)
- [Error Handling](#error-handling)
- [Examples](#examples)

## Overview

ParseUtils enables powerful navigation of nested `Map<String, Object>` structures using:
- **Dot notation** for nested field access
- **Numeric indexing** for array access
- **Condition-based filtering** for dynamic array searching
- **Method calls** on objects using FreeMarker-style `?` syntax
- **Complex logical expressions** with AND/OR/parentheses support
- **Integration with ConditionEvaluator** for robust condition evaluation

## Basic Methods

### Core Access Methods
```java
// Get raw object
Object get(Map<String, Object> map, String attributeName)

// Type-safe accessors
String getString(Map<String, Object> map, String attributeName)
Map<String, Object> getMap(Map<String, Object> map, String attributeName)
List<Map<String, Object>> getList(Map<String, Object> map, String attributeName)
Integer getInteger(Map<String, Object> map, String attributeName)
Double getDouble(Map<String, Object> map, String attributeName)
Boolean getBoolean(Map<String, Object> map, String attributeName)
```

### Helper Methods
```java
// Get with default value
Object getOrDefault(Map<String, Object> map, String attributeName, Object defaultValue)

// Check existence
boolean exists(Map<String, Object> map, String attributeName)

// String with default
String getStringOrDefault(Map<String, Object> map, String attributeName, String defaultValue)
```

## Dot Notation Navigation

Access nested fields using dot notation:

```java
Map<String, Object> data = Map.of(
    "user", Map.of(
        "profile", Map.of(
            "name", "John Doe",
            "email", "john@example.com",
            "address", Map.of(
                "street", "123 Main St",
                "city", "New York",
                "zipCode", "10001"
            )
        )
    )
);

// Simple nested access
String name = ParseUtils.getString(data, "user.profile.name");
// Returns: "John Doe"

String email = ParseUtils.getString(data, "user.profile.email");
// Returns: "john@example.com"

// Deep nested access
String city = ParseUtils.getString(data, "user.profile.address.city");
// Returns: "New York"

String zipCode = ParseUtils.getString(data, "user.profile.address.zipCode");
// Returns: "10001"

// Get nested map
Map<String, Object> address = ParseUtils.getMap(data, "user.profile.address");
// Returns: {street=123 Main St, city=New York, zipCode=10001}
```

## Array Access

### Numeric Index Access
```java
Map<String, Object> data = Map.of(
    "users", Arrays.asList(
        Map.of("id", 1, "name", "Alice", "role", "admin"),
        Map.of("id", 2, "name", "Bob", "role", "user"),
        Map.of("id", 3, "name", "Charlie", "role", "moderator")
    ),
    "hobbies", Arrays.asList("reading", "swimming", "coding"),
    "scores", Arrays.asList(85, 92, 78, 95)
);

// Array index access
String firstUser = ParseUtils.getString(data, "users[0].name");
// Returns: "Alice"

String secondUserRole = ParseUtils.getString(data, "users[1].role");
// Returns: "user"

// Simple array elements
String hobby = ParseUtils.getString(data, "hobbies[1]");
// Returns: "swimming"

Integer score = ParseUtils.getInteger(data, "scores[3]");
// Returns: 95

// Nested array access
Map<String, Object> user = ParseUtils.getMap(data, "users[2]");
// Returns: {id=3, name=Charlie, role=moderator}
```

## Condition-Based Filtering

Filter arrays using conditions with ConditionEvaluator.Operator enums:

### Basic Conditions
```java
Map<String, Object> data = Map.of(
    "users", Arrays.asList(
        Map.of("id", 1, "name", "Alice", "role", "admin", "age", 30, "email", "alice@company_com"),
        Map.of("id", 2, "name", "Bob", "role", "user", "age", 25, "email", "bob@gmail.com"),
        Map.of("id", 3, "name", "Charlie", "role", "moderator", "age", 35, "email", "charlie@company_com")
    )
);

// Find by exact match
Map<String, Object> admin = ParseUtils.getMap(data, "users[role:EQUALS:admin]");
// Returns: {id=1, name=Alice, role=admin, age=30, email=alice@company_com}

// Find by numeric comparison
Map<String, Object> olderUser = ParseUtils.getMap(data, "users[age:GREATER_THAN:30]");
// Returns: {id=3, name=Charlie, role=moderator, age=35, email=charlie@company_com}

// Find by string operations
Map<String, Object> gmailUser = ParseUtils.getMap(data, "users[email:CONTAINS:gmail]");
// Returns: {id=2, name=Bob, role=user, age=25, email=bob@gmail.com}

// Find by field existence
Map<String, Object> userWithRole = ParseUtils.getMap(data, "users[role:EXISTS]");
// Returns: First user with role field (all users in this case)

// Chain access after filtering
String adminEmail = ParseUtils.getString(data, "users[role:EQUALS:admin].email");
// Returns: "alice@company_com"
```

### Available Operators
All ConditionEvaluator.Operator enums are supported:

```java
// Equality operators
users[field:EQUALS:value]
users[field:NOT_EQUALS:value]

// Comparison operators  
users[field:GREATER_THAN:value]
users[field:LESS_THAN:value]
users[field:GREATER_THAN_OR_EQUAL:value]
users[field:LESS_THAN_OR_EQUAL:value]

// String operators
users[field:CONTAINS:substring]
users[field:NOT_CONTAINS:substring]
users[field:STARTS_WITH:prefix]
users[field:ENDS_WITH:suffix]
users[field:REGEX_MATCH:pattern]

// Existence operators
users[field:EXISTS]
users[field:NOT_EXISTS]
users[field:IS_NULL]
users[field:IS_NOT_NULL]

// Collection operators
users[field:IS_EMPTY]
users[field:NOT_EMPTY]
users[field:IN:value1,value2,value3]
users[field:NOT_IN:value1,value2,value3]
```

## Method Calls with FreeMarker Style

Call methods on objects using `?` syntax (like FreeMarker templates):

```java
Map<String, Object> data = Map.of(
    "users", Arrays.asList(
        Map.of("id", 1, "name", "Alice", "segments", Arrays.asList("premium", "enterprise", "beta")),
        Map.of("id", 2, "name", "Bob", "segments", Arrays.asList("basic")),
        Map.of("id", 3, "name", "Charlie", "segments", Arrays.asList())
    )
);

// Find users with multiple segments
Map<String, Object> userWithMultipleSegments = ParseUtils.getMap(data, "users[segments?size:GREATER_THAN:1]");
// Returns: {id=1, name=Alice, segments=[premium, enterprise, beta]}

// Find users with exactly one segment
Map<String, Object> userWithOneSegment = ParseUtils.getMap(data, "users[segments?size:EQUALS:1]");
// Returns: {id=2, name=Bob, segments=[basic]}

// Find users with empty segments
Map<String, Object> userWithEmptySegments = ParseUtils.getMap(data, "users[segments?isEmpty:EQUALS:true]");
// Returns: {id=3, name=Charlie, segments=[]}

// String method calls
Map<String, Object> shortName = ParseUtils.getMap(data, "users[name?length:LESS_THAN:5]");
// Returns: User with name length < 5

// Chain method calls with field access
String emailOfUserWithMultipleSegments = ParseUtils.getString(data, "users[segments?size:GREATER_THAN:1].email");
// Returns: Email of first user with multiple segments
```

### Supported Methods
The method call system supports common methods efficiently and falls back to reflection:

**Efficient built-in methods:**
- `size()` - for Lists, Maps (String uses length())
- `length()` - for Strings (List uses size())
- `isEmpty()` - for Lists, Maps, Strings
- `toString()` - for any object
- `toLowerCase()` - for Strings
- `toUpperCase()` - for Strings
- `trim()` - for Strings

**Custom methods via reflection:**
- Any no-argument method on the object

## Multiple Conditions with AND/OR Logic

Combine multiple conditions using AND/OR operators:

```java
Map<String, Object> data = Map.of(
    "users", Arrays.asList(
        Map.of("id", 1, "name", "Alice", "role", "admin", "age", 30, "status", "active"),
        Map.of("id", 2, "name", "Bob", "role", "user", "age", 25, "status", "active"),
        Map.of("id", 3, "name", "Charlie", "role", "moderator", "age", 35, "status", "inactive"),
        Map.of("id", 4, "name", "Diana", "role", "admin", "age", 28, "status", "active")
    )
);

// AND logic (default)
Map<String, Object> activeAdmin = ParseUtils.getMap(data, 
    "users[role:EQUALS:admin AND status:EQUALS:active]");
// Returns: First active admin (Alice)

// OR logic  
Map<String, Object> adminOrModerator = ParseUtils.getMap(data, 
    "users[role:EQUALS:admin OR role:EQUALS:moderator]");
// Returns: First admin or moderator (Alice)

// Complex combinations
Map<String, Object> youngActiveUser = ParseUtils.getMap(data, 
    "users[age:LESS_THAN:30 AND status:EQUALS:active AND role:NOT_EQUALS:admin]");
// Returns: Bob (young, active, not admin)

// Mixed AND/OR with precedence (AND before OR)
Map<String, Object> specialUser = ParseUtils.getMap(data, 
    "users[role:EQUALS:admin AND age:GREATER_THAN:25 OR role:EQUALS:moderator AND status:EQUALS:active]");
// Evaluates as: (admin AND age>25) OR (moderator AND active)
```

## Parentheses and Complex Grouping

Use parentheses for explicit precedence control and complex logical expressions:

```java
Map<String, Object> data = Map.of(
    "users", Arrays.asList(
        Map.of("id", 1, "role", "admin", "age", 30, "status", "active", "email", "alice@company_com"),
        Map.of("id", 2, "role", "user", "age", 25, "status", "active", "email", "bob@gmail.com"),
        Map.of("id", 3, "role", "moderator", "age", 35, "status", "inactive", "email", "charlie@company_com"),
        Map.of("id", 4, "role", "vip", "age", 28, "status", "active", "email", "diana@gmail.com")
    )
);

// Force OR precedence with parentheses
Map<String, Object> privilegedYoungUser = ParseUtils.getMap(data, 
    "users[(role:EQUALS:admin OR role:EQUALS:moderator) AND age:LESS_THAN:32]");
// Returns: User who is (admin OR moderator) AND age < 32

// Group email conditions
Map<String, Object> activeGmailOrCompanyUser = ParseUtils.getMap(data, 
    "users[status:EQUALS:active AND (email:CONTAINS:gmail OR email:CONTAINS:company_com)]");
// Returns: Active user with either Gmail or company email

// Complex nested grouping
Map<String, Object> complexFilter = ParseUtils.getMap(data, 
    "users[(role:EQUALS:admin AND age:GREATER_THAN:25) OR (role:EQUALS:vip AND email:CONTAINS:gmail)]");
// Returns: User who is either (admin AND age>25) OR (vip AND has Gmail)

// Very complex business logic
Map<String, Object> targetUser = ParseUtils.getMap(data, 
    "users[((role:EQUALS:admin OR role:EQUALS:moderator) AND status:EQUALS:active) OR (role:EQUALS:vip AND age:LESS_THAN:30)]");
// Returns: User matching complex business rules

// Method calls in grouped conditions
Map<String, Object> segmentedUser = ParseUtils.getMap(data, 
    "users[(role:EQUALS:admin AND segments?size:GREATER_THAN:1) OR (status:EQUALS:vip AND orders?size:GREATER_THAN:5)]");
// Returns: User with multiple segments OR VIP with many orders
```

### Precedence Rules
1. **Parentheses** - Highest precedence
2. **AND** - Medium precedence
3. **OR** - Lowest precedence
4. **Left-to-right** evaluation within same precedence level

## Type Conversion Methods

ParseUtils provides automatic type conversion with null safety:

```java
Map<String, Object> data = Map.of(
    "user", Map.of(
        "id", 123,
        "name", "John Doe",
        "age", 30,
        "salary", 75000.50,
        "isActive", true,
        "scoreStr", "95",
        "ratingStr", "4.5"
    )
);

// Automatic type conversion
Integer id = ParseUtils.getInteger(data, "user.id");
// Returns: 123

String name = ParseUtils.getString(data, "user.name");
// Returns: "John Doe"

Double salary = ParseUtils.getDouble(data, "user.salary");
// Returns: 75000.5

Boolean active = ParseUtils.getBoolean(data, "user.isActive");
// Returns: true

// String to number conversion
Integer score = ParseUtils.getInteger(data, "user.scoreStr");
// Returns: 95

Double rating = ParseUtils.getDouble(data, "user.ratingStr");
// Returns: 4.5

// Safe access with defaults
String nickname = ParseUtils.getStringOrDefault(data, "user.nickname", "Anonymous");
// Returns: "Anonymous" (field doesn't exist)

Object department = ParseUtils.getOrDefault(data, "user.department", "Engineering");
// Returns: "Engineering" (field doesn't exist)

// Existence checks
boolean hasAge = ParseUtils.exists(data, "user.age");
// Returns: true

boolean hasMiddleName = ParseUtils.exists(data, "user.middleName");
// Returns: false
```

## Error Handling

ParseUtils handles errors gracefully and returns null for invalid paths:

```java
Map<String, Object> data = Map.of(
    "users", Arrays.asList(
        Map.of("id", 1, "name", "Alice")
    )
);

// Null safety
String result1 = ParseUtils.getString(null, "any.path");
// Returns: null

String result2 = ParseUtils.getString(data, "nonexistent.field");
// Returns: null

// Array bounds safety
String result3 = ParseUtils.getString(data, "users[10].name");
// Returns: null (index out of bounds)

// Type safety
String result4 = ParseUtils.getString(data, "users[0]");
// Returns: "{id=1, name=Alice}" (converts map to string)

// Invalid condition safety
Map<String, Object> result5 = ParseUtils.getMap(data, "users[invalid_condition]");
// Returns: null (logs error)

// Method call safety
Map<String, Object> result6 = ParseUtils.getMap(data, "users[name?nonexistentMethod:EQUALS:value]");
// Returns: null (method not found)
```

## Examples

### E-commerce User Management

```java
Map<String, Object> ecommerceData = Map.of(
    "customers", Arrays.asList(
        Map.of(
            "id", 1,
            "name", "Alice Johnson",
            "email", "alice@gmail.com",
            "status", "premium",
            "age", 32,
            "orders", Arrays.asList("order1", "order2", "order3"),
            "addresses", Arrays.asList(
                Map.of("type", "home", "city", "New York"),
                Map.of("type", "work", "city", "Boston")
            ),
            "preferences", Map.of("newsletter", true, "sms", false)
        ),
        Map.of(
            "id", 2,
            "name", "Bob Smith",
            "email", "bob@company_com",
            "status", "regular",
            "age", 28,
            "orders", Arrays.asList("order4"),
            "addresses", Arrays.asList(
                Map.of("type", "home", "city", "Chicago")
            ),
            "preferences", Map.of("newsletter", false, "sms", true)
        )
    )
);

// Find premium customers with multiple orders
String premiumCustomerEmail = ParseUtils.getString(ecommerceData, 
    "customers[status:EQUALS:premium AND orders?size:GREATER_THAN:2].email");
// Returns: "alice@gmail.com"

// Find customers with Gmail and multiple addresses
Map<String, Object> gmailCustomerWithMultipleAddresses = ParseUtils.getMap(ecommerceData, 
    "customers[email:CONTAINS:gmail AND addresses?size:GREATER_THAN:1]");
// Returns: Alice's data

// Complex business logic: Premium customers OR customers with company email and newsletter preference
Map<String, Object> targetCustomer = ParseUtils.getMap(ecommerceData, 
    "customers[status:EQUALS:premium OR (email:CONTAINS:company AND preferences.newsletter:EQUALS:true)]");

// Get home address city of premium customer
String homeCity = ParseUtils.getString(ecommerceData, 
    "customers[status:EQUALS:premium].addresses[type:EQUALS:home].city");
// Returns: "New York"
```

### User Role and Permission Management

```java
Map<String, Object> organizationData = Map.of(
    "employees", Arrays.asList(
        Map.of(
            "id", 101,
            "name", "Sarah Admin",
            "role", "admin",
            "department", "IT",
            "permissions", Arrays.asList("read", "write", "delete", "admin"),
            "projects", Arrays.asList("project1", "project2"),
            "active", true
        ),
        Map.of(
            "id", 102,
            "name", "John Developer",
            "role", "developer",
            "department", "Engineering",
            "permissions", Arrays.asList("read", "write"),
            "projects", Arrays.asList("project1", "project3", "project4"),
            "active", true
        ),
        Map.of(
            "id", 103,
            "name", "Mike Manager",
            "role", "manager",
            "department", "Engineering",
            "permissions", Arrays.asList("read", "write", "manage"),
            "projects", Arrays.asList("project2"),
            "active", false
        )
    )
);

// Find active employees with admin permissions
String adminName = ParseUtils.getString(organizationData, 
    "employees[active:EQUALS:true AND permissions:CONTAINS:admin].name");
// Returns: "Sarah Admin"

// Find Engineering employees with multiple projects
Map<String, Object> busyEngineer = ParseUtils.getMap(organizationData, 
    "employees[department:EQUALS:Engineering AND projects?size:GREATER_THAN:2]");
// Returns: John Developer's data

// Complex access control: Active admins OR active managers in Engineering
Map<String, Object> privilegedUser = ParseUtils.getMap(organizationData, 
    "employees[(role:EQUALS:admin AND active:EQUALS:true) OR (role:EQUALS:manager AND department:EQUALS:Engineering AND active:EQUALS:true)]");

// Find employees who can write and have more than 1 project
List<Map<String, Object>> productiveEmployees = ParseUtils.getList(organizationData, 
    "employees[permissions:CONTAINS:write AND projects?size:GREATER_THAN:1]");
```

### Configuration and Settings Management

```java
Map<String, Object> configData = Map.of(
    "applications", Arrays.asList(
        Map.of(
            "name", "web-app",
            "environment", "production",
            "settings", Map.of(
                "database", Map.of("host", "prod-db.example.com", "port", 5432),
                "cache", Map.of("enabled", true, "ttl", 300),
                "features", Arrays.asList("feature1", "feature2", "feature3")
            ),
            "health", Map.of("status", "healthy", "uptime", 99.9)
        ),
        Map.of(
            "name", "api-service",
            "environment", "staging", 
            "settings", Map.of(
                "database", Map.of("host", "staging-db.example.com", "port", 5432),
                "cache", Map.of("enabled", false, "ttl", 60),
                "features", Arrays.asList("feature1")
            ),
            "health", Map.of("status", "degraded", "uptime", 95.2)
        )
    )
);

// Get production database host
String prodDbHost = ParseUtils.getString(configData, 
    "applications[environment:EQUALS:production].settings.database.host");
// Returns: "prod-db.example.com"

// Find applications with cache enabled and multiple features
Map<String, Object> cachedAppWithFeatures = ParseUtils.getMap(configData, 
    "applications[settings.cache.enabled:EQUALS:true AND settings.features?size:GREATER_THAN:2]");
// Returns: web-app data

// Find healthy applications in production OR any application with high uptime
Map<String, Object> reliableApp = ParseUtils.getMap(configData, 
    "applications[(environment:EQUALS:production AND health.status:EQUALS:healthy) OR health.uptime:GREATER_THAN:99]");

// Get cache TTL for staging environment
Integer stagingCacheTtl = ParseUtils.getInteger(configData, 
    "applications[environment:EQUALS:staging].settings.cache.ttl");
// Returns: 60
```

## Legacy Support

ParseUtils maintains backward compatibility with legacy `containsKey` syntax:

```java
// Legacy format (still supported)
Map<String, Object> user = ParseUtils.getMap(data, "users[containsKey=role]");

// Modern equivalent
Map<String, Object> user = ParseUtils.getMap(data, "users[role:EXISTS]");
```

## Performance Notes

- **Method calls** use efficient built-in implementations for common methods (size, length, isEmpty)
- **Reflection fallback** for custom methods with caching
- **Short-circuit evaluation** in AND/OR expressions
- **Null-safe** operations throughout
- **Type coercion** with minimal overhead

## Integration with ConditionEvaluator

ParseUtils leverages the existing ConditionEvaluator class for all condition evaluation:
- **Consistent behavior** across direct ConditionEvaluator usage and ParseUtils
- **Automatic support** for new operators added to ConditionEvaluator
- **Robust type handling** and edge case management
- **Complex logical expressions** with proper precedence

---

**Note**: All operators must be valid `ConditionEvaluator.Operator` enum values. The system is future-proof - any new operators added to ConditionEvaluator automatically work with ParseUtils without code changes.