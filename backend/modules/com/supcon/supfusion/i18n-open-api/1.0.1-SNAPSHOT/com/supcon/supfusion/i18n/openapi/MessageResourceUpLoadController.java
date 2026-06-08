package com.supcon.supfusion.i18n.openapi;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.dao.ExcelDao;
import com.supcon.supfusion.i18n.manager.service.I18nManagerService;
import com.supcon.supfusion.i18n.service.I18nResourceUploadService;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.until.UploadingUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

/*
 *
 * 国际化 键值对 open-api 接口
 *
 */
@Slf4j
@Api(tags = "模块国际化资源上传相关接口")
@RestController
@RequestMapping("/open-api/i18n/v1")
public class MessageResourceUpLoadController {

    @Autowired
    private I18nResourceUploadService i18nResourceUploadService;
    @Autowired
    private I18nManagerService i18nManagerService;
    @Autowired
    private ExcelDao excelDao;
    @Autowired
    private I18nProperties i18nProperties;

    /*
     *  资源上载
     *  POST/open-api/i18n/v1/resource/code/all/module_id/file
     *  入参 模块code 令牌 国际化资源版本号 接收zip包
     */
    @ApiOperation(value = "上传当前模块的 properties文件格式国际化资源包")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "map", value = "模块code和模块版本号code和获取到的token参数集合", required = true, paramType = "query",
                    example = "{'moduleCode':'reg','moduleVersion':'reg202007161529'}"
            ),
    })
    @PostMapping(value = "/resource/module/upload")
    public Result uploadResource(@RequestParam("file")MultipartFile[] uploadFiles, @RequestParam Map map) {
        //如果缺少参数直接返回
        Result result = new Result();
        if (map.get(Constants.MODULE_CODE) == null || (map.get(Constants.MODULE_CODE)
                != null && map.get(Constants.MODULE_CODE).equals(Constants.STR_NO_SPACE))
        ) {
            throw  new I18nException(I18nErrorEnum.FILE_NO_MODULE_ERROR);
        }
        if (map.get(Constants.MODULE_VERSION_CODE) == null || (map.get(Constants.MODULE_VERSION_CODE) != null && map.get(Constants.MODULE_VERSION_CODE).equals(Constants.STR_NO_SPACE))) {
            throw  new I18nException(I18nErrorEnum.FILE_NO_MODULE_AND_VERSION_ERROR);
        }
        String moduleCode = (String) map.get(Constants.MODULE_CODE);
        String versionCode = (String) map.get(Constants.MODULE_VERSION_CODE);
        //校验版本号格式是否正确
        if(!versionCode.substring(0,versionCode.length()-12).equals(moduleCode)){
            throw new I18nException(I18nErrorEnum.MODULE_VERSION_RESOLVE_ERROR);
        }
        List<String> moduleCodes = i18nManagerService.getAllModuleCode();
        //校验模块名
        if (!moduleCodes.contains(moduleCode)) {
            //模块名不存在
            throw new I18nException(I18nErrorEnum.MODULE_CODE_ERROR);
        }
        //查看当前系统是否正在上传excel
        UploadingUtil.getExcelUpLoadingState(excelDao);
        String path = FilePathUtil.getFilePath(i18nProperties) + Constants.RESOURCE_FILE_PATH+versionCode;
        MyFileUtils.createDir(path);
        if (uploadFiles != null && uploadFiles.length > 0) {
            try {
                for (MultipartFile uploadFile : uploadFiles) {
                    //获取文件名
                    String filename = uploadFile.getOriginalFilename();
                    //校验文件后缀名
                    String fileName = filename.substring(filename.length() - 10);
                    String fileNameUpperCase = fileName.toUpperCase();
                    if (!Constants.PROPERTIES.equals(fileNameUpperCase)) {
                        continue;
                    }
                    FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), new File(path + Constants.PATH + filename));
                }
            } catch (Exception e) {
                throw new I18nException(I18nErrorEnum.FILE_TRANSPORT_ERROR);
            }
        } else {
            throw new I18nException(I18nErrorEnum.FILE_UPLOAD_NO_FILE_ERROR);
        }
        try {
            result = i18nResourceUploadService.postModuleResourceOpenApi(map,path);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }

}