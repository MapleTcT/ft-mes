package com.supcon.supfusion.organization.openapi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.supcon.supfusion.framework.cloud.annotation.OpenApi;
import com.supcon.supfusion.framework.cloud.common.constants.HttpConstants;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import com.supcon.supfusion.framework.cloud.common.result.PageResult;
import com.supcon.supfusion.framework.cloud.common.result.Result;
import com.supcon.supfusion.organization.common.constants.Constants;
import com.supcon.supfusion.organization.common.exception.DepartmentErrorEnum;
import com.supcon.supfusion.organization.common.exception.DepartmentException;
import com.supcon.supfusion.organization.common.exception.OrganizationErrorEnum;
import com.supcon.supfusion.organization.common.exception.OrganizationException;
import com.supcon.supfusion.organization.dao.po.position.PositionAddPO;
import com.supcon.supfusion.organization.openapi.vo.PositionAddVO;
import com.supcon.supfusion.organization.openapi.vo.PositionDetailVO;
import com.supcon.supfusion.organization.openapi.vo.PositionLocationVO;
import com.supcon.supfusion.organization.openapi.vo.PositionTreeNoIdVO;
import com.supcon.supfusion.organization.openapi.vo.PositionTreeVO;
import com.supcon.supfusion.organization.openapi.vo.PositionUpdateVO;
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
import com.supcon.supfusion.organization.service.bo.person.PersonBaseInfoBO;
import com.supcon.supfusion.organization.service.bo.person.PersonResultBO;
import com.supcon.supfusion.organization.service.bo.person.SystemCodeBO;
import com.supcon.supfusion.organization.service.bo.position.CompanyForPositionSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.position.DepartmentForPositionSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailBO;
import com.supcon.supfusion.organization.service.bo.position.PositionDetailInfoBO;
import com.supcon.supfusion.organization.service.bo.position.PositionLocationBO;
import com.supcon.supfusion.organization.service.bo.position.PositionRoleBaseBO;
import com.supcon.supfusion.organization.service.bo.position.PositionSynchronizationInfoBO;
import com.supcon.supfusion.organization.service.bo.position.PositionTreeBO;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 岗位管理接口
 *
 * @author
 * @date 20-5-20 上午10:42
 */
@Slf4j
@Setter
@Getter
@OpenApi(path = HttpConstants.URL_OPENAPI + HttpConstants.URL_SPLITER + "organization" + HttpConstants.URL_SPLITER + "v2")
@Validated
@Api(tags = "岗位管理OpenApi", description = "岗位管理OpenApi接口文档说明", hidden = true)
public class PositionController {

    @Autowired
    private PositionService positionService;

    @Autowired
    private PersonService personService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private DepartmentService departmentService;

    /**
     * 岗位新增openapi借口
     * @param positionAddVO
     */
    @PostMapping(value = "/position")
    @ResponseStatus(HttpStatus.OK)
    void addDepartment(@Validated @RequestBody PositionAddVO positionAddVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PositionAddPO positionAddPO = new PositionAddPO();
        BeanUtils.copyProperties(positionAddVO, positionAddPO);
        positionService.addPosition(positionAddPO, positionAddVO.getManagerIds(), tenantId);
    }

    /**
     * 根据id修改岗位信息
     * @param positionUpdateVO
     */
    @PutMapping(value = "/position")
    @ResponseStatus(HttpStatus.OK)
    void updateDepartment(@Validated @RequestBody PositionUpdateVO positionUpdateVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PositionAddPO positionAddPO = new PositionAddPO();
        BeanUtils.copyProperties(positionUpdateVO, positionAddPO);
        positionService.updatePosition(positionAddPO, positionUpdateVO.getManagerIds(), tenantId);
    }

    /**
     * 查询岗位详细信息
     * @param id 岗位id
     * @return
     */
    @GetMapping(value = "/position")
    @ResponseBody
    Result<PositionDetailVO> getDepDetail(@NotNull(message = Constants.POSITION_PARAM_ID_NECESSARY) @RequestParam("id") Long id) {
        PositionDetailBO positionDetailPO = positionService.getPosDetail(id);
        PositionDetailVO positionDetailVO = new PositionDetailVO();
        BeanUtils.copyProperties(positionDetailPO, positionDetailVO);
        return new Result<PositionDetailVO>(positionDetailVO);
    }

    /**
     *  删除指定岗位
     * @param id 岗位id
     */
    @DeleteMapping(value = "/position/{id}")
    @ResponseStatus(HttpStatus.OK)
    void deleteDep(@PathVariable("id") Long id) {
        String tenantId = RpcContext.getContext().getTenantId();
        positionService.deletePosById(id, tenantId);
    }

