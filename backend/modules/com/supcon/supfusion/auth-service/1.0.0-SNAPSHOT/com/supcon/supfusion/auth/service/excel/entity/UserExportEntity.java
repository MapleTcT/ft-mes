package com.supcon.supfusion.auth.service.excel.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import com.alibaba.excel.annotation.write.style.HeadStyle;
import lombok.Data;
import org.apache.poi.ss.usermodel.FillPatternType;

/**
 * @author lifangyuan
 */
@Data
@ColumnWidth(20)
@HeadRowHeight(20)
public class UserExportEntity {

    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)
    @ExcelProperty("*用户名称")
    private String userName;

    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)
    @ExcelProperty("*用户类型")
    private String userType;

    @HeadStyle(fillPatternType = FillPatternType.SOLID_FOREGROUND, fillForegroundColor = 10)
    @ExcelProperty("*人员名称")
    private String personName;


    @ExcelProperty("角色")
    private String role;

    @ExcelProperty("用户描述")
    private String description;

}
