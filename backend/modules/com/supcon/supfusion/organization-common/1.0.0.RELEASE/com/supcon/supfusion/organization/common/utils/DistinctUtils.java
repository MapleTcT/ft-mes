package com.supcon.supfusion.organization.common.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class DistinctUtils {

    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> result = new ConcurrentHashMap<>();
        return object -> result.putIfAbsent(keyExtractor.apply(object), Boolean.TRUE) == null;
    }
}
