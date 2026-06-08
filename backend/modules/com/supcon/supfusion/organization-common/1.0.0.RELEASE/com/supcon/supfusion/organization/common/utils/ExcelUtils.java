package com.supcon.supfusion.organization.common.utils;

import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.model.ExcelTitle;
import io.micrometer.core.instrument.util.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Excel处理类
 */
public class ExcelUtils {

    public static final Logger logger = LoggerFactory.getLogger(ExcelUtils.class);
    /**
     * 人员导入模板标题
     */
    private static Map<String, String> PERSON_EXCEL_TEMPLATE_EXPORT = new HashMap<String, String>();

    public static final String EXCEL_FILE_PATH = "excel/";
    public static final String EXCEL_ERROR_FILE_PATH = "excel/error/";
    public static final String EXCEL_FILE_TEMPLATE_PATH = "excel/template/";
    public static final String EXCEL_FILE_EXPORT_PATH = "excel/export/";

    public static final String PERSON_FILE = "人员信息模板.xlsx";
    public static final String POSITION_FILE = "岗位信息模板.xlsx";
    public static final String DEPARTMENT_FILE = "部门信息模板.xlsx";
    public static final String POSITION_RELATION_FILE = "岗位人员关系.xlsx";
    public static final String DEPARTMENT_RELATION_FILE = "部门人员关系.xlsx";

    public static final String PERSON_FILE_ERROR = "人员信息.xlsx";
    public static final String POSITION_FILE_ERROR = "岗位信息.xlsx";
    public static final String DEPARTMENT_FILE_ERROR = "部门信息.xlsx";

    /**
     * 人员导出数据标题
     */
    private static final Map<String, String> PERSON_EXCEL_EXPORT = new HashMap<String, String>();

    private static final List<String> PERSON_TEMPLATE_EXPLAIN = new ArrayList<String>();


    public static final List<String> DEPARTMENT_TEMPLATE_EXPLAIN = new ArrayList<String>();

    public static final List<ExcelTitle> DEPARTMENT_IMPORT_TEMPLATE = new ArrayList<ExcelTitle>();

    public static final List<ExcelTitle> POSITION_IMPORT_TEMPLATE = new ArrayList<ExcelTitle>();

    public static final List<ExcelTitle> RELATION_PERSON_EXPORT = new ArrayList<ExcelTitle>();
    public static final List<String> POSITION_TEMPLATE_EXPLAIN = new ArrayList<String>();
    public static final List<ExcelTitle> PERSON_IMPORT_TEMPLATE = new ArrayList<>();

    private static final Integer EXPLAIN_COLUMN_LENGTH = 256 * 100;

    public static final Integer COLUMN_LENGTH = 256 * 30;

    /**
     * 手机正则
     */
    public static final String PHONE_PATTERN = "^[0-9]*$";

    /**
     * 身份证号正则
     */
    public static final String ID_NUMBER_PATTERN = "[^\\u4e00-\\u9fa5]+";
    /**
     * 邮箱地址正则
     */
    public static final String EMAIL_PATTERN = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

