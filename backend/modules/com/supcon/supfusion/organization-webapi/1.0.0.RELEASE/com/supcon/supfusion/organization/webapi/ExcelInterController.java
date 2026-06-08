package com.supcon.supfusion.organization.webapi;

import com.supcon.supfusion.framework.cloud.annotation.InternalApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.common.utils.ExcelUtils;
import com.supcon.supfusion.organization.service.ExcelManageService;
import com.supcon.supfusion.organization.service.bo.excel.ExcelStatusBO;
import com.supcon.supfusion.organization.service.bo.person.PersonDetailBO;
import com.supcon.supfusion.organization.webapi.vo.excel.ExcelStatusVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

/**
 * 人员管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@InternalApi(path = HttpConstants.URL_INTERNALAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v1")
@Validated
@Api(tags = "组织导入导出管理", description = "组织导入导出管理", hidden = true)
public class ExcelInterController {

    @Autowired
    private ExcelManageService excelManageService;
    /**
     * Excel文件导入
     * @param file
     */
    @PostMapping(value = "/excel")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Excel文件导入", notes = "Excel文件导入")
    Result<ExcelStatusVO> importDepExcel(@ApiParam(value = "公司id", required = true) @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam("companyId") Long companyId,
                                         @ApiParam(value = "导入文件类型, Department部门,Position岗位, Person人员, Relation岗位人员关系", required = true, allowableValues = "Department, Position, Person, Relation") @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam("type") String type,
                                         @ApiParam(value = "导入的文件", required = true) @RequestParam MultipartFile file,
                                         @ApiParam(value = "时区", required = false, defaultValue = "0000") String timeZone) throws IOException {

        if (file != null && file.getSize() > 0) {
            String tenantId = RpcContext.getContext().getTenantId();
            String fileName = file.getOriginalFilename();

            if (fileName != null && !fileName.endsWith(".xlsx")) {
                throw new OrganizationException(OrganizationErrorEnum.EXCEL_SIZE_EMPTY);
            }
            ExcelStatusBO excelStatusBO = excelManageService.importExcel(companyId, type, file, tenantId, timeZone);
            ExcelStatusVO excelStatusVO = new ExcelStatusVO();
            if (excelStatusBO != null) {
                BeanUtils.copyProperties(excelStatusBO, excelStatusVO);
            }
            return new Result<ExcelStatusVO>(excelStatusVO);
        } else {
            throw new OrganizationException(OrganizationErrorEnum.EXCEL_SIZE_EMPTY);
        }
    }

    /**
     * 导入模板下载
     * @param response
     */
    @GetMapping(value = "/excel/template")
    @ApiOperation(value = "导入模板Excel文件下载", notes = "导入模板Excel文件下载")
    void exportPositionTemplate(@ApiParam(value = "导入文件类型, Department部门,Position岗位, Person人员, Relation岗位人员关系", required = true, allowableValues = "Department, Position, Person, Relation") @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam("type") String type,
                                HttpServletResponse response) throws IOException {
        excelManageService.downlowdExcelTemplate(type, response);
    }

    /**
     * 错误文件下载
     * @param response
     */
    @GetMapping(value = "/excel/file")
    @ApiOperation(value = "文件下载", notes = "文件下载")
    void exportPositionErrorFile(@ApiParam(value = "导入任务id", required = true)@RequestParam(value = "id", required = true) Long id, HttpServletResponse response) throws IOException {
        excelManageService.downlowdExcel(id, response);
    }

    /**
     * 导入状态查询
     * @param id
     * @return
     */
    @GetMapping(value = "/excel/status")
    @ApiOperation(value = "导入状态查询", notes = "导入状态查询")
    Result<ExcelStatusVO> checkStatus(@ApiParam(value = "导入任务id", required = true)@RequestParam(value = "id", required = true) Long id) {
        ExcelStatusBO excelStatusBO = excelManageService.checkStatus(id);
        ExcelStatusVO excelStatusVO = new ExcelStatusVO();
        if (excelStatusBO != null) {
            BeanUtils.copyProperties(excelStatusBO, excelStatusVO);
        }
        return new Result<ExcelStatusVO>(excelStatusVO);
    }

    /**
     * 导入下载
     * @param response
     * @param request
     */
    @GetMapping(value = "/excel")
    @ApiOperation(value = "导出Excel文件", notes = "导出Excel文件")
    Result<ExcelStatusVO>  exportPositionFile(@ApiParam(value = "id列表", required = false) @RequestParam(value = "ids", required = false) List<Long> ids,
                                @ApiParam(value = "是否导出全部", required = true) @RequestParam(value = "all", required = true) Boolean all,
                                @ApiParam(value = "公司id", required = true) @RequestParam(value = "companyId", required = true) Long companyId,
                                @ApiParam(value = "导入文件类型, Department部门,Position岗位, Person人员, Position-Relation岗位人员关系, Department-Relation部门人员关系", required = true, allowableValues = "Department, Position, Person, Relation, Position-Relation, Department-Relation") @NotNull(message = Constants.COM_PARAM_ID_NOTNULL) @RequestParam("type") String type,
                                @ApiParam(value = "组织id") @RequestParam(value = "orgId", required = false) Long orgId,
                                @ApiParam(value = "模糊匹配关键字") @RequestParam(value = "keyword", required = false) String keyword,
                                @ApiParam(value = "人员编码") @RequestParam(value = "code", required = false) String code,
                                @ApiParam(value = "人员名称") @RequestParam(value = "name",required = false) String name,
                                @ApiParam(value = "描述") @RequestParam(value = "description", required = false) String description,
                                @ApiParam(value = "邮箱") @RequestParam(value = "email", required = false) String email,
                                @ApiParam(value = "性别") @RequestParam(value = "gender", required = false) String gender,
                                @ApiParam(value = "手机号") @RequestParam(value = "phone", required = false) String phone,
                                @ApiParam(value = "状态") @RequestParam(value = "status", required = false) String status,
                                @ApiParam(value = "时区", required = false, defaultValue = "0000") String timeZone,
                                HttpServletResponse response, HttpServletRequest request) {

        PersonDetailBO conditionQuery = new PersonDetailBO();
        conditionQuery.setCode(code);
        conditionQuery.setName(name);
        conditionQuery.setDescription(description);
        conditionQuery.setEmail(email);
        conditionQuery.setGender(gender);
        conditionQuery.setPhone(phone);
        conditionQuery.setStatus(status);
        ExcelStatusBO excelStatusBO = excelManageService.exportExcelData(ids, all, type, response, companyId, conditionQuery, keyword, orgId);
        ExcelStatusVO excelStatusVO = new ExcelStatusVO();
        if (excelStatusBO != null) {
            BeanUtils.copyProperties(excelStatusBO, excelStatusVO);
        }
        return new Result<ExcelStatusVO>(excelStatusVO);
    }
}
