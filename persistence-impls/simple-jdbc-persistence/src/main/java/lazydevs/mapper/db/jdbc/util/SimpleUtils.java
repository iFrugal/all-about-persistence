package lazydevs.mapper.db.jdbc.util;

import lazydevs.mapper.db.jdbc.annotation.Column;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SimpleUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleUtils.class );

    public static String getColumnName(Field field){
        Column column;
        if(field.isAnnotationPresent(Column.class) && !(column = field.getAnnotation(Column.class)).name().equals("")){
            return column.name();
        }
        return field.getName();
    }

    public static void close(ResultSet resultSet, Statement statement, Connection connection){
        try {
            if (null != resultSet && !resultSet.isClosed()) {
                resultSet.close();
            }
            if(null != statement && !statement.isClosed()){
                statement.close();
            }
            if(null != connection && !connection.isClosed()){
                connection.close();
            }
        }catch (Exception e){
            throw new RuntimeException("Error while closing ResultSet/Statement/Connection", e);
        }
    }

    public static void closeQuietly(ResultSet resultSet, Statement statement, Connection connection){
        try {
            close(resultSet, statement, connection);
        }catch (Exception e){
            LOGGER.error("Error while closing ResultSet", e);
        }
    }
}
