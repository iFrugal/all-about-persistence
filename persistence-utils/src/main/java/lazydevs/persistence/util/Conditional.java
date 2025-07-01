package lazydevs.persistence.util;


import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;


/**
 * @author Abhijeet Rai
 */
public class Conditional<T> {
    /**
     * Common instance for {@code empty()}.
     */
    private static final Conditional<?> EMPTY = new Conditional<>();

    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private final T value;

    /**
     * Constructs an empty instance.
     *
     * Impl Note:  Generally only one empty instance, {@link Conditional#EMPTY},
     * should exist per VM.
     */
    private Conditional() {
        this.value = null;
    }

    /**
     * Returns an empty {@code Conditional} instance.  No value is present for this
     * Conditional.
     *
     * API Note:  Though it may be tempting to do so, avoid testing if an object
     * is empty by comparing with {@code ==} against instances returned by
     * {@code Option.empty()}. There is no guarantee that it is a singleton.
     * Instead, use {@link #isPresent()}.
     *
     * @param <T> Type of the non-existent value
     * @return an empty {@code Conditional}
     */
    public static<T> Conditional<T> empty() {
        @SuppressWarnings("unchecked")
        Conditional<T> t = (Conditional<T>) EMPTY;
        return t;
    }

    /**
     * Constructs an instance with the value present.
     *
     * @param value the non-null value to be present
     * @throws NullPointerException if value is null
     */
    private Conditional(T value) {
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Returns an {@code Conditional} with the specified present non-null value.
     *
     * @param <T> the class of the value
     * @param value the value to be present, which must be non-null
     * @return an {@code Conditional} with the value present
     * @throws NullPointerException if value is null
     */
    public static <T> Conditional<T> of(T value) {
        return new Conditional<>(value);
    }

    /**
     * Returns an {@code Conditional} describing the specified value, if non-null,
     * otherwise returns an empty {@code Conditional}.
     *
     * @param <T> the class of the value
     * @param value the possibly-null value to describe
     * @return an {@code Conditional} with a present value if the specified value
     * is non-null, otherwise an empty {@code Conditional}
     */
    public static <T> Conditional<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }

    /**
     * If a value is present in this {@code Conditional}, returns the value,
     * otherwise throws {@code NoSuchElementException}.
     *
     * @return the non-null value held by this {@code Conditional}
     * @throws NoSuchElementException if there is no value present
     *
     * @see Conditional#isPresent()
     */
    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    /**
     * Return {@code true} if there is a value present, otherwise {@code false}.
     *
     * @return {@code true} if there is a value present, otherwise {@code false}
     */
    public boolean isPresent() {
        return value != null;
    }


    public boolean in(T... tArr){
        if(!isPresent()) return false;
        if(null != tArr) {
            return Arrays.stream(tArr).anyMatch(value::equals);
        }
        return false;
    }

    public boolean notIn(T... tArr) {
        if (!isPresent()) return true;
        if (tArr != null) {
            return Arrays.stream(tArr).noneMatch(value::equals);
        }
        return true;
    }

}
