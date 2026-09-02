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
    void missingTenantPassesThroughWhenConfiguredLenient() throws SQLException {
        FakeDb db = new FakeDb();
        RlsDataSource rls = new RlsDataSource(db.dataSource(),
                new RlsSettings("app.tenant_id", false, TenantContext::getTenantId));
        Connection connection = rls.getConnection();
        assertTrue(db.events.isEmpty(), "no set_config expected without a tenant");
        connection.close();
        assertEquals(List.of("connection-closed"), db.events);
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
