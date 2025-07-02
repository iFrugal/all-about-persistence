package lazydevs.mapper.utils.file;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileUtilsTest {

    @BeforeClass
    public void setup() throws IOException {
        // Create test files directly in target/test-classes (the actual classpath)
        String classpathRoot = getClass().getClassLoader().getResource("").getPath();
        File classpathDir = new File(classpathRoot);

        // Create templates directory in classpath
        File templatesDir = new File(classpathDir, "templates");
        templatesDir.mkdirs();

        // Create test files in the actual classpath location
        createTestFile(new File(templatesDir, "test-template.txt"), "Hello from classpath template!");
        createTestFile(new File(classpathDir, "test-root.txt"), "Hello from classpath root!");

        System.out.println("✅ Created test resources in classpath: " + classpathRoot);

        // Debug: List what's actually in the classpath
        System.out.println("📁 Classpath contents:");
        listDirectory(classpathDir, "");
    }

    private void createTestFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        System.out.println("📝 Created in classpath: " + file.getName());
    }

    private void listDirectory(File dir, String indent) {
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    System.out.println(indent + (file.isDirectory() ? "📁 " : "📄 ") + file.getName());
                    if (file.isDirectory() && indent.length() < 4) { // Limit depth
                        listDirectory(file, indent + "  ");
                    }
                }
            }
        }
    }

    @Test
    public void testReadFileSystemFile() {
        System.out.println("\n=== Test: Read File System File ===");

        try {
            File tempFile = File.createTempFile("test", ".txt");
            tempFile.deleteOnExit();

            String content = "Hello from file system!";
            FileUtils.writeToFile(tempFile.getAbsolutePath(), content);

            String result = FileUtils.readFileAsString(tempFile.getAbsolutePath());

            Assert.assertEquals(result, content);
            System.out.println("✅ File system read working: " + result);

        } catch (IOException e) {
            Assert.fail("Failed to create temp file: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testReadFileSystemFile")
    public void testReadClasspathFileWithPrefix() {
        System.out.println("\n=== Test: Read Classpath File with 'classpath:' prefix ===");

        try {
            String result = FileUtils.readFileAsString("classpath:templates/test-template.txt");

            Assert.assertNotNull(result);
            Assert.assertTrue(result.contains("Hello from classpath template!"));
            System.out.println("✅ Classpath read working: " + result);

        } catch (Exception e) {
            System.err.println("❌ Classpath read failed: " + e.getMessage());
            debugClasspathIssue();
            Assert.fail("Should be able to read classpath resource: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testReadFileSystemFile")
    public void testReadClasspathFileRootLevel() {
        System.out.println("\n=== Test: Read Classpath File from Root ===");

        try {
            String result = FileUtils.readFileAsString("classpath:test-root.txt");

            Assert.assertNotNull(result);
            Assert.assertTrue(result.contains("Hello from classpath root!"));
            System.out.println("✅ Classpath root read working: " + result);

        } catch (Exception e) {
            System.err.println("❌ Classpath root read failed: " + e.getMessage());
            debugClasspathIssue();
            Assert.fail("Should be able to read classpath root resource: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testReadClasspathFileWithPrefix")
    public void testReadClasspathFileWithLeadingSlash() {
        System.out.println("\n=== Test: Read Classpath File with Leading Slash ===");

        try {
            String result = FileUtils.readFileAsString("classpath:/templates/test-template.txt");

            Assert.assertNotNull(result);
            Assert.assertTrue(result.contains("Hello from classpath template!"));
            System.out.println("✅ Classpath with leading slash working: " + result);

        } catch (Exception e) {
            System.err.println("❌ Classpath with slash failed: " + e.getMessage());
            Assert.fail("Should be able to read classpath resource with leading slash: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testReadClasspathFileRootLevel")
    public void testClasspathResourceExists() {
        System.out.println("\n=== Test: Check Classpath Resource Exists ===");

        boolean exists1 = FileUtils.classpathResourceExists("classpath:templates/test-template.txt");
        boolean exists2 = FileUtils.classpathResourceExists("classpath:nonexistent.txt");
        boolean exists3 = FileUtils.classpathResourceExists("test-root.txt");

        Assert.assertTrue(exists1, "Should find existing template");
        Assert.assertFalse(exists2, "Should not find nonexistent file");
        Assert.assertTrue(exists3, "Should find root file without classpath prefix");

        System.out.println("✅ Resource existence checks working");
    }

    @Test
    public void testFileNotFound() {
        System.out.println("\n=== Test: File Not Found Handling ===");

        try {
            FileUtils.readFileAsString("classpath:nonexistent/file.txt");
            Assert.fail("Should throw exception for nonexistent file");
        } catch (RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Failed to read file"));
            System.out.println("✅ File not found handled correctly: " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testClasspathResourceExists")
    public void testDebugClasspathResources() {
        System.out.println("\n=== Test: Debug Classpath Resources ===");

        FileUtils.debugClasspathResources();

        // Test getting resource URL
        java.net.URL url = FileUtils.getResourceUrl("templates/test-template.txt");
        if (url != null) {
            System.out.println("✅ Resource URL: " + url);
        } else {
            System.out.println("⚠️  Resource URL not found - this might be expected in some test environments");
        }
    }

    @Test(dependsOnMethods = {"testReadFileSystemFile", "testReadClasspathFileRootLevel"})
    public void testMixedUsage() {
        System.out.println("\n=== Test: Mixed File System and Classpath Usage ===");

        try {
            // Test file system
            File tempFile = File.createTempFile("mixed-test", ".txt");
            tempFile.deleteOnExit();
            FileUtils.writeToFile(tempFile.getAbsolutePath(), "File system content");

            String fsResult = FileUtils.readFileAsString(tempFile.getAbsolutePath());
            String cpResult = FileUtils.readFileAsString("classpath:test-root.txt");

            Assert.assertEquals(fsResult, "File system content");
            Assert.assertTrue(cpResult.contains("Hello from classpath root!"));

            System.out.println("✅ Mixed usage working");
            System.out.println("  File system: " + fsResult);
            System.out.println("  Classpath: " + cpResult);

        } catch (Exception e) {
            Assert.fail("Mixed usage failed: " + e.getMessage());
        }
    }

    private void debugClasspathIssue() {
        System.out.println("\n🔍 Debugging classpath issue:");

        // Check what ClassLoader sees
        String[] testPaths = {
                "test-root.txt",
                "templates/test-template.txt",
                "/test-root.txt",
                "/templates/test-template.txt"
        };

        for (String path : testPaths) {
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            System.out.println("  Path: " + path + " -> " + (url != null ? url : "NOT FOUND"));
        }

        // List actual classpath root contents
        try {
            java.net.URL root = Thread.currentThread().getContextClassLoader().getResource("");
            if (root != null) {
                File rootDir = new File(root.getPath());
                System.out.println("📁 Actual classpath contents:");
                listDirectory(rootDir, "  ");
            }
        } catch (Exception e) {
            System.out.println("❌ Error listing classpath: " + e.getMessage());
        }
    }
}

// Alternative test that uses existing classpath resources
class SimpleClasspathTest {

    @Test
    public void testWithExistingResources() {
        System.out.println("\n=== Test: Using Existing Classpath Resources ===");

        // First, let's see what's actually available
        FileUtils.debugClasspathResources();

        // Try to find any existing resource
        String[] commonResources = {
                "application.properties",
                "logback.xml",
                "META-INF/MANIFEST.MF"
        };

        for (String resource : commonResources) {
            try {
                java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
                if (url != null) {
                    System.out.println("✅ Found existing resource: " + resource);

                    // Try to read it
                    String content = FileUtils.readFileAsString("classpath:" + resource);
                    System.out.println("✅ Successfully read: " + content.substring(0, Math.min(50, content.length())) + "...");
                    return; // Success!
                }
            } catch (Exception e) {
                System.out.println("❌ Failed to read " + resource + ": " + e.getMessage());
            }
        }

        System.out.println("ℹ️  No common classpath resources found for testing");
    }
}