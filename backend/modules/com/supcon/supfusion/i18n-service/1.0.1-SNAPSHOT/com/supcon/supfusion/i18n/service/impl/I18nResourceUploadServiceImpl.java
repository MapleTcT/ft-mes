package com.supcon.supfusion.i18n.service.impl;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.common.tools.IDGenerator;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.ListUtils;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.common.until.ResourcePropertiesWrapper;
import com.supcon.supfusion.i18n.common.until.TokenUtil;
import com.supcon.supfusion.i18n.dao.ExcelDao;
import com.supcon.supfusion.i18n.dao.I18nIndexDao;
import com.supcon.supfusion.i18n.dao.I18nResourceDao;
import com.supcon.supfusion.i18n.dao.I18nTokenDao;
import com.supcon.supfusion.i18n.dao.I18nVersionDao;
import com.supcon.supfusion.i18n.dao.po.I18nIndexPO;
import com.supcon.supfusion.i18n.dao.po.I18nResourcePO;
import com.supcon.supfusion.i18n.dao.po.I18nTokenPO;
import com.supcon.supfusion.i18n.dao.po.I18nVersionPO;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import com.supcon.supfusion.i18n.service.I18nResourceUploadService;
import com.supcon.supfusion.i18n.service.OperateDBService;
import com.supcon.supfusion.i18n.until.CachedResourceBundle;
import com.supcon.supfusion.i18n.until.UploadingUtil;
import com.supcon.supfusion.i18n.until.ZipUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description:
 * @Author: ShenZhiqiang
 * @Date: Create in  11:16 2020/6/12
 * @Modified:
 */
@Service
@Slf4j
public class I18nResourceUploadServiceImpl extends ServiceImpl<I18nResourceDao, I18nResourcePO> implements I18nResourceUploadService {

    @Autowired
    private I18nVersionDao i18nVersionDao;
    @Autowired
    private I18nResourceDao i18nResourceDao;
    @Autowired
    private I18nTokenDao i18nTokenDao;
    @Autowired
    private I18nIndexDao i18nIndexDao;
    @Autowired
    private I18nManagerService i18nManagerService;
    @Autowired
    private OperateDBService operateDBService;
    @Autowired
    private ExcelDao excelDao;
    @Autowired
    private I18nProperties i18nProperties;
    @Autowired
    private CachedResourceBundle cachedResourceBundle;
    @Autowired
	private LockService lockService;




    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result judgePostModuleResource2(Map<String, String> moduleMap,String versionErrorSb) {
        //查看当前系统是否正在上传资源
        Result result = new Result();
        try {
            UploadingUtil.getExcelUpLoadingState(excelDao);
            boolean locked = lockService.isLocked(Constants.I18N_CLUSTER_LOCK);
            if (locked) {
                log.error("....... 请求Token失败, 国际化服务还在初始化过程, 请稍后重试 ......., 当前请求参数: {}", moduleMap);
                throw new I18nException(I18nErrorEnum.MODULE_VERSION_RESOLVE_ERROR);
            }
        } catch (I18nException e) {
            result.setCode(100107053);
            result.setMessage(Constants.FILE_UPLOADING_ERROR);
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK);
            tokenMap.put(Constants.HAS_TOKEN_MESSAGE, Constants.FILE_UPLOADING_ERROR);
            result.setData(tokenMap);
            log.info("100107053:" + Constants.FILE_UPLOADING_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK);
            return result;
        }
        
