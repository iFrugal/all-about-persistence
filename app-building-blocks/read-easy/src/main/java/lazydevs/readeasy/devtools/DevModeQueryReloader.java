package lazydevs.readeasy.devtools;

import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.config.ReadEasyConfig.DevtoolsConfig;
import lazydevs.readeasy.registry.QueryRegistry;
import lazydevs.readeasy.validation.QueryValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static lazydevs.mapper.utils.file.FileUtils.readInputStreamAsString;

/**
 * Development-mode hot reload of query YAML files.
 *
 * <p>Polls the configured query files on a dedicated daemon thread and, when a
 * file's lastModified changes, re-registers that file through the shared
 * {@link QueryRegistry} - the same path startup registration uses. Because the
 * registry replaces queries per source file, reloading one file never touches
 * queries contributed by other files of the same namespace, and readers always
 * see a complete snapshot.</p>
 *
 * <p>Uses its own {@link ScheduledExecutorService} instead of Spring scheduling,
 * so enabling it does not switch on {@code @EnableScheduling} (and thereby other
 * dormant {@code @Scheduled} methods) in the host application.</p>
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * readeasy:
 *   devtools:
 *     enabled: true            # default: false; only enable in development
 *     watchIntervalMs: 2000    # file poll interval
 *     validateOnReload: true   # validate before applying a changed file
 * }</pre>
 *
 * <p>Only file-system resources can be watched; classpath resources inside a JAR
 * are logged and skipped.</p>
 *
 * @author Abhijeet Rai
 */
@Slf4j
public class DevModeQueryReloader {

    private final ReadEasyConfig readEasyConfig;
    private final ResourceLoader resourceLoader;
    private final QueryRegistry queryRegistry;
    private final QueryValidator queryValidator = new QueryValidator();

    private final Map<String, Long> fileModifiedTimes = new ConcurrentHashMap<>();
    private ScheduledExecutorService executor;

    public DevModeQueryReloader(ReadEasyConfig readEasyConfig, ResourceLoader resourceLoader,
                                QueryRegistry queryRegistry) {
        this.readEasyConfig = readEasyConfig;
        this.resourceLoader = resourceLoader;
        this.queryRegistry = queryRegistry;
    }

    @PostConstruct
    public void init() {
        DevtoolsConfig devtools = readEasyConfig.getDevtools();
        log.info("READ-EASY DEV MODE ENABLED - hot reload active for query YAML files (interval = {} ms)",
                devtools.getWatchIntervalMs());
        recordInitialTimestamps();
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "read-easy-devtools");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::checkForChanges,
                devtools.getWatchIntervalMs(), devtools.getWatchIntervalMs(), TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void shutdown() {
        if (null != executor) {
            executor.shutdownNow();
        }
    }

    void recordInitialTimestamps() {
        forEachQueryFile((namespace, filePath) -> {
            try {
                Resource resource = resourceLoader.getResource(filePath);
                if (resource.exists() && resource.isFile()) {
                    fileModifiedTimes.put(filePath, resource.getFile().lastModified());
                    log.info("Watching for changes: {} (namespace = {})", filePath, namespace);
                } else {
                    log.warn("Cannot watch non-file resource: {}. Hot reload requires file: resources.", filePath);
                }
            } catch (IOException e) {
                log.warn("Cannot monitor file {}: {}", filePath, e.getMessage());
            }
        });
    }

    void checkForChanges() {
        forEachQueryFile((namespace, filePath) -> {
            try {
                checkAndReloadFile(namespace, filePath);
            } catch (Exception e) {
                log.error("Error checking file {}: {}", filePath, e.getMessage());
            }
        });
    }

    private void checkAndReloadFile(String namespace, String filePath) throws IOException {
        Resource resource = resourceLoader.getResource(filePath);
        if (!resource.exists() || !resource.isFile()) {
            return;
        }
        long currentModified = resource.getFile().lastModified();
        Long lastModified = fileModifiedTimes.get(filePath);
        if (null != lastModified && currentModified <= lastModified) {
            return;
        }
        log.info("File change detected: {}", filePath);
        try {
            reloadQueryFile(namespace, filePath, resource);
            fileModifiedTimes.put(filePath, currentModified);
            log.info("Successfully reloaded queries from: {}", filePath);
        } catch (Exception e) {
            // Record the timestamp anyway so a broken save is reported once, not every tick.
            fileModifiedTimes.put(filePath, currentModified);
            log.error("Failed to reload {}. Previous queries remain active; fix the file and save again. Cause: {}",
                    filePath, e.getMessage());
        }
    }

    private void reloadQueryFile(String namespace, String filePath, Resource resource) throws IOException {
        String content = readInputStreamAsString(resource.getInputStream());
        if (readEasyConfig.getDevtools().isValidateOnReload()) {
            // Reader ids are not known here; the reader check happens at request time.
            queryValidator.validateAndThrow(namespace, filePath, content, Collections.emptySet(),
                    readEasyConfig.getValidation().isValidateTemplates(), true);
        }
        int count = queryRegistry.register(namespace, filePath, content);
        log.info("Reloaded {} queries from namespace '{}'", count, namespace);
    }

    private void forEachQueryFile(QueryFileConsumer consumer) {
        readEasyConfig.getQueryFiles().forEach((namespace, filePaths) ->
                filePaths.forEach(filePath -> consumer.accept(namespace, filePath)));
    }

    @FunctionalInterface
    private interface QueryFileConsumer {
        void accept(String namespace, String filePath);
    }
}
