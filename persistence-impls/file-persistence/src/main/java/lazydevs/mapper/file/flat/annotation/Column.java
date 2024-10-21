package lazydevs.mapper.file.flat.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;

@Target({FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {

	int index();

	String name() default "";

	//String deserializeUsing() default "";


	@Target({FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Serialize {
		Class<? extends Serializer<?>> using();
	}

	@Target({FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface DeSerialize {
		Class<? extends DeSerializer> using();
	}

	@Target({FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface ToAndFroSerialize {
		Class<? extends ToAndFroSerializer> using();
	}

	@Target({TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Filter {
		Class<? extends FilterDoer<?>> using();
	}

	@Target({FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface NestedColumn {
		int[] index();

		String name() default "";

		String deserializeUsing() default "";

		String serializeUsing() default "";
	}

	public interface Serializer<T> {
		String serialize(T o);
	}

	public interface DeSerializer<T> {
		T deSerialize(String s);
	}

	public interface ToAndFroSerializer<T> extends Serializer<T>, DeSerializer<T>{

	}

	public interface FilterDoer<T> {
		boolean accept(T t);
	}

}

