package lazydevs.mapper.db.jdbc;

import lazydevs.mapper.db.jdbc.dumb.ConnectionUnawareJdbcRepository;
import lazydevs.mapper.db.jdbc.simple.EntityAwarePreparedStatementSetter;
import lazydevs.mapper.db.jdbc.simple.PreparedStatementSetter;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.jdbc.general.JdbcParam;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.String.format;

/**
 * @param <T>
 */
@Slf4j
public class JdbcRepository<T> extends ResultSetMapper<T>{
    @Getter
    private final ConnectionUnawareJdbcRepository<T> dumbJdbcRepository;

    public JdbcRepository(DataSource dataSource, Class<T> type) {
        super(dataSource, type);
        this.dumbJdbcRepository = new ConnectionUnawareJdbcRepository<>(type);
    }

    public JdbcRepository(ConnectionProvider connectionProvider, Class<T> type) {
        super(connectionProvider, type);
        this.dumbJdbcRepository = new ConnectionUnawareJdbcRepository<>(type);
    }


    public boolean execute(String sql) {
        return execute(sql, null);
    }

    public boolean execute(String sql, PreparedStatementSetter pss){
        try(Connection connection = getConnection()){
            return dumbJdbcRepository.execute(connection, sql, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public long executeUpdate(String sql, PreparedStatementSetter pss){
        try(Connection connection = getConnection()){
            return dumbJdbcRepository.executeUpdate(connection, sql, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public long executeUpdate(String sql, BatchIterator<T> batchIterator, EntityAwarePreparedStatementSetter taPss, Function<List<T>, List<T>>... filters) {
        try(Connection connection = getConnection()){
            connection.setAutoCommit(false);
            long noOfRowsAffected = 0l;
            while(batchIterator.hasNext()){
                List<T> list = batchIterator.next();
                if(null != filters){
                    for(Function<List<T>, List<T>> filter : filters){
                        list = filter.apply(list);
                    }
                }
                if(null != list && list.isEmpty()) {
                    noOfRowsAffected += dumbJdbcRepository.executeUpdate(connection, sql, list, taPss);
                }
            }
            connection.commit();
            return noOfRowsAffected;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public long executeUpdate(String sql, List<T> list, EntityAwarePreparedStatementSetter taPss){
        try(Connection connection = getConnection()){
            connection.setAutoCommit(false);
            long noOfRowsAffected = dumbJdbcRepository.executeUpdate(connection, sql, list, taPss);
            if(list.size() != noOfRowsAffected){
                log.error(format("ExpectedCount = %s, AffectedCount = %s. SQL = %s , List<T> = %s", list.size(), noOfRowsAffected, sql, list));
            }
            connection.commit();
            return noOfRowsAffected;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public long executeUpdate(@NonNull List<String> sqls){
        try(Connection connection = getConnection()){
            connection.setAutoCommit(false);
            long noOfRowsAffected = dumbJdbcRepository.executeUpdate(connection, sqls);
            connection.commit();
            return noOfRowsAffected;
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }


    public long executeUpdate(String sql, T t, EntityAwarePreparedStatementSetter taPss){
        return executeUpdate(sql, Arrays.asList(t), taPss);
    }

}
