package lazydevs.mapper.db.jdbc;

import lazydevs.mapper.db.jdbc.annotation.Column;
import lazydevs.persistence.reader.Param;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static java.lang.String.format;

import static java.util.Objects.hash;
import static lazydevs.mapper.db.jdbc.JDBCParam.Type.*;

@ToString
public class JDBCParam extends Param {
    @Getter @Setter private Type type;

    public JDBCParam(){}

    public JDBCParam(Object value){
        this(Type.get(value.getClass()),value);
    }

    public JDBCParam(String name, Type type, Object value) {
        super(name, value);
        this.type = type;
    }

    public JDBCParam(Type type, Object value) {
        super(UUID.randomUUID().toString(), value);
        this.type = type;
    }

    public JDBCParam(String name, Object value) {
        super(name, value);
    }

    private static int getSqlType(Type type){
        switch (type) {
            case STRING: return Types.VARCHAR;
            case INTEGER: return Types.INTEGER;
            case SHORT: return Types.SMALLINT;
            case LONG:  return Types.BIGINT;
            case DOUBLE:  return Types.DOUBLE;
            case DATE: case TIMESTAMP:  return Types.TIMESTAMP;
            case BOOLEAN: return Types.BIT;
            default: throw new RuntimeException("Unrecognized JDBCParamType for the method setPreparedStatement(). Type = "+ type);
        }
    }

    public static void setPreparedStatement(PreparedStatement ps, int index, Object value, Type type) throws SQLException {
        if(null == value){
            ps.setNull(index, getSqlType(type));
            return;
        }
        if(value instanceof String){
            ps.setString(index, (String) value);
            return;
        }
        switch (type) {
            case STRING: ps.setString(index, (String)value); break;
            case INTEGER: ps.setInt(index, (Integer)value); break;
            case SHORT: ps.setShort(index, (Short) value); break;
            case LONG:  ps.setLong(index, (Long)value); break;
            case DOUBLE:  ps.setDouble(index, (Double)value);break;
            case DATE: case TIMESTAMP:  ps.setTimestamp(index, new Timestamp(((Date)value).getTime())); break;
            case BOOLEAN: ps.setBoolean(index, (Boolean)value); break;
            default: throw new RuntimeException("Unrecognized JDBCParamType for the method setPreparedStatement(). Type = "+ type);
        }
    }
    public static Object getValue(Type type, String columnName, ResultSet rs, Set<String> columnsAvailable) throws SQLException{
        if(null != columnsAvailable && !columnsAvailable.contains(columnName.toLowerCase())){
            return null;
        }
        if(null == rs.getObject(columnName)){
            return null;
        }
        switch (type) {
            case STRING: return rs.getString(columnName);
            case INTEGER: return rs.getInt(columnName);
            case SHORT : return rs.getShort(columnName);
            case LONG: return rs.getLong(columnName);
            case DOUBLE: return rs.getDouble(columnName);
            case DATE: case TIMESTAMP: return rs.getTimestamp(columnName);
            case BOOLEAN: return rs.getBoolean(columnName);
            default: throw new RuntimeException("Unrecognized JDBCParamType for the method getValue():"+ type);
        }
    }

    public static String getDbTypeAndPrecision(Type type, String precision){
        precision = precision.isEmpty() ? null : precision;
        switch (type) {
            case STRING: return String.format("VARCHAR(%s)", null == precision ? 256 : precision);
            case INTEGER: return String.format("INTEGER(%s)", null == precision ? 10 : precision);
            case SHORT: return String.format("SMALLINT(%s)", null == precision ? 6 : precision);
            case LONG: return String.format("BIGINT(%s)", null == precision ? 10 : precision);
            case DOUBLE: return String.format("NUMBER%s", null == precision ? "" : "("+precision+")");
            case DATE: case TIMESTAMP: return "TIMESTAMP";
            case BOOLEAN: return "BIT";
            default: throw new RuntimeException("Unrecognized JDBCParamType for the method getDbTypeAndPrecision(). Type = "+ type);
        }
    }

    public static String getCsvStringForInClause(List list, Type paramType){
        if(list.size() > 1000)
            throw new IllegalArgumentException("We don't support In-Clause list size > 1000.");
        StringBuilder sb = new StringBuilder();
        String prefix = "", suffix = "";
        if(STRING_ARR.equals(paramType)){
            prefix = "'"; suffix = "'";
            if(list.size() > 1 && !(String.class.equals(list.get(0).getClass()))){
                throw new IllegalStateException("The paramType passed was STRING_ARR but the element inside is of type = "+list.get(0).getClass());
            }
        }
        else if(INTEGER_ARR.equals(paramType) || LONG_ARR.equals(paramType)){
            if(list.size() > 1 && !(Integer.class.equals(list.get(0).getClass())) || Long.class.equals(list.get(0).getClass())){
                throw new IllegalStateException("The paramType passed was INTEGER_ARR/LONG_ARR but the element inside is of type = "+list.get(0).getClass());
            }
        }
        else throw new IllegalArgumentException("Unsupported method getCsvStringForInClause(List list, Class<?> paramType); for paramType = "+ paramType);
        for (Object object : list) {
            sb.append(prefix).append(object.toString()).append(suffix).append(",");
        }
        return sb.substring(0, sb.lastIndexOf(","));
    }


    public static enum Type {
        STRING,
        INTEGER,
        SHORT,
        LONG,
        DATE,
        BOOLEAN,
        TIMESTAMP,
        DOUBLE,
        STRING_ARR,
        INTEGER_ARR,
        LONG_ARR,
        SHORT_ARR;

        public boolean isSupportedArray(){
            return this.equals(STRING_ARR) || this.equals(INTEGER_ARR) || this.equals(LONG_ARR) || this.equals(SHORT_ARR);
        }

        public static Type get(Class javaType){
            String javaTypeName = javaType.getName();
            switch(javaTypeName){
                case "java.lang.String" : return STRING;
                case "java.lang.Long" : return LONG;
                case "java.lang.Integer" : return INTEGER;
                case "java.lang.Short" : return SHORT;
                case "java.lang.Double" : return DOUBLE;
                case "java.util.Date" : return DATE;
                case "java.lang.Boolean" : return BOOLEAN;
                default:
            }
            return STRING;
        }
    }

    public Field getField(Class<?> entityType) {
        Field field;
        try {
            field = entityType.getDeclaredField(this.getName());
        }catch (NoSuchFieldException e){
            throw new IllegalArgumentException(format("No field found with name '%s' in type = '%s'", this.getName(), entityType));
        }
        if(!field.isAnnotationPresent(Column.class)) {
            throw new IllegalArgumentException(format("Field with name '%s' in type = '%s' is not annotated with '%s'", this.getName(), entityType, Column.class));
        }
        return field;
    }

    @Override
    public int hashCode() {
        return hash(this.getName(), this.getValue(),  type);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JDBCParam) {
            JDBCParam jdbcParam = (JDBCParam) o;

            return Objects.equals(this.getName(), jdbcParam.getName())
                    && Objects.equals(this.getValue(), jdbcParam.getValue())
                    && Objects.equals(type, jdbcParam.type);
        }

        return false;
    }

}
