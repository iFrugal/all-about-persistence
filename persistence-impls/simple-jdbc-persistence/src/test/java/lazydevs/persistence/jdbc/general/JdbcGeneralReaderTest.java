package lazydevs.persistence.jdbc.general;

import lazydevs.persistence.reader.Page;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdbcGeneralReaderTest {

    private static JdbcDataSource dataSource;
    private static JdbcGeneralReader reader;

    @BeforeAll
    static void setUp() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:readertest;DB_CLOSE_DELAY=-1");
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("create table person (id int primary key, name varchar(50))");
            for (int i = 1; i <= 5; i++) {
                s.execute("insert into person values (" + i + ", 'Person" + i + "')");
            }
            // Mixed-case literal: proves count() no longer lowercases the SQL.
            s.execute("insert into person values (6, 'MixedCase')");
        }
        reader = new JdbcGeneralReader(dataSource);
    }

    @Test
    void findPageReturnsRequestedSliceWithTotals() {
        JdbcOperation query = JdbcOperation.nativeSql("select id, name from person order by id");
        Page<Map<String, Object>> page = reader.findPage(
                Page.PageRequest.builder().pageNum(2).pageSize(2).build(), query, Map.of());

        assertEquals(6, page.getTotalNoOfRecords());
        assertEquals(3, page.getTotalNoOfPages());
        List<Map<String, Object>> data = page.getData();
        assertEquals(2, data.size());
        assertEquals(3, ((Number) data.get(0).get("ID")).intValue());
        assertEquals(4, ((Number) data.get(1).get("ID")).intValue());
    }

    @Test
    void lastPartialPageIsReturned() {
        JdbcOperation query = JdbcOperation.nativeSql("select id from person order by id");
        Page<Map<String, Object>> page = reader.findPage(
                Page.PageRequest.builder().pageNum(2).pageSize(5).build(), query, Map.of());
        assertEquals(1, page.getData().size());
        assertEquals(6, page.getTotalNoOfRecords());
    }

    @Test
    void countPreservesCaseSensitiveLiterals() {
        // The old implementation lowercased the whole SQL, turning the literal
        // into 'mixedcase' and returning 0 here.
        JdbcOperation query = JdbcOperation.nativeSql("select id from person where name = 'MixedCase'");
        assertEquals(1, reader.count(query, Map.of()));
    }
}
