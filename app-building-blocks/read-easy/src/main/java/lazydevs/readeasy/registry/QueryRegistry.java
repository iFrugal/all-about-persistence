package lazydevs.readeasy.registry;

import lazydevs.mapper.utils.SerDe;
import lazydevs.readeasy.config.ReadEasyConfig.Query;
import lazydevs.readeasy.config.ReadEasyConfig.QueryWithDynaBeans;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The single owner of registered queries. Both startup registration
 * ({@code ConfiguredReadController#init}) and dev-mode hot reload
 * ({@code DevModeQueryReloader}) go through {@link #register}, so registration
 * semantics cannot drift between the two paths.
 *
 * <p>Thread safety: writers replace the whole map under a lock (copy-on-write)
 * and publish it through a volatile reference, so request threads always read a
 * complete, immutable snapshot - never a half-updated map.</p>
 *
 * <p>Each registration is keyed by a source (normally the file path). Re-registering
 * a source replaces only the queries that source contributed, so a namespace fed by
 * several files keeps the other files' queries intact when one file is reloaded.</p>
 *
 * @author Abhijeet Rai
 */
@Slf4j
public class QueryRegistry {

    private final DynaBeansAutoConfiguration dynaBeansAutoConfiguration;

    private volatile Map<String, Query> queries = Collections.emptyMap();

    /** source key (file path or upload key) -> query ids contributed by that source. Guarded by writeLock. */
    private final Map<String, Set<String>> queryIdsBySource = new HashMap<>();
    private final Object writeLock = new Object();

    public QueryRegistry(DynaBeansAutoConfiguration dynaBeansAutoConfiguration) {
        this.dynaBeansAutoConfiguration = dynaBeansAutoConfiguration;
    }

    /**
     * Parses the YAML content and registers its queries under
     * {@code namespace.queryName}, replacing whatever this source registered before.
     *
     * @param namespace the namespace the file belongs to
     * @param sourceKey identity of the content's origin (file path or upload key)
     * @param content   the query YAML content
     * @return number of queries registered from this content
     */
    public int register(String namespace, String sourceKey, String content) {
        QueryWithDynaBeans queryWithDynaBeans = SerDe.YAML.deserialize(content, QueryWithDynaBeans.class);
        if (null != dynaBeansAutoConfiguration) {
            dynaBeansAutoConfiguration.initializeAndInject(namespace, queryWithDynaBeans.getDynaBeans());
        }
        Map<String, Query> newQueries = queryWithDynaBeans.getQueries();
        if (null == newQueries) {
            newQueries = Collections.emptyMap();
        }
        synchronized (writeLock) {
            Map<String, Query> next = new HashMap<>(queries);
            Set<String> previousIds = queryIdsBySource.getOrDefault(sourceKey, Collections.emptySet());
            previousIds.forEach(next::remove);
            Set<String> newIds = new LinkedHashSet<>();
            newQueries.forEach((queryId, query) -> {
                String fullQueryId = namespace + "." + queryId;
                next.put(fullQueryId, query);
                newIds.add(fullQueryId);
            });
            queryIdsBySource.put(sourceKey, newIds);
            queries = Collections.unmodifiableMap(next);
            log.debug("Registered {} queries from source = {} (namespace = {})", newIds.size(), sourceKey, namespace);
            return newIds.size();
        }
    }

    public Query get(String queryId) {
        return queries.get(queryId);
    }

    public int size() {
        return queries.size();
    }
}
