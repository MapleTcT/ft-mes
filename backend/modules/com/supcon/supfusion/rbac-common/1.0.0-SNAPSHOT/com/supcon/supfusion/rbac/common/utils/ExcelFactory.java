package com.supcon.supfusion.rbac.common.utils;

import com.supcon.supfusion.rbac.common.exception.ExcelErrorEnum;
import com.supcon.supfusion.rbac.common.exception.ExcelException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class ExcelFactory {

    private final Map<String,String> alias;

    public ExcelFactory(Map<String, String> alias) {
        this.alias = alias;
    }

    /**
     * @description: 
     * @param: header 表头
     * @param: data 表数据
     * @return: org.apache.poi.ss.usermodel.Workbook
     * @author: 袁阳
     * @date: 2020/6/24
     */
    public Workbook createExcel(List<String> header,List<Map<String,Object>> data){
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet();
        //写入表头数据
        Row rowHeader = sheet.createRow(0);
        for (int i = 0; i < header.size(); i++) {
            Cell cell = rowHeader.createCell(i);
            cell.setCellValue(header.get(i));
        }
        //写入数据
        for (int i = 0; i < data.size(); i++) {
            Row row = sheet.createRow(i + 1);
            Map<String, Object> map = data.get(i);
            for (int j = 0; j < header.size(); j++) {
                Cell cell = row.createCell(j);
                Object o = map.get(alias.get(header.get(j)));
                if (o instanceof Double){
                    cell.setCellValue((double) o);
                } else if (o instanceof Date){
                    cell.setCellValue((Date) o);
                } else if (o instanceof LocalDateTime){
                    cell.setCellValue((LocalDateTime) o);
                } else if (o instanceof LocalDate){
                    cell.setCellValue((LocalDate) o);
                } else if (o instanceof Calendar){
                    cell.setCellValue((Calendar) o);
                } else if (o instanceof String){
                    cell.setCellValue((String) o);
                } else if (o instanceof RichTextString){
                    cell.setCellValue((RichTextString) o);
                } else if (o instanceof Long){
                    cell.setCellValue((long) o);
                }

            }
        }
        return workbook;
    }
    /**
     * @description: 创建导入模板
     * @param: header 表头
     * @param: data 表数据
     * @return: org.apache.poi.ss.usermodel.Workbook
     * @author: 袁阳
     * @date: 2020/6/24
     */
    public Workbook createExcel(List<String> header){
        Workbook workbook = new XSSFWorkbook();
        CreationHelper helper = workbook.getCreationHelper();
        ClientAnchor anchor = helper.createClientAnchor();
        Sheet sheet = workbook.createSheet();
        Drawing draw = sheet.createDrawingPatriarch();
        //写入表头数据
        Row rowHeader = sheet.createRow(0);
        for (int i = 0; i < header.size(); i++) {
            Cell cell = rowHeader.createCell(i);
            anchor.setCol1(i);
            anchor.setRow1(0);
            Comment comment = draw.createCellComment(anchor);
            comment.setString(helper.createRichTextString(alias.get(header.get(i))));
            cell.setCellValue(header.get(i));
            cell.setCellComment(comment);
        }
        return workbook;
    }

    /**
     * @description: 文件导出
     * @param: workbook 导出的excel
     * @param: fileName 导出文件名
     * @param: filePath 导出文件路径
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/24
     */
    public void output(InputStream is,String fileName) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        HttpServletRequest request = requestAttributes.getRequest();
        try {
            //下面三行是关键代码，处理乱码问题
            response.setContentType("application/x-download");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + encode(fileName,request));
            ServletOutputStream outputStream = response.getOutputStream();
            // inputStream：读文件，前提是这个文件必须存在，要不就会报错
            byte[] b = new byte[4096];
            int size = is.read(b);
            while (size > 0) {
                outputStream.write(b, 0, size);
                size = is.read(b);
            }
            outputStream.close();
            is.close();
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }
    /**
     * @description: 临时文件创建
     * @param: workbook 导出的excel
     * @param: fileName 导出文件名
     * @return: void
     * @author: 袁阳
     * @date: 2020/6/24
     */
    public void createTempFile(Workbook workbook,String filePath) {
        try {
            File file = new File(filePath);
            FileOutputStream fo = new FileOutputStream(file);
            // inputStream：读文件，前提是这个文件必须存在，要不就会报错
            workbook.write(fo);
            fo.close();
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }

    /**
     * @description: 读取excel
     * @param: inputStream
     * @return: sheets -> sheetData -> rowData
     * @author: 袁阳
     * @date: 2020/6/29
     */
    public List<List<Map<String, String>>> readExcel(InputStream inputStream) throws IOException {
        Workbook workbook = new XSSFWorkbook(inputStream);
        ExcelFactory excelFactory = new ExcelFactory(alias);
        List<List<Map<String, String>>> data = new ArrayList<>();
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            List<Map<String, String>> sheetData = new ArrayList<>();
            Sheet sheet = workbook.getSheetAt(i);
            // 校验sheet是否合法
            if (sheet != null) {
                // 获取表头
                int firstRowNum = sheet.getFirstRowNum();
                Row firstRow = sheet.getRow(firstRowNum);
                List<String> headers = excelFactory.parseFirstRow(firstRow);
                //如果没有数据 则抛异常
                if (ObjectUtils.isEmpty(firstRow)){
                    throw new ExcelException(ExcelErrorEnum.EMPTY_DATA);
                }
                // 解析每一行的数据，构造数据对象
                for (int j = firstRowNum + 1; j < sheet.getLastRowNum(); j++) {
                    Map<String, String> row2Map = excelFactory.parseRow2Map(sheet.getRow(j), headers);
                    sheetData.add(row2Map);
                }
                data.add(sheetData);
            }
        }
        return data;
    }

    private List<String> parseFirstRow(Row row){
        short firstCellNum = row.getFirstCellNum();
        short lastCellNum = row.getLastCellNum();
        List<String> header = new ArrayList<>();
        for (int i = firstCellNum; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            header.add(cell.getStringCellValue());
        }
        return header;
    }

    private Map<String,String> parseRow2Map(Row row,List<String> header){
        short firstCellNum = row.getFirstCellNum();
        short lastCellNum = row.getLastCellNum();
        Map<String,String> map = new HashMap<>();
        for (int i = firstCellNum; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            map.put(header.get(i),cell.getStringCellValue());
        }
        return map;
    }

    /**
     * 根据浏览器的不同，重新对文件名进行编码
     *
     * @param fileName
     * @param request
     * @return
     * @throws UnsupportedEncodingException
     * @throws Exception
     */
    public String encode(String fileName, HttpServletRequest request) throws UnsupportedEncodingException{
        String agent = request.getHeader("User-Agent");
        String returnFileName = fileName;
        if (null != agent) {
            if (agent.contains("MSIE") || agent.contains("Trident/7.")|| agent.contains("Edge")) { // is IE browser
                returnFileName = StringUtils.replace(URLEncoder.encode(
                        fileName, "UTF-8"), "+", "%20");
                if (returnFileName.length() > 150) {// 处理超长文件名，IE的bug。
                    //%E9%87%8D%E5%91%BD%E5%90%8D
                    returnFileName = new String(fileName
                            .getBytes(getClientCharacterEncoding(request)),
                            "ISO8859_1");
                    returnFileName = StringUtils.replace(returnFileName, " ",
                            "%20");
                }
            } else if ((agent.indexOf("Firefox") > -1 || agent.indexOf("AppleWebKit") > -1) && agent.indexOf("Edge") == -1) { // is Firefox
                returnFileName = new String(fileName.getBytes(request
                        .getCharacterEncoding()), "ISO8859_1");
            }
        }
        return returnFileName;
    }

    /**
     * 根据 Accept-Language header 来判断客户端的编码
     *
     * <ul>
     * <li>若是zh-cn，则返回 GBK</li>
     * <li>若是zh-tw，则返回 BIG5</li>
     * <li>默认返回 ISO8859_1</li>
     * </ul>
     *
     * @param request
     * @return
     */
    public String getClientCharacterEncoding(HttpServletRequest request) {
        String language = request.getHeader("Accept-Language");
        if ("zh-cn".equalsIgnoreCase(language))
            return "GBK";
        else if ("zh-tw".equalsIgnoreCase(language))
            return "BIG5";
        return "ISO8859_1";
    }
}
