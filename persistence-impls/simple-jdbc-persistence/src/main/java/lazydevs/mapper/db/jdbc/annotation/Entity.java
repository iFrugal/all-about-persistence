package lazydevs.mapper.db.jdbc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Target({TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Entity {
	String value();
	boolean autoCreate() default false;
	String defaultOrder() default "";
}
