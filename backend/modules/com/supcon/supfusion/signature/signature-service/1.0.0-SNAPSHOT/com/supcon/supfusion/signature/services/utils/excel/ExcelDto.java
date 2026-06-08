package com.supcon.supfusion.signature.services.utils.excel;

import com.alibaba.fastjson.JSONArray;
import lombok.Data;
import java.util.List;

@Data
public class ExcelDto {
    private List<ExcelHeaderDto> excelHeaders;
    private JSONArray data;
    //字体大小
    private int fontSize = 14;
    //行高
    private int rowHeight = 30;
    //列宽
    private int columWidth = 200;
    //工作表
    private String sheetName ;

}
