package lazydevs.persistence.jdbc.rls;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A {@link Connection} proxy whose {@code close()} clears the tenant session
 * variable before returning the connection to the pool, so a pooled connection
 * can never carry one request's tenant into the next request.
 *
 * <p>If the reset itself fails, the underlying physical connection is closed
 * hard so a dirty session cannot re-enter the pool.</p>
 *
 * @author Abhijeet Rai
 */
public final class TenantBoundConnection implements InvocationHandler {

    private final Connection delegate;
    private final String settingName;

    private TenantBoundConnection(Connection delegate, String settingName) {
        this.delegate = delegate;
        this.settingName = settingName;
    }

    public static Connection wrap(Connection delegate, String settingName) {
        return (Connection) Proxy.newProxyInstance(
                TenantBoundConnection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new TenantBoundConnection(delegate, settingName));
    }

    /** Executes {@code SELECT set_config(name, value, false)} on the given connection. */
    static void setConfig(Connection connection, String settingName, String value) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT set_config(?, ?, false)")) {
            ps.setString(1, settingName);
            ps.setString(2, value);
            ps.execute();
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("close".equals(method.getName()) && (null == args || args.length == 0)) {
            close();
            return null;
        }
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    private void close() throws SQLException {
        if (delegate.isClosed()) {
            return;
        }
        try {
            // '' rather than "unset": PostgreSQL offers no way back to unset for a
            // custom GUC once it has been written (set_config(name, NULL),
            // RESET and DISCARD ALL all leave ''), so '' IS the cleared state
            // and every checkout after the first one sees it. An equality check
            // against '' matches no tenant; a policy that CASTS the setting must
            // therefore be written nullif(current_setting(...), '')::uuid,
            // because a bare ''::uuid raises instead of matching nothing.
            setConfig(delegate, settingName, "");
        } catch (SQLException resetFailure) {
            try {
                delegate.close();
            } catch (SQLException closeFailure) {
                resetFailure.addSuppressed(closeFailure);
            }
            throw new SQLException("Failed to reset RLS setting '" + settingName
                    + "' on connection close; the physical connection was closed to keep the pool clean.", resetFailure);
        }
        delegate.close();
    }
}
