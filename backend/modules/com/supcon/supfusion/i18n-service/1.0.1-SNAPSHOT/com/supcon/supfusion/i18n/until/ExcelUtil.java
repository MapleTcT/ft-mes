package com.supcon.supfusion.i18n.until;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.vo.I18nResourceVO;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelUtil {

	public static SXSSFWorkbook createExcelWorkbook(String excelPath) {
		try (FileInputStream fileInputStream = new FileInputStream(excelPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
             SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(workbook, 100);) {
			return sxssfWorkbook;
		} catch (IOException e) {
			log.error("创建excel Workbook 失败", e);
			return null;
		}
	}
	
	public static int appendToExcel(SXSSFWorkbook sxssfWorkbook, List<I18nLanguagePO> languageEntities, Collection<I18nResourceVO> i18nResourceList, int startRow) {
		//获取第一个Sheet页
        SXSSFSheet sheet = sxssfWorkbook.getSheetAt(0);
        //写入数据的地方
        int index = startRow;
        for (I18nResourceVO i18nResource : i18nResourceList) {
            //每一个国际化key对应一行 第一行已经有表头了
            SXSSFRow row = sheet.createRow(index++);
            //该行第一单元格放入国际化key
            row.createCell(0).setCellValue(i18nResource.getI18nKey());
            Map<String, String> map = i18nResource.getI18nValues();
            //其他单元格按照语言对应放入国际化value
            for (Object language : map.keySet()) {
                for (int f = 0; f < languageEntities.size(); f++) {
                    if (language.toString().equals(languageEntities.get(f).getLanguCode())) {
                        if ((map.get(language) != null)) {
                            row.createCell(f + 1).setCellValue(map.get(language).toString());
                        } else {
                            row.createCell(f + 1).setCellValue(Constants.STR_NO_SPACE);
                        }
                    }
                }
            }
        }
        return index;
	}
	
	
	public static boolean exportExcel(String excelPath, List<I18nLanguagePO> languageEntities, Collection<I18nResourceVO> i18nResourceList) {
		try (FileInputStream fileInputStream = new FileInputStream(excelPath);
             XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
             SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(workbook, 100);
     		 BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(excelPath));) {
	            //获取第一个Sheet页
	            SXSSFSheet sheet = sxssfWorkbook.getSheetAt(0);
	            //写入数据的地方
	            int index = 0;
	            for (I18nResourceVO i18nResource : i18nResourceList) {
	                //每一个国际化key对应一行 第一行已经有表头了 这里i+1
	                SXSSFRow row = sheet.createRow(++index);
	                //该行第一单元格放入国际化key
	                row.createCell(0).setCellValue(i18nResource.getI18nKey());
	                Map<String, String> map = i18nResource.getI18nValues();
	                //其他单元格按照语言对应放入国际化value
	                for (Object language : map.keySet()) {
	                    for (int f = 0; f < languageEntities.size(); f++) {
	                        if (language.toString().equals(languageEntities.get(f).getLanguCode())) {
	                            if ((map.get(language) != null)) {
	                                row.createCell(f + 1).setCellValue(map.get(language).toString());
	                            } else {
	                                row.createCell(f + 1).setCellValue(Constants.STR_NO_SPACE);
	                            }
	                        }
	                    }
	                }
	            }
	            //写出到文件
                sxssfWorkbook.write(outputStream);
                // 释放workbook所占用的所有windows资源
                outputStream.flush();
                sxssfWorkbook.dispose();
                return true;
	        } catch (Exception e) {
	            log.error("导出国际化数据失败", e);
	            return false;
	        }
	}
	
	public void appendExcel(Collection<I18nResourceVO> i18nResourceList, List<I18nLanguagePO> languageEntities) {
		
	}
}
