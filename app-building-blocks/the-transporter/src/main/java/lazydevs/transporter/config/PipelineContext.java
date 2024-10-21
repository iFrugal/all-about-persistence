package lazydevs.transporter.config;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Abhijeet Rai
 */
@Slf4j
public class PipelineContext extends ConcurrentHashMap<String, Object> {
    protected static Class<? extends PipelineContext> contextClass = PipelineContext.class;
    protected static final ThreadLocal<? extends PipelineContext> threadLocal = new ThreadLocal<PipelineContext>() {
        protected PipelineContext initialValue() {
            try {
                return PipelineContext.contextClass.newInstance();
            } catch (Throwable var2) {
                throw new RuntimeException(var2);
            }
        }
    };


    public static void setContextClass(Class<? extends PipelineContext> clazz) {
        contextClass = clazz;
    }


    public static PipelineContext getCurrentContext() {
        return threadLocal.get();
    }

    public boolean getBoolean(String key) {
        return this.getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean defaultResponse) {
        Boolean b = (Boolean)this.get(key);
        return b != null ? b : defaultResponse;
    }

    public void set(String key) {
        this.put(key, Boolean.TRUE);
    }

    public void set(String key, Object value) {
        if (value != null) {
            this.put(key, value);
        } else {
            this.remove(key);
        }

    }

    public void unset() {
        threadLocal.remove();
    }

}