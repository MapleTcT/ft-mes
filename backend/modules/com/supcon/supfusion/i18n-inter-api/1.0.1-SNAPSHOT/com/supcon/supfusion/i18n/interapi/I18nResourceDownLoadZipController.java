package com.supcon.supfusion.i18n.interapi;

import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.service.I18nResourceDownloadService;
import com.supcon.supfusion.i18n.service.I18nResourceUploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 *
 * 国际化 键值对 inter-api 接口
 *
 */
@Slf4j
@Api(tags = "inter-api 模块国际化资源zip获取相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
@Deprecated
public class I18nResourceDownLoadZipController {

    @Autowired
    private I18nResourceUploadService i18nResourceUploadService;
    @Autowired
    private I18nResourceDownloadService i18nResourceDownloadService;
    @Autowired
    private I18nProperties i18nProperties;


    /*
     *   判断资源下载接口
     *  GET/inter-api/i18n/v1/resource/code/all/module_ids
     *  入参 单个模块code
     *  出参 该模块的国际化资源索引
     */
    @ApiOperation(value = "获取单个模块的索引和令牌")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCode", value = "单个模块code", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/code/all/module_ids")
    public PageResult judgeDownLoadResource(@RequestParam(value = "moduleCode") String moduleCode) {
        PageResult result = i18nResourceDownloadService.judgeGetModuleResource(moduleCode);
        return result;
    }

    /*
     *  资源下载  获取某个模块的国际化资源
     *  GET/inter-api/i18n/v1/resource/code/all/module_ids/files
     *  入参 模块code  令牌
     *  出参 zip资源
     */
    @ApiOperation(value = "获取单个模块的zip格式国际化资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "单个模块code和token集合", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/code/all/module_ids/files")
    public PageResult downLoadResource(@RequestParam Map map) {
        //如果缺少参数直接返回
        if (map.get(Constants.MODULE_CODE) == null || map.get(Constants.TOKEN) == null || map.get(Constants.MODULE_VERSION_CODE) == null) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        if ((map.get(Constants.MODULE_CODE) != null && map.get(Constants.MODULE_CODE).equals(Constants.STR_NO_SPACE))
                || (map.get(Constants.TOKEN) != null && map.get(Constants.TOKEN).equals(Constants.STR_NO_SPACE))) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }

        String moduleCode = (String) map.get(Constants.MODULE_CODE);
        String filename = moduleCode + Constants.STR_POINT + Constants.ZIP_LOW;
        //根据模块code 和 令牌生成压缩包文件到临时目录
        PageResult result = i18nResourceDownloadService.getModuleResource(map);
        //传输zip
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        try {
            response.addHeader("content-disposition", "attachment;filename=" + java.net.URLEncoder.encode(filename, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        String path3 = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + filename;
        try (OutputStream out = response.getOutputStream();
             InputStream is = new FileInputStream(path3)) {
            File zip = new File(path3);
            if (!zip.exists()) {
                return result;
            }
            byte[] b = new byte[4096];
            int size = is.read(b);
            while (size > 0) {
                out.write(b, 0, size);
                size = is.read(b);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            //删除临时压缩包
            String path = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + filename;
            File zip = new File(path);
            if (!zip.delete()) {
                log.error(zip.getName() + Constants.DELETE_ERROR);
            }
        }
        return result;
    }

    /*
     *  资源下载 压缩包形式  获取多个模块的国际化资源 不要token 过来就给
     *  入参 模块code
     *   出参 zip资源 多个模块的所有语言打成压缩包
     */
    @ApiOperation(value = "获取多个模块的zip格式国际化资源")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "moduleCodes", value = "多个模块code和语言类型集合", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/code/all/module_ids/allfiles")
    public PageResult downLoadResources(@RequestParam("moduleCodes") String moduleCodes) {
        //如果缺少参数直接返回
        if (moduleCodes == null || (moduleCodes != null && moduleCodes.equals(Constants.STR_NO_SPACE))) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        List<String> list = new ArrayList<>();
        String[] mcs = moduleCodes.split(Constants.STR_POINT_DOU);
        for (String s :
                mcs) {
            list.add(s);
        }
        String tempFilePath = i18nResourceDownloadService.getModulesResource(list);
        //传输zip
        String filename = Constants.I18N_STR + Constants.STR_POINT + Constants.ZIP_LOW;
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        try {
            response.addHeader("content-disposition", "attachment;filename=" + java.net.URLEncoder.encode(filename, "utf-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        String path3 = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + filename;
        try (OutputStream out = response.getOutputStream();
             InputStream is = new FileInputStream(path3);) {
            File zip = new File(path3);
            if (!zip.exists()) {
                throw new I18nException(I18nErrorEnum.FILE_ZIP_CREATE_ERROR);
            }
            byte[] b = new byte[4096];
            int size = is.read(b);
            while (size > 0) {
                out.write(b, 0, size);
                size = is.read(b);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            //删除临时压缩包
            String path = FilePathUtil.getFilePath(i18nProperties) + Constants.ZIP_FILE_EXPORT_PATH + tempFilePath + filename;
            File zip = new File(path);
            if (!zip.delete()) {
                log.error(zip.getName() + Constants.DELETE_ERROR);
            }

        }
        return new PageResult();
    }
}