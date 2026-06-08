package com.supcon.supfusion.i18n.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.supcon.supfusion.framework.cloud.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.service.I18nResourceDownloadService;
import com.supcon.supfusion.i18n.service.I18nResourceService;
import com.supcon.supfusion.i18n.until.CachedResourceBundle;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MessageResourceDownloadServiceImpl {

	@Autowired
    private I18nIndexDao i18nIndexDao;
	@Autowired
    private I18nResourceService i18nResourceService;
	@Autowired
	private I18nResourceDownloadService i18nResourceDownloadService;
	@Autowired
	private CachedResourceBundle cachedResourceBundle;
	@Autowired
	private LockService lockService;
	/**
	 * 用于国际化资源同步到客户端缓存
	 * @return
	 * 		Map<tenantId, Map<moduleId, Map<language, Map<k, v>>>>
	 */
	public Result<Map<String, Map<String, Map<String, Map<String, String>>>>> downLoadResources(String moduleCodes, String useGetAllModule) {
		log.debug("<-----------远程获取remoteMessage 入参 moduleCodes: {}, useGetAllModule:{}------------->", moduleCodes , useGetAllModule);
		Map<String, Map<String, Map<String, Map<String, String>>>> resourcesMap = new HashMap<>();
		List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(Constants.DEFAULT_TENANT);
		Map<String, Set<String>> moduleSet = getModuleCodes(moduleCodes, useGetAllModule);
		for (Map.Entry<String, Set<String>> moduleEntry : moduleSet.entrySet()) {
			LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
	        		.in(I18nIndexPO::getModuleCode, moduleEntry.getValue())
	        		.eq(I18nIndexPO::getTenantId, moduleEntry.getKey());
			List<I18nIndexPO> indexs = i18nIndexDao.selectList(queryWrapper);
			Map<String, Map<String, Map<String, String>>> moduleMap = new HashMap<>(); // <module|index, <language, <key, value>>>
			for (I18nIndexPO index : indexs) {
				Map<String, Map<String, String>> langMap = new HashMap<>(); // <language, <key, value>>
				for (I18nLanguagePO language : allLanguage) {
					KeyValuePairCollection resources = cachedResourceBundle.getResourceForSingleTenant(moduleEntry.getKey(), index.getModuleCode(), language.getLanguCode());
					if (resources != null && !resources.getKvs().isEmpty()) {
						langMap.put(language.getLanguCode(), resources.getKvs());
					}
				}
				moduleMap.put(index.getModuleCode() + Constants.STR_POINT_SHU + index.getModuleIndexCode(), langMap);
			}
			resourcesMap.put(moduleEntry.getKey(), moduleMap);
		}
        Result<Map<String, Map<String, Map<String, Map<String, String>>>>> result = new Result<>();
        result.setMessage(Constants.PARAM_SUCCESS);
        result.setData(resourcesMap);
        if (log.isDebugEnabled()) {
			log.debug("<-----------远程获取remoteMessage 出参 resultMessage{}------------->", result.getMessage());
			for (Map.Entry<String, Map<String, Map<String, Map<String, String>>>> dtEntry : result.getData().entrySet()) {
				log.debug("<-----------当前租户 {} ---------------->", dtEntry.getKey());
				Map<String, Map<String, Integer>> moduleResources = new HashMap<>();
				for (Map.Entry<String, Map<String, Map<String, String>>> moduleEntry : dtEntry.getValue().entrySet()) {
					Map<String, Integer> moduleSize = new HashMap<>();
					for (Map.Entry<String, Map<String, String>> languEntry : moduleEntry.getValue().entrySet()) {
						moduleSize.put(languEntry.getKey(),languEntry.getValue().size());
					}
					moduleResources.put(moduleEntry.getKey(), moduleSize);
				}
				log.debug("<---------------当前租户获取的远程资源 {}--------------->", JsonUtil.toJson(moduleResources));
			}
		}
		return result;
	}
	
	private Map<String, Set<String>> getModuleCodes(String moduleCodes, String useGetAllModule) {
		Map<String, Set<String>> moduleMap = new HashMap<>(); // <tenantId, Set<moduleId>>
        if (Boolean.valueOf(useGetAllModule)) {
        	List<I18nIndexPO> indexs = i18nIndexDao.selectList(new QueryWrapper<I18nIndexPO>());
        	for (I18nIndexPO index : indexs) {
        		Set<String> modules = moduleMap.get(index.getTenantId());
        		if (modules == null) {
        			modules = new HashSet<>();
        		}
        		modules.add(index.getModuleCode());
        		moduleMap.put(index.getTenantId(), modules);
        	}
        } else {
        	String[] mcs = moduleCodes.split(Constants.STR_POINT_DOU);
            for (String s : mcs) {
            	String[] vars = s.split(Constants.STR_POINT_SHU1);
            	String moduleId = vars[0];
            	String tenantId = vars.length == 1 ? Constants.DEFAULT_TENANT : vars[1];
            	Set<String> modules = moduleMap.get(tenantId);
        		if (modules == null) {
        			modules = new HashSet<>();
        		}
        		modules.add(moduleId);
        		moduleMap.put(tenantId, modules);
            }
        }
        return moduleMap;
	}

	public Result<Map<String, Map<String, String>>> judgeDownLoadResource(@RequestParam(value = "moduleCodes") String moduleCodes, @RequestParam(value = "useGetAllModule", required = false) String useGetAllModule) {
		// 客户端获取index入参
		log.debug("<------------远程获取remoteIndex 入参, moduleCodes {}--------------->", moduleCodes + " " + useGetAllModule);
		Result result = new Result();
		boolean locked = lockService.isLocked(Constants.I18N_CLUSTER_LOCK);
        if (locked) {
        	log.error("....... 请求Index失败, 国际化服务还在初始化过程, 请稍后重试 ......., 当前请求参数: {}", moduleCodes);
        	result.setCode(100107010);
            result.setMessage("....... 国际化服务还在初始化过程, 请稍后请求Index .......");
            return result;
        }
		
        if (moduleCodes == null || (moduleCodes.equals(Constants.STR_NO_SPACE))) {
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            return result;
        }
        Set<String> moduleSet = new HashSet<>();
        if (useGetAllModule != null && useGetAllModule.toUpperCase().equals(Constants.TRUE.toUpperCase())) {
        	LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda().select(I18nIndexPO::getModuleCode)
        			.groupBy(I18nIndexPO::getModuleCode);
            List<I18nIndexPO> i18nIndexPOS = i18nIndexDao.selectList(queryWrapper);
            i18nIndexPOS.forEach(i18nIndexPO -> {
                if(i18nIndexPO!=null && i18nIndexPO.getModuleCode()!=null){
                    moduleSet.add(i18nIndexPO.getModuleCode());
                }
            });
        } else {
        	List<String> list = new ArrayList<>();
            String[] mcs = moduleCodes.split(Constants.STR_POINT_DOU);
            if (mcs == null || mcs.length == 0) {
                result.setCode(100107008);
                result.setMessage(Constants.NO_MODULE_CODE);
                return result;
            }
            for (String s : mcs) {
                moduleSet.add(s);
            }
        }
        return i18nResourceDownloadService.judgeGetModuleResource2(moduleSet);
    }
    
}
