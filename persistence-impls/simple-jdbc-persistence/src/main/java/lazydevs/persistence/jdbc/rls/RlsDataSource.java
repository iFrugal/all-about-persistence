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
 * <p>When no tenant is in scope, the default is to throw (fail loud); with
 * {@code failWhenTenantMissing=false} the raw connection is returned unbound and
 * the database policy itself fails closed (an unset variable matches no rows).</p>
 *
 * <p>The DB side (policies, roles) is owned by your SQL migrations, e.g.:</p>
 * <pre>{@code
 * ALTER TABLE employee ENABLE ROW LEVEL SECURITY;
 * CREATE POLICY employee_tenant_isolation ON employee
 *     USING (tenant_id = current_setting('app.tenant_id', true)::uuid);
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
            if (settings.isFailWhenTenantMissing()) {
                closeQuietly(connection);
                throw new IllegalStateException("No tenant in scope while acquiring an RLS-bound connection ("
                        + settings.getSettingName() + "). Set TenantContext (or configure failWhenTenantMissing=false).");
            }
            log.debug("No tenant in scope; returning connection without setting {}", settings.getSettingName());
            return connection;
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
