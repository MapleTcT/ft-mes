package com.supcon.supfusion.i18n.interapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
import com.supcon.supfusion.i18n.common.until.MyFileUtils;
import com.supcon.supfusion.i18n.dao.vo.ExcelVO;
import com.supcon.supfusion.i18n.service.I18nResourceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/*
 *
 * 国际化 键值对 inter-api 接口
 *
 */
@Slf4j
@Api(tags = "inter-api 国际化键值对excel导入相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
public class I18nResourceExcelImportController {

    @Autowired
    private I18nResourceService i18nResourceService;
    @Autowired
    private I18nProperties i18nProperties;


    /*
     *初始化 导入excel时默认下载一个模板
     */
    @ApiOperation(value = "下载excel文件模版接口")
    @GetMapping(value = "/resource/export/exportSet")
    public void exportSet() {
        i18nResourceService.exportExcelModel();
        //下载excel
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        String modeFimeName = Constants.FILENAME + Constants.STR_POINT + Constants.XLSX_LOW;
        //下面三行是关键代码，处理乱码问题
        response.setContentType("application/x-download");
        response.setCharacterEncoding("utf-8");
        try {
            response.setHeader("Content-Disposition", "attachment;fileName=" + java.net.URLEncoder.encode(modeFimeName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        String tenantId = TenantUtil.getTenantId();
        String folderPath = FilePathUtil.getFilePath(i18nProperties) + Constants.EXCEL_FILE_TEMPLATE_PATH + tenantId + "/";
        try (OutputStream out = response.getOutputStream();
             InputStream is = new FileInputStream(folderPath + modeFimeName);) {
            byte[] b = new byte[4096];
            int size = is.read(b);
            while (size > 0) {
                out.write(b, 0, size);
                size = is.read(b);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }


    /*
     *导入指定模块数据  xlsx一列对应多列值 解析之后写入数据库
     *1. 校验国际化是否标准,
     *2. 校验是否存在该模块,不存在则归入系统模块
     *3. 校验value的长度,校验是否包含特殊校验
     *4. 内存做比较,去重,主要是系统模块的比较
     */
    @ApiOperation(value = "发起导入excel文件请求")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "uploader", value = "上传者", required = false, paramType = "query"),
    })
    @PostMapping(value = "/resource/file")
    @ResponseBody
    public Result impor2(@RequestParam("file") MultipartFile uploadFile, String uploader) {
        Result result = new Result();
        if (uploadFile == null) {
            result.setCode(100001010);
            result.setMessage("请选择文件上传");
            return result;
        }
        //获取文件名
        String filename = uploadFile.getOriginalFilename();
        //上传文件超过xx M 提示
        if (!MyFileUtils.checkFileSize(uploadFile.getSize(), i18nProperties.getXlsxUploadMaxSize(), "M")) {
            throw new I18nException(I18nErrorEnum.XLSX_UPLOAD_MAX_SIZE_ERROR);
        }
        //获取文件后缀名
        String fileName = filename.substring(filename.length() - 4, filename.length());
        String fileNameUpperCase = fileName.toUpperCase();
        if (!Constants.XLSX.equals(fileNameUpperCase)) {
            //如果不是以 xlsx 或 xls  结尾的直接返回 文件格式错误
            result.setCode(100107009);
            result.setMessage(Constants.EXCEL_FILE_ERROR);
            return result;
        }
        //本地上的文件的拷贝位置
        UUID uuid = UUID.randomUUID();
        String uuidFileName = uuid + Constants.STR_NO_SPACE + Constants.STR_POINT + Constants.XLSX_LOW;
        String originalFileName = filename.substring(0, filename.length() - 5) + MyFileUtils.DateTime() + Constants.STR_POINT + Constants.XLSX_LOW;
        //判断路径是否存在
        String rootPath = FilePathUtil.getFilePath(i18nProperties);
        String tenantId = TenantUtil.getTenantId();
        String targetFolderPath = rootPath + Constants.EXCEL_FILE_IMPORT_PATH + tenantId + Constants.PATH;
        File targetFolder = new File(targetFolderPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        try {
            //接收文件到服务器端
            FileUtils.copyInputStreamToFile(uploadFile.getInputStream(), new File(targetFolderPath + uuidFileName));
        } catch (Exception e) {
            throw new I18nException(I18nErrorEnum.FILE_TRANSPORT_ERROR, e);
        }
        try {
            result = i18nResourceService.importation(originalFileName, uuidFileName);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new I18nException(I18nErrorEnum.FILE_RESOLVER_SHEET_ERROR);
        }
        return result;
    }

    /*
     *  前端定时访问接口 查询是否插入完成
     */
    @ApiOperation(value = "监听导入excel状态接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "查询文件名", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/file/heart")
    @ResponseBody
    public Result<ExcelVO> impor3(@RequestParam(value = "id") String id) {
        Long fileId = Long.valueOf(id);
        return i18nResourceService.checkImportStatus(fileId);
    }

    /*
     *	导入指定模块数据  之后错误文件的下载链接
     */
    @ApiOperation(value = "下载excel导入中错误信息文件接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "文件名", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/file/error", produces = "application/json")
    @ResponseBody
    public void error(@RequestParam(value = "id") String id) {
        Long fileId = Long.valueOf(id);
        Result<ExcelVO> result = i18nResourceService.checkImportStatus(fileId);
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        String path3 = Constants.STR_NO_SPACE;
        String errorFilename = result.getData().getErrorFile();
        String filename = result.getData().getFileName();
        response.setContentType("application/x-download");
        response.setCharacterEncoding("utf-8");
        try {
            response.setHeader("Content-Disposition", "attachment;fileName=" + java.net.URLEncoder.encode(errorFilename, "UTF-8").replace("+", "%20"));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        String tenantId = TenantUtil.getTenantId();
        String parentFolder = FilePathUtil.getFilePath(i18nProperties) + Constants.EXCEL_ERROR_FILE_PATH + tenantId + "/";
        MyFileUtils.createDir(parentFolder);
        path3 = parentFolder + filename;
        try (OutputStream out = response.getOutputStream();
             InputStream is = new FileInputStream(path3);) {
            byte[] b = new byte[4096];
            int size = is.read(b);
            while (size > 0) {
                out.write(b, 0, size);
                size = is.read(b);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new I18nException(I18nErrorEnum.FILE_ERROR_MESSAGE);
        }
    }
}