package lazydevs.mapper.db.jdbc.dumb;

import dto.A;
import lazydevs.mapper.db.jdbc.DataSourceHolder;
import lazydevs.mapper.db.jdbc.JDBCParam;
import lazydevs.mapper.db.jdbc.JdbcRepository;
import lazydevs.mapper.db.jdbc.simple.EntityAwarePreparedStatementSetter;
import lazydevs.mapper.utils.BatchIterator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

import static lazydevs.mapper.db.jdbc.JDBCParam.Type.INTEGER;
import static lazydevs.mapper.db.jdbc.JDBCParam.Type.STRING;
import static org.testng.Assert.assertEquals;

public class ConnectionUnawareResultSetMapperTest {

    private JdbcRepository<A> aRepository ;


    @BeforeClass
    public void setUp() {
        aRepository = new JdbcRepository<A>(DataSourceHolder.getInstance().getDataSource(), A.class);
        tearDown();
        aRepository.executeUpdate("insert into A_TABLE(name, rollNo) values(?, ?)", getListOfA(10), new EntityAwarePreparedStatementSetter<A>() {
            @Override
            public void setValues(PreparedStatement preparedStatement, A a) throws SQLException {
                preparedStatement.setString(1, a.getName());
                preparedStatement.setShort(2, a.getRollNo());
            }
        });
    }

    @AfterClass
    public void tearDown() {
        aRepository.execute("Delete from A_TABLE");
    }