        Map<String, String> resultMap = new HashMap<>();
        List<String> moduleCodes = i18nManagerService.getModuleEnumModuleCode();
        StringBuilder sb = new StringBuilder();
        moduleMap.forEach((moduleCode, versionCode) -> {
            //校验模块名 防止下面查询索引为空 仍去进行后续无用查询
            if (!moduleCodes.contains(moduleCode)) {
                sb.append(moduleCode);
                sb.append(Constants.STR_POINT_DOU);
            }
        });
        if (!sb.toString().equals(Constants.STR_NO_SPACE)) {
            StringBuilder sb2 = new StringBuilder();
            /*List<String> moduleCodesAll = i18nManagerService.getAllModuleCode();
            moduleMap.forEach((moduleCode, versionCode) -> {
                //校验模块名 防止下面查询索引为空 仍去进行后续无用查询
                if (!moduleCodesAll.contains(moduleCode)) {
                    sb2.append(moduleCode);
                    sb2.append(Constants.STR_POINT_DOU);
                }
            });*/
            //模块名不存在
            if (!sb2.toString().equals(Constants.STR_NO_SPACE)) {
                String modulesStr = sb2.toString().substring(0, sb.toString().length() - 1);
                result.setCode(100107024);
                result.setMessage(modulesStr + Constants.MODULE_CODE_ERROR);
                Map<String, String> tokenMap = new HashMap<>();
                tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
                tokenMap.put(Constants.HAS_TOKEN_MESSAGE, modulesStr + Constants.MODULE_CODE_ERROR );
                result.setData(tokenMap);
                log.info("100107024:" + modulesStr + Constants.MODULE_CODE_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
                return result;
            }
        }
        if (!versionErrorSb.equals(Constants.STR_NO_SPACE)) {
            String moduleVersionsStr = versionErrorSb.substring(0, versionErrorSb.length() - 1);
            result.setCode(100107057);
            result.setMessage(moduleVersionsStr + Constants.STR_POINT_M + Constants.RESOURCE_VERSION_ERROR);
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            tokenMap.put(Constants.HAS_TOKEN_MESSAGE, moduleVersionsStr + Constants.STR_POINT_M + Constants.RESOURCE_VERSION_ERROR);
            result.setData(tokenMap);
            log.info("100107057:" + moduleVersionsStr + Constants.STR_POINT_M + Constants.RESOURCE_VERSION_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        StringBuilder versionSb = new StringBuilder();
        StringBuilder versionNumSb = new StringBuilder();
        //查询对应的版本号
        moduleMap.forEach((moduleCode, versionCode) -> {
        	LambdaQueryWrapper<I18nVersionPO> queryWrapper = new QueryWrapper<I18nVersionPO>().lambda()
            		.eq(I18nVersionPO::getModuleCode, moduleCode);
            I18nVersionPO i18nVersionPO = i18nVersionDao.selectOne(queryWrapper);
            if (i18nVersionPO != null && i18nVersionPO.getModuleVersionCode().equals(versionCode)) {
                versionSb.append(moduleCode);
                versionSb.append(Constants.STR_POINT_DOU);
            }
            if (i18nVersionPO != null) {
                long versionNumDB = 0;
                long versionNum = 0;
                try {
                    versionNumDB = Long.valueOf(i18nVersionPO.getModuleVersionCode().substring(i18nVersionPO.getModuleVersionCode().length() - 12));
                    versionNum = Long.valueOf(versionCode.substring(versionCode.length() - 12));
                } catch (Exception e) {
                    log.error(e.getMessage());
                    throw new I18nException(I18nErrorEnum.MODULE_VERSION_RESOLVE_ERROR);
                }
                if (versionNumDB >= versionNum) {
                    versionNumSb.append(versionCode);
                    versionNumSb.append(Constants.STR_POINT_DOU);
                }
            }
        });
        if (!versionSb.toString().equals(Constants.STR_NO_SPACE)) {
            //服务端存在该模块的该版本的国际化资源
            String moduleCodesStr = versionSb.toString().substring(0, versionSb.toString().length() - 1);
            result.setCode(100107043);
            result.setMessage(moduleCodesStr + Constants.STR_POINT_M + Constants.RESOURCE_EXIST);
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            tokenMap.put(Constants.HAS_TOKEN_MESSAGE, moduleCodesStr + Constants.STR_POINT_M + Constants.RESOURCE_EXIST);
            result.setData(tokenMap);
            log.info("100107043:" + moduleCodesStr + Constants.STR_POINT_M + Constants.RESOURCE_EXIST + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        if (!versionNumSb.toString().equals(Constants.STR_NO_SPACE)) {
            //throw  new I18nException(I18nErrorEnum.RESOURCE_ERROR);
            String versionCodesStr = versionNumSb.toString().substring(0, versionNumSb.toString().length() - 1);
            result.setCode(100107044);
            result.setMessage(versionCodesStr + Constants.STR_POINT_M + Constants.VERSION_LOW_DB_VERSION);
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            tokenMap.put(Constants.HAS_TOKEN_MESSAGE, versionCodesStr + Constants.STR_POINT_M + Constants.VERSION_LOW_DB_VERSION);
            result.setData(tokenMap);
            log.info("100107044:" + versionCodesStr + Constants.STR_POINT_M + Constants.VERSION_LOW_DB_VERSION + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        //要同步资源判断有没有锁
        StringBuilder tokenNumSb = new StringBuilder();
        moduleMap.forEach((moduleCode, versionCode) -> {
            I18nTokenPO tokenValidOne = new I18nTokenPO();
            tokenValidOne.setModuleCode(moduleCode);
            I18nTokenPO i18nTokenPO = i18nTokenDao.selectByModuleCodeAndValidOne(tokenValidOne);
            if (i18nTokenPO != null && i18nTokenPO.getHasLock().equals(Constants.ONE_STR)) {
                tokenNumSb.append(moduleCode);
                tokenNumSb.append(Constants.STR_POINT_DOU);
            }
        });
        if (!tokenNumSb.toString().equals(Constants.STR_NO_SPACE)) {
            //当前有该模块正在进行资源上传
            String tokensStr = tokenNumSb.toString().substring(0, tokenNumSb.toString().length() - 1);
            result.setCode(100107014);
            result.setMessage(tokensStr + Constants.STR_POINT_M + Constants.FILE_IS_TOKEN_ERROR);
            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put(Constants.HAS_TOKEN, Constants.STOP);
            tokenMap.put(Constants.HAS_TOKEN_MESSAGE, tokensStr + Constants.STR_POINT_M + Constants.FILE_IS_TOKEN_ERROR);
            result.setData(tokenMap);
            log.info("100107014:" + tokensStr + Constants.STR_POINT_M + Constants.FILE_IS_TOKEN_ERROR + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK);
            return result;
        }
        //要同步资源判断有没有锁
        moduleMap.forEach((moduleCode, versionCode) -> {
            //需要创建锁
            String token = TokenUtil.getToken(moduleCode, versionCode);
            I18nTokenPO tokenPO = new I18nTokenPO();
            tokenPO.setId(IDGenerator.newInstance().generate().longValue());
            tokenPO.setModuleCode(moduleCode);
            tokenPO.setToken(token);
            tokenPO.setHasLock(Constants.ONE_STR);
            tokenPO.setValid(Constants.ONE_STR);
            i18nTokenDao.insert(tokenPO);
            resultMap.put(moduleCode, token);
        });
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.HAS_TOKEN, Constants.BREAK_NO);
        map.put(Constants.TOKEN, resultMap);
        result.setMessage(Constants.PARAM_SUCCESS);
        result.setData(map);
        return result;
    }

    @Override
    public Result postModuleResource2(Map<String, String> filenameMap, List<String> moduleCodeList, List<String> moduleVersionList, Map<String, String> tokenMap) {
        Result result = new Result();
        Map<String, String> moduleMap = new HashMap<>();
        Map<String, String> moduleVersionMap = new HashMap<>();
        moduleCodeList.forEach(moduleCode -> {
            moduleVersionList.forEach(moduleVersionCode -> {
                if (moduleVersionCode.substring(0, moduleVersionCode.length() - 12).equals(moduleCode)) {
                    moduleVersionMap.put(moduleCode, moduleVersionCode);
                }
            });
        });
        StringBuilder moduleCodeErrorSb = new StringBuilder();
        StringBuilder moduleTokenErrorSb = new StringBuilder();
        Map<String, String> moduleCodeMap = new HashMap<>();
        Map<String, String> moduleCodeMessageMap = new HashMap<>();
        Boolean useAllModuleCode = false;
        List<String> moduleCodes = i18nManagerService.getModuleEnumModuleCode();
        StringBuilder sb = new StringBuilder();
        filenameMap.forEach((moduleCode, versionCode) -> {
            //校验模块名 防止下面查询索引为空 仍去进行后续无用查询
            if (!moduleCodes.contains(moduleCode)) {
                sb.append(moduleCode);
                sb.append(Constants.STR_POINT_DOU);
            }
        });
        if (!sb.toString().equals(Constants.STR_NO_SPACE)) {
        	List<String> moduleCodesAll = i18nManagerService.getAllModuleCode();
            useAllModuleCode = true;
            execFileNameMap(filenameMap, moduleCodeErrorSb, moduleCodeMap, moduleCodesAll);
        }else{
            //传过来的文件名就是模块名 统一校验
            execFileNameMap(filenameMap, moduleCodeErrorSb, moduleCodeMap, moduleCodes);
        }
        if (moduleCodeMap != null && moduleCodeMap.size() > 0) {
            for (String moduleCode : moduleCodeMap.keySet()) {
                List<String> list = new ArrayList<>();
                list.add(moduleCode);
                for (String moduleCode2 : moduleVersionMap.keySet()) {
                    if (moduleCode2.equals(moduleCode)) {
                        list.add(moduleVersionMap.get(moduleCode2));
                    }
                }
                for (String moduleCode3 : tokenMap.keySet()) {
                    if (moduleCode3.equals(moduleCode)) {
                        list.add(tokenMap.get(moduleCode3));
                    }
                }
                //校验 模块code  校验token 和是否获得了锁
                I18nTokenPO tokenPO = i18nTokenDao.selectOne(new QueryWrapper<I18nTokenPO>().lambda()
                		.eq(I18nTokenPO::getToken, list.get(2))
                		.eq(I18nTokenPO::getModuleCode, list.get(0)));
                String folderPath = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_IMPORT_PATH;
                String filePath = folderPath + moduleCodeMap.get(moduleCode);
                list.add(filePath);
                if (tokenPO == null || (tokenPO != null && tokenPO.getHasLock().equals(Constants.ZERO_STR))) {
                    //没有获得锁 删除临时压缩包 返回
                    File file = new File(filePath);
                    if (file.exists() && file.isFile()) {
                        if (!file.delete()) {
                            log.error(file.getName() + Constants.DELETE_ERROR);
                        }
                    }
                    moduleTokenErrorSb.append(moduleCode);
                    moduleTokenErrorSb.append(Constants.STR_POINT_DOU);
                } else {
                    list.add(tokenPO.getId().toString());
                    moduleCodeMessageMap.put(moduleCode, list.toString());
                }
            }
        }
        if (moduleCodeMessageMap != null && moduleCodeMessageMap.size() > 0) {
            Boolean finalUseAllModuleCode = useAllModuleCode;
            moduleCodeMessageMap.forEach((moduleCode, listMessage) -> {
                List<String> list = ListUtils.listStringToList(listMessage);
                File zipFile = new File(list.get(3));
                // 解压zip到对应文件目录
                String i18nPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH + list.get(1);
                String newCustomPath = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_CUSTOM_PATH + moduleCode + Constants.PATH;
                //创建临时解压目录
                String destDir = list.get(3).substring(0, list.get(3).length() - 4);
                //判断路径是否存在
                MyFileUtils.createDir(destDir);
                MyFileUtils.createDir(i18nPath);
                MyFileUtils.createDir(newCustomPath);
                try {
                    //解压到存储目录 支持多个模块
                    ZipUtil.unzipToI18nDir2(zipFile, i18nPath, finalUseAllModuleCode);
                    //解压到临时目录
                    ZipUtil.unZipFiles2(zipFile, destDir);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    //解压失败记录日志
                    throw new I18nException(I18nErrorEnum.RESOURCE_UNZIP_ERROR);
                } finally {
                    File file = new File(list.get(3));
                    if (file.exists() && file.isFile()) {
                        if (!file.delete()) {
                            log.error(file.getName() + Constants.DELETE_ERROR);
                        }
                    }
                }
                String moduleIndexCode = Constants.STR_NO_SPACE;
                try {
                    //遍历当前文件夹 找到所有properties文件 //String path = "D:\\zOther\\model\\zip\\";//要遍历的路径
                    //传入解析之后的文件路径 解析所有文件到数据库
                    moduleIndexCode = operateDBService.readPropertiesToDB(moduleCode, list.get(1), destDir);
                    //将该模块的索引存入缓存
                    cachedResourceBundle.flushModuleIndexCache(moduleCode, Constants.DEFAULT_TENANT, moduleIndexCode);
                	String indexFile = MyFileUtils.getIndexToFile(i18nProperties);
                	ResourcePropertiesWrapper.updatePropertiesFile(moduleCode, moduleIndexCode, indexFile);
                } catch (Exception e) {
                    log.error(e.getMessage());
                    throw new I18nException(I18nErrorEnum.DATA_INPUT_FILE_ERROR);
                } finally {
                	//释放锁 删除token
                    i18nTokenDao.deleteOne(Long.valueOf(list.get(4)) + Constants.STR_NO_SPACE);
                    //删除接收到的压缩包临时文件  destDir 这个临时文件目录下的所有文件和这个文件夹
                    MyFileUtils.deleteAnyone(destDir);
                }
                //返回索引
                moduleMap.put(moduleCode, moduleIndexCode);
            });
        }

        result.setData(moduleMap);
        return result;
    }

    private void execFileNameMap(Map<String, String> filenameMap, StringBuilder moduleCodeErrorSb, Map<String, String> moduleCodeMap, List<String> moduleCodesAll) {
        //传过来的文件名就是模块名 统一校验
        filenameMap.forEach((moduleCode, fileNewName) -> {
            //校验模块名 防止下面查询索引为空 仍去进行后续无用查询
            if (!moduleCodesAll.contains(moduleCode)) {
                moduleCodeErrorSb.append(moduleCode);
                moduleCodeErrorSb.append(Constants.STR_POINT_DOU);
            } else {
                moduleCodeMap.put(moduleCode, fileNewName);
            }
        });
    }

    @Override
    public Result postModuleResourceOpenApi(Map map, String path) {
        Result result = new Result();
        String moduleCode = (String) map.get(Constants.MODULE_CODE);
        String versionCode = (String) map.get(Constants.MODULE_VERSION_CODE);
        I18nTokenPO i18nTokenPO = new I18nTokenPO();
        i18nTokenPO.setModuleCode(moduleCode);
        //校验 模块code 当前是否当前模块正在上传
        I18nTokenPO tokenPO = i18nTokenDao.selectOne(new QueryWrapper<I18nTokenPO>().lambda().eq(I18nTokenPO::getModuleCode, moduleCode));
        if (tokenPO != null && !tokenPO.getToken().equals(Constants.STR_NO_SPACE)) {
            throw new I18nException(I18nErrorEnum.RESOURCE_IS_UPLOADING);
        } else {
            i18nTokenPO.setId(IDGenerator.newInstance().generate().longValue());
            i18nTokenPO.setToken(TokenUtil.getToken(moduleCode, versionCode));
            i18nTokenPO.setValid(Constants.ONE_STR);
            i18nTokenPO.setHasLock(Constants.ONE_STR);
            i18nTokenDao.insert(i18nTokenPO);
        }
        //当前目录下有该模块上传的所有资源
        String filePath = path;
        String tenantId = TenantUtil.getTenantId();
        try {
            //先删除当前模块在数据库中的旧数据
            //删除resource
            i18nResourceDao.delete(new QueryWrapper<I18nResourcePO>().lambda().eq(I18nResourcePO::getModuleCode, moduleCode));
            //删除version
            i18nVersionDao.delete(new QueryWrapper<I18nVersionPO>().lambda()
        		.eq(I18nVersionPO::getModuleCode, moduleCode));
            //删除index
            i18nIndexDao.delete(new QueryWrapper<I18nIndexPO>().lambda()
        		.eq(I18nIndexPO::getModuleCode, moduleCode)
        		.eq(I18nIndexPO::getTenantId, tenantId));
            //遍历当前文件夹 找到所有properties文件
            //传入解析之后的文件路径 解析所有文件到数据库
            String moduleIndexCode = operateDBService.readPropertiesToDB(moduleCode, versionCode, filePath);
        	//将该模块的索引存入索引目录文件中
            String indexFile = MyFileUtils.getIndexToFile(i18nProperties);
            ResourcePropertiesWrapper.updatePropertiesFile(moduleCode, moduleIndexCode, indexFile);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new I18nException(I18nErrorEnum.DATA_INPUT_FILE_ERROR);
        }
        //释放锁 删除token
        i18nTokenDao.deleteOne(i18nTokenPO.getId() + Constants.STR_NO_SPACE);
        result.setMessage(Constants.PARAM_SUCCESS);
        return result;
    }


    private boolean judgeModuleCode(String moduleCode, Result result) {
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        //校验模块名 防止下面查询索引为空 仍去进行后续无用查询
        if (!moduleCodes.contains(moduleCode)) {
            //模块名不存在
            result.setCode(100107024);
            result.setMessage(moduleCode + "模块名错误");
            return true;
        }
        return false;
    }
}
