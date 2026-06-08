package com.supcon.supfusion.i18n.service.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.supcon.supfusion.framework.boot.scaffold.dbp.DataSourceConnectionProperties;
import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.dto.I18nQueryDTO;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.dao.ExcelDao;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nLanguageDao;
import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.I18nTokenDao;
import com.supcon.supfusion.i18n.dao.I18nVersionDao;
import com.supcon.supfusion.i18n.dao.po.ExcelPO;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.i18n.dao.po.I18nVersionPO;
import com.supcon.supfusion.i18n.dao.po.ModulePO;
import com.supcon.supfusion.i18n.dao.vo.ExcelVO;
import com.supcon.supfusion.i18n.dao.vo.I18nResourceVO;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import com.supcon.supfusion.i18n.service.I18nResourceService;
import com.supcon.supfusion.i18n.service.OperateDBService;
import com.supcon.supfusion.i18n.until.CachedResourceBundle;
import com.supcon.supfusion.i18n.until.ExcelUtil;
import com.supcon.supfusion.i18n.until.ResolveExcelUtils;
import com.supcon.supfusion.i18n.until.UploadingUtil;
import com.supcon.supfusion.module.registry.ModuleEnum;
import com.supcon.supfusion.module.registry.dto.ModuleDTO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Service
@Slf4j
public class I18nResourceServiceImpl extends ServiceImpl<I18nResourceDao, I18nResourcePO> implements I18nResourceService {

    @Autowired
    private I18nResourceDao i18nResourceDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private I18nLanguageDao i18nLanguageDao;
    @Autowired
    private I18nManagerService i18nManagerService;
    @Autowired
    private ExcelDao excelDao;
    @Autowired
    private I18nTokenDao i18nTokenDao;
    @Autowired
    private I18nVersionDao i18nVersionDao;
    @Autowired
    private I18nProperties i18nProperties;
    @Autowired
    private OperateDBService operateDBService;
    @Autowired
    private CachedResourceBundle cachedResourceBundle;
    @Autowired
    private I18nResourceService i18nResourceService;

    @Autowired
    DataSourceConnectionProperties dataSourceConnectionProperties;

    @Override
    public PageResult<ModulePO> getAllModel() {
        List<ModuleDTO> moduleDTOS = (List<ModuleDTO>) i18nManagerService.queryModules();
        List<ModulePO> modEntities2 = new ArrayList<>();
        for (int i = 0; i < moduleDTOS.size(); i++) {
            ModulePO modulePO = new ModulePO();
            modulePO.setModuleCode(moduleDTOS.get(i).getModuleId());
            modulePO.setModuleName(moduleDTOS.get(i).getModuleName());
            modEntities2.add(modulePO);
        }
        return new PageResult<>(modEntities2, Constants.ZERO_INT, Constants.ZERO_INT, Constants.ZERO_INT);
    }

    @Override
    public PageResult<I18nResourceVO> queryByKeyOrValue(String fuzzyI18nKey, String fuzzyI18nValue, int pageNo, int pageSize) {
        String tenantId = TenantUtil.getTenantId();
        List<String> languages = getEnableLanguage(tenantId);
        List<I18nResourcePO> tenantI18nResources = null;
        if (Constants.DEFAULT_TENANT.equals(tenantId)) {
            return searchElementWithDefaultTenant(fuzzyI18nKey, fuzzyI18nValue, languages, pageNo, pageSize);
        } else {
            // 查出租户的i18nKey
            tenantI18nResources = i18nResourceDao.selectList(new QueryWrapper<I18nResourcePO>().lambda()
            		.in(I18nResourcePO::getLanguCode, languages)
                    .eq(I18nResourcePO::getTenantId, tenantId));
        }
        if (tenantI18nResources.isEmpty()) {
            return searchElementWithDefaultTenant(fuzzyI18nKey, fuzzyI18nValue, languages, pageNo, pageSize);
        }
        Set<String> tenantI18nKeys = tenantI18nResources.stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        return searchElementWithMultipleTenant(fuzzyI18nKey, fuzzyI18nValue, tenantI18nKeys, languages, pageNo, pageSize);
    }

