package com.supcon.supfusion.organization.openapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.exception.BizHttpStatusException;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.DepartmentErrorEnum;
import com.supcon.supfusion.organization.common.exception.DepartmentException;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.dao.po.department.DepartmentAddPO;
import com.supcon.supfusion.organization.openapi.vo.*;
import com.supcon.supfusion.organization.openapi.vo.department.DepartmentDetailInfoVO;
import com.supcon.supfusion.organization.openapi.vo.department.DepartmentInfoVO;
import com.supcon.supfusion.organization.openapi.vo.department.DepartmentSynchronizationInfoVO;
import com.supcon.supfusion.organization.openapi.vo.department.ManagerForDepartmentSynchronizationInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.MainPositionBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonBaseInfoVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonCompanyBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonDepartmentBaseVO;
import com.supcon.supfusion.organization.openapi.vo.person.PersonResultVO;
import com.supcon.supfusion.organization.openapi.vo.person.SystemCodeVO;
import com.supcon.supfusion.organization.openapi.vo.position.*;
import com.supcon.supfusion.organization.service.CompanyService;
import com.supcon.supfusion.organization.service.DepartmentService;
import com.supcon.supfusion.organization.service.PersonService;
import com.supcon.supfusion.organization.service.PositionService;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentLocationBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.department.DepartmentTreeBO;
import com.supcon.supfusion.organization.service.bo.department.ManagerForDepartmentSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.position.*;
import io.micrometer.core.instrument.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2")
@Validated
@Api(tags = "部门管理OpenApi", description = "部门管理OpenApi接口文档说明", hidden = true)
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private PersonService personService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private PositionService positionService;

    /**
     * 部门新增openapi借口
     * @param departmentAddVO
     */
    @PostMapping(value = "/department")
    @ResponseStatus(HttpStatus.OK)
    void addDepartment(@Validated @RequestBody DepartmentAddVO departmentAddVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        BeanUtils.copyProperties(departmentAddVO, departmentAddPO);
        departmentService.addDepartment(departmentAddPO, departmentAddVO.getManagerIds(), tenantId);
    }

    /**
     * 根据id修改部门信息
     * @param departmentUpdateVO
     */
    @PutMapping(value = "/department")
    @ResponseStatus(HttpStatus.OK)
    void updateDepartment(@Validated @RequestBody DepartmentUpdateVO departmentUpdateVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentAddPO departmentAddPO = new DepartmentAddPO();
        BeanUtils.copyProperties(departmentUpdateVO, departmentAddPO);
        departmentService.updateDepartment(departmentAddPO, departmentUpdateVO.getManagerIds(), tenantId);
    }

    /**
     * 查询部门详细信息
     * @param id
     * @return
     */
    @GetMapping(value = "/department")
    @ResponseBody
    @ApiOperation(value = "根据部门编码查询部门详情", notes = "根据部门编码查询部门详情")
    Result<DepartmentDetailVO> getDepDetail(@NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY) @RequestParam("id") Long id) {
        DepartmentDetailBO departmentDetailBO = departmentService.getDepDetail(id);
        DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
        BeanUtils.copyProperties(departmentDetailBO, departmentDetailVO);
        List<PositionDetailVO> voRelPos = new ArrayList<PositionDetailVO>();
        if (departmentDetailVO.getRelPos() == null) {
            return new Result<DepartmentDetailVO>(departmentDetailVO);
        }

        departmentDetailVO.getRelPos().stream().forEach(item -> {
            PositionDetailVO posForDepVO = new PositionDetailVO();
            BeanUtils.copyProperties(item, posForDepVO);
            voRelPos.add(posForDepVO);
        });
        departmentDetailVO.setRelPos(voRelPos);
        return new Result<DepartmentDetailVO>(departmentDetailVO);
    }

    /**
     *  删除指定部门
     * @param id
     */
    @DeleteMapping(value = "/department/{id}")
    @ResponseStatus(HttpStatus.OK)
    void deleteDep(@PathVariable("id") Long id) {
        String tenantId = RpcContext.getContext().getTenantId();
        departmentService.deleteDepById(id, tenantId);
    }

    /**
     * 修改部门的位置
     * @param departmentLocationVO
     */
    @PutMapping(value = "/department/location")
    @ResponseStatus(HttpStatus.OK)
    void updateDepLocation(@Validated @RequestBody DepartmentLocationVO departmentLocationVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        DepartmentLocationBO departmentLocationPO = new DepartmentLocationBO();
        BeanUtils.copyProperties(departmentLocationVO, departmentLocationPO);
        departmentService.updateDepLocation(departmentLocationPO, tenantId);
    }


