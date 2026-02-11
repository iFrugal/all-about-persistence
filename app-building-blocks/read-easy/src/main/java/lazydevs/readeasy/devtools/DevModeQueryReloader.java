package lazydevs.readeasy.devtools;

import lazydevs.mapper.utils.SerDe;
import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.config.ReadEasyConfig.Query;
import lazydevs.readeasy.config.ReadEasyConfig.QueryWithDynaBeans;
import lazydevs.readeasy.validation.QueryValidator;
import lazydevs.springhelpers.dynabeans.DynaBeansAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static lazydevs.mapper.utils.file.FileUtils.readInputStreamAsString;

/**
 * Development mode component that enables hot-reloading of query YAML files.
 *
 * <p>When enabled, this component watches for changes to query YAML files and
 * automatically reloads them without requiring an application restart.</p>
 *
 * <h2>Configuration:</h2>
 * <pre>{@code
 * readeasy:
 *   devtools:
 *     enabled: true              # Enable hot-reload (default: false)
 *     watchIntervalMs: 2000      # File check interval in milliseconds (default: 2000)
 *     validateOnReload: true     # Validate queries before reloading (default: true)
 * }</pre>
 *
 * <h2>Important Notes:</h2>
 * <ul>
 *   <li>Only enable in development environments</li>
 *   <li>File watching works with file: resources, not classpath: in JARs</li>
 *   <li>Invalid files will be logged but won't break running queries</li>
 * </ul>
 *
 * @author Abhijeet Rai
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "readeasy.devtools.enabled", havingValue = "true")
public class DevModeQueryReloader {

    @Autowired
    private ReadEasyConfig readEasyConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired(required = false)
    private DynaBeansAutoConfiguration dynaBeansAutoConfiguration;

    @Autowired(required = false)
    private QueryValidator queryValidator;

    @Value("${readeasy.devtools.validateOnReload:true}")
    private boolean validateOnReload;

    // Map of file path -> last modified time
    private final Map<String, Long> fileModifiedTimes = new ConcurrentHashMap<>();

    // Map of namespace.queryId -> Query
    private Map<String, Query> queriesRef;

    // Callback to update queries in ConfiguredReadController
    private Consumer<Map<String, Query>> queryUpdateCallback;

    // Available reader IDs for validation
    private Set<String> availableReaderIds;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        log.info("═".repeat(60));
        log.info("READ-EASY DEV MODE ENABLED");
        log.info("Hot-reload is active for query YAML files");
        log.info("═".repeat(60));

        // Initialize file modification times
        readEasyConfig.getQueryFiles().forEach((namespace, filePaths) -> {
            filePaths.forEach(filePath -> {
                try {
                    Resource resource = resourceLoader.getResource(filePath);
                    if (resource.exists() && resource.isFile()) {
                        long lastModified = resource.getFile().lastModified();
                        fileModifiedTimes.put(filePath, lastModified);
                        log.info("Watching for changes: {} (namespace: {})", filePath, namespace);
                    } else {
                        log.warn("Cannot watch classpath resource in JAR: {}. Hot-reload requires file: resources.", filePath);
                    }
                } catch (IOException e) {
                    log.warn("Cannot monitor file {}: {}", filePath, e.getMessage());
                }
            });
        });

        initialized.set(true);
    }

    /**
     * Sets the reference to the queries map and the callback for updates.
     * Called by ConfiguredReadController during initialization.
     *
     * @param queries The queries map to update on reload
     * @param updateCallback Callback to notify of updates
     * @param readerIds Available reader IDs for validation
     */
    public void setQueryReference(Map<String, Query> queries,
                                   Consumer<Map<String, Query>> updateCallback,
                                   Set<String> readerIds) {
        this.queriesRef = queries;
        this.queryUpdateCallback = updateCallback;
        this.availableReaderIds = readerIds;
    }

    /**
     * Periodically checks for file modifications and reloads changed files.
     * Runs every 2 seconds by default.
     */
    @Scheduled(fixedDelayString = "${readeasy.devtools.watchIntervalMs:2000}")
    public void checkForChanges() {
        if (!initialized.get() || queriesRef == null) {
            return;
        }

        readEasyConfig.getQueryFiles().forEach((namespace, filePaths) -> {
            filePaths.forEach(filePath -> {
                try {
                    checkAndReloadFile(namespace, filePath);
                } catch (Exception e) {
                    log.error("Error checking file {}: {}", filePath, e.getMessage());
                }
            });
        });
    }

    private void checkAndReloadFile(String namespace, String filePath) throws IOException {
        Resource resource = resourceLoader.getResource(filePath);

        if (!resource.exists() || !resource.isFile()) {
            return;
        }

        long currentModified = resource.getFile().lastModified();
        Long lastModified = fileModifiedTimes.get(filePath);

        if (lastModified != null && currentModified > lastModified) {
            log.info("─".repeat(50));
            log.info("File changed detected: {}", filePath);

            try {
                reloadQueryFile(namespace, filePath, resource);
                fileModifiedTimes.put(filePath, currentModified);
                log.info("Successfully reloaded queries from: {}", filePath);
            } catch (Exception e) {
                log.error("Failed to reload {}: {}", filePath, e.getMessage());
                log.error("Previous queries remain active. Fix the file and save again.");
            }

            log.info("─".repeat(50));
        }
    }

    private void reloadQueryFile(String namespace, String filePath, Resource resource) throws IOException {
        String content = readInputStreamAsString(resource.getInputStream());

        // Validate first if enabled
        if (validateOnReload && queryValidator != null) {
            queryValidator.validateAndThrow(namespace, filePath, content, availableReaderIds, true);
        }

        // Parse the YAML
        QueryWithDynaBeans queryWithDynaBeans = SerDe.YAML.deserialize(content, QueryWithDynaBeans.class);

        // Reload dynamic beans if present
        if (dynaBeansAutoConfiguration != null && queryWithDynaBeans.getDynaBeans() != null) {
            dynaBeansAutoConfiguration.initializeAndInject(namespace, queryWithDynaBeans.getDynaBeans());
        }

        // Remove old queries from this file and add new ones
        synchronized (queriesRef) {
            // Remove queries that start with this namespace
            // (we'll re-add the ones from the reloaded file)
            String prefix = namespace + ".";
            queriesRef.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));

            // Add reloaded queries
            queryWithDynaBeans.getQueries().forEach((queryId, query) -> {
                String fullQueryId = namespace + "." + queryId;
                queriesRef.put(fullQueryId, query);
                log.debug("Reloaded query: {}", fullQueryId);
            });
        }

        // Notify callback if set
        if (queryUpdateCallback != null) {
            queryUpdateCallback.accept(queriesRef);
        }

        log.info("Reloaded {} queries from namespace '{}'",
                queryWithDynaBeans.getQueries().size(), namespace);
    }

    /**
     * Forces an immediate reload of all query files.
     * Useful for admin endpoints or testing.
     */
    public void forceReloadAll() {
        log.info("Force reloading all query files...");

        readEasyConfig.getQueryFiles().forEach((namespace, filePaths) -> {
            filePaths.forEach(filePath -> {
                try {
                    Resource resource = resourceLoader.getResource(filePath);
                    if (resource.exists()) {
                        reloadQueryFile(namespace, filePath, resource);
                        fileModifiedTimes.put(filePath, resource.getFile().lastModified());
                    }
                } catch (Exception e) {
                    log.error("Failed to reload {}: {}", filePath, e.getMessage());
                }
            });
        });

        log.info("Force reload complete.");
    }

    /**
     * Returns the current status of watched files.
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", true);
        status.put("validateOnReload", validateOnReload);

        List<Map<String, Object>> files = new ArrayList<>();
        fileModifiedTimes.forEach((path, lastModified) -> {
            Map<String, Object> fileInfo = new LinkedHashMap<>();
            fileInfo.put("path", path);
            fileInfo.put("lastModified", new Date(lastModified));
            files.add(fileInfo);
        });
        status.put("watchedFiles", files);

        return status;
    }
}
