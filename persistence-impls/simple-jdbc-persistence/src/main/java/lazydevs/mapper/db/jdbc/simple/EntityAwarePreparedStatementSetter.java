package lazydevs.mapper.db.jdbc.simple;


import lazydevs.mapper.db.jdbc.JDBCParam;
import lazydevs.mapper.db.jdbc.annotation.Column;
import lazydevs.persistence.jdbc.general.JdbcParam;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

import static java.lang.String.format;

public interface EntityAwarePreparedStatementSetter<T> {
    void setValues(PreparedStatement preparedStatement, T t) throws SQLException;
    default void setValues(PreparedStatement ps, JdbcParam... params){
        if(null == params || params.length == 0){
            return;
        }
        int index = 0;
        for (JDBCParam param : params) {
            try{
                Object value = param.getValue();
                JDBCParam.setPreparedStatement(ps, ++index, value, param.getType());
            }catch(Exception e){
                throw new RuntimeException(format("Exception occurred while setting the param = %s from params = %s ", param, Arrays.asList(params)), e);
            }
        }
    }
}
