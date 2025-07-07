package lazydevs.persistence.util;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Complete TestNG test suite for the improved ParseUtils with consistent array behavior
 * Tests all functionality including the new consistent array filtering behavior
 */
public class ParseUtilsTest {

    private Map<String, Object> testData;
    private Map<String, Object> ecommerceData;
    private Map<String, Object> organizationData;
    private Map<String, Object> configData;

    @BeforeMethod
    public void setUp() {
        testData = createBasicTestData();
        ecommerceData = createEcommerceTestData();
        organizationData = createOrganizationTestData();
        configData = createConfigTestData();
    }

    // ========== TEST DATA CREATION ==========

    private Map<String, Object> createBasicTestData() {
        Map<String, Object> data = new HashMap<>();

        // Simple fields
        data.put("name", "John Doe");
        data.put("age", 30);
        data.put("salary", 75000.50);
        data.put("isActive", true);
        data.put("nullField", null);
        data.put("emptyString", "");

        // Nested object
        Map<String, Object> address = new HashMap<>();
        address.put("street", "123 Main St");
        address.put("city", "New York");
        address.put("zipCode", "10001");
        data.put("address", address);

        // Array of strings
        data.put("hobbies", Arrays.asList("reading", "swimming", "coding"));

        // Array of numbers
        data.put("scores", Arrays.asList(85, 92, 78, 95));

        // Array of objects - users with different characteristics
        List<Map<String, Object>> users = new ArrayList<>();

        // User 1 - admin with 3 segments (premium, enterprise, beta)
        Map<String, Object> user1 = new HashMap<>();
        user1.put("id", 1);
        user1.put("name", "Alice Admin");
        user1.put("role", "admin");
        user1.put("age", 35);
        user1.put("email", "alice@company_com");
        user1.put("status", "active");
        user1.put("segments", Arrays.asList("premium", "enterprise", "beta"));
        user1.put("addresses", Arrays.asList(
                Map.of("type", "home", "city", "New York"),
                Map.of("type", "work", "city", "San Francisco")
        ));
        user1.put("orders", Arrays.asList("order1", "order2", "order3", "order4"));
        users.add(user1);

        // User 2 - regular user with 1 segment (basic)
        Map<String, Object> user2 = new HashMap<>();
        user2.put("id", 2);
        user2.put("name", "Bob User");
        user2.put("role", "user");
        user2.put("age", 25);
        user2.put("email", "bob@gmail.com");
        user2.put("status", "active");
        user2.put("segments", Arrays.asList("basic"));
        user2.put("addresses", Arrays.asList(Map.of("type", "home", "city", "Boston")));
        user2.put("orders", Arrays.asList("order5", "order6"));
        users.add(user2);

        // User 3 - inactive user with 0 segments
        Map<String, Object> user3 = new HashMap<>();
        user3.put("id", 3);
        user3.put("name", "Charlie Inactive");
        user3.put("role", "user");
        user3.put("age", 45);
        user3.put("email", "charlie@yahoo.com");
        user3.put("status", "inactive");
        user3.put("segments", Arrays.asList());
        user3.put("addresses", Arrays.asList());
        user3.put("orders", Arrays.asList());
        users.add(user3);

        // User 4 - moderator with exactly 2 segments (premium, support)
        Map<String, Object> user4 = new HashMap<>();
        user4.put("id", 4);
        user4.put("name", "Diana Moderator");
        user4.put("role", "moderator");
        user4.put("age", 28);
        user4.put("email", "diana@company_com");
        user4.put("status", "active");
        user4.put("segments", Arrays.asList("premium", "support"));
        user4.put("addresses", Arrays.asList(
                Map.of("type", "home", "city", "Chicago"),
                Map.of("type", "work", "city", "Chicago"),
                Map.of("type", "vacation", "city", "Miami")
        ));
        user4.put("orders", Arrays.asList("order7"));
        users.add(user4);

        // User 5 - another admin with 2 segments (admin, special)
        Map<String, Object> user5 = new HashMap<>();
        user5.put("id", 5);
        user5.put("name", "Eve SecondAdmin");
        user5.put("role", "admin");
        user5.put("age", 30);
        user5.put("email", "eve@company_com");
        user5.put("status", "active");
        user5.put("segments", Arrays.asList("admin", "special"));
        user5.put("addresses", Arrays.asList(Map.of("type", "home", "city", "Seattle")));
        user5.put("orders", Arrays.asList("order8", "order9"));
        users.add(user5);

        data.put("users", users);

        // Deeply nested structure
        Map<String, Object> company = new HashMap<>();
        Map<String, Object> department = new HashMap<>();
        Map<String, Object> team = new HashMap<>();
        team.put("name", "Engineering");
        team.put("size", 15);
        department.put("backend", team);
        company.put("engineering", department);
        data.put("company", company);

        return data;
    }

