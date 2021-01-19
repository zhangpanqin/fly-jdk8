package java.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * @author 张攀钦
 */
public final class Optional<T> {

    private static final Optional<?> EMPTY = new Optional<>();

    /**
     * If non-null, the value; if null, indicates no value is present
     */
    private final T value;


    private Optional() {
        this.value = null;
    }

    public static <T> Optional<T> empty() {
        Optional<T> t = (Optional<T>) EMPTY;
        return t;
    }


    private Optional(T value) {
        this.value = Objects.requireNonNull(value);
    }


    public static <T> Optional<T> of(T value) {
        return new Optional<>(value);
    }


    public static <T> Optional<T> ofNullable(T value) {
        return value == null ? empty() : of(value);
    }


    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }


    public void ifPresent(Consumer<? super T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    /**
     * 对其进行过滤
     */
    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) {
            return this;
        } else {
            return predicate.test(value) ? this : empty();
        }
    }

    /**
     * 将其转换为另一个 map
     */
    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            return Optional.ofNullable(mapper.apply(value));
        }
    }

    public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            return Objects.requireNonNull(mapper.apply(value));
        }
    }


    public T orElse(T other) {
        return value != null ? value : other;
    }


    public T orElseGet(Supplier<? extends T> other) {
        return value != null ? value : other.get();
    }


    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (value != null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }


    /**
     * 判断 value 是否相等
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Optional)) {
            return false;
        }

        Optional<?> other = (Optional<?>) obj;
        return Objects.equals(value, other.value);
    }
}
