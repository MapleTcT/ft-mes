package com.supcon.supfusion.systemcode.common.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import org.springframework.beans.BeanUtils;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;

/**
 * 对象操作工具类
 * 提供1、对象属性复制方法
 * @author root
 *
 */
public class SupBeanUtils {
	
	/**
	 * 通过无参构造方法创建target对象，将source对象的属性复制给target对象
	 * @param source 源头对象实例 
	 * @param targetClass 目标对象类型
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static <T> T copyProperties(Object source, Class<T> targetClass) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor<T> constructor = targetClass.getConstructor();
		T target = constructor.newInstance();
		BeanUtils.copyProperties(source, target);
		return target;
	}
	
	/**
	 * 对PageResult的结果集中的PO对象转换成DTO或VO对象
	 * @param source 源头PageResult对象实例 
	 * @param targetClass 目标对象类型
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static <T, E> PageResult<T> copyPageResult(PageResult<E> source, Class<T> targetClass) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<E> sourceList = (List<E>) source.getList();
		PageResult<T> target = new PageResult<T>(null, source.getPagination().getTotal(), source.getPagination().getPageSize(), source.getPagination().getCurrent());
		if (sourceList == null || sourceList.size() < 1) {
			return target;
		}
		List<T> targetList = new ArrayList<T>(sourceList.size());
		for (E sourceItem: sourceList) {
			T targetItem = copyProperties(sourceItem, targetClass);
			targetList.add(targetItem);
		}
		target.setList(targetList);
		return target;
	}

	/**
	 * 对ListResult的结果集中的PO对象转换成DTO或VO对象
	 * @param source 源头ListResult对象实例
	 * @param targetClass 目标对象类型
	 * @return
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	public static <T, E> ListResult<T> copyListResult(ListResult<E> source, Class<T> targetClass) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<E> sourceList = (List<E>) source.getList();
		ListResult<T> target = new ListResult<T>();
		if (sourceList == null || sourceList.size() < 1) {
			return target;
		}
		List<T> targetList = new ArrayList<T>(sourceList.size());
		for (E sourceItem: sourceList) {
			T targetItem = copyProperties(sourceItem, targetClass);
			targetList.add(targetItem);
		}
		target.setList(targetList);
		return target;
	}
	
}
