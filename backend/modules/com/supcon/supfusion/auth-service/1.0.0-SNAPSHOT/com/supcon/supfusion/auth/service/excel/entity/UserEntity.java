package com.supcon.supfusion.auth.service.excel.entity;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ColumnWidth(20)
@HeadRowHeight(20)
public class UserEntity {
    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)
    //名称
    @ExcelProperty(index = 0, value = "*用户名称")
    @NotBlank(message = "用户名必填")
    @Length(max = 50, message = "用户名称超过长度50")
    @Pattern(regexp = "^\\w+$", message = "用户名称支持数字 字母 _")
    private String userName;

    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)
    @ExcelProperty(index = 1, value = "*用户密码")
    @NotBlank(message = "用户密码必填")
    private String password;


    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)
    @ExcelProperty(index = 2, value = "*人员名称")
    @NotBlank(message = "人员名称必填")
    private String personName;
    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)

    @ExcelProperty(index = 3, value = "*人员编号")
    @NotBlank(message = "人员编号必填")
    private String personCode;

    @ExcelProperty(index = 4, value = "用户描述")
    @Length(max = 255, message = "用户描述超过长度255")
    private String description;

}
