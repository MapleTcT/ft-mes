package com.supcon.supfusion.i18n.service.impl;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nTokenDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nTokenPO;
import com.supcon.supfusion.i18n.service.I18nResourceDownloadService;
import com.supcon.supfusion.i18n.until.CachedResourceBundle;
import com.supcon.supfusion.i18n.until.ResourceZipWrapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StopWatch;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Service
@Slf4j
public class I18nResourceDownloadServiceImpl implements I18nResourceDownloadService {

    @Autowired
    private I18nTokenDao i18nTokenDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private CachedResourceBundle cachedResourceBundle;
    @Autowired
    private I18nProperties i18nProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult judgeGetModuleResource(String moduleCode) {
        //如果缺少参数直接返回
        if (moduleCode.equals(Constants.STR_NO_SPACE)) {
            throw new I18nException(I18nErrorEnum.FILE_NO_MODULE_ERROR);
        }
        String tenantId = TenantUtil.getTenantId();
        //查询对应索引
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
        		.eq(I18nIndexPO::getModuleCode, moduleCode)
        		.eq(I18nIndexPO::getTenantId, tenantId);
        I18nIndexPO i18nIndexPO = i18nIndexDao.selectOne(queryWrapper);
        if (i18nIndexPO != null && !i18nIndexPO.getModuleCode().equals(Constants.STR_NO_SPACE) && !i18nIndexPO.getModuleIndexCode().equals(Constants.STR_NO_SPACE)) {
            //创建令牌 不上锁 多个服务同时可以请求
            String token = moduleCode + UUID.randomUUID();
            I18nTokenPO tokenPO = new I18nTokenPO();
            tokenPO.setId(IDGenerator.newInstance().generate().longValue());
            tokenPO.setModuleCode(moduleCode);
            tokenPO.setToken(token);
            tokenPO.setHasLock(Constants.ZERO_STR);
            tokenPO.setValid(Constants.ONE_STR);
            i18nTokenDao.add(tokenPO);
            Map resoultMap = new HashMap();
            List<Map> list = new ArrayList<>();
            resoultMap.put(Constants.MODULE_CODE, moduleCode);
            resoultMap.put(Constants.TOKEN, token);
            resoultMap.put(Constants.MODULE_INDEX_CODE, i18nIndexPO.getModuleIndexCode());
            list.add(resoultMap);
            //返回索引和令牌
            return new PageResult(list, Constants.ZERO_INT, Constants.ZERO_INT, Constants.ZERO_INT);
        }
        return new PageResult();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageResult getModuleResource(Map map) {
        String moduleCode = (String) map.get(Constants.MODULE_CODE);
        String token = (String) map.get(Constants.TOKEN);
        I18nTokenPO i18nTokenPO = new I18nTokenPO();
        i18nTokenPO.setModuleCode(moduleCode);
        //查询是否有锁
        //校验 模块code  校验当前是否有其对应模块正在更新该模块的国际化资源
        I18nTokenPO tokenPO = i18nTokenDao.selectByModuleCodeAndValidOne(i18nTokenPO);
        if (tokenPO != null) { //当前有其对应模块正在更新该模块的国际化资源
            throw new I18nException(I18nErrorEnum.FILE_IS_TOKEN_ERROR);
        }
        I18nTokenPO i18nTokenPO2 = new I18nTokenPO();
        i18nTokenPO2.setToken(token);
        i18nTokenPO2.setModuleCode(moduleCode);
        //查询是否有锁
        //校验 模块code  校验token 和是否获得了锁  //校验模块名和 令牌
        I18nTokenPO tokenPO2 = i18nTokenDao.selectByModuleCodeAndValidZero(i18nTokenPO);
        if (tokenPO2 == null || (tokenPO2 != null && tokenPO2.getHasLock().equals(Constants.ONE_STR))) {
            //没有获得TOKEN 或者 对应的锁是锁上的状态
            throw new I18nException(I18nErrorEnum.FILE_NO_TOKEN_ERROR);
        }
        //生成对应文件的压缩包
        createFile(moduleCode);
        //创建压缩包
        File sourceFile = new File(FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + moduleCode + Constants.PATH + Constants.RESOURCE_FILE_PATH);
        String zipFullName = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + moduleCode + Constants.STR_POINT + Constants.ZIP_LOW;
        ResourceZipWrapper.createZip(sourceFile, zipFullName);
        return getPageResult(moduleCode, tokenPO2);
    }

    private PageResult getPageResult(String moduleCode, I18nTokenPO tokenPO2) {
        //删除对应的令牌
        i18nTokenDao.deleteOneByModuleCodeAndToken(tokenPO2);
        //删除该模块的临时目录中的文件
        try {
            MyFileUtils.deleteAnyone(FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + moduleCode + Constants.PATH);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        String tenantId = TenantUtil.getTenantId();
        //返回索引
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
        		.eq(I18nIndexPO::getModuleCode, moduleCode)
        		.eq(I18nIndexPO::getTenantId, tenantId);
        I18nIndexPO i18nIndexPO = i18nIndexDao.selectOne(queryWrapper);
        Map<String, String> moduleMap = new HashMap<>();
        List<Map<String, String>> list = new ArrayList<>();
        moduleMap.put(Constants.MODULE_CODE, moduleCode);
        moduleMap.put(Constants.MODULE_INDEX_CODE, i18nIndexPO.getModuleIndexCode());
        list.add(moduleMap);
        return new PageResult(list, Constants.ZERO_INT, Constants.ZERO_INT, Constants.ZERO_INT);
    }

    private void createFile(String moduleCode) {
        String newPath1 = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + moduleCode + Constants.PATH + Constants.RESOURCE_FILE_PATH + moduleCode + Constants.PATH;
        String newPath2 = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + moduleCode + Constants.PATH + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
        File zipFilePath1 = new File(newPath1);
        File zipFilePath2 = new File(newPath2);
        if (!zipFilePath1.exists()) {
            zipFilePath1.mkdirs();
        }
        if (!zipFilePath2.exists()) {
            zipFilePath2.mkdirs();
        }
        String oldPath1 = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + moduleCode;
        String oldPath2 = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode;
        try {
            MyFileUtils.copyFiles(oldPath1, newPath1);
            MyFileUtils.copyFiles(oldPath2, newPath2);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new I18nException(I18nErrorEnum.RESOURCE_FILE_COPY_ERROR);
        }
    }

    @Override
    public String getModulesResource(List list) {
        //为了防止同时有多个请求过来，每个请求多个模块的国际化资源需要为每一个请求创建对应的临时文件目录
        String tempFilePath = UUID.randomUUID() + Constants.PATH;
        for (Object code : list) {
            String moduleCode = (String) code;
            String newPath1 = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + Constants.RESOURCE_FILE_PATH + moduleCode + Constants.PATH;
            String newPath2 = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
            MyFileUtils.createDir(newPath1);
            MyFileUtils.createDir(newPath2);
            String oldPath1 = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + moduleCode;
            String oldPath2 = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode;
            MyFileUtils.createDir(oldPath1);
            MyFileUtils.createDir(oldPath2);
            try {
                MyFileUtils.copyFiles(oldPath1, newPath1);
                MyFileUtils.copyFiles(oldPath2, newPath2);
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new I18nException(I18nErrorEnum.RESOURCE_FILE_COPY_ERROR);
            }
        }
        //创建压缩包
        File sourceFile = new File(FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + Constants.RESOURCE_FILE_PATH);
        String zipFullName = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + Constants.I18N_STR + Constants.STR_POINT + Constants.ZIP_LOW;
        ResourceZipWrapper.createZip(sourceFile, zipFullName);
        //删除该模块的临时目录中的文件
        try {
            MyFileUtils.deleteAnyone(FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + Constants.RESOURCE_FILE_PATH);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return tempFilePath;
    }

    /**
     * 返回list的结构为
     * <moduleId, <i18nKey, i18nValue>>
     */
    @Override
    public PageResult<Map<String, Map<String, String>>> getModulesResourceKeyValues(List<String> moduleIds, String languageCode) {
    	String tenantId = TenantUtil.getTenantId();
        Map<String, Map<String, String>> i18nMap = new HashMap<>();
        for (String moduleId : moduleIds) {
            // 查询缓存
        	KeyValuePairCollection resource = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, languageCode);
        	if (resource != null) {
        		i18nMap.put(moduleId, resource.getKvs());
        	}
        }
        return new PageResult<>(Collections.singleton(i18nMap), Constants.ZERO_INT, Constants.ZERO_INT, Constants.ZERO_INT);
    }

    @Override
    public Result<Map<String, Map<String, String>>> judgeGetModuleResource2(Collection<String> moduleIds) {
        Result<Map<String, Map<String, String>>> result = new Result<>();
        Map<String, Map<String, String>> indexMap = new HashMap<>();
        if (!moduleIds.isEmpty()) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            List<I18nIndexPO> indexs = i18nIndexDao.selectList(new QueryWrapper<I18nIndexPO>().lambda().in(I18nIndexPO::getModuleCode, moduleIds));
            stopWatch.stop();
            log.debug("<--------------remoteIndex 查询时间---------------->", stopWatch.getTotalTimeMillis());
            for (I18nIndexPO index : indexs) {
                Map<String, String> innerMap = indexMap.get(index.getTenantId());
                if (innerMap == null) {
                    innerMap = new HashMap<>();
                }
                innerMap.put(index.getModuleCode(), index.getModuleIndexCode());
                indexMap.put(index.getTenantId(), innerMap);
            }
        }
        result.setData(indexMap);
        result.setMessage(Constants.PARAM_SUCCESS);
        if(log.isDebugEnabled()){
            // 远程获取 index 出参数
            log.debug("<------------远程获取remoteIndex-judgeGetModuleResource2-出参---------------->");
            for (Map.Entry<String, Map<String, String>> entry : result.getData().entrySet()) {
                log.debug("<--------当前租户 {}------>", entry.getKey());
                for (Map.Entry<String, String> entry1 : entry.getValue().entrySet()) {
                    log.debug("<---------模块Code-moduleCode {},模块Index-moduleIndex {} --------->", entry1.getKey(),entry1.getValue());
                }
                log.debug("-------------------");
            }
        }
        return result;
    }

    /**
     * 没地方用, 标记为过时
     */
    @Deprecated
    @Override
    public Result getModulesResourcesOpenApi(String[] moduleCodes) {
        Result result = new Result();
        Map<String, Object> i18nMap1 = new LinkedHashMap();
        //获取语言类型
        //List<I18nLanguagePO> languageEntities = i18nLanguageDao.selectList(new QueryWrapper<I18nLanguagePO>());
        /*List<String> moduleCodesSystem = i18nResourceService.getAllModuleCode();
        long time = System.currentTimeMillis();
        Map<String, Map<String, Map<String, String>>> cacheMap = cachedResourceBundle.getCachedModuleResourceBundles();
        for (String moduleCode : Arrays.asList(moduleCodes)) {
            if (moduleCodesSystem.contains(moduleCode)) {
                //遍历所有语言类型 按语言类型和模块code 查询 国际化key-value
                Map<String, Map<String, String>> i18nMap2 = new LinkedHashMap();
                //execData(languageEntities, moduleCode, i18nMap2);
                if (cacheMap != null && cacheMap.size() > 0) {
                    cacheMap.forEach((k, v) -> {
                        if (moduleCode.equals(k)) {
                            if (v != null && v.size() > 0) {
                                v.forEach((k1, v1) -> {
                                    if (k1 != null && v1 != null && v1.size() > 0) {
                                        i18nMap2.put(k1, v1);
                                    }
                                });
                            }
                        }
                    });
                }
                i18nMap1.put(moduleCode, i18nMap2);
            } else {
                i18nMap1.put(moduleCode, Constants.MODULE_CODE_ERROR);
            }
        }
        result.setData(i18nMap1);
        long time1 = System.currentTimeMillis();
        System.out.println("处理数据时间：" + (time1 - time));*/
        return result;
    }

    @Override
    public Result getModulesResourceIndexs(List<String> moduleCodes) {
        Result<Map<String, String>> result = new Result<>();
        Map<String, String> indexMap = new LinkedHashMap<>();
        String tenantId = TenantUtil.getTenantId();
        for (String moduleCode : moduleCodes) {
        	String moduleIndex = cachedResourceBundle.getModuleIndex(moduleCode, tenantId);
            indexMap.put(moduleCode, moduleIndex);
        }
        result.setData(indexMap);
        return result;
    }


}
