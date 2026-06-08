package com.supcon.supfusion.i18n.interapi;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import com.supcon.supfusion.framework.cloud.i18n.context.utils.TenantUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Pagination;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.i18n.common.config.I18nProperties;
import com.supcon.supfusion.i18n.common.dto.I18nQueryDTO;
import com.supcon.supfusion.i18n.common.execption.I18nErrorEnum;
import com.supcon.supfusion.i18n.common.execption.I18nException;
import com.supcon.supfusion.i18n.common.until.Constants;
import com.supcon.supfusion.i18n.common.until.FilePathUtil;
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
@Api(tags = "inter-api 国际化键值对excel导出相关接口")
@RestController
@RequestMapping("/inter-api/i18n/v1")
public class I18nResourceExcelExportController {

    @Autowired
    private I18nResourceService i18nResourceService;
    @Autowired
    private I18nProperties i18nProperties;

    /*
     * 导出当前 全部查询结果/ 导出当前页的结果/ 选择的指定key的国际化键值对
     * 发起导出请求
     */
    @ApiOperation(value = "发起请求  导出全部查询结果/ 导出当前页的结果/ 导出选择的指定key的国际化键值对")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "page", value = "分页和条件参数", required = true, paramType = "body", example =
                    "{\"current\":1,\"pageSize\":20,\"i18n_key\":\"appcode,add\",\"i18n_values\":{\"zh_CN\":[\"支持&&%" +
                            "%%%#@\",\"获取\"],\"en_US\":[\"insert\"]},\"downAll\":true,\"i18nKeyListStr\":\"moduleRegi" +
                            "stry.moduleName.authentication,moduleRegistry.moduleName.authorization\"}"),
    })
    @PostMapping(value = "/resource/export/download/file")
    public Result<?> exportFile(@RequestBody Map<String, Object> page) {
        Object pageNo = page.get(Constants.PAGE_NO);
        Object pageSize = page.get(Constants.PAGE_SIZE);
        pageNo = pageNo == null ? Constants.ONE_INT : pageNo;
        pageSize = pageSize == null ? Constants.PAGE_SIZE_NUM_DEFA : pageSize;
        
        List<String> i18nKeysList = new ArrayList<>();
        if (page.get(Constants.I18N_KEYS_LIST_STR) != null && !page.get(Constants.I18N_KEYS_LIST_STR).equals(Constants.STR_NO_SPACE)) {
            String i18nKeysListStr = (String) page.get(Constants.I18N_KEYS_LIST_STR);
            i18nKeysList = Arrays.asList(i18nKeysListStr.split(Constants.STR_POINT_DOU));
        }
        I18nQueryDTO queryDto = new I18nQueryDTO();
        if (page.get(Constants.I18N_KEY) != null && !page.get(Constants.I18N_KEY).equals(Constants.STR_NO_SPACE)) {
            String str = (String) page.get(Constants.I18N_KEY);
            queryDto.setI18nKeys(str.split(Constants.STR_POINT_DOU));
        }
        Map<String, List<String>> valuesMap = null;
        if (page.get(Constants.I18N_VALUES) != null && !page.get(Constants.I18N_VALUES).toString().equals(Constants.STR_NO_SPACE)) {
            valuesMap = (Map<String, List<String>>) page.get(Constants.I18N_VALUES);
            queryDto.setLanguageMap(valuesMap);
        }
        queryDto.setTenantId(RpcContext.getContext().getTenantId());
        Object download = page.get(Constants.DOWN_ALL_STR);
        boolean downloadAll = download == null ? Boolean.FALSE : Boolean.parseBoolean(download.toString());
        int intPageNo = Integer.parseInt(pageNo.toString());
        int intPageSize = Integer.parseInt(pageSize.toString());
        
        return i18nResourceService.exportExcelFile(i18nKeysList, queryDto, new Pagination(0, intPageSize, intPageNo), downloadAll);
    }

    /*
     *  导出  查询当前excel导出状态
     *  查询excel导出状态
     */
    @ApiOperation(value = "监听导出状态接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "文件名", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/export/download/heart")
    public Result<ExcelVO> exportHeart(@RequestParam String id) {
        if (id == null) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        long fileId = Long.parseLong(id);
        return i18nResourceService.checkImportStatus(fileId);
    }

    /*
     *  导出  下载需要导出的文件
     */
    @ApiOperation(value = "导出excel文件接口")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "文件名", required = true, paramType = "query"),
    })
    @GetMapping(value = "/resource/export/download")
    public Result<ExcelVO> exportDownload(@RequestParam String id) throws UnsupportedEncodingException {
    	if (id == null) {
            throw new I18nException(I18nErrorEnum.PARAM_LOST);
        }
        long fileId = Long.parseLong(id);
        Result<ExcelVO> result = i18nResourceService.checkImportStatus(fileId);
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = requestAttributes.getResponse();
        String filename = result.getData().getFileName();
        response.addHeader("content-disposition", "attachment;filename=" + java.net.URLEncoder.encode(filename, "utf-8"));
        String rootPath = FilePathUtil.getFilePath(i18nProperties);
        String tenantId = TenantUtil.getTenantId();
        String targetFolderPath = rootPath + Constants.EXCEL_FILE_EXPORT_PATH + tenantId + Constants.PATH;
        //判断路径是否存在
        File targetFolder = new File(targetFolderPath);
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        String targetFile = targetFolderPath + filename;
        try (OutputStream out = response.getOutputStream();
             InputStream is = new FileInputStream(targetFile)) {
            byte[] b = new byte[4096];
            int size = is.read(b);
            while (size > 0) {
                out.write(b, 0, size);
                size = is.read(b);
            }
        } catch (Exception e) {
            throw new I18nException(I18nErrorEnum.FILE_DOWNLOAD_ERROR, e);
        } finally {
            //删除服务器端这个已经下载过的文件文件
            File exportFile = new File(targetFile);
            exportFile.delete();
            if (exportFile.exists()) {
                log.error(exportFile.getName() + Constants.DELETE_ERROR);
            }
        }
        return new Result<>();
    }


}