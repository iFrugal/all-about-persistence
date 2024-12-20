package lazydevs.persistence.jdbc.general;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lazydevs.mapper.db.jdbc.JDBCParam;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;


/**
 * @author Abhijeet Rai
 */
@Getter
@Setter
@ToString
public class JdbcOperation {
    private String nativeSQL;
    private List<JdbcParam> params;
    private CreateOrReplaceOperation createOrReplaceOperation;

    @Getter@Setter
    public static class CreateOrReplaceOperation
    {
        JdbcOperation select;
        JdbcOperation update;
        JdbcOperation insert;

        public JdbcOperation getSelect() {
            return select;
        }

        public void setSelect(JdbcOperation select) {
            this.select = select;
        }

        public JdbcOperation getUpdate() {
            return update;
        }

        public void setUpdate(JdbcOperation update) {
            this.update = update;
        }

        public JdbcOperation getInsert() {
            return insert;
        }

        public void setInsert(JdbcOperation insert) {
            this.insert = insert;
        }
    }

    public JdbcOperation(){

    }
    public JdbcOperation(String nativeSQL, JdbcParam... params) {
        this.nativeSQL = nativeSQL;
        this.params = null == params || params.length == 0 ? null : Arrays.asList(params);
    }
    public static JdbcOperation nativeSql(String nativeSQL) {
        return new JdbcOperation(nativeSQL);
    }

    public JdbcOperation params(JdbcParam... params){
        this.params = null == params || params.length == 0 ? null : Arrays.asList(params);
        return this;
    }


    public JdbcOperation values(Object... values){
        if(null != values && values.length > 0){
            this.params = Arrays.asList(values).stream().map(value ->{
                if(value instanceof JdbcParam) {
                    return (JdbcParam) value;
                } else if(Objects.isNull(value)) {
                    return new JdbcParam();
                }
                else {
                    return new JdbcParam(value);
                }
            }).collect(toList());
        }
        return this;
    }

    @JsonIgnore
    public JdbcParam[] getParamsAsArr(){
        if(null == params){
            return null;
        }else{
            JdbcParam[] paramArr = new JdbcParam[params.size()];
            params.toArray(paramArr);
            return paramArr;
        }
    }

    public void validate(){
        params.forEach(jdbcParam -> {
            if(null == jdbcParam.getType()){
                throw new IllegalArgumentException("jdbcParam.getType() is set to null. jdbcParam = "+ jdbcParam +". jdbcOperation = "+ this.toString());
            }
        });
    }

    public String getNativeSQL() {
        return nativeSQL;
    }

    public void setNativeSQL(String nativeSQL) {
        this.nativeSQL = nativeSQL;
    }

    public List<JdbcParam> getParams() {
        return params;
    }

    public void setParams(List<JdbcParam> params) {
        this.params = params;
    }

    public CreateOrReplaceOperation getCreateOrReplaceOperation() {
        return createOrReplaceOperation;
    }

    public void setCreateOrReplaceOperation(CreateOrReplaceOperation createOrReplaceOperation) {
        this.createOrReplaceOperation = createOrReplaceOperation;
    }
}
