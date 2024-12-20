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
        throw new UnsupportedOperationException("Not yet implemented.");
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
        String q = query.getNativeSQL().toLowerCase();
        String q1 = String.format("select count(*) from (%s) countingQuery", q);
        log.debug("oldQuery = {}, newQuery = {}", q, q1);
        return getResultSetMapper().count(q1, query.getParamsAsArr());
    }

    @Override
    public Class<JdbcOperation> getQueryType() {
        return JdbcOperation.class;
    }
}
