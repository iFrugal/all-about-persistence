package lazydevs.persistence.jdbc.rls;

import lazydevs.persistence.connection.multitenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RlsDataSourceTest {

    /** Records set_config invocations as "name=value" and connection close events. */
    static class FakeDb {
        final List<String> events = new ArrayList<>();
        boolean failOnSetConfig = false;
        boolean closed = false;
        String pendingName;
        String pendingValue;

        Connection connection() {
            InvocationHandler psHandler = null; // assigned per prepareStatement
            return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "prepareStatement":
                                String sql = (String) args[0];
                                assertTrue(sql.contains("set_config"), "unexpected SQL: " + sql);
                                return preparedStatement();
                            case "isClosed":
                                return closed;
                            case "close":
                                closed = true;
                                events.add("connection-closed");
                                return null;
                            case "toString":
                                return "FakeConnection";
                            default:
                                throw new UnsupportedOperationException("Connection." + method.getName());
                        }
                    });
        }

        private PreparedStatement preparedStatement() {
            return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{PreparedStatement.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setString":
                                if ((Integer) args[0] == 1) pendingName = (String) args[1];
                                else pendingValue = (String) args[1];
                                return null;
                            case "execute":
                                if (failOnSetConfig) throw new SQLException("simulated set_config failure");
                                events.add(pendingName + "=" + pendingValue);
                                return true;
                            case "close":
                                return null;
                            default:
                                throw new UnsupportedOperationException("PreparedStatement." + method.getName());
                        }
                    });
        }

        DataSource dataSource() {
            return (DataSource) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                        if ("getConnection".equals(method.getName())) return connection();
                        throw new UnsupportedOperationException("DataSource." + method.getName());
                    });
        }
    }

    @AfterEach
    void cleanup() {
        TenantContext.reset();
    }

    @Test
    void bindsTenantOnCheckoutAndResetsOnClose() throws SQLException {
        FakeDb db = new FakeDb();
        TenantContext.setTenantId("tenant-1");
        RlsDataSource rls = new RlsDataSource(db.dataSource());

        Connection connection = rls.getConnection();
        assertEquals(List.of("app.tenant_id=tenant-1"), db.events);

        connection.close();
        assertEquals(List.of("app.tenant_id=tenant-1", "app.tenant_id=", "connection-closed"), db.events);
    }

    @Test
    void doubleCloseIsIdempotent() throws SQLException {
        FakeDb db = new FakeDb();
        TenantContext.setTenantId("tenant-1");
        Connection connection = new RlsDataSource(db.dataSource()).getConnection();
        connection.close();
        connection.close();
        assertEquals(List.of("app.tenant_id=tenant-1", "app.tenant_id=", "connection-closed"), db.events);
    }

    @Test
    void missingTenantFailsLoudAndReleasesConnection() {
        FakeDb db = new FakeDb();
        RlsDataSource rls = new RlsDataSource(db.dataSource());
        assertThrows(IllegalStateException.class, rls::getConnection);
        assertEquals(List.of("connection-closed"), db.events);
    }

    @Test
    void missingTenantBindsTheStandInValueAndStillResetsOnClose() throws SQLException {
        FakeDb db = new FakeDb();
        RlsDataSource rls = new RlsDataSource(db.dataSource(),
                new RlsSettings("app.tenant_id", MissingTenant.BIND, ""));
        Connection connection = rls.getConnection();
        assertEquals(List.of("app.tenant_id="), db.events, "the stand-in must be bound, not skipped");
        connection.close();
        assertEquals(List.of("app.tenant_id=", "app.tenant_id=", "connection-closed"), db.events);
    }

    @Test
    void missingTenantCanBindACustomStandInValue() throws SQLException {
        FakeDb db = new FakeDb();
        Connection connection = new RlsDataSource(db.dataSource(), "app.tenant_id", "~pool").getConnection();
        assertEquals(List.of("app.tenant_id=~pool"), db.events);
        connection.close();
        assertEquals(List.of("app.tenant_id=~pool", "app.tenant_id=", "connection-closed"), db.events);
    }

    /**
     * The regression this whole change exists for. Before it, the second
     * checkout recorded NO set_config at all, leaving the connection carrying
     * the '' written by the first one's reset - which a policy casting the
     * setting to uuid rejects outright, while a never-used connection (NULL)
     * would have been fine. Same request, two outcomes, chosen by pool warmth.
     */
    @Test
    void tenantLessCheckoutAfterATenantScopedOneBindsRatherThanInheritingSessionState() throws SQLException {
        FakeDb db = new FakeDb();
        RlsDataSource rls = new RlsDataSource(db.dataSource(),
                new RlsSettings("app.tenant_id", MissingTenant.BIND, ""));

        TenantContext.setTenantId("tenant-1");
        rls.getConnection().close();

        // FakeDb models ONE physical connection, and its close() is real rather
        // than a return-to-pool, so clear the flag to represent the pool handing
        // that same connection back. This is the whole scenario: the second
        // checkout runs on a connection that has already been bound and reset.
        db.closed = false;

        TenantContext.reset();
        rls.getConnection().close();

        assertEquals(List.of(
                "app.tenant_id=tenant-1", "app.tenant_id=", "connection-closed",
                "app.tenant_id=", "app.tenant_id=", "connection-closed"), db.events);
    }

    @Test
    @SuppressWarnings("deprecation")
    void legacyLenientFlagMapsToBindingTheEmptyString() throws SQLException {
        FakeDb db = new FakeDb();
        RlsSettings settings = new RlsSettings("app.tenant_id", false, TenantContext::getTenantId);
        assertEquals(MissingTenant.BIND, settings.getMissingTenant());
        assertFalse(settings.isFailWhenTenantMissing());

        Connection connection = new RlsDataSource(db.dataSource(), settings).getConnection();
        assertEquals(List.of("app.tenant_id="), db.events);
        connection.close();
    }

    @Test
    void setConfigFailureOnCheckoutReleasesConnection() {
        FakeDb db = new FakeDb();
        db.failOnSetConfig = true;
        TenantContext.setTenantId("tenant-1");
        RlsDataSource rls = new RlsDataSource(db.dataSource());
        assertThrows(SQLException.class, rls::getConnection);
        assertEquals(List.of("connection-closed"), db.events);
    }

    @Test
    void resetFailureOnCloseHardClosesPhysicalConnection() throws SQLException {
        FakeDb db = new FakeDb();
        TenantContext.setTenantId("tenant-1");
        Connection connection = new RlsDataSource(db.dataSource()).getConnection();
        db.failOnSetConfig = true;
        assertThrows(SQLException.class, connection::close);
        assertEquals(List.of("app.tenant_id=tenant-1", "connection-closed"), db.events);
    }

    @Test
    void customSettingNameIsUsed() throws SQLException {
        FakeDb db = new FakeDb();
        TenantContext.setTenantId("t9");
        Connection connection = new RlsDataSource(db.dataSource(), "myapp.org_id").getConnection();
        connection.close();
        assertEquals(List.of("myapp.org_id=t9", "myapp.org_id=", "connection-closed"), db.events);
    }

    @Test
    void invalidSettingNamesAreRejected() {
        FakeDb db = new FakeDb();
        assertThrows(IllegalArgumentException.class, () -> new RlsDataSource(db.dataSource(), "tenant_id"));
        assertThrows(IllegalArgumentException.class, () -> new RlsDataSource(db.dataSource(), "app.tenant id"));
        assertThrows(IllegalArgumentException.class, () -> new RlsDataSource(db.dataSource(), "app.x; drop table t"));
    }
}
