package com.supcon.supfusion.notification.common;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;


public final class CachabledOptional<T> {

    private static final CachabledOptional<?> EMPTY = new CachabledOptional<>(null);

    private Long expire = Long.valueOf(1 << 31);

    public CachabledOptional<T> setExpireMs(Long expire) {
        this.expire = expire;
        return this;
    }

    private final Pair<T> value;

    private final class Pair<T> {
        T value;
        Long ts;

        public Pair(T value, Long timeStampe) {
            this.value = value;
            this.ts = timeStampe;
        }
    }

    public static <T> CachabledOptional<T> empty() {
        @SuppressWarnings("unchecked")
        CachabledOptional<T> t = (CachabledOptional<T>) EMPTY;
        return t;
    }

    private CachabledOptional(T value) {
        this.value = new Pair(value, System.currentTimeMillis());
    }

    public static <T> CachabledOptional<T> of(T value) {
        return new CachabledOptional<>(Objects.requireNonNull(value));
    }

    @SuppressWarnings("unchecked")
    public static <T> CachabledOptional<T> ofNullable(T value) {
        return value == null ? (CachabledOptional<T>) EMPTY
                : new CachabledOptional<>(value);
    }


    public T get() {
        if (value == null) {
            throw new NoSuchElementException("No value present");
        }
        return value.value;
    }


    public boolean isPresent() {
        return value != null && value.value!=null && value.ts + expire >= System.currentTimeMillis();
    }


    public boolean isEmpty() {
        return value == null || value.value==null;
    }


    public void ifPresent(Consumer<? super T> action) {
        if (isPresent()) {
            action.accept(value.value);
        }
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isPresent()) {
            action.accept(value.value);
        } else {
            emptyAction.run();
        }
    }

    public CachabledOptional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (!isPresent()) {
            return this;
        } else {
            return predicate.test(value.value) ? this : empty();
        }
    }

    public <U> CachabledOptional<U> map(Function<? super T, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            return CachabledOptional.ofNullable(mapper.apply(value.value));
        }
    }

    public <U> CachabledOptional<U> flatMap(Function<? super T, ? extends CachabledOptional<? extends U>> mapper) {
        Objects.requireNonNull(mapper);
        if (!isPresent()) {
            return empty();
        } else {
            @SuppressWarnings("unchecked")
            CachabledOptional<U> r = (CachabledOptional<U>) mapper.apply(value.value);
            return Objects.requireNonNull(r);
        }
    }

    public T orElse(T other) {

        return isPresent() ? value.value : other;
    }


    public T orElseGet(Supplier<? extends T> supplier) {

        if (isPresent()) {
            return value.value;
        } else {
            T t = supplier.get();
            this.value.value = t;
            this.value.ts = System.currentTimeMillis();
            return t;
        }
    }


    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return isPresent()
                ? String.format("Optional[%s]", value)
                : "Optional.empty";
    }
}
