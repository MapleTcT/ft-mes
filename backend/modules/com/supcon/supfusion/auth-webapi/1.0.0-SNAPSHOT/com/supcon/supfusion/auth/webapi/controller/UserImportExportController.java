package com.supcon.supfusion.auth.webapi.controller;

import com.supcon.supfusion.auth.service.ExcelService;
import com.supcon.supfusion.auth.service.bo.ExcelStatusBO;
import com.supcon.supfusion.auth.webapi.vo.ExcelStatusVO;
import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + "/auth/v1")
@Api(value = "用户导入导出", tags = "用户导入导出")
public class UserImportExportController extends BaseController {

    @Resource
    private ExcelService excelService;

    /**
     * Excel文件导入
     *
     * @param file
     */
    @PostMapping(value = "/importExcel")
    @ApiOperation(value = "Excel文件导入", notes = "Excel文件导入", httpMethod = "POST")
    Result<ExcelStatusVO> importExcel(@ApiParam(value = "导入的文件", required = true) @RequestParam MultipartFile file) throws Exception {
        ExcelStatusVO excelStatusVO = new ExcelStatusVO();

        ExcelStatusBO excelStatusBO = excelService.importExcel(file);
        if (excelStatusBO != null) {
            BeanUtils.copyProperties(excelStatusBO, excelStatusVO);
        }
        return data(excelStatusVO);
    }

    /**
     * 导入模板下载
     *
     * @param response
     * @param request
     */
    @GetMapping(value = "/excel/template")
    @ApiOperation(value = "导入模板下载", notes = "导入模板下载", httpMethod = "GET")
    void exportPositionTemplate(HttpServletResponse response, HttpServletRequest request) throws IOException {
        excelService.downlowdExcelTemplate(response);
    }

    /**
     * 导入状态查询
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/excel/status")
    @ApiOperation(value = "导入状态查询", notes = "导入状态查询", httpMethod = "GET")
    Result<ExcelStatusVO> checkStatus(@RequestParam(value = "id", required = true) Long id) {
        ExcelStatusBO excelStatusBO = excelService.checkStatus(id);
        ExcelStatusVO excelStatusVO = new ExcelStatusVO();
        if (excelStatusBO != null) {
            BeanUtils.copyProperties(excelStatusBO, excelStatusVO);
        }
        return data(excelStatusVO);
    }


    /**
     * 错误文件下载
     *
     * @param response
     */
    @GetMapping(value = "/excel/file")
    @ApiOperation(value = "文件下载", notes = "文件下载")
    void exportPositionErrorFile(@RequestParam(value = "id", required = true) Long id, HttpServletResponse response) throws IOException {
        excelService.downlowdExcel(id, response);
    }


    /**
     * 导出下载
     *
     * @param response
     * @param request
     */
    @GetMapping(value = "/excel")
    @ApiOperation(value = "导出", notes = "导出", httpMethod = "GET")
    Result<ExcelStatusVO> exportPositionTemplate(
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "all", required = true) Boolean all,
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletResponse response,
            HttpServletRequest request
    ) {

        ExcelStatusBO excelStatusBO = excelService.exportExcelData(ids, all, keyword, response);
        ExcelStatusVO excelStatusVO = new ExcelStatusVO();
        if (excelStatusBO != null) {
            BeanUtils.copyProperties(excelStatusBO, excelStatusVO);
        }
        return data(excelStatusVO);
    }
}
