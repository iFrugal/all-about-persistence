package lazydevs.mapper.utils.engine;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

/**
 * Regression test: the working-directory "templates" folder must be picked up even when
 * it is created after TemplateEngine was initialised. Uses template names that no other
 * test uses, and restores any pre-existing "templates" folder afterwards.
 */
public class LazyTemplatesDirectoryTest {

    private static final String TEMPLATE_NAME = "lazy-check.ftl";
    private static final String MISSING_TEMPLATE_NAME = "lazy-check-missing.ftl";

    private final File templatesDir = new File("templates");
    private File backupDir;
    private boolean createdTemplatesDir;
    private boolean createdTemplateFile;
    private TemplateEngine singleton;
    private TemplateEngine engineCreatedWithoutDirectory;
    private Path tempDir;

    @BeforeClass
    public void moveTemplatesDirectoryAsideAndCreateEngines() throws Exception {
        if (templatesDir.exists()) {
            File candidate = new File("templates.lazy-check-backup-" + System.nanoTime());
            assertTrue(templatesDir.renameTo(candidate), "could not move existing templates folder aside");
            // Recorded only after the rename succeeded, so cleanup never touches a folder this test did not move.
            backupDir = candidate;
        }
        assertFalse(templatesDir.exists());

        singleton = TemplateEngine.getInstance();
        // The singleton may already have been created by an earlier test class in this JVM,
        // so also build a fresh engine now, while the folder is known to be absent.
        Constructor<TemplateEngine> constructor = TemplateEngine.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        engineCreatedWithoutDirectory = constructor.newInstance();

        tempDir = Files.createTempDirectory("lazy-template-loader");
    }

    @AfterClass(alwaysRun = true)
    public void cleanUp() throws IOException {
        // Delete only what this test created; if setup failed part-way the folder may belong to someone else.
        if (createdTemplateFile) {
            Files.deleteIfExists(new File(templatesDir, TEMPLATE_NAME).toPath());
        }
        String[] remaining = templatesDir.list();
        if (createdTemplatesDir && remaining != null && remaining.length == 0) {
            Files.delete(templatesDir.toPath());
        }
        if (backupDir != null && !templatesDir.exists()) {
            assertTrue(backupDir.renameTo(templatesDir), "could not restore templates folder from " + backupDir);
        }
        if (tempDir != null) {
            try (Stream<Path> paths = Files.walk(tempDir)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            }
        }
    }

    @Test(priority = 0)
    public void includeOfMissingTemplateStillFailsWhileDirectoryIsAbsent() {
        assertFalse(templatesDir.exists());
        String template = "<#include \"" + MISSING_TEMPLATE_NAME + "\">";

        for (TemplateEngine engine : new TemplateEngine[]{singleton, engineCreatedWithoutDirectory}) {
            RuntimeException e = expectThrows(RuntimeException.class, () -> engine.generate(template, Map.of()));
            assertTrue(e.getMessage().contains(MISSING_TEMPLATE_NAME), e.getMessage());
        }
    }

    @Test(priority = 1)
    public void includePicksUpDirectoryCreatedAfterEngineInitialisation() throws IOException {
        createdTemplatesDir = templatesDir.mkdirs();
        assertTrue(templatesDir.isDirectory());
        Files.writeString(new File(templatesDir, TEMPLATE_NAME).toPath(), "Hello ${name} from lazy-check");
        createdTemplateFile = true;
        String template = "[<#include \"" + TEMPLATE_NAME + "\">]";

        for (TemplateEngine engine : new TemplateEngine[]{singleton, engineCreatedWithoutDirectory}) {
            assertEquals(engine.generate(template, Map.of("name", "World")), "[Hello World from lazy-check]");
        }
    }

    @Test
    public void loaderReportsNotFoundUntilDirectoryExistsThenDelegates() throws IOException {
        File dir = tempDir.resolve("created-later").toFile();
        LazyDirectoryTemplateLoader loader = new LazyDirectoryTemplateLoader(dir);

        assertNull(loader.findTemplateSource("t.ftl"));

        assertTrue(dir.mkdirs());
        assertNull(loader.findTemplateSource("t.ftl"));
        Files.writeString(dir.toPath().resolve("t.ftl"), "content", StandardCharsets.UTF_8);

        Object source = loader.findTemplateSource("t.ftl");
        assertNotNull(source);
        assertTrue(loader.getLastModified(source) > 0);
        StringWriter out = new StringWriter();
        try (Reader reader = loader.getReader(source, "UTF-8")) {
            reader.transferTo(out);
        }
        loader.closeTemplateSource(source);
        assertEquals(out.toString(), "content");

        Files.delete(dir.toPath().resolve("t.ftl"));
        Files.delete(dir.toPath());
        assertNull(loader.findTemplateSource("t.ftl"));
    }
}
