package lazydevs.persistence.jdbc.general;

import lazydevs.mapper.db.jdbc.JDBCParam;
import lazydevs.mapper.db.jdbc.JdbcRepository;
import lazydevs.mapper.db.jdbc.simple.EntityAwarePreparedStatementSetter;
import lazydevs.mapper.utils.engine.TemplateEngine;
import lazydevs.persistence.connection.ConnectionProvider;
import lazydevs.persistence.writer.general.GeneralUpdater;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * @author Abhijeet Rai
 */
@Slf4j
public class JdbcGeneralUpdater implements GeneralUpdater<JdbcOperation, JdbcOperation> {
    private final ConnectionProvider<DataSource> connectionProvider;

    public JdbcGeneralUpdater(ConnectionProvider<DataSource> connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public JdbcGeneralUpdater(DataSource dataSource){
        this(() ->  dataSource);
    }

    @Override
    public Map<String, Object> replace(Map<String, Object> row, JdbcOperation jdbcOperation) {
        executeUpdate(row, jdbcOperation);
        return row;
    }

    @Override
    public List<Map<String, Object>> replace(List<Map<String, Object>> list, JdbcOperation jdbcOperation) {
        executeUpdate(list, jdbcOperation);
        return list;
    }

    @Override
    public Map<String, Object> updateOne(String id, Map<String, Object> fieldsToUpdate, JdbcOperation jdbcOperation) {
        Map<String, Object> params =  new LinkedHashMap<>(fieldsToUpdate);
        params.put("id", id);
        JdbcOperation operation = new JdbcOperation();
        operation.setNativeSQL(jdbcOperation.getNativeSQL());
        List<JdbcParam> list = new ArrayList<>(jdbcOperation.getParams());
        list.add(new JdbcParam("id", JDBCParam.Type.STRING, id));
        operation.setParams(list);
        executeUpdate(params, operation);
        return null;
    }

    @Override
    public long updateMany(JdbcOperation query, Map<String, Object> fieldsToUpdate, JdbcOperation jdbcOperation) {
        return executeUpdate(fieldsToUpdate, jdbcOperation);
    }

    @Override
    public Map<String, Object> delete(Map<String, Object> t, JdbcOperation jdbcOperation) {
        executeUpdate(t, jdbcOperation);
        return null;
    }

    @Override
    public Map<String, Object> delete(String id, JdbcOperation jdbcOperation) {
        throw new UnsupportedOperationException("The method is not supported, yet !!");
    }

    @Override
    public Map<String, Object> update(Map<String, Object> row, JdbcOperation jdbcOperation) {
        executeUpdate(row, jdbcOperation);
        return row;
    }

    @Override
    public List<Map<String, Object>> update(List<Map<String, Object>> rows,  JdbcOperation jdbcOperation) {
        executeUpdate(rows, jdbcOperation);
        return rows;
    }

    @Override
    public Map<String, Object> create(Map<String, Object> row, JdbcOperation jdbcOperation) {
        executeUpdate(row, jdbcOperation);
        return row;
    }

    @Override
    public List<Map<String, Object>> create(List<Map<String, Object>> rows,  JdbcOperation jdbcOperation) {
        executeUpdate(rows, jdbcOperation);
        return rows;
    }


    private long executeUpdate(Map<String, Object> row, JdbcOperation jdbcOperation) {
        return getJdbcRepository().executeUpdate(jdbcOperation.getNativeSQL(), row, (EntityAwarePreparedStatementSetter<Map<String, Object>>) (preparedStatement, o) -> setValuesInPreparedStatement(row, preparedStatement, jdbcOperation.getParamsAsArr()));

    }

    private long executeUpdate(List<Map<String, Object>> rows,  JdbcOperation jdbcOperation) {
        return getJdbcRepository().executeUpdate(jdbcOperation.getNativeSQL(), rows, (EntityAwarePreparedStatementSetter<Map<String, Object>>) (preparedStatement, row) -> setValuesInPreparedStatement(row, preparedStatement, jdbcOperation.getParamsAsArr()));
    }

    @Override
    public Class<JdbcOperation> getWriteInstructionType() {
        return JdbcOperation.class;
    }

    private JdbcRepository<Map<String, Object>> getJdbcRepository(){
        return new JdbcRepository(connectionProvider.getConnection(), Map.class);
    }

    public static void setValuesInPreparedStatement(Map<String, Object> row, PreparedStatement ps, JdbcParam... params) {
        if(null == params || params.length == 0){
            return;
        }
        int index = 0;
        for (JdbcParam param : params) {
            try{
                Object value = null;
                if(param.isNameTemplate()){
                    value = TemplateEngine.getInstance().generate(param.getName(), row);
                }else{
                    value = row.get(param.getName());
                }
                setPreparedStatement(ps, ++index, value, param.getType());
            }catch(Exception e){
                throw new RuntimeException(format("Exception occurred while setting the param = %s from params = %s ", param, Arrays.asList(params)), e);
            }
        }
    }

    public static void setPreparedStatement(PreparedStatement ps, int index, Object value, JDBCParam.Type type) throws SQLException {
        if(null == value){
            ps.setNull(index, getSqlType(type));
            return;
        }
        if(value instanceof String){
            String stringVal = (String) value;
            switch (type) {
                case STRING: ps.setString(index, stringVal); break;
                case INTEGER: ps.setInt(index, Integer.valueOf(stringVal)); break;
                case SHORT: ps.setShort(index, Short.valueOf(stringVal)); break;
                case LONG:  ps.setLong(index, Long.valueOf(stringVal)); break;
                case DOUBLE:  ps.setDouble(index, Double.valueOf(stringVal));break;
                case BOOLEAN: ps.setBoolean(index, Boolean.valueOf(stringVal)); break;
                case TIMESTAMP: ps.setTimestamp(index, Timestamp.valueOf(stringVal)); break;
                default: throw new RuntimeException("Unrecognized JDBCParamType for the method setPreparedStatement(). Type = "+ type);
            }
        }else {
            switch (type) {
                case STRING:
                    ps.setString(index, String.valueOf(value));
                    break;
                case INTEGER:
                    ps.setInt(index, (Integer)value);
                    break;
                case SHORT:
                    ps.setShort(index, (Short) value);
                    break;
                case LONG:
                    ps.setLong(index, (Long) value);
                    break;
                case DOUBLE:
                    ps.setDouble(index, (Double) value);
                    break;
                case DATE:
                case TIMESTAMP:
                    ps.setTimestamp(index, new Timestamp(((Date) value).getTime()));
                    break;
                case BOOLEAN:
                    ps.setBoolean(index, (Boolean) value);
                    break;
                default:
                    throw new IllegalArgumentException("setPreparedStatement(): Unrecognized JDBCParamType for the method setPreparedStatement(). Type = " + type);
            }
        }
    }

    private static int getSqlType(JDBCParam.Type type){
        switch (type) {
            case STRING: return Types.VARCHAR;
            case INTEGER: return Types.INTEGER;
            case SHORT: return Types.SMALLINT;
            case LONG:  return Types.BIGINT;
            case DOUBLE:  return Types.DOUBLE;
            case DATE: case TIMESTAMP:  return Types.TIMESTAMP;
            case BOOLEAN: return Types.BIT;
            default: throw new IllegalArgumentException("getSqlType(): Unrecognized JDBCParamType for the method setPreparedStatement(). Type = "+ type);
        }
    }

    private JdbcGeneralReader getGeneralReader(){
        return new JdbcGeneralReader(connectionProvider.getConnection());
    }

    private Set<String> getIdsWhichExistsInDb(List<Map<String, Object>> rows, String uniqueColumnName, JdbcOperation selectJdbcOperation){
        String selectStatement = selectJdbcOperation.getNativeSQL();
        String csv = rows.stream().map(row -> String.valueOf(row.get(uniqueColumnName))).collect(Collectors.joining("','"));
        csv = "'".concat(csv.concat("'"));
        selectStatement = selectStatement.replace("?", csv);
        return getGeneralReader().findAll(new JdbcOperation(selectStatement))
                .stream()
                .map(rowFoundInDb -> String.valueOf(rowFoundInDb.get(uniqueColumnName)))
                .collect(Collectors.toSet());
    }

    @Override
    public List<Map<String, Object>> createOrReplace(List<Map<String, Object>> rows,  JdbcOperation jdbcOperation){
        JdbcOperation select = jdbcOperation.getCreateOrReplaceOperation().getSelect();
        String uniqueColumnName = select.getParams().get(0).getName();
        Set<String> uniqueIdsFoundInDb = getIdsWhichExistsInDb(rows, uniqueColumnName, select);
        Map<String, Map<String, Object>> insertRows = new HashMap<>();
        Map<String, Map<String, Object>> updateRows = new HashMap<>();
        rows.forEach(row -> {
            String uniqueColVal = String.valueOf(row.get(uniqueColumnName));
            if(uniqueIdsFoundInDb.contains(uniqueColVal)){
                updateRows.put(uniqueColVal, row);
            }else{
                insertRows.put(uniqueColVal, row);
            }
        });

        executeUpdate("insert", new ArrayList<>(insertRows.values()), jdbcOperation.getCreateOrReplaceOperation().getInsert());
        executeUpdate("update", new ArrayList<>(updateRows.values()), jdbcOperation.getCreateOrReplaceOperation().getUpdate());
        return rows;
    }

    private void executeUpdate(String action, List<Map<String, Object>> rows,  JdbcOperation jdbcOperation){
        if(null == jdbcOperation){
            log.info("JdbcOperation for action = {} is null, Skipping action", action);
            return;
        }
        long noOfRecordsAffected = executeUpdate(rows, jdbcOperation);
        log.debug("Action = {}, noOfRecordsAffected = {}", action, noOfRecordsAffected);
    }

    @Override
    public Map<String, Object> createOrReplace(Map<String, Object> row,  JdbcOperation jdbcOperation){
        List<Map<String, Object>> rows = new ArrayList<>();
        rows.add(row);
        createOrReplace(rows,jdbcOperation);
        return row;
    }
}
