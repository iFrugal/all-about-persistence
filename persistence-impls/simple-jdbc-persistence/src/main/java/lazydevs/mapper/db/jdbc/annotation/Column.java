package lazydevs.mapper.db.jdbc.annotation;

import lazydevs.mapper.db.jdbc.JDBCParam;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;


@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String name() default "";
    JDBCParam.Type dbType();
    String precision() default "";
    boolean nullable() default true;
    Class<?> javaType() default String.class;
    String converterMethod() default "";
}
