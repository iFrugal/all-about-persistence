package lazydevs.persistence.jdbc.rls;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * A {@link DataSource} decorator that binds the current tenant to every
 * connection it hands out, enabling PostgreSQL row-level-security policies
 * keyed on a session variable (e.g. {@code app.tenant_id}).
 *
 * <p>On {@code getConnection()} it runs
 * {@code SELECT set_config('&lt;settingName&gt;', '&lt;tenant&gt;', false)} and returns the
 * connection wrapped in a {@link TenantBoundConnection}, whose {@code close()}
 * clears the variable before the connection returns to the pool. Because every
 * JDBC path in this library (including batch iterators that hold the connection
 * open across a streamed export) acquires and closes connections through the
 * DataSource, decorating at this seam covers all of them.</p>
 *
 * <p>When no tenant is in scope the behaviour is {@link RlsSettings#getMissingTenant()}:
 * {@link MissingTenant#FAIL} (the default) throws, and {@link MissingTenant#BIND}
 * binds a configured stand-in value - the empty string unless set otherwise -
 * through the same bind-and-reset path, so the session state a policy sees does
 * not depend on how warm the pool is.</p>
 *
 * <p>The DB side (policies, roles) is owned by your SQL migrations. Write the
 * predicate so an empty tenant matches nothing rather than raising - a bare
 * {@code ''::uuid} is an error, and {@code ''} is what a released connection is
 * reset to:</p>
 * <pre>{@code
 * ALTER TABLE employee ENABLE ROW LEVEL SECURITY;
 * CREATE POLICY employee_tenant_isolation ON employee
 *     USING (tenant_id = nullif(current_setting('app.tenant_id', true), '')::uuid);
 * }</pre>
 *
 * @author Abhijeet Rai
 */
@Slf4j
public class RlsDataSource implements DataSource {

    private final DataSource delegate;
    private final RlsSettings settings;

    public RlsDataSource(DataSource delegate, RlsSettings settings) {
        if (null == delegate) {
            throw new IllegalArgumentException("delegate DataSource is required");
        }
        this.delegate = delegate;
        this.settings = null == settings ? RlsSettings.defaults() : settings;
    }

    public RlsDataSource(DataSource delegate) {
        this(delegate, RlsSettings.defaults());
    }

    /** Convenience constructor for config-driven wiring (e.g. read-easy InitDTO YAML). */
    public RlsDataSource(DataSource delegate, String settingName) {
        this(delegate, new RlsSettings(settingName));
    }

    /**
     * Convenience constructor for config-driven wiring that must also serve
     * requests with no tenant: supplying {@code missingTenantValue} selects
     * {@link MissingTenant#BIND} and binds that value when the tenant is absent.
     */
    public RlsDataSource(DataSource delegate, String settingName, String missingTenantValue) {
        this(delegate, new RlsSettings(settingName, missingTenantValue));
    }

    @Override
    public Connection getConnection() throws SQLException {
        return bindTenant(delegate.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return bindTenant(delegate.getConnection(username, password));
    }

    private Connection bindTenant(Connection connection) throws SQLException {
        String tenantId = settings.getTenantSupplier().get();
        if (null == tenantId || tenantId.isEmpty()) {
            if (MissingTenant.FAIL == settings.getMissingTenant()) {
                closeQuietly(connection);
                throw new IllegalStateException("No tenant in scope while acquiring an RLS-bound connection ("
                        + settings.getSettingName() + "). Set TenantContext (or configure MissingTenant.BIND).");
            }
            // Bound, not skipped: an unbound connection reads back as NULL when
            // fresh and as '' once recycled, and a policy cannot be written
            // against both.
            tenantId = settings.getMissingTenantValue();
            log.debug("No tenant in scope; binding {} = '{}'", settings.getSettingName(), tenantId);
        }
        try {
            TenantBoundConnection.setConfig(connection, settings.getSettingName(), tenantId);
        } catch (SQLException e) {
            closeQuietly(connection);
            throw e;
        }
        return TenantBoundConnection.wrap(connection, settings.getSettingName());
    }

    private void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException e) {
            log.warn("Failed to close connection after tenant binding failure", e);
        }
    }

    //=================== plain delegation ===================

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }
}
