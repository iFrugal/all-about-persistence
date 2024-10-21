package lazydevs.scheduleit.annotations;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(METHOD)
@Retention(RUNTIME)
public @interface ScheduleIt {
    String cron() default "";
    String timezone() default "";
    boolean audit() default true;
    String multiCron() default "";
    boolean locked() default false;
    String lockName() default "";
    int lockAtLeastFor() default 300;
    int lockAtMostFor() default 600;

    @Getter @Setter @ToString
    class MultiCron {
        private String lockName;
        private Integer lockAtLeastFor;
        private Integer lockAtMostFor;
        private String cronExpression;
        private String timezone;
        private Object[] paramsInOrder;
    }
}
