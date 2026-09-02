package lazydevs.readeasy.devtools;

import lazydevs.readeasy.config.ReadEasyConfig;
import lazydevs.readeasy.registry.QueryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class DevModeQueryReloaderTest {

    private static final String INITIAL = """
            queries:
              byId:
                raw: '{"nativeSQL": "SELECT * FROM users WHERE id = :id"}'
            """;

    private static final String UPDATED = """
            queries:
              byId:
                raw: '{"nativeSQL": "SELECT id FROM users WHERE id = :id"}'
              all:
                raw: '{"nativeSQL": "SELECT * FROM users"}'
            """;

    private static final String INVALID = """
            queries:
              broken:
                readerId: default
            """;

    @TempDir
    Path tempDir;

    private Path queryFile;
    private ReadEasyConfig config;
    private QueryRegistry registry;
    private DevModeQueryReloader reloader;

    @BeforeEach
    void setUp() throws IOException {
        queryFile = tempDir.resolve("users.yaml");
        Files.writeString(queryFile, INITIAL);

        config = new ReadEasyConfig();
        config.getQueryFiles().put("users", List.of("file:" + queryFile));
        config.getDevtools().setEnabled(true);

        registry = new QueryRegistry(null);
        registry.register("users", "file:" + queryFile, INITIAL);

        reloader = new DevModeQueryReloader(config, new DefaultResourceLoader(), registry);
        reloader.recordInitialTimestamps();
    }

    private void touch() throws IOException {
        // Advance mtime well past the recorded value; second-granularity file systems
        // would otherwise hide a fast rewrite.
        Files.setLastModifiedTime(queryFile,
                java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() + 10_000));
    }

    @Test
    void unchangedFileDoesNothing() {
        reloader.checkForChanges();
        assertEquals(1, registry.size());
    }

    @Test
    void changedFileIsReloaded() throws IOException {
        Files.writeString(queryFile, UPDATED);
        touch();
        reloader.checkForChanges();
        assertEquals(2, registry.size());
        assertNotNull(registry.get("users.all"));
    }

    @Test
    void invalidChangeKeepsPreviousQueriesActive() throws IOException {
        Files.writeString(queryFile, INVALID);
        touch();
        reloader.checkForChanges();
        assertEquals(1, registry.size());
        assertNotNull(registry.get("users.byId"), "previous queries must survive an invalid save");
        assertNull(registry.get("users.broken"));
    }

    @Test
    void queryRemovedFromFileIsRemovedOnReload() throws IOException {
        Files.writeString(queryFile, UPDATED);
        touch();
        reloader.checkForChanges();

        Files.writeString(queryFile, INITIAL);
        touch();
        reloader.checkForChanges();

        assertEquals(1, registry.size());
        assertNull(registry.get("users.all"));
    }

    @Test
    void invalidChangeAppliesWhenValidateOnReloadDisabled() throws IOException {
        config.getDevtools().setValidateOnReload(false);
        Files.writeString(queryFile, INVALID);
        touch();
        reloader.checkForChanges();
        // Without validation the broken (but parseable) file is applied as-is.
        assertNotNull(registry.get("users.broken"));
        assertNull(registry.get("users.byId"));
    }

    @Test
    void missingFileIsIgnored() {
        config.setQueryFiles(new java.util.LinkedHashMap<>(Map.of(
                "users", List.of("file:" + queryFile),
                "ghost", List.of("file:" + tempDir.resolve("missing.yaml")))));
        reloader.checkForChanges();
        assertEquals(1, registry.size());
    }
}