    /**
     * 修改岗位的位置
     * @param positionLocationVO
     */
    @PutMapping(value = "/position/location")
    @ResponseStatus(HttpStatus.OK)
    void updateDepLocation(@Validated @RequestBody PositionLocationVO positionLocationVO) {
        String tenantId = RpcContext.getContext().getTenantId();
        PositionLocationBO positionLocationPO = new PositionLocationBO();
        BeanUtils.copyProperties(positionLocationVO, positionLocationPO);
        positionService.updatePosLocation(positionLocationPO, tenantId);
    }

    /**
     * 查询岗位树形结构
     * @param companyId 公司id
     * @return
     */
    /*@GetMapping(value = "/positions")
    @ResponseBody
    Result<PositionTreeVO> getDepTree(@NotNull(message = Constants.POSITION_PARAM_COMPANYID_NECESSARY) @RequestParam("companyId") Long companyId, Long parentId, String keyword) {
        PositionTreeBO positionTreePO = positionService.getPosTree(companyId, parentId, keyword);
        PositionTreeVO positionTreeVO = new PositionTreeVO();
        BeanUtils.copyProperties(positionTreePO, positionTreeVO);
        List<PositionTreeBO> poList = positionTreePO.getChildren();
        if (poList == null || poList.size() == 0)
            return new Result<PositionTreeVO>(positionTreeVO);
        positionTreeVO.setChildren(new ArrayList<PositionTreeVO>());

        poToVo(positionTreePO.getChildren(), positionTreeVO.getChildren());
        return new Result<PositionTreeVO>(positionTreeVO);
    }*/


    @PostMapping(value = "/position/excel")
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
    private void poToVo(List<PositionTreeBO> poList, List<PositionTreeVO> voList) {
        poList.stream().forEach(poDep -> {
            PositionTreeVO voDep = new PositionTreeVO();
            BeanUtils.copyProperties(poDep, voDep);
            voList.add(voDep);
            if (poDep.getChildren() != null && poDep.getChildren().size() > 0) {
                voDep.setChildren(new ArrayList<PositionTreeVO>());
                poToVo(poDep.getChildren(), voDep.getChildren());
            }
        });
    }

