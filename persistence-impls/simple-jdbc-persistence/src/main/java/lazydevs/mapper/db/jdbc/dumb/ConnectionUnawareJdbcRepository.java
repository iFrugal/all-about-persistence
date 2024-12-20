package lazydevs.mapper.db.jdbc.dumb;

import lazydevs.mapper.db.jdbc.JDBCParam;
import lazydevs.mapper.db.jdbc.dumb.ConnectionUnawareResultSetMapper;
import lazydevs.mapper.db.jdbc.simple.EntityAwarePreparedStatementSetter;
import lazydevs.mapper.db.jdbc.simple.PreparedStatementSetter;
import lazydevs.persistence.jdbc.general.JdbcOperation;
import lazydevs.persistence.jdbc.general.JdbcParam;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ConnectionUnawareJdbcRepository<T> extends ConnectionUnawareResultSetMapper<T>{
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionUnawareJdbcRepository.class );

    public ConnectionUnawareJdbcRepository(Class<T> type) {
        super(type);
    }

    public boolean execute(Connection connection, String sql, PreparedStatementSetter pss){
        try(PreparedStatement ps = connection.prepareStatement(sql);){
            if(null != pss){
                pss.setValues(ps);
            }
            LOGGER.trace(sql);
            return ps.execute();
        }catch(SQLException e){
            throw new RuntimeException("Exception occurred while executing statement. " + sql, e);
        }
    }

    public long executeUpdate(Connection connection, String sql, PreparedStatementSetter pss){
        try(PreparedStatement ps = connection.prepareStatement(sql);){
            if(null != pss){
                pss.setValues(ps);
            }
            LOGGER.trace(sql);
            return ps.executeUpdate();
        }catch(SQLException e){
            throw new RuntimeException("Exception occurred while executing statement. " + sql, e);
        }
    }

    public long executeUpdate(Connection connection, JdbcOperation jdbcOperation){
        try(PreparedStatement ps = connection.prepareStatement(jdbcOperation.getNativeSQL());){
            setValuesInPreparedStatement(ps, jdbcOperation.getParamsAsArr());
            LOGGER.trace(jdbcOperation.getNativeSQL());
            return ps.executeUpdate();
        }catch(SQLException e){
            throw new RuntimeException("Exception occurred while executing statement. " + jdbcOperation.getNativeSQL(), e);
        }
    }

    public void executeUpdate(Connection connection, JdbcOperation... jdbcOperations){
        if(null != jdbcOperations){
            for (JdbcOperation jdbcOperation : jdbcOperations) {
                executeUpdate(connection, jdbcOperation);
            }
        }
    }

    public long executeUpdate(Connection connection, String sql, List<T> list, EntityAwarePreparedStatementSetter<T> taPss){
        Objects.requireNonNull(list);
        if(list.isEmpty()){
            LOGGER.trace("Nothing to save..");
            return 0l;
        }
        String debugMessage = String.format("SQL = %s , List<T>.size() = %s", sql, list.size());
        LOGGER.trace(debugMessage);
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql);){
            for (T t : list) {
                if(null != taPss) {
                    taPss.setValues(preparedStatement, t);
                }
                preparedStatement.addBatch();
            }
            int[] noOfRowsAffectedArr = preparedStatement.executeBatch();
            long affectedCount = 0;
            for(int i : noOfRowsAffectedArr){
                affectedCount += i;
            }
            preparedStatement.clearBatch();
            return affectedCount;
        }catch(SQLException e){
            StringBuilder sb = new StringBuilder();
            e.iterator().forEachRemaining(
                    t -> sb.append(t.getMessage())
            );
            throw new RuntimeException("Exception occurred while saving the batch." + debugMessage +"\n"+sb.toString(), e);
        }
    }

    public long executeUpdate(Connection connection, @NonNull List<String> sqls){
        String debugMessage = String.format("sqls.size = %s", sqls.size());
        LOGGER.trace(debugMessage);
        try(Statement st = connection.createStatement()){
            for (String sql : sqls) {
                st.addBatch(sql);
            }
            int[] noOfRowsAffectedArr = st.executeBatch();
            long affectedCount = 0;
            for(int i : noOfRowsAffectedArr){
                affectedCount += i;
            }
            st.clearBatch();
            return affectedCount;
        }catch(SQLException e){
            StringBuilder sb = new StringBuilder();
            e.iterator().forEachRemaining(
                    t -> sb.append(t.getMessage())
            );
            throw new RuntimeException("Exception occurred while saving the batch." + debugMessage +"\n"+sb.toString(), e);
        }
    }

    public long executeUpdate(Connection connection, String sql, T t, EntityAwarePreparedStatementSetter<T> taPss){
        if(null == t){
            throw new IllegalArgumentException("Nothing to update. T t is null. Consider using overloaded method: 'executeUpdate(Connection connection, String sql, PreparedStatementSetter pss)'");
        }
        return executeUpdate(connection, sql, Arrays.asList(t), taPss);
    }

}
