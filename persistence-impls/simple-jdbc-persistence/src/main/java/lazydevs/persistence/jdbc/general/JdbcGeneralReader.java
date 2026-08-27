 package lazydevs.persistence.jdbc.general;

import lazydevs.mapper.db.jdbc.ResultSetMapper;
import lazydevs.mapper.utils.BatchIterator;
import lazydevs.persistence.connection.ConnectionProvider;
import lazydevs.persistence.reader.GeneralReader;
import lazydevs.persistence.reader.Page;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * @author Abhijeet Rai
 */

@Slf4j
public class JdbcGeneralReader implements GeneralReader<JdbcOperation, Object> {

    private final ConnectionProvider<DataSource> connectionProvider;

    public JdbcGeneralReader(ConnectionProvider<DataSource> connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    public JdbcGeneralReader(DataSource dataSource){
        this.connectionProvider = () ->  dataSource;
    }

    @Override
    public Map<String, Object> findOne(JdbcOperation query, Map<String, Object> params) {
        List<Map<String, Object>> response = (List<Map<String, Object>>) getResultSetMapper().findAllRowsAsMap(query.getNativeSQL(), query.getParamsAsArr());

        if (response.size() > 0) {
            return response.get(0);
        }

        return null;
    }

    protected ResultSetMapper getResultSetMapper(){
        return new ResultSetMapper(connectionProvider.getConnection(), Object.class);
    }
    @Override
    public List<Map<String, Object>> findAll(JdbcOperation query, Map<String, Object> params) {
        return getResultSetMapper().findAllRowsAsMap(query.getNativeSQL(), query.getParamsAsArr());
    }

    @Override
    public Page<Map<String, Object>> findPage(Page.PageRequest pageRequest, JdbcOperation query, Map<String, Object> params) {
        long total = count(query, params);
        int pageSize = Math.max(1, pageRequest.getPageSize());
        long offset = (long) (Math.max(1, pageRequest.getPageNum()) - 1) * pageSize;
        List<Map<String, Object>> data = getResultSetMapper()
                .findAllRowsAsMap(paginate(query.getNativeSQL(), pageSize, offset), query.getParamsAsArr());
        return Page.<Map<String, Object>>builder(pageRequest)
                .totalNoOfRecords(total)
                .data(data)
                .build();
    }

    /**
     * Wraps the query for one page. LIMIT/OFFSET covers PostgreSQL, MySQL,
     * MariaDB, SQLite and H2; override for dialects that page differently
     * (Oracle/SQL Server: OFFSET ? ROWS FETCH NEXT ? ROWS ONLY).
     */
    protected String paginate(String nativeSQL, int pageSize, long offset) {
        return String.format("select * from (%s) paged_query limit %d offset %d", nativeSQL, pageSize, offset);
    }

    @Override
    public BatchIterator<Map<String, Object>> findAllInBatch(int batchSize, JdbcOperation query, Map<String, Object> params) {
        return getResultSetMapper().findAllRowsAsMapInBatch(query.getNativeSQL(), batchSize, query.getParamsAsArr());
    }
    

    @Override
    public List<Map<String, Object>> distinct(JdbcOperation query, Map<String, Object> params) {
        return findAll(query, params);
    }

    @Override
    public long count(JdbcOperation query, Map<String, Object> params) {
        // Wrap the SQL untouched: lowercasing it corrupts case-sensitive
        // string literals and quoted identifiers inside the query.
        String q = query.getNativeSQL();
        String q1 = String.format("select count(*) from (%s) countingQuery", q);
        log.debug("oldQuery = {}, newQuery = {}", q, q1);
        return getResultSetMapper().count(q1, query.getParamsAsArr());
    }

    @Override
    public Class<JdbcOperation> getQueryType() {
        return JdbcOperation.class;
    }
}