    static {
        //人员导入模板标题
        PERSON_EXCEL_TEMPLATE_EXPORT.put("code", "编号");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("name", "姓名");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("gender", "性别");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("positionCode", "岗位编号");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("classifiedLevel", "涉密等级");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("phone", "手机号");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("email", "邮箱");
        PERSON_EXCEL_TEMPLATE_EXPORT.put("description", "描述");

        //人员导出数据标题
        PERSON_EXCEL_EXPORT.put("name", "姓名");
        PERSON_EXCEL_EXPORT.put("code", "编号");
        PERSON_EXCEL_EXPORT.put("phone", "手机号");
        PERSON_EXCEL_EXPORT.put("departmentFullPath", "部门路径");
        PERSON_EXCEL_EXPORT.put("positionFullPath", "岗位路径");
        PERSON_EXCEL_EXPORT.put("email", "邮箱");
        PERSON_EXCEL_EXPORT.put("gender", "性别");
        PERSON_EXCEL_EXPORT.put("description", "描述");
        PERSON_EXCEL_EXPORT.put("status", "状态");

        //人员导入模板初始化
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("name", "*姓名"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("code", "*编号"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("gender", "*性别"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("status", "*状态"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("mainPositionCode", "主岗编码"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("mainPositionName", "*主岗名称"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("positionCode", "所属岗位编码"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("positionName", "*所属岗位名称"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("phone", "手机号码"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("email", "邮箱地址"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("description", "描述"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("entryDate", "入职日期"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("title", "职称"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("qualification", "资质"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("education", "学历"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("major", "专业"));
        PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("idNumber", "身份证号"));
        //PERSON_IMPORT_TEMPLATE.add(new ExcelTitle("classifiedLevel", "涉密等级"));

        //人员模板说明
        PERSON_TEMPLATE_EXPLAIN.add("人员编码必填,并且长度不可以超过50");
        PERSON_TEMPLATE_EXPLAIN.add("人员姓名必填,并且长度不可以超过200");
        PERSON_TEMPLATE_EXPLAIN.add("人员描述长度不可以超过500");
        PERSON_TEMPLATE_EXPLAIN.add("性别为\"男\"或\"女\"");
        PERSON_TEMPLATE_EXPLAIN.add("当主岗名称存在重复的情况，需要提示用户填写主岗编码");
        PERSON_TEMPLATE_EXPLAIN.add("当主岗编码和主岗名称同时填写，以编码为主");
        PERSON_TEMPLATE_EXPLAIN.add("手机号码，支持填写数字，长度不限");
        PERSON_TEMPLATE_EXPLAIN.add("邮箱地址，验证邮箱格式");
        PERSON_TEMPLATE_EXPLAIN.add("所属岗位必须填写，标识该人所在的岗位，可以只填写所属岗位名称，当有重复时提醒用户填写所属岗位编码，如果二个都填写，以岗位编码为准。");
        PERSON_TEMPLATE_EXPLAIN.add("模板中标红部分为必填");
        PERSON_TEMPLATE_EXPLAIN.add("入职时间格式为yyyy-MM-dd,例如:2021-05-28");
        PERSON_TEMPLATE_EXPLAIN.add("职称为\"初级\"或\"中级\"或\"高级\"");
        PERSON_TEMPLATE_EXPLAIN.add("学历为\"初中及以下\"或\"高中/中专\"或\"大专\"或\"本科\"或\"硕士\"或\"博士\"");
        PERSON_TEMPLATE_EXPLAIN.add("身份证号由数字、大小写字母、特殊字符组成，长度不超过200字符!");
        //PERSON_TEMPLATE_EXPLAIN.add("涉密等级为\"一般涉密\"或\"重要涉密\"或\"核心涉密\"");

        //部门导入模板模板说明
        DEPARTMENT_TEMPLATE_EXPLAIN.add("部门编码不可以为空，并且长度不可以超过50");
        DEPARTMENT_TEMPLATE_EXPLAIN.add("部门名称不可以为空，并且长度不可以超过200");
        DEPARTMENT_TEMPLATE_EXPLAIN.add("描述长度不可以超过500");
        DEPARTMENT_TEMPLATE_EXPLAIN.add("上级部门不传时则是第一级部门，如果要在其他部门下创建部门则必须传上级部门，可以只填写上级部门名称、当有重复时，提示用户填写上级部门编码，如果二者都填了，以编码为准。");
        DEPARTMENT_TEMPLATE_EXPLAIN.add("负责人编码，填写多个，以英文逗号隔开，导入时以编码为准");
        DEPARTMENT_TEMPLATE_EXPLAIN.add("负责人名称，填写多个，以英文逗号隔开，导入时以编码为准");
        DEPARTMENT_TEMPLATE_EXPLAIN.add("标红部分为必填项");

        //部门导入批注对应标题
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("code", "*部门编码"));
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("name", "*部门名称"));
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("parentCode", "上级部门编码"));
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("parentName", "上级部门名称"));
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("managerCode", "负责人编号[英文逗号隔开]"));
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("managerName", "负责人姓名[英文逗号隔开]"));
        DEPARTMENT_IMPORT_TEMPLATE.add(new ExcelTitle("description", "描述"));

        //岗位导入模板说明
        POSITION_TEMPLATE_EXPLAIN.add("岗位编码不可以为空，并且长度不可以超过50");
        POSITION_TEMPLATE_EXPLAIN.add("岗位名称不可以为空，并且长度不可以超过200");
        POSITION_TEMPLATE_EXPLAIN.add("描述长度不可以超过500");
        POSITION_TEMPLATE_EXPLAIN.add("部门名称不可以为空，只允许填写已存在的部门，如果部门名称存在重复，需要提示用户，该部门名存在重复请填写部门编码");
        POSITION_TEMPLATE_EXPLAIN.add("部门编码可以为空，当部门名称存在重复时必须填写，如果部门编码和名称都填写了，以部门编码的为准");
        POSITION_TEMPLATE_EXPLAIN.add("上级部门不传时则是第一级部门，如果要在其他部门下创建部门则必须传上级部门，可以只填写上级部门名称、当有重复时，提示用户填写上级部门编码，如果二者都填了，以编码为准。");
        POSITION_TEMPLATE_EXPLAIN.add("所属角色编码可空，支持多个，以英文逗号隔开，导入时以编码为准");
        POSITION_TEMPLATE_EXPLAIN.add("所属角色名称，可空，支持多个，以英文逗号隔开");
        POSITION_TEMPLATE_EXPLAIN.add("上级岗位编码和名称不传时，则为第一级岗位，如果需要在其他岗位下创建岗位，则必须填上级岗位编码或名称、填写了编码以编码为准，如果只填写了名称，当有重复的时候，需要提醒用户该上级岗位名称存在重复，请填写上级岗位的编码");
        POSITION_TEMPLATE_EXPLAIN.add("标红部分为必填项");
        //岗位导入批注对应标题
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("code", "*岗位编码"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("name", "*岗位名称"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("departmentCode", "部门编码"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("departmentName", "*部门名称"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("parentCode", "上级岗位编码"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("parentName", "上级岗位名称"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("roleCode", "所属角色编号[英文逗号隔开]"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("roleName", "所属角色名称[英文逗号隔开]"));
        POSITION_IMPORT_TEMPLATE.add(new ExcelTitle("description", "描述"));

        //关联人员信息
        RELATION_PERSON_EXPORT.add(new ExcelTitle("name", "姓名"));
        RELATION_PERSON_EXPORT.add(new ExcelTitle("code", "编号"));
        RELATION_PERSON_EXPORT.add(new ExcelTitle("phone", "手机号"));
        RELATION_PERSON_EXPORT.add(new ExcelTitle("gender", "性别"));

    }



    public static List<String> getPersonTemplateExplainComments() {
        return PERSON_TEMPLATE_EXPLAIN;
    }




    /**
     * 创建模板"说明"sheet页
     * @param explainMessages 说明信息
     * @param workbook Excel对象
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
     * 设置单元格样式
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
     * 创建导入模板的标题头的行
     * @param sheet
     * @param list
     */
/*    public static void createHead(Sheet sheet, List<String> list) {
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
            } else if ("时间粒度".equals(aList) || "统计类型".equals(aList)) {
                Cell b = head.createCell(columnIndex++, CellType.STRING);
                Cell c = head.createCell(columnIndex++, CellType.STRING);
                Cell d = head.createCell(columnIndex++, CellType.STRING);
                cell.setCellStyle(centerStyle);
                cell.setCellValue(headStr);

                CellRangeAddress region = new CellRangeAddress(0, 0, columnIndex - 4, columnIndex - 1);
                sheet.addMergedRegion(region);
            } else {
                cell.setCellStyle(headStyle);
                cell.setCellValue(headStr);
            }
        }
    }*/

    /**
     * 设置模板标题头的字体
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
     * 根据标题头校验标题头
     * @param titles 标题模板列表
     * @param titleRow 传入的标题行
     * @param titleRow 传入的标题行
     * @return
     */
    public static Boolean checkImportExcelTitle(List<ExcelTitle> titles, Row titleRow, Map<String, Integer> titleMap) {
        if (titles == null || titles.size() == 0 || titleRow == null || titleRow.getLastCellNum() == 0 || titleRow.getLastCellNum() < titles.size()) {
            return false;
        }
        for (int i = 0; i < titleRow.getLastCellNum(); i++) {
            Cell curCell = titleRow.getCell(i);
            if (curCell == null || curCell.getCellComment() == null || curCell.getCellComment().getString() == null || StringUtils.isBlank(curCell.getCellComment().getString().getString())) {
                continue;
            }
            for (ExcelTitle title : titles) {
                if (title.getComment().equals(curCell.getCellComment().getString().getString())) {
                    titleMap.put(curCell.getCellComment().getString().getString(), i);
                }
            }
        }
        if (titleMap.size() != titles.size()) {
            return false;
        }
        return true;
    }

    /**
     * 创建目录
     * @param folderPath
     */
    public static void createFolder(String folderPath) {
        File dir = new File(folderPath);
        if (!dir.exists()) {
            boolean mkFlag = dir.mkdirs();
            if (!mkFlag) {
                throw new OrganizationException(OrganizationErrorEnum.EXCEL_FILE_CREATE_ERROR);
            }
        }
    }

    /**
     * 创建Excel错误信息文件保存本地
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
     * @param workbook
     * @param id
     * @return
     * @throws IOException
     */
    public static String createExportFile(Workbook workbook, String fileName, Long id) throws IOException  {
        createFolder(ExcelUtils.EXCEL_FILE_EXPORT_PATH);
        String filePath = EXCEL_FILE_EXPORT_PATH + id + "_" + fileName;
        FileOutputStream fileOutputStream = new FileOutputStream(filePath);
        workbook.write(fileOutputStream);
        //workbook.close();
        fileOutputStream.flush();
        fileOutputStream.close();
        return filePath;
    }

    /**
     * 创建标题
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
     * 创建标题和标题的批注
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
                    throw new OrganizationException(OrganizationErrorEnum.EXCEL_FILE_CREATE_ERROR);
                }
            } catch (IOException e) {
                logger.error("创建文件异常", e);
                throw new OrganizationException(OrganizationErrorEnum.EXCEL_FILE_CREATE_ERROR);
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