/*    @GetMapping(value = "/departments")
    @ResponseBody
    Result<DepartmentTreeVO> getDepTree(@NotNull(message = Constants.DEPARTMENT_PARAM_COMPANYID_NECESSARY) @RequestParam("companyId") Long companyId, Long parentId, String keyword) {
        DepartmentTreeBO departmentTreePO = departmentService.getDepTree(companyId, keyword, parentId);
        DepartmentTreeVO departmentTreeVO = new DepartmentTreeVO();
        BeanUtils.copyProperties(departmentTreePO, departmentTreeVO);
        List<DepartmentTreeBO> poList = departmentTreePO.getChildren();
        if (poList == null || poList.size() == 0) {
            return new Result<DepartmentTreeVO>(departmentTreeVO);
        }
        departmentTreeVO.setChildren(new ArrayList<DepartmentTreeVO>());

        poToVo(departmentTreePO.getChildren(), departmentTreeVO.getChildren());
        return new Result<DepartmentTreeVO>(departmentTreeVO);
    }*/


    @PostMapping(value = "/department/excel", produces = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    void importDepExcel(@NotNull(message = Constants.DEPARTMENT_PARAM_COMPANYID_NECESSARY) @RequestParam("companyId") Long companyId, @RequestParam MultipartFile file) {
        if (file != null && file.getSize() > 0) {

        } else {
            throw new DepartmentException(DepartmentErrorEnum.DEPARTMENT_EXCEL_IMPORT_FILE_NOT_EXISTS);
        }
    }
    /**
     * 查询树形结构中PO对象转BO对象
     * @param poList
     * @param voList
     */
    private void poToVo(List<DepartmentTreeBO> poList, List<DepartmentTreeVO> voList) {
        poList.stream().forEach(poDep -> {
            DepartmentTreeVO voDep = new DepartmentTreeVO();
            BeanUtils.copyProperties(poDep, voDep);
            voList.add(voDep);
            if (poDep.getChildren() != null && poDep.getChildren().size() > 0) {
                voDep.setChildren(new ArrayList<DepartmentTreeVO>());
                poToVo(poDep.getChildren(), voDep.getChildren());
            }
        });
    }

    @GetMapping("/department/{departmentId}/persons")
    @ApiOperation(value = "根据部门id查询人员列表", notes = "根据部门id查询人员列表")
    PageResult<PersonResultVO> queryPersonsByDepartmentId(@ApiParam(value = "部门id", required = true) @PathVariable(value = "departmentId") Long departmentId,
                                                        @ApiParam(value = "当前页码", required = true) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                        @ApiParam(value = "每页条数", required = true) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<PersonResultBO> pageResultBO = departmentService.queryPersonsByDepartmentId(departmentId, current, pageSize);
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
    @GetMapping("/departments/page")
    @ApiOperation(value = "分页查询部门列表", notes = "分页查询部门列表")
    PageResult<DepartmentDetailVO> queryDepartmentsPage(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @RequestParam("current") Integer current,
                                                        @ApiParam(value = "每页条数", required = true) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<DepartmentDetailBO> pageResultBO = departmentService.queryDepartmentsPage(current, pageSize);
        if (pageResultBO == null) {
            return new PageResult<>(null, 0, pageSize, current);
        }
        List<DepartmentDetailVO> list = new ArrayList<>();
        pageResultBO.getList().stream().forEach(departmentDetailBO -> {
            DepartmentDetailVO departmentDetailVO = new DepartmentDetailVO();
            BeanUtils.copyProperties(departmentDetailBO, departmentDetailVO);
            list.add(departmentDetailVO);
        });
        return new PageResult<>(list, pageResultBO.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/departments/{departmentCode}/persons")
    @ApiOperation(value = "根据部门编码查询部门下的人员列表")
    PageResult<PersonBaseInfoVO> getPersonsByDepartmentCode(@ApiParam(value = "部门编码", required = true) @PathVariable(value = "departmentCode") String departmentCode,
                                                            @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                            @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PersonBaseInfoBO> pageResult = personService.getPersonsByDepartmentCode(departmentCode, current, pageSize);

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

    @GetMapping("/departments")
    @ApiOperation(value = "批量查询部门列表")
    PageResult<DepartmentSynchronizationInfoVO> getDepartments(@ApiParam(value = "最后修改时间", required = false) @RequestParam(value = "modifyTime", required = false) String modifyTime,
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

        PageResult<DepartmentSynchronizationInfoBO> pageResult = departmentService.getDepartments(modifyTime, current, pageSize);

        List<DepartmentSynchronizationInfoVO> positionSynchronizationInfoVOS = new ArrayList<>();
        for (DepartmentSynchronizationInfoBO positionSynchronizationInfoBO : pageResult.getList()) {
            DepartmentSynchronizationInfoVO positionSynchronizationInfoVO = new DepartmentSynchronizationInfoVO();
            BeanUtils.copyProperties(positionSynchronizationInfoBO, positionSynchronizationInfoVO);

            SystemCodeBO deptSystemCodeBO = positionSynchronizationInfoBO.getDeptType();
            SystemCodeVO deptSystemCodeVO = new SystemCodeVO();
            BeanUtils.copyProperties(deptSystemCodeBO, deptSystemCodeVO);
            positionSynchronizationInfoVO.setDeptType(deptSystemCodeVO);

            CompanyForPositionSynchronizationInfoVO companyForPositionSynchronizationInfoVO = new CompanyForPositionSynchronizationInfoVO();
            BeanUtils.copyProperties(positionSynchronizationInfoBO.getCompany(), companyForPositionSynchronizationInfoVO);
            positionSynchronizationInfoVO.setCompany(companyForPositionSynchronizationInfoVO);

            List<ManagerForDepartmentSynchronizationInfoVO> managerForDepartmentSynchronizationInfoVOS = new ArrayList<>();
            if (!CollectionUtils.isEmpty(positionSynchronizationInfoBO.getManagers())) {
                for (ManagerForDepartmentSynchronizationInfoBO managerForDepartmentSynchronizationInfoBO : positionSynchronizationInfoBO.getManagers()) {
                    ManagerForDepartmentSynchronizationInfoVO managerForDepartmentSynchronizationInfoVO = new ManagerForDepartmentSynchronizationInfoVO();
                    BeanUtils.copyProperties(managerForDepartmentSynchronizationInfoBO, managerForDepartmentSynchronizationInfoVO);
                    managerForDepartmentSynchronizationInfoVOS.add(managerForDepartmentSynchronizationInfoVO);
                }
            }
            positionSynchronizationInfoVO.setManagers(managerForDepartmentSynchronizationInfoVOS);

            positionSynchronizationInfoVOS.add(positionSynchronizationInfoVO);
        }
        return new PageResult<>(positionSynchronizationInfoVOS, pageResult.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/departments/{departmentCode}")
    @ApiOperation(value = "根据部门编码查询部门详情")
    DepartmentDetailInfoVO getDeptByCode(@ApiParam(value = "部门编码", required = true) @PathVariable(value = "departmentCode", required = true) String departmentCode) {
        DepartmentDetailInfoVO departmentDetailInfoVO = new DepartmentDetailInfoVO();
        Result<DepartmentDetailInfoBO> departmentDetailInfoBO = departmentService.getDepartmentByCode(departmentCode);
        BeanUtils.copyProperties(departmentDetailInfoBO.getData(), departmentDetailInfoVO);

        SystemCodeBO deptSystemCodeBO = departmentDetailInfoBO.getData().getDeptType();
        SystemCodeVO deptSystemCodeVO = new SystemCodeVO();
        BeanUtils.copyProperties(deptSystemCodeBO, deptSystemCodeVO);
        departmentDetailInfoVO.setDeptType(deptSystemCodeVO);
        CompanyForPositionSynchronizationInfoVO companyForPositionSynchronizationInfoVO = new CompanyForPositionSynchronizationInfoVO();
        BeanUtils.copyProperties(departmentDetailInfoBO.getData().getCompany(), companyForPositionSynchronizationInfoVO);
        departmentDetailInfoVO.setCompany(companyForPositionSynchronizationInfoVO);
        List<ManagerForDepartmentSynchronizationInfoVO> managerForDepartmentSynchronizationInfoVOS = new ArrayList<>();
        if (!CollectionUtils.isEmpty(departmentDetailInfoBO.getData().getManagers())) {
            for (ManagerForDepartmentSynchronizationInfoBO managerForDepartmentSynchronizationInfoBO : departmentDetailInfoBO.getData().getManagers()) {
                ManagerForDepartmentSynchronizationInfoVO managerForDepartmentSynchronizationInfoVO = new ManagerForDepartmentSynchronizationInfoVO();
                BeanUtils.copyProperties(managerForDepartmentSynchronizationInfoBO, managerForDepartmentSynchronizationInfoVO);
                managerForDepartmentSynchronizationInfoVOS.add(managerForDepartmentSynchronizationInfoVO);
            }
        }
        departmentDetailInfoVO.setManagers(managerForDepartmentSynchronizationInfoVOS);

        List<DepartmentForPositionBaseInfoBO> departmentForPositionBaseInfoBOList = departmentDetailInfoBO.getData().getPositons();
        List<DepartmentForPositionBaseInfoVO> departmentForPositionBaseInfoVOList = departmentForPositionBaseInfoBOList.stream().map(positionBO -> {
            DepartmentForPositionBaseInfoVO departmentForPositionBaseInfoVO = new DepartmentForPositionBaseInfoVO();
            BeanUtils.copyProperties(positionBO, departmentForPositionBaseInfoVO);
            return departmentForPositionBaseInfoVO;
        }).collect(Collectors.toList());
        departmentDetailInfoVO.setPositions(departmentForPositionBaseInfoVOList);
        return new Result<>(departmentDetailInfoVO).getData();
    }

    /**
     * 查询部门的子节点列表 test
     *
     * @param departmentCode
     * @return
     */
    @GetMapping(value = "/departments/{departmentCode}/children")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "根据部门编码查询部门的子节点列表", notes = "根据部门编码查询部门的子节点列表")
    public PageResult<DepartmentInfoVO> getDeptChildList(@ApiParam(value = "部门编码", required = true) @NotNull(message = Constants.DEPARTMENT_PARAM_ID_NECESSARY)
                                                               @PathVariable(value = "departmentCode", required = true) String departmentCode,
                                                         @ApiParam(value = "是否查询多级") @RequestParam(value = "multistage", defaultValue = "true", required = false) Boolean multistage,
                                                         @ApiParam(value = "当前页码", required = true) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = true) Integer current,
                                                         @ApiParam(value = "每页条数", required = false) @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        DepartmentAddPO departmentAddPO = departmentService.getDepartmentAddPoByCode(departmentCode);
        // 查询部门不存在
        if (null == departmentAddPO) {
            throw new BizHttpStatusException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS, 400);
        }
        Long treeId = departmentAddPO.getId();
        Long companyId = departmentAddPO.getCompanyId();
        PageResult<DepartmentDetailInfoBO> depTreePage = departmentService.querySubDepartmentInfoByParentId(treeId, multistage, companyId, current, pageSize);
        List<DepartmentDetailInfoBO> depTree = (List)depTreePage.getList();
        if (null == depTree) {
            return new PageResult<DepartmentInfoVO>(new ArrayList<>(), 0, pageSize, current);
        }

        List<DepartmentInfoVO> departmentDetailList = new ArrayList<>();
        for (DepartmentDetailInfoBO detailBO : depTree) {
            DepartmentInfoVO departmentDetailInfoVO = new DepartmentInfoVO();
            BeanUtils.copyProperties(detailBO, departmentDetailInfoVO);
            // 父级编码
            departmentDetailInfoVO.setParentCode(departmentCode);

            // 部门类型
            SystemCodeBO deptSystemCodeBO = detailBO.getDeptType();
            SystemCodeVO deptSystemCodeVO = new SystemCodeVO();
            BeanUtils.copyProperties(deptSystemCodeBO, deptSystemCodeVO);
            departmentDetailInfoVO.setDeptType(deptSystemCodeVO);

            // 公司
            CompanyForPositionSynchronizationInfoVO companyForPositionSynchronizationInfoVO = new CompanyForPositionSynchronizationInfoVO();
            BeanUtils.copyProperties(detailBO.getCompany(), companyForPositionSynchronizationInfoVO);
            departmentDetailInfoVO.setCompany(companyForPositionSynchronizationInfoVO);

            // 管理者
            List<ManagerForDepartmentSynchronizationInfoVO> managerForDepartmentSynchronizationInfoVOS = new ArrayList<>();
            if (!CollectionUtils.isEmpty(detailBO.getManagers())) {
                for (ManagerForDepartmentSynchronizationInfoBO managerForDepartmentSynchronizationInfoBO : detailBO.getManagers()) {
                    ManagerForDepartmentSynchronizationInfoVO managerForDepartmentSynchronizationInfoVO = new ManagerForDepartmentSynchronizationInfoVO();
                    BeanUtils.copyProperties(managerForDepartmentSynchronizationInfoBO, managerForDepartmentSynchronizationInfoVO);
                    managerForDepartmentSynchronizationInfoVOS.add(managerForDepartmentSynchronizationInfoVO);
                }
            }
            departmentDetailInfoVO.setManagers(managerForDepartmentSynchronizationInfoVOS);

            // 关联岗位的岗位编码
            List<DepartmentForPositionBaseInfoBO> departmentForPositionBaseInfoBOList = detailBO.getPositons();
            List<DepartmentForPositionBaseInfoVO> departmentForPositionBaseInfoVOList = departmentForPositionBaseInfoBOList.stream().map(positionBO -> {
                DepartmentForPositionBaseInfoVO departmentForPositionBaseInfoVO = new DepartmentForPositionBaseInfoVO();
                BeanUtils.copyProperties(positionBO, departmentForPositionBaseInfoVO);
                return departmentForPositionBaseInfoVO;
            }).collect(Collectors.toList());
            departmentDetailInfoVO.setPositions(departmentForPositionBaseInfoVOList);

            departmentDetailList.add(departmentDetailInfoVO);
        }

        PageResult<DepartmentInfoVO> res = new PageResult<>(departmentDetailList, depTreePage.getPagination().getTotal(), pageSize, current);
        res.setList(departmentDetailList);
        return res;
    }

    /**
     * 查询指定部门关联的岗位信息
     *
     * @param departmentCode
     * @return
     */
    @GetMapping(value = "/departments/{departmentCode}/positions")
    @ResponseBody
    @ApiOperation(value = "查询指定部门关联的岗位信息", notes = "查询指定部门关联的岗位信息")
    List<PositionChildVO> getDepPositions(@ApiParam(value = "部门编码", required = true)
                                               @NotNull(message = Constants.DEPARTMENT_PARAM_CODE_NECESSARY) @PathVariable("departmentCode") String departmentCode) {
        DepartmentAddPO departmentAddPO = departmentService.getDepartmentAddPoByCode(departmentCode);
        // 指定部门不存在
        if (null == departmentAddPO) {
            throw new BizHttpStatusException(DepartmentErrorEnum.DEPARTMENT_ID_NOT_EXISTS, 400);
        }

        List<PositionChildVO> positionChildVOS = new ArrayList<>();
        DepartmentDetailBO departmentDetailBO = new DepartmentDetailBO();
        BeanUtils.copyProperties(departmentAddPO, departmentDetailBO);
        //查询部门关联岗位
        List<PositionDetailInfoBO> positionDetailInfoBOS = positionService.getPositionsByDepartment(departmentDetailBO);

        for (PositionDetailInfoBO positionDetailInfoBO : positionDetailInfoBOS) {
            PositionChildVO positionChildVO = new PositionChildVO();
            BeanUtils.copyProperties(positionDetailInfoBO, positionChildVO);
            positionChildVOS.add(positionChildVO);

            CompanyForPositionSynchronizationInfoBO companyBO = positionDetailInfoBO.getCompany();
            CompanyForPositionSynchronizationInfoVO companyForPositionSynchronizationInfoVO = new CompanyForPositionSynchronizationInfoVO();
            BeanUtils.copyProperties(companyBO, companyForPositionSynchronizationInfoVO);
            positionChildVO.setCompany(companyForPositionSynchronizationInfoVO);

            DepartmentForPositionSynchronizationInfoBO departmentBO = positionDetailInfoBO.getDepartment();
            DepartmentForPositionSynchronizationInfoVO departmentForPositionSynchronizationInfoVO = new DepartmentForPositionSynchronizationInfoVO();
            BeanUtils.copyProperties(departmentBO, departmentForPositionSynchronizationInfoVO);
            positionChildVO.setDepartment(departmentForPositionSynchronizationInfoVO);

            List<PositionRoleBaseBO> positionRoleBaseBOS = positionDetailInfoBO.getRoles();
            if (!CollectionUtils.isEmpty(positionRoleBaseBOS)) {
                List<PositionRoleBaseVO> roles = positionRoleBaseBOS.stream().map(positionRoleBaseBO -> {
                    PositionRoleBaseVO positionRoleBaseVO = new PositionRoleBaseVO();
                    BeanUtils.copyProperties(positionRoleBaseBO, positionRoleBaseVO);
                    return positionRoleBaseVO;
                }).collect(Collectors.toList());
                positionChildVO.setRoles(roles);
            }
        }
        return positionChildVOS;
    }

    /**
     * 查询部门列表树
     *
     * @return
     */
    @GetMapping("/departments/tree")
    @ApiOperation(value = "批量查询部门列表树", notes = "批量查询部门列表树")
    List<DepartmentTreeNoIdVO> getDepartmentsTree() {
        List<DepartmentAddPO> list = departmentService.listDepartments();
        if (list == null || list.size() == 0) {
            return null;
        }

        List<DepartmentTreeNoIdVO> departmentTreeNoIdVOS = new ArrayList<>();
        Map<Long, DepartmentTreeNoIdVO> departmentTreeNoIdVOMap = new HashMap<>();
        for (DepartmentAddPO departmentAddPO : list) {
            DepartmentTreeNoIdVO departmentTreeNoIdVO = new DepartmentTreeNoIdVO();
            BeanUtils.copyProperties(departmentAddPO, departmentTreeNoIdVO);
            departmentTreeNoIdVO.setCompanyCode(companyService.getCompanyById(departmentAddPO.getCompanyId()).getCode());
            departmentTreeNoIdVOS.add(departmentTreeNoIdVO);
            departmentTreeNoIdVOMap.put(departmentTreeNoIdVO.getId(), departmentTreeNoIdVO);
        }

        for (int i = departmentTreeNoIdVOS.size() - 1; i >= 0; i--) {
            if (departmentTreeNoIdVOS.get(i).getParentId() != null &&
                    departmentTreeNoIdVOMap.containsKey(departmentTreeNoIdVOS.get(i).getParentId())) {
                DepartmentTreeNoIdVO companyTreeParent = departmentTreeNoIdVOMap.get(departmentTreeNoIdVOS.get(i).getParentId());
                companyTreeParent.getChildren().add(departmentTreeNoIdVOS.get(i));
                departmentTreeNoIdVOS.get(i).setParentCode(companyTreeParent.getCode());
                departmentTreeNoIdVOS.remove(i);
            }
        }

        return departmentTreeNoIdVOS;
    }
}
