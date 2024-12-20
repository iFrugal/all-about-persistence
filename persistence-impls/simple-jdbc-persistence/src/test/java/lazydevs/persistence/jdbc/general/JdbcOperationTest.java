package lazydevs.persistence.jdbc.general;

import lazydevs.mapper.db.jdbc.JDBCParam;
import lazydevs.mapper.utils.SerDe;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * @author Abhijeet Rai
 */
public class JdbcOperationTest {

    @Test
    public void simple(){
        JdbcOperation jdbcOperation = new JdbcOperation();
        jdbcOperation.setNativeSQL("select * from x where id = ? and name = ?");
        jdbcOperation.setParams(Arrays.asList(
                new JdbcParam("id", JDBCParam.Type.STRING, "id1"),
                new JdbcParam("name", JDBCParam.Type.STRING, "name1")
        ));
        System.out.println(SerDe.JSON.serialize(jdbcOperation, true));

        jdbcOperation = new JdbcOperation("select * from x where id = ? and name = ?",
                new JdbcParam("id", JDBCParam.Type.STRING, "id1"),
                new JdbcParam(true, "name", JDBCParam.Type.STRING, "name1")
        );
        System.out.println(SerDe.JSON.serialize(jdbcOperation, true));
    }


}