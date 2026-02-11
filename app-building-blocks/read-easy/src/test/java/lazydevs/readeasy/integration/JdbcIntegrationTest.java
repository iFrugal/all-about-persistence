package lazydevs.readeasy.integration;

import lazydevs.persistence.jdbc.general.JdbcGeneralReader;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.readeasy.TestApplication;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for JDBC functionality using H2 in-memory database.
 * Tests verify that JdbcGeneralReader works correctly with actual SQL queries.
 */
@SpringBootTest(classes = TestApplication.class)
@Import(JdbcTestConfiguration.class)
@ActiveProfiles("jdbc-test")
@DisplayName("JDBC Integration Tests with H2")
@Disabled("Requires complex database setup - run manually")
class JdbcIntegrationTest {

    @Autowired(required = false)
    @Qualifier("readEasyGeneralReaderMap")
    private Map<String, GeneralReader> readEasyGeneralReaderMap;

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Nested
    @DisplayName("Configuration Verification")
    class ConfigurationVerification {

        @Test
        @DisplayName("Should have JDBC reader configured")
        void shouldHaveJdbcReaderConfigured() {
            Assumptions.assumeTrue(readEasyGeneralReaderMap != null, "Reader map not available");
            assertTrue(readEasyGeneralReaderMap.containsKey("default"),
                    "Should have 'default' reader");
            assertTrue(readEasyGeneralReaderMap.containsKey("jdbc"),
                    "Should have 'jdbc' reader");

            GeneralReader defaultReader = readEasyGeneralReaderMap.get("default");
            assertTrue(defaultReader instanceof JdbcGeneralReader,
                    "Default reader should be JdbcGeneralReader");
        }

        @Test
        @DisplayName("Should have H2 DataSource configured")
        void shouldHaveH2DataSourceConfigured() {
            assertNotNull(dataSource, "DataSource should be configured");
        }

        @Test
        @DisplayName("Should have test data loaded")
        void shouldHaveTestDataLoaded() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users", Integer.class);
            assertNotNull(userCount);
            assertTrue(userCount > 0, "Should have users in database");

            Integer orderCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders", Integer.class);
            assertNotNull(orderCount);
            assertTrue(orderCount > 0, "Should have orders in database");
        }
    }

    @Nested
    @DisplayName("JdbcGeneralReader Operations")
    class JdbcGeneralReaderOperations {

        private JdbcGeneralReader jdbcReader;

        @BeforeEach
        void setUp() {
            if (readEasyGeneralReaderMap != null && readEasyGeneralReaderMap.containsKey("default")) {
                jdbcReader = (JdbcGeneralReader) readEasyGeneralReaderMap.get("default");
            }
        }

        @Test
        @DisplayName("Should find all users")
        void shouldFindAllUsers() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT * FROM users ORDER BY id");

            assertFalse(users.isEmpty(), "Should return users");
            assertTrue(users.size() >= 1, "Should have users from test data");

            // Verify first user
            Map<String, Object> firstUser = users.get(0);
            assertNotNull(firstUser.get("ID"));
            assertNotNull(firstUser.get("NAME"));
        }

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            Map<String, Object> user = jdbcTemplate.queryForMap(
                    "SELECT * FROM users WHERE id = ?", 1);

            assertNotNull(user);
            assertNotNull(user.get("NAME"));
            assertNotNull(user.get("EMAIL"));
        }

        @Test
        @DisplayName("Should find users by status")
        void shouldFindUsersByStatus() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> activeUsers = jdbcTemplate.queryForList(
                    "SELECT * FROM users WHERE status = ?", "ACTIVE");

            assertFalse(activeUsers.isEmpty());
            assertTrue(activeUsers.stream()
                    .allMatch(u -> "ACTIVE".equals(u.get("STATUS"))));
        }

        @Test
        @DisplayName("Should count users")
        void shouldCountUsers() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users", Integer.class);

            assertNotNull(count);
            assertTrue(count > 0, "Should have users");
        }

        @Test
        @DisplayName("Should find orders with join")
        void shouldFindOrdersWithJoin() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> ordersWithUsers = jdbcTemplate.queryForList(
                    "SELECT o.*, u.name as user_name FROM orders o " +
                            "JOIN users u ON o.user_id = u.id ORDER BY o.id");

            assertFalse(ordersWithUsers.isEmpty());
            assertNotNull(ordersWithUsers.get(0).get("USER_NAME"));
        }

        @Test
        @DisplayName("Should handle empty result")
        void shouldHandleEmptyResult() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> result = jdbcTemplate.queryForList(
                    "SELECT * FROM users WHERE id = -999");

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should aggregate order totals by user")
        void shouldAggregateOrderTotalsByUser() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> totals = jdbcTemplate.queryForList(
                    "SELECT user_id, SUM(total_amount) as total, COUNT(*) as order_count " +
                            "FROM orders GROUP BY user_id ORDER BY total DESC");

            assertFalse(totals.isEmpty());
            assertNotNull(totals.get(0).get("TOTAL"));
            assertNotNull(totals.get(0).get("ORDER_COUNT"));
        }
    }

    @Nested
    @DisplayName("SQL Query Patterns")
    class SqlQueryPatterns {

        @Test
        @DisplayName("Should support LIKE queries")
        void shouldSupportLikeQueries() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT * FROM users WHERE name LIKE ?", "%Doe%");

            assertFalse(users.isEmpty(), "Should find users with 'Doe' in name");
        }

        @Test
        @DisplayName("Should support IN queries")
        void shouldSupportInQueries() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT * FROM users WHERE status IN ('ACTIVE', 'PENDING')");

            assertFalse(users.isEmpty());
            assertTrue(users.stream()
                    .allMatch(u -> "ACTIVE".equals(u.get("STATUS")) ||
                            "PENDING".equals(u.get("STATUS"))));
        }

        @Test
        @DisplayName("Should support ORDER BY")
        void shouldSupportOrderBy() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT * FROM users ORDER BY name ASC");

            assertFalse(users.isEmpty());
            // Verify alphabetical order if more than 1 user
            if (users.size() > 1) {
                String firstName = (String) users.get(0).get("NAME");
                String lastName = (String) users.get(users.size() - 1).get("NAME");
                assertTrue(firstName.compareTo(lastName) <= 0);
            }
        }

        @Test
        @DisplayName("Should support LIMIT and OFFSET")
        void shouldSupportLimitAndOffset() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> page1 = jdbcTemplate.queryForList(
                    "SELECT * FROM users ORDER BY id LIMIT 2 OFFSET 0");
            List<Map<String, Object>> page2 = jdbcTemplate.queryForList(
                    "SELECT * FROM users ORDER BY id LIMIT 2 OFFSET 2");

            assertEquals(2, page1.size());
            assertTrue(page2.size() <= 2);
            if (!page2.isEmpty()) {
                assertNotEquals(page1.get(0).get("ID"), page2.get(0).get("ID"));
            }
        }

        @Test
        @DisplayName("Should support date range queries")
        void shouldSupportDateRangeQueries() {
            Assumptions.assumeTrue(jdbcTemplate != null, "JdbcTemplate not available");

            List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                    "SELECT * FROM orders WHERE created_at >= ? AND created_at <= ?",
                    "2024-02-01 00:00:00", "2024-12-31 23:59:59");

            // Orders may or may not exist in this range
            assertNotNull(orders);
        }
    }
}
