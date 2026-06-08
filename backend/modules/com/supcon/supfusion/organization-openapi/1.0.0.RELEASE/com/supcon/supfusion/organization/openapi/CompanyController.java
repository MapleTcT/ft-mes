package com.supcon.supfusion.organization.openapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.ListResult;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.framework.cloud.controller.BaseController;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.dao.po.company.CompanyPO;
import com.supcon.supfusion.organization.manager.OrganizationAdapter;
import com.supcon.supfusion.organization.openapi.vo.CompanyUpdateVO;
import com.supcon.supfusion.organization.openapi.vo.CompanyVO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyBaseInfoVO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyChildVO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.company.CompanyTreeInfoVO;
import com.supcon.supfusion.organization.openapi.vo.department.DepartmentBaseInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.MainPositionBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonBaseInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonCompanyBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonDepartmentBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonResultVO;
import com.supcon.supfusion.organization.openapi.vo.person.SystemCodeVO;
import com.supcon.supfusion.organization.openapi.vo.position.DepartmentForPositionBaseInfoVO;
import com.supcon.supfusion.organization.openapi.vo.position.PositionBaseInfoVO;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.company.CompanyBO;
import com.supcon.supfusion.organization.service.bo.company.CompanyDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.position.PositionBaseInfoBO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;

/**
 * @Description: 公司开放接口
 * @Author:
 * @CreateDate: 2020/5/25
 */
@Slf4j
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2")
@Validated
@Api(tags = "公司管理openApi", description = "公司管理openApi说明", hidden = true)
public class CompanyController extends BaseController {

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PersonService personService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PositionService positionService;

    @Autowired
    private OrganizationAdapter organizationAdapter;


    @PostMapping("/company")
    public void addCompany(@Validated @RequestBody CompanyVO comVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        CompanyPO comPO = new CompanyPO();
        BeanUtils.copyProperties(comVO, comPO);
        companyService.addCompany(comPO, comVO.getTags(), comVO.getUserName(), comVO.getPassword(), tenantId);
    }

    @PutMapping("/company")
    public void updateCompany(@Validated @RequestBody CompanyUpdateVO comVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        CompanyPO comPO = new CompanyPO();
        BeanUtils.copyProperties(comVO, comPO);
        companyService.saveOrUpdateCom(comPO, comVO.getTags(), tenantId);
    }

    @GetMapping("/company/{id}")
    @ResponseBody
    public Result<CompanyVO> company(@PathVariable Long id) {
        CompanyVO comVO = new CompanyVO();
        CompanyPO comPO = companyService.findCompany(id);
        Optional.ofNullable(comPO).ifPresent(com -> BeanUtils.copyProperties(com, comVO));
        return Result.custom().data(comVO).build();
    }

    @DeleteMapping("/company/{id}")
    public void delCompany(@PathVariable Long id) {
        String tenantId = RpcContext.getContext().getTenantId();
        companyService.delCompany(id, tenantId);
    }

