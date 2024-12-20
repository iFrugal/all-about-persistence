package lazydevs.mapper.db.jdbc.util;

import lazydevs.mapper.utils.BatchIterator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public abstract class ResultSetBatchIterator<T> extends BatchIterator<T> {
    private boolean hasNextBatch = true;
    private final ResultSet resultSet;
    private final Statement statement;
    private final Connection connection;
    private final Set<String> columnNamesAvailableinRs = new LinkedHashSet<>();

    protected ResultSetBatchIterator(final ResultSet resultSet, final int batchSize, final Statement statement, final Connection connection){
        super(batchSize);
        this.resultSet = resultSet;
        this.statement = statement;
        this.connection = connection;
    }

    protected ResultSetBatchIterator(final ResultSet resultSet, final int batchSize, final Statement statement){
        this(resultSet, batchSize, statement, null);
    }

    @Override
    public void close() {
       SimpleUtils.closeQuietly(resultSet, statement, connection);
    }

    @Override
    public boolean hasNext() {
        return this.hasNextBatch;
    }

    @Override
    public List<T> next() {
        if(hasNext()) {
            int recordCount = 0;
            List<T> list = new ArrayList<>();
            try {
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    columnNamesAvailableinRs.add(resultSet.getMetaData().getColumnLabel(i).toLowerCase());
                }
                while (recordCount < batchSize && resultSet.next()) {
                    list.add(map(resultSet, columnNamesAvailableinRs));
                    recordCount++;
                }
            } catch (Exception e) {
                throw new IllegalStateException("Unhandled Exception occured while iterating resultset", e);
            }
            hasNextBatch = recordCount == batchSize;
            return list;
        }else{
            throw new NoSuchElementException("No more records");
        }
    }

    public abstract T map(ResultSet resultSet, Set<String> columnNamesAvailableinRs);
}


