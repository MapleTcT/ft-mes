package com.supcon.supfusion.i18n.service.impl;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.ListUtils;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.service.I18nResourceUploadService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MessageResourceUpLoadServiceImpl {

	@Autowired
    private I18nResourceUploadService i18nResourceUploadService;
	@Autowired
    private I18nProperties i18nProperties;
	@Autowired
	private LockService lockService;
	
    public Result judgeUploadResource2(@RequestParam Map map) {
        //如果缺少参数直接返回
        Result result = new Result();
        if (map.get(Constants.MODULE_CODES) == null || (map.get(Constants.MODULE_CODES) != null
                && map.get(Constants.MODULE_CODES).equals(Constants.STR_NO_SPACE))) {
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            Map tokenMap = new HashMap();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107008:" + Constants.NO_MODULE_CODE + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        if (map.get(Constants.MODULE_VERSION_CODES) == null || (map.get(Constants.MODULE_VERSION_CODES) != null
                && map.get(Constants.MODULE_VERSION_CODES).equals(Constants.STR_NO_SPACE))) {
            result.setCode(100107013);
            result.setMessage(Constants.NO_VERSION_CODE);
            Map tokenMap = new HashMap();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107013:" + Constants.NO_VERSION_CODE + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        String moduleCodesStr = (String) map.get(Constants.MODULE_CODES);
        String versionCodesStr = (String) map.get(Constants.MODULE_VERSION_CODES);
        String[] moduleCodesArr = moduleCodesStr.split(Constants.STR_POINT_DOU);
        String[] versionCodesArr = versionCodesStr.split(Constants.STR_POINT_DOU);
        if (moduleCodesArr.length != versionCodesArr.length) {
            result.setCode(100107050);
            result.setMessage(Constants.MODULE_NUM_NOT_VERSION_NUM);
            Map tokenMap = new HashMap();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107050:" + Constants.MODULE_NUM_NOT_VERSION_NUM + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        Map<String, String> moduleMap = new HashMap<String, String>();
        if (moduleCodesArr != null && moduleCodesArr.length > 0) {
            if (versionCodesArr != null && versionCodesArr.length > 0) {
                Set<String> moduleVersionErrorSet = new HashSet<>();
                Set<String> moduleVersionSet = new HashSet<>();
                StringBuilder versionErrorSb = new StringBuilder();
                for (String moduleCode : moduleCodesArr) {
                    for (String versionCode : versionCodesArr) {
                        if (versionCode.substring(0, versionCode.length() - 12).equals(moduleCode)) {
                            moduleMap.put(moduleCode, versionCode);
                            moduleVersionSet.add(versionCode);
                        } else {
                            moduleVersionErrorSet.add(versionCode);
                        }
                    }
                }
                moduleVersionErrorSet.forEach(v -> {
                    if(!moduleVersionSet.contains(v)){
                        versionErrorSb.append(v);
                        versionErrorSb.append(Constants.STR_POINT_DOU);
                    }
                });
                result = i18nResourceUploadService.judgePostModuleResource2(moduleMap,versionErrorSb.toString());
            } else {
                result.setCode(100107013);
                result.setMessage(Constants.NO_VERSION_CODE);
                Map tokenMap = new HashMap();
                tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
                result.setData(tokenMap);
                log.info("100107013:" + Constants.NO_VERSION_CODE + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
                return result;
            }
        } else {
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            Map tokenMap = new HashMap();
            tokenMap.put(Constants.HAS_TOKEN, Constants.BREAK_ERROR);
            result.setData(tokenMap);
            log.info("100107008:" + Constants.NO_MODULE_CODE + "," + Constants.HAS_TOKEN + ":" + Constants.BREAK_ERROR);
            return result;
        }
        return result;
    }
	
	/**
	 * 客户端资源上载
	 * @param map 
	 * 		{
	 * 			moduleCodes: "rbac, auth",
	 * 			moduleVersions: "rbac202012281531, auth202012281530",
	 * 			token: 
	 * 		}
	 */
	public Result<?> uploadResource2(@RequestParam("file") MultipartFile[] uploadFiles, @RequestParam Map map) {
        //如果缺少参数直接返回
        Result<?> result = new Result<>();
        //是否有模块code参数
        if (map.get(Constants.MODULE_CODES) == null || (map.get(Constants.MODULE_CODES) != null && map.get(Constants.MODULE_CODES).equals(Constants.STR_NO_SPACE))
        ) {
            result.setCode(100107008);
            result.setMessage(Constants.NO_MODULE_CODE);
            log.info("100107008:" + Constants.NO_MODULE_CODE);
            return result;
        }
        //是否有版本号参数
        if (map.get(Constants.MODULE_VERSION_CODES) == null || (map.get(Constants.MODULE_VERSION_CODES) != null && map.get(Constants.MODULE_VERSION_CODES).equals(Constants.STR_NO_SPACE))
        ) {
            result.setCode(100107013);
            result.setMessage(Constants.NO_VERSION_CODE);
            log.info("100107013:" + Constants.NO_VERSION_CODE);
            return result;
        }
        //是否有token参数
        if (map.get(Constants.TOKEN) == null || (map.get(Constants.TOKEN) != null && map.get(Constants.TOKEN).equals(Constants.STR_NO_SPACE))) {
            result.setCode(100001016);
            result.setMessage(Constants.NO_TOKEN_CODE);
            log.info("100001016:" + Constants.NO_TOKEN_CODE);
            return result;
        }
        boolean locked = lockService.isLocked(Constants.I18N_CLUSTER_LOCK);
        if (locked) {
        	log.error("....... 请求索引失败, 国际化服务还在初始化过程, 请稍后重试 .......,  当前请求参数: {}", map);
        	result.setCode(100001018);
            result.setMessage("国际化服务还在初始化过程, 请稍后重试");
            return result;
        }
        //开始接收文件
        Map<String, String> filenameMap = new HashMap<>();
        if (uploadFiles != null && uploadFiles.length > 0) {
            StringBuilder filenameSb = new StringBuilder();
            StringBuilder fileSb = new StringBuilder();
            List<MultipartFile> list = Arrays.asList(uploadFiles);
            list.forEach(uploadFile -> {
                //获取文件名
                String filename = uploadFile.getOriginalFilename();
                //校验文件后缀名
                String fileName = filename.substring(filename.length() - 3);
                String fileNameUpperCase = fileName.toUpperCase();
                if (!Constants.ZIP.equals(fileNameUpperCase)) {
                    //如果不是以 zip 结尾的直接返回 文件格式错误
                    filenameSb.append(filename);
                    filenameSb.append(Constants.STR_POINT_DOU);
                    //跳出本次循环不接受该文件
                    return;
                }
                try {
                    //接收文件到服务器端
                    UUID uuid = UUID.randomUUID();
                    String fileNewName = uuid + Constants.STR_NO_SPACE + Constants.STR_POINT + Constants.ZIP_LOW;
                    //本地上的文件的拷贝位置
                    //判断路径是否存在
                    String path = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_IMPORT_PATH;
                    MyFileUtils.createDir(path);
                    FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), new File(path + fileNewName));
                    //解压文件入库 同步properties文件
                    filenameMap.put(filename.substring(0, filename.length() - 4), fileNewName);
                } catch (Exception e) {
                    fileSb.append(filename);
                    fileSb.append(Constants.STR_POINT_DOU);
                }
            });
            if (!filenameSb.toString().equals(Constants.STR_NO_SPACE)) {
                String zipFilenames = filenameSb.toString().substring(0, filenameSb.toString().length() - 1);
                result.setCode(100107018);
                result.setMessage(zipFilenames + Constants.UPLOAD_FILE_NO_ZIP_ERROR);
                log.info("100107018:" + zipFilenames + Constants.UPLOAD_FILE_NO_ZIP_ERROR);
                return result;
            }
            if (!filenameSb.toString().equals(Constants.STR_NO_SPACE)) {
                String filenames = fileSb.toString().substring(0, fileSb.toString().length() - 1);
                result.setCode(100107003);
                result.setMessage(filenames + Constants.FILE_TRANSPORT_ERROR);
                log.info("100107003:" + filenames + Constants.UPLOAD_FILE_NO_ZIP_ERROR);
                return result;
            }
        } else {
            result.setCode(100107021);
            result.setMessage(Constants.FILE_ZIP_CREATE_ERROR);
            log.info("100107021:" + Constants.FILE_ZIP_CREATE_ERROR);
            return result;
        }
        List<String> moduleCodeList = ListUtils.listStringToList((String) map.get(Constants.MODULE_CODES));
        List<String> moduleVersionList = ListUtils.listStringToList((String) map.get(Constants.MODULE_VERSION_CODES));
        Map<String, String> tokenMap = ListUtils.mapStringToMap((String) map.get(Constants.TOKEN));
        try {
            result = i18nResourceUploadService.postModuleResource2(filenameMap, moduleCodeList, moduleVersionList, tokenMap);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }

}
