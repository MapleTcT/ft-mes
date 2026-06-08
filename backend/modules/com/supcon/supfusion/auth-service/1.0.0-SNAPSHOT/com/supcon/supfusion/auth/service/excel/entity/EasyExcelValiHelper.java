package com.supcon.supfusion.auth.service.excel.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class EasyExcelValiHelper {
    private EasyExcelValiHelper() {
    }

    private static Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public static <T> boolean validateEntity(T obj, Integer row, CustomCellWriteHandler customCellWriteHandler, Pattern passwordRex, String passwordRemind) throws NoSuchFieldException {
        long now = System.currentTimeMillis();
        Set<ConstraintViolation<T>> set = validator.validate(obj, Default.class);
        if (set != null && !set.isEmpty()) {
            for (ConstraintViolation<T> cv : set) {
                Field declaredField = obj.getClass().getDeclaredField(cv.getPropertyPath().toString());
                ExcelProperty annotation = declaredField.getAnnotation(ExcelProperty.class);
                customCellWriteHandler.put(row, annotation.index(), cv.getMessage());
            }
            return false;
        }
        if (obj instanceof UserEntity) {
            UserEntity userEntity = (UserEntity) obj;
            Matcher matcher = passwordRex.matcher(userEntity.getPassword());
            if (!matcher.find()) {
                customCellWriteHandler.put(row, 1, passwordRemind);
                return false;
            }
        }
        log.info("valid cost time ======>"+(System.currentTimeMillis()-now));
        return true;
    }
}
