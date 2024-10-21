package lazydevs.conman;

import org.springframework.context.annotation.Import;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Abhijeet Rai
 */


@Target(TYPE)
@Retention(RUNTIME)
@Import({ConmanAutoConfiguration.class})
public @interface EnableConman {
}
