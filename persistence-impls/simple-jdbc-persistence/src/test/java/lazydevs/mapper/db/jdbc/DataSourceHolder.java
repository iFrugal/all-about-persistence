package lazydevs.mapper.db.jdbc;

import lazydevs.mapper.utils.PropertyUtils;
import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static lazydevs.mapper.utils.PropertyUtils.getValue;

public class DataSourceHolder {
    private static final DataSourceHolder INSTANCE = new DataSourceHolder();

    public static DataSourceHolder getInstance() {
        return INSTANCE;
    }

    public DataSource getDataSource(){
        return this.dataSource;
    }
    private final DataSource dataSource;

    private DataSourceHolder(){
        dataSource = buildDataSource();
    }

    private final String CREATE_TABLE_BADBOY = "CREATE TABLE if not exists BADBOY (NAME VARCHAR2(256), ROLLNO SMALLINT)";
    private final String CREATE_TABLE_A = "CREATE TABLE if not exists A_TABLE (NAME VARCHAR2(256), ROLLNO SMALLINT)";


    private final String[] CREATE_TABLE_STATEMENTS = {CREATE_TABLE_BADBOY, CREATE_TABLE_A};

    private DataSource buildDataSource(){
        PropertyUtils.load("src/test/resources/config.properties");
        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(getValue("db.driver"));
        basicDataSource.setUrl(getValue("db.url"));
        basicDataSource.setUsername(getValue("db.username"));
        basicDataSource.setPassword(getValue("db.password"));
        basicDataSource.setInitialSize(Integer.parseInt(getValue("db.pool.initialSize")));
        basicDataSource.setMaxIdle(Integer.parseInt(getValue("db.pool.maxIdle")));
        basicDataSource.setMinIdle(Integer.parseInt(getValue("db.pool.minIdle")));
        try(Connection connection = basicDataSource.getConnection(); Statement st1 = connection.createStatement();){

            for(String sql : new String[]{"DROP TABLE BADBOY", "DROP TABLE A_TABLE"}){
                try {
                    st1.execute(sql);
                }catch (Exception e){
                    //ignore..
                }
            }
            //Create Tables

            for(String string:CREATE_TABLE_STATEMENTS) {
                st1.execute(string);
            }


        }catch (Exception e){
            e.printStackTrace();
            //throw new RuntimeException("", e);
        }
        return basicDataSource;
    }
}
