package lazydevs.mapper.utils.file;

import java.io.*;
import java.net.URL;
import java.util.stream.Collectors;

/**
 * Enhanced FileUtils with classpath support
 * Created by Abhijeet Rai on 12/19/2018.
 */
public class FileUtils {

    /**
     * Read file as string with support for both file system and classpath resources
     * @param fileName - can be:
     *                 - Regular file path: "/path/to/file.txt" or "file.txt"
     *                 - Classpath resource: "classpath:templates/file.txt" or "classpath:/templates/file.txt"
     * @return file content as string
     */
    public static String readFileAsString(String fileName) {
        try {
            if (fileName.startsWith("classpath:")) {
                return readClasspathFileAsString(fileName);
            } else {
                return readFileSystemFileAsString(fileName);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read file: " + fileName, t);
        }
    }

    /**
     * Read file from classpath
     */
    private static String readClasspathFileAsString(String fileName) throws IOException {
        // Remove "classpath:" prefix
        String resourcePath = fileName.substring("classpath:".length());

        // Remove leading slash if present (both /templates/file.txt and templates/file.txt should work)
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        // Try to get resource from classpath
        InputStream inputStream = getClasspathResource(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Classpath resource not found: " + resourcePath);
        }

        return readInputStreamAsString(inputStream);
    }

    /**
     * Get classpath resource as InputStream with multiple fallback strategies
     */
    private static InputStream getClasspathResource(String resourcePath) {
        // Strategy 1: Use current thread's context class loader
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath);

        if (inputStream != null) {
            return inputStream;
        }

        // Strategy 2: Use FileUtils class loader
        inputStream = FileUtils.class.getClassLoader().getResourceAsStream(resourcePath);

        if (inputStream != null) {
            return inputStream;
        }

        // Strategy 3: Use system class loader
        inputStream = ClassLoader.getSystemResourceAsStream(resourcePath);

        return inputStream; // May be null if resource not found
    }

    /**
     * Read file from file system
     */
    private static String readFileSystemFileAsString(String fileName) throws IOException {
        try (FileReader fr = new FileReader(fileName);
             BufferedReader br = new BufferedReader(fr)) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Read InputStream as string
     */
    public static String readInputStreamAsString(InputStream inputStream) {
        try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
             BufferedReader br = new BufferedReader(inputStreamReader)) {
            return br.lines().collect(Collectors.joining("\n"));
        } catch (Throwable t) {
            throw new RuntimeException("Failed to read InputStream", t);
        }
    }

    /**
     * Write content to file
     */
    public static void writeToFile(String fileName, String content) {
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(content);
            fw.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to file: " + fileName, e);
        }
    }

    /**
     * Delete file
     */
    public static void delete(String filePath) {
        boolean isDeleted = new File(filePath).delete();
        if (!isDeleted) {
            throw new IllegalArgumentException("Unable to delete the file: " + filePath);
        }
    }

    /**
     * Validate file exists (file system only)
     */
    public static void validateFileExists(String filepath, boolean isDirAccepted) {
        File file = new File(filepath);
        if (!file.exists()) {
            throw new IllegalArgumentException("File not found: " + filepath);
        }
        if (!isDirAccepted && file.isDirectory()) {
            throw new IllegalArgumentException("File is a Directory: " + filepath);
        }
    }

    /**
     * Check if a classpath resource exists
     */
    public static boolean classpathResourceExists(String resourcePath) {
        if (resourcePath.startsWith("classpath:")) {
            resourcePath = resourcePath.substring("classpath:".length());
        }
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        URL resource = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        return resource != null;
    }

    /**
     * Get resource URL (useful for debugging)
     */
    public static URL getResourceUrl(String resourcePath) {
        if (resourcePath.startsWith("classpath:")) {
            resourcePath = resourcePath.substring("classpath:".length());
        }
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }

        return Thread.currentThread().getContextClassLoader().getResource(resourcePath);
    }

    /**
     * List available resources in a classpath directory (for debugging)
     */
    public static void debugClasspathResources() {
        try {
            // This is helpful for debugging what resources are available
            URL resource = Thread.currentThread().getContextClassLoader().getResource("");
            if (resource != null) {
                System.out.println("Classpath root: " + resource);
            }

            // Try to list some common directories
            String[] commonDirs = {"", "templates", "static", "config"};
            for (String dir : commonDirs) {
                URL dirResource = Thread.currentThread().getContextClassLoader().getResource(dir);
                if (dirResource != null) {
                    System.out.println("Found directory: " + dir + " -> " + dirResource);
                }
            }
        } catch (Exception e) {
            System.err.println("Error debugging classpath: " + e.getMessage());
        }
    }
}