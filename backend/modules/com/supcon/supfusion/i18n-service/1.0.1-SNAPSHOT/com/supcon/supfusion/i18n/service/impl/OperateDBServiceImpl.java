package com.supcon.supfusion.i18n.service.impl;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.until.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.bo.UploadResourceBO;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nLanguageDao;
import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.I18nVersionDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nLanguagePO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.i18n.dao.po.I18nVersionPO;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import com.supcon.supfusion.i18n.service.OperateDBService;
import com.supcon.supfusion.i18n.until.CachedResourceBundle;
import com.supcon.supfusion.module.registry.ModuleEnum;

import lombok.extern.slf4j.Slf4j;

@ServiceApiService
@Slf4j
public class OperateDBServiceImpl implements OperateDBService {

    @Autowired
    private I18nVersionDao i18nVersionDao;
    @Autowired
    private I18nResourceDao i18nResourceDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private I18nLanguageDao i18nLanguageDao;
    @Autowired
    private I18nManagerService i18nManagerService;
    @Autowired
    private CachedResourceBundle cachedResourceBundle;
    @Autowired
    private I18nResourceServiceImpl i18nResourceService;
    @Autowired
    private I18nProperties i18nProperties;

    /**
     * 客户端本地资源上载的时候会调用这个方法
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String readPropertiesToDB(String moduleCode, String newVersionCode, String destDir) {
        //获取其file对象
        File file = new File(destDir);
        if (file.listFiles().length > 0) {
            File[] fs = file.listFiles();
            for (File f : fs) {
                if (f.isDirectory()) {
                    //若是目录，则递归打印该目录下的文件
                    //readPropertiesToDB(moduleCode, i18nVersionPO, f.toString());
                } else if (f.isFile()) {
                    //若是文件 判断是不是properties文件
                    String destDirs = f.toString().substring(f.toString().length() - 10, f.toString().length());//获取文件名后缀 "properties"
                    String languageCode = f.toString().substring(f.toString().length() - 16, f.toString().length() - 11);//获取后缀前的 语言类型
                    List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(Constants.DEFAULT_TENANT);
                    Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
                    String destDirUpper = destDirs.toUpperCase();
                    if (destDirUpper.equals(Constants.PROPERTIES)) {
                        if (languages.contains(languageCode)) { //文件名中当前语言不对应 就不存入数据库
                            Map map1 = ResourcePropertiesWrapper.readValue(f.toString());
                            //当前map1中存有一种语言的properties文件的所有国际化键值对
                            //判断当前是什么模式（20210114 当前上载的包中都是没有custom目录资源的）
                            if (i18nProperties.getProfile().equals(Constants.DEV_ENVIRO)) {
                                //当前是productDev   product->productDev  忽略上载上来的custom 直接使用上载上来的map资源插入数据库
                                //当前是productDev   productDev->productDev  直接使用上载上来的map资源插入数据库
                            } else if (!i18nProperties.getProfile().equals(Constants.DEV_ENVIRO)) {
                                //当前是product  product(A)->product(B)  A的custom覆盖B的custom A的非custom覆盖B的非custom
                                //当前是product  productDev(A)->product(B)  B的custom为准 其余的使用A的资源
                                //找到当前模块的custom目录
                                String moduleCustomFilePath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
                                MyFileUtils.createDir(moduleCustomFilePath);
                                File moduleCustomFileDir = new File(moduleCustomFilePath);
                                File[] moduleCustomFiles = moduleCustomFileDir.listFiles();
                                Map finalMap = new HashMap();
                                Arrays.asList(moduleCustomFiles).forEach(customFile -> {
                                    if (customFile.isFile()) {
                                        String customFileLanguageCode = customFile.toString().substring(customFile.toString().length() - 16, customFile.toString().length() - 11);//获取后缀前的 语言类型
                                        if (customFileLanguageCode.equals(languageCode)) {
                                            Map customFileMap = new HashMap();
                                            customFileMap = ResourcePropertiesWrapper.readValue(customFile.toString());
                                            if (!customFileMap.isEmpty()) {
                                                customFileMap.forEach((k1, v1) -> {
                                                    finalMap.put(k1, v1);
                                                });
                                            }
                                        }
                                    }
                                });
                                if (!finalMap.isEmpty()) {
                                    Iterator it = finalMap.entrySet().iterator();
                                    while (it.hasNext()) {
                                        Map.Entry entry = (Map.Entry) it.next();
                                        Object key = entry.getKey();
                                        if (key != null && finalMap.get(key) != null) {
                                            map1.put(key.toString(), finalMap.get(key));
                                        }
                                    }
                                }
                            }
                            if (map1 != null && map1.size() > 0) {
                                List<I18nResourcePO> list1 = new ArrayList<>();
                                Date date = new Date();
                                for (Object key : map1.keySet()) {
                                    I18nResourcePO i18nResourcePO = new I18nResourcePO();
                                    i18nResourcePO.setLanguCode(languageCode);
                                    i18nResourcePO.setModuleCode(moduleCode);
                                    i18nResourcePO.setValid(Constants.ONE_STR);
                                    i18nResourcePO.setModuleVersionCode(newVersionCode);
                                    i18nResourcePO.setId(IDGenerator.newInstance().generate().longValue());
                                    i18nResourcePO.setTenantId(Constants.DEFAULT_TENANT);
                                    i18nResourcePO.setI18nKey(key.toString());
                                    i18nResourcePO.setI18nValue((String) map1.get(key));
                                    i18nResourcePO.setCreateTime(date);
                                    i18nResourcePO.setCreator(Constants.ONE_STR);
                                    i18nResourcePO.setModifyTime(date);
                                    list1.add(i18nResourcePO);
                                }
                                this.saveBatch(list1);
                                cachedResourceBundle.flushResourceCacheByUpdate(Constants.DEFAULT_TENANT, moduleCode, languageCode);
                            }
                        }
                    }
                }
            }
        }
        //先清楚旧版本数据库数据
        LambdaQueryWrapper<I18nVersionPO> versionQuery = new QueryWrapper<I18nVersionPO>().lambda()
                .eq(I18nVersionPO::getModuleCode, moduleCode);
        I18nVersionPO oldVersionPO = i18nVersionDao.selectOne(versionQuery);
        if (oldVersionPO != null) {
            i18nResourceDao.delete(new QueryWrapper<I18nResourcePO>().lambda()
                    .eq(I18nResourcePO::getModuleVersionCode, oldVersionPO.getModuleVersionCode())
                    .eq(I18nResourcePO::getTenantId, Constants.DEFAULT_TENANT));
            i18nVersionDao.delete(versionQuery);
        }
        //存下版本号
        I18nVersionPO versionPO = new I18nVersionPO();
        versionPO.setId(IDGenerator.newInstance().generate().longValue());
        versionPO.setModuleCode(moduleCode);
        versionPO.setModuleVersionCode(newVersionCode);
        versionPO.setValid(Constants.ONE_STR);
        i18nVersionDao.insert(versionPO);
        //生成索引
        i18nIndexDao.delete(new QueryWrapper<I18nIndexPO>().lambda()
                .eq(I18nIndexPO::getModuleCode, moduleCode)
                .eq(I18nIndexPO::getTenantId, Constants.DEFAULT_TENANT));
        I18nIndexPO i18nIndexPO = new I18nIndexPO();
        i18nIndexPO.setId(IDGenerator.newInstance().generate().longValue());
        i18nIndexPO.setModuleCode(moduleCode);
        String moduleIndexCode = moduleCode + UUID.randomUUID();
        i18nIndexPO.setModuleIndexCode(moduleIndexCode);
        i18nIndexPO.setTenantId(Constants.DEFAULT_TENANT);
        i18nIndexPO.setValid(Constants.ONE_STR);
        i18nIndexDao.insert(i18nIndexPO);
        // 更新缓存索引
        cachedResourceBundle.flushModuleIndexCache(Constants.DEFAULT_TENANT, moduleCode, moduleIndexCode);
        return moduleIndexCode;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public String readPropertiesToDBAndCacheOneFile(UploadResourceBO uploadResource) {
        //获取其file对象
        File file = new File(uploadResource.getFilePath());
        if (file.listFiles().length > 0) {
            File[] fs = file.listFiles();

            for (File f : fs) {
                if (f.isDirectory()) {
                    //若是目录，则递归打印该目录下的文件
                    //readPropertiesToDB(moduleCode, i18nVersionPO, f.toString());
                } else if (f.isFile()) {
                    //若是文件 判断是不是properties文件
                    String destDirs = f.toString().substring(f.toString().length() - 10, f.toString().length());//获取文件名后缀 "properties"
                    String languageCode = f.toString().substring(f.toString().length() - 16, f.toString().length() - 11);//获取后缀前的 语言类型
                    String destDirUpper = destDirs.toUpperCase();
                    if (destDirUpper.equals(Constants.PROPERTIES)) {
                        List<I18nLanguagePO> allLangus= getAllLanguage(TenantUtil.getTenantId());
                        Set<String> languCodes = allLangus.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
                        if (uploadResource.getLanguageCode().equals(languageCode) && languCodes.contains(languageCode)) { //文件名中当前语言不对应 就不存入数据库
                            //先清楚旧版本数据库数据
                            LambdaQueryWrapper<I18nResourcePO> versionQuery = new QueryWrapper<I18nResourcePO>().lambda()
                                    .eq(I18nResourcePO::getModuleCode, uploadResource.getModuleId())
                                    .eq(I18nResourcePO::getLanguCode, uploadResource.getLanguageCode());
                            List<I18nResourcePO> modelVersions = i18nResourceDao.selectList(versionQuery);
                            if (!modelVersions.isEmpty()) {
                                Set<String> oldVersions = modelVersions.stream().map(I18nResourcePO::getModuleVersionCode).collect(Collectors.toSet());
                                oldVersions.remove(null);
                                log.info("上载properties--delete--oldVersions:" + oldVersions.toString());
                                log.info("上载properties--delete--uploadResource:" + uploadResource.toString());
                                i18nResourceDao.delete(new QueryWrapper<I18nResourcePO>().lambda()
                                        .in(I18nResourcePO::getModuleVersionCode, oldVersions)
                                        .eq(I18nResourcePO::getLanguCode, uploadResource.getLanguageCode()));
                            }
                            Map<String, String> map1 = ResourcePropertiesWrapper.readValue(f.toString());
                            // 当前map1中存有一种语言的properties文件的所有国际化键值对
                            //判断当前是什么模式（20210114 当前上载的包中都是没有custom目录资源的）
                            if (i18nProperties.getProfile().equals(Constants.DEV_ENVIRO)) {
                                //当前是productDev   product->productDev  忽略上载上来的custom 直接使用上载上来的map资源插入数据库
                                //当前是productDev   productDev->productDev  直接使用上载上来的map资源插入数据库
                            } else if (!i18nProperties.getProfile().equals(Constants.DEV_ENVIRO)) {
                                //当前是product  product(A)->product(B)  A的custom覆盖B的custom A的非custom覆盖B的非custom
                                //当前是product  productDev(A)->product(B)  B的custom为准 其余的使用A的资源
                                //找到当前模块的custom目录
                                String moduleCustomFilePath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + uploadResource.getModuleId() + Constants.PATH;
                                MyFileUtils.createDir(moduleCustomFilePath);
                                File moduleCustomFileDir = new File(moduleCustomFilePath);
                                File[] moduleCustomFiles = moduleCustomFileDir.listFiles();
                                Map finalMap = new HashMap();
                                Arrays.asList(moduleCustomFiles).forEach(customFile -> {
                                    if (customFile.isFile()) {
                                        String customFileLanguageCode = customFile.toString().substring(customFile.toString().length() - 16, customFile.toString().length() - 11);//获取后缀前的 语言类型
                                        if (customFileLanguageCode.equals(languageCode)) {
                                            Map customFileMap = new HashMap();
                                            customFileMap = ResourcePropertiesWrapper.readValue(customFile.toString());
                                            if (!customFileMap.isEmpty()) {
                                                customFileMap.forEach((k1, v1) -> {
                                                    finalMap.put(k1, v1);
                                                });
                                            }
                                        }
                                    }
                                });
                                if (!finalMap.isEmpty()) {
                                    Iterator it = finalMap.entrySet().iterator();
                                    while (it.hasNext()) {
                                        Map.Entry entry = (Map.Entry) it.next();
                                        Object key = entry.getKey();
                                        if (key != null && finalMap.get(key) != null) {
                                            map1.put(key.toString(), finalMap.get(key).toString());
                                        }
                                    }
                                }
                            }
                            if (map1 != null && map1.size() > 0) {
                                IDGenerator idGenerator = IDGenerator.newInstance();
                                List<I18nResourcePO> list1 = new ArrayList<>(map1.size());
                                Map<String, String> kvs = new HashMap<>();
                                Date date = new Date();
                                for (String key : map1.keySet()) {
                                    I18nResourcePO i18nResourcePO = new I18nResourcePO();
                                    i18nResourcePO.setLanguCode(languageCode);
                                    i18nResourcePO.setModuleCode(uploadResource.getModuleId());
                                    i18nResourcePO.setValid(Constants.ONE_STR);
                                    i18nResourcePO.setModuleVersionCode(uploadResource.getVersionCode());
                                    Long id = idGenerator.generate().longValue();
                                    // ID如果会重复需要在框架层面解决
                                    /*while (idSet.contains(id)) {
                                        id = idGenerator.generate().longValue();
                                    }
                                    idSet.add(id);*/
                                    i18nResourcePO.setId(id);
                                    i18nResourcePO.setI18nKey(key);
                                    i18nResourcePO.setI18nValue(map1.get(key));
                                    i18nResourcePO.setTenantId(Constants.DEFAULT_TENANT);
                                    i18nResourcePO.setCreateTime(date);
                                    i18nResourcePO.setCreator(Constants.ONE_STR);
                                    i18nResourcePO.setModifyTime(date);
                                    kvs.put(key, map1.get(key));
                                    list1.add(i18nResourcePO);
                                }
                                log.info("------------------->开始存入数据库， 待保存数据为：语言：{}, 国际化长度：{}:{}<-------------------", uploadResource.getLanguageCode(), map1.values().size(), list1.size());
                                boolean result = this.saveBatch(list1);
                                cachedResourceBundle.flushResourceCacheByUpdate(Constants.DEFAULT_TENANT, uploadResource.getModuleId()
                                        , uploadResource.getLanguageCode());
                                log.info("------------------->数据存储完成, 存储结果： {}<-------------------", result);
                            }
                        }
                    }
                }
            }
        }
        i18nVersionDao.delete(new QueryWrapper<I18nVersionPO>().lambda()
                .eq(I18nVersionPO::getModuleCode, uploadResource.getModuleId()));
        //存下版本号
        I18nVersionPO versionPO = new I18nVersionPO();
        versionPO.setId(IDGenerator.newInstance().generate().longValue());
        versionPO.setModuleCode(uploadResource.getModuleId());
        versionPO.setModuleVersionCode(uploadResource.getVersionCode());
        versionPO.setValid(Constants.ONE_STR);
        i18nVersionDao.insert(versionPO);
        //生成索引
        I18nIndexPO i18nIndexPO = new I18nIndexPO();
        i18nIndexPO.setId(IDGenerator.newInstance().generate().longValue());
        i18nIndexPO.setModuleCode(uploadResource.getModuleId());
        String moduleIndexCode = uploadResource.getModuleId() + UUID.randomUUID();
        i18nIndexPO.setModuleIndexCode(moduleIndexCode);
        i18nIndexPO.setTenantId(Constants.DEFAULT_TENANT);
        i18nIndexPO.setValid(Constants.ONE_STR);
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
                .eq(I18nIndexPO::getModuleCode, uploadResource.getModuleId())
                .eq(I18nIndexPO::getTenantId, Constants.DEFAULT_TENANT);
        i18nIndexDao.delete(queryWrapper);
        i18nIndexDao.insert(i18nIndexPO);
        cachedResourceBundle.flushModuleIndexCache(uploadResource.getModuleId(), Constants.DEFAULT_TENANT, moduleIndexCode);
        return moduleIndexCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String readPropertiesToDBAndCacheFiles(UploadResourceBO uploadResource) {
        // 获取其file对象
        File file = new File(uploadResource.getFilePath());
        if (file.listFiles().length > 0) {
            File[] fs = file.listFiles();

            for (File f : fs) {
                // 判断是不是properties文件
                String destDirs = f.toString().substring(f.toString().length() - 10, f.toString().length()); // 获取文件后缀 "properties"
                String languageCode = f.toString().substring(f.toString().length() - 16, f.toString().length() - 11);  // 获取后缀前的语言类型
                String destDirUpper = destDirs.toUpperCase();
                if (destDirUpper.equals(Constants.PROPERTIES)) {
                    List<I18nLanguagePO> allLangus = getAllLanguage(TenantUtil.getTenantId());
                    Set<String> languCodes = allLangus.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
                    if(languCodes.contains(languageCode)) {  // 文件名中当前系统所有语言不对应， 就不存入数据库
                        // 先清除旧版本数据库数据
                        LambdaQueryWrapper<I18nResourcePO> versionQuery = new QueryWrapper<I18nResourcePO>().lambda()
                                .eq(I18nResourcePO::getModuleCode, uploadResource.getModuleId())
                                .eq(I18nResourcePO::getLanguCode, languageCode);
                        List<I18nResourcePO> modelVersions = i18nResourceDao.selectList(versionQuery);
                        if (!modelVersions.isEmpty()) {
                            Set<String> oldVersions = modelVersions.stream().map(I18nResourcePO::getModuleVersionCode).collect(Collectors.toSet());
                            i18nResourceDao.delete(new QueryWrapper<I18nResourcePO>().lambda()
                                    .in(I18nResourcePO::getModuleVersionCode, oldVersions)
                                    .eq(I18nResourcePO::getLanguCode, languageCode));
                        }
                        Map<String, String> map1 = ResourcePropertiesWrapper.readValue(f.toString());
                        // 当前map1中存有一种语言properties文件的所有国际化键值对
                        // 工程期包上载，所有模式下，均存入custom

                        if (map1 != null && map1.size() > 0) {
                            IDGenerator idGenerator = IDGenerator.newInstance();
                            List<I18nResourcePO> list1 = new ArrayList<>(map1.size());
                            Map<String, String> kvs = new HashMap<>();
                            Date date = new Date();
                            for (String key : map1.keySet()) {
                                I18nResourcePO i18nResourcePO = new I18nResourcePO();
                                i18nResourcePO.setLanguCode(languageCode);
                                i18nResourcePO.setModuleCode(uploadResource.getModuleId());
                                i18nResourcePO.setValid(Constants.ONE_STR);
                                i18nResourcePO.setModuleVersionCode(uploadResource.getVersionCode());
                                Long id = idGenerator.generate().longValue();
                                i18nResourcePO.setId(id);
                                i18nResourcePO.setI18nKey(key);
                                i18nResourcePO.setI18nValue(map1.get(key));
                                i18nResourcePO.setTenantId(Constants.DEFAULT_TENANT);
                                i18nResourcePO.setCreateTime(date);
                                i18nResourcePO.setCreator(Constants.ONE_STR);
                                i18nResourcePO.setModifyTime(date);
                                list1.add(i18nResourcePO);
                            }
                            log.info("------------------->开始存入数据库， 待保存数据为：语言：{}, 国际化长度：{}:{}<-------------------", languageCode, map1.values().size(), list1.size());
                            boolean result = this.saveBatch(list1);
                            cachedResourceBundle.flushResourceCacheByUpdate(Constants.DEFAULT_TENANT, uploadResource.getModuleId(),
                                    languageCode);
                            log.info("------------------->数据存储完成, 存储结果： {}<-------------------", result);
                        }


                    }
                }
            }
        }
        i18nVersionDao.delete(new QueryWrapper<I18nVersionPO>().lambda()
                .eq(I18nVersionPO::getModuleCode, uploadResource.getModuleId()));
        // 存下版本号
        I18nVersionPO versionPO = new I18nVersionPO();
        versionPO.setId(IDGenerator.newInstance().generate().longValue());
        versionPO.setModuleCode(uploadResource.getModuleId());
        versionPO.setModuleVersionCode(uploadResource.getVersionCode());
        versionPO.setValid(Constants.ONE_STR);
        i18nVersionDao.insert(versionPO);
        // 生成索引
        I18nIndexPO i18nIndexPO = new I18nIndexPO();
        i18nIndexPO.setId(IDGenerator.newInstance().generate().longValue());
        i18nIndexPO.setModuleCode(uploadResource.getModuleId());
        String moduleIndexCode = uploadResource.getModuleId() + UUID.randomUUID();
        i18nIndexPO.setModuleIndexCode(moduleIndexCode);
        i18nIndexPO.setTenantId(Constants.DEFAULT_TENANT);
        i18nIndexPO.setValid(Constants.ONE_STR);
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
                .eq(I18nIndexPO::getModuleCode, uploadResource.getModuleId())
                .eq(I18nIndexPO::getTenantId, Constants.DEFAULT_TENANT);
        i18nIndexDao.delete(queryWrapper);
        i18nIndexDao.insert(i18nIndexPO);
        cachedResourceBundle.flushModuleIndexCache(uploadResource.getModuleId(), Constants.DEFAULT_TENANT, moduleIndexCode);
        return moduleIndexCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveListToDB(List<I18nResourcePO> tenantResources) throws InterruptedException {
        long time = System.currentTimeMillis();
        final int MAX_DELETE_NUM = 1000;
        String tenantId = TenantUtil.getTenantId();
        log.info("++++++++++++++++++++++++ i18n excel import sql delete begin +++++++++++++++++++++++++" + System.currentTimeMillis());
        Set<String> i18nKeys = tenantResources.stream().map(I18nResourcePO::getI18nKey).collect(Collectors.toSet());
        List<String> subI18nKeys = new ArrayList<>(MAX_DELETE_NUM);
        Iterator<String> iterator = i18nKeys.iterator();
        while (iterator.hasNext()) {
            subI18nKeys.add(iterator.next());
            iterator.remove();
            if (subI18nKeys.size() >= MAX_DELETE_NUM || !iterator.hasNext()) {
                LambdaQueryWrapper<I18nResourcePO> deleteWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                        .in(I18nResourcePO::getI18nKey, subI18nKeys)
                        .eq(I18nResourcePO::getTenantId, tenantId);
                i18nResourceDao.delete(deleteWrapper);
                subI18nKeys.clear();
            }
        }
        log.info("++++++++++++++++++++++++ i18n excel import sql delete end +++++++++++++++++++++++++" + System.currentTimeMillis());
        log.info("++++++++++++++++++++++++ i18n excel import insert sql begin +++++++++++++++++++++++++" + System.currentTimeMillis());
        if (!tenantResources.isEmpty()) {
            this.saveBatch(tenantResources);
        }
        log.info("++++++++++++++++++++++++ i18n excel import insert sql end +++++++++++++++++++++++++" + System.currentTimeMillis());
        long time2 = System.currentTimeMillis();
        log.info("i8n excel import sql all time：" + (time2 - time) + "ms");
    }

    /*
     * 针对不同的数据库 批量插入时 参数集大小进行不同处理
     * mariadb
     * mysql     参数长度限制 mysql 参数限制 打开
     * oracle    修改每次批量插入的参数个数
     * sqlserver 修改每次批量插入的参数个数
     *
     */
    @Override
    public boolean saveBatch(List<I18nResourcePO> list) {
        return i18nResourceService.saveBatch(list);
    }

    @Override
    public void delete(Set<String> i18nModuleSet) {
        String tenantId = TenantUtil.getTenantId();
        LambdaQueryWrapper<I18nResourcePO> queryWrapper = new QueryWrapper<I18nResourcePO>()
                .lambda().eq(I18nResourcePO::getTenantId, tenantId)
                .in(I18nResourcePO::getModuleCode, i18nModuleSet);
        i18nResourceDao.delete(queryWrapper);
    }

    //更新模块索引的方法
    public String updateModuleIndexCode(String s) {
        String tenantId = TenantUtil.getTenantId();
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda()
                .eq(I18nIndexPO::getModuleCode, s)
                .eq(I18nIndexPO::getTenantId, tenantId);
        i18nIndexDao.delete(queryWrapper);
        String moduleIndexCode = s + UUID.randomUUID();
        I18nIndexPO i18nIndexPO = new I18nIndexPO();
        i18nIndexPO.setId(IDGenerator.newInstance().generate().longValue());
        i18nIndexPO.setModuleCode(s);
        i18nIndexPO.setModuleIndexCode(moduleIndexCode);
        i18nIndexPO.setTenantId(tenantId);
        i18nIndexPO.setCreator(Constants.TWO_STR);
        i18nIndexPO.setValid(Constants.ONE_STR);
        i18nIndexDao.insert(i18nIndexPO);
        // 更新缓存索引
        cachedResourceBundle.flushModuleIndexCache(tenantId, s, moduleIndexCode);
        return moduleIndexCode;
    }

    @Transactional
    @Override
    public String createI18nResourceInternal(Map i18n_value, String moduleId, String i18nKey, String tenantId, List<I18nResourcePO> i18nResourcePOs) {
        log.error("-------------------------> i18nKey = {} <-------------------------", i18nKey);
        if(moduleId.contains(Constants.STR_LINE)){
            moduleId = moduleId.split(Constants.STR_LINE)[0];
        }
        LambdaQueryWrapper<I18nResourcePO> oldI18nWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                .eq(I18nResourcePO::getI18nKey, i18nKey)
                .eq(I18nResourcePO::getTenantId, tenantId);
        i18nResourceDao.delete(oldI18nWrapper);
        //有记录 返回当前已经有这个key了
        Date date = new Date();
        for (Object key : i18n_value.keySet()) {
            I18nResourcePO i18nResourcePO = new I18nResourcePO();
            i18nResourcePO.setId(IDGenerator.newInstance().generate().longValue());
            i18nResourcePO.setI18nKey(i18nKey);
            i18nResourcePO.setLanguCode((String) key);
            i18nResourcePO.setI18nValue(i18n_value.get(key).toString());
            i18nResourcePO.setValid(Constants.ONE_STR);
            i18nResourcePO.setTenantId(tenantId);
            i18nResourcePO.setModuleCode(moduleId);
            i18nResourcePO.setCreateTime(date);
            i18nResourcePO.setModifyTime(date);
            //在添加当前数据进入
            i18nResourcePOs.add(i18nResourcePO);
        }
        if (i18nResourcePOs != null && i18nResourcePOs.size() > 0) {
            i18nResourceDao.saveBatch(i18nResourcePOs);
        }
        // 更新索引 (数据库和文件都更新)
        return updateModuleIndexCode(moduleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOneModuleAllResourceAndVersionAndIndex(String moduleCode) {
        String tenantId = TenantUtil.getTenantId();
        Set<String> i18nModuleSet = new HashSet<>();
        i18nModuleSet.add(moduleCode);
        this.delete(i18nModuleSet);
        LambdaQueryWrapper<I18nVersionPO> versionDelete = new QueryWrapper<I18nVersionPO>().lambda()
                .eq(I18nVersionPO::getModuleCode, moduleCode);
        i18nVersionDao.delete(versionDelete);
        LambdaQueryWrapper<I18nIndexPO> indexDelete = new QueryWrapper<I18nIndexPO>().lambda()
                .eq(I18nIndexPO::getModuleCode, moduleCode)
                .eq(I18nIndexPO::getTenantId, tenantId);
        i18nIndexDao.delete(indexDelete);
    }

    @Override
    public Map<String, List<I18nResourcePO>> selectByKey(List<String> i18nKeys) {
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        Map<String, List<I18nResourcePO>> resourceMap = new HashMap<>();
        List<String> allModuleCode = i18nManagerService.getAllModuleCode();
        for (String i18nKey : i18nKeys) {
            String moduleId = i18nKey.split(Constants.STR_POINT1)[0];
            if (!allModuleCode.contains(moduleId)) {
                moduleId = ModuleEnum.DEFAULT.getModuleId();
            }
            List<I18nResourcePO> pos = new ArrayList<>();
            for (I18nLanguagePO language : allLanguage) {
                KeyValuePairCollection kvs = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleId, language.getLanguCode());
                if (kvs != null && kvs.getKvs().get(i18nKey) != null) {
                    I18nResourcePO po = new I18nResourcePO();
                    po.setI18nKey(i18nKey);
                    po.setLanguCode(language.getLanguCode());
                    po.setI18nValue(kvs.getKvs().get(i18nKey));
                    po.setModuleCode(moduleId);
                    pos.add(po);
                }
            }
            resourceMap.put(i18nKey, pos);
        }
        return resourceMap;
    }

    @Override
    public List<String> selectKeysByKeys(Map<String, Object> whereMap, Boolean downAll) {
        List<String> i18n_keys = new ArrayList<>();
        Integer num = 1000;
        Integer count = 0;
        List<String> i18n_keys2 = (List<String>) whereMap.get(Constants.I18N_KEYS);
        if (i18n_keys2 != null && i18n_keys2.size() > 0) {
            count = i18n_keys2.size();
        }
        List<I18nResourcePO> i18nResourcePOs = new ArrayList<I18nResourcePO>();
        Map<String, Long> map = new HashMap<>();
        Integer currentPageNum = 1;
        if (count % num > 0) {
            currentPageNum = count / num + 1;
        } else {
            currentPageNum = count / num;
        }
        for (int q = 0; q < currentPageNum; q++) {
            if (q == currentPageNum - 1) {
                whereMap.put(Constants.OFFSET, 0);
                whereMap.put(Constants.LIMIT, count - q * num);
                whereMap.put(Constants.I18N_KEYS, i18n_keys2.subList(q * num, count));
                List<I18nResourcePO> i18nResourceEntities = i18nResourceDao.selectKeysByValuesReturnPO(whereMap);
                i18nResourcePOs.addAll(i18nResourceEntities);
            } else {
                whereMap.put(Constants.OFFSET, 0);
                whereMap.put(Constants.LIMIT, num);
                whereMap.put(Constants.I18N_KEYS, i18n_keys2.subList(q * num, (q + 1) * num));
                List<I18nResourcePO> i18nResourceEntities = i18nResourceDao.selectKeysByValuesReturnPO(whereMap);
                i18nResourcePOs.addAll(i18nResourceEntities);
            }
        }
        //结果按照 I18nResourcePO updateTime 字段排序
        if (i18nResourcePOs != null && i18nResourcePOs.size() > 0) {
            i18nResourcePOs.forEach(i18nResourcePO -> {
                map.put(i18nResourcePO.getI18nKey(), i18nResourcePO.getModifyTime().getTime());
            });
        }
        if (map.size() > 0) {
            Map<String, Long> sortValueDescMap = MapUtils.sortMapByValueDesc(map);
            sortValueDescMap.forEach((k, v) -> {
                i18n_keys.add(k);
            });
        }
        if (i18n_keys.size() > 0) {
            if (downAll) {
                return i18n_keys;
            } else {
                int pageSize = (Integer) whereMap.get(Constants.PAGE_SIZE);
                int pageNO = (Integer) whereMap.get(Constants.PAGE_NO);
                Integer countNum = 0;
                if (i18n_keys.size() % pageSize > 0) {
                    countNum = i18n_keys.size() / pageSize + 1;
                } else {
                    countNum = i18n_keys.size() / pageSize;
                }
                if (pageNO == countNum) {
                    return i18n_keys.subList((pageNO - 1) * pageSize, i18n_keys.size());
                } else {
                    return i18n_keys.subList((pageNO - 1) * pageSize, pageNO * pageSize);
                }
            }
        } else {
            return i18n_keys;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<I18nResourcePO> addOrUpdateList(String language, String moduleCode, Map<String, String> map) {
        String tenantId = TenantUtil.getTenantId();
        List<String> i18nKeys = new ArrayList<>();
        map.forEach((k, v) -> {
            i18nKeys.add(k);
        });
        deleteListByKeyAndLanguage(language, i18nKeys, moduleCode);
        List<I18nResourcePO> list = new ArrayList<>();
        Date date = new Date();
        map.forEach((k, v) -> {
            I18nResourcePO i18nResourcePO = new I18nResourcePO();
            i18nResourcePO.setId(IDGenerator.newInstance().generate().longValue());
            i18nResourcePO.setI18nKey(k);
            i18nResourcePO.setLanguCode(language);
            i18nResourcePO.setI18nValue(v);
            i18nResourcePO.setTenantId(tenantId);
            i18nResourcePO.setValid(Constants.ONE_STR);
            i18nResourcePO.setCreateTime(date);
            i18nResourcePO.setModifyTime(date);
            i18nResourcePO.setModuleCode(moduleCode);
            list.add(i18nResourcePO);
        });
        this.saveBatch(list);
        return list;
    }

    @Override
    public void deleteListByKeyAndLanguage(String language, List<String> i18nKeys, String moduleCode) {
        String tenantId = TenantUtil.getTenantId();
        LambdaQueryWrapper<I18nResourcePO> deleteWrapper = new QueryWrapper<I18nResourcePO>().lambda()
                .eq(I18nResourcePO::getModuleCode, moduleCode)
                .eq(I18nResourcePO::getLanguCode, language)
                .eq(I18nResourcePO::getTenantId, tenantId)
                .in(I18nResourcePO::getI18nKey, i18nKeys);
        i18nResourceDao.delete(deleteWrapper);
    }


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
}