    @GetMapping("/company/{companyId}/persons")
    @ApiOperation(value = "根据公司id查询人员列表", notes = "根据公司id查询人员列表")
    PageResult<PersonResultVO> queryPersonsByCompanyId(@ApiParam(value = "公司id", required = true) @PathVariable(value = "companyId") Long companyId,
                                                       @ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                       @ApiParam(value = "每页条数", required = true) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<PersonResultBO> pageResultBO = companyService.queryPersonsByCompanyId(companyId, current, pageSize);
        if (pageResultBO == null) {
            return new PageResult<>(null, 0, pageSize, current);
        }
        List<PersonResultVO> list = new ArrayList<>();
        pageResultBO.getList().stream().forEach(personResultBO -> {
            PersonResultVO personResultVO = new PersonResultVO();
            BeanUtils.copyProperties(personResultBO, personResultVO);
            list.add(personResultVO);
        });

        return new PageResult<>(list, pageResultBO.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/companies/page")
    @ApiOperation(value = "分页查询公司列表", notes = "分页查询公司列表")
    PageResult<CompanyVO> queryCompaniesPages(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                              @ApiParam(value = "每页条数", required = true) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<CompanyBO> pageResultBO = companyService.queryCompaniesPages(current, pageSize);
        if (pageResultBO == null) {
            return new PageResult<>();
        }
        List<CompanyVO> list = new ArrayList<>();
        pageResultBO.getList().stream().forEach(companyBO -> {
            CompanyVO companyVO = new CompanyVO();
            BeanUtils.copyProperties(companyBO, companyVO);
            list.add(companyVO);
        });
        return new PageResult<>(list, pageResultBO.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/companies/{companyCode}/persons")
    @ApiOperation(value = "根据公司编码查询公司下的人员列表")
    PageResult<PersonBaseInfoVO> getPersonsByDepartmentCode(@ApiParam(value = "公司编码", required = true) @PathVariable(value = "companyCode") String companyCode,
                                                            @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                            @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max (value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PersonBaseInfoBO> pageResult = personService.getPersonsByCompanyCode(companyCode, current, pageSize);

        List<PersonBaseInfoVO> personBaseInfoVOS = new ArrayList<>();
        for (PersonBaseInfoBO personBaseInfoBO : pageResult.getList()) {
            PersonBaseInfoVO personBaseInfoVO = new PersonBaseInfoVO();
            BeanUtils.copyProperties(personBaseInfoBO, personBaseInfoVO);

            MainPositionBaseVO mainPositionBaseVO = new MainPositionBaseVO();
            BeanUtils.copyProperties(personBaseInfoBO.getMainPosition(), mainPositionBaseVO);

            SystemCodeBO genderBO = personBaseInfoBO.getGender();
            if (Objects.nonNull(genderBO)) {
                SystemCodeVO genderVO = new SystemCodeVO();
                BeanUtils.copyProperties(genderBO, genderVO);
                personBaseInfoVO.setGender(genderVO);
            }

            SystemCodeBO statusBO = personBaseInfoBO.getStatus();
            if (Objects.nonNull(statusBO)) {
                SystemCodeVO statusVO = new SystemCodeVO();
                BeanUtils.copyProperties(statusBO, statusVO);
                personBaseInfoVO.setStatus(statusVO);
            }

            SystemCodeBO titleBO = personBaseInfoBO.getTitle();
            if (Objects.nonNull(titleBO)) {
                SystemCodeVO titleVO = new SystemCodeVO();
                BeanUtils.copyProperties(titleBO, titleVO);
                personBaseInfoVO.setTitle(titleVO);
            }

            SystemCodeBO educationBO = personBaseInfoBO.getEducation();
            if (Objects.nonNull(educationBO)) {
                SystemCodeVO educationVO = new SystemCodeVO();
                BeanUtils.copyProperties(educationBO, educationVO);
                personBaseInfoVO.setEducation(educationVO);
            }

            personBaseInfoVO.setMainPosition(mainPositionBaseVO);
            personBaseInfoVO.setCompanies(JSONArray.parseArray(JSON.toJSONString(personBaseInfoBO.getCompanies()), PersonCompanyBaseVO.class));
            personBaseInfoVO.setDepartments(JSONArray.parseArray(JSON.toJSONString(personBaseInfoBO.getDepartments()), PersonDepartmentBaseVO.class));
            personBaseInfoVOS.add(personBaseInfoVO);
        }
        return new PageResult<>(personBaseInfoVOS, pageResult.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/companies")
    @ApiOperation(value = "批量查询公司列表")
    PageResult<CompanyDetailInfoVO> getCompanies(@ApiParam(value = "最后修改时间", required = false) @RequestParam(value = "modifyTime", required = false) String modifyTime,
                                                 @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                 @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max (value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        Date modifyDate = null;
        /**
         * 时间格式校验,转换
         * 时间格式: 2021-01-31T10:30:20.000+1000
         */
        if (StringUtils.isNotBlank(modifyTime)) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                modifyDate = format.parse(modifyTime);
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf1.setTimeZone(TimeZone.getTimeZone("UTC"));
                modifyTime = sdf1.format(modifyDate);
            } catch (ParseException e) {
                throw new OrganizationException(OrganizationErrorEnum.DATE_FORMAT_ERROR);
            }
        }
        PageResult<CompanyDetailInfoBO> pageResult = companyService.getCompanies(modifyTime, current, pageSize);

        List<CompanyDetailInfoVO> companyDetailInfoVOS = new ArrayList<>();
        for (CompanyDetailInfoBO companyDetailInfoBO : pageResult.getList()) {
            CompanyDetailInfoVO companyDetailInfoVO = new CompanyDetailInfoVO();
            BeanUtils.copyProperties(companyDetailInfoBO, companyDetailInfoVO);
            companyDetailInfoVOS.add(companyDetailInfoVO);
        }
        return new PageResult<>(companyDetailInfoVOS, pageResult.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/companies/{companyCode}/departments")
    @ApiOperation(value = "根据公司编码查询部门列表")
    PageResult<DepartmentBaseInfoVO> getDepartmentsByCompanyCode(@ApiParam(value = "公司编码", required = true) @PathVariable(value = "companyCode") String companyCode,
                                                                 @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                                 @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max (value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<DepartmentBaseInfoBO> pageResult = departmentService.getDepartmentsByCompanyCode(companyCode, current, pageSize);

        List<DepartmentBaseInfoVO> departmentBaseInfoVOList = new ArrayList<>();
        for (DepartmentBaseInfoBO departmentBaseInfoBO : pageResult.getList()) {
            DepartmentBaseInfoVO departmentBaseInfoVO = new DepartmentBaseInfoVO();
            BeanUtils.copyProperties(departmentBaseInfoBO, departmentBaseInfoVO);
            CompanyBaseInfoVO companyForDepartmentBaseInfoVO = new CompanyBaseInfoVO();
            BeanUtils.copyProperties(departmentBaseInfoBO.getCompany(), companyForDepartmentBaseInfoVO);
            departmentBaseInfoVO.setCompany(companyForDepartmentBaseInfoVO);
            SystemCodeBO deptSystemCodeBO = departmentBaseInfoBO.getDeptType();
            SystemCodeVO deptSystemCodeVO = new SystemCodeVO();
            BeanUtils.copyProperties(deptSystemCodeBO, deptSystemCodeVO);
            departmentBaseInfoVO.setDeptType(deptSystemCodeVO);

            departmentBaseInfoVOList.add(departmentBaseInfoVO);
        }
        return new PageResult<>(departmentBaseInfoVOList, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent());
    }

    @GetMapping("/companies/{companyCode}/positions")
    @ApiOperation(value = "根据公司编码查询岗位列表")
    PageResult<PositionBaseInfoVO> getPositionsByCompanyCode(@ApiParam(value = "公司编码", required = true) @PathVariable(value = "companyCode") String companyCode,
                                                             @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                             @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max (value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PositionBaseInfoBO> pageResult = positionService.getPositionsByCompanyCode(companyCode, current, pageSize);

        List<PositionBaseInfoVO> positionBaseInfoVOList = new ArrayList<>();
        for (PositionBaseInfoBO positionBaseInfoBO : pageResult.getList()) {
            PositionBaseInfoVO positionBaseInfoVO = new PositionBaseInfoVO();
            BeanUtils.copyProperties(positionBaseInfoBO, positionBaseInfoVO);
            CompanyBaseInfoVO companyBaseInfoVO = new CompanyBaseInfoVO();
            BeanUtils.copyProperties(positionBaseInfoBO.getCompany(), companyBaseInfoVO);
            positionBaseInfoVO.setCompany(companyBaseInfoVO);

            DepartmentForPositionBaseInfoVO departmentForPositionBaseInfoVO = new DepartmentForPositionBaseInfoVO();
            BeanUtils.copyProperties(positionBaseInfoBO.getDepartment(), departmentForPositionBaseInfoVO);
            positionBaseInfoVO.setDepartment(departmentForPositionBaseInfoVO);

            positionBaseInfoVOList.add(positionBaseInfoVO);
        }
        return new PageResult<>(positionBaseInfoVOList, pageResult.getPagination().getTotal(), pageResult.getPagination().getPageSize(), pageResult.getPagination().getCurrent());
    }


    @GetMapping("/companies/{companyCode}")
    @ApiOperation(value = "根据公司编码查询公司详情")
    CompanyDetailInfoVO getCompanyByCode(@ApiParam(value = "公司编码", required = true) @PathVariable(value = "companyCode", required = true) String companyCode) {
        CompanyDetailInfoVO companyDetailInfoVO = new CompanyDetailInfoVO();
        Result<CompanyDetailInfoBO> companyDetailInfoBO = companyService.getCompanyByCode(companyCode);
        BeanUtils.copyProperties(companyDetailInfoBO.getData(), companyDetailInfoVO);
        return new Result<>(companyDetailInfoVO).getData();
    }

    /**
     * 查询公司以及下级
     */
    @GetMapping(value = {"/companies/{companyCode}/children"})
    @ResponseBody
    @ApiOperation(value = "查询当前公司及下级公司")
    public ListResult<CompanyChildVO> supCompanies(@ApiParam(value = "公司编码", required = true) @NotBlank(message = Constants.COM_PARAM_CODE_NOTNULL) @PathVariable(value = "companyCode") String companyCode,
                                                 @ApiParam(value = "是否查询多级") @RequestParam(value = "multistage", defaultValue = "true", required = false) Boolean multistage,
                                                 @ApiParam(value = "当前页码", required = true) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = true) Integer current,
                                                 @ApiParam(value = "每页条数", required = false) @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<CompanyDetailInfoBO> comBOS = companyService.getSubCompaniesByCode(companyCode, null, multistage, current, pageSize);

        List<CompanyChildVO> result = Lists.newArrayList();
        for (CompanyDetailInfoBO comBO : comBOS.getList()) {
            CompanyChildVO comVO = new CompanyChildVO();
            BeanUtils.copyProperties(comBO, comVO);
            result.add(comVO);
        }
        return new PageResult<>(result, comBOS.getPagination().getTotal(), comBOS.getPagination().getPageSize(), comBOS.getPagination().getCurrent());
    }

    @GetMapping("/companies/tree")
    @ApiOperation(value = "查询公司列表树")
    @ResponseBody
    List<CompanyTreeInfoVO> getCompaniesTree() {
        List<CompanyPO> list = companyService.listCompanies();
        if (list == null || list.size() == 0) {
            return null;
        }
        List<CompanyTreeInfoVO> companyTreeInfoVOS = new ArrayList<>();
        Map<Long, CompanyTreeInfoVO> companyTreeInfoVOMap = new HashMap<>();
        for (CompanyPO companyPO : list) {
            CompanyTreeInfoVO companyTreeInfoVO = new CompanyTreeInfoVO();
            BeanUtils.copyProperties(companyPO, companyTreeInfoVO);
            companyTreeInfoVOS.add(companyTreeInfoVO);
            companyTreeInfoVOMap.put(companyTreeInfoVO.getId(), companyTreeInfoVO);
        }

        for (int i = companyTreeInfoVOS.size() - 1; i >= 0; i--) {
            if (companyTreeInfoVOS.get(i).getParentId() != null &&
                    companyTreeInfoVOMap.containsKey(companyTreeInfoVOS.get(i).getParentId())) {
                CompanyTreeInfoVO companyTreeParent = companyTreeInfoVOMap.get(companyTreeInfoVOS.get(i).getParentId());
                companyTreeParent.getChildren().add(companyTreeInfoVOS.get(i));
                companyTreeInfoVOS.get(i).setParentCode(companyTreeParent.getCode());
                companyTreeInfoVOS.remove(i);
            }
        }

        return companyTreeInfoVOS;
    }
}
