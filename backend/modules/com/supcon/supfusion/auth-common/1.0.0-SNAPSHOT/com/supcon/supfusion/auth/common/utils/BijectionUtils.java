package com.supcon.supfusion.auth.common.utils;

import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class BijectionUtils {


    public static <T, R> List<R> applys(List<T> list, Callable<R> func) {

        if (list==null){
              return Collections.emptyList();
        }

        return list.stream().map(x -> {
            if (x == null) {
                return null;
            }
            R call = func.call();
            BeanUtils.copyProperties(x, call);
            return call;
        }).collect(Collectors.toList());
    }

    public static <T, R> R apply(T t, Callable<R> func) {

        if (t == null) {
            return null;
        }
        R call = func.call();
        BeanUtils.copyProperties(t, call);
        return call;

    }

    public static <T, R> R apply(T t, Function<T, R> func) {
        if (t == null) {
            return null;
        }
        R call = func.apply(t);
        return call;

    }

    public static <T, R> List<R> applys(List<T> list, Function<T, R> func) {
        if (list==null){
            return Collections.emptyList();
        }
        return list.stream().map(x -> {
            if (x == null) {
                return null;
            }
            return func.apply(x);

        }).collect(Collectors.toList());
    }

    @FunctionalInterface
    public interface Callable<V> {

        V call();
    }

}
