package com.supcon.supfusion.auth.service.excel.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

/**
 * @author lifangyuan
 */
@Data
@ColumnWidth(100)
@HeadRowHeight(20)
public class Explain {
    @ExcelProperty("导入规则说明")
    private String string;
}
