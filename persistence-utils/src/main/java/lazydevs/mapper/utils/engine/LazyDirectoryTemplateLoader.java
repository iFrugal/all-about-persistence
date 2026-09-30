package lazydevs.mapper.utils.engine;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;

import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * File system template loader whose directory may not exist yet when the loader is
 * created. {@link FileTemplateLoader} requires an existing directory at construction
 * time, so {@link TemplateEngine} (a singleton built once per JVM) could only pick up
 * a working-directory {@code templates} folder that already existed when the singleton
 * was first touched. This loader defers creating the {@link FileTemplateLoader} until
 * the first lookup that finds the directory, and reports "not found" while it is absent.
 *
 * <p>FreeMarker's template cache still applies on top of this loader, including for
 * lookups that missed, for the configured template update delay.
 */
final class LazyDirectoryTemplateLoader implements TemplateLoader {

    private final File directory;
    private volatile FileTemplateLoader delegate;

    LazyDirectoryTemplateLoader(File directory) {
        this.directory = directory;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        if (!directory.isDirectory()) {
            return null;
        }
        return delegate().findTemplateSource(name);
    }

    @Override
    public long getLastModified(Object templateSource) {
        FileTemplateLoader loader = delegate;
        return loader == null ? -1 : loader.getLastModified(templateSource);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        FileTemplateLoader loader = delegate;
        if (loader == null) {
            throw new IOException("No template source was found by " + this);
        }
        return loader.getReader(templateSource, encoding);
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        FileTemplateLoader loader = delegate;
        if (loader != null) {
            loader.closeTemplateSource(templateSource);
        }
    }

    private FileTemplateLoader delegate() throws IOException {
        FileTemplateLoader loader = delegate;
        if (loader == null) {
            synchronized (this) {
                loader = delegate;
                if (loader == null) {
                    loader = new FileTemplateLoader(directory);
                    delegate = loader;
                }
            }
        }
        return loader;
    }

    @Override
    public String toString() {
        return "LazyDirectoryTemplateLoader(directory=\"" + directory.getAbsolutePath() + "\")";
    }
}
