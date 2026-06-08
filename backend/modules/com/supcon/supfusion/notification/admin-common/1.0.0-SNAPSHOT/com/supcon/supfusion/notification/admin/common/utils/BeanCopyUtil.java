package com.supcon.supfusion.notification.admin.common.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class BeanCopyUtil extends BeanUtils {

    /**
     * 集合数据的拷贝
     *
     * @param sources: 数据源类
     * @param target:  目标类::new(eg: UserVO::new)
     * @return
     */
    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target) {
        return copyListProperties(sources, target, null);
    }


    /**
     * 带回调函数的集合数据的拷贝（可自定义字段拷贝规则）
     *
     * @param sources:  数据源类
     * @param target:   目标类::new(eg: UserVO::new)
     * @param callBack: 回调函数
     * @return
     */
    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target, BiConsumer<S, T> callBack) {
        List<T> list = new ArrayList<>(sources.size());
        for (S source : sources) {
            T t = target.get();
            copyProperties(source, t);
            list.add(t);
            if (callBack != null) {
                // 回调
                callBack.accept(source, t);
            }
        }
        return list;
    }


    /**
     * 数据拷贝
     *
     * @param source:   数据源类
     * @param target:   目标类::new(eg: UserVO::new)
     * @return
     */
    public static <S, T> T copyBeanProperties(S source, Supplier<T> target) {
        return copyBeanProperties(source, target, null);
    }

    /**
     * 带回调函数的数据的拷贝（可自定义字段拷贝规则）
     *
     * @param source:   数据源类
     * @param target:   目标类::new(eg: UserVO::new)
     * @param callBack: 回调函数
     * @return
     */
    public static <S, T> T copyBeanProperties(S source, Supplier<T> target, BiConsumer<S, T> callBack) {
        T t = target.get();
        copyProperties(source, t);
        if (callBack != null) {
            // 回调
            callBack.accept(source, t);
        }
        return t;
    }
}

