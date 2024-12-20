package lazydevs.mapper.db.jdbc;

import lazydevs.mapper.db.jdbc.dumb.ConnectionUnawareResultSetMapper;
import lazydevs.mapper.db.jdbc.simple.PreparedStatementSetter;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.Page;
import lombok.Getter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class ResultSetMapper<T> {
    protected final ConnectionProvider connectionProvider;
    private final ConnectionUnawareResultSetMapper<T> dumbResultSetMapper;
    @Getter
    private Class<T> entityClass;

    public ResultSetMapper(DataSource dataSource, Class<T> type) {
        this.connectionProvider = () -> dataSource.getConnection();
        this.entityClass = type;
        this.dumbResultSetMapper = new ConnectionUnawareResultSetMapper<>(type);
    }

    public ResultSetMapper(ConnectionProvider connectionProvider, Class<T> type) {
        this.connectionProvider = connectionProvider;
        this.entityClass = type;
        this.dumbResultSetMapper = new ConnectionUnawareResultSetMapper<>(type);
    }

    public T findOne(String sql, JDBCParam... params){
        return findOne(sql, null, params);
    }

    public T findOne(String sql, PreparedStatementSetter pss){
        return findOne(sql, pss, null);
    }

    public List<T> findAll(String sql, JDBCParam... params){
        return findAll(sql, null, params);
    }

    public List<T> findAll(String sql, PreparedStatementSetter pss){
        return findAll(sql, pss, null);
    }

    public Page<T> findAll(String sql, int pageNum, int pageSize, JDBCParam... params){
        return findAll(sql, pageNum, pageSize, null, params);
    }

    public Page<T> findAll(String sql, int pageNum, int pageSize, PreparedStatementSetter pss){
        return findAll(sql, pageNum, pageSize, pss, null);
    }

    public BatchIterator<T> findAllInBatch(String sql, int batchSize, JDBCParam... params){
        return findAllInBatch(sql, batchSize, null, params);
    }

    public BatchIterator<T> findAllInBatch(String sql, int batchSize, PreparedStatementSetter pss){
        return findAllInBatch(sql, batchSize, pss, null);
    }

    //=================== Returns Map ===================

    public List<Map<String, Object>> findAllRowsAsMap(String sql, JDBCParam... params){
        return findAllRowsAsMap(sql, null, params);
    }

    public List<Map<String, Object>> findAllRowsAsMap(String sql, PreparedStatementSetter pss){
        return findAllRowsAsMap(sql, pss, null);
    }

    public Page<Map<String, Object>> findAllRowsAsMap(String sql, int pageNum, int pageSize, JDBCParam... params){
        return findAllRowsAsMap(sql, pageNum, pageSize, null, params);
    }

    public Page<Map<String, Object>> findAllRowsAsMap(String sql, int pageNum, int pageSize, PreparedStatementSetter pss){
        return findAllRowsAsMap(sql, pageNum, pageSize, pss, null);
    }

    public BatchIterator<Map<String, Object>> findAllRowsAsMapInBatch(String sql, int batchSize, JDBCParam... params){
        return findAllRowsAsMapInBatch(sql, batchSize, null, params);
    }

    public BatchIterator<Map<String, Object>> findAllRowsAsMapInBatch(String sql, int batchSize, PreparedStatementSetter pss){
        return findAllRowsAsMapInBatch(sql, batchSize, pss, null);
    }




    public long count(String sql, PreparedStatementSetter pss){
        return count(sql, pss, null);
    }

    public long count(String sql, JDBCParam... params){
        return count(sql, null, params);
    }


    private Page<T> findAll(String sql, int pageNum, int pageSize, PreparedStatementSetter pss, JDBCParam... params){
        try(Connection connection = getConnection()){
            if(null == pss) {
                return dumbResultSetMapper.findAll(connection, sql, pageNum, pageSize, params);
            }
            return dumbResultSetMapper.findAll(connection, sql, pageNum, pageSize, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private long count(String sql, PreparedStatementSetter pss, JDBCParam... params){
        try(Connection connection = getConnection()){
            if(null == pss) {
                return dumbResultSetMapper.count(connection, sql, params);
            }
            return dumbResultSetMapper.count(connection, sql, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private List<T> findAll(String sql, PreparedStatementSetter pss, JDBCParam... params){
        try(Connection connection = getConnection()){
            if(null == pss){
                return dumbResultSetMapper.findAll(connection, sql, params);
            }
            return dumbResultSetMapper.findAll(connection, sql, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private T findOne(String sql, PreparedStatementSetter pss, JDBCParam... params){
        try(Connection connection = getConnection()){
            if(null == pss) {
                return dumbResultSetMapper.findOne(connection, sql, params);
            }
            return dumbResultSetMapper.findOne(connection, sql, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private List<Map<String, Object>> findAllRowsAsMap(String sql, PreparedStatementSetter pss, JDBCParam... params){
        try(Connection connection = getConnection()){
            if(null == pss){
                return dumbResultSetMapper.findAllRowsAsMap(connection, sql, params);
            }
            return dumbResultSetMapper.findAllRowsAsMap(connection, sql, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private Page<Map<String, Object>> findAllRowsAsMap(String sql, int pageNum, int pageSize, PreparedStatementSetter pss, JDBCParam... params){
        try(Connection connection = getConnection()){
            if(null == pss){
                return dumbResultSetMapper.findAllRowsAsMap(connection, sql, pageNum, pageSize,  params);
            }
            return dumbResultSetMapper.findAllRowsAsMap(connection, sql, pageNum, pageSize, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private BatchIterator<Map<String, Object>> findAllRowsAsMapInBatch(String sql, int batchSize, PreparedStatementSetter pss, JDBCParam... params){
        try{
            Connection connection = getConnection();
            if(null == pss) {
                return dumbResultSetMapper.findAllRowsAsMapInBatch(connection, sql, batchSize, params);
            }
            return dumbResultSetMapper.findAllRowsAsMapInBatch(connection, sql, batchSize, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    private BatchIterator<T> findAllInBatch(String sql, int batchSize, PreparedStatementSetter pss, JDBCParam... params){
        try{
            Connection connection = getConnection();
            if(null == pss) {
                return dumbResultSetMapper.findAllInBatch(connection, sql, batchSize, params);
            }
            return dumbResultSetMapper.findAllInBatch(connection, sql, batchSize, pss);
        }catch (SQLException e){
            throw new RuntimeException(e);
        }
    }

    public interface ConnectionProvider{
        Connection getConnection() throws SQLException;
    }

    protected Connection getConnection() throws SQLException {
        return connectionProvider.getConnection();
    }


}