    @GetMapping("/position/{positionId}/persons")
    @ApiOperation(value = "根据岗位id查询人员列表", notes = "根据岗位id查询人员列表")
    PageResult<PersonResultVO> queryPersonsByPositionId(@ApiParam(value = "岗位id", required = true) @PathVariable(value = "positionId") Long positionIdId,
                                                       @ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current" ,required = false)  Integer current,
                                                       @ApiParam(value = "每页条数", required = true) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<PersonResultBO> pageResultBO = positionService.queryPersonsByPositionId(positionIdId, current, pageSize);
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
    @GetMapping("/positions/page")
    @ApiOperation(value = "分页查询岗位列表", notes = "分页查询岗位列表")
    PageResult<PositionDetailVO> queryPositionsPage(@ApiParam(value = "当前页码", required = true) @Min(value = -1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                    @ApiParam(value = "每页条数", required = true) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @RequestParam("pageSize") Integer pageSize) {
        PageResult<PositionDetailBO> pageResultBO = positionService.queryPositionsPage(current, pageSize);
        if (pageResultBO == null) {
            return new PageResult<>(null, 0, pageSize, current);
        }
        List<PositionDetailVO> list = new ArrayList<>();
        pageResultBO.getList().stream().forEach(positionDetailBO -> {
            PositionDetailVO positionDetailVO = new PositionDetailVO();
            BeanUtils.copyProperties(positionDetailBO, positionDetailVO);
            list.add(positionDetailVO);
        });
        return new PageResult<>(list, pageResultBO.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/positions/{positionCode}/persons")
    @ApiOperation(value = "根据岗位编码查询岗位下的人员列表")
    PageResult<PersonBaseInfoVO> getPersonsByPositionCode(@ApiParam(value = "岗位编码", required = true) @PathVariable(value = "positionCode") String positionCode,
                                                          @ApiParam(value = "当前页码", required = false) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = false) Integer current,
                                                          @ApiParam(value = "每页条数", required = false) @Min (value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PersonBaseInfoBO> pageResult = personService.getPersonsByPositionCode(positionCode, current, pageSize);

        if (pageResult == null || pageResult.getList() == null || pageResult.getList().size() == 0) {
            return new PageResult<>(new ArrayList<>(), 0, pageSize, current);
        }
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

    @GetMapping("/positions")
    @ApiOperation(value = "批量查询岗位列表")
    PageResult<PositionSynchronizationInfoVO> getPositions(@ApiParam(value = "最后修改时间", required = false) @RequestParam(value = "modifyTime", required = false) String modifyTime,
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
        PageResult<PositionSynchronizationInfoBO> pageResult = positionService.getPositions(modifyTime, current, pageSize);

        List<PositionSynchronizationInfoVO> positionSynchronizationInfoVOS = new ArrayList<>();
        for (PositionSynchronizationInfoBO positionSynchronizationInfoBO : pageResult.getList()) {
            PositionSynchronizationInfoVO positionSynchronizationInfoVO = new PositionSynchronizationInfoVO();
            BeanUtils.copyProperties(positionSynchronizationInfoBO, positionSynchronizationInfoVO);
            DepartmentForPositionSynchronizationInfoVO departmentForPositionSynchronizationInfoVO = new DepartmentForPositionSynchronizationInfoVO();
            BeanUtils.copyProperties(positionSynchronizationInfoBO.getDepartment(), departmentForPositionSynchronizationInfoVO);
            positionSynchronizationInfoVO.setDepartment(departmentForPositionSynchronizationInfoVO);

            CompanyForPositionSynchronizationInfoVO companyForPositionSynchronizationInfoVO = new CompanyForPositionSynchronizationInfoVO();
            BeanUtils.copyProperties(positionSynchronizationInfoBO.getCompany(), companyForPositionSynchronizationInfoVO);
            positionSynchronizationInfoVO.setCompany(companyForPositionSynchronizationInfoVO);

            positionSynchronizationInfoVOS.add(positionSynchronizationInfoVO);
        }
        return new PageResult<>(positionSynchronizationInfoVOS, pageResult.getPagination().getTotal(), pageSize, current);
    }

    @GetMapping("/positions/{positionCode}")
    @ApiOperation(value = "根据岗位编码查询岗位详情")
    PositionDetailInfoVO getPositionByCode(@ApiParam(value = "岗位编码", required = true) @PathVariable(value = "positionCode", required = true) String positionCode) {
        PositionDetailInfoVO positionDetailInfoVO = new PositionDetailInfoVO();
        Result<PositionDetailInfoBO> positionDetailInfoBO = positionService.getPositionByCode(positionCode);
        BeanUtils.copyProperties(positionDetailInfoBO.getData(), positionDetailInfoVO);

        CompanyForPositionSynchronizationInfoBO companyBO = positionDetailInfoBO.getData().getCompany();
        CompanyForPositionSynchronizationInfoVO companyForPositionSynchronizationInfoVO = new CompanyForPositionSynchronizationInfoVO();
        BeanUtils.copyProperties(companyBO, companyForPositionSynchronizationInfoVO);
        positionDetailInfoVO.setCompany(companyForPositionSynchronizationInfoVO);

        DepartmentForPositionSynchronizationInfoBO departmentBO = positionDetailInfoBO.getData().getDepartment();
        DepartmentForPositionSynchronizationInfoVO departmentForPositionSynchronizationInfoVO = new DepartmentForPositionSynchronizationInfoVO();
        BeanUtils.copyProperties(departmentBO, departmentForPositionSynchronizationInfoVO);
        positionDetailInfoVO.setDepartment(departmentForPositionSynchronizationInfoVO);

        List<PositionRoleBaseBO> positionRoleBaseBOS = positionDetailInfoBO.getData().getRoles();
        if (!CollectionUtils.isEmpty(positionRoleBaseBOS)) {
            List<PositionRoleBaseVO> roles = positionRoleBaseBOS.stream().map(positionRoleBaseBO -> {
                PositionRoleBaseVO positionRoleBaseVO = new PositionRoleBaseVO();
                BeanUtils.copyProperties(positionRoleBaseBO, positionRoleBaseVO);
                return positionRoleBaseVO;
            }).collect(Collectors.toList());
            positionDetailInfoVO.setRoles(roles);
        }
        return new Result<>(positionDetailInfoVO).getData();
    }

    /**
     * 根据岗位编码查询岗位的子节点列表 test
     *
     * @param positionCode
     * @return
     */
    @GetMapping(value = "/positions/{positionCode}/children")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "查询指定岗位的子节点列表", notes = "查询指定岗位的子节点列表")
    public PageResult<PositionInfoVO> getPosChildList(@ApiParam(value = "岗位编码", required = true) @NotNull(message = Constants.POSITION_PARAM_CODE_NECESSARY)
                                                            @PathVariable(value = "positionCode", required = true) String positionCode,
                                                      @ApiParam(value = "是否查询多级") @RequestParam(value = "multistage", defaultValue = "true", required = false) Boolean multistage,
                                                      @ApiParam(value = "当前页码", required = true) @Min(value = 1, message = Constants.PAGE_CURRENT_ERROR) @NotNull(message = Constants.PAGE_CURRENT_ERROR) @RequestParam(value = "current", required = true) Integer current,
                                                      @ApiParam(value = "每页条数", required = false) @Min(value = 1, message = Constants.PAGE_PAGESIZE_ERROR) @Max(value = 500, message = Constants.PAGE_PAGESIZE_MAX_ERROR) @RequestParam(value = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        PageResult<PositionDetailInfoBO> posChildList = positionService.querySubPositionByParentCode(positionCode, multistage, current, pageSize);
        if (null == posChildList) {
            return new PageResult<PositionInfoVO>(new ArrayList<>(1), 0, pageSize, current);
        }

        List<PositionInfoVO> positionDetailInfoVOList = new ArrayList<>();
        posChildList.getList().stream().forEach(bo -> {
                    PositionInfoVO positionDetailInfoVO = new PositionInfoVO();
                    BeanUtils.copyProperties(bo, positionDetailInfoVO);

                    // 公司
                    CompanyForPositionSynchronizationInfoVO companyVo = new CompanyForPositionSynchronizationInfoVO();
                    BeanUtils.copyProperties(bo.getCompany(), companyVo);
                    positionDetailInfoVO.setCompany(companyVo);

                    // 部门
                    DepartmentForPositionSynchronizationInfoVO departmentVo = new DepartmentForPositionSynchronizationInfoVO();
                    BeanUtils.copyProperties(bo.getDepartment(), departmentVo);
                    positionDetailInfoVO.setDepartment(departmentVo);

                    // 角色
                    List<PositionRoleBaseBO> roleBoList = bo.getRoles();
                    if (roleBoList != null) {
                        List<PositionRoleBaseVO> roleVoList = new ArrayList<>();
                        roleBoList.stream().forEach(roleBaseBO -> {
                            PositionRoleBaseVO positionRoleBaseVO = new PositionRoleBaseVO();
                            BeanUtils.copyProperties(roleBaseBO, positionRoleBaseVO);
                            roleVoList.add(positionRoleBaseVO);
                        });
                        positionDetailInfoVO.setRoles(roleVoList);
                    }

                    positionDetailInfoVOList.add(positionDetailInfoVO);
                }
        );

        return new PageResult<PositionInfoVO>(positionDetailInfoVOList, posChildList.getPagination().getTotal(), pageSize, current);
    }

    /**
     * 查询部门列表树
     *
     * @return
     */
    @GetMapping("/positions/tree")
    @ApiOperation(value = "批量查询岗位列表树", notes = "批量查询岗位列表树")
    List<PositionTreeNoIdVO> getPositionsTree() {
        List<PositionAddPO> list = positionService.listDepartments();
        if (list == null || list.size() == 0) {
            return null;
        }

        List<PositionTreeNoIdVO> positionTreeVOS = new ArrayList<>();
        Map<Long, PositionTreeNoIdVO> positionTreeVOHashMap = new HashMap<>();
        for (PositionAddPO positionAddPO : list) {
            PositionTreeNoIdVO positionTreeVO = new PositionTreeNoIdVO();
            BeanUtils.copyProperties(positionAddPO, positionTreeVO);
            positionTreeVO.setCompanyCode(companyService.getCompanyById(positionTreeVO.getCompanyId()).getCode());
            positionTreeVO.setDepCode(departmentService.getDepDetail(positionAddPO.getDepId()).getCode());
            positionTreeVOS.add(positionTreeVO);
            positionTreeVOHashMap.put(positionTreeVO.getId(), positionTreeVO);
        }

        for (int i = positionTreeVOS.size() - 1; i >= 0; i--) {
            if (positionTreeVOS.get(i).getParentId() != null &&
                    positionTreeVOHashMap.containsKey(positionTreeVOS.get(i).getParentId())) {
                PositionTreeNoIdVO companyTreeParent = positionTreeVOHashMap.get(positionTreeVOS.get(i).getParentId());
                companyTreeParent.getChildren().add(positionTreeVOS.get(i));
                positionTreeVOS.get(i).setParentCode(companyTreeParent.getCode());
                positionTreeVOS.remove(i);
            }
        }

        return positionTreeVOS;
    }
}
