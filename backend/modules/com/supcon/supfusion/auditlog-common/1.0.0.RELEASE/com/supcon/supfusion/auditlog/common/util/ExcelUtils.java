package com.supcon.supfusion.auditlog.common.util;

import com.supcon.supfusion.auditlog.common.exception.AuditLogErrorEnum;
import com.supcon.supfusion.auditlog.common.exception.AuditLogException;
import com.supcon.supfusion.auditlog.common.model.ExcelTitle;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Excel处理类
 */
@Slf4j
public class ExcelUtils {

    public static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);

    public static final String EXCEL_FILE_PATH = "excel/";
    public static final String EXCEL_ERROR_FILE_PATH = "excel/error/";
    public static final String EXCEL_FILE_TEMPLATE_PATH = "excel/template/";
    public static final String EXCEL_FILE_EXPORT_PATH = "excel/export/";

    private static final Integer EXPLAIN_COLUMN_LENGTH = 256 * 100;
    public static final Integer COLUMN_LENGTH = 256 * 30;

    public static final List<String> AUDITLOG_TEMPLATE_EXPLAIN = new ArrayList<String>();

    /**
     * 审计日志导出数据标题
     */
    public static final List<ExcelTitle> AUDITLOG_IMPORT_TEMPLATE = new ArrayList<ExcelTitle>();

    /**
     * 审计日志导出数据模型标题
     */
    public static final List<ExcelTitle> AUDITLOG_MODEL_IMPORT_TEMPLATE = new ArrayList<ExcelTitle>();

    public static final String AUDITLOG_FILE = "审计日志模板.xlsx";
    public static final String AUDITLOG_MODEL_FILE = "审计日志模型模板.xlsx";

    static {
        //审计日志导入模板模板说明
        AUDITLOG_TEMPLATE_EXPLAIN.add("编码不可以为空，并且长度不可以超过50");
        AUDITLOG_TEMPLATE_EXPLAIN.add("名称不可以为空，并且长度不可以超过200");
        AUDITLOG_TEMPLATE_EXPLAIN.add("描述长度不可以超过500");
        AUDITLOG_TEMPLATE_EXPLAIN.add("标红部分为必填项");

        //审计日志导入模板
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("moduleName", "模块名称"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("formName", "表单名称"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("operateUserName", "操作用户名称"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("operateTime", "操作时间"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("modelObjName", "被操作对象名称"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("modelObjCode", "被操作对象编码"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("operateType", "操作类型"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("ipAddress", "IP地址"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("description", "描述"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("exceptionDescription", "操作异常描述"));
        AUDITLOG_IMPORT_TEMPLATE.add(new ExcelTitle("fileName", "文件名称"));

        //审计日志模型导入模板
        AUDITLOG_MODEL_IMPORT_TEMPLATE.add(new ExcelTitle("formName", "表单名称"));
        AUDITLOG_MODEL_IMPORT_TEMPLATE.add(new ExcelTitle("modelObjName", "被操作对象名称"));
        AUDITLOG_MODEL_IMPORT_TEMPLATE.add(new ExcelTitle("modelObjCode", "被操作对象编码"));
        AUDITLOG_MODEL_IMPORT_TEMPLATE.add(new ExcelTitle("operateType", "操作类型"));
        AUDITLOG_MODEL_IMPORT_TEMPLATE.add(new ExcelTitle("description", "描述"));
    }

    /**
     * 设置单元格样式
     *
     * @param workbook
     * @return
     */
    public static CellStyle createCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        //新建Cell字体
        style.setFont(createCellFont(workbook));
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }


    public static Font createCellFont(Workbook workbook) {
        Font font = workbook.createFont();
        //在对应的workbook中新建字体
        font.setFontName("微软雅黑");
        //字体微软雅黑
        font.setFontHeightInPoints((short) 11);
        return font;
    }

    /**
     * 设置模板标题头的字体
     *
     * @param workbook
     * @return
     */
    public static Font createHeadFont(Workbook workbook) {
        Font font = workbook.createFont();
        //在对应的workbook中新建字体
        font.setFontName("微软雅黑");
        //字体加粗
        font.setBold(true);
        //字体微软雅黑
        font.setFontHeightInPoints((short) 11);
        return font;
    }

    /**
     * 设置模板标题行的样式
     *
     * @param workbook
     * @param font
     * @return
     */
    public static CellStyle createHeadStyle(Workbook workbook, Font font) {
        CellStyle style = workbook.createCellStyle();
        //新建Cell字体
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    /**
     * 设置模板标题头的背景颜色
     *
     * @param workbook
     * @param font
     * @return
     */
    public static CellStyle createCellStyleWithBackgroundColor(Workbook workbook, Font font) {
        CellStyle style = workbook.createCellStyle();
        //新建Cell字体
        style.setFont(font);
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("@"));
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    /**
     * 设置导入错误的错误文件的单元格样式
     *
     * @param workbook
     * @return
     */
    public static CellStyle createImportErrorCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.MEDIUM);
        style.setBorderLeft(BorderStyle.MEDIUM);
        style.setBorderRight(BorderStyle.MEDIUM);
        style.setBorderTop(BorderStyle.MEDIUM);

        style.setBottomBorderColor(IndexedColors.RED.getIndex()); //下边框
        style.setLeftBorderColor(IndexedColors.RED.getIndex());//左边框
        style.setTopBorderColor(IndexedColors.RED.getIndex());//上边框
        style.setRightBorderColor(IndexedColors.RED.getIndex());//右边框
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        style.setFont(font);
        return style;
    }

    /**
     * 创建目录
     *
     * @param folderPath
     */
    public static void createFolder(String folderPath) {
        File dir = new File(folderPath);
        if (!dir.exists()) {
            boolean mkFlag = dir.mkdirs();
            if (!mkFlag) {
                throw new AuditLogException(AuditLogErrorEnum.EXCEL_FILE_CREATE_ERROR);
            }
        }
    }

    /**
     * 创建Excel错误信息文件保存本地
     *
     * @param workbook
     * @param fileName
     * @param id
     */
    public static String createErrorExcelFile(Workbook workbook, String fileName, Long id) throws FileNotFoundException, IOException {
        createFolder(ExcelUtils.EXCEL_ERROR_FILE_PATH);

        String filePath = EXCEL_ERROR_FILE_PATH + id + "_error_" + fileName;
        createFile(filePath);
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        workbook.write(fileOutputStream);
        fileOutputStream.flush();
        fileOutputStream.close();
        workbook.close();
        return filePath;
    }

    /**
     * 创建Excel文件保存本地
     *
     * @param workbook
     * @param fileName
     * @param id
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static String createExcelFile(Workbook workbook, String fileName, Long id) throws FileNotFoundException, IOException {
        createFolder(ExcelUtils.EXCEL_FILE_PATH);
        String filePath = EXCEL_FILE_PATH + id + "_" + fileName;
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        workbook.write(fileOutputStream);
        //workbook.close();
        fileOutputStream.flush();
        fileOutputStream.close();
        return id + "_" + fileName;
    }

    /**
     * 导出文件写入
     *
     * @param workbook
     * @return
     * @throws IOException
     */
    public static void createExportFile(Workbook workbook, String fileName, HttpServletResponse response) throws IOException {
        // 导出Excel
        OutputStream outputStream = null;

        try {
            outputStream = response.getOutputStream();
            //下面三行是关键代码，处理乱码问题
            response.setContentType("application/x-download");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            workbook.write(outputStream);
        } catch (Exception e) {
            log.error(e.getMessage());
        }finally {
            outputStream.close();
            workbook.close();
        }
    }

    /**
     * 创建标题
     *
     * @param sheet
     * @param list
     */
    public static void createHead(Sheet sheet, List<String> list) {
        Row head = sheet.createRow(0);
        Workbook workbook = sheet.getWorkbook();

        Font font = createHeadFont(workbook);
        font.setColor(IndexedColors.RED.index);
        CellStyle colorStyle = createHeadStyle(workbook, font);

        CellStyle backgroundColorStyle = createCellStyleWithBackgroundColor(workbook, createHeadFont(workbook));

        CellStyle centerStyle = createHeadStyle(workbook, createHeadFont(workbook));
        centerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle headStyle = createHeadStyle(workbook, createHeadFont(workbook));

        int columnIndex = 0;
        for (String aList : list) {
            Cell cell = head.createCell(columnIndex++, CellType.STRING);
            String headStr = aList;
            if (headStr.startsWith("*")) {
                cell.setCellStyle(colorStyle);
                cell.setCellValue(headStr);
            } else if (headStr.startsWith("color:")) {
                cell.setCellStyle(backgroundColorStyle);
                cell.setCellValue(headStr.substring("color:".length()));
            } else {
                cell.setCellStyle(headStyle);
                cell.setCellValue(headStr);
            }
        }
    }

    /**
     * 创建模板"说明"sheet页
     *
     * @param explainMessages 说明信息
     * @param workbook        Excel对象
     */
    public static void createExplainSheet(List<String> explainMessages, Workbook workbook) {
        Sheet explainSheet = workbook.createSheet("说明");

        CellStyle cellStyle = createCellStyle(workbook);
        cellStyle.setWrapText(true);
        explainSheet.setColumnWidth(0, EXPLAIN_COLUMN_LENGTH);
        createHead(explainSheet, Arrays.asList("导入规则说明:"));

        for (int i = 0; i < explainMessages.size(); i++) {
            Cell cell = explainSheet.createRow(i).createCell(0);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(explainMessages.get(i));
        }
    }

    /**
     * 创建标题和标题的批注
     *
     * @param sheet
     * @param titles
     */
    public static void createHeadComments(Sheet sheet, List<ExcelTitle> titles) {
        List<String> comments = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        Iterator<ExcelTitle> it = titles.iterator();
        while (it.hasNext()) {
            ExcelTitle title = it.next();
            comments.add(title.getComment());
            values.add(title.getTitle());
        }
        createHead(sheet, values);
        Row headRow = sheet.getRow(0);
        Iterator<Cell> cellIt = headRow.cellIterator();
        Drawing drawing = sheet.createDrawingPatriarch();
        int columnIndex = 0;
        while (cellIt.hasNext()) {
            Cell cell = cellIt.next();
            Comment comment = drawing.createCellComment(new XSSFClientAnchor(0, 0, 0, 0, (short) 3, 3, (short) 9, 10));
            comment.setString(new XSSFRichTextString(comments.get(columnIndex)));
            cell.setCellComment(comment);
            columnIndex++;
        }
    }

    public static File createFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                boolean flag = file.createNewFile();
                if (!flag) {
                    throw new AuditLogException(AuditLogErrorEnum.EXCEL_FILE_CREATE_ERROR);
                }
            } catch (IOException e) {
                logger.error("创建文件异常", e);
                throw new AuditLogException(AuditLogErrorEnum.EXCEL_FILE_CREATE_ERROR);
            }
        }
        return file;
    }

    public static void removeComment(Cell cell) {
        if (cell != null && cell.getCellComment() != null && cell.getCellComment().getString() != null) {
            cell.removeCellComment();
        }
    }

}
