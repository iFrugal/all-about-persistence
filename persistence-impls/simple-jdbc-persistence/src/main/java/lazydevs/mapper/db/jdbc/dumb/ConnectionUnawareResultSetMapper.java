package lazydevs.mapper.db.jdbc.dumb;

import lazydevs.mapper.db.jdbc.JDBCParam;
import lazydevs.mapper.db.jdbc.annotation.Column;
import lazydevs.mapper.db.jdbc.simple.PreparedStatementSetter;
import lazydevs.mapper.db.jdbc.util.SimpleUtils;
import lazydevs.mapper.db.jdbc.util.ResultSetBatchIterator;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.reader.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static java.lang.String.format;

public class ConnectionUnawareResultSetMapper<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionUnawareResultSetMapper.class );

    protected Class<T> type;

    public ConnectionUnawareResultSetMapper(Class<T> type) {
        this.type = type;
    }

    public long count(Connection connection, String sql, JDBCParam... params){
        return count(connection, sql, null, params);
    }

    public long count(Connection connection, String sql, PreparedStatementSetter pss){
        return count(connection, sql, pss, null);
    }

    /** throws IllegalStateException if there are more than one rows found
     * with message "More than 1 record found."
     */
    public T findOne(Connection connection, String sql, JDBCParam... params) {
        List<T> list = findAllAndConvert(new ListContainer(), connection, sql, true, null, params);
        return (null == list || list.isEmpty()) ? null : list.get(0);
    }

    public List<T> findAll(Connection connection, String sql, JDBCParam... params) {
        return findAllAndConvert(new ListContainer(), connection, sql, false, null, params);
    }

    /** <b>memory-leak candidate</b>
     *  <br>Make sure to call this method in try-with-resource block, or close it manually.
     *  <br>If it is not closed, <tt>java.sql.Statement</tt> and  <tt>java.sql.ResultSet</tt> will not be closed.
     */
    public BatchIterator<T> findAllInBatch(Connection connection, String sql, int batchSize, JDBCParam... params) {
        return findAllInBatchInternal(false, connection, sql, batchSize, null, params);
    }

    public T findOne(Connection connection, String sql, PreparedStatementSetter pss) {
        List<T> list = findAllAndConvert(new ListContainer(), connection, sql, true, pss);
        return (null == list || list.isEmpty()) ? null : list.get(0);
    }

    public List<T> findAll(Connection connection, String sql, PreparedStatementSetter pss) {
        return findAllAndConvert(new ListContainer(), connection, sql, false, pss);
    }

    public BatchIterator<T> findAllInBatch(Connection connection, String sql, int batchSize, PreparedStatementSetter pss) {
        return findAllInBatchInternal(false, connection, sql, batchSize, pss, null);
    }

    public List<Map<String, Object>> findAllRowsAsMap(Connection connection, String sql, PreparedStatementSetter pss) {
        return findAllAndConvert(new RowMapContainer(), connection, sql, false, pss, null);
    }

    public List<Map<String, Object>> findAllRowsAsMap(Connection connection, String sql, JDBCParam... params) {
        return findAllAndConvert(new RowMapContainer(), connection, sql, false, null, params);
    }

    public BatchIterator<Map<String, Object>> findAllRowsAsMapInBatch(Connection connection, String sql, int batchSize, JDBCParam... params) {
        return findAllInBatchInternal(true, connection, sql, batchSize, null, params);
    }

    public BatchIterator<Map<String, Object>> findAllRowsAsMapInBatch(Connection connection, String sql, int batchSize, PreparedStatementSetter pss) {
        return findAllInBatchInternal(true, connection, sql, batchSize, pss, null);
    }

    public Page<T> findAll(Connection connection, String sql, int pageNum, int pageSize, JDBCParam... params){
        return findAll(connection, sql, pageNum, pageSize, null, params);
    }

    public Page<T> findAll(Connection connection, String sql, int pageNum, int pageSize, PreparedStatementSetter pss){
        return findAll(connection, sql, pageNum, pageSize, pss, null);
    }

    public Page<Map<String, Object>> findAllRowsAsMap(Connection connection, String sql, int pageNum, int pageSize, JDBCParam... params){
        return findAllRowsAsMap(connection, sql, pageNum, pageSize, null, params);
    }

    public Page<Map<String, Object>> findAllRowsAsMap(Connection connection, String sql, int pageNum, int pageSize, PreparedStatementSetter pss){
        return findAllRowsAsMap(connection, sql, pageNum, pageSize, pss, null);
    }

    private Page<T> findAll(Connection connection, String sql, int pageNum, int pageSize, PreparedStatementSetter pss, JDBCParam... params){
        throw new UnsupportedOperationException("The pagination functionality is not yet implemented. Do you want to contribute ? If yes, then contact Abhijeet Rai.");
    }

    private Page<Map<String, Object>> findAllRowsAsMap(Connection connection, String sql, int pageNum, int pageSize, PreparedStatementSetter pss, JDBCParam... params){
        throw new UnsupportedOperationException("The pagination functionality is not yet implemented. Do you want to contribute ? If yes, then contact Abhijeet Rai.");
    }

    private <X> X findAllAndConvert(final Container<X> container, Connection connection, String sql, boolean isOneRequired, PreparedStatementSetter pss, JDBCParam... params){
        String debugMessage = null;
        try(PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            if(null == pss) {
                debugMessage = format("SQL = \"%s\" | Params = %s", sql, null == params ? null: Arrays.asList(params));
                setValuesInPreparedStatement(preparedStatement, params);
            }else{
                pss.setValues(preparedStatement);
                debugMessage = format("SQL = \"%s\" | ParamsValuesInOrder = %s", sql, "Using provided PreparedStatementSetter to set preparedStatement");
            }
            LOGGER.trace(debugMessage);
            try(ResultSet rs = preparedStatement.executeQuery()){
                boolean isOneFetched = false;
                while(rs.next()){
                    if(isOneRequired && isOneFetched){
                        throw new IllegalStateException("More than 1 record found.");
                    }
                    container.contain(rs);
                    isOneFetched = true;
                }
            }
        }catch(SQLException e){
            throw new RuntimeException("Exception occurred while querying. "+ debugMessage, e);
        }
        return container.x;
    }

    private <X> ResultSetBatchIterator<X> findAllInBatchInternal(boolean isMap, Connection connection, String sql, int batchSize, PreparedStatementSetter pss, JDBCParam... params){
        String debugMessage = null;
        try{
            PreparedStatement ps = connection.prepareStatement(sql);
            if(null == pss) {
                debugMessage = format("SQL = \"%s\" | Params = %s", sql, null == params ? null: Arrays.asList(params));
                setValuesInPreparedStatement(ps, params);
            }else{
                pss.setValues(ps);
                debugMessage = format("SQL = \"%s\" | ParamsValuesInOrder = %s", sql, "Using provided PreparedStatementSetter to set preparedStatement");
            }
            LOGGER.trace(debugMessage);
            ps.setFetchSize(batchSize);
            return new ResultSetBatchIterator<X>(ps.executeQuery(), batchSize, ps,connection) {
                @Override
                public X map(ResultSet rs, Set<String> columnsAvailable) {
                    if(isMap){
                        return (X) convertRowToMap(rs);
                    }
                    return (X) convertToType(rs, columnsAvailable);
                }
            };
        }catch(SQLException e){
            throw new RuntimeException("Exception occurred while querying. "+ debugMessage, e);
        }
    }

    private abstract class Container<X>{
        private final X x;
        protected Container(X x) {
            this.x = x;
        }
        abstract void contain(ResultSet rs) throws SQLException;
    }

    private class ListContainer extends Container<List<T>> {
        protected ListContainer() {
            super(new ArrayList<>());
        }
        @Override
        void contain(ResultSet rs) throws SQLException {
            super.x.add(convertToType(rs));
        }
    };

    private class RowMapContainer extends Container<List<Map<String, Object>>> {
        protected RowMapContainer() {
            super(new ArrayList<>());

        }
        @Override
        void contain(ResultSet rs) {
            super.x.add(convertRowToMap(rs));
        }
    };

    protected void setValuesInPreparedStatement(PreparedStatement ps, JDBCParam... params) throws RuntimeException {
        if(null == params || params.length == 0){
            return;
        }
        int index = 0;
        for (JDBCParam param : params) {
            try{
                if(null == param.getType()){
                    param.setType(param.getField(type).getAnnotation(Column.class).dbType());
                }
                Object value = param.getValue();
                JDBCParam.setPreparedStatement(ps, ++index, value, param.getType());
            }catch(Exception e){
                throw new RuntimeException(format("Exception occurred while setting the param = %s from params = %s ", param, Arrays.asList(params)), e);
            }
        }
    }

    protected Map<String, Object> convertRowToMap(ResultSet rs){
        try {
            Map<String, Object> row = new LinkedHashMap<>();
            int count = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= count; i++) {
                row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
            }
            return row;
        }catch (SQLException e){
            throw new RuntimeException("", e);
        }
    }


    protected T convertToType(ResultSet rs, Set<String> columnsAvailable){
        try{
            T t = (T)type.newInstance();
            for (Field field: type.getDeclaredFields()) {
                setField(rs, columnsAvailable, t, field);
            }
            return t;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    private void setField(ResultSet rs, Set<String> columnsAvailable, T t, Field field) throws IllegalAccessException {
        if(field.isAnnotationPresent(Column.class)){
            Column column = field.getAnnotation(Column.class);
            field.setAccessible(true);
            Object value;
            String columnName = null;
            try {
                columnName = SimpleUtils.getColumnName(field);
                value = JDBCParam.getValue(column.dbType(), columnName, rs, columnsAvailable);
            }catch(Exception e){
                throw new RuntimeException("Exception occurred while getting the value for name = "+ columnName+ "; type = "+column.dbType(), e);
            }
            if(!("".equals(column.converterMethod()))){
                try{
                    Method converterMethod  = type.getDeclaredMethod(column.converterMethod(), column.javaType());
                    converterMethod.setAccessible(true);
                    value = converterMethod.invoke(t, value);
                }catch(Exception e){
                    throw new RuntimeException("value = "+ value + format("Exception occurred while mapping class = %s,field = %s, converterMethod = %s",type.toString(), field.getName(), column.converterMethod()) , e);
                }
            }
            if(value != null) {
                field.set(t, value);
            }
        }
    }

    protected T convertToType(ResultSet rs) throws SQLException {
         final Set<String> columnNamesAvailableinRs = new LinkedHashSet<>();
        for(int i = 1; i <=rs.getMetaData().getColumnCount(); i++){
            columnNamesAvailableinRs.add(rs.getMetaData().getColumnLabel(i).toLowerCase());
        }
       return convertToType(rs, columnNamesAvailableinRs);
    }

    private long count(Connection connection, String sql, PreparedStatementSetter pss, JDBCParam... params){
        List<Map<String, Object>> list = findAllAndConvert(new RowMapContainer(), connection, sql, true, pss, params);
        if(null == list || list.size() == 0){
            throw new IllegalArgumentException("The count Sql Query did not returned any row. Rows = "+list);
        }
        if(1 != list.get(0).entrySet().size()){
            throw new IllegalArgumentException("The count Sql Query returned 1 row with more than 1 column. Rows = "+list);
        }
        Object val = list.get(0).entrySet().iterator().next().getValue();
        if(!(val instanceof Long)){
            if(!(val instanceof Integer)) {
                throw new IllegalArgumentException(String.format("The count Sql Query returned 1 row with 1 column, but the column is not of type 'long'. Type = '%s'. Rows = %s", val.getClass(), list));
            }else{
                return Long.valueOf(val.toString());
            }
        }
        return (long) val;
    }
}