    private ConnectionUnawareResultSetMapper<A> connectionUnawareResultSetMapper = new ConnectionUnawareResultSetMapper<>(A.class);
    @Test
    public void testCount() throws SQLException {
        try(Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection()) {
            //Count Simple --> WithOut params, without preparedStatementSetter
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE"), 10);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(1) from A_TABLE"), 10);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(2) from A_TABLE"), 10);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where name = 'User1'"), 1);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where name like 'User1%'"), 2);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo = 1"), 1);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo = 10"), 1);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo between 1 AND 10"), 10);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo > 2 AND rollNo < 8"), 5);

            //Count with Params
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where name = ?", new JDBCParam(STRING, "User1")), 1);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo = ?", new JDBCParam(INTEGER, 1)), 1);
            assertEquals(connectionUnawareResultSetMapper.count(connection,
                    "select count(*) from A_TABLE where rollNo = ? and name = ?"
                    , new JDBCParam(INTEGER, 1), new JDBCParam(STRING, "User1"))
            , 1);

            //Count with PreparedStatementSetter
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo = ? and name = ?", preparedStatement -> {
                preparedStatement.setShort(1, (short)10);
                preparedStatement.setString(2, "User10");
            }), 1);
            assertEquals(connectionUnawareResultSetMapper.count(connection, "select count(*) from A_TABLE where rollNo IN (1, 10) and name like ?", preparedStatement -> {
               // preparedStatement.setShort(1, (short)10);
                preparedStatement.setString(1, "User1%");
            }), 2);

        }
    }
    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "More than 1 record found.")
    public void testFindOneWithMoreThanOneRecords() throws SQLException{
        try(Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection()) {
            connectionUnawareResultSetMapper.findOne(connection, "select * from A_TABLE");
        }
    }

    @Test
    public void testFindOne() throws SQLException{
        try(Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection()) {
            assertEquals(connectionUnawareResultSetMapper.findOne(connection, "select * from A_TABLE where name = 'User2'"), new A("User2", 2));
            assertEquals(connectionUnawareResultSetMapper.findOne(connection, "select * from A_TABLE where name = ?", new JDBCParam(STRING, "User1")), new A("User1", 1));
            assertEquals(connectionUnawareResultSetMapper.findOne(connection, "select * from A_TABLE where name = ? and rollNo = ?", (ps)->{
                ps.setString(1, "User3");
                ps.setShort(2, (short)3);
            }), new A("User3", 3));
        }
    }

    @Test
    public void testFindAll() throws SQLException{
        try(Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection()) {
            assertEquals(connectionUnawareResultSetMapper.findAll(connection, "select * from A_TABLE where name = 'User2'"), Arrays.asList(new A("User2", 2)));
            assertEquals(connectionUnawareResultSetMapper.findAll(connection, "select * from A_TABLE where name like ? order by rollNo", new JDBCParam(STRING, "User1%"))
                    , Arrays.asList(new A("User1", 1), new A("User10", 10)));
            assertEquals(connectionUnawareResultSetMapper.findAll(connection, "select * from A_TABLE where name like ? order by rollNo", (ps)->{
                ps.setString(1, "User1%");
            }), Arrays.asList(new A("User1", 1), new A("User10", 10)));
        }
    }

    @Test
    public void testFindAllRowsAsMap() throws SQLException{
        try(Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection()) {
            List<Map<String, Object>> listFromDb = connectionUnawareResultSetMapper.findAllRowsAsMap(connection, "select * from A_TABLE where name = 'User2'");
            assertEquals(listFromDb, Arrays.asList(getMap(new A("User2", 2))));
            assertEquals(connectionUnawareResultSetMapper.findAllRowsAsMap(connection, "select * from A_TABLE where name like ? order by rollNo", new JDBCParam(STRING, "User1%"))
                    , Arrays.asList( getMap(new A("User1", 1)),  getMap(new A("User10", 10))));
            assertEquals(connectionUnawareResultSetMapper.findAllRowsAsMap(connection, "select * from A_TABLE where name like ? order by rollNo", (ps)->{
                ps.setString(1, "User1%");
            }), Arrays.asList( getMap(new A("User1", 1)),  getMap(new A("User10", 10))));
        }
    }

    //## ====   BATCHING Tests below ====

    @Test
    public void testFindAllInBatch() throws SQLException{
            Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection();
            try(BatchIterator<A> batchIterator = connectionUnawareResultSetMapper.findAllInBatch(connection, "select * from A_TABLE where rollNo BETWEEN 1 AND 3 order by rollNo", 2)){
                assertEquals(batchIterator.next(), Arrays.asList(new A("User1", 1), new A("User2", 2)));
                assertEquals(batchIterator.next(), Arrays.asList(new A("User3", 3)));
            }


            connection = DataSourceHolder.getInstance().getDataSource().getConnection();
            try(BatchIterator<A> batchIterator = connectionUnawareResultSetMapper.findAllInBatch(connection, "select * from A_TABLE where rollNo BETWEEN ? AND ? order by rollNo", 2
                        , new JDBCParam(INTEGER, 1), new JDBCParam(INTEGER, 3))){
                assertEquals(batchIterator.next(), Arrays.asList(new A("User1", 1), new A("User2", 2)));
                assertEquals(batchIterator.next(), Arrays.asList(new A("User3", 3)));
            }

            connection = DataSourceHolder.getInstance().getDataSource().getConnection();
            try(BatchIterator<A> batchIterator = connectionUnawareResultSetMapper.findAllInBatch(connection, "select * from A_TABLE where rollNo BETWEEN ? AND ? order by rollNo", 2
                    , (preparedStatement -> {
                        preparedStatement.setInt(1, 1);
                        preparedStatement.setInt(2, 3);
                    }))){
                assertEquals(batchIterator.next(), Arrays.asList(new A("User1", 1), new A("User2", 2)));
                assertEquals(batchIterator.next(), Arrays.asList(new A("User3", 3)));
            }

    }

    @Test
    public void testFindAllRowsAsMapInBatch() throws SQLException{
        Connection connection = DataSourceHolder.getInstance().getDataSource().getConnection();
            try(BatchIterator<Map<String, Object>> batchIterator = connectionUnawareResultSetMapper.findAllRowsAsMapInBatch(connection, "select * from A_TABLE where rollNo BETWEEN 1 AND 3 order by rollNo", 2)){
                assertEquals(batchIterator.next(), Arrays.asList(getMap(new A("User1", 1)), getMap(new A("User2", 2))));
                assertEquals(batchIterator.next(), Arrays.asList(getMap(new A("User3", 3))));
            }

        connection = DataSourceHolder.getInstance().getDataSource().getConnection();
            try(BatchIterator<Map<String, Object>> batchIterator = connectionUnawareResultSetMapper.findAllRowsAsMapInBatch(connection, "select * from A_TABLE where rollNo BETWEEN ? AND ? order by rollNo", 2
                    , new JDBCParam(INTEGER, 1), new JDBCParam(INTEGER, 3))){
                assertEquals(batchIterator.next(), Arrays.asList(getMap(new A("User1", 1)), getMap(new A("User2", 2))));
                assertEquals(batchIterator.next(), Arrays.asList(getMap(new A("User3", 3))));
            }
        connection = DataSourceHolder.getInstance().getDataSource().getConnection();
            try(BatchIterator<Map<String, Object>> batchIterator = connectionUnawareResultSetMapper.findAllRowsAsMapInBatch(connection, "select * from A_TABLE where rollNo BETWEEN ? AND ? order by rollNo", 2
                    , (preparedStatement -> {
                        preparedStatement.setInt(1, 1);
                        preparedStatement.setInt(2, 3);
                    }))){
                assertEquals(batchIterator.next(), Arrays.asList(getMap(new A("User1", 1)), getMap(new A("User2", 2))));
                assertEquals(batchIterator.next(), Arrays.asList(getMap(new A("User3", 3))));
            }

    }



    private List<A> getListOfA(int counter){
        List<A> list = new ArrayList<>(counter);
        for(int i = 1; i <= counter; i++){
            list.add(new A("User"+i, i));
        }
        return list;
    }

    private Map<String, Object> getMap(A a){
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("NAME", a.getName());
        map.put("ROLLNO", a.getRollNo());
        return map;
    }
}