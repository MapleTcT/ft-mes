package com.supcon.supfusion.signature.services.utils.excel;

import com.supcon.supfusion.framework.cloud.common.util.DateUtil;
import com.supcon.supfusion.signature.dao.entity.SignatureLog;
import com.supcon.supfusion.signature.services.utils.callback.ExcelCallback;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExcelUtils {


    public static void createSignatureLogExcelFile(ExcelCallback excelCallback, String filePath){
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("电子签名日志");
        //行号计数器
        AtomicInteger rownum = new AtomicInteger();
        //设置表头
        XSSFRow row = sheet.createRow(0);
        rownum.incrementAndGet();
        String[] headers = {
                "业务模块","实体名称","模型名称","业务主键","按钮名称",
                "IP地址","流程名称","活动名称","迁移线名称","首签人",
                "首签时间","签名类型","首签原因","首签备注","次签人",
                "次签时间","次签原因","次签备注"
        };
        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth( i,255 * 30);
            XSSFCell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
        }
        //创建样式
        CellStyle cellStyle = wb.createCellStyle();
//        //设置自动换行
//        cellStyle.setWrapText(true);
        //页码计数器
        //添加数据
        setData(excelCallback, sheet, rownum, cellStyle);
        //输出到文件
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        if (!parentFile.isFile()){
            parentFile.mkdirs();
        }
        try(
                OutputStream outputStream = new FileOutputStream(file);
        ) {
            wb.write(outputStream);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static void setData(ExcelCallback excelCallback, XSSFSheet sheet, AtomicInteger rownum, CellStyle cellStyle) {
        AtomicInteger pagination = new AtomicInteger();
        while (true){
            List<SignatureLog> data = excelCallback.getData(pagination.incrementAndGet());
            if (data == null || data.size() <= 0){
              return;
            }
            for (int i = 0; i < data.size(); i++) {
                XSSFRow dataRow = sheet.createRow(rownum.getAndIncrement());
                SignatureLog signatureLog = data.get(i);
                setColumValue(dataRow,0,signatureLog.getModuleName(),cellStyle);
                setColumValue(dataRow,1,signatureLog.getEntityName(),cellStyle);
                setColumValue(dataRow,2,signatureLog.getModelName(),cellStyle);
                setColumValue(dataRow,3,signatureLog.getBusinessKey(),cellStyle);
                setColumValue(dataRow,4,signatureLog.getButtonName(),cellStyle);

                setColumValue(dataRow,5,signatureLog.getIpAddress(),cellStyle);
                setColumValue(dataRow,6,signatureLog.getProcessName(),cellStyle);
                setColumValue(dataRow,7,signatureLog.getTaskName(),cellStyle);
                setColumValue(dataRow,8,signatureLog.getTransitionName(),cellStyle);
                setColumValue(dataRow,9,signatureLog.getFirstUserName(),cellStyle);

                setColumValue(dataRow,10,signatureLog.getFirstSignTime(),cellStyle);
                setColumValue(dataRow,11,signatureLog.getSignatureType(),cellStyle);
                setColumValue(dataRow,12,signatureLog.getFirstReason(),cellStyle);
                setColumValue(dataRow,13,signatureLog.getFirstRemark(),cellStyle);
                setColumValue(dataRow,14,signatureLog.getSecondUserName(),cellStyle);

                setColumValue(dataRow,15,signatureLog.getSecondSignTime(),cellStyle);
                setColumValue(dataRow,16,signatureLog.getSecondReason(),cellStyle);
                setColumValue(dataRow,17,signatureLog.getSecondRemark(),cellStyle);
            }
        }
    }

    private static void setColumValue(XSSFRow row ,int columcolumnNumber,Object value,CellStyle cellStyle) {
        XSSFCell cell = row.createCell(columcolumnNumber);
        cell.setCellStyle(cellStyle);
        if (value == null){
            return;
        }
        if (value instanceof Boolean){
            cell.setCellValue((Boolean)value);
        }else if (value instanceof Date){
            String format = DateUtil.format((Date) value, DateUtil.PATTERN_DATETIME);
            cell.setCellValue(format);
        }else {
            cell.setCellValue(String.valueOf(value));
        }
    }

}
