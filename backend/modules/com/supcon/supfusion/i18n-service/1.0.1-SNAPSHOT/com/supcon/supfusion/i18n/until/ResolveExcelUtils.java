package com.supcon.supfusion.i18n.until;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.ExcelUtils;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.module.registry.ModuleEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ResolveExcelUtils {

    public static Map resolverExcel(XSSFSheet sheetAt, List<String> moduleCodes, List<I18nLanguagePO> languageEntities, Integer valueLengthLimit, Integer keyLengthLimit) {
        //解析当前sheet
        int count = 0;
        Integer errorNum = 0;
        Integer allNum = 0;
        List<String> language_code = new ArrayList<>();
        Map paramMap = new HashMap();
        Map<Integer, String> i18n_key_errorMap = new LinkedHashMap<>();
        Map<String, Integer> count_errorMap = new LinkedHashMap<>();
        Set<String> i18nModuleSet = new HashSet<>();
        List<I18nResourcePO> i18n_POs = new ArrayList<>();
        Map<Integer, Integer> i18n_value_errorMap = new LinkedHashMap<>();
        Set<String> i18nKeySet = new HashSet<>();
        //先判断表头是否符合模版
        for (Row row : sheetAt) {
            if (row.getRowNum() == 0) {
                judgeRowZero(row, languageEntities);
                count = getRowZero(language_code, count, row);
                break;
            }
        }
        //如果数据只有一行
        if (!(sheetAt.getLastRowNum() > 0)) {
            throw new I18nException(I18nErrorEnum.FILE_SHEET_NONE_ERROR);
        }
        //校验excel国际化key数据准确性
        List<String> in18nKeys = new ArrayList<>();
        for (int i = 0; i < sheetAt.getLastRowNum() + 1; i++) {
            //如果国际化key这一列中 其中有一行是空的 报错
            if (sheetAt.getRow(i) == null) {
            	break;
            }
        	String[] rowValues = new String[count];
        	boolean isEndRow = true;
        	for (int index = 0; index < count; index ++) {
        		rowValues[index] = ExcelUtils.getCellValueByCell(sheetAt.getRow(i).getCell(index));
        		if (!StringUtils.isEmpty(rowValues[index])) {
        			isEndRow = false;
        		}
        	}
        	if (isEndRow) {
        		break;
        	} else if (StringUtils.isEmpty(rowValues[0])) {
        		throw new I18nException(I18nErrorEnum.XLSX_UPLOAD_KEY_BLANK_ERROR);
        	}
        	if (in18nKeys.contains(rowValues[0])) {
                //如果国际化key这一列中 有重复的国际化key 报错
                throw new I18nException(I18nErrorEnum.XLSX_UPLOAD_KEY_REPEAT_ERROR);
            }
            in18nKeys.add(rowValues[0]);
        }
        for (Row row : sheetAt) {
            if (row.getRowNum() > 0 && (row.getCell(0) == null ||
                    ExcelUtils.getCellValueByCell(row.getCell(0))
                            .equals(Constants.STR_NO_SPACE))) {
                //没有key了 直接跳出循环 //没有跳空格继续导入下一行的功能
                break;
            }
            //处理第一行的数据
            //处理 非第一行的所有数据 //国际化key不为null
            if (row.getRowNum() > 0 && row.getCell(0) != null) {
                allNum++;
                //国际化key不为空字符串
                if (!ExcelUtils.getCellValueByCell(row.getCell(0)).equals(Constants.STR_NO_SPACE)) {
                    //国际化key 长度校验
                    String i18n_key1 = ExcelUtils.getCellValueByCell(row.getCell(0));
                    if (i18n_key1.length() < keyLengthLimit) {
                        //国际化key 中的特殊字符校验
                        if (i18n_key1.matches(Constants.I18N_KEY_REGEX)) {
                            if (i18n_key1.contains(Constants.STR_POINT)) {//国际化key中包含点 可以切分出模块code
                                String module_Code = i18n_key1.substring(Constants.ZERO_INT, i18n_key1.indexOf(Constants.STR_POINT, Constants.ONE_INT));
                                if (moduleCodes.contains(module_Code)) {
                                    paramMap = judgeCellValue(count, errorNum, row, module_Code, i18n_value_errorMap,
                                            valueLengthLimit, i18nKeySet, i18nModuleSet, i18n_POs, language_code);
                                } else {//模块名 没有 使用默认模块名
                                    paramMap = judgeCellValue(count, errorNum, row, ModuleEnum.DEFAULT.getModuleId(),
                                            i18n_value_errorMap, valueLengthLimit, i18nKeySet, i18nModuleSet, i18n_POs, language_code);
                                }
                            } else {//默认属于 系统国际化资源
                                paramMap = judgeCellValue(count, errorNum, row, ModuleEnum.DEFAULT.getModuleId(), i18n_value_errorMap,
                                        valueLengthLimit, i18nKeySet, i18nModuleSet, i18n_POs, language_code);
                            }
                        } else {
                            errorNum++;
                            //校验是否有 英文字母 数字 下划线 点 之外的其他字符
                            i18n_key_errorMap.put(row.getRowNum(), I18nErrorEnum.I18N_KEY_ERROR.getMessage());
                        }
                    } else {//国际化key长度超过255
                        errorNum++;
                        i18n_key_errorMap.put(row.getRowNum(), I18nErrorEnum.I18N_KEY_LENGTH_ERROR.getMessage());
                    }
                }
            }
        }
        Set<Integer> st = new HashSet<>();
        if (i18n_value_errorMap != null && i18n_value_errorMap.size() > 0) {
            i18n_value_errorMap.forEach((k, v) -> {
                st.add(k);
            });
        }
        errorNum = errorNum + st.size();
        count_errorMap.put(Constants.STR_ERROR_NUM, errorNum);
        count_errorMap.put(Constants.STR_ALL_NUM, allNum);
        // TODO
        count_errorMap.put(Constants.ADD_NUM, 0);
        count_errorMap.put(Constants.UPDATE_NUM, 0);
        paramMap.put(Constants.COUNT_ERROR_MAP, count_errorMap);
        paramMap.put(Constants.I18N_KEY_ERROR_MAP, i18n_key_errorMap);
        paramMap.put(Constants.COUNT, count);
        paramMap.put(Constants.STR_ERROR_NUM, errorNum);
        paramMap.put(Constants.SHEET_AT, sheetAt);
        log.info("sheet 解析完成 sheetName:" + sheetAt.getSheetName() + "LastRowNum:" + sheetAt.getLastRowNum());
        return paramMap;
    }


    private static void judgeRowZero(Row row, List<I18nLanguagePO> languageEntities) {
        List<String> dbLanguages = new ArrayList<>();
        for (I18nLanguagePO i18nLanguagePO : languageEntities) {
            dbLanguages.add(i18nLanguagePO.getLanguCode());
        }
        int cellNUm = row.getLastCellNum();
        Boolean is = true;
        //动态校验表头是否 都是数据库中的语言
        if ((cellNUm - 1) != dbLanguages.size()) {
            is = false;
        } else {
            for (int i = 0; i < cellNUm; i++) {
                //String cellValue =   row.getCell(i).getStringCellValue();
                if (i == 0 && !ExcelUtils.getCellValueByCell(row.getCell(i)).equals(Constants.I18N_KEY_ZHCN)) {
                    is = false;
                    break;
                } else if (i > 0 && !dbLanguages.contains(ExcelUtils.getCellValueByCell(row.getCell(i)))) {
                    is = false;
                    break;
                }
            }
        }
        if (!is) {
            throw new I18nException(I18nErrorEnum.FILE_HEAD_RESOLVER_ERROR);
        }
    }

    private static int getRowZero(List<String> language_code, int count, Row row) {
        if (row.getRowNum() == 0) {
            //获取一共多少列
            for (int i = 0; i < 20; i++) {
                if (row.getCell(i) != null) {
                    if (!StringUtils.isEmpty(ExcelUtils.getCellValueByCell(row.getCell(i)))) {
                        count++;
                    }
                }
            }
            //将第一行的所有表头 存入 language_code
            for (int i = 1; i < count; i++) {
                if (row.getCell(i) != null) {
                    if (row.getCell(i) != null && !StringUtils.isEmpty(ExcelUtils.getCellValueByCell(row.getCell(i)))) {
                        //row.getCell(i).setCellType(CellType.STRING);
                        language_code.add(ExcelUtils.getCellValueByCell(row.getCell(i)));
                    } else {
                        break;
                    }
                }
            }
        }
        return count;
    }

    private static Map judgeCellValue(int count, Integer errorNum, Row row, String module_Code, Map i18n_value_errorMap,
                                      Integer valueLength, Set i18nKeySet, Set i18nModuleSet, List i18n_POs, List<String> language_code) {
        Boolean isRight = true;
        Map paramMap = new HashMap();
        //先校验每个value值长度是否超过限制
        for (int f = 1; f < count; f++) {
            Cell c = row.getCell(f, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (c != null) {
                if (ExcelUtils.getCellValueByCell(row.getCell(f)).length() > valueLength) {
                    isRight = false;
                    i18n_value_errorMap.put(row.getRowNum(), f);
                }
            }
        }
        Boolean isNotAllCellNull = true;
        Integer nullCellCount = 0;
        //先校验每个value值长度是否超过限制
        for (int f = 1; f < count; f++) {
            Cell c = row.getCell(f, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (c == null) {
                nullCellCount = nullCellCount + 1;
            }
        }
        if (nullCellCount == count - 1) {
            isNotAllCellNull = false;
            errorNum++;
            i18n_value_errorMap.put(row.getRowNum(), 0);
        }
        if (isRight && isNotAllCellNull) {//每一种语言的value值长度都没有问题
            //遍历每一行中的每一个单元格 excel单元格中的数据存入对象
            for (int k = 1; k < count; k++) {
                paramMap = foreachEveryCell(row, module_Code, k, i18nKeySet, i18nModuleSet, i18n_POs, language_code);
            }
        } else {
            errorNum++;
        }
        paramMap.put(Constants.STR_ERROR_NUM, errorNum);
        paramMap.put(Constants.I18N_VALUE_ERROR_MAP, i18n_value_errorMap);
        return paramMap;
    }

    private static Map foreachEveryCell(Row row, String module_Code, int f, Set i18nKeySet, Set i18nModuleSet,
                                        List i18n_POs, List<String> language_code) {
        i18nKeySet.add(String.valueOf(row.getCell(0)));
        i18nModuleSet.add(module_Code);
        if (row.getCell(f) != null) {
            //如果单元格有数据
            I18nResourcePO i18n_po = new I18nResourcePO();
            //将KEY 先存入对象的key字段
            i18n_po.setI18nKey(String.valueOf(row.getCell(0)));
            i18n_po.setI18nValue(ExcelUtils.getCellValueByCell(row.getCell(f)));
            i18n_po.setLanguCode(language_code.get(f - 1));
            i18n_po.setModuleCode(module_Code);
            i18n_po.setCreator(Constants.TWO_STR);
            i18n_po.setValid(Constants.ONE_STR);
            i18n_POs.add(i18n_po);
        }
        Map paramMap = new HashMap();
        paramMap.put(Constants.I18N_KEY_SET, i18nKeySet);
        paramMap.put(Constants.I18N_MODULE_SET, i18nModuleSet);
        paramMap.put(Constants.I18N_POS, i18n_POs);
        return paramMap;
    }

    //按照模块 查询数据库和excel 对比  按模块删除 在按模块插入数据库
    public static List<I18nResourcePO> execExcelMapAndDBMap(List<I18nResourcePO> excelResources, List<I18nResourcePO> dbResources) {
    	String tenantId = TenantUtil.getTenantId();
    	// 租户新增的国际化key
    	Set<String> tenantI18nKeys = new HashSet<>();
    	List<I18nResourcePO> tenantResources = new LinkedList<>();
    	// 先找出excel与数据库中不同的数据, 将这些数据作为租户数据
    	for (I18nResourcePO excelResource : excelResources) {
    		if (!dbResources.contains(excelResource)) {
    			tenantI18nKeys.add(excelResource.getI18nKey());
    		}
    	}
    	Map<String, List<I18nResourcePO>> excelResourceGroups = excelResources.stream().collect(Collectors.groupingBy(I18nResourcePO::getI18nKey));
    	// 添加租户的国际化资源
    	for (String tenantI18nKey : tenantI18nKeys) {
    		List<I18nResourcePO> oneKeyResources = excelResourceGroups.get(tenantI18nKey);
    		if (oneKeyResources != null) {
    			for (I18nResourcePO oneKeyResource : oneKeyResources) {
    				oneKeyResource.setTenantId(tenantId);
    				tenantResources.add(oneKeyResource);
    			}
    		}
    	}
    	return tenantResources;
    }
}

