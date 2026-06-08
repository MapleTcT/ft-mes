package com.supcon.supfusion.i18n.common.until;

import java.text.DecimalFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.util.StringUtils;
@Slf4j
public class ExcelUtils{

    /**
     * 解析excel
     */
    private static FormulaEvaluator evaluator;

//    //获取单元格各类型值，返回字符串类型
    public static String getCellValueByCell(Cell cell) {
        //判断是否为null或空串
        if (cell == null || cell.toString().trim().equals("")) {
            return "";
        }
        String cellValue = "";
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) { //表达式类型
            cellType = evaluator.evaluate(cell).getCellType();
        }
        switch (cellType) {
            case STRING: //字符串类型
                cellValue = cell.getStringCellValue().trim();
                cellValue = StringUtils.isEmpty(cellValue) ? "" : cellValue;
                break;
            case BOOLEAN:  //布尔类型
                cellValue = String.valueOf(cell.getBooleanCellValue());
                break;
            case NUMERIC: //数值类型
                //cellValue = new DecimalFormat("#").format(cell.getNumericCellValue());
//                cellValue =String.valueOf(cell.getNumericCellValue());
                cell.setCellType(CellType.STRING);
                cellValue =String.valueOf(cell.getStringCellValue());
                break;
            default: //其它类型，取空串吧
                cellValue = "";
                break;
        }
        return cellValue;
    }

    private ExcelUtils() {
        throw new IllegalStateException("ExcelUtils class");
    }
}
