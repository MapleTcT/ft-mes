package com.supcon.supfusion.i18n.service.impl;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import com.supcon.supfusion.i18n.dao.*;
import com.supcon.supfusion.i18n.dao.po.*;
import com.supcon.supfusion.i18n.until.ZipUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.supcon.supfusion.framework.cloud.annotation.ServiceApiService;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.bo.UploadResourceBO;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.dto.KeyValuePairCollection;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.LocaleCustomUtil;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.common.until.ResourcePropertiesWrapper;
import com.supcon.supfusion.i18n.common.until.TokenUtil;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import com.supcon.supfusion.i18n.service.I18nResourceService;
import com.supcon.supfusion.i18n.service.OperateDBService;
import com.supcon.supfusion.i18n.service.api.MessageResourceService;
import com.supcon.supfusion.i18n.until.CachedResourceBundle;
import com.supcon.supfusion.i18n.until.UploadingUtil;
import com.supcon.supfusion.module.registry.ModuleEnum;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServiceApiService
public class MessageResourceServiceImpl implements MessageResourceService {

    @Autowired
    private CachedResourceBundle cachedResourceBundle;
    @Autowired
    private I18nProperties i18nProperties;
    @Autowired
    private I18nManagerService i18nManagerService;
    @Autowired
    private I18nLanguageDao i18nLanguageDao;
    @Autowired
    private I18nResourceDao i18nResourceDao;
    @Autowired
    private I18nVersionDao i18nVersionDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private ExcelDao excelDao;
    @Autowired
    private I18nTokenDao i18nTokenDao;
    @Autowired
    private OperateDBService operateDBService;
    @Autowired
    private I18nResourceService i18nResourceService;
    @Qualifier("i18nRedisTemplate")
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Map<String, String> MessageResourceSearchOne(String value, String langu) {
        String language = LocaleCustomUtil.localeChange(langu);
        String tenantId = TenantUtil.getTenantId();
        Set<String> moduleCodes = getModuleCodes(tenantId); // 获取当前租户下所有的模块编号
        Map<String, String> result = new HashMap<>();
        for (String moduleCode : moduleCodes) {
            KeyValuePairCollection cachedI18n = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleCode, language);
            if (cachedI18n != null && cachedI18n.getKvs() != null && !cachedI18n.getKvs().isEmpty()) {
                for (Map.Entry<String, String> entry : cachedI18n.getKvs().entrySet()) {
                    if(entry.getValue() == null)  log.info("-----------------null key and Value-------------- :" + entry.getKey() + " and " +entry.getValue());
                    if (null != entry.getValue() && entry.getValue().contains(value)) {

                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    private Set<String> getModuleCodes(String tenantId) {
        Set<String> moduleCodes = new HashSet<>();
        LambdaQueryWrapper<I18nIndexPO> queryWrapper = new QueryWrapper<I18nIndexPO>().lambda().in(I18nIndexPO::getTenantId, Constants.DEFAULT_TENANT, tenantId);
        List<I18nIndexPO> indexs = i18nIndexDao.selectList(queryWrapper);
        for (I18nIndexPO index : indexs) {
            moduleCodes.add(index.getModuleCode());
        }
        return moduleCodes;
    }

    @Override
    public Map<String, String> MessageResourceSearchAll(String value) {
        //缓存中查询 指定的结果并且返回
        String languageCode = RpcContext.getContext().getLanguage().toString();
        return MessageResourceSearchOne(value, languageCode);
    }

    @Override
    public Map<String, String> MessageResourceSearchOneMatchCase(String value, String langu) {
        String language = LocaleCustomUtil.localeChange(langu);
        String tenantId = TenantUtil.getTenantId();
        Map<String, String> result = new HashMap<>();
        Set<String> moduleCodes = getModuleCodes(tenantId); // 获取当前租户下所有的模块编号
        for (String moduleCode : moduleCodes) {
            KeyValuePairCollection cachedI18n = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleCode, language);
            if (cachedI18n != null) {
                for (Map.Entry<String, String> entry : cachedI18n.getKvs().entrySet()) {
                    if (entry.getValue().toLowerCase().contains(value.toLowerCase())) {
                        result.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> MessageResourceSearchAllMatchCase(String value) {
        //缓存中查询 指定的结果并且返回
        String language = RpcContext.getContext().getLanguage().toString();
        return MessageResourceSearchOneMatchCase(value, language);
    }

    @Override
    public Map<String, String> messageResourceSearchModuleMatchCase(String value, String moduleCode) {
        //缓存中查询 指定的结果并且返回
        String language = RpcContext.getContext().getLanguage().toString();
        String tenantId = TenantUtil.getTenantId();
        KeyValuePairCollection cachedI18n = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleCode, language);
        Map<String, String> result = new HashMap<>();
        if (cachedI18n != null) {
            for (Map.Entry<String, String> entry : cachedI18n.getKvs().entrySet()) {
                if (entry.getValue().toLowerCase().contains(value.toLowerCase())) {
                    result.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> MessageResourceGetByModuleCodeAllLanguage(String moduleCode) {
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        Map<String, Map<String, String>> result = new HashMap<>();
        for (I18nLanguagePO language : allLanguage) {
            KeyValuePairCollection cachedI18n = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleCode, language.getLanguCode());
            if (cachedI18n != null) {
                result.put(language.getLanguCode(), cachedI18n.getKvs());
            }
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> MessageResourceGetByModuleCodeOneLanguage(String moduleCode, String langu) {
        String language = RpcContext.getContext().getLanguage().toString();
        if (langu != null) {
            language = LocaleCustomUtil.localeChange(langu);
        }
        if (StringUtils.isEmpty(language)) {
            language = i18nProperties.getDefaultLanguage();
        }
        String tenantId = TenantUtil.getTenantId();
        Map<String, Map<String, String>> result = new HashMap<>();
        KeyValuePairCollection cachedI18n = cachedResourceBundle.getSingleResourceForMultipleTenant(tenantId, moduleCode, language);
        if (cachedI18n != null) {
            result.put(language, cachedI18n.getKvs());
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> MessageResourceGetByKeyAllLanguage(String i18nKey) {
        String moduleId = i18nKey.split(Constants.STR_POINT1)[0];
        boolean moduleExist = i18nManagerService.moduleExists(moduleId);
        if (!moduleExist) {
            moduleId = ModuleEnum.DEFAULT.getModuleId();
        }
        return MessageResourceGetByModuleCodeAllLanguage(moduleId);

    }

    @Override
    public Map<String, String> MessageResourceGetByKeyOneLanguage(String i18nKey, String langu) {
        String language = RpcContext.getContext().getLanguage().toString();
        if (langu != null) {
            language = LocaleCustomUtil.localeChange(langu);
        }
        if (StringUtils.isEmpty(language)) {
            language = i18nProperties.getDefaultLanguage();
        }
        String moduleId = i18nKey.split(Constants.STR_POINT1)[0];
        boolean moduleExist = i18nManagerService.moduleExists(moduleId);
        if (!moduleExist) {
            moduleId = ModuleEnum.DEFAULT.getModuleId();
        }
        Map<String, Map<String, String>> result = MessageResourceGetByModuleCodeOneLanguage(moduleId, language);
        return result.get(language);
    }

    @Override
    public Map<String, String> messageResourceGetByKeyOneLanguage(String i18nKey, String langu) {
        String language = RpcContext.getContext().getLanguage().toString();
        if (langu != null) {
            language = LocaleCustomUtil.localeChange(langu);
        }
        if (StringUtils.isEmpty(language)) {
            language = i18nProperties.getDefaultLanguage();
        }
        if (i18nKey.contains("\\"+Constants.STR_POINT) || i18nKey.contains(Constants.STR_POINT)) {
            String moduleId = i18nKey.split("\\"+Constants.STR_POINT)[0];
            Map<String, Map<String, String>> result = MessageResourceGetByModuleCodeOneLanguage(moduleId, language);
            return result.get(language);
        } else {
            return new HashMap<>();
        }
    }


    @Override
    public Map<String, Map<String, Object>> getAllLanguage() {
        LambdaQueryWrapper<I18nLanguagePO> queryWrapper = new QueryWrapper<I18nLanguagePO>().lambda()
                .eq(I18nLanguagePO::getTenantId, RpcContext.getContext().getTenantId())
                .eq(I18nLanguagePO::getValid, Constants.ONE_STR)
                .eq(I18nLanguagePO::getHasUsed, Constants.ONE_STR);
        List<I18nLanguagePO> languageEntities = i18nLanguageDao.selectList(queryWrapper);
        if (languageEntities.isEmpty()) {
            queryWrapper.eq(I18nLanguagePO::getTenantId, Constants.DEFAULT_TENANT);
            languageEntities = i18nLanguageDao.selectList(queryWrapper);
        }
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (I18nLanguagePO i18nLanguagePO : languageEntities) {
            Map<String, Object> mapDetail = new HashMap<>();
            if (i18nLanguagePO.getHasUsed().equals(Constants.ONE_STR)) {
                mapDetail.put(Constants.USED, true);
            } else {
                mapDetail.put(Constants.USED, false);
            }
            mapDetail.put(Constants.LANGU_TYPE, i18nLanguagePO.getLanguType());
            mapDetail.put(Constants.LANGU_NAME, i18nLanguagePO.getLanguName());
            result.put(i18nLanguagePO.getLanguCode(), mapDetail);
        }
        return result;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> messageResourceGetByModuleCodesAllLanguage(String[] moduleCodes) {
        Map<String, Map<String, Map<String, String>>> resultMap = new HashMap<>();
        for (String moduleCode : moduleCodes) {
            Map<String, Map<String, String>> singleCodeI18nResource = MessageResourceGetByModuleCodeAllLanguage(moduleCode);
            resultMap.put(moduleCode, singleCodeI18nResource);
        }
        return resultMap;
    }



    /**
     * 实体配置上载包资源上载
     */
    @Override
    public Result messageResourceUploadProFile(MultipartFile uploadFile, Map map) {
        log.info("-->serviceAPI receive one file upload start<------------时间: {}", System.currentTimeMillis());
        log.info("-----------文件名：{}", uploadFile.getName());
        //如果缺少参数直接返回
        Result result = new Result();
        try {
            UploadingUtil.getExcelUpLoadingState(excelDao);
        } catch (I18nException e) {
            //throw new I18nException(I18nErrorEnum.FILE_UPLOADING_ERROR);
            result.setCode(100107053);
            result.setMessage(Constants.FILE_UPLOADING_ERROR);
            log.error("serviceAPI messageResourceUploadResource 100107053:" + Constants.FILE_UPLOADING_ERROR);
            return result;
        }
        if (map.get(Constants.MODULE_CODE) == null || (map.get(Constants.MODULE_CODE)
                != null && map.get(Constants.MODULE_CODE).equals(Constants.STR_NO_SPACE))
        ) {
            //throw new I18nException(I18nErrorEnum.FILE_NO_MODULE_ERROR);
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            log.error("serviceAPI messageResourceUploadResource 100107008:" + Constants.NO_MODULE_CODE);
            return result;
        }
        if (map.get(Constants.MODULE_VERSION_CODE) == null || (map.get(Constants.MODULE_VERSION_CODE) != null
                && map.get(Constants.MODULE_VERSION_CODE).equals(Constants.STR_NO_SPACE))) {
            //throw new I18nException(I18nErrorEnum.FILE_NO_MODULE_AND_VERSION_ERROR);
            result.setCode(100107013);
            result.setMessage(Constants.NO_VERSION_CODE);
            log.error("serviceAPI messageResourceUploadResource 100107013:" + Constants.NO_VERSION_CODE);
            return result;
        }
        String moduleId = (String) map.get(Constants.MODULE_CODE);
        String versionCode = (String) map.get(Constants.MODULE_VERSION_CODE);
        boolean moduleExists = i18nManagerService.moduleExists(moduleId);
        //校验版本号格式是否正确
        if (!versionCode.substring(0, versionCode.length() - 12).equals(moduleId)) {
            //throw new I18nException(I18nErrorEnum.RESOURCE_VERSION_ERROR);
            result.setCode(100107057);
            result.setMessage(versionCode + Constants.STR_POINT_M + Constants.RESOURCE_VERSION_ERROR);
            log.error("serviceAPI messageResourceUploadResource 100107057:" + versionCode + Constants.STR_POINT_M + Constants.RESOURCE_VERSION_ERROR);
            return result;
        }
        String path = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + versionCode;
        MyFileUtils.createDir(path);
        String language = Constants.STR_NO_SPACE;
        try {
            //获取文件名
            String filename = uploadFile.getOriginalFilename();
            language = filename.substring(filename.length() - 16, filename.length() - 11);
            //校验文件后缀名
            String fileName = filename.substring(filename.length() - 10);
            String fileNameUpperCase = fileName.toUpperCase();
            if (Constants.PROPERTIES.equals(fileNameUpperCase)) {
                FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), new File(path + Constants.PATH + filename));
                // 删除历史版本数据
                //deleteHistoryFile(FilePathUtil.getFilePath(i18nConfig) + Constants.RESOURCE_FILE_PATH, versionCode);
            }
        } catch (Exception e) {
            throw new I18nException(I18nErrorEnum.FILE_TRANSPORT_ERROR, e);
        }
        redisTemplate.opsForList().leftPush("i18n_upload_queue", new UploadResourceBO(moduleId, language, versionCode, path));
        log.info("================请求来了 文件入队。"+System.currentTimeMillis());
        boolean unlock = redisTemplate.opsForValue().setIfAbsent("i18n_upload_lock", "1", 7200, TimeUnit.SECONDS);
        log.info("================上锁。"+System.currentTimeMillis());
        log.info("----------------->lock status: {}<-------------------", unlock);
        if (unlock) {
            new Thread() {
                public void run() {
                    try {
                        Object message = redisTemplate.opsForList().rightPop("i18n_upload_queue", 10, TimeUnit.SECONDS);
                        log.info("----------------->消息队列取出：{}", message == null ? "":message.toString());
                        while (message != null) {
                            UploadResourceBO uploadResource = (UploadResourceBO) message;
                            try {
                                operateDBService.readPropertiesToDBAndCacheOneFile(uploadResource);
                            } catch (Exception e) {
                                log.error("队列消费失败, 本次处理 moduleId: {}, moduleVersion: {}, language: {}", uploadResource.getModuleId(), uploadResource.getVersionCode(), uploadResource.getLanguageCode(), e);
                            } finally {
                                // 取下一个
                                message = redisTemplate.opsForList().rightPop("i18n_upload_queue", 10, TimeUnit.SECONDS);
                                log.info("----------------->循环体内消息队列取出：{}", message == null ? "":message.toString());
                            }
                        }
                    } finally {
                        // 释放锁
                        redisTemplate.delete("i18n_upload_lock");
                        log.info("================锁释放。"+System.currentTimeMillis());
                    }
                }
            }.start();
        }
        return result;
    }

    /**
     * 工程期 实体配置上载包资源上载
     */
    @Override
    public Result messageResourceUploadCustomFile(MultipartFile uploadFile, Map map) {
        log.info("-->serviceAPI receive one file upload start<---------时间： {}", System.currentTimeMillis());
        log.info("-----------文件名：{}",uploadFile.getName());
        Result result = new Result();
        try {
            // 判断当前是否有上传excel
            UploadingUtil.getExcelUpLoadingState(excelDao);
        } catch (I18nException e) {
            result.setCode(100107053);
            result.setMessage(Constants.FILE_UPLOADING_ERROR);
            log.error("serviceAPI messageResourceUploadResource 100107053:" + Constants.FILE_UPLOADING_ERROR);
            return result;
        }
        if (map.get(Constants.MODULE_CODE) == null || (map.get(Constants.STR_NO_SPACE) != null
                && map.get(Constants.MODULE_CODE).equals(Constants.STR_NO_SPACE))) {
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            log.error("serviceAPI messageResourceUploadResource 100107008:" + Constants.NO_MODULE_CODE);
            return result;
        }

        String moduleCode = (String) map.get(Constants.MODULE_CODE);
        String path = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode;
        MyFileUtils.createDir(path);
        try {
            // 获取文件名
            String filename = uploadFile.getOriginalFilename();
            // 校验文件后缀名
            String fileName = filename.substring(filename.length() - 3);
            String fileNameUpperCase = fileName.toUpperCase();
            if (Constants.ZIP.equals(fileNameUpperCase)) {
                // 接收文件到服务器端
                UUID uuid = UUID.randomUUID();
                String fileNewName = uuid + Constants.STR_NO_SPACE + Constants.STR_POINT + Constants.ZIP_LOW;
                String tempPath = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_IMPORT_PATH;
                MyFileUtils.createDir(path);

                // 1. 接收压缩文件
                FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), new File(tempPath + fileNewName));
                File zipFile = new File(tempPath + fileNewName);
                // 2. 解压到路径
                String newCustomPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH  + moduleCode;
                MyFileUtils.createDir(newCustomPath);
                ZipUtil.unzipToI18nDir2(zipFile, newCustomPath, true);
                // 3. 执行存储
                /*File unZipFile = new File(newCustomPath + Constants.PATH );
                File[] files = unZipFile.listFiles();*/

                UploadResourceBO uploadResourceBO = new UploadResourceBO(moduleCode, "", (String) map.get(Constants.MODULE_VERSION_CODE), newCustomPath);
                operateDBService.readPropertiesToDBAndCacheFiles(uploadResourceBO);

            }

        } catch (Exception e) {
            throw new I18nException(I18nErrorEnum.FILE_TRANSPORT_ERROR, e);
        }


        return result;
    }



    @Override
    public String initI18nKey(String prefix) {
        String value = Constants.CUSTOM;
        StringBuilder key = new StringBuilder(prefix);
        if (null != value && !Constants.STR_NO_SPACE.equals(value) && null != prefix && !Constants.STR_NO_SPACE.equals(prefix)) {
            key.append(Constants.STR_POINT);
            key.append(value);
            key.append(Constants.STR_POINT);
            key.append(Constants.STR_RANDOM);
//            key.append(System.currentTimeMillis());
            key.append(IDGenerator.newInstance().generate().longValue());
        }
        return key.toString();
    }

    /**
     * 根据模块名称创建国际化key值 返回一批当前模块的国际化key
     * moduleCode ：模块编码
     * num：生成该模块国际化key数量
     *
     * @param moduleCode
     * @param num
     */
    @Override
    public List<String> initI18nKeys(String moduleCode, Integer num) {
        String value = Constants.CUSTOM;
        List<String> list = new ArrayList<>();
        if (num > 10000) {
            throw new I18nException(I18nErrorEnum.NO_MORE_THAN_ERROR);
        }
        if (num < 0) {
            throw new I18nException(I18nErrorEnum.NO_LESS_THAN_ERROR);
        }
        Set<Integer> numSet = new HashSet<>(num);
        Random random = new Random();
        while (numSet.size() < num) {
            numSet.add(random.nextInt(10000));
        }
        List<Integer> numList = new ArrayList<>(numSet);
        if (num != null && num > 0) {
            StringBuilder key = new StringBuilder(moduleCode);
            for (int i = 0; i < num; i++) {
                if (null != moduleCode && !Constants.STR_NO_SPACE.equals(moduleCode)) {
                    key.append(Constants.STR_POINT);
                    key.append(value);
                    key.append(Constants.STR_POINT);
                    key.append(Constants.STR_RANDOM);
                    key.append(System.currentTimeMillis() + i + "" + numList.get(i));
                }
                list.add(key.toString());
                key.delete(0, key.length());
                key.append(moduleCode);
            }
        }
        return list;
    }

    @Override
//    @Transactional
    public Result messageResourceAddOrUpdateOne(Map<String, Object> map) {
        Result result = new Result();
        String moduleCode;
        String i18nKey;
        Map i18n_value;
        Map tokenMap = new HashMap();
        if (map != null && map.size() == 3 && map.get(Constants.I18N_VALUE) != null && map.get(Constants.I18N_KEY) != null && map.get(Constants.MODULE_CODE).toString() != null) {
            i18n_value = (Map<String, String>) map.get(Constants.I18N_VALUE);
            i18nKey = (String) map.get(Constants.I18N_KEY);
            i18nKey = i18nKey.replace(Constants.STR_SPACE, Constants.STR_NO_SPACE);
            moduleCode = map.get(Constants.MODULE_CODE).toString();
        } else {
            result.setCode(100107023);
            result.setMessage(Constants.PARAM_LOST);
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107023:" + Constants.PARAM_LOST + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        try {
            //查看当前系统是否正在上传资源
            UploadingUtil.getUpLoadingStateForAddOrUpdateOne(excelDao, i18nTokenDao, moduleCode);
        } catch (I18nException e) {
            result.setCode(100107053);
            result.setMessage(Constants.FILE_UPLOADING_ERROR);
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK);
            result.setData(tokenMap);
            log.info("100107053:" + Constants.FILE_UPLOADING_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK);
            return result;
        }
        //不是以模块名开头
        String moduleId = i18nKey.substring(Constants.ZERO_INT, i18nKey.indexOf(Constants.STR_POINT, Constants.ONE_INT));
        if (!moduleId.equals(moduleCode)) {
            result.setCode(100107055);
            result.setMessage(Constants.I18N_KEY_START_ERROR);
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107055:" + i18nKey + ":" + Constants.I18N_KEY_START_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        //超过长度限制
        if (i18nKey.length() > i18nProperties.getI18nKeyLengthNumDefa()) {
            result.setCode(100107035);
            result.setMessage(Constants.I18N_KEY_LENGTH_ERROR);
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107035:" + i18nKey + ":" + Constants.I18N_KEY_LENGTH_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        //校验是否有 英文字母 数字 下划线 点 之外的其他字符
        String regEx = Constants.I18N_KEY_REGEX;
        if (!i18nKey.matches(regEx)) {
            result.setCode(100107035);
            result.setMessage(Constants.I18N_KEY_ERROR);
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107035:" + i18nKey + ":" + Constants.I18N_KEY_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        //校验模块名是否正确 然后存入当前module_code
        //List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        //新增这个模块的国际化时校验当前模块是否已经注册到注册中心
//        if (!moduleCodes.contains(moduleCode)) {
//            result.setCode(100107024);
//            result.setMessage(Constants.MODULE_CODE_ERROR);
//            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
//            result.setData(tokenMap);
//            log.info("100107024:" + moduleCode + ":" + Constants.MODULE_CODE_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
//            return result;
//        }
        //校验value长度 大于500 提醒
        for (Object key : i18n_value.keySet()) {
            if (i18n_value.get(key).toString().length() > i18nProperties.getI18nValueLengthNumDefa()) {
                result.setCode(100107034);
                result.setMessage(Constants.I18N_VALUE_LENGTH_ERROR);
                tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
                result.setData(tokenMap);
                log.info("100107034:" + i18n_value.get(key) + ":" + Constants.I18N_VALUE_LENGTH_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
                return result;
            }
        }
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> languageEntities = i18nResourceService.getAllLanguage(tenantId);
        List<String> languages = new ArrayList<>();
        languageEntities.forEach(i18nLanguagePO -> {
            languages.add(i18nLanguagePO.getLanguCode());
        });
        for (Object key : i18n_value.keySet()) {
            if (!languages.contains(key)) {
                result.setCode(100107052);
                result.setMessage(key + ":" + Constants.FIND_NO_LANGUAGE);
                tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
                result.setData(tokenMap);
                log.info("100107052:" + key + ":" + Constants.FIND_NO_LANGUAGE + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
                return result;
            }
        }
        List<I18nResourcePO> i18nResourcePOs = new ArrayList<>();
        String moduleIndexCode = operateDBService.createI18nResourceInternal(i18n_value, moduleId, i18nKey, tenantId, i18nResourcePOs);
        //存入文件用于更新 同步文件
        Set<String> i18nKeys = new HashSet<>();
        i18nKeys.add(i18nKey);
        MyFileUtils.saveI18nKeyCode(i18nKeys, i18nProperties);
        //更新缓存
        cachedResourceBundle.flushModuleIndexCache(moduleId, tenantId, moduleIndexCode);
        for (String language : languages) {
            cachedResourceBundle.flushResourceCacheByUpdate(tenantId, moduleId, language);
        }
        result.setMessage(Constants.PARAM_SUCCESS);
        return result;
    }

    @Override
    @Transactional
    public Result messageResourceAddOrUpdateList(Map<String, String> map, String moduleId, String language) {
        Result result = new Result();
        if (moduleId == null) {
            log.error("messageResourceAddOrUpdateList:100107023:" + Constants.PARAM_LOST + ",moduleCode is null");
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        if (map == null || map.isEmpty()) {
            log.error("messageResourceAddOrUpdateList:100107023:" + Constants.PARAM_LOST + ",map no data!");
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        try {
            //查看当前系统是否正在上传excel资源
            UploadingUtil.getExcelUpLoadingState(excelDao);
        } catch (I18nException e) {
            result.setCode(100107053);
            result.setMessage(Constants.FILE_UPLOADING_ERROR);
            log.info("100107053:" + Constants.FILE_UPLOADING_ERROR);
            return result;
        }
        //查看当前模块是否正在上传资源

        //校验模块名是否正确 然后存入当前module_code
        boolean moduleExists = i18nManagerService.moduleExists(moduleId);
        if (!moduleExists) {
            throw new I18nException(I18nErrorEnum.MODULE_CODE_ERROR);
        }
        if (StringUtils.isEmpty(language)) {
            language = RpcContext.getContext().getLanguage().toString();
        }
        if (StringUtils.isEmpty(language)) {
            language = i18nProperties.getDefaultLanguage();
        }
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> languageEntities = i18nResourceService.getAllLanguage(tenantId);
        List<String> languages = new ArrayList<>();
        if (languageEntities != null && !languageEntities.isEmpty()) {
            languageEntities.forEach(I18nLanguagePO -> {
                languages.add(I18nLanguagePO.getLanguCode());
            });
        }
        if (!languages.contains(language)) {
            throw new I18nException(I18nErrorEnum.LANGUAGE_HAS_NO_ERROR);
        }
        //校验 模块code 当前是否当前模块正在上传
        I18nTokenPO i18nTokenPO = new I18nTokenPO();
        i18nTokenPO.setModuleCode(moduleId);
        LambdaQueryWrapper<I18nTokenPO> tokenQueryWrapper = new QueryWrapper<I18nTokenPO>().lambda()
                .eq(I18nTokenPO::getModuleCode, moduleId);
        I18nTokenPO tokenPO = i18nTokenDao.selectOne(tokenQueryWrapper);
        if (tokenPO != null && !tokenPO.getToken().equals(Constants.STR_NO_SPACE)) {
            result.setCode(100107046);
            result.setMessage(Constants.RESOURCE_IS_UPLOADING);
            log.info("100107046:" + moduleId + ":" + Constants.RESOURCE_IS_UPLOADING);
            return result;
            //throw new I18nException(I18nErrorEnum.RESOURCE_IS_UPLOADING);
        } else {
            i18nTokenPO.setId(IDGenerator.newInstance().generate().longValue());
            i18nTokenPO.setToken(TokenUtil.getToken(moduleId, moduleId + "000000000000"));
            i18nTokenPO.setValid(Constants.ONE_STR);
            i18nTokenPO.setHasLock(Constants.ONE_STR);
            i18nTokenDao.insert(i18nTokenPO);
        }
        //调用service 删除这一批 当前语言的国际化key 新增当前这一批 国际化key
        operateDBService.addOrUpdateList(language, moduleId, map);
        // 更新索引
        String moduleIndexCode = operateDBService.updateModuleIndexCode(moduleId);
        // 更新缓存
        cachedResourceBundle.flushModuleIndexCache(moduleId, tenantId, moduleIndexCode);
        cachedResourceBundle.flushResourceCacheByUpdate(tenantId, moduleId, language);
        //存入文件用于更新 同步文件
        Set<String> i18nKeys = new HashSet<>();
        map.forEach((k, v) -> {
            i18nKeys.add(k);
        });
        MyFileUtils.saveI18nKeyCode(i18nKeys, i18nProperties);
        //释放锁 删除token
        i18nTokenDao.deleteOne(i18nTokenPO.getId() + Constants.STR_NO_SPACE);
        return result;
    }

    @Override
    @Transactional
    public Result messageResourceDeleteKeys(String[] keys) {
        //查看当前系统是否正在上传资源
        Result result = new Result();
        try {
            UploadingUtil.getUpLoadingState(excelDao, i18nTokenDao);
        } catch (I18nException e) {
            result.setCode(100107053);
            result.setMessage(Constants.FILE_UPLOADING_ERROR);
            Map tokenMap = new HashMap();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK);
            result.setData(tokenMap);
            log.error("100107053:" + Constants.FILE_UPLOADING_ERROR);
            return result;
        }
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
        Arrays.asList(keys).forEach(i18n_key -> {
            //物理删除
            i18nResourceDao.delete(new UpdateWrapper<I18nResourcePO>().lambda().eq(I18nResourcePO::getI18nKey, i18n_key));
            //更新索引
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
            //更新缓存
            cachedResourceBundle.flushResourceCacheByDelete(module_code, languages, tenantId);
            //存入文件用于更新 同步文件
            Set<String> i18nKeys = new HashSet<>();
            i18nKeys.add(i18n_key);
            MyFileUtils.saveI18nKeyCode(i18nKeys, i18nProperties);
        });
        result.setMessage(Constants.PARAM_SUCCESS);
        return result;
    }

//    @Override
//    public Result messageResourceDeleteByModuleCodes(String[] moduleCodes) {
//        //查看当前系统是否正在上传资源
//        Result result = new Result();
//        try {
//            UploadingUtil.getUpLoadingState(excelDao, i18nTokenDao);
//        } catch (I18nException e) {
//            result.setCode(100107053);
//            result.setMessage(Constants.FILE_UPLOADING_ERROR);
//            Map tokenMap = new HashMap();
//            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK);
//            result.setData(tokenMap);
//            log.info("100107053:" + Constants.FILE_UPLOADING_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK);
//            return result;
//        }
//        List<String> allModuleCodes = i18nManagerService.getAllModuleCode();
//        if (moduleCodes != null && moduleCodes.length > 0) {
//            //校验这些模块是否都在模块注册服务中已经删除 如果没有提示先去模块注册服务中心将该模块code删除掉 再删除国际化资源
//            StringBuilder sb = new StringBuilder();
//            Arrays.asList(moduleCodes).forEach(code -> {
//                if (allModuleCodes.contains(code)) {
//                    sb.append(code);
//                    sb.append(Constants.STR_POINT_DOU);
//                }
//            });
//            if (!sb.toString().equals(Constants.STR_NO_SPACE)) {
//                //模块名仍然存在
//                String modulesStr = sb.toString().substring(0, sb.toString().length() - 1);
//                result.setCode(100107054);
//                result.setMessage(modulesStr + Constants.MODULE_CODE_EXIST_ERROR);
//                Map tokenMap = new HashMap();
//                tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK);
//                result.setData(tokenMap);
//                log.info("100107054:" + modulesStr + Constants.MODULE_CODE_EXIST_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK);
//                return result;
//            }
//            String tenantId = TenantUtil.getTenantId();
//            List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
//            Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
//            Arrays.asList(moduleCodes).forEach(moduleCode -> {
//                if (!moduleCode.equals(Constants.STR_NO_SPACE)) {
//                    //删除一个模块数据库中相关信息
//                    operateDBService.deleteOneModuleAllResourceAndVersionAndIndex(moduleCode);
//                    //删除properties文件
//                    String i18nPath = FilePathUtil.getFilePath(i18nConfig) + Constants.RESOURCE_FILE_PATH;
//                    MyFileUtils.deleteFileByModuleCodeNotCustom(i18nPath, moduleCode);
//                    String customPath = FilePathUtil.getFilePath(i18nConfig) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
//                    MyFileUtils.deleteFileByModuleCodeCustom(customPath, moduleCode);
//                    //删除某个模块的缓存
//                    cachedResourceBundle.flushResourceCacheByDelete(moduleCode, languages, tenantId);
//                }
//            });
//        }
//        result.setMessage(Constants.PARAM_SUCCESS);
//        return result;
//    }

    /**
     * @param excludeModuleCodes 不需要查询的模块编号
     */
    @Override
    public Map<String, Map<String, Map<String, String>>> getAllBIZModuleAllLanguageResource(String[] excludeModuleCodes) {
        List<String> bizModuleCodes = i18nManagerService.queryBIZModules();
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
        // resultMap结构为<moduleId, <language, <i18nKey, i18nValue>>>
        Map<String, Map<String, Map<String, String>>> resultMap = new HashMap<>();
        if (excludeModuleCodes != null) {
            for (String moduleCode : excludeModuleCodes) {
                bizModuleCodes.remove(moduleCode);
            }
        }
        for (String moduleId : bizModuleCodes) {
            Map<String, KeyValuePairCollection> moduleResources = cachedResourceBundle.getModuleResourceForMultipleTenant(tenantId, moduleId, languages);
            Map<String, Map<String, String>> innerMap = new HashMap<>(8);
            for (Map.Entry<String, KeyValuePairCollection> entry : moduleResources.entrySet()) {
                innerMap.put(entry.getKey(), entry.getValue().getKvs());
            }
            resultMap.put(moduleId, innerMap);
        }
        return resultMap;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> getAllSystemModuleAllLanguageResource(String[] excludeModuleCodes) {
        String tenantId = TenantUtil.getTenantId();
        List<String> systemModuleCodes = i18nManagerService.querySystemModules();
        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
        // resultMap结构为<moduleId, <language, <i18nKey, i18nValue>>>
        Map<String, Map<String, Map<String, String>>> resultMap = new HashMap<>();
        if (excludeModuleCodes != null) {
            for (String moduleCode : excludeModuleCodes) {
                systemModuleCodes.remove(moduleCode);
            }
        }
        for (String moduleId : systemModuleCodes) {
            Map<String, KeyValuePairCollection> moduleResources = cachedResourceBundle.getModuleResourceForMultipleTenant(tenantId, moduleId, languages);
            Map<String, Map<String, String>> innerMap = new HashMap<>(8);
            for (Map.Entry<String, KeyValuePairCollection> entry : moduleResources.entrySet()) {
                innerMap.put(entry.getKey(), entry.getValue().getKvs());
            }
            resultMap.put(moduleId, innerMap);
        }
        return resultMap;
    }

    // TODO 内存溢出风险
    @Override
    public Map<String, Map<String, Map<String, String>>> getAllModuleAllLanguageResource() {
        Map<String, Map<String, Map<String, String>>> systemModuleResource = getAllSystemModuleAllLanguageResource(null);
        Map<String, Map<String, Map<String, String>>> bizModuleResources = getAllBIZModuleAllLanguageResource(null);
        systemModuleResource.putAll(bizModuleResources);
        return systemModuleResource;
    }

    @Override
    public Map<String, Map<String, Map<String, String>>> getModulesResource(String[] moduleCodes) {
        Map<String, Map<String, Map<String, String>>> resultMap = new HashMap<>();
        if (moduleCodes == null) {
            return resultMap;
        }
        String tenantId = TenantUtil.getTenantId();
        List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
        Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
        for (String moduleId : moduleCodes) {
            Map<String, KeyValuePairCollection> moduleResources = cachedResourceBundle.getModuleResourceForMultipleTenant(tenantId, moduleId, languages);
            Map<String, Map<String, String>> innerMap = new HashMap<>(8);
            for (Map.Entry<String, KeyValuePairCollection> entry : moduleResources.entrySet()) {
                innerMap.put(entry.getKey(), entry.getValue().getKvs());
            }
            resultMap.put(moduleId, innerMap);
        }
        return resultMap;
    }

    @Override
    public Map<String, Map<String, String>> downloadFiles(String moduleCode) {
        Map<String, Map<String, String>> resultMap = new HashMap<>();
        if (!i18nProperties.getProfile().equals(Constants.PRO_ENVIRO)) {
            //System.out.println("当前环境是productDev环境。");
            //Map<String, Map<String, String>> resultMap = new HashMap<>();
            String tenantId = TenantUtil.getTenantId();
            List<I18nLanguagePO> allLanguage = i18nResourceService.getAllLanguage(tenantId);
            Set<String> languages = allLanguage.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
            boolean moduleExist = i18nManagerService.moduleExists(moduleCode);
            if (!moduleExist) {
                moduleCode = ModuleEnum.DEFAULT.getModuleId();
            }
            Map<String, KeyValuePairCollection> moduleResources = cachedResourceBundle.getModuleResourceForMultipleTenant(tenantId, moduleCode, languages);
            for (Map.Entry<String, KeyValuePairCollection> entry : moduleResources.entrySet()) {
                resultMap.put(entry.getKey(), entry.getValue().getKvs());
            }
            //return resultMap;
        } else if (i18nProperties.getProfile().equals(Constants.PRO_ENVIRO)) {
            //TODO  从文件中获取 当前非custom  目录文件
            //System.out.println("当前环境是product环境。");
            //Map<String, Map<String, String>> resultMap = new HashMap<>();
            Map<String, Map<String, String>> resultMap2 = new HashMap<>();
            //获取文件存储路径
            String i18nFilePath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH;
            //获取对应的文件夹名称
            File file = new File(i18nFilePath);
            if (file != null && file.listFiles() != null && file.listFiles().length > 0) {
                AtomicReference<String> constVersion = new AtomicReference<>(Constants.STR_NO_SPACE);
                //获取最新版本文件目录
                File[] files = file.listFiles();
                //遍历目录查找以模块名命名的资源文件夹
                String finalModuleCode = moduleCode;
                Arrays.asList(files).forEach(oneFile -> {
                    if (oneFile.getName().equals(finalModuleCode)) {
                        constVersion.set(oneFile.getName());
                    }
                });
                if (!constVersion.get().equals(Constants.STR_NO_SPACE)) {
                    //当前目录下有需要的最新版本号的资源文件
                    String i18nFileVersionPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + constVersion.get() + Constants.PATH;
                    File i18nFileVersionPathFiles = new File(i18nFileVersionPath);
                    if (i18nFileVersionPathFiles != null && i18nFileVersionPathFiles.listFiles() != null && i18nFileVersionPathFiles.listFiles().length > 0) {
                        File[] lastPropertiesFiles = i18nFileVersionPathFiles.listFiles();
                        for (int i = 0; i < lastPropertiesFiles.length; i++) {
                            if (lastPropertiesFiles[i].getName().contains(Constants.STR_LINE)) {
                                String language = lastPropertiesFiles[i].getName().substring(moduleCode.length() + 1, moduleCode.length() + 6);
                                Map map1 = ResourcePropertiesWrapper.readValue(i18nFileVersionPath + lastPropertiesFiles[i].getName());
                                resultMap.put(language, map1);
                            }
                        }
                    }
                }
                if (!constVersion.get().equals(Constants.STR_NO_SPACE)) {
                    //当前目录下有需要的最新版本号的资源文件
                    String i18nFileVersionPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + constVersion.get() + Constants.PATH;
                    File i18nFileVersionPathFiles = new File(i18nFileVersionPath);
                    if (i18nFileVersionPathFiles != null && i18nFileVersionPathFiles.listFiles() != null && i18nFileVersionPathFiles.listFiles().length > 0) {
                        File[] lastPropertiesFiles = i18nFileVersionPathFiles.listFiles();
                        //直接压缩当前目录到临时文件目录
                        for (int i = 0; i < lastPropertiesFiles.length; i++) {
                            if (lastPropertiesFiles[i].getName().contains(Constants.STR_LINE)) {
                                String language = lastPropertiesFiles[i].getName().substring(moduleCode.length() + 1, moduleCode.length() + 6);
                                Map map2 = ResourcePropertiesWrapper.readValue(i18nFileVersionPath + lastPropertiesFiles[i].getName());
                                if (resultMap2.get(language) != null) {
                                    Map map3 = resultMap2.get(language);
                                    if (map2 != null && map2.size() > 0) {
                                        map2.forEach((k, v) -> {
                                            map3.put(k, v);
                                        });
                                    }
                                    resultMap2.put(language, map3);
                                } else {
                                    resultMap2.put(language, map2);
                                }
                            }
                        }
                    }
                }
                //遍历目录查找最新版本号的文件夹 （版本号eg:i18n202010091052）
                Arrays.asList(files).forEach(oneFile -> {
                    if (oneFile.getName().contains(finalModuleCode) && oneFile.getName().startsWith(finalModuleCode) &&
                            (oneFile.getName().length() > 12 && oneFile.getName().substring(0, oneFile.getName().length() - 12).equals(finalModuleCode))) {
                        if (constVersion.get().equals(Constants.STR_NO_SPACE) || constVersion.get().equals(finalModuleCode)) {
                            constVersion.set(oneFile.getName());
                        } else {
                            if (Long.valueOf(oneFile.getName().replace(finalModuleCode, Constants.STR_NO_SPACE)) >
                                    Long.valueOf(constVersion.get().replace(finalModuleCode, Constants.STR_NO_SPACE))) {
                                constVersion.set(oneFile.getName());
                            }
                        }
                    }
                });
                if (!constVersion.get().equals(Constants.STR_NO_SPACE)) {
                    //当前目录下有需要的最新版本号的资源文件
                    String i18nFileVersionPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + constVersion.get() + Constants.PATH;
                    File i18nFileVersionPathFiles = new File(i18nFileVersionPath);
                    if (i18nFileVersionPathFiles != null && i18nFileVersionPathFiles.listFiles() != null && i18nFileVersionPathFiles.listFiles().length > 0) {
                        File[] lastPropertiesFiles = i18nFileVersionPathFiles.listFiles();
                        //直接压缩当前目录到临时文件目录
                        for (int i = 0; i < lastPropertiesFiles.length; i++) {
                            if (lastPropertiesFiles[i].getName().contains(Constants.STR_LINE)) {
                                String language = lastPropertiesFiles[i].getName().substring(moduleCode.length() + 1, moduleCode.length() + 6);
                                Map map2 = ResourcePropertiesWrapper.readValue(i18nFileVersionPath + lastPropertiesFiles[i].getName());
                                if (resultMap.get(language) != null) {
                                    Map map3 = resultMap.get(language);
                                    if (map2 != null && map2.size() > 0) {
                                        map2.forEach((k, v) -> {
                                            map3.put(k, v);
                                        });
                                    }
                                    resultMap.put(language, map3);
                                } else {
                                    resultMap.put(language, map2);
                                }
                            }
                        }
                    }
                }
                if (resultMap2 != null && resultMap2.size() > 0) {
                    resultMap2.forEach((k, v) -> {
                        if (resultMap != null && resultMap.size() > 0) {
                            resultMap2.forEach((k1, v1) -> {
                                if (k.equals(k1) && v.size() > 0) {
                                    v.forEach((k2, v2) -> {
                                        v1.put(k2, v2);
                                    });
                                }
                            });
                        }
                    });
                }
            }
            //return resultMap;
        }

        // 删除国际化值为null
        List<String> langu = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : resultMap.entrySet()) {
            List<String> keys = new ArrayList<>();
            for (Map.Entry<String, String> entry1 : entry.getValue().entrySet()) {
                if (entry1.getValue() == null) {
                    keys.add(entry1.getKey());
                }
            }
            if (keys.size() == entry.getValue().size()) {
                langu.add(entry.getKey());
            } else {
                for (String key : keys) {
                    entry.getValue().remove(key);
                }
            }
        }
        if(langu.size() > 0) {
            for (String l : langu) {
                resultMap.remove(l);
            }
        }
        return resultMap;
    }

    /**
     * 获取某个模块的国际化资源 运行期变化的资源（custom 目录下的资源）
     *
     * @param moduleCode 模块编码
     * @return 返回 zip 资源包
     */
    @Override
    public Map<String, Map<String, String>> downloadCustomFiles(String moduleCode) {
        Map<String, Map<String, String>> resultMap = new HashMap<>();
        //获取文件存储路径
        String fileCustomPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
        //获取对应的文件夹名称
        File file = new File(fileCustomPath);
        if (file != null && file.listFiles() != null && file.listFiles().length > 0) {
            AtomicReference<String> constVersion = new AtomicReference<>(Constants.STR_NO_SPACE);
            //获取最新版本文件目录
            File[] files = file.listFiles();
            // 获取当前系统所有语言类型
            List<I18nLanguagePO> allLanguages = i18nResourceService.getAllLanguage(TenantUtil.getTenantId());
            Set<String> languageCodes = allLanguages.stream().map(I18nLanguagePO::getLanguCode).collect(Collectors.toSet());
            //遍历目录查找以模块名命名的资源文件夹
            Arrays.asList(files).forEach(oneFile -> {
                String f = oneFile.toString();
                // 判断是否为properties文件
                String fileTypeUpper = f.substring(f.length() - 10, f.length()).toUpperCase();
                if (fileTypeUpper.equals(Constants.PROPERTIES)) {
                    // 判断语言类型是否正确
                    String fileLanguCode = f.substring(f.length() - 16, f.length() - 11);
                    if (languageCodes.contains(fileLanguCode)) {
                        //constVersion.set(oneFile.getName());
                        Map map = ResourcePropertiesWrapper.readValue(fileCustomPath + oneFile.getName());
                        resultMap.put(fileLanguCode, map);
                    }
                }
                /*if (oneFile.getName().equals(moduleCode)) {
                    constVersion.set(oneFile.getName());
                }*/
            });
            /*if (!constVersion.get().equals(Constants.STR_NO_SPACE)) {
                //当前目录下有需要的最新版本号的资源文件
                String i18nFileVersionPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + constVersion.get() + Constants.PATH;
                File i18nFileVersionPathFiles = new File(i18nFileVersionPath);
                if (i18nFileVersionPathFiles != null && i18nFileVersionPathFiles.listFiles() != null && i18nFileVersionPathFiles.listFiles().length > 0) {
                    File[] lastPropertiesFiles = i18nFileVersionPathFiles.listFiles();
                    //直接压缩当前目录到临时文件目录
                    for (int i = 0; i < lastPropertiesFiles.length; i++) {
                        if (lastPropertiesFiles[i].getName().contains(Constants.STR_LINE)) {
                            String language = lastPropertiesFiles[i].getName().substring(moduleCode.length() + 1, moduleCode.length() + 6);
                            Map map = ResourcePropertiesWrapper.readValue(i18nFileVersionPath + lastPropertiesFiles[i].getName());
                            resultMap.put(language, map);
                        }
                    }
                }
            }*/
        }
        return resultMap;
    }

    private void deleteHistoryFile(String path, String versionCode) {
        // 模块名与版本号
        String moduleId = versionCode.substring(0, versionCode.length() - 12);
        String version = versionCode.substring(versionCode.length() - 12);

        File file = new File(path);
        File[] files = file.listFiles();

        for (File f : files) {
            String fName = f.getName();
            if (f.isDirectory() && fName.length() > 12) {
                String fModuleCode = fName.substring(0, versionCode.length() - 12);
                String fVersionCode = fName.substring(versionCode.length() - 12);
                if (fModuleCode.equals(moduleId) && Long.valueOf(fVersionCode) < Long.valueOf(version)) {
                    log.info("<--------------------删除的文件夹为：" + fName + "-------------------------->");
                    remove(f);
                    f.delete();
                }
            }
        }
    }

    private void remove(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i ++) {
                if (files[i].isFile()) {
                    log.info("<--------------------删除的文件为：" +  files[i].getName() + "-------------------------->");
                    files[i].delete();
                } else if (files[i].isDirectory()) {
                    remove(files[i]);
                }
                files[i].delete();
            }
        }
    }
}