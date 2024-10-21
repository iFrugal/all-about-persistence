package lazydevs.scheduleit.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

import java.lang.reflect.Method;

/**
 * @author Abhijeet Rai
 */
@Getter
@ToString
public class ScheduledTarget{
    @JsonIgnore private final Method method;
    @Setter private Object[] args;
    @Getter @JsonIgnore private final Runnable runnable;
    @JsonIgnore private Class<? extends Runnable> pre = DummyRunnable.class;
    @JsonIgnore private Class<? extends Runnable> post = DummyRunnable.class;

    public ScheduledTarget(@NonNull Method method) {
        this.method = method;
        this.runnable = null;
    }

    public ScheduledTarget(@NonNull Runnable runnable) {
        this.method = null;
        this.runnable = runnable;
    }

    public static class DummyRunnable implements Runnable{
        @Override
        public void run() {
            //NoOps
        }
    }
}
