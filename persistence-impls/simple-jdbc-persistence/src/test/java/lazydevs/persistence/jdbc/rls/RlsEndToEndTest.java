package lazydevs.persistence.jdbc.rls;

import lazydevs.persistence.connection.multitenant.TenantContext;
import lazydevs.persistence.jdbc.general.JdbcGeneralReader;
import lazydevs.persistence.jdbc.general.JdbcOperation;
import lazydevs.mapper.utils.BatchIterator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Proves the full wiring through {@link JdbcGeneralReader}: the tenant GUC is
 * set before the query runs and cleared when the connection is released - on
 * the plain query path and on the connection-escaping batch path used by
 * exports. H2 stands in for PostgreSQL via a {@code SET_CONFIG} alias that
 * records invocations; the actual row filtering is PostgreSQL's job and is
 * covered by the policy, not this test.
 */
public class RlsEndToEndTest {

    /** Recorded set_config calls, as "name=value". H2 alias writes here. */
    static final List<String> SET_CONFIG_CALLS = new ArrayList<>();

    @SuppressWarnings("unused") // referenced by the H2 CREATE ALIAS below
    public static String setConfig(String name, String value, boolean isLocal) {
        SET_CONFIG_CALLS.add(name + "=" + value);
        return value;
    }

    private static JdbcDataSource h2;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:rlstest;DB_CLOSE_DELAY=-1");
        try (Connection c = h2.getConnection(); Statement s = c.createStatement()) {
            s.execute("create alias if not exists set_config for \""
                    + RlsEndToEndTest.class.getName() + ".setConfig\"");
            s.execute("create table employee (id int primary key, name varchar(50), tenant_id varchar(36))");
            s.execute("insert into employee values (1, 'A', 't1'), (2, 'B', 't2')");
        }
    }

    @BeforeEach
    void reset() {
        SET_CONFIG_CALLS.clear();
        TenantContext.setTenantId("t1");
    }

    @AfterEach
    void cleanup() {
        TenantContext.reset();
    }

    @Test
    void findAllSetsTenantBeforeQueryAndClearsAfter() {
        JdbcGeneralReader reader = new JdbcGeneralReader(h2, "app.tenant_id");
        List<Map<String, Object>> rows = reader.findAll(
                JdbcOperation.nativeSql("select * from employee order by id"), Map.of());
        assertEquals(2, rows.size());
        assertEquals(List.of("app.tenant_id=t1", "app.tenant_id="), SET_CONFIG_CALLS);
    }

    @Test
    void batchIterationClearsTenantWhenIteratorCloses() throws Exception {
        JdbcGeneralReader reader = new JdbcGeneralReader(h2, "app.tenant_id");
        try (BatchIterator<Map<String, Object>> it = reader.findAllInBatch(1,
                JdbcOperation.nativeSql("select * from employee order by id"), Map.of())) {
            int count = 0;
            while (it.hasNext()) {
                count += it.next().size();
            }
            assertEquals(2, count);
            // Connection is still held by the iterator: tenant set, not yet cleared.
            assertEquals(List.of("app.tenant_id=t1"), SET_CONFIG_CALLS);
        }
        assertEquals(List.of("app.tenant_id=t1", "app.tenant_id="), SET_CONFIG_CALLS);
    }

    @Test
    void countAndFindOneEachGetTheirOwnBoundConnection() {
        JdbcGeneralReader reader = new JdbcGeneralReader(h2, "app.tenant_id");
        reader.count(JdbcOperation.nativeSql("select * from employee"), Map.of());
        reader.findOne(JdbcOperation.nativeSql("select * from employee where id = 1"), Map.of());
        assertEquals(List.of(
                "app.tenant_id=t1", "app.tenant_id=",
                "app.tenant_id=t1", "app.tenant_id="), SET_CONFIG_CALLS);
    }
}