    private PageResult<I18nResourceVO> searchElementWithDefaultTenant(String fuzzyI18nKey, String fuzzyI18nValue, List<String> languages, int pageNo, int pageSize) {
    	String sql = buildElementI18nQuerySql(fuzzyI18nKey, fuzzyI18nValue, Constants.DEFAULT_TENANT);

        String dbType = dataSourceConnectionProperties.getSystem().getDbType();
        LambdaQueryWrapper<I18nResourcePO> queryWrapper = null;
        if (dbType.equals("sqlserver")) {
            queryWrapper = new QueryWrapper<I18nResourcePO>().select("i18n_key, modify_time").lambda()
                    .inSql(I18nResourcePO::getI18nKey, sql)
                    .in(I18nResourcePO::getLanguCode, languages)
                    .groupBy(I18nResourcePO::getI18nKey, I18nResourcePO::getModifyTime);
        } else {
            queryWrapper = new QueryWrapper<I18nResourcePO>().select("distinct i18n_key, modify_time").lambda()
                    .inSql(I18nResourcePO::getI18nKey, sql)
                    .in(I18nResourcePO::getLanguCode, languages)
                    .groupBy(I18nResourcePO::getI18nKey, I18nResourcePO::getModifyTime)
                    .orderByDesc(I18nResourcePO::getModifyTime);
        }

        if (log.isDebugEnabled()) {
        	log.debug("组件查询国际化列表SQL: {}", queryWrapper.getTargetSql());
        }
        // 分页找出符合条件的国际化key
        Page<I18nResourcePO> pageResult = i18nResourceDao.selectPage(new Page<I18nResourcePO>(pageNo, pageSize), queryWrapper);
        Set<String> i18nKeys = pageResult.getRecords().stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        if (i18nKeys.isEmpty()) {
        	return new PageResult<>(new ArrayList<>(1), 0, pageSize, pageNo);
        }
        // 根据国际化key查询对应的所有语言资源
        queryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
        		.in(I18nResourcePO::getI18nKey, i18nKeys)
        		.eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT);
        if (!dbType.equals("sqlserver")) {
            queryWrapper.orderByDesc(I18nResourcePO::getModifyTime);
        }
        List<I18nResourcePO> fusionResources = i18nResourceDao.selectList(queryWrapper);
        Map<String, I18nResourceVO> resultMap = transfer(fusionResources);
        return new PageResult<>(resultMap.values(), pageResult.getTotal(), pageSize, pageNo);
    }

    private PageResult<I18nResourceVO> searchElementWithMultipleTenant(String fuzzyI18nKey, String fuzzyI18nValue, Set<String> tenantI18nKeys,
    		 List<String> languages, int pageNo, int pageSize) {
        String tenantId = TenantUtil.getTenantId();
        // 组装根据国际化值查询sql 如果租户数据过大, 可能会导致sql过长报错
        String tenantQuerySql = buildTenantElementI18nQuerySql(fuzzyI18nKey, fuzzyI18nValue, tenantId, tenantI18nKeys);
        LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().select("distinct i18n_key, modify_time").lambda()
                .in(I18nResourcePO::getLanguCode, languages)
                .inSql(I18nResourcePO::getI18nKey, tenantQuerySql)
                .orderByDesc(I18nResourcePO::getModifyTime);
        if (log.isDebugEnabled()) {
        	log.debug("组件查询国际化列表SQL: {}", queryWrapper.getTargetSql());
        }
        // 先分页查询key
        Page<I18nResourcePO> pageResult = i18nResourceDao.selectPage(new Page<>(pageNo, pageSize), queryWrapper);
        // 查询满足要求的国际化
        Set<String> i18nKeys = pageResult.getRecords().stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        if (i18nKeys.isEmpty()) {
        	return new PageResult<>(new ArrayList<>(1), 0, pageSize, pageNo);
        }
        // 根据国际化key查询最终结果
        LambdaQueryWrapper<I18nResourcePO> i18nValueQueryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                .in(I18nResourcePO::getI18nKey, i18nKeys)
                .in(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT, tenantId)
        		.orderByDesc(I18nResourcePO::getModifyTime);
        List<I18nResourcePO> fusionResources = i18nResourceDao.selectList(i18nValueQueryWrapper);
        // PO转VO
        Map<String, I18nResourceVO> resultMap = transfer(fusionResources);
        // 页面的一条数据对应数据库n条数据, 其中n代表启用语言的个数
        return new PageResult<>(resultMap.values(), pageResult.getTotal(), pageSize, pageNo);
    }

    /**
     * 假设所有查询条件均不为空, 最终组装的SQL如下:
     * SELECT t1.i18n_key FROM supfusion_i18n_resource t1 
				WHERE t1.tenant_id = 'dt' 
				AND t1.i18n_key LIKE '%key%' 
				AND t1.i18n_value LIKE '%value%' 
				GROUP BY t1.i18n_key
     */
    private String buildElementI18nQuerySql(String fuzzyI18nKey, String fuzzyI18nValue, String tenantId) {
    	StringBuilder appender = new StringBuilder("SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id = '");
    	appender.append(tenantId).append("' ");
    	if (StringUtils.isNotEmpty(fuzzyI18nKey)) {
    		appender.append("AND t1.i18n_key LIKE '%").append(fuzzyI18nKey).append("%' ");
    	}
    	if (StringUtils.isNotEmpty(fuzzyI18nValue)) {
    		appender.append("AND t1.i18n_value LIKE '%").append(fuzzyI18nValue).append("%' ");
    	}
    	appender.append("GROUP BY t1.i18n_key");
    	return appender.toString();
    }
    
    /*
     * 假设所有查询条件均不为空, 最终组装的SQL如下:
     * SELECT i18n_key FROM (
			SELECT t1.i18n_key FROM supfusion_i18n_resource t1 
				WHERE t1.tenant_id = '' 
				AND t1.i18n_key LIKE '%key%' 
				AND t1.i18n_value LIKE '%value%' 
				AND t1.i18n_key IN ('#{tenant_i18n_keys}') 
				GROUP BY t1.i18n_key
			UNION
			SELECT t1.i18n_key FROM supfusion_i18n_resource t1 
				WHERE t1.tenant_id = 'dt' 
				AND t1.i18n_key LIKE '%key%' 
				AND t1.i18n_value LIKE '%value%' 
				AND t1.i18n_key NOT IN ('#{tenant_i18n_keys}') 
				GROUP BY t1.i18n_key
		) a 
     */
    private String buildTenantElementI18nQuerySql(String fuzzyI18nKey, String fuzzyI18nValue, String tenantId, Set<String> tenantI18nKeys) {
        StringBuilder appender = new StringBuilder("SELECT i18n_key FROM (SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id = '");
        appender.append(tenantId).append("' ");
        if (StringUtils.isNotEmpty(fuzzyI18nKey)) {
            appender.append(" AND t1.i18n_key LIKE '%").append(fuzzyI18nKey).append("%' ");
        }
        if (StringUtils.isNotEmpty(fuzzyI18nValue)) {
            appender.append(" AND t1.i18n_value LIKE '%").append(fuzzyI18nValue).append("%' ");
        }
        appender.append(" AND t1.i18n_key IN ('");
        int length = 0;
        for (String tenantI18nKey : tenantI18nKeys) {
            if (length++ > 0) {
                appender.append(",'");
            }
            appender.append(tenantI18nKey).append("'");
        }
        appender.append(") GROUP BY t1.i18n_key UNION ");
        appender.append("SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id = 'dt' ");
        if (StringUtils.isNotEmpty(fuzzyI18nKey)) {
            appender.append(" AND t1.i18n_key LIKE '%").append(fuzzyI18nKey).append("%' ");
        }
        if (StringUtils.isNotEmpty(fuzzyI18nValue)) {
            appender.append(" AND t1.i18n_value LIKE '%").append(fuzzyI18nValue).append("%' ");
        }
        appender.append(" AND t1.i18n_key NOT IN ('");
        length = 0;
        for (String tenantI18nKey : tenantI18nKeys) {
            if (length++ > 0) {
                appender.append(",'");
            }
            appender.append(tenantI18nKey).append("'");
        }
        appender.append(") GROUP BY t1.i18n_key ) a ");
        return appender.toString();
    }

    /**
     * 国际化资源列表查询-联想功能
     * i18nKey
     * i18nValue, language
     * 以上两对参数不会同时存在有值
     */
    @Override
    public PageResult<String> queryKeyOrValueByKeyOrValue(String i18nKey, String i18nValue, String language, int pageNo, int pageSize) {

        String dbType = dataSourceConnectionProperties.getSystem().getDbType();
        String tenantId = TenantUtil.getTenantId();
        List<String> resultList = new LinkedList<>();
        if (StringUtils.isNotEmpty(i18nKey)) {
        	List<String> languages = getEnableLanguage(tenantId);

        	LambdaQueryWrapper<I18nResourcePO> queryWrapper = null;
        	if (dbType.equals("sqlserver")) {
        	     queryWrapper = new QueryWrapper<I18nResourcePO>().select(" i18n_key, modify_time").lambda();
            } else {
                queryWrapper = new QueryWrapper<I18nResourcePO>().select("DISTINCT i18n_key, modify_time").lambda();
            }
            queryWrapper.in(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT, tenantId)
                    .in(I18nResourcePO::getLanguCode, languages)
                    .like(I18nResourcePO::getI18nKey, i18nKey);
            if (!dbType.equals("sqlserver")) {
                queryWrapper.orderByDesc(I18nResourcePO::getModifyTime);
            }
            Page<I18nResourcePO> pageResult = i18nResourceDao.selectPage(new Page<I18nResourcePO>(pageNo, pageSize), queryWrapper);
            for (I18nResourcePO i18nResource : pageResult.getRecords()) {
                resultList.add(i18nResource.getI18nKey());
            }
        } else {
            I18nQueryDTO queryDto = new I18nQueryDTO();
            Map<String, List<String>> queryMap = new HashMap<>();
            queryMap.put(language, Collections.singletonList(i18nValue));
            queryDto.setLanguageMap(queryMap);
            queryDto.setAssociate(true);
            queryDto.setTenantId(tenantId);
            PageResult<I18nResourceVO> pageResult = queryByMap(queryDto, new Pagination(0, pageSize, pageNo));
            for (I18nResourceVO i18nResource : pageResult.getList()) {
                resultList.add(i18nResource.getI18nValue());
            }
        }
        return new PageResult<>(resultList, 0, pageSize, pageNo);
    }

    @Override
    public List<I18nResourceVO> queryKeyExist(String key) {
        String moduleId = key.split(Constants.STR_POINT1)[0];
        boolean moduleExist = i18nManagerService.moduleExists(moduleId);
        if (!moduleExist) {
            moduleId = ModuleEnum.DEFAULT.getModuleId();
        }
        String tenantId = TenantUtil.getTenantId();
        String language = RpcContext.getContext().getLanguage().toString();
        List<I18nResourceVO> i18NResourceVOS = new ArrayList<>();
        KeyValuePairCollection cachedResource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, language);
        if (cachedResource != null) {
        	for (String i18nKey : cachedResource.getKvs().keySet()) {
        		if (i18nKey.equals(key)) {
        			i18NResourceVOS.add(new I18nResourceVO());
        			break;
        		}
        	}
        }
        return i18NResourceVOS;
    }

    /**
     * 前端国际化数据列表搜索
     * <p>
     * 目前支持3种语言, 3种语言在前端展示是一条数据, 在数据库中对应有3条数据, 查询如下:
     * 先分页查询国际化key, 根据key进行分组过滤, 根据keys再次查询国际化值
     * 最后将3条数据合并为一条返回
     * 
     * 在多租户环境下, 先查询租户数据国际化key, 如果租户数据不存在则只需要查系统数据, 否则需要将租户数据和系统数据做一次union
     * </p>
     */
    @Override
    public PageResult<I18nResourceVO> queryByMap(I18nQueryDTO queryDto, Pagination pagination) {
        List<I18nResourcePO> tenantI18nResources = null;
        List<String> languages = getEnableLanguage(queryDto.getTenantId());
        if (Constants.DEFAULT_TENANT.equals(queryDto.getTenantId())) {
            return searchWithDefaultTenant(queryDto, languages, pagination);
        } else {
            // 查出租户的i18nKey
            tenantI18nResources = i18nResourceDao.selectList(new QueryWrapper<I18nResourcePO>().lambda()
            		.in(I18nResourcePO::getLanguCode, languages)
                    .eq(I18nResourcePO::getTenantId, queryDto.getTenantId()));
        }
        if (tenantI18nResources == null || tenantI18nResources.isEmpty()) {
            return searchWithDefaultTenant(queryDto, languages, pagination);
        }
        Set<String> tenantI18nKeys = tenantI18nResources.stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        return searchWithMultipleTenant(queryDto, tenantI18nKeys, languages, pagination);
    }

    private PageResult<I18nResourceVO> searchWithMultipleTenant(I18nQueryDTO queryDto, Set<String> tenantI18nKeys, List<String> languages, Pagination pagination) {
        // 组装根据国际化值查询sql 如果租户数据过大, 可能会导致sql过长报错
        String tenantQuerySql = buildTenantQuerySql(queryDto, tenantI18nKeys, queryDto.getTenantId());
        LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().select("distinct i18n_key, modify_time").lambda()
        		.inSql(I18nResourcePO::getI18nKey, tenantQuerySql)
        		.in(I18nResourcePO::getLanguCode, languages)
        		.orderByDesc(I18nResourcePO::getModifyTime);
        if (log.isDebugEnabled()) {
            log.debug("多租户国际化列表查询SQL: {}", queryWrapper.getTargetSql());
        }
        // 先分页查询key
        Page<I18nResourcePO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        Page<I18nResourcePO> pageResult = i18nResourceDao.selectPage(page, queryWrapper);
        Set<String> i18nKeys = pageResult.getRecords().stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        if (i18nKeys.isEmpty()) {
        	return new PageResult<>(new ArrayList<>(1), 0, pagination.getPageSize(), pagination.getCurrent());
        }
        // 根据国际化key查询国际化值
        LambdaQueryWrapper<I18nResourcePO> i18nValueQueryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                .in(I18nResourcePO::getI18nKey, i18nKeys)
                .in(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT, queryDto.getTenantId());
        if (queryDto.isAssociate()) {
        	i18nValueQueryWrapper.in(I18nResourcePO::getLanguCode, queryDto.getLanguageMap().keySet());
        } else {
        	i18nValueQueryWrapper.in(I18nResourcePO::getLanguCode, languages);
        }
        i18nValueQueryWrapper.orderByDesc(I18nResourcePO::getModifyTime);
        List<I18nResourcePO> fusionResources = i18nResourceDao.selectList(i18nValueQueryWrapper);
        // PO转VO
        Map<String, I18nResourceVO> resultMap = transfer(fusionResources);
        return new PageResult<>(resultMap.values(), pageResult.getTotal(), pagination.getPageSize(), pagination.getCurrent());
    }

    private PageResult<I18nResourceVO> searchWithDefaultTenant(I18nQueryDTO queryDto, List<String> languages, Pagination pagination) {

        String dbType = dataSourceConnectionProperties.getSystem().getDbType();
        LambdaQueryWrapper<I18nResourcePO> queryWrapper = null;

        if (dbType.equals("sqlserver")) {
            queryWrapper = new QueryWrapper<I18nResourcePO>().select(" i18n_key, modify_time").lambda();
        } else {
            queryWrapper = new QueryWrapper<I18nResourcePO>().select(" distinct i18n_key, modify_time ").lambda();
        }
        // 设置查询条件
        //LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().select(" i18n_key, modify_time").lambda();
        // 组装查询sql
        String querySql = buildQuerySql(queryDto, Constants.DEFAULT_TENANT);
        queryWrapper.inSql(I18nResourcePO::getI18nKey, "select a.i18n_key from (" + querySql + " GROUP BY t1.i18n_key) a ");
        if (queryDto.isAssociate()) {
        	queryWrapper.in(I18nResourcePO::getLanguCode, queryDto.getLanguageMap().keySet());
        } else {
        	queryWrapper.in(I18nResourcePO::getLanguCode, languages);
        }
        if (log.isDebugEnabled()) {
        	log.debug("最终组装国际化列表查询SQL: {}", queryWrapper.getTargetSql());
        }
        // 先分页查询key
        Page<I18nResourcePO> page = new Page<>(pagination.getCurrent(), pagination.getPageSize());
        if (!dbType.equals("sqlserver")) {
            queryWrapper.groupBy(I18nResourcePO::getI18nKey).orderByDesc(I18nResourcePO::getModifyTime);
        } else {
            queryWrapper.groupBy(I18nResourcePO::getI18nKey, I18nResourcePO::getModifyTime);
        }
//        queryWrapper.orderByDesc(I18nResourcePO::getModifyTime);
        Page<I18nResourcePO> pageResult = i18nResourceDao.selectPage(page, queryWrapper);
        Set<String> i18nKeys = pageResult.getRecords().stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        if (i18nKeys.isEmpty()) {
        	return new PageResult<>(new ArrayList<>(1), 0, pagination.getPageSize(), pagination.getCurrent());
        }
        // 根据国际化key查询国际化值
        LambdaQueryWrapper<I18nResourcePO> i18nValueQueryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                .in(I18nResourcePO::getI18nKey, i18nKeys)
                .eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT);
        if (queryDto.isAssociate()) {
        	i18nValueQueryWrapper.in(I18nResourcePO::getLanguCode, queryDto.getLanguageMap().keySet());
        }
        i18nValueQueryWrapper .orderByDesc(I18nResourcePO::getModifyTime);
        List<I18nResourcePO> fusionResources = i18nResourceDao.selectList(i18nValueQueryWrapper);
        // PO转VO
        Map<String, I18nResourceVO> resultMap = transfer(fusionResources);
        // 页面的一条数据对应数据库n条数据, 其中n代表启用语言的个数
        return new PageResult<>(resultMap.values(), pageResult.getTotal(), pagination.getPageSize(), pagination.getCurrent());
    }

    private Map<String, I18nResourceVO> transfer(List<I18nResourcePO> resourcePos) {
    	// 保证转换之后的顺序和数据库查询的保持一致
        Map<String, I18nResourceVO> resultMap = new LinkedHashMap<>();
        for (I18nResourcePO resource : resourcePos) {
            I18nResourceVO i18nResourceVO = resultMap.get(resource.getI18nKey());
            if (i18nResourceVO == null) {
                resultMap.put(resource.getI18nKey(), buildI18nResourceVO(resource));
            } else {
                String i18nValue = i18nResourceVO.getI18nValues().get(resource.getLanguCode());
                // 只有当值不存在或者当前值不是租户数据才能覆盖
                if (i18nValue == null || !Constants.DEFAULT_TENANT.equals(resource.getTenantId())) {
                    i18nResourceVO.getI18nValues().put(resource.getLanguCode(), resource.getI18nValue());
                }
            }
        }
        return resultMap;
    }

    // 假设所有查询条件均不为空, 最终组装的SQL如下:
    /*
     * SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id = 'dt' AND  (t1.I18N_KEY LIKE '%%' or t1.I18N_KEY LIKE '%%') AND t1.I18N_KEY IN (
			SELECT i18n_key from (
					select t2.i18n_key from supfusion_i18n_resource t2 
					JOIN 
					supfusion_i18n_resource t3 on t2.i18n_key=t3.I18N_KEY and t3.tenant_id=t2.tenant_id
					JOIN 
					supfusion_i18n_resource t4 on t3.i18n_key=t4.I18N_KEY AND t4.tenant_id=t3.tenant_id
					WHERE t2.tenant_id='dt' 
					AND t2.langu_code='zh_CN' AND (t2.i18n_value LIKE '%%' or t2.i18n_value LIKE '%%')
					AND t3.langu_code='zh_HK' AND (t3.i18n_value LIKE '%%' or t3.i18n_value LIKE '%%')
					AND t4.langu_code='en_US' AND (t4.i18n_value LIKE '%%' or t4.i18n_value LIKE '%%')
			) b
		)
     */
    private String buildQuerySql(I18nQueryDTO queryDto, String tenantId) {
        StringBuilder sqlBuilder = new StringBuilder(" SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id='")
                .append(tenantId).append("' ");
        StringBuilder whereSqlBuilder = new StringBuilder();
        if (queryDto.getI18nKeys() != null) {
        	int count = 0;
        	for (String i18nKey : queryDto.getI18nKeys()) {
        		if (count++ == 0) {
        			sqlBuilder.append(" AND (t1.i18n_key LIKE '%").append(i18nKey).append("%' ");
        		} else {
        			sqlBuilder.append(" OR t1.i18n_key LIKE '%").append(i18nKey).append("%' ");
        		}
        	}
        	sqlBuilder.append(") ");
        }
        if (queryDto.getLanguageMap() != null) {
            sqlBuilder.append(" AND t1.i18n_key in (SELECT i18n_key FROM (");
            int tableCount = 0;
            for (Map.Entry<String, List<String>> langMap : queryDto.getLanguageMap().entrySet()) {
                if (++tableCount == 1) {
                    sqlBuilder.append("SELECT t").append(tableCount).append(".i18n_key FROM supfusion_i18n_resource t").append(tableCount).append(" ");
                    whereSqlBuilder.append(" WHERE t").append(tableCount).append(".tenant_id='").append(tenantId).append("' ");
                } else {
                    sqlBuilder.append(" JOIN supfusion_i18n_resource t").append(tableCount).append(" ON t").append(tableCount - 1).append(".i18n_key=t").append(tableCount).append(".i18n_key ");
                }
                whereSqlBuilder.append(" AND t").append(tableCount).append(".langu_code='").append(langMap.getKey()).append("' ");
                int valueCount = 0;
                for (String i18nValue : langMap.getValue()) {
                	if (valueCount++ == 0) {
                		whereSqlBuilder.append(" AND (t").append(tableCount).append(".i18n_value LIKE '%").append(i18nValue).append("%' ");
                	} else {
                		whereSqlBuilder.append(" OR t").append(tableCount).append(".i18n_value LIKE '%").append(i18nValue).append("%' ");
                	}
                }
                whereSqlBuilder.append(") ");
            }
            sqlBuilder.append(whereSqlBuilder.toString()).append(") b)");
        }
        return sqlBuilder.toString();
    }

    // 假设所有查询条件均不为空, 最终组装的SQL如下:
    /*
     * SELECT * FROM (
			SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id = '' AND  (t1.i18n_key LIKE '%%' OR t1.i18n_key LIKE '%%') AND t1.i18n_key IN (
				SELECT i18n_key from (
						select t2.i18n_key from supfusion_i18n_resource t2 
						JOIN 
						supfusion_i18n_resource t3 on t2.i18n_key=t3.I18N_KEY and t3.tenant_id=t2.tenant_id
						JOIN 
						supfusion_i18n_resource t4 on t3.i18n_key=t4.I18N_KEY AND t4.tenant_id=t3.tenant_id
						WHERE t2.tenant_id='' 
						AND t2.langu_code='zh_CN' AND (t2.i18n_value like '%%' OR t2.i18n_value like '%%')
						AND t3.langu_code='zh_HK' AND (t3.i18n_value like '%%' OR t3.i18n_value like '%%')
						AND t4.langu_code='en_US' AND (t4.i18n_value like '%%' OR t4.i18n_value like '%%')
				) b
			) AND t1.I18N_KEY IN ('#{}') GROUP BY t1.i18n_key
			UNION
			SELECT t1.i18n_key FROM supfusion_i18n_resource t1 WHERE t1.tenant_id = 'dt' AND  (t1.i18n_key LIKE '%%' OR t1.i18n_key LIKE '%%') AND t1.i18n_key IN (
				SELECT i18n_key from (
						select t2.i18n_key from supfusion_i18n_resource t2 
						JOIN 
						supfusion_i18n_resource t3 on t2.i18n_key=t3.I18N_KEY and t3.tenant_id=t2.tenant_id
						JOIN 
						supfusion_i18n_resource t4 on t3.i18n_key=t4.I18N_KEY AND t4.tenant_id=t3.tenant_id
						WHERE t2.tenant_id='dt' 
						AND t2.langu_code='zh_CN' AND (t2.i18n_value like '%%' OR t2.i18n_value like '%%')
						AND t3.langu_code='zh_HK' AND (t3.i18n_value like '%%' OR t3.i18n_value like '%%')
						AND t4.langu_code='en_US' AND (t4.i18n_value like '%%' OR t4.i18n_value like '%%')
				) b
			) AND t1.i18n_key NOT IN ('#{}') GROUP BY t1.i18n_key
		)  
     */
    private String buildTenantQuerySql(I18nQueryDTO queryDto, Set<String> tenantI18nKeys, String tenantId) {
        String tenantQuerySql = buildQuerySql(queryDto, tenantId);
        StringBuilder appender = new StringBuilder("SELECT * FROM (	").append(tenantQuerySql).append(" AND t1.i18n_key IN ('");
        int length = 0;
        for (String tenantI18nKey : tenantI18nKeys) {
            if (length++ > 0) {
                appender.append(",'");
            }
            appender.append(tenantI18nKey).append("'");
        }
        appender.append(") GROUP BY t1.i18n_key UNION ALL ");
        String systemQuerySql = buildQuerySql(queryDto, Constants.DEFAULT_TENANT);
        appender.append(systemQuerySql).append(" AND t1.i18n_key NOT IN ('");
        length = 0;
        for (String tenantI18nKey : tenantI18nKeys) {
            if (length++ > 0) {
                appender.append(",'");
            }
            appender.append(tenantI18nKey).append("'");
        }
        appender.append(" ) GROUP BY t1.i18n_key) t");
        return appender.toString();
    }

    private I18nResourceVO buildI18nResourceVO(I18nResourcePO resourcePo) {
        Map<String, String> langI18nValueMap = new HashMap<>(8);
        I18nResourceVO resourceVo = new I18nResourceVO();
        resourceVo.setI18nKey(resourcePo.getI18nKey());
        resourceVo.setI18nValue(resourcePo.getI18nValue());
        resourceVo.setLanguCode(resourcePo.getLanguCode());
        resourceVo.setModuleCode(resourcePo.getModuleCode());
        langI18nValueMap.put(resourcePo.getLanguCode(), resourcePo.getI18nValue());
        resourceVo.setI18nValues(langI18nValueMap);
        return resourceVo;
    }

    /*private Map getMap(Map<String, Object> whereMap, String[] i18nKeysArr, Map valuesMap, boolean downAll) {
        int pageSize = (Integer) whereMap.get(Constants.PAGE_SIZE);
        int pageNO = (Integer) whereMap.get(Constants.PAGE_NO);
        if (downAll) {
            whereMap.put(Constants.OFFSET, 0);
        }
        whereMap.put(Constants.TENANT_ID, RpcContext.getContext().getTenantId());
        List<String> i18nKeys = null;
        if (i18nKeysArr != null && i18nKeysArr.length > 0) {
            i18nKeys = new ArrayList<>();
            for (String v : i18nKeysArr) {
                String i18n_key = Constants.STR_NO_SPACE;
                char[] i18nKeyChar = v.toCharArray();
                //每一个key搜索条件针对不同数据库处理特殊字符
                i18n_key = StringUtil.getString(i18nKeyChar, i18n_key, i18nDataSourceConfig);
                if (i18nKeyChar != null && i18nKeyChar.length > 0) {
                    i18nKeys.add(i18n_key);
                }
            }
        }
        if (valuesMap != null && !valuesMap.isEmpty()) {
            for (Object ske : valuesMap.keySet()) {
                List<String> i18nValues = (List<String>) valuesMap.get(ske);
                List<String> i18nValues2 = new ArrayList<>();
                if (i18nValues != null && !i18nValues.isEmpty()) {
                    for (String v : i18nValues) {
                        String i18n_value = Constants.STR_NO_SPACE;
                        char[] i18nValueChar = v.toCharArray();
                        //每一个key搜索条件针对不同数据库处理特殊字符
                        i18n_value = StringUtil.getString(i18nValueChar, i18n_value, i18nDataSourceConfig);
                        if (i18nValueChar != null && i18nValueChar.length > 0) {
                            i18nValues2.add(i18n_value);
                        }
                    }
                    valuesMap.put(ske, i18nValues2);
                }
            }
        }
        List<I18nResourceVO> i18n_resource_list = new ArrayList<>();
        int count = 0;
        if (i18nKeys != null && !i18nKeys.isEmpty() && valuesMap != null) { //既有国际化key 也有国际化value 多个
            Set<String> I18nKeys = new HashSet<>();
            //切记返回数据的顺序问题 修改 新增完成之后 完全按这个接口查询的数据来反馈修改情况
            //如果是多国际化value条件模糊搜索
            for (String key : i18nKeys) {//所有循环的结果取并集
                List<String> i18n_keysSS = new ArrayList<>();
                boolean first = true;
                for (Object languCode : valuesMap.keySet()) { // 所有循环的结果 取交集
                    if (valuesMap.get(languCode) != null && !valuesMap.get(languCode).toString().equals(Constants.STR_NO_SPACE)) {
                        List<String> i18nValues = (List<String>) valuesMap.get(languCode);
                        whereMap.put(Constants.I18NKEY, key);
                        whereMap.put(Constants.LANGUCODE, languCode.toString());
                        whereMap.put(Constants.I18NVALUES, i18nValues);
                        List<String> i18n_keys = i18nResourceDao.selectByOneKeyAndValues(whereMap);
                        if (i18n_keys != null && !i18n_keys.isEmpty()) {
                            if (first) {//第一次查询 一种语言 有结果
                                first = false;
                                for (String s : i18n_keys) {
                                    i18n_keysSS.add(s);
                                }
                                i18n_keys.clear();
                            } else {
                                List<String> templist = MyFileUtils.getStr(i18n_keys, i18n_keysSS);
                                i18n_keysSS.clear();
                                if (templist != null && templist.size() > 0) {
                                    for (String s : templist) {
                                        i18n_keysSS.add(s);
                                    }
                                }
                            }
                        } else {//有一种语言的条件 没有查询出结果 交集一定为空 直接清空
                            i18n_keysSS.clear();
                            break;
                        }
                    }
                }
                if (i18n_keysSS.size() > 0)
                    for (String s : i18n_keysSS) {
                        I18nKeys.add(s);
                    }
                i18n_keysSS.clear();
            }
            List<String> i18n_keys2 = new ArrayList<>(I18nKeys);
            //有顺序的国际化key
            if (i18n_keys2 != null && i18n_keys2.size() > 0) {
                count = i18n_keys2.size();
                whereMap.put(Constants.I18N_KEYS, i18n_keys2);
                if (downAll) {
                    whereMap.put(Constants.LIMIT, count);
                }
                if (count > 1000 && i18nDataSourceConfig != null && i18nDataSourceConfig.getDbType() != null
                        && !i18nDataSourceConfig.getDbType().equals(Constants.STR_NO_SPACE)) {
                    List<String> i18n_keys = operateDBService.selectKeysByKeys(whereMap, downAll);
                    poListToDoList(i18n_resource_list, i18n_keys);
                } else {
                    List<String> i18n_keys = i18nResourceDao.selectKeysByKeys(whereMap);
                    poListToDoList(i18n_resource_list, i18n_keys);
                }
            }
            //这里改动之后 下载excel 的代码也需要改动
        } else if (i18nKeys != null && i18nKeys.size() > 0 && valuesMap == null) { // 只有有国际化key  没有国际化value
            whereMap.put(Constants.I18N_KEYS, i18nKeys);
            //如果是单国际化value 条件模糊查询
            count = i18nResourceDao.selectByKeysCount(whereMap);
            //按顺序查出指定部分国际化key
            if (downAll) {
                whereMap.put(Constants.LIMIT, count);
            }
            List<String> i18n_keys = i18nResourceDao.selectByKeys(whereMap);
            poListToDoList(i18n_resource_list, i18n_keys);
        } else if (i18nKeys == null && valuesMap != null && valuesMap.size() > 0) { // 没有国际化key 只有国际化value
            List<String> i18n_keys1 = new ArrayList<>();
            boolean first = true;
            for (Object languCode : valuesMap.keySet()) { // 所有循环的结果 取交集
                if (valuesMap.get(languCode) != null && !valuesMap.get(languCode).toString().equals(Constants.STR_NO_SPACE)) {
                    List<String> i18nValues = (List<String>) valuesMap.get(languCode);
                    whereMap.put(Constants.LANGUCODE, languCode.toString());
                    whereMap.put(Constants.I18NVALUES, i18nValues);
                    List<String> i18n_keys = i18nResourceDao.selectByOneKeyAndValues(whereMap);
                    if (i18n_keys != null && i18n_keys.size() > 0) {
                        if (first) {//第一次查询 一种语言 有结果
                            first = false;
                            for (String s : i18n_keys) {
                                i18n_keys1.add(s);
                            }
                            i18n_keys.clear();
                        } else {
                            List<String> templist = MyFileUtils.getStr(i18n_keys, i18n_keys1);
                            i18n_keys1.clear();
                            if (templist != null && templist.size() > 0) {
                                for (String s : templist) {
                                    i18n_keys1.add(s);
                                }
                            }
                        }
                    } else {//有一种语言的条件 没有查询出结果 交集一定为空 直接清空
                        i18n_keys1.clear();
                        break;
                    }
                }
            }
            List<String> i18n_keys2 = new ArrayList<>(i18n_keys1);
            //有顺序的国际化key
            if (i18n_keys2 != null && i18n_keys2.size() > 0) {
                count = i18n_keys2.size();
                whereMap.put(Constants.I18N_KEYS, i18n_keys2);
                if (downAll) {
                    whereMap.put(Constants.LIMIT, count);
                }
                if (count > 1000 && i18nDataSourceConfig != null && i18nDataSourceConfig.getDbType() != null
                        && !i18nDataSourceConfig.getDbType().equals(Constants.STR_NO_SPACE)) {
                    List<String> i18n_keys = operateDBService.selectKeysByKeys(whereMap, downAll);
                    poListToDoList(i18n_resource_list, i18n_keys);
                } else {
                    List<String> i18n_keys = i18nResourceDao.selectKeysByKeys(whereMap);
                    poListToDoList(i18n_resource_list, i18n_keys);
                }
            }
        } else if (i18nKeys == null && valuesMap == null) { //没有查询条件
            count = i18nResourceDao.selectByKeyAndValueAndLanguageCount(whereMap);
            if (downAll) {
                whereMap.put(Constants.LIMIT, count);
            }
            //按顺序查出指定部分国际化key
            List<String> i18n_keys = i18nResourceDao.selectKeyByKeyAndValueAndLanguage(whereMap);
            poListToDoList(i18n_resource_list, i18n_keys);
        }

        Map map = new HashMap();
        map.put(Constants.RESOURCE_LIST, i18n_resource_list);
        map.put(Constants.COUNT, count);
        map.put(Constants.PAGE_SIZE, pageSize);
        map.put(Constants.PAGENO, pageNO);
        return map;
    }*/

    private void poListToDoList(Collection<I18nResourceVO> i18nResourceList, List<String> i18nKeys) {
        if (i18nKeys != null && !i18nKeys.isEmpty()) {
            // 支持多租户
            Map<String, List<I18nResourcePO>> i18nResourceEntities = operateDBService.selectByKey(i18nKeys);
            for (String i18nKey : i18nKeys) {
                List<I18nResourcePO> languageResources = i18nResourceEntities.get(i18nKey);
                if (languageResources != null && !languageResources.isEmpty()) {
                    Map<String, String> i18nValues = languageResources.stream().collect(Collectors.toMap(I18nResourcePO::getLanguCode, I18nResourcePO::getI18nValue));
                    I18nResourceVO vo = new I18nResourceVO();
                    vo.setI18nKey(i18nKey);
                    vo.setI18nValues(i18nValues);
                    i18nResourceList.add(vo);
                }
            }
        }
    }

    @Override
    public PageResult createNewInternationalByValue(String value, String prefix) {
        StringBuilder key = new StringBuilder(prefix);
        List list = new ArrayList();
        if (null != value && !Constants.STR_NO_SPACE.equals(value) && null != prefix && !Constants.STR_NO_SPACE.equals(prefix)) {
            key.append(Constants.STR_POINT);
            key.append(value);
            key.append(Constants.STR_POINT);
            key.append(Constants.STR_RANDOM);
            key.append(System.currentTimeMillis());
            list.add(key);
        }
        PageResult pageResult = new PageResult(list, Constants.ZERO_INT, Constants.ZERO_INT, Constants.ZERO_INT);
        return pageResult;
    }
    
    @Transactional(rollbackFor = Exception.class)
    public void saveToDB(String moduleCode, String newVersionCode, boolean hasResource, List<I18nResourcePO> i18nResourceList) {
        log.info("开始数据入库");
        if (hasResource) {
            //先清除旧版本数据库数据
            I18nVersionPO oldVersionPO = i18nVersionDao.selectOne(new QueryWrapper<I18nVersionPO>().lambda()
            		.eq(I18nVersionPO::getModuleCode, moduleCode));
            if (oldVersionPO != null) {
                i18nResourceDao.delete(new QueryWrapper<I18nResourcePO>().lambda()
                        .eq(I18nResourcePO::getModuleVersionCode, oldVersionPO.getModuleVersionCode())
                        .eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT));
                i18nVersionDao.deleteById(oldVersionPO.getId());
            }
            //存下版本号
            operateDBService.saveBatch(i18nResourceList);
            I18nVersionPO versionPO = new I18nVersionPO();
            versionPO.setId(IDGenerator.newInstance().generate().longValue());
            versionPO.setModuleCode(moduleCode);
            versionPO.setModuleVersionCode(newVersionCode);
            versionPO.setValid(Constants.ONE_STR);
            i18nVersionDao.insert(versionPO);
            i18nIndexDao.delete(new QueryWrapper<I18nIndexPO>().lambda()
            		.eq(I18nIndexPO::getTenantId, Constants.DEFAULT_TENANT)
            		.eq(I18nIndexPO::getModuleCode, moduleCode));
            //生成索引
            String moduleIndex = moduleCode + UUID.randomUUID();
            I18nIndexPO i18nIndexPO = new I18nIndexPO();
            i18nIndexPO.setId(IDGenerator.newInstance().generate().longValue());
            i18nIndexPO.setModuleCode(moduleCode);
            i18nIndexPO.setModuleIndexCode(moduleIndex);
            i18nIndexPO.setValid(Constants.ONE_STR);
            i18nIndexPO.setTenantId(Constants.DEFAULT_TENANT);
            i18nIndexDao.insert(i18nIndexPO);
            // 更新缓存索引
            cachedResourceBundle.flushModuleIndexCache(Constants.DEFAULT_TENANT, moduleCode, moduleIndex);
        }
    }

    @Override
    @Transactional
    public PageResult addI18nResource(Map map) {
        //查看当前系统是否正在上传资源
        String moduleCode = map.get(Constants.MODULE_CODE).toString();
        UploadingUtil.getUpLoadingStateForAddOrUpdateOne(excelDao, i18nTokenDao, moduleCode);
        String i18n_key;
        Map<String, String> i18n_value;
        if (map.get(Constants.I18N_KEY) != null) {
            i18n_key = (String) map.get(Constants.I18N_KEY);
            i18n_key = i18n_key.replace(Constants.STR_SPACE, Constants.STR_NO_SPACE);
            if (i18n_key.length() > i18nProperties.getI18nKeyLengthNumDefa()) {
                throw new I18nException(I18nErrorEnum.I18N_KEY_LENGTH_ERROR);
            }
            //校验是否有 英文字母 数字 下划线 点 之外的其他字符
            String regEx = Constants.I18N_KEY_REGEX;
            if (!i18n_key.matches(regEx)) {
                throw new I18nException(I18nErrorEnum.I18N_KEY_ERROR);
            }
            //校验模块名是否正确 然后存入当前module_code
            String module_code = i18n_key.substring(Constants.ZERO_INT, i18n_key.indexOf(Constants.STR_POINT, Constants.ONE_INT));
            List<String> moduleCodes = i18nManagerService.getAllModuleCode();
            String tenantId = TenantUtil.getTenantId();
            if (moduleCodes.contains(module_code)) {
                List<I18nResourcePO> is = i18nResourceDao.selectList(new QueryWrapper<I18nResourcePO>().lambda()
                		.eq(I18nResourcePO::getI18nKey, i18n_key)
                		.eq(I18nResourcePO::getTenantId, tenantId));
                if (!is.isEmpty()) {
                    //有记录 返回当前已经有这个key了
                    throw new I18nException(I18nErrorEnum.I18N_KEY_EXIST);
                }
                //如果有模块名
                if (map.get(Constants.I18N_VALUE) != null) {
                    i18n_value = (Map) map.get(Constants.I18N_VALUE);
                    if (i18n_value != null && i18n_value.size() > 0) {
                        //校验value长度 大于500 提醒
                        for (Object key : i18n_value.keySet()) {
                            if (i18n_value.get(key) != null) {
                                if (i18n_value.get(key).toString().length() > i18nProperties.getI18nValueLengthNumDefa()) {
                                    throw new I18nException(I18nErrorEnum.I18N_VALUE_LENGTH_ERROR);
                                }
                            }
                        }
                        //查询当前租户的所有语言 补全新增数据中对应语言的国际化值为空字符串 便于 后续value搜索
                        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
                        for (I18nLanguagePO i18nLanguagePO : allLanguage) {
                            if (!i18n_value.keySet().contains(i18nLanguagePO.getLanguCode())) {
                                i18n_value.put(i18nLanguagePO.getLanguCode(), Constants.STR_NO_SPACE);
                            }
                        }
                        //存入数据
                        Date date = new Date();
                        for (String language : i18n_value.keySet()) {
                        	I18nResourcePO i18nResource = new I18nResourcePO();
                            i18nResource.setId(IDGenerator.newInstance().generate().longValue());
                            i18nResource.setI18nKey(i18n_key);
                            i18nResource.setLanguCode(language);
                            i18nResource.setI18nValue(i18n_value.get(language));
                            i18nResource.setValid(Constants.ONE_STR);
                            i18nResource.setCreateTime(date);
                            i18nResource.setModifyTime(date);
                            i18nResource.setModuleCode(module_code);
                            i18nResource.setTenantId(tenantId);
                            //在添加当前数据进入
                            i18nResourceDao.insert(i18nResource);
                            //更新缓存
                            try {
                                cachedResourceBundle.flushResourceCacheByUpdate(tenantId, module_code, language);
                            } catch(Exception e) {
                                log.error("<------ 数据新增入库成功，缓存刷新失败 ------->");
                                log.error(e.getMessage(), e);
                            }
                        }
                        // 更新索引
                        operateDBService.updateModuleIndexCode(module_code);
                        //存入文件用于更新 同步文件
                        Set<String> i18nKeys = new HashSet<>();
                        i18nKeys.add(i18n_key);
                        MyFileUtils.saveI18nKeyCode(i18nKeys, i18nProperties);
                        return new PageResult();
                    } else {
                        throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_LANGUAGE);
                    }
                } else {
                    throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_VALUE);
                }
            } else {
                //模块名错误
                throw new I18nException(I18nErrorEnum.MODULE_CODE_ERROR);
            }
        } else {
            throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_KEY);
        }
    }

    /**
     * @param map 参数结构为
     * {
     * 	"i18n_key":"authentication.custom.random1610083675029",
     * 	"i18n_value": {
     * 					"zh_CN":"部门大声地说",
     * 					"zh_HK":"部門",
     * 					"en_US":"Dept."
     *  			  },
     *  "moduleCode":"authentication"
     *  }
     */
    @Override
    @Transactional
    public PageResult updateI18nResource(Map map) {
        String module_code = map.get(Constants.MODULE_CODE).toString();
        //查看当前系统是否正在上传资源
        UploadingUtil.getUpLoadingStateForAddOrUpdateOne(excelDao, i18nTokenDao, module_code);
        String i18n_key;
        Map<String, String> i18n_value;
        i18n_key = (String) map.get(Constants.I18N_KEY);
        i18n_key = i18n_key.replace(Constants.STR_SPACE, Constants.STR_NO_SPACE);
        //校验模块名是否正确
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        if (moduleCodes.contains(module_code)) {
            //如果有正确
            String tenantId = TenantUtil.getTenantId();
            if (map.get(Constants.I18N_VALUE) != null) {
                i18n_value = (Map) map.get(Constants.I18N_VALUE);
                if (i18n_value != null && i18n_value.size() > 0) {
                    //校验value长度 大于500 提醒
                    for (Object key : i18n_value.keySet()) {
                        if (i18n_value.get(key) != null) {
                            if (i18n_value.get(key).toString().length() > i18nProperties.getI18nValueLengthNumDefa()) {
                                throw new I18nException(I18nErrorEnum.I18N_VALUE_LENGTH_ERROR);
                            }
                        }
                    }
                    //查询当前租户的所有语言 补全新增数据中对应语言的国际化值为空字符串 便于 后续value搜索
                    List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
                    for (I18nLanguagePO i18nLanguagePO : allLanguage) {
                        if (!i18n_value.keySet().contains(i18nLanguagePO.getLanguCode())) {
                            i18n_value.put(i18nLanguagePO.getLanguCode(), Constants.STR_NO_SPACE);
                        }
                    }
                    Date date = new Date();
                    for (String language : i18n_value.keySet()) {
                    	LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                    			.eq(I18nResourcePO::getLanguCode, language)
                        		.eq(I18nResourcePO::getI18nKey, i18n_key)
                        		.eq(I18nResourcePO::getTenantId, tenantId);
                        Integer count = i18nResourceDao.selectCount(queryWrapper);
                    	I18nResourcePO i18nResourcePO = new I18nResourcePO();
                    	if (count == null || count.intValue() == 0) {
                        	i18nResourcePO.setId(IDGenerator.newInstance().generate().longValue());
                            i18nResourcePO.setI18nKey(i18n_key);
                            i18nResourcePO.setLanguCode(language);
                            i18nResourcePO.setI18nValue(i18n_value.get(language));
                            i18nResourcePO.setTenantId(tenantId);
                            i18nResourcePO.setValid(Constants.ONE_STR);
                            i18nResourcePO.setCreateTime(date);
                            i18nResourcePO.setModifyTime(date);
                            i18nResourcePO.setModuleCode(module_code);
                            //在添加当前数据进入
                            i18nResourceDao.insert(i18nResourcePO);
                        } else {
                        	i18nResourcePO.setI18nValue(i18n_value.get(language));
                        	i18nResourcePO.setModifyTime(date);
                        	i18nResourceDao.update(i18nResourcePO, queryWrapper);
                        }
                        //更新缓存
                        try {
                            cachedResourceBundle.flushResourceCacheByUpdate(tenantId, module_code, language);
                        } catch(Exception e) {
                            log.error("<------------数据更新入库成功， 缓存刷新失败 ----------->");
                            log.error(e.getMessage(), e);
                        }
                    }
                    // 更新索引 (数据库和文件都更新)  TODO 1、同步刷新redis缓存 2、创建定时任务，定时从数据库同步索引到redis缓存
                    operateDBService.updateModuleIndexCode(module_code);
                    //存入文件用于更新 同步文件
                    Set<String> i18nKeys = new HashSet<>();
                    i18nKeys.add(i18n_key);
                    MyFileUtils.saveI18nKeyCode(i18nKeys, i18nProperties);
                    return new PageResult();
                } else {
                    throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_LANGUAGE);
                }
            } else {
                throw new I18nException(I18nErrorEnum.PARAM_LOST_I18N_VALUE);
            }
        } else {
            throw new I18nException(I18nErrorEnum.MODULE_CODE_ERROR);
        }
    }

    @Override
    @Transactional
    public PageResult deleteI18nResourceByKey(String i18n_key) {
        //查看当前系统是否正在上传资源
        UploadingUtil.getUpLoadingState(excelDao, i18nTokenDao);
        String tenantId = TenantUtil.getTenantId();
        //物理删除
        LambdaUpdateWrapper<I18nResourcePO> updateWrapper = new UpdateWrapper<I18nResourcePO>().lambda()
        		.eq(I18nResourcePO::getI18nKey, i18n_key)
        		.eq(I18nResourcePO::getTenantId, tenantId);
        int deleteResult = i18nResourceDao.delete(updateWrapper);
        // 租户无法删除系统数据
        if (deleteResult == 0) {
        	throw new I18nException(I18nErrorEnum.I18N_DELETE_DENY_ERROR);
        }
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        String module_code = Constants.STR_NO_SPACE;
        if (i18n_key.contains(Constants.STR_POINT)) {
            String moduleCode = i18n_key.substring(Constants.ZERO_INT, i18n_key.indexOf(Constants.STR_POINT, Constants.ONE_INT));
            if (moduleCodes.contains(moduleCode)) {
                operateDBService.updateModuleIndexCode(moduleCode);
                module_code = moduleCode;
            } else {
                operateDBService.updateModuleIndexCode(ModuleEnum.DEFAULT.getModuleId());
                module_code = ModuleEnum.DEFAULT.getModuleId();
            }
        } else {
            operateDBService.updateModuleIndexCode(ModuleEnum.DEFAULT.getModuleId());
            module_code = ModuleEnum.DEFAULT.getModuleId();
        }
        List<I18nLanguagePO> allLanguage = getAllLanguage(tenantId);
        Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
        cachedResourceBundle.flushResourceCacheByDelete(module_code, languages, tenantId);
        //存入文件用于更新 同步文件
        Set<String> i18nKeys = new HashSet<>();
        i18nKeys.add(i18n_key);
        MyFileUtils.saveI18nKeyCode(i18nKeys, i18nProperties);
        return new PageResult<>();
    }

    @Override
    public PageResult exportExcelModel() {
        try {
            XSSFWorkbookImportExcel();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new I18nException(I18nErrorEnum.MODE_EXPORT_ERROR);
        }
        return new PageResult<>();
    }

    @Override
    public Result<String> importation(String originalFileName, String uuidFilename) {
        //查看当前系统是否正在上传资源
        String tenantId = TenantUtil.getTenantId();
        UploadingUtil.getUpLoadingState(excelDao, i18nTokenDao);
        Result<String> result = new Result<>();
        ExcelPO excelPO = new ExcelPO();
        excelPO.setId(IDGenerator.newInstance().generate().longValue());
        excelPO.setOperateType(Constants.STR_IMPORT);
        excelPO.setStatus(1);
        excelPO.setFileName(uuidFilename);
        excelPO.setValid(Constants.ONE_STR);
        excelPO.setTenantId(tenantId);
        excelDao.insert(excelPO);
        new Thread() {
            public void run() {
                importExcelMain(uuidFilename, originalFileName);
            }
        }.start();
        result.setMessage(Constants.EXCEL_IMPORTING);
        result.setData(excelPO.getId().toString());
        return result;
    }

    public void exportExcelMain(String filename, List<String> i18nKeysList, I18nQueryDTO queryDto, Pagination pagination, boolean downloadAll) {
    	String rootFolder = FilePathUtil.getFilePath(i18nProperties);
        String exportFolder = new StringBuilder(rootFolder).append(Constants.EXCEL_FILE_EXPORT_PATH).append(queryDto.getTenantId()).append("/").toString();
        String exportTemplateFolder = new StringBuilder(rootFolder).append(Constants.EXCEL_FILE_TEMPLATE_PATH).append(queryDto.getTenantId()).append("/").toString();
        //判断路径是否存在
        File targetFile = new File(exportFolder);
        if (!targetFile.exists()) {
            targetFile.mkdirs();
        }
        String filePath = String.format("%s%s", exportFolder, filename);
        String oldPath = String.format("%s%s.xlsx", exportTemplateFolder, Constants.FILENAME);
        LambdaQueryWrapper<ExcelPO> excelQueryWrapper = new QueryWrapper<ExcelPO>().lambda()
                .eq(ExcelPO::getFileName, filename)
                .eq(ExcelPO::getValid, Constants.ONE_STR)
                .eq(ExcelPO::getTenantId, queryDto.getTenantId());
        //创建模版 拷贝过来 重命名
        try {
            XSSFWorkbookImportExcel();
            File filePathFile = new File(filePath);
            File oldPathFile = new File(oldPath);
            MyFileUtils.copyFileUsingFileStreams(oldPathFile, filePathFile);
        } catch (Exception e) {
            log.error(e.getMessage());
            //修改导出状态
            ExcelPO excelPO2 = new ExcelPO();
            excelPO2.setStatus(3);
            excelPO2.setFileName(filename);
            excelPO2.setErrorMessage(I18nErrorEnum.FILE_COPY_ERROR.getMessage());
            excelDao.update(excelPO2, excelQueryWrapper);
            throw new I18nException(I18nErrorEnum.FILE_COPY_ERROR);
        }
        LambdaQueryWrapper<I18nLanguagePO> queryWrapper = new QueryWrapper<I18nLanguagePO>().lambda().eq(I18nLanguagePO::getTenantId, queryDto.getTenantId());
        List<I18nLanguagePO> languageEntities = i18nLanguageDao.selectList(queryWrapper);
        boolean exportResult = false;
        //数据到内存
        Collection<I18nResourceVO> i18nResourceList = new ArrayList<>();
        if (downloadAll) { // 导出全部, 分页查询追加到excel, 防止一次性查询所有数据导致内存溢出
            try (FileInputStream fileInputStream = new FileInputStream(filePath);
                 XSSFWorkbook workbook = new XSSFWorkbook(fileInputStream);
                 SXSSFWorkbook sxssfWorkbook = new SXSSFWorkbook(workbook, 100);
                 BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath));) {
                pagination = new Pagination(0, 2000, 1); // 初始
                PageResult<I18nResourceVO> pageResult = queryByMap(queryDto, pagination);
                int startRow = 1;
                while (!pageResult.getList().isEmpty()) {
                    i18nResourceList = pageResult.getList();
                    startRow = ExcelUtil.appendToExcel(sxssfWorkbook, languageEntities, i18nResourceList, startRow);
                    pageResult.getList().clear(); // let gc go
                    pagination.setCurrent(pagination.getCurrent() + 1);
                    pageResult = queryByMap(queryDto, pagination);
                }
                exportResult = true;
                //写出到文件
                sxssfWorkbook.write(outputStream);
                // 释放workbook所占用的所有windows资源
                outputStream.flush();
                sxssfWorkbook.dispose();
            } catch (IOException e) {
                log.error("导出所有国际化数据失败", e);
            }
        } else {
            if (i18nKeysList != null && !i18nKeysList.isEmpty()) { // 导出当前选择的数据
                poListToDoList(i18nResourceList, i18nKeysList);
            } else { // 导出当前页
                PageResult<I18nResourceVO> pageResult = queryByMap(queryDto, pagination);
                i18nResourceList = pageResult.getList();
            }
            exportResult = ExcelUtil.exportExcel(filePath, languageEntities, i18nResourceList);
        }
        //修改导出状态 2-表示成功 3-表示失败
        ExcelPO excelPO = excelDao.selectOne(excelQueryWrapper);
        if (excelPO != null) {
            ExcelPO excelPO2 = new ExcelPO();
            excelPO2.setStatus(exportResult ? 2 : 3);
            excelPO2.setFileName(filename);
            excelDao.update(excelPO2, excelQueryWrapper);
        }
    }

    @Override
    public Result<String> exportExcelFile(List<String> i18nKeysList, I18nQueryDTO queryDto, Pagination pagination, boolean downloadAll) {
        log.info("开始异步执行导出excel");
        Result<String> result = new Result<>();
        String filename = UUID.randomUUID() + Constants.STR_POINT + Constants.XLSX_LOW;
        ExcelPO excelPO = new ExcelPO();
        excelPO.setId(IDGenerator.newInstance().generate().longValue());
        excelPO.setOperateType(Constants.STR_EXPORT);
        excelPO.setStatus(1);
        excelPO.setFileName(filename);
        excelPO.setValid(Constants.ONE_STR);
        excelPO.setTenantId(RpcContext.getContext().getTenantId());
        excelDao.insert(excelPO);
        new Thread() {
            public void run() {
                exportExcelMain(filename, i18nKeysList, queryDto, pagination, downloadAll);
            }
        }.start();
        result.setMessage(Constants.STR_EXPORT);
        result.setData(excelPO.getId().toString());
        return result;
    }

    @Override
    public Result<ExcelVO> checkImportStatus(long id) {
        Result<ExcelVO> result = new Result<>();
        ExcelVO excelVO = new ExcelVO();
        ExcelPO excelPO = excelDao.selectOne(new QueryWrapper<ExcelPO>().lambda().eq(ExcelPO::getId, id).eq(ExcelPO::getValid, Constants.ONE_STR));
        if (excelPO != null) {
            excelVO.setId(excelPO.getId());
            excelVO.setStatus(excelPO.getStatus());
            excelVO.setFileName(excelPO.getFileName());
            excelVO.setErrorNum(excelPO.getErrorNum());
            excelVO.setAddNum(excelPO.getAddNum());
            excelVO.setUpdateNum(excelPO.getUpdateNum());
            excelVO.setAllNum(excelPO.getAllNum());
            excelVO.setErrorFile(excelPO.getErrorFile());
            excelVO.setErrorMessage(excelPO.getErrorMessage());
        } else {
            throw new I18nException(I18nErrorEnum.RESOURCE_NOT_FIND_ERROR);
        }
        result.setData(excelVO);
        return result;
    }

    /**
     * 1. 校验上传的excel数据是否合法, 规则如下:
     *   <ul>
     *   	<li>校验上传文件的语言是否和当前启用的语言一致, 否则终止导入</li>
     *   	<li>上传的国际化key不能重复, 否则终止导入</li>
     *   	<li>国际化key不能为空, 否则终止导入</li>
     *   	<li>国际化key长度不能超过255, 否则加入错误数据清单</li>
     *   	<li>国际化key不能包含除英文字母 数字 下划线 小数点之外的符号, 否则加入错误数据清单</li>
     *   	<li>国际化value长度不能超过限制, 否则加入错误清单</li>
     *   </ul>
     * 2. 按照模块查找数据库, 在内存和excel数据进行匹配整合
     * 3. 按照模块删除数据库数据, 之后按模块一次性导入第二步的数据
     * 4. 更新缓存, 文件
     */
    public void importExcelMain(String uuidFilename, String originalFileName) {
        long time0 = System.currentTimeMillis();
        log.info("开始异步执行解析excel: {}", time0);
        String tenantId = TenantUtil.getTenantId();
        String filePath = FilePathUtil.getFilePath(i18nProperties) + Constants.EXCEL_FILE_IMPORT_PATH + tenantId + Constants.PATH + uuidFilename;
        File excel = new File(filePath);
        String fileName = excel.toString().substring(excel.toString().length() - 4, excel.toString().length());
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        String fileNameUpperCase = fileName.toUpperCase();
        //格式为.xlsx
        if (Constants.XLSX.equals(fileNameUpperCase)) {
            Integer valueLengthLimit = i18nProperties.getI18nValueLengthNumDefa();
            Integer keyLengthLimit = i18nProperties.getI18nKeyLengthNumDefa();
            try (FileInputStream fileInputStream = new FileInputStream(excel);
                 XSSFWorkbook xssfWorkbook = new XSSFWorkbook(fileInputStream)) {
                List<I18nLanguagePO> languageEntities = getAllLanguage(tenantId);
                //只处理第一张sheet表
                Map paramMap = ResolveExcelUtils.resolverExcel(xssfWorkbook.getSheetAt(0), moduleCodes, languageEntities, valueLengthLimit, keyLengthLimit);
                List<I18nResourcePO> i18n_POs = (List<I18nResourcePO>) paramMap.get(Constants.I18N_POS);
                Set<String> i18nModuleSet = (Set<String>) paramMap.get(Constants.I18N_MODULE_SET);
                Set<String> i18nKeySet = (Set<String>) paramMap.get(Constants.I18N_KEY_SET);
                Map<String, Integer> count_errorMap = (Map<String, Integer>) paramMap.get(Constants.COUNT_ERROR_MAP);
                Integer count = (Integer) paramMap.get(Constants.COUNT);
                Integer errorNum = (Integer) paramMap.get(Constants.STR_ERROR_NUM);
                XSSFSheet sheetAt = (XSSFSheet) paramMap.get(Constants.SHEET_AT);
                Map<Integer, String> i18n_key_errorMap = (Map<Integer, String>) paramMap.get(Constants.I18N_KEY_ERROR_MAP);
                Map<Integer, Integer> i18n_value_errorMap = (Map<Integer, Integer>) paramMap.get(Constants.I18N_VALUE_ERROR_MAP);
                if (i18nModuleSet != null && i18nModuleSet.size() > 0) {
                    //获取excel中包含的所有模块的在数据库中的键值对信息 用于数据对比 删除对应的之后全部导入
                	for (String moduleId : i18nModuleSet) {
	                    List<I18nResourcePO> dbResources = i18nResourceDao.selectList(new QueryWrapper<I18nResourcePO>().lambda()
	                            .eq(I18nResourcePO::getModuleCode, moduleId)
	                            .in(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT, tenantId));
	                    List<I18nResourcePO> tenantResources = ResolveExcelUtils.execExcelMapAndDBMap(i18n_POs, dbResources);
	                    log.info("导入excel excel数据内存处理时间：" + (System.currentTimeMillis() - time0));
	                    log.info("+++++++++++++++++++++ i18n db operate begin {} ++++++++++++++++++++++", System.currentTimeMillis());
	                    if (!tenantResources.isEmpty()) {
	                        operateDBService.saveListToDB(tenantResources);
	                        // 更新缓存
                            for (I18nLanguagePO language : languageEntities) {
                                cachedResourceBundle.flushResourceCacheByUpdate(tenantId, moduleId, language.getLanguCode());
                            }
                            // 更新索引
                            I18nIndexPO i18nIndexEntity = new I18nIndexPO();
                        	i18nIndexEntity.setModuleIndexCode(moduleId + UUID.randomUUID());
                        	i18nIndexDao.update(i18nIndexEntity, new UpdateWrapper<I18nIndexPO>().lambda()
                        			.eq(I18nIndexPO::getModuleCode, moduleId)
                        			.eq(I18nIndexPO::getTenantId, tenantId));
                            //将该模块的索引存入索引目录文件中
                            cachedResourceBundle.flushModuleIndexCache(moduleId, tenantId, i18nIndexEntity.getModuleIndexCode());
                        }
	                    dbResources.clear();
                    }
                    log.info("+++++++++++++++++++++ i18n db operate end () ++++++++++++++++++++++", System.currentTimeMillis());
                } else {
                    count_errorMap.put(Constants.UPDATE_NUM, 0);
                    count_errorMap.put(Constants.ADD_NUM, 0);
                }
                // TODO i18nKeySet 写入文件
                MyFileUtils.saveI18nKeyCode(i18nKeySet, i18nProperties);
                //i18n_key_errorMap 写入文件
                updateExcelErrorFile(count, errorNum, uuidFilename, sheetAt, originalFileName, i18n_key_errorMap, i18n_value_errorMap, count_errorMap);
            } catch (I18nException e) {
                log.error(e.getMessage());
                //线程失败的 直接修改excelPO 3
                updateExcelPO(uuidFilename, null, 3, e.getSimpleError().getMessage(), Constants.STR_NO_SPACE);
                throw new I18nException(I18nErrorEnum.FILE_RESOLVER_SHEET_ERROR);
            } catch (OutOfMemoryError e) {
                log.error(e.getMessage());
                //线程失败的 直接修改excelPO 3
                updateExcelPO(uuidFilename, null, 3, Constants.EXCEL_IMPORTING_OOM_ERROR, Constants.STR_NO_SPACE);
                throw new I18nException(I18nErrorEnum.FILE_RESOLVER_SHEET_RM_ERROR);
            } catch (Exception e) {
                log.error(e.getMessage());
                updateExcelPO(uuidFilename, null, 3, Constants.EXCEL_IMPORTING_ERROR, Constants.STR_NO_SPACE);
                throw new I18nException(I18nErrorEnum.FILE_RESOLVER_UN_KNOW_ERROR);
            } finally {
                //最后 把数据库 exclePO状态修改掉 先查询  如果状态是 1  这里更新为2 如果是 3 不用管
                LambdaQueryWrapper<ExcelPO> excelQueryWrapper = new QueryWrapper<ExcelPO>().lambda().eq(ExcelPO::getFileName, uuidFilename).eq(ExcelPO::getValid, Constants.ONE_STR);
                if (StringUtils.isNotEmpty(tenantId)) {
                    excelQueryWrapper.eq(ExcelPO::getTenantId, tenantId);
                }
                ExcelPO excelPO = excelDao.selectOne(excelQueryWrapper);
                if (excelPO != null && excelPO.getStatus() == 1) {
                    ExcelPO excelPO2 = new ExcelPO();
                    excelPO2.setStatus(2);
                    excelPO2.setFileName(uuidFilename);
                    excelDao.update(excelPO2, excelQueryWrapper);
                }
                if (!excel.delete()) {
                    log.error(excel.getName() + Constants.DELETE_ERROR);
                }
            }
        } else {
            throw new I18nException(I18nErrorEnum.FILE_FORMAT_ERROR);
        }
    }

    public void XSSFWorkbookImportExcel() {
        String tenantId = TenantUtil.getTenantId();
        //查询数据库所有语言
        List<I18nLanguagePO> allLanguage = getAllLanguage(tenantId);
        try (XSSFWorkbook wb = new XSSFWorkbook();) {
            //创建工作簿---->XSSF代表10版的Excel(HSSF是03版的Excel)
            //工作表
            for (int f = 0; f < 1; f++) {
                XSSFSheet sheet = wb.createSheet(Constants.FILE_TABLE_NAME);
                //标头行，代表第一行
                XSSFRow header = sheet.createRow(0);
                //创建单元格，0代表第一行第一列
                XSSFCell cell0 = header.createCell(0);
                cell0.setCellValue(Constants.I18N_KEY_ZHCN);
                for (int i = 0; i < allLanguage.size(); i++) {
                    header.createCell(i + 1).setCellValue(allLanguage.get(i).getLanguCode());
                }
                //设置列的宽度
                //getPhysicalNumberOfCells()代表这行有多少包含数据的列
                for (int i = 0; i < header.getPhysicalNumberOfCells(); i++) {
                    //POI设置列宽度时比较特殊，它的基本单位是1/255个字符大小，
                    //因此我们要想让列能够盛的下40个字符的话，就需要用255*40
                    if (i == 0) {
                        sheet.setColumnWidth(i, 255 * 30);
                    } else {
                        sheet.setColumnWidth(i, 255 * 40);
                    }
                }
            }
            //判断路径是否存在
            String targetFolderPath = FilePathUtil.getFilePath(i18nProperties) + Constants.EXCEL_FILE_TEMPLATE_PATH;
            if (StringUtils.isNotEmpty(tenantId)) {
                targetFolderPath += tenantId + "/";
            }
            File targetFile = new File(targetFolderPath);
            if (!targetFile.exists()) {
                targetFile.mkdirs();
            }
            try (FileOutputStream fos = new FileOutputStream(targetFolderPath + Constants.FILENAME + Constants.STR_POINT + Constants.XLSX_LOW)) {
                //向指定文件写入内容
                wb.write(fos);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public String queryOneByOneKey(String i18nKey) {
        String languageCode = RpcContext.getContext().getLanguage().toString();
        String tenantId = TenantUtil.getTenantId();
        String moduleId = i18nKey.split(Constants.STR_POINT1)[0];
        boolean moduleExist = i18nManagerService.moduleExists(moduleId);
        if (!moduleExist) {
            moduleId = ModuleEnum.DEFAULT.getModuleId();
        }
        KeyValuePairCollection cachedResource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, languageCode);
        return cachedResource == null ? "" : cachedResource.getKvs().get(i18nKey);
    }

    @Override
    public Map<String, String> queryKeyValuesByKeys(String[] keysArr) {
        Map<String, String> i18nMap = new HashMap<>();
        for (String key : keysArr) {
            String i18nValue = queryOneByOneKey(key);
            i18nMap.put(key, i18nValue);
        }
        return i18nMap;
    }

    @Override
    public Map<String, String> queryKeyValuesByKeys1(String[] keysArr) {
        Map<String, Set<String>> moduleAndKeyMap = new HashMap<>();
        for (String key : keysArr) {
            String moduleId = key.split(Constants.STR_POINT1)[0];
            Set<String> keySet = new HashSet<>();
            if (moduleAndKeyMap.containsKey(moduleId)) {
                keySet = moduleAndKeyMap.get(moduleId);
            }
            keySet.add(key);
            moduleAndKeyMap.put(moduleId, keySet);
        }
        return queryKeyByMap(moduleAndKeyMap);
    }

    private Map<String, String> queryKeyByMap(Map<String, Set<String>> moduleAndKeyMap) {
        Map<String, String> resultMap = new HashMap<>();

        String languageCode = RpcContext.getContext().getLanguage().toString();
        String tenantId = TenantUtil.getTenantId();

        for (Map.Entry<String, Set<String>> entry : moduleAndKeyMap.entrySet()) {
            String moduleId = entry.getKey();
            boolean moduleExist = i18nManagerService.moduleExists(moduleId);
            if (!moduleExist) {
                moduleId = ModuleEnum.DEFAULT.getModuleId();
            }
            KeyValuePairCollection cachedResource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, languageCode);
            for (String key : entry.getValue()) {
                resultMap.put(key, cachedResource.getKvs().get(key));
            }
        }
        return resultMap;
    }

    @Override
    public Result<String> searchOneI18NKeyValues(String key, String language) {
        String tenantId = TenantUtil.getTenantId();
        String moduleId = key.split(Constants.STR_POINT1)[0];
        boolean moduleExist = i18nManagerService.moduleExists(moduleId);
        if (!moduleExist) {
            moduleId = ModuleEnum.DEFAULT.getModuleId();
        }
        String languageCode = language == null ? RpcContext.getContext().getLanguage().toString(): language;
        KeyValuePairCollection cachedResource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, languageCode);
        Result<String> result = new Result<>();
        if (cachedResource != null) {
            result.setData(cachedResource.getKvs().get(key));
        }
        return result;
    }

    @Override
    public Result<Map<String, String>> searchI18NKeysValues(List<String> keys, String language) {
        String tenantId = TenantUtil.getTenantId();
        String languageCode = language == null ? RpcContext.getContext().getLanguage().toString() : language;
        Map<String, String> keyAndValueMap = new HashMap<>();
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        for (String i18nKey : keys) {
            String moduleId = i18nKey.split(Constants.STR_POINT1)[0];
            if (!moduleCodes.contains(moduleId)) {
                moduleId = ModuleEnum.DEFAULT.getModuleId();
            }
            KeyValuePairCollection cachedResource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, languageCode);
            if (cachedResource != null) {
                keyAndValueMap.put(i18nKey, cachedResource.getKvs().get(i18nKey));
            }
        }
        return new Result<>(keyAndValueMap);
    }

    public I18nResourceVO queryByI18nKey(String key) {
        String tenantId = TenantUtil.getTenantId();
        LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
        		.eq(I18nResourcePO::getI18nKey, key)
        		.in(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT, tenantId);
        List<I18nResourcePO> i18nResourcePOs = i18nResourceDao.selectList(queryWrapper);
        Map<String, List<I18nResourcePO>> groupedResource = i18nResourcePOs.stream().collect(Collectors.groupingBy(I18nResourcePO::getTenantId));
        // 如果存在租户数据, 分组长度为2
        if (groupedResource.size() > 1) {
            i18nResourcePOs = groupedResource.get(tenantId);
        }
        I18nResourceVO i18nResourceVO = new I18nResourceVO();
        i18nResourceVO.setI18nKey(key);
        Map<String, String> i18nValues = new HashMap<>();

        // key 找不到值，i18nValues 所有语言的value 为当前key
        if (CollectionUtils.isEmpty(i18nResourcePOs)) {
            List<I18nLanguagePO> allLangus= getAllLanguage(tenantId);
            allLangus.forEach(langu -> {
                i18nValues.put(langu.getLanguCode(), key);
            });
        }
        i18nResourcePOs.forEach(i18nResourcePO1 -> {
            i18nValues.put(i18nResourcePO1.getLanguCode(), i18nResourcePO1.getI18nValue());
        });
        i18nResourceVO.setI18nValues(i18nValues);
        return i18nResourceVO;
    }

    private void updateExcelErrorFile(int count, Integer errorNum, String uuidFilename, XSSFSheet sheetAt,
                                      String originalFileName, Map<Integer, String> i18n_key_errorMap,
                                      Map<Integer, Integer> i18n_value_errorMap, Map<String, Integer> count_errorMap) {
        if (errorNum > 0) {
            //创建错误返回文件
            //先判断是否有其他的线程已经创建了该文件
            String errorFilename = originalFileName;
            String rootPath = FilePathUtil.getFilePath(i18nProperties);
            String tenantId = TenantUtil.getTenantId();
            String errorTargetFolderPath = rootPath + Constants.EXCEL_ERROR_FILE_PATH + tenantId + Constants.PATH;
            String oldTargetFolderPath = rootPath + Constants.EXCEL_FILE_IMPORT_PATH + tenantId + Constants.PATH;
            String errorFilePath = errorTargetFolderPath + uuidFilename;
            File newErrorFile = new File(errorFilePath);
            if (!newErrorFile.exists()) {//没有该文件则创建
                MyFileUtils.createDir(errorTargetFolderPath);
                File oldFile = new File(oldTargetFolderPath + uuidFilename);
                MyFileUtils.copyFileUsingFileStreams(oldFile, newErrorFile);
            }
            try (FileInputStream fileInputStream = new FileInputStream(newErrorFile);
                 XSSFWorkbook xssfWorkbook = new XSSFWorkbook(fileInputStream);) {
                XSSFSheet errorFileSheet = xssfWorkbook.getSheet(sheetAt.getSheetName());
                // 设置字体//颜色
                CellStyle redStyle = xssfWorkbook.createCellStyle();
                XSSFFont redFont = xssfWorkbook.createFont();
                redFont.setColor(Font.COLOR_RED);
                redStyle.setFont(redFont);
                //修改对应的
                if (i18n_key_errorMap != null && i18n_key_errorMap.size() > 0) {
                    for (Integer rowNum : i18n_key_errorMap.keySet()) {
                        Row row = errorFileSheet.getRow(rowNum);
                        Cell cell = row.getCell(0);
                        cell.setCellStyle(redStyle);
                        Cell cell2 = row.createCell(count);
                        cell2.setCellValue(i18n_key_errorMap.get(rowNum));
                        cell2.setCellStyle(redStyle);
                    }
                }
                if (i18n_value_errorMap != null && i18n_value_errorMap.size() > 0) {
                    for (Integer rowNum : i18n_value_errorMap.keySet()) {
                        Row row = errorFileSheet.getRow(rowNum);
                        Cell cell = row.getCell(i18n_value_errorMap.get(rowNum));
                        if (i18n_value_errorMap.get(rowNum) == 0) {
                            cell.setCellStyle(redStyle);
                            Cell cell2 = row.createCell(count + 1);
                            cell2.setCellValue(Constants.I18N_VALUE_BLANK_ERROR);
                            cell2.setCellStyle(redStyle);
                        } else {
                            if (i18n_value_errorMap.size() > 0 && i18n_value_errorMap.keySet().contains(rowNum)) {
                                cell.setCellStyle(redStyle);
                                Cell cell2 = row.createCell(count + 1);
                                cell2.setCellValue(I18nErrorEnum.I18N_VALUE_LENGTH_ERROR.getMessage());
                                cell2.setCellStyle(redStyle);
                            } else {
                                cell.setCellStyle(redStyle);
                                Cell cell2 = row.createCell(count);
                                cell2.setCellValue(I18nErrorEnum.I18N_VALUE_LENGTH_ERROR.getMessage());
                                cell2.setCellStyle(redStyle);
                            }
                        }
                    }
                }
                try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(errorFilePath));) {
                    xssfWorkbook.write(outputStream);
                    outputStream.flush();
                } catch (IOException e) {
                    log.error(e.getMessage());
                }
            } catch (IOException e) {
                log.error(e.getMessage());
            }
            updateExcelPO(uuidFilename, count_errorMap, null, Constants.STR_NO_SPACE, errorFilename);
        } else {
            updateExcelPO(uuidFilename, count_errorMap, null, Constants.STR_NO_SPACE, Constants.STR_NO_SPACE);
        }
    }

    //更新导入状态
    public void updateExcelPO(String uuidFilename, Map<String, Integer> count_errorMap, Integer
            status, String errorMessage, String errorFilename) {
        ExcelPO excelPO = new ExcelPO();
        if (count_errorMap != null && count_errorMap.size() > 0) {
            int errorNum = count_errorMap.get(Constants.STR_ERROR_NUM);
            int addNum = count_errorMap.get(Constants.ADD_NUM);
            int updateNum = count_errorMap.get(Constants.UPDATE_NUM);
            int allNum = count_errorMap.get(Constants.STR_ALL_NUM);
            excelPO.setErrorNum(errorNum);
            excelPO.setAddNum(addNum);
            excelPO.setUpdateNum(updateNum);
            excelPO.setAllNum(allNum);
        }
        excelPO.setStatus(status);
        excelPO.setFileName(uuidFilename);
        if (!errorMessage.equals(Constants.STR_NO_SPACE)) {
            excelPO.setErrorMessage(errorMessage);
        }
        if (!errorFilename.equals(Constants.STR_NO_SPACE)) {
            excelPO.setErrorFile(errorFilename);
        }
        String tenantId = TenantUtil.getTenantId();
        LambdaUpdateWrapper<ExcelPO> updateWrapper = new UpdateWrapper<ExcelPO>().lambda().eq(ExcelPO::getFileName, uuidFilename);
        if (StringUtils.isNotEmpty(tenantId)) {
            updateWrapper.eq(ExcelPO::getTenantId, tenantId);
        }
        excelDao.update(excelPO, updateWrapper);
    }

    @Override
    public List<I18nLanguagePO> getAllLanguage(String tenantId) {
        LambdaQueryWrapper<I18nLanguagePO> queryWrapper = new QueryWrapper<I18nLanguagePO>().lambda()
                .eq(I18nLanguagePO::getTenantId, tenantId)
                .eq(I18nLanguagePO::getValid, Constants.ONE_STR);
        List<I18nLanguagePO> languageEntities = i18nLanguageDao.selectList(queryWrapper);
        if (languageEntities.isEmpty()) {
            queryWrapper = new QueryWrapper<I18nLanguagePO>().lambda()
            		.eq(I18nLanguagePO::getTenantId, Constants.DEFAULT_TENANT)
            		.eq(I18nLanguagePO::getValid, Constants.ONE_STR);
            languageEntities = i18nLanguageDao.selectList(queryWrapper);
        }
        return languageEntities;
    }
    
    @Override
    public List<String> getEnableLanguage(String tenantId) {
        LambdaQueryWrapper<I18nLanguagePO> queryWrapper = new QueryWrapper<I18nLanguagePO>().lambda()
                .eq(I18nLanguagePO::getTenantId, tenantId)
                .eq(I18nLanguagePO::getValid, Constants.ONE_STR)
                .eq(I18nLanguagePO::getHasUsed, Constants.ONE_STR);
        List<I18nLanguagePO> languageEntities = i18nLanguageDao.selectList(queryWrapper);
        if (languageEntities.isEmpty()) {
        	queryWrapper = new QueryWrapper<I18nLanguagePO>().lambda()
                    .eq(I18nLanguagePO::getTenantId, Constants.DEFAULT_TENANT)
                    .eq(I18nLanguagePO::getValid, Constants.ONE_STR)
                    .eq(I18nLanguagePO::getHasUsed, Constants.ONE_STR);
            languageEntities = i18nLanguageDao.selectList(queryWrapper);
        }
        return languageEntities.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toList());
    }

    @Override
    public List<I18nResourceVO> getValueByKey(String key) {
        List<I18nResourceVO> i18NResourceVOS = new ArrayList<>();
        String moduleId = key.split(Constants.STR_POINT1)[0];
        boolean moduleExist = i18nManagerService.moduleExists(moduleId);
        if (!moduleExist) {
            moduleId = ModuleEnum.DEFAULT.getModuleId();
        }
        String tenantId = TenantUtil.getTenantId();
        List<String> allLanguages = getEnableLanguage(tenantId);
        for (String language : allLanguages) {
            KeyValuePairCollection cachedResource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, language);
            if (cachedResource != null) {
                I18nResourceVO i18nResourceVO = new I18nResourceVO();
                i18nResourceVO.setI18nKey(key);
                i18nResourceVO.setI18nValue(cachedResource.getKvs().get(key));
                i18nResourceVO.setLanguCode(language);
                i18NResourceVOS.add(i18nResourceVO);
            }
        }
        return i18NResourceVOS;
    }

    @Override
    public Map<String, String> putResourceToCache(String moduleCode, String language) {
        Map<String, String> resultMap = new HashMap<>();
        String tenantId = Constants.DEFAULT_TENANT;

        LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                .eq(I18nResourcePO::getTenantId, tenantId)
                .eq(I18nResourcePO::getModuleCode, moduleCode)
                .eq(I18nResourcePO::getLanguCode, language);
        List<I18nResourcePO> resources = i18nResourceDao.selectList(queryWrapper);
        for (I18nResourcePO resource : resources) {
            resultMap.put(resource.getI18nKey(), resource.getI18nValue());
        }
        // 刷新到缓存
        cachedResourceBundle.flushResourceCacheByUpdate(tenantId, moduleCode, language);
        i18nIndexDao.delete(new QueryWrapper<I18nIndexPO>().lambda()
                .eq(I18nIndexPO::getTenantId, Constants.DEFAULT_TENANT)
                .eq(I18nIndexPO::getModuleCode, moduleCode));
        // 生成索引并更新缓存索引
        String moduleIndex = moduleCode + UUID.randomUUID();
        I18nIndexPO i18nIndexPO = new I18nIndexPO();
        i18nIndexPO.setId(IDGenerator.newInstance().generate().longValue());
        i18nIndexPO.setModuleCode(moduleCode);
        i18nIndexPO.setModuleIndexCode(moduleIndex);
        i18nIndexPO.setValid(Constants.ONE_STR);
        i18nIndexPO.setTenantId(tenantId);
        i18nIndexDao.insert(i18nIndexPO);
        cachedResourceBundle.flushModuleIndexCache(tenantId, moduleCode, moduleIndex);
        return resultMap;
    }
}
