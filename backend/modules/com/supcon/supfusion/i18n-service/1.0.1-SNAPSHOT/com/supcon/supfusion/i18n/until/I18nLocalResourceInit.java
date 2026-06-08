package com.supcon.supfusion.i18n.until;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.supcon.supfusion.framework.cloud.common.constants.SystemConstant;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.common.until.ResourcePropertiesWrapper;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nLanguageDao;
import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.i18n.service.impl.I18nResourceServiceImpl;
import com.supcon.supfusion.i18n.service.impl.LockService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class I18nLocalResourceInit implements CommandLineRunner {

    @Autowired
    private I18nProperties i18nProperties;
    @Autowired
    private I18nLanguageDao i18nLanguageDao;
    @Autowired
    private I18nResourceDao i18nResourceDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private CachedResourceBundle cachedResourceBundle;
    @Autowired
    private I18nResourceServiceImpl i18nResourceService;
    @Autowired
	private LockService lockService;
    
    /*
     *将国际化服务的国际化资源 和 defaultModule的国际化资源加载进数据库
     *
     *issue: 通过先删后插的方式来初始化本地国际化数据, 修改时间的改变会导致排序错误
     */
    @Override
    public void run(String... strings) {
    	// 初始化缓存, 集群控制
        boolean acquire = lockService.acquire(Constants.I18N_CLUSTER_LOCK);
        if (acquire) {
        	try {
                RpcContext.getContext().setTenantId(SystemConstant.SYSTEM_TENANT_ID);
		        if (i18nProperties.getI18nResourceCode() != null && i18nProperties.getI18nResourceVersion() != null && i18nProperties.getI18nResourcePath() != null) {
		            log.info("开始执行初始化国际化自己国际化资源");
		            //处理国际化自己的国际化资源
		            String moduleCode = i18nProperties.getI18nResourceCode();
		            String versionCode = i18nProperties.getI18nResourceVersion();
		            String messageDir = i18nProperties.getI18nResourcePath();
		            MyFileUtils.execRescource(messageDir, moduleCode, versionCode, i18nProperties);
                    log.error("<----------- "+ moduleCode + " ----文件复制完成-------------->");
		            execPath(moduleCode, versionCode);
                    log.error("<----------- "+ moduleCode + " ----数据入库完成-------------->");
		        }
		        //处理系统默认的国际化资源
		        if (i18nProperties.getDefaultResourceCode() != null && i18nProperties.getDefaultResourceVersion() != null && i18nProperties.getDefaultResourcePath() != null) {
		            log.info("开始执行初始化默认国际化资源");
		            String moduleCode = i18nProperties.getDefaultResourceCode();
		            String versionCode = i18nProperties.getDefaultResourceVersion();
		            String messageDir = i18nProperties.getDefaultResourcePath();
		            MyFileUtils.execRescource(messageDir, moduleCode, versionCode, i18nProperties);
                    log.error("<----------- "+ moduleCode + " ----文件复制完成-------------->");
		            execPath(moduleCode, versionCode);
                    log.error("<----------- "+ moduleCode + " ----数据入库完成-------------->");
		        }
        		initCache();
			} finally {
				lockService.release(Constants.I18N_CLUSTER_LOCK);
			}
        }
    }
    
    // 初始化国际化资源和索引缓存
    private void initCache() {
    	LambdaQueryWrapper<I18nResourcePO> tenantQuery = new QueryWrapper<I18nResourcePO>().lambda().select(I18nResourcePO::getTenantId)
    			.groupBy(I18nResourcePO::getTenantId);
    	LambdaQueryWrapper<I18nIndexPO> moduleQuery = new QueryWrapper<I18nIndexPO>().lambda().select(I18nIndexPO::getModuleCode)
    			.eq(I18nIndexPO::getValid, Constants.ONE_STR)
    			.groupBy(I18nIndexPO::getModuleCode);
    	List<I18nResourcePO> tenants = i18nResourceDao.selectList(tenantQuery);
    	List<I18nIndexPO> modules = i18nIndexDao.selectList(moduleQuery);
    	Set<String> tenantIds = tenants.stream().map(I18nResourcePO::getTenantId).collect(Collectors.toSet());
    	Set<String> moduleIds = modules.stream().map(I18nIndexPO::getModuleCode).collect(Collectors.toSet());
    	for (String tenantId : tenantIds) {
    		for (String moduleId : moduleIds) {
    			List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
    			for (I18nLanguagePO language : allLanguage) {
                    cachedResourceBundle.flushResourceCacheByUpdate(tenantId, moduleId, language.getLanguCode());
    			}
        	}
    	}
    	putIndexCache();
    }
    
    private void putIndexCache() {
    	List<I18nIndexPO> indexList = i18nIndexDao.selectList(new QueryWrapper<I18nIndexPO>());
    	for (I18nIndexPO index : indexList) {
    		cachedResourceBundle.flushModuleIndexCache(index.getModuleCode(), index.getTenantId(), index.getModuleIndexCode());
    	}
    }

    private void execPath(String moduleCode, String versionCode) {
        String i18nPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + moduleCode + Constants.PATH;
        File resourceFiles = new File(i18nPath);
        List<I18nLanguagePO> languageEntities = i18nLanguageDao.selectList(new QueryWrapper<I18nLanguagePO>().lambda().eq(I18nLanguagePO::getTenantId, Constants.DEFAULT_TENANT));
        List<String> languageCodes = new ArrayList<>();
        for (I18nLanguagePO i18nLanguagePO : languageEntities) {
            languageCodes.add(i18nLanguagePO.getLanguCode());
        }
        if (resourceFiles.isDirectory() && resourceFiles.listFiles() != null) {
            List<I18nResourcePO> i18nResourceList = new ArrayList<>();
            File[] fs = resourceFiles.listFiles();
            for (File f : fs) {
                if (f.isFile()) {
                	readProperties(moduleCode, versionCode, f, i18nResourceList, languageCodes);
                }
            }
            boolean hasResource = i18nResourceList.size() > 0;
            i18nResourceService.saveToDB(moduleCode, versionCode, hasResource, i18nResourceList);
        }
    }

    private void readProperties(String moduleCode, String newVersionCode, File file, List<I18nResourcePO> resourceList, List<String> languageCodes) {
        //若是文件 判断是不是properties文件
        String destDirs = file.toString().substring(file.toString().length() - 10);//获取文件名后缀 "properties"
        String languageCode = file.toString().substring(file.toString().length() - 16, file.toString().length() - 11);//获取后缀前的 语言类型
        String destDirUpper = destDirs.toUpperCase();
        if (destDirUpper.equals(Constants.PROPERTIES)) {
            if (languageCodes.contains(languageCode)) {
                //文件名中当前语言不对应 就不存入数据库
                Map<String, String> singleLanguageResourceMap = ResourcePropertiesWrapper.readValue(file.toString());
                Date date = new Date();
                //当前map1中存有一种语言的properties文件的所有国际化键值对
                if (singleLanguageResourceMap != null) {
                    for (Object key : singleLanguageResourceMap.keySet()) {
                        I18nResourcePO i18nResourcePO = new I18nResourcePO();
                        i18nResourcePO.setLanguCode(languageCode);
                        i18nResourcePO.setModuleCode(moduleCode);
                        i18nResourcePO.setValid(Constants.ONE_STR);
                        i18nResourcePO.setModuleVersionCode(newVersionCode);
                        i18nResourcePO.setId(IDGenerator.newInstance().generate().longValue());
                        i18nResourcePO.setI18nKey(key.toString());
                        i18nResourcePO.setI18nValue(singleLanguageResourceMap.get(key));
                        i18nResourcePO.setTenantId(Constants.DEFAULT_TENANT);
                        i18nResourcePO.setCreator(Constants.ONE_STR);
                        i18nResourcePO.setCreateTime(date);
                        i18nResourcePO.setModifyTime(date);
                        resourceList.add(i18nResourcePO);
                    }
                }
            }
        }
    }

}