    private Map<String, Object> createEcommerceTestData() {
        return Map.of(
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
                        ),
                        Map.of(
                                "id", 3,
                                "name", "Carol Premium",
                                "email", "carol@gmail.com",
                                "status", "premium",
                                "age", 29,
                                "orders", Arrays.asList("order10", "order11", "order12", "order13"),
                                "addresses", Arrays.asList(
                                        Map.of("type", "home", "city", "Los Angeles")
                                ),
                                "preferences", Map.of("newsletter", true, "sms", true)
                        )
                )
        );
    }

    private Map<String, Object> createOrganizationTestData() {
        return Map.of(
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
                        ),
                        Map.of(
                                "id", 104,
                                "name", "Lisa Admin",
                                "role", "admin",
                                "department", "Security",
                                "permissions", Arrays.asList("read", "write", "delete", "admin", "security"),
                                "projects", Arrays.asList("project5", "project6"),
                                "active", true
                        )
                )
        );
    }

    private Map<String, Object> createConfigTestData() {
        return Map.of(
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
                        ),
                        Map.of(
                                "name", "worker-service",
                                "environment", "production",
                                "settings", Map.of(
                                        "database", Map.of("host", "prod-db.example.com", "port", 5432),
                                        "cache", Map.of("enabled", true, "ttl", 600),
                                        "features", Arrays.asList("feature1", "feature4")
                                ),
                                "health", Map.of("status", "healthy", "uptime", 98.5)
                        )
                )
        );
    }

    // ========== BASIC FUNCTIONALITY TESTS ==========

    @Test
    public void testSimpleFieldAccess() {
        assertEquals(ParseUtils.getString(testData, "name"), "John Doe");
        assertEquals(ParseUtils.getInteger(testData, "age"), Integer.valueOf(30));
        assertEquals(ParseUtils.getDouble(testData, "salary"), Double.valueOf(75000.50));
        assertEquals(ParseUtils.getBoolean(testData, "isActive"), Boolean.TRUE);
    }

    @Test
    public void testNestedFieldAccess() {
        assertEquals(ParseUtils.getString(testData, "address.street"), "123 Main St");
        assertEquals(ParseUtils.getString(testData, "address.city"), "New York");
        assertEquals(ParseUtils.getString(testData, "address.zipCode"), "10001");
    }

    @Test
    public void testArrayIndexAccess() {
        assertEquals(ParseUtils.getString(testData, "hobbies[0]"), "reading");
        assertEquals(ParseUtils.getString(testData, "hobbies[1]"), "swimming");
        assertEquals(ParseUtils.getString(testData, "hobbies[2]"), "coding");
        assertEquals(ParseUtils.getInteger(testData, "scores[0]"), Integer.valueOf(85));
        assertEquals(ParseUtils.getInteger(testData, "scores[3]"), Integer.valueOf(95));
    }

    @Test
    public void testArrayObjectAccess() {
        assertEquals(ParseUtils.getInteger(testData, "users[0].id"), Integer.valueOf(1));
        assertEquals(ParseUtils.getString(testData, "users[0].name"), "Alice Admin");
        assertEquals(ParseUtils.getString(testData, "users[1].role"), "user");
        assertEquals(ParseUtils.getString(testData, "users[2].status"), "inactive");
    }

    // ========== NEW CONSISTENT ARRAY BEHAVIOR TESTS ==========

    @Test
    public void testArrayFilteringReturnsListWhenLastToken() {
        // Test: users[condition] should return List<Map> when it's the last token

        // Get all admin users (should return List)
        Object adminUsersObj = ParseUtils.get(testData, "users[role:EQUALS:admin]");
        assertNotNull(adminUsersObj);
        assertTrue(adminUsersObj instanceof List, "Should return List when condition is last token");

        List<Map<String, Object>> adminUsers = (List<Map<String, Object>>) adminUsersObj;
        assertEquals(adminUsers.size(), 2, "Should find 2 admin users (Alice and Eve)");

        // Verify the admin users
        Set<String> adminNames = adminUsers.stream()
                .map(user -> (String) user.get("name"))
                .collect(Collectors.toSet());
        assertTrue(adminNames.contains("Alice Admin"));
        assertTrue(adminNames.contains("Eve SecondAdmin"));
    }

    @Test
    public void testArrayFilteringWithMethodCallsReturnsListWhenLastToken() {
        // Test: users[segments?size:EQUALS:2] should return List of users with exactly 2 segments

        Object usersWithTwoSegmentsObj = ParseUtils.get(testData, "users[segments?size:EQUALS:2]");
        assertNotNull(usersWithTwoSegmentsObj);
        assertTrue(usersWithTwoSegmentsObj instanceof List, "Should return List when condition is last token");

        List<Map<String, Object>> usersWithTwoSegments = (List<Map<String, Object>>) usersWithTwoSegmentsObj;
        assertEquals(usersWithTwoSegments.size(), 2, "Should find 2 users with exactly 2 segments (Diana and Eve)");

        // Verify the users
        Set<String> userNames = usersWithTwoSegments.stream()
                .map(user -> (String) user.get("name"))
                .collect(Collectors.toSet());
        assertTrue(userNames.contains("Diana Moderator"));
        assertTrue(userNames.contains("Eve SecondAdmin"));

        // Verify they actually have 2 segments
        for (Map<String, Object> user : usersWithTwoSegments) {
            List<?> segments = (List<?>) user.get("segments");
            assertEquals(segments.size(), 2, "Each user should have exactly 2 segments");
        }
    }

    @Test
    public void testArrayFilteringWithContinuationUsesFirstMatch() {
        // Test: users[condition].field should use first match when continuing navigation

        // Get name of first admin user
        String firstAdminName = ParseUtils.getString(testData, "users[role:EQUALS:admin].name");
        assertEquals(firstAdminName, "Alice Admin", "Should return name of first admin user");

        // Get email of first user with multiple segments
        String firstUserWithSegmentsEmail = ParseUtils.getString(testData, "users[segments?size:GREATER_THAN:1].email");
        assertEquals(firstUserWithSegmentsEmail, "alice@company_com", "Should return email of first user with multiple segments");

        // Get age of first active user
        Integer firstActiveUserAge = ParseUtils.getInteger(testData, "users[status:EQUALS:active].age");
        assertEquals(firstActiveUserAge, Integer.valueOf(35), "Should return age of first active user");
    }

    @Test
    public void testExplicitIndexAccessAfterFiltering() {
        // Test: users[condition][index] should work for explicit index access on filtered results

        // Get second admin user explicitly
        Map<String, Object> secondAdmin = ParseUtils.getList(testData, "users[role:EQUALS:admin]").get(1);
        assertNotNull(secondAdmin);
        assertEquals(secondAdmin.get("name"), "Eve SecondAdmin", "Should return second admin user");

        // Get name of second admin user
        String secondAdminName = (String)ParseUtils.getList(testData, "users[role:EQUALS:admin]").get(1).get("name");
        assertEquals(secondAdminName, "Eve SecondAdmin", "Should return name of second admin user");

    }

    @Test
    public void testComplexFilteringReturnsAppropriateResults() {
        // Test complex conditions return correct lists

        // Get all active users with company email
        List<Map<String, Object>> companyUsers = ParseUtils.getList(testData,
                "users[status:EQUALS:active AND email:CONTAINS:company_com]");
        assertNotNull(companyUsers);
        assertEquals(companyUsers.size(), 3, "Should find 3 active users with company email");

        // Verify all results match the condition
        for (Map<String, Object> user : companyUsers) {
            assertEquals(user.get("status"), "active");
            assertTrue(((String) user.get("email")).contains("company_com"));
        }

        // Get all users with more than 1 segment
        List<Map<String, Object>> usersWithMultipleSegments = ParseUtils.getList(testData,
                "users[segments?size:GREATER_THAN:1]");
        assertNotNull(usersWithMultipleSegments);
        assertEquals(usersWithMultipleSegments.size(), 3, "Should find 3 users with multiple segments");

        // Verify all results have multiple segments
        for (Map<String, Object> user : usersWithMultipleSegments) {
            List<?> segments = (List<?>) user.get("segments");
            assertTrue(segments.size() > 1, "Each user should have more than 1 segment");
        }
    }

    @Test
    public void testParenthesesWithArrayFiltering() {
        // Test complex conditions with parentheses return correct lists

        List<Map<String, Object>> privilegedUsers = ParseUtils.getList(testData,
                "users[(role:EQUALS:admin OR role:EQUALS:moderator) AND status:EQUALS:active]");
        assertNotNull(privilegedUsers);
        assertEquals(privilegedUsers.size(), 3, "Should find 3 privileged active users");

        // Verify all results match the condition
        for (Map<String, Object> user : privilegedUsers) {
            String role = (String) user.get("role");
            String status = (String) user.get("status");
            assertTrue(Arrays.asList("admin", "moderator").contains(role));
            assertEquals(status, "active");
        }

        // Get first privileged user's name
        String firstPrivilegedUserName = ParseUtils.getString(testData,
                "users[(role:EQUALS:admin OR role:EQUALS:moderator) AND status:EQUALS:active].name");
        assertEquals(firstPrivilegedUserName, "Alice Admin");
    }

    // ========== HELPER METHOD TESTS ==========

    @Test
    public void testGetListHelperMethod() {
        // Test getList() helper method works correctly with new behavior

        List<Map<String, Object>> allUsers = ParseUtils.getList(testData, "users");
        assertNotNull(allUsers);
        assertEquals(allUsers.size(), 5, "Should return all 5 users");

        List<Map<String, Object>> adminUsers = ParseUtils.getList(testData, "users[role:EQUALS:admin]");
        assertNotNull(adminUsers);
        assertEquals(adminUsers.size(), 2, "Should return 2 admin users");

        // Test that getList returns null for non-list results
        List<Map<String, Object>> singleUser = ParseUtils.getList(testData, "users[0]");
        assertNull(singleUser, "Should return null when result is not a List");

        List<Map<String, Object>> userName = ParseUtils.getList(testData, "users[0].name");
        assertNull(userName, "Should return null when result is not a List");
    }

    @Test
    public void testGetMapHelperMethod() {
        // Test getMap() helper method works correctly with new behavior

        Map<String, Object> firstUser = ParseUtils.getMap(testData, "users[0]");
        assertNotNull(firstUser);
        assertEquals(firstUser.get("name"), "Alice Admin");

        Map<String, Object> firstAdmin = ParseUtils.getMap(testData, "users[role:EQUALS:admin].[0]");
        assertNotNull(firstAdmin);
        assertEquals(firstAdmin.get("name"), "Alice Admin");

        // Test that getMap returns null for list results
        Map<String, Object> userList = ParseUtils.getMap(testData, "users[role:EQUALS:admin]");
        assertNull(userList, "Should return null when result is a List");

        Map<String, Object> stringResult = ParseUtils.getMap(testData, "users[0].name");
        assertNull(stringResult, "Should return null when result is not a Map");
    }

    // ========== E-COMMERCE SCENARIO TESTS ==========

    @Test
    public void testEcommerceScenarios() {
        // Get all premium customers (should return List)
        List<Map<String, Object>> premiumCustomers = ParseUtils.getList(ecommerceData,
                "customers[status:EQUALS:premium]");
        assertNotNull(premiumCustomers);
        assertEquals(premiumCustomers.size(), 2, "Should find 2 premium customers");

        // Get all customer names for premium customers
        List<String> premiumCustomerNames = premiumCustomers.stream()
                .map(customer -> (String) customer.get("name"))
                .collect(Collectors.toList());
        assertTrue(premiumCustomerNames.contains("Alice Johnson"));
        assertTrue(premiumCustomerNames.contains("Carol Premium"));

        // Get first premium customer's email
        String firstPremiumEmail = ParseUtils.getString(ecommerceData,
                "customers[status:EQUALS:premium].email");
        assertEquals(firstPremiumEmail, "alice@gmail.com");

        // Get all customers with multiple orders
        List<Map<String, Object>> customersWithMultipleOrders = ParseUtils.getList(ecommerceData,
                "customers[orders?size:GREATER_THAN:2]");
        assertNotNull(customersWithMultipleOrders);
        assertEquals(customersWithMultipleOrders.size(), 2, "Should find 2 customers with multiple orders");

        // Get second customer with multiple orders
        Map<String, Object> secondCustomerWithOrders = ParseUtils.getList(ecommerceData,
                "customers[orders?size:GREATER_THAN:2]").get(1);
        assertNotNull(secondCustomerWithOrders);
        assertEquals(secondCustomerWithOrders.get("name"), "Carol Premium");
    }

    // ========== ORGANIZATION SCENARIO TESTS ==========

    @Test
    public void testOrganizationScenarios() {
        // Get all admin employees
        List<Map<String, Object>> adminEmployees = ParseUtils.getList(organizationData,
                "employees[role:EQUALS:admin]");
        assertNotNull(adminEmployees);
        assertEquals(adminEmployees.size(), 2, "Should find 2 admin employees");

        // Get all active employees with multiple projects
        List<Map<String, Object>> busyEmployees = ParseUtils.getList(organizationData,
                "employees[active:EQUALS:true AND projects?size:GREATER_THAN:1]");
        assertNotNull(busyEmployees);
        assertEquals(busyEmployees.size(), 3, "Should find 3 busy active employees");

        // Get first busy employee's name
        String firstBusyEmployeeName = ParseUtils.getString(organizationData,
                "employees[active:EQUALS:true AND projects?size:GREATER_THAN:1].name");
        assertEquals(firstBusyEmployeeName, "Sarah Admin");

        // Get all employees in Engineering department
        List<Map<String, Object>> engineeringEmployees = ParseUtils.getList(organizationData,
                "employees[department:EQUALS:Engineering]");
        assertNotNull(engineeringEmployees);
        assertEquals(engineeringEmployees.size(), 2, "Should find 2 Engineering employees");
    }

    // ========== CONFIGURATION SCENARIO TESTS ==========

    @Test
    public void testConfigurationScenarios() {
        // Get all production applications
        List<Map<String, Object>> prodApps = ParseUtils.getList(configData,
                "applications[environment:EQUALS:production]");
        assertNotNull(prodApps);
        assertEquals(prodApps.size(), 2, "Should find 2 production applications");

        // Get all applications with cache enabled
        List<Map<String, Object>> cachedApps = ParseUtils.getList(configData,
                "applications[settings?cache?enabled:EQUALS:true]");
        assertNotNull(cachedApps);
        assertEquals(cachedApps.size(), 2, "Should find 2 applications with cache enabled");

        // Get first production app's database host
        String prodDbHost = ParseUtils.getString(configData,
                "applications[environment:EQUALS:production].settings.database.host");
        assertEquals(prodDbHost, "prod-db.example.com");

        // Get all healthy applications
        List<Map<String, Object>> healthyApps = ParseUtils.getList(configData,
                "applications[health?status:EQUALS:healthy]");
        assertNotNull(healthyApps);
        assertEquals(healthyApps.size(), 2, "Should find 2 healthy applications");
    }

    // ========== CONDITION OPERATOR TESTS ==========

    @DataProvider(name = "filteringTestData")
    public Object[][] filteringTestData() {
        return new Object[][]{
                // {condition, expectedCount, description}
                {"role:EQUALS:admin", 2, "admin users"},
                {"role:EQUALS:user", 2, "regular users"},
                {"status:EQUALS:active", 4, "active users"},
                {"age:GREATER_THAN:30", 2, "users over 30"},
                {"age:LESS_THAN:30", 2, "users under 30"},
                {"segments?size:EQUALS:2", 2, "users with exactly 2 segments"},
                {"segments?size:GREATER_THAN:1", 3, "users with multiple segments"},
                {"segments?size:EQUALS:0", 1, "users with no segments"},
                {"email:CONTAINS:company_com", 2, "users with company email"},
                {"email:CONTAINS:gmail.com", 1, "users with gmail"},
                {"orders?size:GREATER_THAN:2", 2, "users with multiple orders"},
                {"addresses?size:GREATER_THAN:1", 2, "users with multiple addresses"}
        };
    }

    @Test(dataProvider = "filteringTestData")
    public void testSystematicFiltering(String condition, int expectedCount, String description) {
        List<Map<String, Object>> results = ParseUtils.getList(testData, "users[" + condition + "]");
        assertNotNull(results, "Should find results for: " + description);
        assertEquals(results.size(), expectedCount,
                "Should find " + expectedCount + " " + description + " with condition: " + condition);
    }

    @Test
    public void testComplexMultipleConditions() {
        // Test complex AND/OR combinations return correct lists

        // Active admins OR active moderators
        List<Map<String, Object>> privilegedActiveUsers = ParseUtils.getList(testData,
                "users[(role:EQUALS:admin OR role:EQUALS:moderator) AND status:EQUALS:active]");
        assertEquals(privilegedActiveUsers.size(), 3);

        // Users with either multiple segments OR company email
        List<Map<String, Object>> targetUsers = ParseUtils.getList(testData,
                "users[segments?size:GREATER_THAN:1 OR email:CONTAINS:company_com]");
        assertEquals(targetUsers.size(), 3);

        // Young active users with gmail OR company email
        List<Map<String, Object>> youngEmailUsers = ParseUtils.getList(testData,
                "users[age:LESS_THAN:35 AND status:EQUALS:active AND (email:CONTAINS:gmail OR email:CONTAINS:company_com)]");
        assertEquals(youngEmailUsers.size(), 3);
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    public void testErrorHandlingWithNewBehavior() {
        // Test that error conditions still return null appropriately

        // Invalid conditions should return empty lists or null
        List<Map<String, Object>> invalidResults = ParseUtils.getList(testData, "users[invalid_condition]");
        assertEquals(invalidResults.size(), 0);

        // Out of bounds on filtered results
        Map<String, Object> outOfBounds = ParseUtils.getMap(testData, "users[10]");
        assertNull(outOfBounds, "Out of bounds access should return null");

        // Non-existent field access after filtering
        String nonExistentField = ParseUtils.getString(testData, "users[role:EQUALS:admin].nonExistentField");
        assertNull(nonExistentField, "Non-existent field should return null");

        // Invalid method calls
        List<Map<String, Object>> invalidMethod = ParseUtils.getList(testData, "users[name?invalidMethod:EQUALS:value]");
        assertEquals(invalidMethod.size(), 0, "Invalid method should return null");
    }

    @Test
    public void testNullAndEmptyHandling() {
        // Test null safety with new behavior
        assertNull(ParseUtils.get(null, "users[condition]"));
        assertNull(ParseUtils.getList(null, "users[condition]"));
        assertNull(ParseUtils.getMap(null, "users[condition]"));

        // Test empty conditions
        assertNull(ParseUtils.getList(testData, "users[]"));

        // Test conditions that match nothing
        List<Map<String, Object>> noMatches = ParseUtils.getList(testData, "users[role:EQUALS:nonexistent]");
        assertNotNull(noMatches, "Should return empty list, not null");
        assertTrue(noMatches.isEmpty(), "Should return empty list when no matches found");

        // Test field access on empty results
        String nameFromEmptyResults = ParseUtils.getString(testData, "users[role:EQUALS:nonexistent].name");
        assertNull(nameFromEmptyResults, "Should return null when no matches to get field from");
    }

    // ========== TYPE CONVERSION TESTS ==========

    @Test
    public void testTypeConversions() {
        Map<String, Object> typeTestData = new HashMap<>();
        typeTestData.put("stringNumber", "123");
        typeTestData.put("stringDouble", "123.45");
        typeTestData.put("stringBoolean", "true");
        typeTestData.put("numberAsString", 456);

        // String to Integer
        assertEquals(ParseUtils.getInteger(typeTestData, "stringNumber"), Integer.valueOf(123));

        // String to Double
        assertEquals(ParseUtils.getDouble(typeTestData, "stringDouble"), Double.valueOf(123.45));

        // String to Boolean
        assertEquals(ParseUtils.getBoolean(typeTestData, "stringBoolean"), Boolean.TRUE);

        // Number to String
        assertEquals(ParseUtils.getString(typeTestData, "numberAsString"), "456");

        // Invalid conversions should return null
        assertNull(ParseUtils.getBoolean(typeTestData, "stringNumber"));
    }

    @Test
    public void testHelperMethods() {
        // getOrDefault
        assertEquals(ParseUtils.getOrDefault(testData, "name", "default"), "John Doe");
        assertEquals(ParseUtils.getOrDefault(testData, "nonExistent", "default"), "default");

        // exists
        assertTrue(ParseUtils.exists(testData, "name"));
        assertFalse(ParseUtils.exists(testData, "nonExistent"));

        // getStringOrDefault
        assertEquals(ParseUtils.getStringOrDefault(testData, "name", "default"), "John Doe");
        assertEquals(ParseUtils.getStringOrDefault(testData, "nonExistent", "default"), "default");
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    public void testLargeDatasetFiltering() {
        // Create a larger dataset for performance testing
        List<Map<String, Object>> largeUserList = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", i);
            user.put("name", "User" + i);
            user.put("age", 20 + (i % 50));
            user.put("role", i % 3 == 0 ? "admin" : (i % 3 == 1 ? "user" : "moderator"));
            user.put("status", i % 4 == 0 ? "inactive" : "active");
            user.put("segments", Arrays.asList("segment" + (i % 5)));
            largeUserList.add(user);
        }

        Map<String, Object> largeData = Map.of("users", largeUserList);

        // Test filtering performance
        long startTime = System.currentTimeMillis();
        List<Map<String, Object>> admins = ParseUtils.getList(largeData, "users[role:EQUALS:admin]");
        long endTime = System.currentTimeMillis();

        assertNotNull(admins);
        assertEquals(admins.size(), 334, "Should find approximately 334 admin users"); // 1000/3 + 1
        assertTrue(endTime - startTime < 1000, "Filtering should be fast: " + (endTime - startTime) + "ms");

        // Test complex filtering performance
        startTime = System.currentTimeMillis();
        List<Map<String, Object>> complexResults = ParseUtils.getList(largeData,
                "users[(role:EQUALS:admin AND age:GREATER_THAN:40) OR (role:EQUALS:moderator AND status:EQUALS:active)]");
        endTime = System.currentTimeMillis();

        assertNotNull(complexResults);
        assertTrue(complexResults.size() > 0, "Should find some results");
        assertTrue(endTime - startTime < 1000, "Complex filtering should be fast: " + (endTime - startTime) + "ms");
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    public void testCompleteIntegrationScenario() {
        // Test a complete real-world scenario using all features

        // 1. Get all active users with multiple segments (List)
        List<Map<String, Object>> targetUsers = ParseUtils.getList(testData,
                "users[status:EQUALS:active AND segments?size:GREATER_THAN:1]");
        assertNotNull(targetUsers);
        assertEquals(targetUsers.size(), 3, "Should find 3 active users with multiple segments");

        // 2. Process the list to extract specific information
        List<String> targetEmails = targetUsers.stream()
                .map(user -> (String) user.get("email"))
                .collect(Collectors.toList());

        assertTrue(targetEmails.contains("alice@company_com"));
        assertTrue(targetEmails.contains("diana@company_com"));
        assertTrue(targetEmails.contains("eve@company_com"));


        // 4. Get field from first matching user
        String firstTargetUserEmail = ParseUtils.getString(testData,
                "users[status:EQUALS:active AND segments?size:GREATER_THAN:1].email");
        assertEquals(firstTargetUserEmail, "alice@company_com");

        // 5. Verify user counts by different criteria
        List<Map<String, Object>> allActiveUsers = ParseUtils.getList(testData, "users[status:EQUALS:active]");
        List<Map<String, Object>> allUsersWithSegments = ParseUtils.getList(testData, "users[segments?size:GREATER_THAN:1]");

        assertEquals(allActiveUsers.size(), 4);
        assertEquals(allUsersWithSegments.size(), 3);

        // 6. Test complex business logic
        List<Map<String, Object>> businessTargets = ParseUtils.getList(testData,
                "users[(role:EQUALS:admin AND segments?size:GREATER_THAN:1) OR (role:EQUALS:moderator AND status:EQUALS:active)]");
        assertEquals(businessTargets.size(), 3, "Should find users matching business criteria");
    }

    @Test
    public void testCrossDatasetIntegration() {
        // Test integration across different datasets

        // E-commerce: Get all premium customers with multiple orders
        List<Map<String, Object>> premiumCustomersWithOrders = ParseUtils.getList(ecommerceData,
                "customers[status:EQUALS:premium AND orders?size:GREATER_THAN:2]");
        assertEquals(premiumCustomersWithOrders.size(), 2);

        // Organization: Get all active employees with admin permissions
        List<Map<String, Object>> adminEmployees = ParseUtils.getList(organizationData,
                "employees[active:EQUALS:true AND permissions:CONTAINS:admin]");
        assertEquals(adminEmployees.size(), 2);

        // Config: Get all production applications with cache enabled
        List<Map<String, Object>> prodCachedApps = ParseUtils.getList(configData,
                "applications[environment:EQUALS:production AND settings?cache?enabled:EQUALS:true]");
        assertEquals(prodCachedApps.size(), 2);

        // Verify we can get specific data from each
        String firstPremiumCustomer = ParseUtils.getString(ecommerceData,
                "customers[status:EQUALS:premium AND orders?size:GREATER_THAN:2].name");
        assertNotNull(firstPremiumCustomer);

        String firstAdminEmployee = ParseUtils.getString(organizationData,
                "employees[active:EQUALS:true AND permissions:CONTAINS:admin].name");
        assertNotNull(firstAdminEmployee);

        String firstProdCachedApp = ParseUtils.getString(configData,
                "applications[environment:EQUALS:production AND settings?cache?enabled:EQUALS:true].name");
        assertNotNull(firstProdCachedApp);
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    public void testEmptyArrayFiltering() {
        // Test filtering on empty arrays
        Map<String, Object> dataWithEmptyArray = Map.of(
                "items", Arrays.asList()
        );

        List<Map<String, Object>> results = ParseUtils.getList(dataWithEmptyArray, "items[field:EQUALS:value]");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Filtering empty array should return empty list");

        String fieldFromEmpty = ParseUtils.getString(dataWithEmptyArray, "items[field:EQUALS:value].name");
        assertNull(fieldFromEmpty, "Field access on empty filtered results should return null");
    }

    @Test
    public void testNestedArrayFiltering() {
        // Test filtering arrays within filtered results
        Map<String, Object> nestedData = Map.of(
                "departments", Arrays.asList(
                        Map.of(
                                "name", "Engineering",
                                "teams", Arrays.asList(
                                        Map.of("name", "Backend", "size", 5),
                                        Map.of("name", "Frontend", "size", 3),
                                        Map.of("name", "DevOps", "size", 2)
                                )
                        ),
                        Map.of(
                                "name", "Sales",
                                "teams", Arrays.asList(
                                        Map.of("name", "Inside", "size", 8),
                                        Map.of("name", "Field", "size", 4)
                                )
                        )
                )
        );

        // Get all teams from Engineering department
        List<Map<String, Object>> engineeringTeams = ParseUtils.getList(nestedData,
                "departments[name:EQUALS:Engineering].teams");
        assertNotNull(engineeringTeams, "Should return null because .teams is continuation after filtering");

        // Get first team from Engineering department
        Map<String, Object> firstEngineeringTeam = ParseUtils.getMap(nestedData,
                "departments[name:EQUALS:Engineering].teams.[0]");
        assertNotNull(firstEngineeringTeam);
        assertEquals(firstEngineeringTeam.get("name"), "Backend");

        // Get name of first large team from Engineering
        String largeTeamName = ParseUtils.getString(nestedData,
                "departments[name:EQUALS:Engineering].teams.[size:GREATER_THAN:3].name");
        assertEquals(largeTeamName, "Backend");
    }

    @Test
    public void testSpecialCharactersInConditions() {
        Map<String, Object> specialData = Map.of(
                "items", Arrays.asList(
                        Map.of("description", "Item with: special, characters! @#$%"),
                        Map.of("description", "Normal item"),
                        Map.of("path", "/api/v1/users/{id}/profile"),
                        Map.of("regex", "^[a-zA-Z0-9]+$")
                )
        );

        // Test special characters in values
        List<Map<String, Object>> specialItems = ParseUtils.getList(specialData,
                "items[description:CONTAINS:special]");
        assertEquals(specialItems.size(), 1);

        List<Map<String, Object>> pathItems = ParseUtils.getList(specialData,
                "items[path:CONTAINS:/api/v1/]");
        assertEquals(pathItems.size(), 1);

        List<Map<String, Object>> regexItems = ParseUtils.getList(specialData,
                "items[regex:STARTS_WITH:^]");
        assertEquals(regexItems.size(), 1);
    }

    @Test
    public void testConcurrentAccess() {
        // Test thread safety of new implementation
        List<Thread> threads = new ArrayList<>();
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 10; i++) {
            Thread thread = new Thread(() -> {
                try {
                    // Test list access
                    List<Map<String, Object>> admins = ParseUtils.getList(testData, "users[role:EQUALS:admin]");
                    results.add("List size: " + admins.size());

                    // Test single value access
                    String name = ParseUtils.getString(testData, "users[role:EQUALS:admin].name");
                    results.add("Name: " + name);
                } catch (Exception e) {
                    exceptions.add(e);
                }
            });
            threads.add(thread);
            thread.start();
        }

        // Wait for all threads
        threads.forEach(thread -> {
            try {
                thread.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Verify results
        assertTrue(exceptions.isEmpty(), "Concurrent access should not cause exceptions");
        assertEquals(results.size(), 20, "Should have 20 results (10 threads × 2 operations each)");

        // Verify consistency
        long listSizeResults = results.stream().filter(r -> r.startsWith("List size: 2")).count();
        long nameResults = results.stream().filter(r -> r.startsWith("Name: Alice Admin")).count();
        assertEquals(listSizeResults, 10, "All list size results should be consistent");
        assertEquals(nameResults, 10, "All name results should be consistent");
    }

    // ========== DOCUMENTATION COMPLIANCE TESTS ==========

    @Test
    public void testAllDocumentedBehavior() {
        // Test all documented examples work as expected

        // Basic examples from documentation
        List<Map<String, Object>> allUsers = ParseUtils.getList(testData, "users");
        assertEquals(allUsers.size(), 5);

        List<Map<String, Object>> adminUsers = ParseUtils.getList(testData, "users[role:EQUALS:admin]");
        assertEquals(adminUsers.size(), 2);

        String firstAdminName = ParseUtils.getString(testData, "users[role:EQUALS:admin].name");
        assertEquals(firstAdminName, "Alice Admin");

        Map<String, Object> firstUserByIndex = ParseUtils.getMap(testData, "users[0]");
        assertEquals(firstUserByIndex.get("name"), "Alice Admin");


        // Method call examples
        List<Map<String, Object>> usersWithMultipleSegments = ParseUtils.getList(testData,
                "users[segments?size:GREATER_THAN:1]");
        assertEquals(usersWithMultipleSegments.size(), 3);

        String emailOfUserWithSegments = ParseUtils.getString(testData,
                "users[segments?size:GREATER_THAN:1].email");
        assertEquals(emailOfUserWithSegments, "alice@company_com");

        // Complex condition examples
        List<Map<String, Object>> complexResults = ParseUtils.getList(testData,
                "users[(role:EQUALS:admin OR role:EQUALS:moderator) AND status:EQUALS:active]");
        assertEquals(complexResults.size(), 3);
    }

    @Test
    public void testBackwardCompatibility() {
        // Ensure that old usage patterns still work where they should

        // Single field access should work as before
        assertEquals(ParseUtils.getString(testData, "name"), "John Doe");
        assertEquals(ParseUtils.getString(testData, "address.city"), "New York");
        assertEquals(ParseUtils.getString(testData, "users[0].name"), "Alice Admin");

        // Array index access should work as before
        assertEquals(ParseUtils.getString(testData, "hobbies[1]"), "swimming");
        assertEquals(ParseUtils.getInteger(testData, "scores[2]"), Integer.valueOf(78));

        // Type conversion should work as before
        assertEquals(ParseUtils.getInteger(testData, "age"), Integer.valueOf(30));
        assertEquals(ParseUtils.getBoolean(testData, "isActive"), Boolean.TRUE);

        // Helper methods should work as before
        assertTrue(ParseUtils.exists(testData, "name"));
        assertFalse(ParseUtils.exists(testData, "nonExistent"));
        assertEquals(ParseUtils.getOrDefault(testData, "nonExistent", "default"), "default");
    }

    // ========== FINAL VALIDATION ==========

    @Test
    public void testConsistentBehaviorValidation() {
        // Final validation that the new behavior is consistent and logical

        // Verify list returns
        Object usersResult = ParseUtils.get(testData, "users");
        assertTrue(usersResult instanceof List, "users should return List");

        Object filteredResult = ParseUtils.get(testData, "users[role:EQUALS:admin]");
        assertTrue(filteredResult instanceof List, "users[condition] should return List");

        // Verify single returns
        Object indexResult = ParseUtils.get(testData, "users[0]");
        assertTrue(indexResult instanceof Map, "users[0] should return Map");

        Object fieldResult = ParseUtils.get(testData, "users[role:EQUALS:admin].name");
        assertTrue(fieldResult instanceof String, "users[condition].field should return field type");

        // Verify helper method consistency
        List<Map<String, Object>> listHelper = ParseUtils.getList(testData, "users[role:EQUALS:admin]");
        assertNotNull(listHelper);
        assertTrue(listHelper instanceof List);

        Map<String, Object> mapHelper = ParseUtils.getMap(testData, "users[0]");
        assertNotNull(mapHelper);
        assertTrue(mapHelper instanceof Map);

        // Verify null returns for type mismatches
        assertNull(ParseUtils.getList(testData, "users[0]")); // Map result, expecting List
        assertNull(ParseUtils.getMap(testData, "users[role:EQUALS:admin]")); // List result, expecting Map

        System.out.println("✅ All consistency validations passed!");
    }
}